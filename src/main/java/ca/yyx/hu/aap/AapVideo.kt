package ca.yyx.hu.aap

import ca.yyx.hu.decoder.VideoDecoder
import ca.yyx.hu.utils.AppLog

/**
 * @author algavris
 * *
 * @date 01/10/2016.
 */
internal class AapVideo(private val mVideoDecoder: VideoDecoder) {

    // Global assembly buffer for video fragments: Up to 1 megabyte   ; 128K is fine for now at 800*640
    private val assy = ByteArray(65536 * 16)
    // Current size
    private var assy_size = 0

    fun process(message: AapMessage): Int {
        return process(message.type, message.flags.toInt(), message.data, message.size)
    }

    private fun process(msg_type: Int, flags: Int, buf: ByteArray, len: Int): Int {
        // Process video packet
        if (flags == 11 && (msg_type == 0 || msg_type == 1) && buf[10].toInt() == 0 && buf[11].toInt() == 0 && buf[12].toInt() == 0 && buf[13].toInt() == 1) {  // If Not fragmented Video
            iaap_video_decode(buf, 10, len - 10)
            // Decode H264 video
        } else if (flags == 9 && (msg_type == 0 || msg_type == 1) && buf[10].toInt() == 0 && buf[11].toInt() == 0 && buf[12].toInt() == 0 && buf[13].toInt() == 1) {   // If First fragment Video
            System.arraycopy(buf, 10, assy, 0, len - 10)
            // Len in bytes 2,3 doesn't include total len 4 bytes at 4,5,6,7
            assy_size = len - 10                                                                                                   // Add to re-assembly in progress
        } else if (flags == 11 && msg_type == 1 && buf[2].toInt() == 0 && buf[3].toInt() == 0 && buf[4].toInt() == 0 && buf[5].toInt() == 1) {                     // If Not fragmented First video config packet
            iaap_video_decode(buf, 2, len - 2)                                                                                 // Decode H264 video
        } else if (flags == 8) {                                                                                                     // If Middle fragment Video
            System.arraycopy(buf, 0, assy, assy_size, len)
            assy_size += len                                                                                                       // Add to re-assembly in progress
        } else if (flags == 10) {                                                                                                    // If Last fragment Video
            System.arraycopy(buf, 0, assy, assy_size, len)
            assy_size += len                                                                                                       // Add to re-assembly in progress
            iaap_video_decode(assy, 0, assy_size)
            // Decode H264 video fully re-assembled
        } else {
            AppLog.e("Video error msg_type: %d flags: 0x%x len: %d", msg_type, flags, len)
        }

        return 0
    }

    private fun iaap_video_decode(buf: ByteArray, start: Int, len: Int) {
        mVideoDecoder.decode(buf, start, len)
    }

}
