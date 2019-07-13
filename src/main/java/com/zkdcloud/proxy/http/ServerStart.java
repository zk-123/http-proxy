package com.zkdcloud.proxy.http;

import com.zkdcloud.proxy.http.config.ServerConfigure;
import com.zkdcloud.proxy.http.handler.client.ExceptionDuplexHandler;
import com.zkdcloud.proxy.http.handler.client.JudgeTypeInboundHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/**
 * http start
 *
 * @author zk
 * @since 2018/10/9
 */
public class ServerStart {
    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(ServerStart.class);
    private static NioEventLoopGroup bossGroup;
    private static NioEventLoopGroup workGroup;

    public static void main(String[] args) throws InterruptedException {
        // init args
        initCliArgs(args);
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        bossGroup = new NioEventLoopGroup((short) Math.min(Runtime.getRuntime().availableProcessors() + 1, 32), new DefaultThreadFactory("boss-threads"));
        workGroup = new NioEventLoopGroup(serverConfigure.getThreadNumber(),
                new DefaultThreadFactory("workers-threads"));

        serverBootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast("idle", new IdleStateHandler(0, 0, serverConfigure.getIdleTime(), TimeUnit.SECONDS))
                                .addLast("http-decoder", new HttpRequestDecoder())
                                .addLast("http-encoder", new HttpResponseEncoder())
                                .addLast("connect-judge", new JudgeTypeInboundHandler())
                                .addLast("exception", new ExceptionDuplexHandler());
                    }
                });
        int port = serverConfigure.getPort();
        logger.info("http server start at : : {}", port);

        serverBootstrap.bind(port).sync().channel().closeFuture().sync();
    }

    public static ServerConfigure serverConfigure = new ServerConfigure();
    private static Options OPTIONS = new Options();
    private static CommandLine commandLine;
    private static String HELP_STRING = null;

    /**
     * init args
     *
     * @param args args
     */
    private static void initCliArgs(String[] args) {
        // validate args
        {
            CommandLineParser commandLineParser = new DefaultParser();
            // help
            OPTIONS.addOption("h", "help", false, "usage help");
            // port
            OPTIONS.addOption(Option.builder("p").hasArg().argName("port").type(Integer.TYPE).desc("the port of server startup").build());
            // number fo workers thread
            OPTIONS.addOption(Option.builder("n").hasArg().argName("thread numbers").type(Integer.TYPE).build());
            // remote connect timeout
            OPTIONS.addOption(Option.builder("t").hasArg().argName("connect remote timeout (mills)").type(Long.TYPE).build());
            // client idle seconds
            OPTIONS.addOption(Option.builder("i").hasArg().argName("idle time (second)").type(Long.TYPE).build());
            // reconnect
            OPTIONS.addOption(Option.builder("r").hasArg().argName("is reconnect (true/false)").desc("usually reconnect is false").type(Boolean.TYPE).build());
            try {
                commandLine = commandLineParser.parse(OPTIONS, args);
            } catch (ParseException e) {
                logger.error("{}\n{}", e.getMessage(), getHelpString());
                System.exit(0);
            }
        }

        // init serverConfigure
        {
            if (commandLine.hasOption("h")) {
                logger.info("\n" + getHelpString());
                System.exit(1);
            }
            // server port
            String portOptionValue = commandLine.getOptionValue("p");
            int port = portOptionValue == null || "".equals(portOptionValue) ? 1081 : Integer.parseInt(portOptionValue);
            serverConfigure.setPort(port);

            // thread numbers
            String numberWorksOptionValue = commandLine.getOptionValue("n");
            int numberWorks = numberWorksOptionValue == null || "".equals(numberWorksOptionValue) ?
                    Math.min(Runtime.getRuntime().availableProcessors() + 1, 32) : Integer.parseInt(numberWorksOptionValue);
            serverConfigure.setThreadNumber(numberWorks);

            // timeout
            String timeoutOptionValue = commandLine.getOptionValue("t");
            long timeout = timeoutOptionValue == null || "".equals(timeoutOptionValue) ? 5000 : Integer.parseInt(timeoutOptionValue);
            serverConfigure.setTimeout(timeout);

            // client idle time(second)
            String idleTimeOptionValue = commandLine.getOptionValue("i");
            long idleTime = idleTimeOptionValue == null || "".equals(idleTimeOptionValue) ? 60 * 10 * 10 : Integer.parseInt(idleTimeOptionValue);
            serverConfigure.setIdleTime(idleTime);

            // reconnect
            String isReconnectOptionValue = commandLine.getOptionValue("r");
            boolean isReconnect = isReconnectOptionValue == null || "".equals(isReconnectOptionValue) ? true : Boolean.valueOf(isReconnectOptionValue);
            serverConfigure.setReconnect(isReconnect);
        }

    }

    /**
     * get string of help usage
     *
     * @return help string
     */
    private static String getHelpString() {
        if (HELP_STRING == null) {
            HelpFormatter helpFormatter = new HelpFormatter();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);
            helpFormatter.printHelp(printWriter, HelpFormatter.DEFAULT_WIDTH, "http proxy", null,
                    OPTIONS, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null);
            printWriter.flush();
            HELP_STRING = new String(byteArrayOutputStream.toByteArray());
            printWriter.close();
        }
        return HELP_STRING;
    }
}
