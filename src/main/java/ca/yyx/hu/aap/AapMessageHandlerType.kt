package ca.yyx.hu.aap

import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.MsgType
import ca.yyx.hu.aap.protocol.messages.Messages
import ca.yyx.hu.aap.protocol.nano.Protocol
import ca.yyx.hu.decoder.MicRecorder
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.Settings
import com.google.protobuf.nano.InvalidProtocolBufferNanoException
import com.google.protobuf.nano.MessageNano
import java.nio.ByteBuffer

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */

internal class AapMessageHandlerType(
        private val transport: AapTransport,
        recorder: MicRecorder,
        private val aapAudio: AapAudio,
        private val aapVideo: AapVideo,
        settings: Settings) : AapMessageHandler {

    private val aapControl: AapControl = AapControl(transport, recorder, aapAudio, settings)
    private val mediaPlayback = AapMediaPlayback(transport)

    @Throws(AapMessageHandler.HandleException::class)
    override fun handle(message: AapMessage) {

        val msg_type = message.type
        val flags = message.flags

        if (message.isAudio && (msg_type == 0 || msg_type == 1)) {
            transport.sendMediaAck(message.channel)
            aapAudio.process(message)
            // 300 ms @ 48000/sec   samples = 14400     stereo 16 bit results in bytes = 57600
        } else if (message.isVideo && (msg_type == 0 || msg_type == 1 || flags.toInt() == 8 || flags.toInt() == 9 || flags.toInt() == 10)) {
            transport.sendMediaAck(message.channel)
            aapVideo.process(message)
        } else if (message.channel == Channel.ID_MPB && msg_type > 31) {
            mediaPlayback.process(message)
        } else if (msg_type in 0..31 || msg_type in 32768..32799 || msg_type in 65504..65535) {
            try {
                aapControl.execute(message)
            } catch (e: InvalidProtocolBufferNanoException) {
                AppLog.e(e)
                throw AapMessageHandler.HandleException(e)
            }
        } else {
            AppLog.e("Unknown msg_type: %d, flags: %d", msg_type, flags)
        }

    }
}
