package info.anodsplace.headunit.aap.protocol.messages

import com.google.protobuf.Message
import info.anodsplace.headunit.aap.AapMessage
import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.proto.Media

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */

class VideoFocusEvent(gain: Boolean, unsolicited: Boolean)
    : AapMessage(Channel.ID_VID, Media.MsgType.VIDEOFOCUSNOTIFICATION_VALUE, makeProto(gain, unsolicited)) {

    companion object {
        private fun makeProto(gain: Boolean, unsolicited: Boolean): Message {
            return Media.VideoFocusNotification.newBuilder().apply {
                mode = if (gain) Media.VideoFocusMode.FOCUSED else Media.VideoFocusMode.UNFOCUSED
                setUnsolicited(unsolicited)
            }.build()
        }
    }
}
