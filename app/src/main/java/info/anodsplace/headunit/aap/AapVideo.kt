package info.anodsplace.headunit.aap

import info.anodsplace.headunit.aap.protocol.messages.Messages
import info.anodsplace.headunit.decoder.VideoDecoder
import info.anodsplace.headunit.utils.AppLog
import java.nio.ByteBuffer

/**
 * @author algavris
 * *
 * @date 01/10/2016.
 */
internal class AapVideo(private val videoDecoder: VideoDecoder) {

    private val messageBuffer = ByteBuffer.allocate(Messages.DEF_BUFFER_LENGTH * 8)

    fun process(message: AapMessage): Boolean {

        val flags = message.flags.toInt()
        val msg_type = message.type
        val buf = message.data
        val len = message.size

        when (flags) {
            11 -> {
                if ((msg_type == 0 || msg_type == 1)
                        && buf[10].toInt() == 0 && buf[11].toInt() == 0 && buf[12].toInt() == 0 && buf[13].toInt() == 1) {
                    // If Not fragmented Video // Decode H264 video
                    videoDecoder.decode(buf, 10, len - 10)
                    return true
                } else if (msg_type == 1 &&
                        buf[2].toInt() == 0 && buf[3].toInt() == 0 && buf[4].toInt() == 0 && buf[5].toInt() == 1) {
                    // If Not fragmented First video config packet // Decode H264 video
                    videoDecoder.decode(message.data, message.dataOffset, message.size - message.dataOffset)
                    return true
                }
            }
            9 -> {
                if ((msg_type == 0 || msg_type == 1)
                        && buf[10].toInt() == 0 && buf[11].toInt() == 0 && buf[12].toInt() == 0 && buf[13].toInt() == 1) {
                    // If First fragment Video
                    // Len in bytes 2,3 doesn't include total len 4 bytes at 4,5,6,7
                    messageBuffer.put(message.data, 10, message.size - 10)
                    return true
                }
            }
            8 -> {
                messageBuffer.put(message.data, 0, message.size)// If Middle fragment Video
                return true
            }
            10 -> {
                messageBuffer.put(message.data, 0, message.size)
                messageBuffer.flip()
                // Decode H264 video fully re-assembled
                videoDecoder.decode(messageBuffer.array(), 0, messageBuffer.limit())
                messageBuffer.clear()
                return true
            }
        }

        AppLog.e("Video process error for: %s", message.toString())
        return false
    }
}
