package ca.yyx.hu.aap.protocol.messages;

import com.google.protobuf.nano.MessageNano;

import ca.yyx.hu.aap.AapMessage;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;

/**
 * @author algavris
 * @date 24/02/2017.
 */

public class SensorEvent extends AapMessage {
    public final int type;

    public SensorEvent(int type, MessageNano proto) {
        super(Channel.ID_SEN, MsgType.Sensor.EVENT, proto);
        this.type = type;
    }
}
