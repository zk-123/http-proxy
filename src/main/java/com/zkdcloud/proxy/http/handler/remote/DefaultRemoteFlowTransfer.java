package com.zkdcloud.proxy.http.handler.remote;

import com.zkdcloud.proxy.http.context.ChannelContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * remote flow exchange
 *
 * @author zk
 * @since 2019/7/9
 */
@ChannelHandler.Sharable
public class DefaultRemoteFlowTransfer extends ChannelInboundHandlerAdapter {
    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(DefaultRemoteFlowTransfer.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.channel().attr(ChannelContext.CLIENT_CHANNEL).get().writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel clientChannel = ctx.channel().attr(ChannelContext.CLIENT_CHANNEL).get();
        logger.info("[{}-{}] remote channel inactive.", clientChannel.id().asShortText(), ctx.channel().id().asShortText());
        ctx.channel().close();
        clientChannel.close();
    }
}
