package com.lhstack.zuul.websocket.handler;

import com.lhstack.zuul.websocket.entity.HostInfo;
import com.lhstack.zuul.websocket.properties.ZuulWebSocketProxyProperties;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import org.apache.commons.lang.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * @author lhstack
 * @date 2021/12/28
 * @class NettyProxyWebSocketHandShakeHandler
 * @project zuul-websocket-spring-boot-starter
 * @since 1.8
 */
public class NettyProxyWebSocketHandShakeHandler extends AbstractWebSocketHandShakeHandler {

    private static final String HANDSHAKE_HANDLER_NAME = "handshakeHandler";

    public NettyProxyWebSocketHandShakeHandler(ZuulWebSocketProxyProperties.SslProperties sslProperties, Map<String, String> headers, HttpServletRequest request, HttpServletResponse response, HostInfo hostInfo, String requestUri) {
        super(sslProperties, headers, request, response, hostInfo, requestUri);
    }

    @Override
    public void doHandShake() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        NioEventLoopGroup eventExecutors = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap()
                .option(ChannelOption.TCP_NODELAY, true)
                .group(eventExecutors)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (hostInfo.isSecure()) {
                            pipeline.addLast("sslHandler", buildSslHandler(ch.alloc()));
                        }
                        pipeline
                                .addLast(HANDSHAKE_HANDLER_NAME, new ByteToMessageDecoder() {

                                    private byte oldByte = ' ';

                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                        ByteBuf buffer = Unpooled.buffer(128);
                                        buffer.writeBytes(String.format("GET %s HTTP/1.1\r\n", requestUri).getBytes(StandardCharsets.UTF_8));
                                        headers.forEach((k, v) -> {
                                            buffer.writeBytes(String.format("%s: %s\r\n", k, v).getBytes(StandardCharsets.UTF_8));
                                        });
                                        buffer.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
                                        ctx.writeAndFlush(buffer);
                                    }

                                    @Override
                                    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                                        in.markReaderIndex();
                                        while (in.isReadable()) {
                                            byte b = in.readByte();
                                            if (oldByte == '\n' && b == '\r') {
                                                if (in.isReadable()) {
                                                    in.readByte();
                                                    break;
                                                } else {
                                                    in.resetReaderIndex();
                                                    return;
                                                }
                                            }
                                            this.oldByte = b;
                                        }
                                        int index = in.readerIndex();
                                        in.resetReaderIndex();
                                        byte[] bytes = new byte[index];
                                        in.readBytes(bytes);
                                        String responseStr = new String(bytes, StandardCharsets.UTF_8);
                                        String[] lines = responseStr.split("\r\n");
                                        String lineOne = StringUtils.strip(lines[0]);
                                        if (StringUtils.containsIgnoreCase(lineOne, "HTTP/1.1 101")) {
                                            for (int i = 1; i < lines.length; i++) {
                                                String[] kvArray = lines[i].split(":");
                                                String key = StringUtils.strip(kvArray[0]);
                                                String value = StringUtils.strip(kvArray[1]);
                                                if (StringUtils.isNotBlank(key)) {
                                                    response.setHeader(key, value);
                                                }
                                            }
                                            response.setStatus(101);
                                        } else {
                                            eventExecutors.shutdownGracefully();
                                            ctx.close();
                                        }
                                        //握手成功
                                        ctx.pipeline().remove(HANDSHAKE_HANDLER_NAME);
                                        NettyProxyWebSocketUpgradeHandler upgrade = request.upgrade(NettyProxyWebSocketUpgradeHandler.class);
                                        upgrade.init(ctx, eventExecutors, Unpooled.copiedBuffer(in));
                                        countDownLatch.countDown();
                                    }
                                });
                    }
                });
        bootstrap.connect(this.hostInfo.getHost(), this.hostInfo.getPort());
        countDownLatch.await();

    }

    private ChannelHandler buildSslHandler(ByteBufAllocator alloc) throws Exception {
        if (Objects.nonNull(this.sslProperties)) {
            SSLContext sslContext = this.sslProperties.buildSslContext();
            SSLEngine sslEngine = sslContext.createSSLEngine();
            sslEngine.setWantClientAuth(true);
            sslEngine.setUseClientMode(true);
            sslEngine.setNeedClientAuth(false);
            return new SslHandler(sslEngine);
        }
        return SslContextBuilder.forClient()
                .clientAuth(ClientAuth.OPTIONAL)
                .trustManager(new X509TrustManager() {
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
                })
                .build()
                .newHandler(alloc);
    }
}
