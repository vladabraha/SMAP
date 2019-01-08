package cz.uhk.fim.brahavl1.smartmeasurment;

public class Coordinate {

    private double logntitude;
    private double lattitude;

    public Coordinate() {
    }

    public Coordinate(double logntitude, double lattitude) {
        this.logntitude = logntitude;
        this.lattitude = lattitude;
    }

    public double getLogntitude() {
        return logntitude;
    }

    public void setLogntitude(double logntitude) {
        this.logntitude = logntitude;
    }

    public double getLattitude() {
        return lattitude;
    }

    public void setLattitude(double lattitude) {
        this.lattitude = lattitude;
    }
}
