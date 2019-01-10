package cz.uhk.fim.brahavl1.smartmeasurment;

import android.os.Parcel;
import android.os.Parcelable;

import com.here.android.mpa.common.GeoCoordinate;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Ride implements Serializable{

    private String name;
    private Date date;
    private List<Coordinate> locationPoints;
    private List<Float> accelerometerData;


    public Ride(String name, List<Coordinate> locationPoints, List<Float> accelerometerData) {
        this.name = name;
        this.locationPoints = locationPoints;
        this.accelerometerData = accelerometerData;
        this.date = new Date();
    }

    public Ride() {
    }

    public List<Float> getAccelerometerData() {
        return accelerometerData;
    }

    public void setAccelerometerData(List<Float> accelerometerData) {
        this.accelerometerData = accelerometerData;
    }

    public List<Coordinate> getLocationPoints() {
        return locationPoints;
    }

    public void setLocationPoints(List<Coordinate> locationPoints) {
        this.locationPoints = locationPoints;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }


}
