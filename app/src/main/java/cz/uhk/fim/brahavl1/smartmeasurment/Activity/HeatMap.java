package cz.uhk.fim.brahavl1.smartmeasurment.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolyline;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapPolyline;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;

import cz.uhk.fim.brahavl1.smartmeasurment.Database.DatabaseConnector;
import cz.uhk.fim.brahavl1.smartmeasurment.Model.Ride;
import cz.uhk.fim.brahavl1.smartmeasurment.Model.Settings;
import cz.uhk.fim.brahavl1.smartmeasurment.R;

public class HeatMap extends Activity {

    private Map map;

    private List<Ride> rideList = new ArrayList<>();
    private Settings settings;
    List<Double> minMaxOfAllRide;
    private DatabaseConnector databaseConnector = new DatabaseConnector();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //---------------------------------------------------------------------
        //HIDE STATUS BAR
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //---------------------------------------------------------------------
        //MAP
        setContentView(R.layout.activity_heat_map);

        // Search for the map fragment to finish setup by calling init().
        final MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.here_maps_fragment_heat_map);

        mapFragment.init(error -> {
            if (error == OnEngineInitListener.Error.NONE) {
                // retrieve a reference of the map from the map fragment
                map = mapFragment.getMap();

            } else {
                System.out.println("ERROR: Cannot initialize Map Fragment");
            }
        });

        //---------------------------------------------------------------------
        //READ DATA FROM DATABASE
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("Settings");

        // Read from the database
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    settings = postSnapshot.getValue(Settings.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
            }
        });

        myRef = database.getReference("ride");

        // Read from the database
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                boolean isSettingsSame = true;
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    rideList.add(postSnapshot.getValue(Ride.class));

                    //Pro vyhledání maxima a minima v novém datasetu (slouží pro optimalizaci, aby se nemusely procházet všechna data)
                    isSettingsSame = true;
                    if (settings != null) {
                        for (Ride ride : rideList) {
                            isSettingsSame = settings.getListOfLastRide().contains(ride.getDate());
                        }
                    } else {
                        isSettingsSame = false;
                    }
                }
                if (!isSettingsSame) {
                    minMaxOfAllRide = new HeatMap.ComputeDataForHeatMap().doInBackground(rideList);
                } else {
                    minMaxOfAllRide.add(settings.getMinOfAllRides());
                    minMaxOfAllRide.add(settings.getMaxOfAllRides());

                }
                drawAllRoutes();
                if (!minMaxOfAllRide.isEmpty()) databaseConnector.saveSettings(minMaxOfAllRide.get(0), minMaxOfAllRide.get(1), rideList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
            }
        });
        //---------------------------------------------------------------------
    }

    private void drawAllRoutes() {
        double difference = minMaxOfAllRide.get(1) - minMaxOfAllRide.get(0);

        List<MapObject> mapObjectList = new ArrayList<>();
        for (Ride ride : rideList) {
            List<GeoCoordinate> positionPoints = new ArrayList<>();
            DescriptiveStatistics median = new DescriptiveStatistics();
            for (int i = 1; i < ride.getLocationPoints().size(); i++) {

                positionPoints.add(new GeoCoordinate(ride.getLocationPoints().get(i - 1).getLatitude(), ride.getLocationPoints().get(i - 1).getLongtitude(), 10));
                positionPoints.add(new GeoCoordinate(ride.getLocationPoints().get(i).getLatitude(), ride.getLocationPoints().get(i).getLongtitude(), 10));

                //načtení hodnoty aproximace z sharedPreferences
                SharedPreferences sharedPref = HeatMap.this.getSharedPreferences(getString(R.string.amountOfApproximation), MODE_PRIVATE);
                final int defaultValue = 4;
                final int amountOfApproximation = sharedPref.getInt(getString(R.string.amountOfApproximation), defaultValue);

                //aproximace kvůli lepšímu výkonu - každejch x přímek vypočítej průměr otřesu pro celejch x bodů -> bude mene objektu v mape a plynulejsi cara
                int k = i / amountOfApproximation;
                double l = (double) i / amountOfApproximation;
                if ((double) k == l) {
                    for (int q = 0; q < amountOfApproximation; q++){
                        median.addValue(ride.getAccelerometerData().get(i-q));
                    }
                    double accel = median.getPercentile(40);
                    GeoPolyline polyline = new GeoPolyline(positionPoints);
                    MapPolyline mapPolyline = new MapPolyline(polyline);
                    mapPolyline.setLineColor(interpolateColor(Color.GREEN, Color.RED, (float) accel / (float) difference));
                    mapPolyline.setLineWidth(12);
                    mapObjectList.add(mapPolyline);
                    positionPoints.clear();
                    median.clear();
                }
            }
        }
        map.addMapObjects(mapObjectList);
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

    /**
     * Metoda vypočítá (asynchroně) percetil ze zadaného seznamu
     * percentil - aby ořezal outliery (vzdáleného hodnoty)
     */
    @SuppressLint("StaticFieldLeak")
    private class ComputeDataForHeatMap extends AsyncTask<List<Ride>, Integer, List<Double>> {

        @SafeVarargs
        @Override
        protected final List<Double> doInBackground(List<Ride>... lists) {
            DescriptiveStatistics statistic = new DescriptiveStatistics();
            for (Ride ride : rideList){
                for (Float accelData : ride.getAccelerometerData()){
                    statistic.addValue(accelData);
                }
            }

            double minimum = statistic.getPercentile(1);
            double maximum = statistic.getPercentile(99);

            List<Double> list = new ArrayList<>();
            list.add(minimum);
            list.add(maximum);

            return list;
        }
    }
}
