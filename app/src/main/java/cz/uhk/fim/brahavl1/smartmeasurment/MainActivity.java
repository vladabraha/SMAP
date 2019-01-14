package cz.uhk.fim.brahavl1.smartmeasurment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.math3.stat.StatUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView txtLocation;

    private SensorManager mSensorManager;
    private Sensor accelerometer;

    private boolean firstRun = true;

    private final float alpha = 0.8f;
    private float gravity[] = new float[3];
    private float linear_acceleration[] = new float[3];

    //Array bodu, ktere se budou vykreslovat na grafu
    private List<Float> zPoints = new ArrayList<>();

    private LocationManager locationManager;
    private LocationListener locationListener;

    private static final int povoleni_operator = 0;
    private static final int povoleni_gps = 1;

    private GraphView graph;

    private boolean locationUpdateStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLocation = findViewById(R.id.txtPosition);

        Button btnPause = findViewById(R.id.btnPause);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnNew = findViewById(R.id.btnNew);
        Button btnMapBox = findViewById(R.id.btnMapBoxActivity);
        Button btnOverview = findViewById(R.id.btnOverView);

        Button btnStartUpdates = findViewById(R.id.btnStartUpdates);
        Button btnStopUpdates = findViewById(R.id.btnStopUpdates);

        graph = findViewById(R.id.graph);

        //inicializace akcelerometru
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //spusteni mereni z akcelerometru
        btnStart.setOnClickListener(view -> {
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
//            xAverage = 0;
//            yAverage = 0;
//            zAverage = 0;
//            iteration = 0;
            zPoints.clear();
            graph.removeAllSeries();
        });


        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPause();
                calculateAverageAndPlotGraph();

            }
        });

        //spusteni aktualizace polohy
        btnStartUpdates.setOnClickListener(view -> {
            checkPermission();
            locationUpdateStarted = true;
//                startUpdates();
        });

        btnStopUpdates.setOnClickListener(view -> {
            if (locationUpdateStarted){
                stopUpdates();
                locationUpdateStarted = false;
            }
        });


        btnNew.setOnClickListener(view -> {
            Intent notificationIntent = new Intent(MainActivity.this, PositionGoogle.class);
            startActivity(notificationIntent);
        });

        btnMapBox.setOnClickListener(view -> {
            Intent mapBox = new Intent(MainActivity.this, MapBox.class);
            startActivity(mapBox);
        });

        btnOverview.setOnClickListener(view -> {
            Intent rideOverview = new Intent(this, RideOverview.class);
            startActivity(rideOverview);
        });
//        Log.d("TAG", "v locmodelu je ");

    }

    //------------------------------------------------------------------------
    // AKCELEROMETR
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //při prvním spuštění se načte aktuální hodnota (offset)  pro kalibraci
        if (firstRun) {
            calibrationOffset(sensorEvent);
            return;
        }
//        txtXValue.setText(sensorEvent.sensor.getVendor());
        float[] pole = sensorEvent.values.clone();

        //HighPass filter by Google: (v podstatě se vezme zrychlení 9,8 a to se od toho odečte
        gravity[0] = alpha * gravity[0] + (1 - alpha) * getElement(pole, 0);
        gravity[1] = alpha * gravity[1] + (1 - alpha) * getElement(pole, 1);
        gravity[2] = alpha * gravity[2] + (1 - alpha) * getElement(pole, 2);

        linear_acceleration[0] = getElement(pole, 0) - gravity[0];
        linear_acceleration[1] = getElement(pole, 1) - gravity[1];
        linear_acceleration[2] = getElement(pole, 2) - gravity[2];

        zPoints.add(linear_acceleration[2]);

        calculateAverageAndPlotGraph();

    }

    private void calculateAverageAndPlotGraph() {
        graph.removeAllSeries();
        if (zPoints.size() > 75) zPoints.remove(0);

        //TODO ZPŘEHLEDNIT KOD
        //VYPOCET KLOUZAVEHO PRUMERU A JEHO VLOZENI DO GRAFU
        int length = zPoints.size();
        double[] dataForMovingAverage = new double[length]; //kvuli commons math, ktery berou jenom pole double
        DataPoint[] dataPoints = new DataPoint[length]; //pole bodu pro graf
        int i = 0;
        for (float z : zPoints) {
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

    private void calibrationOffset(SensorEvent sensorEvent) {
        float[] pole = sensorEvent.values.clone();
        firstRun = false;
    }

    public float getElement(float[] arrayOfFloat, int index) {

        return arrayOfFloat[index];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    //------------------------------------------------------------------------
    // POLOHA
    @SuppressLint("MissingPermission")
    private void startUpdates() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                txtLocation.setText(String.valueOf(location.getLatitude()));
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    private void stopUpdates() {
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case povoleni_gps: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "povoleni na gps udeleno", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(this, "povoleni na gps neudeleno", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case povoleni_operator: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "povoleni na operatora udeleno", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(this, "povoleni na operatora neudeleno", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void checkPermission() {

        //pokud neni opravneni na polohu od operatora nebo gps, zobraz dotaz
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        povoleni_gps);

            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        povoleni_operator);
            }
        } else {
            startUpdates();
        }
    }

}
