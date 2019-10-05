package info.anodsplace.headunit.aap.protocol.messages

import com.google.protobuf.MessageLite
import info.anodsplace.headunit.aap.AapMessage
import info.anodsplace.headunit.aap.protocol.proto.Media


class MediaAck(channel: Int, sessionId: Int)
    : AapMessage(channel, Media.MediaMsgType.ACK_VALUE, makeProto(sessionId), ackBuf) {
    companion object {

        private val mediaAck = Media.Ack.newBuilder()
        private val ackBuf = ByteArray(20)

        private fun makeProto(sessionId: Int): MessageLite {
            mediaAck.clear()
            mediaAck.sessionId = sessionId
            mediaAck.ack = 1
            // TODO: check creation of new object can be avoided
            return mediaAck.build()
        }
    }
}
