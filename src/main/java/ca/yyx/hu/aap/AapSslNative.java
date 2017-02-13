package ca.yyx.hu.aap;

import ca.yyx.hu.aap.protocol.messages.Messages;
import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 20/10/2016.
 */

class AapSslNative implements AapSsl {

    static {
        System.loadLibrary("hu_jni");
    }

    private static native int native_ssl_prepare();
    private static native int native_ssl_do_handshake();
    private static native int native_ssl_bio_read(int offset, int res_len, byte[] res_buf);
    private static native int native_ssl_bio_write(int offset, int msg_len, byte[] msg_buf);
    private static native int native_ssl_read(int offset, int res_len, byte[] res_buf);
    private static native int native_ssl_write(int offset, int msg_len, byte[] msg_buf);

    private static final byte[] bio_read = new byte[Messages.DEF_BUFFER_LENGTH];
    private static final byte[] enc_buf = new byte[Messages.DEF_BUFFER_LENGTH];
    private static final byte[] dec_buf = new byte[Messages.DEF_BUFFER_LENGTH];

    public int prepare()
    {
        int ret = native_ssl_prepare();
        if (ret < 0) {
            AppLog.e("SSL prepare failed: " + ret);
        }
        return ret;
    }

    public void handshake() {
        native_ssl_do_handshake();
    }

    public ByteArray bioRead() {
        int size = native_ssl_bio_read(0, Messages.DEF_BUFFER_LENGTH, bio_read);
        AppLog.i("SSL BIO read: %d", size);
        if (size <= 0) {
            AppLog.i("SSL BIO read error");
            return null;
        }
        return new ByteArray(bio_read, size);
    }

    public int bioWrite(int start, int length, byte[] buffer) {
        return native_ssl_bio_write(start, length, buffer);
    }

    public ByteArray decrypt(int start, int length, byte[] buffer)
    {
        int bytes_written = native_ssl_bio_write(start, length, buffer);
        // Write encrypted to SSL input BIO
        if (bytes_written <= 0) {
            AppLog.e("BIO_write() bytes_written: %d", bytes_written);
            return null;
        }

        int bytes_read = native_ssl_read(0, Messages.DEF_BUFFER_LENGTH, dec_buf);
        // Read decrypted to decrypted rx buf
        if (bytes_read <= 0) {
            AppLog.e("SSL_read bytes_read: %d", bytes_read);
            return null;
        }

        return new ByteArray(dec_buf, bytes_read);
    }

    public ByteArray encrypt(int offset, int length, byte[] buffer) {

        int bytes_written = native_ssl_write(offset, length, buffer);
        // Write plaintext to SSL
        if (bytes_written <= 0) {
            AppLog.e("SSL_write() bytes_written: %d", bytes_written);
            return null;
        }

        if (bytes_written != length) {
            AppLog.e("SSL Write len: %d  bytes_written: %d", length, bytes_written);
        }

        AppLog.v("SSL Write len: %d  bytes_written: %d", length, bytes_written);

        int bytes_read = native_ssl_bio_read(offset, Messages.DEF_BUFFER_LENGTH - offset, enc_buf);
        if (bytes_read <= 0) {
            AppLog.e("BIO read  bytes_read: %d", bytes_read);
            return null;
        }

        AppLog.v("BIO read bytes_read: %d", bytes_read);

        return new ByteArray(enc_buf, bytes_read + offset);
    }
}
