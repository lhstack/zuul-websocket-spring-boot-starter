package com.lhstack.zuul.websocket.properties;

import com.lhstack.zuul.websocket.handler.AbstractWebSocketHandShakeHandler;
import com.lhstack.zuul.websocket.handler.NettyProxyWebSocketHandShakeHandler;
import com.lhstack.zuul.websocket.utils.ResourceUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.web.servlet.resource.PathResourceResolver;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;

/**
 * @author lhstack
 * @date 2021/12/28
 * @class WebSocketProxyProperties
 * @since 1.8
 */
public class ZuulWebSocketProxyProperties {

    /**
     * 代理处理器
     */
    private Class<? extends AbstractWebSocketHandShakeHandler> handShakeHandlerClass = NettyProxyWebSocketHandShakeHandler.class;

    /**
     * key为route id
     * value为sslProperties配置
     */
    private Map<String, SslProperties> ssl = Collections.emptyMap();

    public Class<? extends AbstractWebSocketHandShakeHandler> getHandShakeHandlerClass() {
        return handShakeHandlerClass;
    }

    public ZuulWebSocketProxyProperties setHandShakeHandlerClass(Class<? extends AbstractWebSocketHandShakeHandler> handShakeHandlerClass) {
        this.handShakeHandlerClass = handShakeHandlerClass;
        return this;
    }

    public Map<String, SslProperties> getSsl() {
        return ssl;
    }

    public ZuulWebSocketProxyProperties setSsl(Map<String, SslProperties> ssl) {
        this.ssl = ssl;
        return this;
    }

    public static class SslProperties {

        /**
         * key store type
         */
        private String keyStoreType = "PKCS12";

        /**
         * key store 内容
         */
        private String keyStorePath;

        /**
         * key store password
         */
        private String keyStorePass;

        /**
         * key pass
         */
        private String keyPass;

        /**
         * trust store type
         */
        private String trustStoreType = "PKCS12";

        /**
         * trust 内容
         */
        private String trustStorePath;

        /**
         * trust pass
         */
        private String trustPass;

        public String getKeyStoreType() {
            return keyStoreType;
        }

        public SslProperties setKeyStoreType(String keyStoreType) {
            this.keyStoreType = keyStoreType;
            return this;
        }

        public String getKeyStorePath() {
            return keyStorePath;
        }

        public SslProperties setKeyStorePath(String keyStorePath) {
            this.keyStorePath = keyStorePath;
            return this;
        }

        public String getKeyStorePass() {
            return keyStorePass;
        }

        public SslProperties setKeyStorePass(String keyStorePass) {
            this.keyStorePass = keyStorePass;
            return this;
        }

        public String getKeyPass() {
            return keyPass;
        }

        public SslProperties setKeyPass(String keyPass) {
            this.keyPass = keyPass;
            return this;
        }

        public String getTrustStoreType() {
            return trustStoreType;
        }

        public SslProperties setTrustStoreType(String trustStoreType) {
            this.trustStoreType = trustStoreType;
            return this;
        }

        public String getTrustStorePath() {
            return trustStorePath;
        }

        public SslProperties setTrustStorePath(String trustStorePath) {
            this.trustStorePath = trustStorePath;
            return this;
        }

        public String getTrustPass() {
            return trustPass;
        }

        public SslProperties setTrustPass(String trustPass) {
            this.trustPass = trustPass;
            return this;
        }

        public SSLContext buildSslContext() throws Exception {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManager[] keyManagers = null;
            TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};
            if (StringUtils.isNotBlank(this.keyStorePath)) {
                KeyStore keyStore = KeyStore.getInstance(this.keyStoreType);
                InputStream in = ResourceUtils.loadResource(this.keyStorePath);
                keyStore.load(in, StringUtils.isNotBlank(this.keyStorePass) ? this.keyStorePass.toCharArray() : null);
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, StringUtils.isNotBlank(this.keyPass) ? this.keyPass.toCharArray() : null);
                keyManagers = keyManagerFactory.getKeyManagers();
                in.close();
            }
            if (StringUtils.isNotBlank(this.trustStorePath)) {
                KeyStore keyStore = KeyStore.getInstance(this.trustStoreType);
                InputStream in = ResourceUtils.loadResource(this.trustStorePath);
                keyStore.load(in, StringUtils.isNotBlank(this.trustPass) ? this.trustPass.toCharArray() : null);
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);
                trustManagers = trustManagerFactory.getTrustManagers();
                in.close();
            }
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
            return sslContext;
        }
    }

    @Override
    public String toString() {
        return "WebSocketProxyProperties{" +
                "handShakeHandlerClass=" + handShakeHandlerClass +
                ", ssl=" + ssl +
                '}';
    }
}
