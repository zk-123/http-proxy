package com.zkdcloud.proxy.http.handler.remote;

import com.zkdcloud.proxy.http.ServerStart;
import com.zkdcloud.proxy.http.context.ChannelContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.zkdcloud.proxy.http.ServerStart.serverConfigure;

/**
 * remote monitor
 *
 * @author zk
 * @since 2019/7/13
 */
@ChannelHandler.Sharable
public class RemoteMonitorDuplexHandler extends ChannelDuplexHandler {
    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(RemoteMonitorDuplexHandler.class);

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel client = ctx.channel().attr(ChannelContext.CLIENT_CHANNEL).get();
        if (client != null && !client.isActive()) {
            if(serverConfigure.isReconnect()){
                reconnect(ctx);
            }
        }
        ctx.fireChannelInactive();
    }

    /**
     * usually,you don't need to reconnect
     *
     * @param ctx ctx
     */
    private void reconnect(final ChannelHandlerContext ctx) {
        final Channel clientChannel = ctx.channel().attr(ChannelContext.CLIENT_CHANNEL).get();
        SocketAddress dstAddress = ctx.channel().remoteAddress();

        //connect handlers
        Iterator<Map.Entry<String, ChannelHandler>> iterator = ctx.pipeline().iterator();
        final List<ChannelHandler> channelHandlers = new LinkedList<>();
        while (iterator.hasNext()) {
            ChannelHandler oneHandler = iterator.next().getValue();
            if (iterator.hasNext()) {
                channelHandlers.add(oneHandler);
            }
        }

        Bootstrap remoteBootstrap = new Bootstrap();
        ChannelFuture channelFuture = remoteBootstrap.group(clientChannel.eventLoop())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) serverConfigure.getTimeout())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    protected void initChannel(Channel ch) {
                        for (ChannelHandler channelHandler : channelHandlers) {
                            ch.pipeline().addLast(channelHandler);
                        }
                    }
                }).connect(dstAddress);
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    clientChannel.attr(ChannelContext.REMOTE_CHANNEL).setIfAbsent(future.channel());
                    logger.info("[{}-{}] reconnect {}:{} success.", clientChannel.id(), ctx.channel().id(), ((InetSocketAddress) ctx.channel().remoteAddress()).getHostName(), ((InetSocketAddress) ctx.channel().remoteAddress()).getPort());
                } else {
                    clientChannel.close();
                    future.channel().close();
                    logger.info("[{}-{}] reconnect {}:{} fail.", clientChannel.id(), ctx.channel().id(), ((InetSocketAddress) ctx.channel().remoteAddress()).getHostName(), ((InetSocketAddress) ctx.channel().remoteAddress()).getPort());
                }
            }
        });
    }
}
