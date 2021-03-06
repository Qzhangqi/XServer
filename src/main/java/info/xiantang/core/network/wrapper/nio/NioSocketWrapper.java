package info.xiantang.core.network.wrapper.nio;

import info.xiantang.core.network.connector.nio.NioPoller;
import info.xiantang.core.network.endpoint.nio.NioEndpoint;
import info.xiantang.core.network.wrapper.SocketWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class NioSocketWrapper implements SocketWrapper {

    private final NioEndpoint server;
    private final NioPoller poller;
    private HttpServletResponse response;
    private HttpServletRequest request;

    private volatile boolean newSocket;

    public NioEndpoint getServer() {
        return server;
    }

    public NioPoller getPoller() {
        return poller;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    private final SocketChannel socketChannel;

    public boolean isNewSocket() {
        return newSocket;
    }

    public NioSocketWrapper(NioEndpoint server, NioPoller poller, SocketChannel socketChannel, boolean isNewSocket) {
        this.server = server;
        this.poller = poller;
        this.socketChannel = socketChannel;
        this.newSocket = isNewSocket;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public void close() throws IOException {
        // 取消注冊
        Selector selector = poller.getSelector();
        // 锁住这个对象
        // 使这个共享的对象的操作原子化
        synchronized (socketChannel) {
            if (socketChannel.isOpen()) {
                socketChannel.keyFor(selector).cancel();
                socketChannel.close();
            }
        }

    }
}
