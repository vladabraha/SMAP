package cz.uhk.fim.brahavl1.smartmeasurment.Activity;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolyline;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapPolyline;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.List;

import cz.uhk.fim.brahavl1.smartmeasurment.Model.Coordinate;
import cz.uhk.fim.brahavl1.smartmeasurment.Model.Ride;
import cz.uhk.fim.brahavl1.smartmeasurment.R;

public class RideDetail extends AppCompatActivity {

    private Map map;
    private List<GeoCoordinate> locationPoints = new ArrayList<>();
    private List<Float> accelerometerPoints = new ArrayList<>();
    private List<Coordinate> rideCoordinates = new ArrayList<>();

    private GraphView graph;
    private Ride ride;

    //rozdíl mezi maximální a minimální hodnotou
    private double difference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_detail);
        graph = findViewById(R.id.graph_detail);

        ride = (Ride) getIntent().getExtras().get("ride");
        double MAX = (double) getIntent().getExtras().get("max");
        double MIN = (double) getIntent().getExtras().get("min");
        difference = MAX - MIN;


        rideCoordinates = ride.getLocationPoints();
        accelerometerPoints = ride.getAccelerometerData();

        try {
            if (ride.getLocationPoints() != null) {
                for (Coordinate coordinate : rideCoordinates) {
//                    Log.d("hoo","getLatitude je " + coordinate.getLatitude());
//                    Log.d("hoo","getLongitude je " +  coordinate.getLongtitude());
                    locationPoints.add(new GeoCoordinate(coordinate.getLatitude(), coordinate.getLongtitude(), 10));
                }

            }
        } catch (NullPointerException e) {
            Toast.makeText(this, "tento zaznam nema data o jizde", Toast.LENGTH_LONG).show();
        }


        // Search for the map fragment to finish setup by calling init().
        final MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.here_maps_fragment2);

        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();

                    if (locationPoints.size() > 2) {

                        int halfListPosition = locationPoints.size() / 2;
                        map.setCenter(new GeoCoordinate(locationPoints.get(halfListPosition).getLatitude(), locationPoints.get(halfListPosition).getLongitude()), Map.Animation.NONE);
                        map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);

//                    GeoPolyline polyline = new GeoPolyline(locationPoints);
//                    MapPolyline mapPolyline = new MapPolyline(polyline);
//                    mapPolyline.setLineColor(Color.RED);
//                    mapPolyline.setLineWidth(12);
//                    map.addMapObject(mapPolyline);

                        //
                        for (int i = 1; i < locationPoints.size() - 1; i++) {
                            List<GeoCoordinate> positionPoints = new ArrayList<>();
                            positionPoints.add(new GeoCoordinate(rideCoordinates.get(i - 1).getLatitude(), rideCoordinates.get(i - 1).getLongtitude(), 10));
                            positionPoints.add(new GeoCoordinate(rideCoordinates.get(i).getLatitude(), rideCoordinates.get(i).getLongtitude(), 10));


                            GeoPolyline polyline = new GeoPolyline(positionPoints);
                            MapPolyline mapPolyline = new MapPolyline(polyline);
                            mapPolyline.setLineColor(interpolateColor(Color.GREEN, Color.RED, accelerometerPoints.get(i) / (float) difference));


                            mapPolyline.setLineWidth(12);
                            map.addMapObject(mapPolyline);
                        }
                    }
                } else {
                    System.out.println("ERROR: Cannot initialize Map Fragment");
                }
            }
        });

        calculateAverageAndPlotGraph();
    }

    private void calculateAverageAndPlotGraph() {
        graph.removeAllSeries();

        //VYPOCET KLOUZAVEHO PRUMERU A JEHO VLOZENI DO GRAFU
        int length = ride.getAccelerometerData().size();
        double[] dataForMovingAverage = new double[length]; //kvuli commons math, ktery berou jenom pole double
        DataPoint[] dataPoints = new DataPoint[length]; //pole bodu pro graf
        int i = 0;
        for (float z : ride.getAccelerometerData()) {
            //do tohodle se pridavaj hodnoty z akcelerometru pro osu Z
            dataForMovingAverage[i] = z;

            //budeme počítat prumer pro poslednich 10 hodnot z dat a ty budeme vykreslovat
            if (i < 10) {
                double nextYValue = StatUtils.mean(dataForMovingAverage);
                dataPoints[i] = new DataPoint(i, nextYValue);
            } else {
                double nextYValue = StatUtils.mean(dataForMovingAverage, i - 10, 10);
                dataPoints[i] = new DataPoint(i, nextYValue);
            }
            i++;
        }
        //přidání dat do grafu
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        graph.addSeries(series);

    }

    /**
     * Interpoluje barvu mezi zadanými 2 barvami a procentem, mezi kterým se to má pohybovat
     *
     * @param a          zacatek rozsahu
     * @param b          konec rozsahu
     * @param proportion procento
     * @return vrátí barvu se zadaným procentem
     */
    private int interpolateColor(int a, int b, float proportion) {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }

    private float interpolate(float a, float b, float proportion) {
        return (a + ((b - a) * proportion));
    }
}
