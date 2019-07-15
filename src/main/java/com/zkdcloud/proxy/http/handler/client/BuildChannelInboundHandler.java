package com.zkdcloud.proxy.http.handler.client;

import com.zkdcloud.proxy.http.context.ChannelContext;
import com.zkdcloud.proxy.http.handler.remote.DefaultRemoteFlowTransfer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import static com.zkdcloud.proxy.http.ServerStart.serverConfigure;
import static com.zkdcloud.proxy.http.context.ChannelContext.TRANSPORT_PROTOCOL;

/**
 * connect or reconnect remote channel and finish handshake
 *
 * @author zk
 * @since 2019/7/9
 */
public class BuildChannelInboundHandler extends ChannelInboundHandlerAdapter {
    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(BuildChannelInboundHandler.class);
    /**
     * client channel
     */
    private Channel clientChannel;
    /**
     * remote channel
     */
    private Channel remoteChannel;
    /**
     * connect state
     */
    private STATE connectState = STATE.UNCONNECT;
    /**
     * remote bootstrap
     */
    private Bootstrap remoteBootstrap;

    private List<Object> flocks = new LinkedList<>();

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        setClientChannel(ctx.channel());
        //build connect
        final InetSocketAddress dstAddress = clientChannel.attr(ChannelContext.DST_ADDRESS).get();

        final ChannelContext.PROTOCOL protocol = clientChannel.attr(TRANSPORT_PROTOCOL).get();
        final ChannelFuture channelFuture;
        switch (protocol) {
            case HTTP:
                channelFuture = connectRemote(dstAddress, new HttpRequestEncoder(), new DefaultRemoteFlowTransfer());
                channelFuture.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {
                            setRemoteChannel(future.channel());
                            logger.info("[{}-{}] type: http, channel connect [{}:{}] success.", clientChannel.id(), remoteChannel.id(), dstAddress.getHostName(), dstAddress.getPort());

                            clientChannel.attr(ChannelContext.REMOTE_CHANNEL).setIfAbsent(remoteChannel);
                            remoteChannel.attr(ChannelContext.CLIENT_CHANNEL).setIfAbsent(clientChannel);
                            clientChannel.pipeline().remove(HttpResponseEncoder.class);
                            clientChannel.pipeline().addLast(new DefaultClientFlowTransfer());//dataTransfer
                            connectState = STATE.FINISHED;//connect success
                            castFlocks();//fire next read
                        } else {
                            logger.error("[{}-]connect [{}:{}] fail {}.", clientChannel.id(), dstAddress.getHostName(), dstAddress.getPort(), future.cause().getMessage());
                            closeChannel();
                        }
                    }
                });
                break;
            case TUNNEL:
                channelFuture = connectRemote(dstAddress, new DefaultRemoteFlowTransfer());
                channelFuture.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {
                            setRemoteChannel(future.channel());
                            logger.info("[{}-{}] type: tunnel, channel connect [{}:{}] success.", clientChannel.id(), remoteChannel.id(), dstAddress.getHostName(), dstAddress.getPort());

                            clientChannel.attr(ChannelContext.REMOTE_CHANNEL).setIfAbsent(remoteChannel);
                            remoteChannel.attr(ChannelContext.CLIENT_CHANNEL).setIfAbsent(clientChannel);
                            clientChannel.pipeline().addBefore("exception", "client-flower-transfer", new DefaultClientFlowTransfer());//dataTransfer

                            connectState = STATE.FINISHED;//connect success

                            //response ack
                            clientChannel.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
                                    .addListener(new ChannelFutureListener() {
                                        public void operationComplete(ChannelFuture future) {
                                            clientChannel.pipeline().remove(HttpRequestDecoder.class);
                                            clientChannel.pipeline().remove(HttpResponseEncoder.class);
                                        }
                                    });

                        } else {
                            logger.error("[{}-] connect [{}:{}] fail {}.", clientChannel.id(), dstAddress.getHostName(), dstAddress.getPort(), future.cause().getMessage());
                            closeChannel();
                        }
                    }
                });
                break;
            default:
                throw new IllegalArgumentException("protocol type is undefined " + protocol);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        switch (connectState) {
            case UNCONNECT:
                flocks.add(msg);
                break;
            case FINISHED:
                ctx.fireChannelRead(msg);
                break;
            default:
                super.channelRead(ctx, msg);
        }
    }

    /**
     * connect remote server
     *
     * @param dstAddress      remote server address
     * @param channelHandlers handlers
     * @return future
     */
    private ChannelFuture connectRemote(InetSocketAddress dstAddress, final ChannelHandler... channelHandlers) {
        if (remoteBootstrap == null) {
            remoteBootstrap = new Bootstrap();
        }
        return remoteBootstrap.group(clientChannel.eventLoop())
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
    }


    /**
     * cast flocks
     */
    private void castFlocks() {
        if (!flocks.isEmpty()) {
            ChannelHandlerContext curHandlerContext = clientChannel.pipeline().context(this);
            for (Object flock : flocks) {
                curHandlerContext.fireChannelRead(flock);
            }
            flocks.clear();
        }
    }

    /**
     * close both channel
     */
    private void closeChannel() {
        if (!flocks.isEmpty()) {
            for (Object flock : flocks) {
                ReferenceCountUtil.release(flock);
            }
            flocks.clear();
        }

        if (clientChannel != null) {
            clientChannel.close();
        }
        if (remoteChannel != null) {
            remoteChannel.close();
        }
    }

    /**
     * set clientChannel
     *
     * @param clientChannel clientChanel
     */
    private void setClientChannel(Channel clientChannel) {
        this.clientChannel = clientChannel;
    }

    /**
     * set remote Channel
     *
     * @param remoteChannel remoteChannel
     */
    private void setRemoteChannel(Channel remoteChannel) {
        this.remoteChannel = remoteChannel;
    }

    private enum STATE {
        /**
         * un connect
         */
        UNCONNECT,
        /**
         * connected
         */
        FINISHED
    }
}
