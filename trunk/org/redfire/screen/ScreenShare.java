package org.redfire.screen;

import java.io.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
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
    public String codec = "flashsv1";

	public CaptureScreen capture = null;
	public Thread thread = null;
	public Robot robot;

	public java.awt.Container contentPane;
	public JFrame t = null;
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

	private long startTime;
    private int kt = 0;

    // ------------------------------------------------------------------------
    //
    // Main
    //
    // ------------------------------------------------------------------------


	public static void main(String[] args)
	{
		instance = ScreenShare.getInstance();

		if (args.length == 5) {
			instance.host = args[0];
			instance.app = args[1];
			instance.port = Integer.parseInt(args[2]);
			instance.publishName = args[3];
			instance.codec = args[4];

			System.out.println("User home " + System.getProperty("user.home"));
			System.out.println("User Dir " + System.getProperty("user.dir"));

		} else {
			instance = null;
			System.out.println("\nRed5 SceenShare: use as java ScreenShare <host> <app name> <port> <stream name>\n Example: SceenShare localhost oflaDemo 1935 screen_stream");
			System.exit(0);
		}

		logger.debug("host: " + instance.host + ", app: " + instance.app + ", port: " + instance.port + ", publish: " + instance.publishName);

		instance.createWindow();
	}

	private ScreenShare() {}

	public static ScreenShare getInstance()
	{
		if (instance == null) instance = new ScreenShare();

		return instance;
	}


    // ------------------------------------------------------------------------
    //
    // GUI
    //
    // ------------------------------------------------------------------------

	public void createWindow()
	{

		try {

			if (options == null)
			{
				options = new ClientOptions(instance.host, instance.port, instance.app, instance.publishName, null, false, null);
				options.publishLive();
				options.setClientVersionToUse(Utils.fromHex("00000000"));
			}

			if (t != null && t.isVisible())
			{
				return;
			}

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
					stopStream();
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
					stopStream();
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

		startTime = System.currentTimeMillis();
		kt = 0;

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
            thread = null;
            startPublish = false;

            disconnect();

            capture.stop();
            capture.release();
            capture = null;
        }
        catch ( Exception e ) {

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

		try {
			this.robot = new Robot();

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

		} catch (Exception e) {

			logger.error("screenPublish error " + e);
			e.printStackTrace();
			showBandwidthWarning("Internal error capturing screen, see log file");
		}
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

	public void mousePress(double button)
	{
		if (capture != null && robot != null)
		{
			logger.info("mousePress " + button);

			if (button == 1) robot.mousePress(InputEvent.BUTTON1_MASK);
			if (button == 2) robot.mousePress(InputEvent.BUTTON2_MASK);
			if (button == 3) robot.mousePress(InputEvent.BUTTON3_MASK);
		}
	}

	public void mouseRelease(double button)
	{
		if (capture != null && robot != null)
		{
			logger.info("mouseRelease " + button);

			if (button == 1) robot.mouseRelease(InputEvent.BUTTON1_MASK);
			if (button == 2) robot.mouseRelease(InputEvent.BUTTON2_MASK);
			if (button == 3) robot.mouseRelease(InputEvent.BUTTON3_MASK);
		}
	}

	public void doubleClick(double x, double y, double width, double height)
	{
		if (capture != null && robot != null)
		{
			logger.info("doubleClick " + x + " " + y + " " + width + " " + height);

			int newX = (int)((x/width*capture.width) + capture.x);
			int newY = (int)((y/height*capture.height) + capture.y);

			robot.mouseMove(newX, newY);
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
		}
	}

	public void keyPress(double key)
	{
		int newKey = translateKey(key);

		if (capture != null && robot != null)
		{
			logger.info("keyPress " + key);
			robot.keyPress(newKey);
		}
	}

	public void keyRelease(double key)
	{
		int newKey = translateKey(key);

		if (capture != null && robot != null)
		{
			logger.info("keyRelease " + key);
			robot.keyRelease(newKey);
		}
	}

	public void mouseMove(double x, double y, double width, double height)
	{
		if (capture != null && robot != null)
		{
			logger.info("mouseMove " + x + " " + y + " " + width + " " + height);

			int newX = (int)((x/width*capture.width) + capture.x);
			int newY = (int)((y/height*capture.height) + capture.y);

			robot.mouseMove(newX, newY);
		}
	}

	private int translateKey(double key)
	{
		if (key == 13)
			return 10;
		else
			return (int) key;
	}

	// ------------------------------------------------------------------------
	//
	// CaptureScreen
	//
	// ------------------------------------------------------------------------


	private final class CaptureScreen extends Object implements Runnable
	{
		public volatile int x = 0;
		public volatile int y = 0;
		public volatile int width = 320;
		public volatile int height = 240;

		private volatile long timestamp = 0;

		private volatile boolean active = true;
		private volatile boolean stopped = false;
		private BufferedImage cursorImage;

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

			try
			{
				cursorImage = ImageIO.read(ScreenShare.class.getResource("/cursor.gif"));

			} catch (Exception e) {

				logger.error("error loading cursor.gif " + e);
			}

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
			double widthTransformScale = 0.5;
			double heightTransformScale = 0.5;

			final int timeBetweenFrames = 1000; //frameRate

			widthTransformScale = width > 1024 ? (double) (width/1024) : 1;
			heightTransformScale = height > 768 ? (double) (height/768) : 1;

        	AffineTransformOp affinetransformop = new AffineTransformOp(AffineTransform.getScaleInstance(widthTransformScale, heightTransformScale), new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC));

			try
			{
				ScreenCodec screenCodec;

				if ("flashsv1".equals(codec))
					screenCodec = new ScreenCodec1(width, height);
				else
					screenCodec = new ScreenCodec2(width, height);

				while (active)
				{
					final long ctime = System.currentTimeMillis();

					try
					{
						BufferedImage image = robot.createScreenCapture(new Rectangle(x, y, width, height));
						BufferedImage image1 = addCursor(image);
						//BufferedImage image2 = affinetransformop.filter(image1, null);

						timestamp =  System.currentTimeMillis() - startTime;

						final byte[] screenBytes = screenCodec.encode(image1);
						pushVideo(screenBytes, timestamp);

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

		private BufferedImage addCursor(BufferedImage image)
		{
			BufferedImage newImage = image;

			Point point = MouseInfo.getPointerInfo().getLocation();

			Graphics2D g2d = newImage.createGraphics();
			g2d.drawImage(cursorImage, new AffineTransform(1f,0f,0f,1f, point.x, point.y), null);
			g2d.dispose();

			return newImage;
		}
	}

}

