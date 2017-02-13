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

public class DrivingStatusEvent extends AapMessage {

    private static MessageNano makeProto(int status)
    {
        Protocol.SensorBatch sensorBatch = new Protocol.SensorBatch();
        sensorBatch.drivingStatus = new Protocol.SensorBatch.DrivingStatusData[1];
        sensorBatch.drivingStatus[0] = new Protocol.SensorBatch.DrivingStatusData();
        sensorBatch.drivingStatus[0].status = status;
        return sensorBatch;
    }

    public DrivingStatusEvent(int status)
    {
        super(Channel.ID_SEN, MsgType.Sensor.EVENT, makeProto(status));
    }
}
