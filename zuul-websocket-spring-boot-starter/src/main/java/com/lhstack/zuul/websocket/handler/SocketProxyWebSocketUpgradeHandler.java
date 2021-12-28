package com.lhstack.zuul.websocket.handler;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * @author lhstack
 * @date 2021/12/27
 * @class SocketProxyWebSocketUpgradeHandler
 * @since 1.8
 */
public class SocketProxyWebSocketUpgradeHandler implements HttpUpgradeHandler {
    private Socket socket;
    private WebConnection connection;

    @Override
    public void init(WebConnection connection) {
        this.connection = connection;
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    ServletOutputStream out = connection.getOutputStream();
                    InputStream in = socket.getInputStream();
                    byte[] bytes = new byte[128];
                    int read;
                    while ((read = in.read(bytes)) != -1) {
                        out.write(bytes, 0, read);
                        out.flush();
                    }
                } catch (Exception e) {
                    destroy();
                }
            });
            OutputStream out = this.socket.getOutputStream();
            ServletInputStream in = connection.getInputStream();
            in.setReadListener(new ReadListener() {
                @Override
                public void onDataAvailable() throws IOException {
                    try {
                        this.write();
                    } catch (Exception e) {
                        destroy();
                    }
                }

                private void write() throws IOException {
                    if (in.isReady()) {
                        byte[] bytes = new byte[128];
                        int read = in.read(bytes);
                        if (read > 0) {
                            out.write(bytes, 0, read);
                            out.flush();
                        }
                    }
                }

                @Override
                public void onAllDataRead() throws IOException {
                    try {
                        this.write();
                    } catch (Exception e) {
                        destroy();
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    destroy();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            destroy();
        }
    }

    @Override
    public void destroy() {
        try {
            this.socket.close();
            this.connection.getOutputStream().write("hello world".getBytes(StandardCharsets.UTF_8));
            this.connection.getOutputStream().flush();
            this.connection.close();
        } catch (Exception ignore) {

        }
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
