package org.redfire.screen;

import com.flazr.rtmp.*;
import com.flazr.rtmp.client.ClientOptions;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

public class ScreenClientPipelineFactory implements ChannelPipelineFactory {

    private final ClientOptions options;
    private final ScreenShare screenShare;

    public ScreenClientPipelineFactory(final ClientOptions options, final ScreenShare screenShare) {
        this.options = options;
        this.screenShare = screenShare;
    }

    @Override
    public ChannelPipeline getPipeline() {
        final ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("handshaker", new ScreenClientHandshakeHandler(options));
        pipeline.addLast("decoder", new RtmpDecoder());
        pipeline.addLast("encoder", new RtmpEncoder());
//        if(options.getLoad() == 1) {
//            pipeline.addLast("executor", new ExecutionHandler(
//                    new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576)));
//        }
        pipeline.addLast("handler", new ScreenClientHandler(options, screenShare));
        return pipeline;
    }

}
