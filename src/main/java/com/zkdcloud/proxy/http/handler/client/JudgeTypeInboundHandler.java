package com.zkdcloud.proxy.http.handler.client;

import com.zkdcloud.proxy.http.context.ChannelContext;
import com.zkdcloud.proxy.http.handler.client.BuildChannelInboundHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;

import static com.zkdcloud.proxy.http.context.ChannelContext.PROTOCOL.*;

/**
 * judge connect type
 *
 * @author zk
 * @since 2018/10/21
 */
public class JudgeTypeInboundHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest httpMsg = (HttpRequest) msg;

            //set http proxy request type
            ChannelContext.PROTOCOL protocol = httpMsg.method() == HttpMethod.CONNECT ? TUNNEL : HTTP;
            ctx.channel().attr(ChannelContext.TRANSPORT_PROTOCOL).setIfAbsent(protocol);

            //set dstAddress
            InetSocketAddress dstAddress = getDstAddress(httpMsg);
            ctx.channel().attr(ChannelContext.DST_ADDRESS).setIfAbsent(dstAddress);

            ctx.pipeline().addAfter("connect-judge", "build-connect", new BuildChannelInboundHandler());

            if(protocol == HTTP){
                ctx.fireChannelRead(msg);
            } else {
                ReferenceCountUtil.release(msg);
            }
            ctx.pipeline().remove(this);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * get address form httpMsg
     *
     * @param httpMsg httpMsg
     * @return address
     */
    private InetSocketAddress getDstAddress(HttpRequest httpMsg) {
        InetSocketAddress result = null;

        String uri = httpMsg.uri();
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            try {
                URL url = new URL(uri);
                result = new InetSocketAddress(url.getHost(), url.getPort() == -1 ? 80 : url.getPort());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(httpMsg.uri() + " is getDstAddress fail");
            }
        } else {
            String host = uri.contains(":") ? uri.substring(0, uri.lastIndexOf(":")) : uri;
            int port = uri.contains(":") ? Integer.valueOf(uri.substring(uri.lastIndexOf(":") + 1)) : 80;
            return new InetSocketAddress(host, port);
        }

        return result;
    }
}
