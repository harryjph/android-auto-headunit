package info.anodsplace.headunit.aap

import info.anodsplace.headunit.aap.protocol.messages.Messages
import info.anodsplace.headunit.aap.protocol.nano.MediaPlayback
import info.anodsplace.headunit.utils.AppLog
import com.google.protobuf.nano.InvalidProtocolBufferNanoException
import com.google.protobuf.nano.MessageNano
import java.nio.ByteBuffer
import info.anodsplace.headunit.main.BackgroundNotification

/**
 * @author algavris
 * @date 08/07/2017
 */

class AapMediaPlayback(private val notification: BackgroundNotification) {
    private val messageBuffer = ByteBuffer.allocate(Messages.DEF_BUFFER_LENGTH * 2)
    private var started = false

    fun process(message: AapMessage) {

        val flags = message.flags.toInt()

        when (message.type) {
            MediaPlayback.MSG_PLAYBACK_METADATA -> {
                val request = message.parse(MediaPlayback.MediaMetaData())
                notifyRequest(request)
            }
            MediaPlayback.MSG_PLAYBACK_METADATASTART -> {
                if (flags == 0x09) {
                    messageBuffer.put(message.data, message.dataOffset, message.size - message.dataOffset)
                    this.started = true
                    // If First fragment
                }
            } else -> {
                if (this.started) {
                    if (flags == 0x08) {
                        messageBuffer.put(message.data, 0, message.size)
                        return
                        // If Middle fragment
                    } else if (flags == 0xa) {
                        messageBuffer.put(message.data, 0 , message.size)
                        messageBuffer.flip()
                        try {
                            val request = MessageNano.mergeFrom(MediaPlayback.MediaMetaData(), messageBuffer.array(), 0, messageBuffer.limit())
                            notifyRequest(request)
                        } catch (e: InvalidProtocolBufferNanoException) {
                            AppLog.e(e)
                        }
                        this.started = false
                        messageBuffer.clear()
                        // If Last fragment
                        return
                    }
                }
                AppLog.e("Unsupported %s", message.toString())
            }
        }
    }

    private fun notifyRequest(request: MediaPlayback.MediaMetaData) {
        notification.notify(request)
    }

}