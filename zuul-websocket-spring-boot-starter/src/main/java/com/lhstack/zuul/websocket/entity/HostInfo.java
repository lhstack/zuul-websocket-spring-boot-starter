package com.lhstack.zuul.websocket.entity;

/**
 * @author lhstack
 * @date 2021/12/27
 * @class HostInfo
 * @since 1.8
 */
public class HostInfo {

    private boolean isSecure;

    private String host;

    private int port;

    public boolean isSecure() {
        return isSecure;
    }

    public HostInfo setSecure(boolean secure) {
        isSecure = secure;
        return this;
    }

    public String getHost() {
        return host;
    }

    public HostInfo setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public HostInfo setPort(int port) {
        this.port = port;
        return this;
    }
}
