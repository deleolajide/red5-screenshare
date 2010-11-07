package org.redfire.screen;

import java.io.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.flazr.util.Utils;
import com.flazr.rtmp.*;
import com.flazr.rtmp.client.ClientOptions;
import com.flazr.rtmp.message.*;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ScreenShare {

    private static final Logger logger = LoggerFactory.getLogger( ScreenShare.class );
    public static ScreenShare instance = null;
	public static ClientOptions options;

    public boolean startPublish = false;
    public Integer playStreamId;
    public Integer publishStreamId;
    public String publishName;
    public int videoTs = 0;
    public int audioTs = 0;
    public int kt = 0;
    public int kt2 = 0;
	public CaptureScreen capture = null;
	public Thread thread = null;

	public java.awt.Container contentPane;
	public JFrame t;
	public JLabel textArea;
	public JLabel textWarningArea;
	public JLabel textAreaQualy;
	public JButton startButton;
	public JButton stopButton;
	public JButton exitButton;
	public JSpinner jSpin;
	public JLabel tFieldScreenZoom;
	public JLabel blankArea;
	public BlankArea virtualScreen;
	public JLabel vscreenXLabel;
	public JLabel vscreenYLabel;
	public JSpinner jVScreenXSpin;
	public JSpinner jVScreenYSpin;
	public JLabel vscreenWidthLabel;
	public JLabel vscreenHeightLabel;
	public JSpinner jVScreenWidthSpin;
	public JSpinner jVScreenHeightSpin;

	public JLabel vScreenIconLeft;
	public JLabel vScreenIconRight;
	public JLabel vScreenIconUp;
	public JLabel vScreenIconDown;
	public JLabel myBandWidhtTestLabel;

	public String host = "btg199251";
	public String app = "oflaDemo";
	public int port = 1935;

	public Float imgQuality = new Float(0.40);

	private Channel clientChannel;
	private ScreenPublisher publisher;

    // ------------------------------------------------------------------------
    //
    // Main
    //
    // ------------------------------------------------------------------------


	public static void main(String[] args)
	{
		instance = new ScreenShare();

		if (args.length == 4) {
			instance.host = args[0];
			instance.app = args[1];
			instance.port = Integer.parseInt(args[2]);
			instance.publishName = args[3];

			options = new ClientOptions(instance.host, instance.port, instance.app, instance.publishName, null, false, null);
			options.publishLive();
			options.setClientVersionToUse(Utils.fromHex("00000000"));

		} else {
			instance = null;
			System.out.println("\nRed5 SceenShare: use as java ScreenShare <host> <app name> <port> <stream name>\n Example: SceenShare localhost oflaDemo 1935 screen_stream");
			System.exit(0);
		}

		logger.debug("host: " + instance.host + ", app: " + instance.app + ", port: " + instance.port + ", publish: " + instance.publishName);

		instance.createWindow();
	}


    // ------------------------------------------------------------------------
    //
    // GUI
    //
    // ------------------------------------------------------------------------

	public void createWindow()
	{
		try {

			//UIManager.setLookAndFeel(new com.incors.plaf.kunststoff.KunststoffLookAndFeel());
			//UIManager.getLookAndFeelDefaults().put( "ClassLoader", getClass().getClassLoader()  );

			t = new JFrame("Desktop Publisher");
			contentPane = t.getContentPane();
			contentPane.setBackground(Color.WHITE);
			textArea = new JLabel();
			textArea.setBackground(Color.WHITE);
			contentPane.setLayout(null);
			contentPane.add(textArea);
			textArea.setText("This application will publish your screen");
			textArea.setBounds(10, 0, 400,24);

			startButton = new JButton( "start Sharing" );
			startButton.addActionListener( new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					// TODO Auto-generated method stub
					captureScreenStart();
				}
			});
			startButton.setBounds(10, 50, 200, 24);
			t.add(startButton);


			stopButton = new JButton( "stop Sharing" );
			stopButton.addActionListener( new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					// TODO Auto-generated method stub
					captureScreenStop();
				}
			});
			stopButton.setBounds(220, 50, 200, 24);
			stopButton.setEnabled(false);
			t.add(stopButton);

			//add the small screen thumb to the JFrame
			new VirtualScreen();

			textWarningArea = new JLabel();
			contentPane.add(textWarningArea);
			textWarningArea.setBounds(10, 310, 400,54);
			//textWarningArea.setBackground(Color.WHITE);

			exitButton = new JButton( "exit" );
			exitButton.addActionListener( new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					// TODO Auto-generated method stub
					t.setVisible(false);
					System.exit(0);
				}
			});
			exitButton.setBounds(190, 370, 200, 24);
			t.add(exitButton);

			Image im_left = ImageIO.read(ScreenShare.class.getResource("/background.png"));
			ImageIcon iIconBack = new ImageIcon(im_left);

			JLabel jLab = new JLabel(iIconBack);
			jLab.setBounds(0, 0, 500, 440);
			t.add(jLab);

			t.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					t.setVisible(false);
					System.exit(0);
				}

			});
			t.pack();
			t.setSize(500, 440);
			t.setVisible(true);
			t.setResizable(false);


			System.err.println("initialized");

		} catch (Exception err)
		{
			System.out.println("createWindow Exception: ");
			err.printStackTrace();
		}
	}

	public void showBandwidthWarning(String warning)
	{
		textWarningArea.setText(warning);
	}

	private void captureScreenStart()
	{
		try {

			System.err.println("captureScreenStart");

			startStream(host, app, port, publishName);

		} catch (Exception err) {
			System.out.println("captureScreenStart Exception: ");
			System.err.println(err);
			textArea.setText("Exception: "+err);
		}
	}

	private void captureScreenStop()
	{
		try {
			stopStream();
			startButton.setEnabled(true);
			stopButton.setEnabled(false);

		} catch (Exception err) {
			System.out.println("captureScreenStop Exception: ");
			System.err.println(err);
			textArea.setText("Exception: "+err);
		}
	}

    // ------------------------------------------------------------------------
    //
    // Public
    //
    // ------------------------------------------------------------------------


    public void startStream( String host, String app, int port, String publishName) {

        System.out.println( "ScreenShare startStream" );
        this.publishName = publishName;

        videoTs = 0;
        audioTs = 0;
        kt = 0;
        kt2 = 0;

		ExecutorService executor = Executors.newCachedThreadPool();

        executor.submit(new Callable<Boolean>()
        {
            public Boolean call() throws Exception
            {
                try {
            		connect(options);
                }
                catch (Exception e) {
            		logger.error( "ScreenShare startStream exception " + e );
                }

                return true;
            }
        });
    }


    public void stopStream() {

        System.out.println( "ScreenShare stopStream" );

        try {
            disconnect();
            capture.stop();
            capture.release();
            thread = null;
            startPublish = false;
        }
        catch ( Exception e ) {
            logger.error( "ScreenShare stopStream exception " + e );
        }

    }


    // ------------------------------------------------------------------------
    //
    // Implementations
    //
    // ------------------------------------------------------------------------

    public void connect(final ClientOptions options)
    {
        final ClientBootstrap bootstrap = getBootstrap(Executors.newCachedThreadPool(), options);
        final ChannelFuture future = bootstrap.connect(new InetSocketAddress(options.getHost(), options.getPort()));
        future.awaitUninterruptibly();

        if(!future.isSuccess())
        {
            // future.getCause().printStackTrace();
            logger.error("error creating client connection: {}", future.getCause().getMessage());
        }

		clientChannel = future.getChannel();
        future.getChannel().getCloseFuture().awaitUninterruptibly();
        bootstrap.getFactory().releaseExternalResources();
    }

    public void disconnect()
    {
		final ChannelFuture future = clientChannel.disconnect();
        future.awaitUninterruptibly();
        clientChannel.getFactory().releaseExternalResources();
    }

    private ClientBootstrap getBootstrap(final Executor executor, final ClientOptions options)
    {
        final ChannelFactory factory = new NioClientSocketChannelFactory(executor, executor);
        final ClientBootstrap bootstrap = new ClientBootstrap(factory);
        bootstrap.setPipelineFactory(new ScreenClientPipelineFactory(options, this));
        bootstrap.setOption("tcpNoDelay" , true);
        bootstrap.setOption("keepAlive", true);
        return bootstrap;
    }

    public void screenPublish(ScreenPublisher publisher )
    {
		this.publisher = publisher;

		logger.debug( "setup capture thread");

		capture = new CaptureScreen(VirtualScreenBean.vScreenSpinnerX,
									VirtualScreenBean.vScreenSpinnerY,
									VirtualScreenBean.vScreenSpinnerWidth,
									VirtualScreenBean.vScreenSpinnerHeight);

		if (thread == null)
		{
			thread = new Thread(capture);
			thread.start();
		}

		capture.start();
		startButton.setEnabled(false);
		stopButton.setEnabled(true);
		startPublish = true;
    }

    public void pushVideo(byte[] video, long ts) throws IOException {

		if (!startPublish) return;

		RtmpMessage rtmpMsg = new Video(video);
		rtmpMsg.getHeader().setTime((int)ts);
		publisher.write(clientChannel, rtmpMsg);

        kt++;

        if ( kt < 10 ) {
            logger.debug( "+++ " + rtmpMsg );
            System.out.println( "+++ " + rtmpMsg);
        }

    }


	// ------------------------------------------------------------------------
	//
	// CaptureScreen
	//
	// ------------------------------------------------------------------------


	private final class CaptureScreen extends Object implements Runnable
	{
		private volatile int x = 0;
		private volatile int y = 0;
		private volatile int width = 320;
		private volatile int height = 240;
		private volatile long timestamp = 0;

		private volatile boolean active = true;
		private volatile boolean stopped = false;

		// ------------------------------------------------------------------------
		//
		// Constructor
		//
		// ------------------------------------------------------------------------


		public CaptureScreen(final int x, final int y, final int width, final int height)
		{
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;

        	logger.debug( "CaptureScreen: x=" + x + ", y=" + y + ", w=" + width + ", h=" + height );

		}


		// ------------------------------------------------------------------------
		//
		// Public
		//
		// ------------------------------------------------------------------------

		public void setOrigin(final int x, final int y)
		{
			this.x = x;
			this.y = y;
		}


		public void start()
		{
			stopped = false;
		}


		public void stop()
		{
			stopped = true;
		}

		public void release()
		{
			active = false;
		}


		// ------------------------------------------------------------------------
		//
		// Thread loop
		//
		// ------------------------------------------------------------------------

		public void run()
		{
			final int blockWidth = 32;
			final int blockHeight = 32;

			final int timeBetweenFrames = 1000; //frameRate

			int frameCounter = 0;

			try
			{
				Robot robot = new Robot();

				byte[] previous = null;

				while (active)
				{
					final long ctime = System.currentTimeMillis();

					BufferedImage image = robot.createScreenCapture(new Rectangle(x, y, width, height));

					byte[] current = toBGR(image);

					try
					{
						timestamp += (1000000 / timeBetweenFrames);

						final byte[] screenBytes = encode(current, previous, blockWidth, blockHeight, width, height);
						pushVideo(screenBytes, timestamp);
						previous = current;

						if (++frameCounter % 100 == 0) previous = null;
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					final int spent = (int) (System.currentTimeMillis() - ctime);

					Thread.sleep(Math.max(0, timeBetweenFrames - spent));
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}


		// ------------------------------------------------------------------------
		//
		// Private
		//
		// ------------------------------------------------------------------------

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


		private byte[] encode(final byte[] current, final byte[] previous, final int blockWidth, final int blockHeight, final int width, final int height) throws Exception
		{
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
					}

					x0 += bwidth;
				}
			}

			return baos.toByteArray();
		}

		private void writeShort(OutputStream os, final int n) throws Exception
		{
			os.write((n >> 8) & 0xFF);
			os.write((n >> 0) & 0xFF);
		}

		public boolean isChanged(final byte[] current, final byte[] previous, final int x0, final int y0, final int blockWidth, final int blockHeight, final int width, final int height)
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


		public int getTag(final int frame, final int codec)
		{
			return ((frame & 0x0F) << 4) + ((codec & 0x0F) << 0);
		}
	}

}

