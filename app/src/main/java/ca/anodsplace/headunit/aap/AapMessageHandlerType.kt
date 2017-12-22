package ca.anodsplace.headunit.aap

import ca.anodsplace.headunit.aap.protocol.Channel
import ca.anodsplace.headunit.decoder.MicRecorder
import ca.anodsplace.headunit.main.BackgroundNotification
import ca.anodsplace.headunit.utils.AppLog
import ca.anodsplace.headunit.utils.Settings
import com.google.protobuf.nano.InvalidProtocolBufferNanoException

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
        settings: Settings,
        backgroundNotification: BackgroundNotification) : AapMessageHandler {

    private val aapControl: AapControl = AapControlGateway(transport, recorder, aapAudio, settings)
    private val mediaPlayback = AapMediaPlayback(backgroundNotification)

    @Throws(AapMessageHandler.HandleException::class)
    override fun handle(message: AapMessage) {

        val msgType = message.type
        val flags = message.flags

        if (message.isAudio && (msgType == 0 || msgType == 1)) {
            transport.sendMediaAck(message.channel)
            aapAudio.process(message)
            // 300 ms @ 48000/sec   samples = 14400     stereo 16 bit results in bytes = 57600
        } else if (message.isVideo && (msgType == 0 || msgType == 1 || flags.toInt() == 8 || flags.toInt() == 9 || flags.toInt() == 10)) {
            transport.sendMediaAck(message.channel)
            aapVideo.process(message)
        } else if (message.channel == Channel.ID_MPB && msgType > 31) {
            mediaPlayback.process(message)
        } else if (msgType in 0..31 || msgType in 32768..32799 || msgType in 65504..65535) {
            try {
                aapControl.execute(message)
            } catch (e: InvalidProtocolBufferNanoException) {
                AppLog.e(e)
                throw AapMessageHandler.HandleException(e)
            }
        } else {
            AppLog.e("Unknown msg_type: %d, flags: %d", msgType, flags)
        }

    }
}
