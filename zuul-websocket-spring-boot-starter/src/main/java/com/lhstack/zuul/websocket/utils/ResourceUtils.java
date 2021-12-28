package com.lhstack.zuul.websocket.utils;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author lhstack
 * @date 2021/12/28
 * @class ResourceUtils
 * @since 1.8
 */
public class ResourceUtils {
    private static final PathMatchingResourcePatternResolver PATH_MATCHING_RESOURCE_PATTERN_RESOLVER = new PathMatchingResourcePatternResolver();

    public static InputStream loadResource(String name) throws IOException {
        return PATH_MATCHING_RESOURCE_PATTERN_RESOLVER.getResource(name).getInputStream();
    }
}
