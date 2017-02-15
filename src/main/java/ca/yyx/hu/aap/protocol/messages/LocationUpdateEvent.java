package ca.yyx.hu.aap.protocol.messages;

import android.location.Location;

import com.google.protobuf.nano.MessageNano;

import ca.yyx.hu.aap.AapMessage;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.aap.protocol.nano.Protocol;

/**
 * @author algavris
 * @date 15/02/2017.
 */

public class LocationUpdateEvent extends AapMessage {

    private static MessageNano makeProto(Location location)
    {
        Protocol.SensorBatch sensorBatch = new Protocol.SensorBatch();
        sensorBatch.locationData = new Protocol.SensorBatch.LocationData[1];
        sensorBatch.locationData[0] = new Protocol.SensorBatch.LocationData();
        sensorBatch.locationData[0].timestamp = location.getTime();
        sensorBatch.locationData[0].latitude = (int)(location.getLatitude() * 1E7);
        sensorBatch.locationData[0].longitude = (int)(location.getLongitude() * 1E7);
        sensorBatch.locationData[0].altitude = (int)(location.getAltitude() * 1E2);
        sensorBatch.locationData[0].bearing = (int)(location.getBearing() * 1E6);
        // AA expects speed in knots, so convert back
        sensorBatch.locationData[0].speed = (int)((location.getSpeed() * 1.94384) * 1E3);
        sensorBatch.locationData[0].accuracy = (int)(location.getAccuracy() * 1E3);
        return sensorBatch;
    }

    public LocationUpdateEvent(Location location) {
        super(Channel.ID_SEN, MsgType.Sensor.EVENT, makeProto(location));
    }
}
