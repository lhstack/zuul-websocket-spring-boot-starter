package com.lhstack.zuul.websocket.utils;

import org.apache.commons.lang.StringUtils;

import java.net.URL;

/**
 * @author lhstack
 * @date 2021/12/27
 * @class HostUtils
 * @since 1.8
 */
public class HostUtils {
    public static String touchHost(URL url) {
        return url.getHost();
    }

    public static boolean isSecure(URL url) {
        return StringUtils.equalsIgnoreCase(url.getProtocol(), "https");
    }

    public static int touchPort(URL url) {
        switch (url.getProtocol()) {
            case "http": {
                return url.getPort() == -1 ? 80 : url.getPort();
            }
            case "https": {
                return url.getPort() == -1 ? 443 : url.getPort();
            }
            default: {
                return -1;
            }
        }
    }
}
