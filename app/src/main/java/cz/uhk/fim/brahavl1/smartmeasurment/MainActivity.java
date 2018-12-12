package cz.uhk.fim.brahavl1.smartmeasurment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView txtXValue;
    private TextView txtYValue;
    private TextView txtZValue;

    private TextView txtLocation;

    private SensorManager mSensorManager;
    private Sensor accelerometer;

    private boolean firstRun = true;
    private float offsetX;
    private float offsetY;
    private float offsetZ;

    private float xAverage = 0;
    private float yAverage = 0;
    private float zAverage = 0;
    private int iteration = 0;

    private Button btnStart;
    private Button btnPause;
    private Button btnNew;

    private Button btnStartUpdates;
    private Button btnStopUpdates;

    private List<Float> zPoints = new ArrayList<Float>();

    private LocationManager locationManager;
    private LocationListener locationListener;

    private static final int povoleni_operator = 0;
    private static final int povoleni_gps = 1;

    private static final int ONGOING_NOTIFICATION_ID = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtXValue = findViewById(R.id.txtXValue);
        txtYValue = findViewById(R.id.txtYValue);
        txtZValue = findViewById(R.id.txtZValue);

        txtLocation = findViewById(R.id.txtPosition);

        btnPause = findViewById(R.id.btnPause);
        btnStart = findViewById(R.id.btnStart);
        btnNew = findViewById(R.id.btnNew);

        btnStartUpdates = findViewById(R.id.btnStartUpdates);
        btnStopUpdates = findViewById(R.id.btnStopUpdates);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onResume();
                xAverage = 0;
                yAverage = 0;
                zAverage = 0;
                iteration = 0;

            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPause();
                calculateAverageAndPlotGraph();
            }
        });



        btnStartUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermission();
//                startUpdates();
            }
        });


        btnStopUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopUpdates();
            }
        });

        btnNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent notificationIntent = new Intent(MainActivity.this, PostionGoogle.class);
//                PendingIntent pendingIntent =
//                        PendingIntent.getActivity(MainActivity.this, 0, notificationIntent, 0);
//                Notification notification =
//                        new Notification.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
//                                .setContentTitle(R.string.notification_title)
//                                .setContentText(R.string.notification_message)
//                                .setSmallIcon(R.drawable.notification_car)
//                                .setContentIntent(pendingIntent)
//                                .setTicker(R.string.ticker_text)
//                                .build();

//                startForeground(ONGOING_NOTIFICATION_ID, notification);
//                startActivity(intent);
            }
        });
//        Log.d("TAG", "v locmodelu je ");


    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //při prvním spuštění se načte aktuální hodnota (offset)  pro kalibraci
        if (firstRun) {
            calibrationOffset(sensorEvent);
            return;
        }
//        txtXValue.setText(sensorEvent.sensor.getVendor());
        float[] pole = sensorEvent.values.clone();

        //tady dostáváme aktuální hodnoty po odečtení offsetu
        float xValue = getElement(pole, 0) - offsetX;
        float yValue = getElement(pole, 1) - offsetY;
        float zValue = getElement(pole, 2) - offsetZ;

        xAverage = xAverage + xValue;
        yAverage = yAverage + yValue;
        zAverage = zAverage + zValue;
        iteration++;


        txtXValue.setText(String.valueOf(xValue));
        txtYValue.setText(String.valueOf(yValue));
        txtZValue.setText(String.valueOf(zValue));

        zPoints.add(zValue);


    }

    private void calculateAverageAndPlotGraph() {
        xAverage = xAverage / (float) iteration;
        yAverage = yAverage / (float) iteration;
        zAverage = zAverage / (float) iteration;

        txtXValue.setText(String.valueOf(xAverage));
        txtYValue.setText(String.valueOf(yAverage));
        txtZValue.setText(String.valueOf(zAverage));

        int length = zPoints.size();
        DataPoint[] dataPoints = new DataPoint[length];
        int i = 0;
        for (float z : zPoints) {
            dataPoints[i] = new DataPoint(i, z);
            i++;
        }


        GraphView graph = (GraphView) findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        graph.addSeries(series);


    }

    private void calibrationOffset(SensorEvent sensorEvent) {
        float[] pole = sensorEvent.values.clone();
        offsetX = getElement(pole, 0);
        offsetY = getElement(pole, 1);
        offsetZ = getElement(pole, 2);
        firstRun = false;
        return;
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
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


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

    private void stopUpdates(){
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case povoleni_gps : {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "povoleni na gps udeleno", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(this, "povoleni na gps neudeleno", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case  povoleni_operator: {
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

    private void checkPermission(){

        //pokud neni opravneni na polohu od operatora nebo gps, zobraz dotaz
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED )  {

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
        }else{
            startUpdates();
        }
    }

}
