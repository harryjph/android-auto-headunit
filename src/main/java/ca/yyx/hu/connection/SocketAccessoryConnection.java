package ca.yyx.hu.connection;


import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 05/11/2016.
 */
public class SocketAccessoryConnection implements AccessoryConnection {
    final String mIp;
    final Socket mSocket;

    public SocketAccessoryConnection(String ip) {
        mSocket = new Socket();
        mIp = ip;
    }

    @Override
    public int send(byte[] buf, int length, int timeout) {
        try {
            mSocket.setSendBufferSize(length);
            mSocket.getOutputStream().write(buf, 0, length);
            return length;
        } catch (IOException e) {
            AppLog.e(e);
            return -1;
        }
    }

    @Override
    public int recv(byte[] buf, int timeout) {

        try {
            mSocket.setSoTimeout(timeout);
            mSocket.setReceiveBufferSize(buf.length);
            return mSocket.getInputStream().read(buf,0,buf.length);
        } catch (IOException e) {
            AppLog.e(e);
            return -1;
        }

    }

    @Override
    public boolean isConnected() {
        return mSocket.isConnected();
    }

    @Override
    public void connect(@NonNull final Listener listener) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket.setTcpNoDelay(true);
                    mSocket.setReuseAddress(true);
                    mSocket.connect((new InetSocketAddress(mIp, 5277)), 3000);
                    listener.onConnectionResult(true);
                } catch (IOException e) {
                    AppLog.e(e);
                    listener.onConnectionResult(false);
                }
            }
        }, "socket_connect").start();
    }

    @Override
    public void disconnect() {
        if (mSocket.isConnected()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                AppLog.e(e);
                //catch logic
            }
        }
    }
}
