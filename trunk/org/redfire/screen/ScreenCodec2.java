package org.redfire.screen;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.awt.image.*;


class ScreenCodec2 implements ScreenCodec
{

    public static final int a = 1;
    public static final int b = 2;
    public final int c;
    public final int d;
    public final int e = 32;
    public final int f = 32;
    private Deflater deflater;
    private int h[];
    private int i[];
    private int j;

    private static final int k[] =
    {
        0, 0x333333, 0x666666, 0x999999, 0xcccccc, 0xffffff, 0x330000, 0x660000, 0x990000, 0xcc0000,
        0xff0000, 13056, 26112, 39168, 52224, 65280, 51, 102, 153, 204,
        255, 0x333300, 0x666600, 0x999900, 0xcccc00, 0xffff00, 13107, 26214, 39321, 52428,
        65535, 0x330033, 0x660066, 0x990099, 0xcc00cc, 0xff00ff, 0xffff33, 0xffff66, 0xffff99, 0xffffcc,
        0xff33ff, 0xff66ff, 0xff99ff, 0xffccff, 0x33ffff, 0x66ffff, 0x99ffff, 0xccffff, 0xcccc33, 0xcccc66,
        0xcccc99, 0xccccff, 0xcc33cc, 0xcc66cc, 0xcc99cc, 0xccffcc, 0x33cccc, 0x66cccc, 0x99cccc, 0xffcccc,
        0x999933, 0x999966, 0x9999cc, 0x9999ff, 0x993399, 0x996699, 0x99cc99, 0x99ff99, 0x339999, 0x669999,
        0xcc9999, 0xff9999, 0x666633, 0x666699, 0x6666cc, 0x6666ff, 0x663366, 0x669966, 0x66cc66, 0x66ff66,
        0x336666, 0x996666, 0xcc6666, 0xff6666, 0x333366, 0x333399, 0x3333cc, 0x3333ff, 0x336633, 0x339933,
        0x33cc33, 0x33ff33, 0x663333, 0x993333, 0xcc3333, 0xff3333, 13158, 0x336600, 0x660033, 26163,
        0x330066, 0x663300, 0x336699, 0x669933, 0x993366, 0x339966, 0x663399, 0x996633, 0x6699cc, 0x99cc66,
        0xcc6699, 0x66cc99, 0x9966cc, 0xcc9966, 0x99ccff, 0xccff99, 0xff99cc, 0x99ffcc, 0xcc99ff, 0xffcc99,
        0x111111, 0x222222, 0x444444, 0x555555, 0xaaaaaa, 0xbbbbbb, 0xdddddd, 0xeeeeee
    };


    public ScreenCodec2(int l, int i1)
    {
        deflater = new Deflater();
        h = null;
        i = null;
        j = 0;
        c = l;
        d = i1;
        i = new int[32768];
    }

    public byte[]  encode(final BufferedImage image) throws Exception
    {
        boolean flag = true;

        int[] ai = ((DataBufferInt)image.getData().getDataBuffer()).getData();
        ai = filterImage(ai);

        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream(0x10000);

		if (h == null)
		{
			bytearrayoutputstream.write(getTag(0x01, 0x06));		// keyframe (all cells)
		}
		else
		{
			bytearrayoutputstream.write(getTag(0x02, 0x06));		// frame (changed cells)
		}

        int l = c + 4096;
        int i1 = d + 4096;

        writeShort(((OutputStream) (bytearrayoutputstream)), l);
        writeShort(((OutputStream) (bytearrayoutputstream)), i1);

        if (h == null) doKeyBlock(ai, i);

        bytearrayoutputstream.write(0);
        int j1 = d;
        boolean flag1 = false;
        byte byte0 = 32;
        byte byte2 = 32;

        while(j1 > 0)
        {
            int i2 = Math.min(j1, 32);
            j1 -= i2;
            byte byte1 = 32;
            int k1 = 0;

            while(k1 < c)
            {
                int l1 = k1 + 32 <= c ? 32 : c - k1;
                boolean flag2 = isChanged(ai, h, k1, j1, l1, i2, c, d);

                if(flag2)
                {
                    ByteArrayOutputStream bytearrayoutputstream1 = new ByteArrayOutputStream(4096);

                    for(int j2 = (j1 + i2) - 1; j2 >= j1; j2--)
                    {
                        int k2 = k1;
                        for(int l2 = k1 + l1; k2 < l2; k2++)
                        {
                            int i3 = ai[j2 * c + k2];
                            int k3 = i[i3];
                            if(k3 < 0)
                                writeShort(((OutputStream) (bytearrayoutputstream1)), 32768 + i3);
                            else
                                bytearrayoutputstream1.write(k3);
                        }

                    }

					try {
						ByteArrayOutputStream bytearrayoutputstream2 = new ByteArrayOutputStream(4096);
						bytearrayoutputstream2.write(16);
						DeflaterOutputStream deflateroutputstream = new DeflaterOutputStream(bytearrayoutputstream2, deflater);
						deflateroutputstream.write(bytearrayoutputstream1.toByteArray());
						deflateroutputstream.finish();
						deflater.reset();
						byte abyte1[] = bytearrayoutputstream2.toByteArray();
						int j3 = abyte1.length;
						writeShort(((OutputStream) (bytearrayoutputstream)), j3);
						bytearrayoutputstream.write(abyte1, 0, j3);

					} catch (Exception e) {
						e.printStackTrace();
					}

                } else
                {
                    flag = false;
                    writeShort(((OutputStream) (bytearrayoutputstream)), 0);
                }
                k1 += l1;
            }
        }

        h = ai;

        if(flag)
        {
            j = 0;

        } else  {

            j++;

            if(j > 10) h = null;
        }

        return bytearrayoutputstream.toByteArray();
    }


    private int[] filterImage(int ai[])
    {
        int l = 0;

        for(int i1 = ai.length; l < i1; l++)
        {
            ai[l] = ((ai[l] & 0xf80000) >> 9 | (ai[l] & 0xf800) >> 6 | (ai[l] & 0xf8) >> 3) & 0x7fff;
		}

        return ai;
    }

    private int[] doKeyBlock(int ai[], int ai1[])
    {
        int l = 0;

        for(int i1 = ai1.length; l < i1; l++)
            ai1[l] = 0;

        l = 0;

        for(int j1 = ai.length; l < j1; l++)
            ai1[ai[l]]++;

        l = 0;

        for(int k1 = ai1.length; l < k1; l++)
            ai1[l]++;

        int ai2[] = new int[128];
        int l1 = 32768;
        int i2 = -1;
        int j2 = 0;

        for(int k2 = ai2.length; j2 < k2; j2++)
        {
            int j3 = ai1[j2];
            if(j3 < l1)
            {
                l1 = j3;
                i2 = j2;
            }
        }

        j2 = ai2.length;

        for(int l2 = ai1.length; j2 < l2; j2++)
        {
            if(ai1[j2] <= l1)
            {
                if(ai1[j2] > 0)
                    ai1[j2] *= -1;
                continue;
            }
            if(ai1[i2] > 0)
                ai1[i2] *= -1;

            int k3 = 32768;
            int i4 = -1;

            for(int k4 = 0; k4 < j2; k4++)
            {
                int l4 = ai1[k4];
                if(l4 >= 0 && l4 < k3)
                {
                    k3 = l4;
                    i4 = k4;
                }
            }

            l1 = k3;
            i2 = i4;
        }

        j2 = 0;
        int i3 = 0;

        for(int l3 = ai1.length; i3 < l3; i3++)
        {
            int j4 = ai1[i3];
            if(j4 >= 0)
            {
                ai1[i3] = getPixel(i3);
                ai2[j2++] = j4;
            }
        }

        return ai2;
    }

    private int getPixel(int l)
    {
        int i1 = (l << 9 & 0xf80000) >> 16;
        int j1 = (l << 6 & 0xf800) >> 8;
        int k1 = l << 3 & 0xf8;
        int l1 = -1;
        int i2 = 0x7fffffff;
        int j2 = 0;

        for(int k2 = k.length; j2 < k2; j2++)
        {
            int l2 = k[j2];
            int i3 = l2 >> 16 & 0xff;
            int j3 = l2 >> 8 & 0xff;
            int k3 = l2 >> 0 & 0xff;
            int l3 = i3 - i1;
            int i4 = j3 - j1;
            int j4 = k3 - k1;
            int k4 = l3 * l3 + i4 * i4 + j4 * j4;

            if(k4 == 0)
                return j2;

            if(k4 < i2)
            {
                l1 = j2;
                i2 = k4;
            }
        }

        return l1;
    }

    private void writeShort(OutputStream outputstream, int l)
    {
		try {
        	outputstream.write(l >> 8 & 0xff);
        	outputstream.write(l >> 0 & 0xff);

		} catch (Exception e) {

			e.printStackTrace();
		}
    }

    private boolean isChanged(int ai[], int ai1[], int l, int i1, int j1, int k1, int l1, int i2)
    {
        if(ai1 == null)
            return true;
        for(int j2 = Math.min((i1 + k1) - 1, i2 - 1); j2 >= i1; j2--)
        {
            int k2 = l;
            for(int l2 = Math.min(l + k1, l1); k2 < l2; k2++)
            {
                int i3 = k2 + l1 * j2;
                if(ai[i3] != ai1[i3])
                    return true;
            }

        }

        return false;
    }

    private int getTag(int l, int i1)
    {
        return ((l & 0xf) << 4) + ((i1 & 0xf) << 0);
    }

}
