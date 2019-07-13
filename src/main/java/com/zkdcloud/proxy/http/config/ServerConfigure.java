package com.zkdcloud.proxy.http.config;

/**
 * server config
 *
 * @author zk
 * @since 2019/1/30
 */
public class ServerConfigure {
    private int port;
    private int threadNumber;
    private long timeout;
    private long idleTime;
    private boolean isReconnect;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getThreadNumber() {
        return threadNumber;
    }

    public void setThreadNumber(int threadNumber) {
        this.threadNumber = threadNumber;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(long idleTime) {
        this.idleTime = idleTime;
    }

    public boolean isReconnect() {
        return isReconnect;
    }

    public void setReconnect(boolean reconnect) {
        isReconnect = reconnect;
    }
}
