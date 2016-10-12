package ca.yyx.hu.aap;

import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 01/10/2016.
 */
class AapVideo {

    private final AapTransport mTransport;

    private byte vid_ack[] = {(byte) 0x80, 0x04, 0x08, 0, 0x10, 1};                    // Global Ack: 0, 1

    private static final int VIDEO_BUFS_SIZE = 65536 * 4;      // Up to 256 Kbytes
    private static final int VIDEO_BUFS_NUM = 16;            // Maximum of NUM_vid_buf_BUFS - 1 in progress; 1 is never used

    private int num_vid_buf_bufs = VIDEO_BUFS_NUM;

    private byte[][] vid_buf_bufs = new byte[VIDEO_BUFS_NUM][VIDEO_BUFS_SIZE];

    private int[] vid_buf_lens = new int[VIDEO_BUFS_NUM];

    private int vid_buf_buf_tail = 0;    // Tail is next index for writer to write to.   If head = tail, there is no info.
    private int vid_buf_buf_head = 0;    // Head is next index for reader to read from.

    private int vid_buf_errs = 0;
    private int vid_max_bufs = 0;
    private int vid_sem_tail = 0;
    private int vid_sem_head = 0;

    private byte[] assy = new byte[65536 * 16];  // Global assembly buffer for video fragments: Up to 1 megabyte   ; 128K is fine for now at 800*640
    private int assy_size = 0;                   // Current size

    AapVideo(AapTransport transport) {
        mTransport = transport;
    }

    int buffersCount() {
        return vid_buf_buf_tail - vid_buf_buf_head;
    }

    ByteArray vid_read_head_buf_get() {
        int len = 0;

        int bufs = vid_buf_buf_tail - vid_buf_buf_head;
        if (bufs < 0)                                                       // If underflowed...
            bufs += num_vid_buf_bufs;                                          // Wrap
        //logd ("vid_read_head_buf_get start bufs: %d  head: %d  tail: %d", bufs, vid_buf_buf_head, vid_buf_buf_tail);

        if (bufs <= 0) {                                                    // If no buffers are ready...
            //logd ("vid_read_head_buf_get no vid_buf_bufs");
            //vid_buf_errs ++;  // Not an error; just no data
            //vid_buf_buf_tail = vid_buf_buf_head = 0;                          // Drop all buffers
            return null;
        }

        int max_retries = 4;
        int retries = 0;
        for (retries = 0; retries < max_retries; retries++) {
            vid_sem_head++;
            if (vid_sem_head == 1)
                break;
            vid_sem_head--;
            Utils.loge ("vid_sem_head wait");
            Utils.ms_sleep(10);
        }
        if (retries >= max_retries) {
            Utils.loge ("vid_sem_head could not be acquired");
            return null;
        }

        if (vid_buf_buf_head < 0 || vid_buf_buf_head > num_vid_buf_bufs - 1)   // Protect
            vid_buf_buf_head &= num_vid_buf_bufs - 1;

        vid_buf_buf_head++;

        if (vid_buf_buf_head < 0 || vid_buf_buf_head > num_vid_buf_bufs - 1)
            vid_buf_buf_head &= num_vid_buf_bufs - 1;

        ByteArray result = new ByteArray(VIDEO_BUFS_SIZE);
        result.data = vid_buf_bufs[vid_buf_buf_head];
        result.length = vid_buf_lens[vid_buf_buf_head];

        //logd ("vid_read_head_buf_get done  ret: %p  bufs: %d  head len: %d  head: %d  tail: %d", ret, bufs, * len, vid_buf_buf_head, vid_buf_buf_tail);

        vid_sem_head--;

        return result;
    }

    //iaap_video_process
    int process(int msg_type, int flags, byte[] buf, int len) {
        // Process video packet
        // MaxUnack

        int ret = mTransport.sendEncrypted(Channel.AA_CH_VID, vid_ack, vid_ack.length);      // Respond with ACK (for all fragments ?)

        if (flags == 11 && (msg_type == 0 || msg_type == 1) && (buf[10] == 0 && buf[11] == 0 && buf[12] == 0 && buf[13] == 1)) {  // If Not fragmented Video
            iaap_video_decode(buf, 10, len - 10);
            // Decode H264 video
        } else if (flags == 9 && (msg_type == 0 || msg_type == 1) && (buf[10] == 0 && buf[11] == 0 && buf[12] == 0 && buf[13] == 1)) {   // If First fragment Video
            System.arraycopy(buf, 10, assy, 0, len - 10);
            // Len in bytes 2,3 doesn't include total len 4 bytes at 4,5,6,7
            assy_size = len - 10;                                                                                                   // Add to re-assembly in progress
        }
        else if (flags == 11 && msg_type == 1 && (buf[2] == 0 && buf[3] == 0 && buf[4] == 0 && buf[5] == 1)) {                     // If Not fragmented First video config packet
            iaap_video_decode(buf, 2, len - 2);                                                                                 // Decode H264 video
        }
        else if (flags == 8) {                                                                                                     // If Middle fragment Video
            System.arraycopy(buf, 0, assy, assy_size, len);
            assy_size += len;                                                                                                       // Add to re-assembly in progress
        }
        else if (flags == 10) {                                                                                                    // If Last fragment Video
            System.arraycopy(buf, 0, assy, assy_size, len);
            assy_size += len;                                                                                                       // Add to re-assembly in progress
            iaap_video_decode(assy, 0, assy_size);
            // Decode H264 video fully re-assembled
        }
        else
            Utils.loge("Video error msg_type: %d  flags: 0x%x  buf: %p  len: %d", msg_type, flags, buf, len);

        return 0;
    }

    private void iaap_video_decode(byte[] buf, int start, int len) {

        byte[] q_buf = vid_write_tail_buf_get(len);                         // Get queue buffer tail to write to     !!! Need to lock until buffer written to !!!!

        // logd ("video q_buf: %p  buf: %p  len: %d", q_buf, buf, len);
        if (q_buf == null) {
            Utils.loge ("Error video no q_buf: %p  buf: %p  len: %d", q_buf, buf, len);
            //return;                                                         // Continue in order to write to record file
        } else {
            System.arraycopy(buf, start, q_buf, 0, len);
        }
    }


    private byte[] vid_write_tail_buf_get(int len) {                          // Get tail buffer to write to

        int bufs = vid_buf_buf_tail - vid_buf_buf_head;
        if (bufs < 0)                                                       // If underflowed...
            bufs += num_vid_buf_bufs;                                         // Wrap
        //logd ("vid_write_tail_buf_get start bufs: %d  head: %d  tail: %d", bufs, vid_buf_buf_head, vid_buf_buf_tail);

        if (bufs > vid_max_bufs)                                            // If new maximum buffers in progress...
            vid_max_bufs = bufs;                                              // Save new max
        if (bufs >= num_vid_buf_bufs - 1) {                                 // If room for another (max = NUM_vid_buf_BUFS - 1)
            Utils.loge ("vid_write_tail_buf_get out of vid_buf_bufs");
            vid_buf_errs++;
            //vid_buf_buf_tail = vid_buf_buf_head = 0;                        // Drop all buffers
            return null;
        }

        int max_retries = 4;
        int retries = 0;
        for (retries = 0; retries < max_retries; retries++) {
            vid_sem_tail++;
            if (vid_sem_tail == 1)
                break;
            vid_sem_tail--;
            Utils.loge ("vid_sem_tail wait");
            Utils.ms_sleep(10);
        }
        if (retries >= max_retries) {
            Utils.loge ("vid_sem_tail could not be acquired");
            return null;
        }

        if (vid_buf_buf_tail < 0 || vid_buf_buf_tail > num_vid_buf_bufs - 1)   // Protect
            vid_buf_buf_tail &= num_vid_buf_bufs - 1;

        vid_buf_buf_tail++;

        if (vid_buf_buf_tail < 0 || vid_buf_buf_tail > num_vid_buf_bufs - 1)
            vid_buf_buf_tail &= num_vid_buf_bufs - 1;

        byte[] ret = vid_buf_bufs[vid_buf_buf_tail];
        vid_buf_lens[vid_buf_buf_tail] = len;

        //logd ("vid_write_tail_buf_get done  ret: %p  bufs: %d  tail len: %d  head: %d  tail: %d", ret, bufs, len, vid_buf_buf_head, vid_buf_buf_tail);

        vid_sem_tail--;

        return ret;
    }
}
