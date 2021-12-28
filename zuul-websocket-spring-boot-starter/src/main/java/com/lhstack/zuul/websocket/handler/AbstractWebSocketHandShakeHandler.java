package com.lhstack.zuul.websocket.handler;

import com.lhstack.zuul.websocket.entity.HostInfo;
import com.lhstack.zuul.websocket.properties.ZuulWebSocketProxyProperties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author lhstack
 * @date 2021/12/27
 * @class WebSocketHandShakeHandler
 * @since 1.8
 */
public abstract class AbstractWebSocketHandShakeHandler {

    protected final Map<String, String> headers;
    protected final HttpServletRequest request;

    protected final HttpServletResponse response;

    protected final HostInfo hostInfo;

    protected final String requestUri;

    protected final ZuulWebSocketProxyProperties.SslProperties sslProperties;

    public AbstractWebSocketHandShakeHandler(ZuulWebSocketProxyProperties.SslProperties sslProperties, Map<String, String> headers, HttpServletRequest request, HttpServletResponse response, HostInfo hostInfo, String requestUri) {
        this.request = request;
        this.response = response;
        this.hostInfo = hostInfo;
        this.requestUri = requestUri;
        this.headers = headers;
        this.sslProperties = sslProperties;
    }

    /**
     * 握手
     *
     * @throws Exception
     */
    public abstract void doHandShake() throws Exception;

}
