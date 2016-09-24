#include "hu_uti.h"
#include "hu_aap.h"

#include <jni.h>



int jni_aap_poll(int res_max, char *res_buf) {
    int res_len = 0;
    int ret = 0;

    int vid_bufs = vid_buf_buf_tail - vid_buf_buf_head;
    int aud_bufs = vid_buf_buf_tail - vid_buf_buf_head;

    // If any queue audio or video...
    if (vid_bufs > 0 || aud_bufs > 0) {
        ret = 0;
    } else {
        // Else Process 1 message w/ iaap_tra_recv_tmo
        ret = hu_aap_recv_process();
    }
    if (ret < 0) {
        return ret;
    }

    if (vid_bufs <= 0 && aud_bufs <= 0) {                               // If no queued audio or video...
        ret = hu_aap_mic_get();
        if (ret >= 1) {// && ret <= 2) {                                  // If microphone start (2) or stop (1)...
            return ret;                                                   // Done w/ mic notification: start (2) or stop (1)
        }
        // Else if no microphone state change...

        if (hu_aap_out_get(AA_CH_AUD) >= 0)                              // If audio out stop...
            return 3;                                                     // Done w/ audio out notification 0
        if (hu_aap_out_get(AA_CH_AU1) >= 0)                              // If audio out stop...
            return 4;                                                     // Done w/ audio out notification 1
        if (hu_aap_out_get(AA_CH_AU2) >= 0)                              // If audio out stop...
            return 5;                                                     // Done w/ audio out notification 2
    }

    char *dq_buf = NULL;
    dq_buf = aud_read_head_buf_get(&res_len);                         // Get audio if ready

    if (dq_buf == NULL) {                                           // If no audio... (Audio has priority over video)
        dq_buf = vid_read_head_buf_get(&res_len);                       // Get video if ready
    } else {                                                              // If audio
        if (dq_buf[0] == 0 && dq_buf[1] == 0 && dq_buf[2] == 0 && dq_buf[3] == 1) {
            dq_buf[3] = 0;                                                   // If audio happened to have magic video signature... (rare), then 0 the 1
            loge ("magic video signature in audio");
        }
    }

    if (dq_buf == NULL || res_len <= 0) {
        return 0;
    }
    memcpy(res_buf, dq_buf, res_len);

    return res_len;
}

JNIEXPORT jint JNICALL
Java_ca_yyx_hu_aap_AapTransport_native_1aap_1start(JNIEnv *env, jclass type, jint ep_in_addr, jint ep_out_addr) {

    return hu_aap_start((unsigned char) ep_in_addr, (unsigned char) ep_out_addr);                     // Start USB/ACC/OAP, AA Protocol
}

JNIEXPORT jint JNICALL
Java_ca_yyx_hu_aap_AapTransport_native_1aap_1poll(JNIEnv *env, jclass type, jint res_len, jbyteArray res_buf_) {
    jbyte *res_buf = (*env)->GetByteArrayElements(env, res_buf_, NULL);

    int ret = jni_aap_poll(res_len, res_buf);

    (*env)->ReleaseByteArrayElements(env, res_buf_, res_buf, 0);

    return ret;
}

JNIEXPORT jint JNICALL
Java_ca_yyx_hu_aap_AapTransport_native_1aap_1send(JNIEnv *env, jclass type, jint channel, jint cmd_len, jbyteArray cmd_buf_) {
    jbyte *cmd_buf = (*env)->GetByteArrayElements(env, cmd_buf_, NULL);

    int ret = hu_aap_enc_send(channel, cmd_buf, cmd_len);

    if (cmd_len >= 4 && cmd_buf[1] == 15) {      // If byebye...
        logd ("Byebye");
        ms_sleep(100);
        ret = hu_aap_stop();
    }

    (*env)->ReleaseByteArrayElements(env, cmd_buf_, cmd_buf, 0);

    return ret;
}

