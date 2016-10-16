#include "hu_uti.h"
#include "hu_ssl.h"

#include <jni.h>

JNIEXPORT jint JNICALL
Java_ca_yyx_hu_aap_AapTransport_native_1ssl_1prepare(JNIEnv *env, jclass type) {

    return hu_ssl_prepare();

}

JNIEXPORT jint JNICALL
Java_ca_yyx_hu_aap_AapTransport_native_1ssl_1do_1handshake(JNIEnv *env, jclass type) {

    return hu_ssl_do_handshake();

}

JNIEXPORT jint JNICALL
Java_ca_yyx_hu_aap_AapTransport_native_1ssl_1bio_1read(JNIEnv *env, jclass type, jint res_len, jbyteArray res_buf_) {
    jbyte *res_buf = (*env)->GetByteArrayElements(env, res_buf_, NULL);

    int ret = hu_ssl_bio_read(res_len, res_buf);

    (*env)->ReleaseByteArrayElements(env, res_buf_, res_buf, 0);

    return ret;
}

JNIEXPORT jint JNICALL
Java_ca_yyx_hu_aap_AapTransport_native_1ssl_1bio_1write(JNIEnv *env, jclass type, jint start, jint msg_len, jbyteArray msg_buf_) {
    jbyte *msg_buf = (*env)->GetByteArrayElements(env, msg_buf_, NULL);

    int ret = hu_ssl_bio_write(start, msg_len, msg_buf);

    (*env)->ReleaseByteArrayElements(env, msg_buf_, msg_buf, 0);

    return ret;
}

JNIEXPORT jint JNICALL
Java_ca_yyx_hu_aap_AapTransport_native_1ssl_1read(JNIEnv *env, jclass type, jint res_len, jbyteArray res_buf_) {
    jbyte *res_buf = (*env)->GetByteArrayElements(env, res_buf_, NULL);

    int ret = hu_ssl_read(res_len, res_buf);

    (*env)->ReleaseByteArrayElements(env, res_buf_, res_buf, 0);

    return ret;
}

JNIEXPORT jint JNICALL
Java_ca_yyx_hu_aap_AapTransport_native_1ssl_1write(JNIEnv *env, jclass type, jint msg_len, jbyteArray msg_buf_) {
    jbyte *msg_buf = (*env)->GetByteArrayElements(env, msg_buf_, NULL);

    int ret = hu_ssl_write(msg_len, msg_buf);

    (*env)->ReleaseByteArrayElements(env, msg_buf_, msg_buf, 0);

    return ret;
}
