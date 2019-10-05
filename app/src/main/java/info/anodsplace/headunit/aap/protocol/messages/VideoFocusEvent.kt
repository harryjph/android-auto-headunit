package info.anodsplace.headunit.aap.protocol.messages

import com.google.protobuf.MessageLite
import info.anodsplace.headunit.aap.AapMessage
import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.proto.Media


class VideoFocusEvent(gain: Boolean, unsolicited: Boolean)
    : AapMessage(Channel.ID_VID, Media.MediaMsgType.VIDEOFOCUSNOTIFICATION_VALUE, makeProto(gain, unsolicited)) {

    companion object {
        private fun makeProto(gain: Boolean, unsolicited: Boolean): MessageLite {
            return Media.VideoFocusNotification.newBuilder().apply {
                mode = if (gain) Media.VideoFocusMode.FOCUSED else Media.VideoFocusMode.UNFOCUSED
                setUnsolicited(unsolicited)
            }.build()
        }
    }
}
