package com.zkdcloud.proxy.http.handler.remote;

import com.zkdcloud.proxy.http.context.ChannelContext;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * remote flow exchange
 *
 * @author zk
 * @since 2019/7/9
 */
@ChannelHandler.Sharable
public class DefaultRemoteFlowTransfer extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.channel().attr(ChannelContext.CLIENT_CHANNEL).get().writeAndFlush(msg);
    }
}
