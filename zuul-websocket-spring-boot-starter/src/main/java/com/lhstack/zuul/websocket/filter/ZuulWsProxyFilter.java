package com.lhstack.zuul.websocket.filter;

import com.lhstack.zuul.websocket.entity.HostInfo;
import com.lhstack.zuul.websocket.handler.AbstractWebSocketHandShakeHandler;
import com.lhstack.zuul.websocket.properties.ZuulWebSocketProxyProperties;
import com.lhstack.zuul.websocket.utils.HostUtils;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;

/**
 * @author lhstack
 * @date 2021/12/27
 * @class ZuulWsProxyFilter
 * @since 1.8
 */

public class ZuulWsProxyFilter extends ZuulFilter {

    @Autowired(required = false)
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private ZuulWebSocketProxyProperties zuulWebSocketProxyProperties;

    @Override
    public String filterType() {
        return FilterConstants.ROUTE_TYPE;
    }

    @Override
    public int filterOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = currentContext.getRequest();
        return StringUtils.equals(request.getHeader("Upgrade"), "websocket") &&
                StringUtils.equals(request.getHeader("Connection"), "Upgrade") &&
                StringUtils.isNotBlank(request.getHeader("Sec-WebSocket-Key"));
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = currentContext.getRequest();
        //设置代理请求头
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName.toLowerCase(Locale.ROOT), request.getHeader(headerName));
        }
        headers.putAll(currentContext.getZuulRequestHeaders());
        HostInfo hostInfo = new HostInfo();
        //获取目标host
        URL routeHost = currentContext.getRouteHost();
        if (Objects.isNull(routeHost)) {
            String serviceId = (String) currentContext.get(FilterConstants.SERVICE_ID_KEY);
            ServiceInstance serviceInstance = loadBalancerClient.choose(serviceId);
            hostInfo.setSecure(serviceInstance.isSecure())
                    .setPort(serviceInstance.getPort())
                    .setHost(serviceInstance.getHost());
        } else {
            hostInfo.setHost(HostUtils.touchHost(routeHost))
                    .setPort(HostUtils.touchPort(routeHost))
                    .setSecure(HostUtils.isSecure(routeHost));
        }
        String proxyId = (String) currentContext.get(FilterConstants.PROXY_KEY);
        headers.put("host", String.format("%s:%s", hostInfo.getHost(), hostInfo.getPort()));
        //获取请求路径
        String requestUri = (String) currentContext.get(FilterConstants.REQUEST_URI_KEY);
        ZuulWebSocketProxyProperties.SslProperties sslProperties = null;
        if (zuulWebSocketProxyProperties.getSsl().containsKey(proxyId)) {
            sslProperties = zuulWebSocketProxyProperties.getSsl().get(proxyId);
        }
        try {

            Constructor<? extends AbstractWebSocketHandShakeHandler> constructor = this.zuulWebSocketProxyProperties.getHandShakeHandlerClass().getConstructor(ZuulWebSocketProxyProperties.SslProperties.class, Map.class, HttpServletRequest.class, HttpServletResponse.class, HostInfo.class, String.class);
            AbstractWebSocketHandShakeHandler handShakeHandler = constructor.newInstance(sslProperties, headers, request, currentContext.getResponse(), hostInfo, requestUri);
            currentContext.setSendZuulResponse(false);
            currentContext.setResponseStatusCode(101);
            handShakeHandler.doHandShake();
        } catch (Exception e) {
            e.printStackTrace();
            currentContext.setResponseStatusCode(500);
            currentContext.setSendZuulResponse(true);
            currentContext.setThrowable(e);
        }

        return null;
    }
}
