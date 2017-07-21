package ca.yyx.hu.aap

import ca.yyx.hu.aap.protocol.messages.Messages
import ca.yyx.hu.aap.protocol.nano.MediaPlayback
import ca.yyx.hu.utils.AppLog
import com.google.protobuf.nano.InvalidProtocolBufferNanoException
import com.google.protobuf.nano.MessageNano
import java.nio.ByteBuffer
import ca.yyx.hu.main.BackgroundNotification

/**
 * @author algavris
 * @date 08/07/2017
 */

class AapMediaPlayback(private val notification: BackgroundNotification) {
    private val media_playback_message = ByteBuffer.allocate(Messages.DEF_BUFFER_LENGTH * 2)
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
                    media_playback_message.put(message.data, message.dataOffset, message.size - message.dataOffset)
                    this.started = true
                    // If First fragment
                }
            } else -> {
                if (this.started) {
                    if (flags == 0x08) {
                        media_playback_message.put(message.data, 0, message.size)
                        return
                        // If Middle fragment
                    } else if (flags == 0xa) {
                        media_playback_message.put(message.data, 0 , message.size)
                        media_playback_message.flip()
                        try {
                            val request = MessageNano.mergeFrom(MediaPlayback.MediaMetaData(), media_playback_message.array(), 0, media_playback_message.limit())
                            notifyRequest(request)
                        } catch (e: InvalidProtocolBufferNanoException) {
                            AppLog.e(e)
                        }
                        this.started = false
                        media_playback_message.clear()
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