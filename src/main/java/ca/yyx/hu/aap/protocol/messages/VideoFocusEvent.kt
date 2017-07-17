package ca.yyx.hu.aap.protocol.messages

import ca.yyx.hu.aap.AapMessage
import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.MsgType
import ca.yyx.hu.aap.protocol.nano.Media
import com.google.protobuf.nano.MessageNano

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */

class VideoFocusEvent(gain: Boolean, unsolicited: Boolean)
    : AapMessage(Channel.ID_VID, Media.MSG_MEDIA_VIDEOFOCUSNOTIFICATION, VideoFocusEvent.makeProto(gain, unsolicited)) {

    companion object {
        private fun makeProto(gain: Boolean, unsolicited: Boolean): MessageNano {
            val videoFocus = Media.VideoFocusNotification()
            videoFocus.mode = if (gain) 1 else 2
            videoFocus.unsolicited = unsolicited

            return videoFocus
        }
    }
}
