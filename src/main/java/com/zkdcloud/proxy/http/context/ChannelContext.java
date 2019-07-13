package com.zkdcloud.proxy.http.context;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.util.Queue;

public class ChannelContext {

    public enum PROTOCOL {
        HTTP,
        TUNNEL
    }
    public static AttributeKey<PROTOCOL> TRANSPORT_PROTOCOL = AttributeKey.valueOf("TRANSPORT_PROTOCOL");
    public static AttributeKey<InetSocketAddress> DST_ADDRESS = AttributeKey.valueOf("DST_ADDRESS");
    public static AttributeKey<Queue<HttpRequest>> REMOTE_QUERY_HTTP = AttributeKey.valueOf("REMOTE_QUERY_HTTP");
    public static AttributeKey<Channel> REMOTE_CHANNEL = AttributeKey.valueOf("REMOTE_CHANNEL");
    public static AttributeKey<Channel> CLIENT_CHANNEL = AttributeKey.valueOf("CLIENT_CHANNEL");
}
