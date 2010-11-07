package org.redfire.screen;

import com.flazr.rtmp.RtmpHandshake;
import com.flazr.rtmp.RtmpPublisher;
import com.flazr.rtmp.client.ClientOptions;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScreenClientHandshakeHandler extends FrameDecoder implements ChannelDownstreamHandler {

   private static final Logger logger = LoggerFactory.getLogger(ScreenClientHandshakeHandler.class);

    private boolean rtmpe;
    private final RtmpHandshake handshake;
    private boolean handshakeDone;

    public ScreenClientHandshakeHandler(ClientOptions options) {
        handshake = new RtmpHandshake(options);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        logger.info("connected, starting handshake");
        Channels.write(ctx, e.getFuture(), handshake.encodeClient0());
        Channels.write(ctx, e.getFuture(), handshake.encodeClient1());
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer in) {
        if(in.readableBytes() < 1 + RtmpHandshake.HANDSHAKE_SIZE * 2) {
            return null;
        }
        handshake.decodeServerAll(in);
        Channels.write(ctx, Channels.succeededFuture(channel), handshake.encodeClient2());
        handshakeDone = true;
        rtmpe = handshake.isRtmpe(); // rare chance server refused rtmpe
        if(handshake.getSwfvBytes() != null) {
            ScreenClientHandler clientHandler = channel.getPipeline().get(ScreenClientHandler.class);
            clientHandler.setSwfvBytes(handshake.getSwfvBytes());
        }
        if(!rtmpe) {
            channel.getPipeline().remove(this);
        }
        Channels.fireChannelConnected(ctx, channel.getRemoteAddress());
        return in;
    }

    @Override
    public void handleUpstream(final ChannelHandlerContext ctx, final ChannelEvent ce) throws Exception {
        if (!handshakeDone || !rtmpe || !(ce instanceof MessageEvent)) {
            super.handleUpstream(ctx, ce);
            return;
        }
        final MessageEvent me = (MessageEvent) ce;
        if(me.getMessage() instanceof RtmpPublisher.Event) {
            super.handleUpstream(ctx, ce);
            return;
        }
        final ChannelBuffer in = (ChannelBuffer) ((MessageEvent) ce).getMessage();
        handshake.cipherUpdateIn(in);
        Channels.fireMessageReceived(ctx, in);
    }

    @Override
    public void handleDownstream(final ChannelHandlerContext ctx, final ChannelEvent ce) {
        if (!handshakeDone || !rtmpe || !(ce instanceof MessageEvent)) {
            ctx.sendDownstream(ce);
            return;
        }
        final ChannelBuffer in = (ChannelBuffer) ((MessageEvent) ce).getMessage();
        handshake.cipherUpdateOut(in);
        ctx.sendDownstream(ce);
    }

}
