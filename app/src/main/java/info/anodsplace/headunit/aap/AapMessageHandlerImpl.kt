package info.anodsplace.headunit.aap

import android.content.Context
import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.decoder.MicRecorder
import info.anodsplace.headunit.utils.AppLog
import info.anodsplace.headunit.utils.Settings
import info.anodsplace.headunit.utils.bytesToHex
import java.lang.Exception


internal class AapMessageHandlerImpl (
        private val transport: AapTransport,
        recorder: MicRecorder,
        private val aapAudio: AapAudio,
        private val aapVideo: AapVideo,
        settings: Settings,
        context: Context) : AapMessageHandler {

    private val aapControl: AapControl = AapControlGateway(transport, recorder, aapAudio, settings, context)

    @Throws(AapMessageHandler.HandleException::class)
    override fun handle(message: AapMessage) {
        val msgType = message.type
        val flags = message.flags

        when {
            message.isAudio && (msgType == 0 || msgType == 1) -> {
                transport.sendMediaAck(message.channel)
                aapAudio.process(message)
                // 300 ms @ 48000/sec   samples = 14400     stereo 16 bit results in bytes = 57600
            }
            message.isVideo && (msgType == 0 || msgType == 1 || flags.toInt() == 8 || flags.toInt() == 9 || flags.toInt() == 10) -> {
                transport.sendMediaAck(message.channel)
                aapVideo.process(message)
            }
            msgType in 0..31 || msgType in 32768..32799 || msgType in 65504..65535 -> try {
                aapControl.execute(message)
            } catch (e: Exception) {
                AppLog.e(e)
                throw AapMessageHandler.HandleException(e)
            }
            else -> AppLog.e {
                val dataHex = bytesToHex(message.data)
                "Unknown msg_type on channel ${Channel.name(message.channel)}: $dataHex"
            }
            // Unknown msg_type: 37709, flags: 8
            // Unknown msg_type: 48451, flags: 8
            // Unknown msg_type: 12601, flags: 8
            // Unknown msg_type: 4727, flags: 8
            // Unknown msg_type: 49590, flags: 8
            // Unknown msg_type: 43051, flags: 8
            // Unknown msg_type: 42083, flags: 10
            // Unknown msg_type: 54007, flags: 8
            // Unknown msg_type: 20533, flags: 8
            // Unknown msg_type: 586, flags: 8
            // Unknown msg_type: 57872, flags: 8
            // Unknown msg_type: 16866, flags: 8
            // Unknown msg_type: 52253, flags: 8
            // Unknown msg_type: 4187, flags: 10
            // Unknown msg_type: 40234, flags: 8
            // Unknown msg_type: 9552, flags: 8
            // Unknown msg_type: 32209, flags: 8
            // Unknown msg_type: 31560, flags: 8
            // Unknown msg_type: 8329, flags: 8
            // Unknown msg_type: 37484, flags: 8
            // Unknown msg_type: 28078, flags: 10
            // Unknown msg_type: 37709, flags: 8
            // Unknown msg_type: 48451, flags: 8
            // Unknown msg_type: 12601, flags: 8
            // Unknown msg_type: 4727, flags: 8
            // Unknown msg_type: 49590, flags: 8
            // Unknown msg_type: 43051, flags: 8
            // Unknown msg_type: 42083, flags: 10
            // Unknown msg_type: 63891, flags: 8
            // Unknown msg_type: 22418, flags: 8
            // Unknown msg_type: 36382, flags: 8
            // Unknown msg_type: 65324, flags: 8
            // Unknown msg_type: 49526, flags: 8
            // Unknown msg_type: 58689, flags: 8
            // Unknown msg_type: 12310, flags: 10
            // Unknown msg_type: 62699, flags: 8
            // Unknown msg_type: 5684, flags: 8
            // Unknown msg_type: 51346, flags: 8
            // Unknown msg_type: 55132, flags: 8
            // Unknown msg_type: 2185, flags: 8
            // Unknown msg_type: 9423, flags: 8
            // Unknown msg_type: 46387, flags: 10
        }
    }
}
