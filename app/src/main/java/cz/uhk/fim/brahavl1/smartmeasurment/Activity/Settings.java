package cz.uhk.fim.brahavl1.smartmeasurment.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.List;

import cz.uhk.fim.brahavl1.smartmeasurment.R;

public class Settings extends AppCompatActivity implements SensorEventListener, NavigationView.OnNavigationItemSelectedListener {

    private TextView txtLocation;

    private DrawerLayout drawer;
    private NavigationView navigationView;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLocation = findViewById(R.id.txtPosition);

        Button btnPause = findViewById(R.id.btnPause);
        Button btnStart = findViewById(R.id.btnStart);
//        Button btnMapBox = findViewById(R.id.btnMapBoxActivity);

//        Button btnStartUpdates = findViewById(R.id.btnStartUpdates);
//        Button btnStopUpdates = findViewById(R.id.btnStopUpdates);

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

        //nastavení hodnoty podle poslední nastavené hodnoty (pokud byla, kdyby ne, tak je tam default value)
        SharedPreferences sharedPref = Settings.this.getSharedPreferences(getString(R.string.amountOfApproximation), MODE_PRIVATE);
        final int defaultValue = 4;
        final int amountOfApproximation = sharedPref.getInt(getString(R.string.amountOfApproximation), defaultValue); //zde se získají uložená data

        //seekbar pro nastavení míry aproximace
        TextView seekBarProgress = findViewById(R.id.txtAmountOfApproximation);
        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setMin(1);
        seekBar.setMax(50);
        seekBar.setProgress(amountOfApproximation);
        seekBarProgress.setText(String.valueOf(amountOfApproximation));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarProgress.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //uložení čísla pro HeatMap aktivity do sharedPreferences
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(getString(R.string.amountOfApproximation), seekBar.getProgress());
                editor.apply();
            }
        });

//        //spusteni aktualizace polohy
//        btnStartUpdates.setOnClickListener(view -> {
//            checkPermission();
//            locationUpdateStarted = true;
////                startUpdates();
//        });
//
//        btnStopUpdates.setOnClickListener(view -> {
//            if (locationUpdateStarted) {
//                stopUpdates();
//                locationUpdateStarted = false;
//            }
//        });


//        btnMapBox.setOnClickListener(view -> {
//            Intent mapBox = new Intent(Settings.this, MapBox.class);
//            startActivity(mapBox);
//        });


        //------------------------------------------------------------------------
        // NAVIGATION DRAWER MENU
        drawer = findViewById(R.id.drawer_layout);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true); //hodi do levyho horniho rohu definovanou ikkonu (hamburger menu)
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(3).setChecked(true);
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            if (!drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.openDrawer(GravityCompat.START);
                return true;
            } else {
                drawer.closeDrawers();
                return false;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_start_measurement) {
            Intent notificationIntent = new Intent(Settings.this, HereMapsMeasurement.class);
            startActivityForResult(notificationIntent, 1);
        } else if (id == R.id.nav_overview) {
            Intent rideOverview = new Intent(this, RideOverview.class);
            startActivityForResult(rideOverview, 2);
        } else if (id == R.id.nav_heat_map) {
            Intent rideOverview = new Intent(this, HeatMap.class);
            startActivityForResult(rideOverview, 3);
        } else if (id == R.id.nav_settings) {

        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (int i = 0; i < navigationView.getMenu().size(); i++) {
            navigationView.getMenu().getItem(i).setChecked(false);
        }
        navigationView.getMenu().getItem(3).setChecked(true);
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
