package ca.yyx.hu.aap.protocol.messages;

import java.util.ArrayList;

import ca.yyx.hu.aap.AapMessage;
import ca.yyx.hu.aap.protocol.AudioConfigs;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.utils.AppLog;

import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.aap.protocol.nano.Protocol.Service;
import ca.yyx.hu.aap.protocol.nano.Protocol.Service.SensorSourceService;
import ca.yyx.hu.aap.protocol.nano.Protocol.Service.MediaSinkService.VideoConfiguration;
import ca.yyx.hu.aap.protocol.nano.Protocol.Service.InputSourceService.TouchConfig;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 08/06/2016.
 */

public class Messages {
    public static final int DEF_BUFFER_LENGTH = 131080;
    public static byte[] VERSION_REQUEST = { 0, 1, 0, 1 };

    public static byte[] createRawMessage(int chan, int flags, int type, byte[] data, int size) {

        int total = 6 + size;
        byte[] buffer = new byte[total];

        buffer[0] = (byte) chan;
        buffer[1] = (byte) flags;
        Utils.intToBytes(size + 2, 2, buffer);
        Utils.intToBytes(type, 4, buffer);

        System.arraycopy(data, 0, buffer, 6, size);
        return buffer;
    }







}
