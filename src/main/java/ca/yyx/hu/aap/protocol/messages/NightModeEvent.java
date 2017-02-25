package ca.yyx.hu.aap.protocol.messages;

import com.google.protobuf.nano.MessageNano;

import ca.yyx.hu.aap.AapMessage;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.aap.protocol.nano.Protocol;

/**
 * @author algavris
 * @date 13/02/2017.
 */

public class NightModeEvent extends SensorEvent {

    private static MessageNano makeProto(boolean enabled)
    {
        Protocol.SensorBatch sensorBatch = new Protocol.SensorBatch();
        sensorBatch.nightMode = new Protocol.SensorBatch.NightModeData[1];
        sensorBatch.nightMode[0] = new Protocol.SensorBatch.NightModeData();
        sensorBatch.nightMode[0].isNight = enabled;
        return sensorBatch;
    }

    public NightModeEvent(boolean enabled)
    {
        super(Protocol.SENSOR_TYPE_NIGHT, makeProto(enabled));
    }
}
