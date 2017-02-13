package ca.yyx.hu.aap;

/**
 * @author algavris
 * @date 14/02/2017.
 */
interface AapSsl {
    int prepare();
    void handshake();
    ByteArray bioRead();
    int bioWrite(int start, int length, byte[] buffer);
    ByteArray decrypt(int start, int length, byte[] buffer);
    ByteArray encrypt(int offset, int length, byte[] buffer);
}
