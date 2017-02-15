package ca.yyx.hu.aap;

import com.google.protobuf.nano.InvalidProtocolBufferNanoException;

import ca.yyx.hu.decoder.MicRecorder;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.Settings;

/**
 * @author algavris
 * @date 13/02/2017.
 */

class AapMessageHandlerType implements AapMessageHandler {
    private final AapAudio mAapAudio;
    private final AapVideo mAapVideo;
    private final AapControl mAapControl;
    private final AapTransport mTransport;

    AapMessageHandlerType(AapTransport transport, MicRecorder recorder, AapAudio aapAudio, AapVideo aapVideo, Settings settings) {
        mAapAudio = aapAudio;
        mAapVideo = aapVideo;
        mTransport = transport;
        mAapControl = new AapControl(transport, recorder, mAapAudio, settings);
    }

    public void handle(AapMessage message) throws HandleException {

        int msg_type = message.type;
        byte flags = message.flags;

        if (message.isAudio() && (msg_type == 0 || msg_type == 1)) {
            mTransport.sendMediaAck(message.channel);
            mAapAudio.process(message);
            // 300 ms @ 48000/sec   samples = 14400     stereo 16 bit results in bytes = 57600
        } else if (message.isVideo() && msg_type == 0 || msg_type == 1 || flags == 8 || flags == 9 || flags == 10) {
            mTransport.sendMediaAck(message.channel);
            mAapVideo.process(message);
        } else if ((msg_type >= 0 && msg_type <= 31) || (msg_type >= 32768 && msg_type <= 32799) || (msg_type >= 65504 && msg_type <= 65535)) {
            try {
                mAapControl.execute(message);
            } catch (InvalidProtocolBufferNanoException e) {
                AppLog.e(e);
                throw new HandleException(e);
            }
        } else {
            AppLog.e("Unknown msg_type: %d", msg_type);
        }

    }
}
