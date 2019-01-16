package cz.uhk.fim.brahavl1.smartmeasurment.Activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolyline;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapPolyline;

import java.util.ArrayList;
import java.util.List;

import cz.uhk.fim.brahavl1.smartmeasurment.Database.DatabaseConnector;
import cz.uhk.fim.brahavl1.smartmeasurment.Fragment.SaveDialogFragment;
import cz.uhk.fim.brahavl1.smartmeasurment.Model.Coordinate;
import cz.uhk.fim.brahavl1.smartmeasurment.Model.Ride;
import cz.uhk.fim.brahavl1.smartmeasurment.R;
import cz.uhk.fim.brahavl1.smartmeasurment.Service.ForegroundService;

public class PositionGoogle extends AppCompatActivity implements ForegroundService.Callbacks, SaveDialogFragment.NoticeDialogListener {

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    private Button btnStartLocationUpdate;

    //Array bodu, ktere se budou vykreslovat na grafu
    private List<Float> zPointsAverage = new ArrayList<>(); //v tomhle poli budou průměry k jednotlivým rámcům z gps

    private TextView txtLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest = new LocationRequest();

    private Map map;

    private List<GeoCoordinate> locationPoints = new ArrayList<>();
    private boolean locationSettingsSatisfied = false;

    ForegroundService mService;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postion);

        txtLocation = findViewById(R.id.txtLocation);
        btnStartLocationUpdate = findViewById(R.id.btnStartLocationUpdates);

        //MAPA
        //---------------------------------------------------------------------------------------------------
        final MapFragment mapFragment = (MapFragment)
                getFragmentManager().findFragmentById(R.id.mapfragment);

        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // now the map is ready to be used
                    map = mapFragment.getMap();
                    map.setCenter(new GeoCoordinate(50.2105358,
                            15.8343583), Map.Animation.NONE);
                    // ...
                } else {
                    Toast.makeText(PositionGoogle.this, error.getDetails(), Toast.LENGTH_LONG).show();
                }
            }
        });

        createLocationRequest();

        btnStartLocationUpdate.setOnClickListener(view -> {
            //Pro přepínání funkce tlačítka - jinak by musely bejt 2
            if (btnStartLocationUpdate.getText() == "save the ride" && locationSettingsSatisfied){
                DialogFragment dialog = new SaveDialogFragment();
                dialog.show(getSupportFragmentManager(), "SaveDialogFragment");
            }else{
                createLocationRequestForForegroundService(); //spusti location update ve foreground service
            }

        });

        //FOREGROUND SEERVICE
        //---------------------------------------------------------------------------------------------------
        //spustí foreground service
        Intent intent = new Intent(this, ForegroundService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        ContextCompat.startForegroundService(this, intent);

        //provede dotaz na opravenni a pripadne se zepta o povoleni
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(PositionGoogle.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        //tohle natahne posledni polohu - hodi se pro zamereni mapy na zacatku
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null && map != null) {
                    // Logic to handle location object
//                    txtLocation.setText(String.valueOf(location.getLongitude()));
                    map.setZoomLevel(map.getMaxZoomLevel());
                    if (map != null)
                        map.setCenter(new GeoCoordinate(location.getLatitude(), location.getLongitude()), Map.Animation.LINEAR);
                }
            }
        });
    }


    private void createLocationRequestForForegroundService() {
        //vytvoří se požadavek na polohu
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //tady se přihodí co se chce za pravnění
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        //dotaz na oprávnění
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // Tady se to rozjede
             mService.createLocationRequest();
            locationSettingsSatisfied = true;
            btnStartLocationUpdate.setText("save the ride");
        });

        //tady se to zeptá na oprávnění, pokud nejsou k dispozici
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(PositionGoogle.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    /**
     * dotaz na to, zda není získávání polohy v režimu úsporný režim (nebo vypnuto)
     * pokud je, tak se zeptá na povolení o přepnutí na kombinovaný režim (gps + geolocation)
     */
    protected void createLocationRequest() {
        //vytvoří se požadavek na polohu
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //tady se přihodí co se chce za pravnění
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        //kontrola zda jsou opravnění nastavena
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        //pokud bylo všem dotazům vyhověno, může se to rozjet
        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // Tady se to rozjede
//            startLocationUpdates();
        });

        //tady se to zeptá na oprávnění, pokud nejsou k dispozici
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(PositionGoogle.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }

        });

    }

    @Override
    protected void onResume() {
        super.onResume();
//        startLocationUpdates();
//        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * spusti looper na dotaz na aktualni polohu, je nutno v oncreate nejdriv nadefinovat, jak má update probihat
     */
    private void startLocationUpdates() {
        //tohle se zepta na opravnění k pozici, pokud neni
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(PositionGoogle.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//        } else {
//            if (mFusedLocationClient != null && mLocationCallback != null) {
//                mFusedLocationClient.requestLocationUpdates(mLocationRequest,
//                        mLocationCallback,
//                        null);
//            }
//        }
    }


    @Override
    protected void onPause() {
        super.onPause();
//        stopLocationUpdates();
//        mSensorManager.unregisterListener(this);
    }

    private void stopLocationUpdates() {
//        if (mFusedLocationClient != null)
//            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
//        mSensorManager.unregisterListener(this);
    }


    @Override
    protected void onStop() {
        super.onStop();
//        unbindService(mConnection);
//        mBound = false;
    }

    /**
     * tohle slouží ke k připojení servisy - je pak možné volat její veřejné metody (a řídit ji)
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ForegroundService.LocalBinder binder = (ForegroundService.LocalBinder) service;
            mService = binder.getService();
            mService.registerClient(PositionGoogle.this); //Activity register in the service as client for callabcks!
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    /**
     * Implementace rozhraní pro komunikaci ze Servisy - tuhle metodu volame ze servisy na predavani dat
     *
     * @param points body ktere prijdou ze servisy
     */
    @Override
    public void updateClient(List<GeoCoordinate> points, List<Float> zPointsAverage ) {
        if (map != null) {
            txtLocation.setText(String.valueOf(points.get(points.size() - 1).getLongitude()));
//            locationPoints.add(new GeoCoordinate(location.getLatitude(), location.getLongitude(), 10));
            if (points.size() > 2) {
                this.locationPoints = points;
                this.zPointsAverage = zPointsAverage;
                GeoPolyline polyline = new GeoPolyline(points);
                MapPolyline mapPolyline = new MapPolyline(polyline);
                mapPolyline.setLineColor(Color.RED);
                mapPolyline.setLineWidth(12);
                map.addMapObject(mapPolyline);
            }
        }
    }


    /**
     * při stisknuti tlačítka zpět se zastavi služba na popředí
     */
    @Override
    public void onBackPressed() {
        // code here to show dialog
        super.onBackPressed();  // optional depending on your needs
        mService.stopService();
    }


    /**
     * Implementace rozhraní pro komunikaci s dialogem (vrátí zpátky text z dialogu)
     * @param dialog
     * @param rideName v tomhle přijde zpátky text z dialogu
     */

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String rideName) {
        mService.stopService(); //Nutne k ukonceni foreground service!

        //tohle je tady protoze Firebase potrebuje prazdnej konstruktor a ten GeoCoordinate bohužel nema
        List<Coordinate> coordinates = new ArrayList<>();
//        stopLocationUpdates();
        for (GeoCoordinate geoCoordinate: locationPoints){
            coordinates.add(new Coordinate(geoCoordinate.getLongitude(), geoCoordinate.getLatitude()));
        }
        if (coordinates.size() == 0){
            Toast.makeText(this,"not a single value has been recorded yet", Toast.LENGTH_LONG).show();
            return;
        }
        Ride ride = new Ride(rideName, coordinates, zPointsAverage);
        DatabaseConnector databaseConnector = new DatabaseConnector();
        databaseConnector.saveRide(ride);
        Toast.makeText(this, "succesfuly saved into database", Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * vrátí prvek z pole
     * @param arrayOfFloat pole ze kterho chceme získat data
     * @param index        pozice ze které chceme získat prvek
     * @return vrátí prvek na daném místě
     */
//    public float getElement(float[] arrayOfFloat, int index) {
//
//        return arrayOfFloat[index];
//    }
}
