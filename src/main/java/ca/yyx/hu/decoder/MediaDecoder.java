package ca.yyx.hu.decoder;

import android.content.Context;
import android.graphics.SurfaceTexture;

import java.nio.ByteBuffer;

import ca.yyx.hu.HeadUnitTransport;
import ca.yyx.hu.Utils;

/**
 * @author algavris
 * @date 28/04/2016.
 */
public class MediaDecoder {
    private final AudioDecoder mAudioDecoder;
    private final VideoDecoder mVideoDecoder;

    public MediaDecoder(Context context) {
        mAudioDecoder = new AudioDecoder(context);
        mVideoDecoder = new VideoDecoder(context);
    }


    public void decode(ByteBuffer content) {                       // Decode audio or H264 video content. Called only by video_test() & HeadUnitTransport.aa_cmd_send()

        Utils.logv("content: " + content);

        if (content == null) {                                              // If no content
            return;
        }

        int pos = content.position();
        if (pos != 0) {                                                       // For content byte array we assume position = 0 so test and log error if position not 0
            Utils.loge("pos != 0  change hardcode 0, 1, 2, 3");
        }

        int siz = content.remaining();
        byte[] ba = content.array();                                      // Create content byte array


        if (ba[0] == 0 && ba[1] == 0 && ba[2] == 0 && ba[3] == 1) {
            Utils.logv("H264 video");
        } else {
            Utils.logv("Audio");
            if (Utils.quiet_file_get("/sdcard/hureca"))                     // If audio record flag file exists...
                mAudioDecoder.audio_record_write(content);
            else if (mAudioDecoder.isRecording())                                         // Else if was recording... (file must have been removed)
                mAudioDecoder.audio_record_stop();

            if (siz <= 2048 + 96)
                mAudioDecoder.out_audio_write(HeadUnitTransport.AA_CH_AU1, ba, pos + siz);                     // Position always 0 so just use siz as len ?
            else
                mAudioDecoder.out_audio_write(HeadUnitTransport.AA_CH_AUD, ba, pos + siz);                     // Position always 0 so just use siz as len ?
            return;
        }

        mVideoDecoder.decode(content);

    }

    public void stop() {

        mAudioDecoder.audio_record_stop();

        mAudioDecoder.out_audio_stop(HeadUnitTransport.AA_CH_AUD);                                         // In case Byebye terminates without proper audio stop
        mAudioDecoder.out_audio_stop(HeadUnitTransport.AA_CH_AU1);
        mAudioDecoder.out_audio_stop(HeadUnitTransport.AA_CH_AU2);

        mVideoDecoder.stop_record();
    }

    public AudioDecoder getAudioDecoder() {
        return mAudioDecoder;
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mVideoDecoder.onSurfaceTextureAvailable(surface, width, height);
    }
}
