package gov.adlnet.xapi.beaconfinder;

import android.os.Parcel;
import android.os.Parcelable;

import com.gimbal.android.Beacon;
import com.gimbal.android.Communication;

import java.util.Collection;
import java.util.Date;

public class MyBeaconInfo implements Parcelable{
    public Beacon beacon;
    public Integer rssi;
    public long timems;
    public Collection<Communication> communications;

    public MyBeaconInfo(Beacon b, Integer rssi, long timems) {
        this.beacon = b;
        this.rssi = rssi;
        this.timems = timems;
    }

    public void update(MyBeaconInfo b) {
        this.beacon = b.beacon;
        this.rssi = b.rssi;
        this.timems = b.timems;
    }

    @Override
    public boolean equals(Object o) {
        return !(o == null || o.getClass() != getClass()) &&
                beacon.getIdentifier().equals(((MyBeaconInfo) o).beacon.getIdentifier());
    }

    @Override
    public int hashCode() {
        return beacon.getIdentifier().hashCode();
    }

    public String toString() {
        return "Beacon Info\nBeacon Id: " + beacon.getIdentifier() +
                "\nrssi: " + rssi +
                "\ntime: " + new Date(timems);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(timems);
        dest.writeInt(rssi);
        dest.writeString(beacon.getName());
        dest.writeString(beacon.getIdentifier());
        dest.writeInt(beacon.getTemperature());
    }

    public static final Creator<MyBeaconInfo> CREATOR = new Creator<MyBeaconInfo>() {
        @Override
        public MyBeaconInfo createFromParcel(Parcel source) {
            return new MyBeaconInfo(source);
        }

        @Override
        public MyBeaconInfo[] newArray(int size) {
            return new MyBeaconInfo[size];
        }
    };

    private MyBeaconInfo(Parcel in) {
        this.timems = in.readLong();
        this.rssi = in.readInt();
        this.beacon = new Beacon();
        this.beacon.setName(in.readString());
        this.beacon.setIdentifier(in.readString());
        this.beacon.setTemperature(in.readInt());
    }
}
