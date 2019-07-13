package com.zkdcloud.proxy.http.handler.client;

import com.zkdcloud.proxy.http.context.ChannelContext;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * default data exchange
 *
 * @author zk
 * @since 2019/7/9
 */
@ChannelHandler.Sharable
public class DefaultClientFlowTransfer extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.channel().attr(ChannelContext.REMOTE_CHANNEL).get().writeAndFlush(msg);
    }
}
