package cz.uhk.fim.brahavl1.smartmeasurment.Model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Settings {

    private double minOfAllRides;
    private double maxOfAllRides;
    private List<Date> listOfLastRide = new ArrayList<>(); //seznam ride, ktere byly v době posledního počítání

    public Settings(double minOfAllRides, double maxOfAllRides, List<Date> listOfLastRide) {
        this.minOfAllRides = minOfAllRides;
        this.maxOfAllRides = maxOfAllRides;
        this.listOfLastRide = listOfLastRide;
    }

    public double getMinOfAllRides() {
        return minOfAllRides;
    }

    public void setMinOfAllRides(double minOfAllRides) {
        this.minOfAllRides = minOfAllRides;
    }

    public double getMaxOfAllRides() {
        return maxOfAllRides;
    }

    public void setMaxOfAllRides(double maxOfAllRides) {
        this.maxOfAllRides = maxOfAllRides;
    }

    public List<Date> getListOfLastRide() {
        return listOfLastRide;
    }

    public void setListOfLastRide(List<Date> listOfLastRide) {
        this.listOfLastRide = listOfLastRide;
    }
}
