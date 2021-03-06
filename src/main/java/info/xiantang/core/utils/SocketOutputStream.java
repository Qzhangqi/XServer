package info.xiantang.core.utils;

import info.xiantang.core.http.HttpResponse;

import java.io.IOException;
import java.io.Writer;
import java.nio.channels.SocketChannel;
import java.util.Date;

public class SocketOutputStream extends Writer{
    private SocketChannel socketChannel;
    private HttpResponse httpResponse;
    private int bufferUsedCap = 0;

    public SocketOutputStream(SocketChannel socketChannel, HttpResponse httpResponse) {
        this.socketChannel = socketChannel;
        this.httpResponse  = httpResponse;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        byte[] btuf = new byte[len - off];
        for (int i = 0, j = off; j < len; i++, j++) {
            btuf[i] = (byte) cbuf[j];
        }

        bufferUsedCap += btuf.length;

        if (httpResponse.getBufferSize() / 0.7 <= bufferUsedCap) {
            httpResponse.setBufferSize(httpResponse.getBufferSize() * 2);
        }

        httpResponse.getBodyBuffer().put(btuf);
    }

    @Override
    public void flush() throws IOException {

    }


    @Override
    public void close() throws IOException {
        httpResponse.setHeader("Date", String.valueOf(new Date()));
        httpResponse.setHeader("Server", "X Server/0.0.1;charset=UTF-8");
        httpResponse.setHeader("Content-Type", "text/html");
        httpResponse.setContentLength(bufferUsedCap);
        System.out.println("close 完成");
    }
}
