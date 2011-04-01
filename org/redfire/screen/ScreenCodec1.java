package org.redfire.screen;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.awt.image.*;

class ScreenCodec1 implements ScreenCodec
{
    private final int width;
    private final int height;
	private final int blockWidth = 32;
	private final int blockHeight = 32;
	private int frameCounter;
	private byte[] previous;

    public ScreenCodec1(final int width, final int height)
    {
        this.width = width;
        this.height = height;
        previous = null;
    }


	public byte[] encode(final BufferedImage image) throws Exception
	{
        boolean flag = true;
        byte[] current = toBGR(image);

		ByteArrayOutputStream baos = new ByteArrayOutputStream(16 * 1024);

		if (previous == null)
		{
			baos.write(getTag(0x01, 0x03));		// keyframe (all cells)
		}
		else
		{
			baos.write(getTag(0x02, 0x03));		// frame (changed cells)
		}

		// write header
		final int wh = width + ((blockWidth / 16 - 1) << 12);
		final int hh = height + ((blockHeight / 16 - 1) << 12);

		writeShort(baos, wh);
		writeShort(baos, hh);

		// write content
		int y0 = height;
		int x0 = 0;
		int bwidth = blockWidth;
		int bheight = blockHeight;

		while (y0 > 0)
		{
			bheight = Math.min(y0, blockHeight);
			y0 -= bheight;

			bwidth = blockWidth;
			x0 = 0;

			while (x0 < width)
			{
				bwidth = (x0 + blockWidth > width) ? width - x0 : blockWidth;

				final boolean changed = isChanged(current, previous, x0, y0, bwidth, bheight, width, height);

				if (changed)
				{
					ByteArrayOutputStream blaos = new ByteArrayOutputStream(4 * 1024);

					DeflaterOutputStream dos = new DeflaterOutputStream(blaos);

					for (int y = 0; y < bheight; y++)
					{
						dos.write(current, 3 * ((y0 + bheight - y - 1) * width + x0), 3 * bwidth);
					}

					dos.finish();

					final byte[] bbuf = blaos.toByteArray();
					final int written = bbuf.length;

					// write DataSize
					writeShort(baos, written);
					// write Data
					baos.write(bbuf, 0, written);
				}
				else
				{
					// write DataSize
					writeShort(baos, 0);
					flag = false;
				}

				x0 += bwidth;
			}
		}

		previous = current;

		if (flag)
		{
			frameCounter = 0;

		} else {

			frameCounter++;

			if (frameCounter > 10) previous = null;

		}
		return baos.toByteArray();
	}

	private byte[] toBGR(BufferedImage image)
	{
		final int width = image.getWidth();
		final int height = image.getHeight();

		byte[] buf = new byte[3 * width * height];

		final DataBuffer buffer = image.getData().getDataBuffer();

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				final int rgb = buffer.getElem(y * width + x);
				final int offset = 3 * (y * width + x);

				buf[offset + 0] = (byte) (rgb & 0xFF);
				buf[offset + 1] = (byte) ((rgb >> 8) & 0xFF);
				buf[offset + 2] = (byte) ((rgb >> 16) & 0xFF);
			}
		}

		return buf;
	}

	private void writeShort(OutputStream os, final int n) throws Exception
	{
		os.write((n >> 8) & 0xFF);
		os.write((n >> 0) & 0xFF);
	}


	private boolean isChanged(final byte[] current, final byte[] previous, final int x0, final int y0, final int blockWidth, final int blockHeight, final int width, final int height)
	{
		if (previous == null) return true;

		for (int y = y0, ny = y0 + blockHeight; y < ny; y++)
		{
			final int foff = 3 * (x0 + width * y);
			final int poff = 3 * (x0 + width * y);

			for (int i = 0, ni = 3 * blockWidth; i < ni; i++)
			{
				if (current[foff + i] != previous[poff + i]) return true;
			}
		}

		return false;
	}

	private int getTag(final int frame, final int codec)
	{
		return ((frame & 0x0F) << 4) + ((codec & 0x0F) << 0);
	}

}
