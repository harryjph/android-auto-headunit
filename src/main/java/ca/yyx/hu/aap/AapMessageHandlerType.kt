package ca.yyx.hu.aap

import com.google.protobuf.nano.InvalidProtocolBufferNanoException

import ca.yyx.hu.decoder.MicRecorder
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.Settings

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */

internal class AapMessageHandlerType(
        private val mTransport: AapTransport,
        recorder: MicRecorder,
        private val mAapAudio: AapAudio,
        private val mAapVideo: AapVideo,
        settings: Settings) : AapMessageHandler {

    private val mAapControl: AapControl = AapControl(mTransport, recorder, mAapAudio, settings)

    @Throws(AapMessageHandler.HandleException::class)
    override fun handle(message: AapMessage) {

        val msg_type = message.type
        val flags = message.flags

        if (message.isAudio && (msg_type == 0 || msg_type == 1)) {
            mTransport.sendMediaAck(message.channel)
            mAapAudio.process(message)
            // 300 ms @ 48000/sec   samples = 14400     stereo 16 bit results in bytes = 57600
        } else if (message.isVideo && msg_type == 0 || msg_type == 1 || flags.toInt() == 8 || flags.toInt() == 9 || flags.toInt() == 10) {
            mTransport.sendMediaAck(message.channel)
            mAapVideo.process(message)
        } else if (msg_type >= 0 && msg_type <= 31 || msg_type >= 32768 && msg_type <= 32799 || msg_type >= 65504 && msg_type <= 65535) {
            try {
                mAapControl.execute(message)
            } catch (e: InvalidProtocolBufferNanoException) {
                AppLog.e(e)
                throw AapMessageHandler.HandleException(e)
            }
        } else {
            AppLog.e("Unknown msg_type: %d", msg_type)
        }

    }
}
