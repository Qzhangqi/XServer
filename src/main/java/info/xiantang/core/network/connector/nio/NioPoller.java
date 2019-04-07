package info.xiantang.core.network.connector.nio;

import info.xiantang.core.network.endpoint.nio.NioEndpoint;
import info.xiantang.core.network.wrapper.nio.NioSocketWrapper;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

public class NioPoller implements Runnable {

    private NioEndpoint nioEndpoint;
    private Selector selector;
    private String pollerName;
    // 事件队列
    private Queue<PollerEvent> events;

    public NioPoller(NioEndpoint nioEndpoint, String pollerName) throws IOException {
        this.nioEndpoint = nioEndpoint;
        // 每个Poller 线程都对应了一个Selector
        this.selector = Selector.open();
        this.pollerName = pollerName;
        this.events = new ConcurrentLinkedDeque<>();
    }

    public Selector getSelector() {
        return selector;
    }

    public void register(SocketChannel socket, boolean isNewSocket) {

        NioSocketWrapper nioSocketWrapper = new NioSocketWrapper(nioEndpoint, this, socket, isNewSocket);

        events.offer(new PollerEvent(nioSocketWrapper));
        // 如果selector 在select 阻塞 就调用wakeup立马返回
        selector.wakeup();
    }

    @Override
    public void run() {
        while (nioEndpoint.isRunning()) {

            try {
                events();
                // 若沒有事件就緒則不往下執行
                int num = selector.select();
                if (num <= 0) {
                    continue;
                }
                // 获得所有已就绪事件Key集合
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                for (Iterator<SelectionKey> it =selectedKeys.iterator();it.hasNext();) {
                    SelectionKey key = it.next();
                    // 先判断是否有效
                    if (key.isValid()) {
                        if (key.isReadable()) {
                            // 读取事件就绪
                            NioSocketWrapper socketWrapper = (NioSocketWrapper)key.attachment();
                            if (socketWrapper != null) {
                                processSocket(socketWrapper);
                            }

                        }
                    }
                    it.remove();

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void events() {
        System.out.println("当前队列大小为 "+ events.size());
        PollerEvent pollerEvent;
        for (int i = 0, size = events.size(); i < size && (pollerEvent = events.poll())!= null; i++) {
            pollerEvent.run();
        }
    }

    /**
     * 將SocketChannel 交付給綫程池進行處理
     * @param nioSocketWrapper
     * @throws IOException
     */
    private void processSocket(NioSocketWrapper nioSocketWrapper) throws IOException {

        nioEndpoint.execute(nioSocketWrapper);


    }

    private static class PollerEvent implements Runnable  {

        // 包装对象
        private NioSocketWrapper wrapper;

        public PollerEvent(NioSocketWrapper wrapper) {
            this.wrapper = wrapper;
        }

        public NioSocketWrapper getWrapper() {
            return wrapper;
        }


        @Override
        public void run() {
            System.out.println("将读事件注册到Poller的selector中");
            try {
                if (wrapper.getSocketChannel().isOpen()) {


                    wrapper.getSocketChannel().register(wrapper.getPoller().getSelector(), SelectionKey.OP_READ, wrapper);
                } else {
                    System.out.println("socket已经关闭，无法注册到Poller");
                }
            }
            catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        }
    }


}
