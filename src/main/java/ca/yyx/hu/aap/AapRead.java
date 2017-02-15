package ca.yyx.hu.aap;

import java.nio.ByteBuffer;

import ca.yyx.hu.aap.protocol.messages.Messages;
import ca.yyx.hu.connection.AccessoryConnection;
import ca.yyx.hu.decoder.MicRecorder;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.Settings;

/**
 * @author algavris
 * @date 13/02/2017.
 */
interface AapRead {
    int read();

    abstract class Base implements AapRead {
        final AccessoryConnection mConnection;
        final AapMessageHandler mHandler;
        final AapSsl mSsl;

        Base(AccessoryConnection connection,AapSsl ssl, AapMessageHandler handler)
        {
            mConnection = connection;
            mHandler = handler;
            mSsl = ssl;
        }

        public int read() {
            if (mConnection == null) {
                AppLog.e("No connection.");
                return -1;
            }

            return doRead();
        }

        protected abstract int doRead();
    }

    class Factory {
        public static AapRead create(AccessoryConnection connection, AapTransport transport, MicRecorder recorder, AapAudio aapAudio, AapVideo aapVideo, Settings settings) {
            AapMessageHandler handler = new AapMessageHandlerType(transport, recorder, aapAudio, aapVideo, settings);

            return connection.isSingleMessage() ?
                    new AapReadSingleMessage(connection, new AapSslNative(), handler) :
                    new AapReadMultipleMessages(connection, new AapSslNative(), handler);
        }
    }
}
