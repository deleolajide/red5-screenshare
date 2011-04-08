package org.red5.server.webapp.screenshare;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.logging.Red5LoggerFactory;

import org.slf4j.Logger;

public class Application extends MultiThreadedApplicationAdapter implements IStreamAwareScopeHandler
{
    protected static Logger log = Red5LoggerFactory.getLogger( Application.class, "voicebridge" );

 	private String version = "0.0.0.1";
    private Map< String, IServiceCapableConnection > publishers 	= new ConcurrentHashMap<String, IServiceCapableConnection>();


    // ------------------------------------------------------------------------
    //
    // Overide
    //
    // ------------------------------------------------------------------------


    @Override
    public boolean appStart( IScope scope )
    {
		try{
			loginfo( "ScreenShare starting in scope " + scope.getName() + " " + System.getProperty( "user.dir" ) );
			loginfo(String.format("ScreenShare version %s", version));

		} catch (Exception e) {

			e.printStackTrace();
		}
        return true;
    }


    @Override
    public void appStop( IScope scope )
    {
        loginfo( "ScreenShare stopping in scope " + scope.getName() );

		IConnection conn = Red5.getConnectionLocal();
		IServiceCapableConnection service = (IServiceCapableConnection) conn;;
    }


    @Override
    public boolean appConnect( IConnection conn, Object[] params ) {

        IServiceCapableConnection service = (IServiceCapableConnection) conn;
        loginfo( "ScreenShare Client connected " + conn.getClient().getId() + " service " + service );

        return true;
    }


    @Override
    public boolean appJoin( IClient client, IScope scope ) {

        loginfo( "ScreenShare Client joined app " + client.getId() );
        IConnection conn = Red5.getConnectionLocal();
        IServiceCapableConnection service = (IServiceCapableConnection) conn;

        return true;
    }


    @Override
    public void appLeave( IClient client, IScope scope ) {

        IConnection conn = Red5.getConnectionLocal();
        loginfo( "ScreenShare Client leaving app " + client.getId() );
    }

    @Override
	public void streamBroadcastStart(IBroadcastStream stream)
	{
        loginfo( "ScreenShare streamBroadcastStart " + stream.getPublishedName() );
        IConnection conn = Red5.getConnectionLocal();
        IServiceCapableConnection service = (IServiceCapableConnection) conn;

        publishers.put(stream.getPublishedName(), service);
	}

    @Override
	public void streamBroadcastClose(IBroadcastStream stream)
	{
        loginfo( "ScreenShare streamBroadcastClose " + stream.getPublishedName() );

        publishers.remove(stream.getPublishedName());
	}


    // ------------------------------------------------------------------------
    //
    // ScreenShare
    //
    // ------------------------------------------------------------------------

	public void mousePress(String streamName, Integer button)
	{
		loginfo("ScreenShare mousePress " + button + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("mousePress", new Object[] {button});

		} else logerror("mousePress stream not found " + streamName);
	}

	public void mouseRelease(String streamName, Integer button)
	{
		loginfo("ScreenShare mouseRelease " + button + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("mouseRelease", new Object[] {button});

		} else logerror("mouseRelease stream not found " + streamName);
	}

	public void doubleClick(String streamName, Integer x, Integer y, Integer width, Integer height)
	{
		loginfo("ScreenShare doubleClick " + x + " " + y + " " + width + " " + height + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("doubleClick", new Object[] {x, y, width, height});

		} else logerror("doubleClick stream not found " + streamName);
	}

	public void keyPress(String streamName, Integer key)
	{
		loginfo("ScreenShare keyPress " + key + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("keyPress", new Object[] {key});

		} else logerror("keyPress stream not found " + streamName);
	}

	public void keyRelease(String streamName, Integer key)
	{
		loginfo("ScreenShare keyRelease " + key + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("keyRelease", new Object[] {key});

		} else logerror("keyRelease stream not found " + streamName);
	}

	public void mouseMove(String streamName, Integer x, Integer y, Integer width, Integer height)
	{
		loginfo("ScreenShare mouseMove " + x + " " + y + " " + width + " " + height + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("mouseMove", new Object[] {x, y, width, height});

		} else logerror("mouseMove stream not found " + streamName);
	}


	private IServiceCapableConnection getPublisher(String streamName)
	{
		IServiceCapableConnection service = null;

		if (publishers.containsKey(streamName))
		{
			service = publishers.get(streamName);
		}

		return service;
	}

    // ------------------------------------------------------------------------
    //
    // Logging
    //
    // ------------------------------------------------------------------------

    private void loginfo( String s ) {

        log.info( s );
        System.out.println( s );
    }

    private void logerror( String s ) {

        log.error( s );
        System.out.println( "[ERROR] " + s );
    }


}
