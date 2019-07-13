package com.zkdcloud.proxy.http.handler.client;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * channel of tail
 *
 * @author zk
 * @since 2019/7/9
 */
public class ExceptionDuplexHandler extends ChannelDuplexHandler {
    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(ExceptionDuplexHandler.class);
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        logger.error("{} happen error, will be close : {}", ctx.channel().id(), cause.getMessage());
        cause.printStackTrace();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
            logger.warn("{} idle timeout, will be close", ctx.channel().id());
        }
    }
}
