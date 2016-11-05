package ca.yyx.hu.aap;

import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.ByteArray;

/**
 * @author algavris
 * @date 20/10/2016.
 */

class AapSsl {

    static {
        System.loadLibrary("hu_jni");
    }

    private static native int native_ssl_prepare();
    private static native int native_ssl_do_handshake();
    private static native int native_ssl_bio_read(int res_len, byte[] res_buf);
    private static native int native_ssl_bio_write(int start, int msg_len, byte[] msg_buf);
    private static native int native_ssl_read(int res_len, byte[] res_buf);
    private static native int native_ssl_write(int msg_len, byte[] msg_buf);


    private static final byte[] bio_read = new byte[Protocol.DEF_BUFFER_LENGTH];
    private static final byte[] enc_buf = new byte[Protocol.DEF_BUFFER_LENGTH];
    private static final byte[] dec_buf = new byte[Protocol.DEF_BUFFER_LENGTH];


    static int prepare()
    {
        int ret = native_ssl_prepare();
        if (ret < 0) {
            AppLog.e("SSL prepare failed: " + ret);
        }
        return ret;
    }

    static void handshake() {
        native_ssl_do_handshake();
    }

    static ByteArray bioRead() {
        int size = native_ssl_bio_read(Protocol.DEF_BUFFER_LENGTH, bio_read);
        AppLog.i("SSL BIO read: %d", size);
        if (size <= 0) {
            AppLog.i("SSL BIO read error");
            return null;
        }
        return new ByteArray(0, bio_read, size);
    }

    static int bioWrite(int start, int length, byte[] buffer) {
        return native_ssl_bio_write(start, length, buffer);
    }

    static ByteArray decrypt(int start, int length, byte[] buffer)
    {
        int bytes_written = native_ssl_bio_write(start, length, buffer);
        // Write encrypted to SSL input BIO
        if (bytes_written <= 0) {
            AppLog.e("BIO_write() bytes_written: %d", bytes_written);
            return null;
        }

        int bytes_read = native_ssl_read(Protocol.DEF_BUFFER_LENGTH, dec_buf);
        // Read decrypted to decrypted rx buf
        if (bytes_read <= 0) {
            AppLog.e("SSL_read bytes_read: %d", bytes_read);
            return null;
        }

        return new ByteArray(0, dec_buf, bytes_read);
    }

    static ByteArray encrypt(int start, int length, byte[] buffer) {

        int bytes_written = native_ssl_write(length, buffer);
        // Write plaintext to SSL
        if (bytes_written <= 0) {
            AppLog.e("SSL_write() bytes_written: %d", bytes_written);
            return null;
        }

        if (bytes_written != length) {
            AppLog.e("SSL Write len: %d  bytes_written: %d", length, bytes_written);
        }

        AppLog.v("SSL Write len: %d  bytes_written: %d", length, bytes_written);

        int bytes_read = native_ssl_bio_read(Protocol.DEF_BUFFER_LENGTH - start,enc_buf);
        if (bytes_read <= 0) {
            AppLog.e("BIO read  bytes_read: %d", bytes_read);
            return null;
        }

        AppLog.v("BIO read bytes_read: %d", bytes_read);

        return new ByteArray(0, enc_buf, bytes_read);
    }
}
