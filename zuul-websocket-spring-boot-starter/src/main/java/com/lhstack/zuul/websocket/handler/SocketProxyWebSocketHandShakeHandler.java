package com.lhstack.zuul.websocket.handler;

import com.lhstack.zuul.websocket.entity.HostInfo;
import com.lhstack.zuul.websocket.properties.ZuulWebSocketProxyProperties;
import com.lhstack.zuul.websocket.utils.SslUtils;
import org.apache.commons.lang.StringUtils;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * @author lhstack
 * @date 2021/12/27
 * @class SocketProxyWebSocketHandShakeHandler
 * @since 1.8
 */
public class SocketProxyWebSocketHandShakeHandler extends AbstractWebSocketHandShakeHandler {

    public SocketProxyWebSocketHandShakeHandler(ZuulWebSocketProxyProperties.SslProperties sslProperties, Map<String, String> headers, HttpServletRequest request, HttpServletResponse response, HostInfo hostInfo, String requestUri) {
        super(sslProperties, headers, request, response, hostInfo, requestUri);
    }

    @Override
    public void doHandShake() throws Exception {
        Socket socket = null;
        if (this.hostInfo.isSecure()) {
            socket = buildSslSocket();
        } else {
            socket = new Socket();
        }
        socket.setTcpNoDelay(true);
        socket.setSendBufferSize(128);
        socket.setReceiveBufferSize(512);
        socket.connect(new InetSocketAddress(this.hostInfo.getHost(), this.hostInfo.getPort()));
        Socket finalSocket = socket;
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CompletableFuture.runAsync(() -> {
            try {
                InputStream in = finalSocket.getInputStream();
                byte oldByte = ' ';
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                while (true) {
                    byte bye = (byte) in.read();
                    if (oldByte == '\n' && bye == '\r') {
                        in.read();
                        String responseStr = bo.toString(StandardCharsets.UTF_8);
                        String[] lines = responseStr.split("\r\n");
                        String lineOne = StringUtils.strip(lines[0]);
                        if (StringUtils.containsIgnoreCase(lineOne, "HTTP/1.1 101")) {
                            for (int i = 1; i < lines.length; i++) {
                                String[] kvArray = lines[i].split(":");
                                String key = StringUtils.strip(kvArray[0]);
                                String value = StringUtils.strip(kvArray[1]);
                                response.setHeader(key, value);
                                response.setStatus(101);
                            }
                            SocketProxyWebSocketUpgradeHandler upgrade = request.upgrade(SocketProxyWebSocketUpgradeHandler.class);
                            upgrade.setSocket(finalSocket);
                        } else {
                            finalSocket.close();
                        }
                        countDownLatch.countDown();
                        return;
                    }
                    oldByte = bye;
                    bo.write(bye);
                }
            } catch (Exception e) {
                try {
                    e.printStackTrace();
                    finalSocket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        OutputStream out = socket.getOutputStream();
        StringBuilder handshakeData = new StringBuilder();
        handshakeData.append(String.format("GET %s HTTP/1.1\r\n", requestUri));
        this.headers.forEach((k, v) -> {
            handshakeData.append(String.format("%s: %s\r\n", k, v));
        });
        handshakeData.append("\r\n");
        out.write(handshakeData.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
        countDownLatch.await();
    }

    private Socket buildSslSocket() throws Exception {
        if (Objects.nonNull(this.sslProperties)) {
            SSLContext sslContext = this.sslProperties.buildSslContext();
            return sslContext.getSocketFactory().createSocket();
        }
        return SslUtils.getDefaultSslContext().getSocketFactory().createSocket();
    }
}
