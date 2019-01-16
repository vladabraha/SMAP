package cz.uhk.fim.brahavl1.smartmeasurment.Model;

import java.io.Serializable;

public class Coordinate implements Serializable {

    private double longtitude;
    private double latitude;

    public Coordinate() {
    }

    public Coordinate(double longtitude, double latitude) {
        this.longtitude = longtitude;
        this.latitude = latitude;
    }

    public double getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(double longtitude) {
        this.longtitude = longtitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}
