package com.lhstack.zuul.websocket.config;

import com.lhstack.zuul.websocket.filter.ZuulWsProxyFilter;
import com.lhstack.zuul.websocket.properties.ZuulWebSocketProxyProperties;
import com.netflix.zuul.ZuulFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;

/**
 * @author lhstack
 * @date 2021/12/27
 * @class ZuulWsProxyFilterConfiguration
 * @since 1.8
 */
@ConditionalOnClass(ZuulFilter.class)
@ConditionalOnBean(annotation = EnableZuulProxy.class)
public class ZuulWsProxyFilterConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "zuul.websocket")
    public ZuulWebSocketProxyProperties webSocketProxyProperties(){
        return new ZuulWebSocketProxyProperties();
    }

    @Bean
    public ZuulWsProxyFilter zuulWsProxyFilter() {
        return new ZuulWsProxyFilter();
    }
}
