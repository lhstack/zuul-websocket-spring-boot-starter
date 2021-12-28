package com.lhstack.zuul.websocket.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ReferenceCountUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * @author lhstack
 * @date 2021/12/28
 * @class NettyProxyWebSocketUpgradeHandler
 * @project zuul-websocket-spring-boot-starter
 * @since 1.8
 */
public class NettyProxyWebSocketUpgradeHandler implements HttpUpgradeHandler {

    private ChannelHandlerContext ctx;
    private WebConnection connection;
    private NioEventLoopGroup eventLoopGroup;
    private ProxyHandler proxyHandler;

    @Override
    public void init(WebConnection connection) {
        this.connection = connection;
        try {
            ServletInputStream in = this.connection.getInputStream();
            this.proxyHandler.setOut(connection.getOutputStream());
            in.setReadListener(new BufferReadListener(in, this));
        } catch (Exception e) {
            destroy();
        }
    }

    @Override
    public void destroy() {
        try {
            //触发浏览器关闭事件
            connection.getOutputStream().write("hello world".getBytes(StandardCharsets.UTF_8));
            connection.getOutputStream().flush();
        } catch (Exception ignore) {

        }
        try {
            //关闭链接
            connection.close();
            //关闭客户端
            ctx.close();
            //关闭线程池
            eventLoopGroup.shutdownGracefully();
        } catch (Exception ignore) {

        }
    }

    public void init(ChannelHandlerContext ctx, NioEventLoopGroup eventLoopGroup, ByteBuf byteBuf) {
        this.ctx = ctx;
        this.eventLoopGroup = eventLoopGroup;
        this.proxyHandler = new ProxyHandler(this, byteBuf);
        this.ctx.pipeline().addLast("proxyHandler", this.proxyHandler);
    }

    class ProxyHandler extends ByteToMessageDecoder {

        private ByteBuf byteBuf;
        private ServletOutputStream out;
        private final NettyProxyWebSocketUpgradeHandler handler;

        public ProxyHandler(NettyProxyWebSocketUpgradeHandler handler, ByteBuf byteBuf) {
            this.handler = handler;
            this.byteBuf = byteBuf;
        }

        public void setOut(ServletOutputStream out) {
            this.out = out;
            try {
                byte[] bytes = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(bytes);
                ReferenceCountUtil.safeRelease(this.byteBuf);
                this.byteBuf = null;
                if (bytes.length > 0) {
                    this.out.write(bytes);
                    this.out.flush();
                }
            } catch (Exception ignore) {

            }
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            if (Objects.nonNull(this.out)) {
                byte[] bytes = new byte[in.readableBytes()];
                in.readBytes(bytes);
                this.out.write(bytes);
                this.out.flush();
            } else {
                if (Objects.nonNull(this.byteBuf)) {
                    this.byteBuf.writeBytes(in);
                } else {
                    this.byteBuf = Unpooled.copiedBuffer(in);
                }
            }
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            this.handler.destroy();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            this.handler.destroy();
        }

    }

    class BufferReadListener implements ReadListener {

        private final ServletInputStream in;
        private final NettyProxyWebSocketUpgradeHandler handler;

        public BufferReadListener(ServletInputStream in, NettyProxyWebSocketUpgradeHandler handler) {
            this.in = in;
            this.handler = handler;
        }

        @Override
        public void onDataAvailable() throws IOException {
            this.write();
        }

        @Override
        public void onAllDataRead() throws IOException {
            this.write();
        }

        @Override
        public void onError(Throwable throwable) {
            handler.destroy();
        }

        private void write() {
            try {
                if (in.isReady()) {
                    int read;
                    byte[] bytes = new byte[128];
                    read = in.read(bytes);
                    if (read > 0) {
                        ctx.writeAndFlush(Unpooled.copiedBuffer(bytes, 0, read));
                    }
                }
            } catch (Exception e) {
                handler.destroy();
            }

        }

    }
}
