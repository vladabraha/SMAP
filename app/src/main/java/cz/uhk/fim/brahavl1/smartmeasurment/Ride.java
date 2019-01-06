package cz.uhk.fim.brahavl1.smartmeasurment;

import com.here.android.mpa.common.GeoCoordinate;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class Ride {

    private String name;
    private Date date;
    private List<GeoCoordinate> testPoints;
    private List<Float> accelerometerData;


    public Ride(String name, List<GeoCoordinate> testPoints, List<Float> accelerometerData) {
        this.name = name;
        this.testPoints = testPoints;
        this.accelerometerData = accelerometerData;
        this.date = new Date();
    }

    public List<Float> getAccelerometerData() {
        return accelerometerData;
    }

    public void setAccelerometerData(List<Float> accelerometerData) {
        this.accelerometerData = accelerometerData;
    }

    public List<GeoCoordinate> getTestPoints() {
        return testPoints;
    }

    public void setTestPoints(List<GeoCoordinate> testPoints) {
        this.testPoints = testPoints;
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
