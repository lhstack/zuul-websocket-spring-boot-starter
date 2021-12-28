package com.lhstack.zuul.websocket.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

/**
 * @author lhstack
 * @date 2021/12/27
 * @class ExampleApplication
 * @since 1.8
 */
@SpringBootApplication
@EnableZuulProxy
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
