package ca.yyx.hu.aap;

import android.support.annotation.NonNull;

import org.junit.Test;

import java.nio.ByteBuffer;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.connection.AccessoryConnection;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.Utils;

import static org.junit.Assert.assertEquals;

/**
 * @author algavris
 * @date 13/02/2017.
 */

public class AapReadTest {

    class FakeConnection implements AccessoryConnection {
        byte[] data = new byte[0];

        @Override
        public boolean isSingleMessage() {
            return false;
        }

        @Override
        public int send(byte[] buf, int length, int timeout) {
            return 0;
        }

        @Override
        public int recv(byte[] buf, int length, int timeout) {
            System.arraycopy(this.data, 0, buf, 0, this.data.length);
            return this.data.length;
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public void connect(@NonNull Listener listener) {

        }

        @Override
        public void disconnect() {

        }
    }

    class FakeHandler implements AapMessageHandler {
        AapMessage[] msg = new AapMessage[10];
        int idx = 0;
        @Override
        public void handle(AapMessage message) throws HandleException {
            this.msg[this.idx] = message;
            this.idx++;
        }
    }

    class FakeSsl implements AapSsl {

        @Override
        public int prepare() {
            return 0;
        }

        @Override
        public void handshake() {

        }

        @Override
        public ByteArray bioRead() {
            return null;
        }

        @Override
        public int bioWrite(int start, int length, byte[] buffer) {
            return 0;
        }

        @Override
        public ByteArray decrypt(int start, int length, byte[] buffer) {
            byte[] result = new byte[length];
            System.arraycopy(buffer, start, result, 0, length);
            return new ByteArray(result, length);
        }

        @Override
        public ByteArray encrypt(int offset, int length, byte[] buffer) {
            return null;
        }
    }

    @Test
    public void readMultipleMessages() {
        AppLog.LOGGER = new AppLog.Logger.StdOut();
        FakeConnection conn = new FakeConnection();
        FakeSsl ssl = new FakeSsl();
        FakeHandler handler = new FakeHandler();
        AapReadMultipleMessages poll = new AapReadMultipleMessages(conn, ssl, handler);

        conn.data = new byte[] {
            Channel.ID_CTR, 0x8, 0, 6, 0, MsgType.Control.PINGRESPONSE, 'M', 'S', 'G', '1',
            Channel.ID_VID, 0x9, 0, 7, 0, 0, 0, 1, 0, MsgType.Control.MEDIADATA, 'V', 'I', 'D', 'E', 'O',
            Channel.ID_CTR, 0x8, 0, 6, 0, MsgType.Control.PINGRESPONSE, 'M',
        };

        int res = poll.read();
        assertEquals(res, 0);
        assertEquals(handler.idx, 2);
        assertEquals(handler.msg[0], new AapMessage(Channel.ID_CTR, (byte) 0x8, MsgType.Control.PINGRESPONSE, 2, 6, new byte[] { 0, MsgType.Control.PINGRESPONSE, 'M', 'S', 'G', '1' }));
        assertEquals(handler.msg[1], new AapMessage(Channel.ID_VID, (byte) 0x9, MsgType.Control.MEDIADATA, 2, 7, new byte[] { 0, MsgType.Control.MEDIADATA, 'V', 'I', 'D', 'E', 'O' }));

        conn.data = new byte[] {
                'S', 'G', '2',
                Channel.ID_CTR, 0x8,
        };

        res = poll.read();
        assertEquals(res, 0);
        assertEquals(handler.idx, 3);
        assertEquals(handler.msg[2], new AapMessage(Channel.ID_CTR, (byte) 0x8, MsgType.Control.PINGRESPONSE, 2, 6, new byte[] { 0, MsgType.Control.PINGRESPONSE, 'M', 'S', 'G', '2' }));

        conn.data = new byte[] {
                 0, 6, 0, MsgType.Control.PINGRESPONSE, 'M', 'S', 'G', '3',
        };

        res = poll.read();
        assertEquals(res, 0);
        assertEquals(handler.idx, 4);
        assertEquals(handler.msg[3], new AapMessage(Channel.ID_CTR, (byte) 0x8, MsgType.Control.PINGRESPONSE, 2, 6, new byte[] { 0, MsgType.Control.PINGRESPONSE, 'M', 'S', 'G', '3' }));
    }
}
