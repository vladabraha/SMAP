package cz.uhk.fim.brahavl1.smartmeasurment.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;


import java.util.ArrayList;
import java.util.List;

import cz.uhk.fim.brahavl1.smartmeasurment.R;
import timber.log.Timber;


public class MapBox extends AppCompatActivity {

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    private boolean mRequestingLocationUpdates = true;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    private LocationRequest mLocationRequest = new LocationRequest();

    private MapView mapView;
    private List<Point> routeCoordinates;
    public MapboxMap map;

    private Button btnStartUpdate;
    private Button btnStopUpdate;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, "pk.eyJ1IjoidGVzdGVyMjQiLCJhIjoiY2pweDJ5Nm53MDZobzQycGpweWwzZHB2diJ9.bU0SVPkkHf1iQT8tte64xQ");
        setContentView(R.layout.activity_map_box);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        routeCoordinates = new ArrayList<>();

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                // MAPBOX UMI VYKRESLIT MAPU JENOM KDYŽ SE PUSTI - UPLNĚ DEBILNI
            }
        });

        btnStartUpdate = findViewById(R.id.startMapBoxUpdate);
        btnStopUpdate = findViewById(R.id.stopMapBoxUpdate);


        btnStartUpdate.setOnClickListener(view ->  {
                createLocationRequest();
        });

        btnStopUpdate.setOnClickListener(view ->  {
                stopLocationUpdates();
        });


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
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
        //tohle natahne posledni polohu - hodi se pro zamereni mapy na zacatku
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.

                if (location != null) {
                    // Logic to handle location object
                    routeCoordinates.add(Point.fromLngLat(location.getLongitude(), location.getLatitude()));
                    drawLineOnMap(routeCoordinates);
//                    Toast.makeText(this, "text", Toast.LENGTH_SHORT).show();
                }
            }

        });

        //CallBack - sem prijde zpatko poloha, kdyz se zmeni, takovej listener
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    routeCoordinates.add(Point.fromLngLat(location.getLongitude(), location.getLatitude()));
                }
               drawLineOnMap(routeCoordinates);
            }
        };
    }

    /**
     * Draw line on map
     *
     * @param routeCoordinates The route to be drawn in the map's LineLayer that was set up above.
     */
    private void drawLineOnMap(List<Point> routeCoordinates) {
        LatLng[] pointsArray = new LatLng[routeCoordinates.size()];
        for (int i = 0; i < routeCoordinates.size(); i++) {
            pointsArray[i] = new LatLng(routeCoordinates.get(i).latitude(), routeCoordinates.get(i).longitude());
        }


        if (map != null){
            Timber.tag("poloha").d("uz neni null");
            map.addPolyline(new PolylineOptions()
                    .add(pointsArray)
                    .color(Color.parseColor("#8a8acb"))
                    .width(4));
        }

    }
    protected void createLocationRequest() {

        //vytvoří se požadavek na polohu
        mLocationRequest.setInterval(500);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //tady se přihodí co se chce za pravnění
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        //kontrola zda jsou opravnění nastavena
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse ->  {
                         // All location settings are satisfied. The client can initialize
                // location requests here.
                // Tady se to rozjede
                startLocationUpdates();

        });

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
                        resolvable.startResolutionForResult(MapBox.this,
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
        mapView.onResume();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(MapBox.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            if (mFusedLocationClient != null) {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                        mLocationCallback,
                        null);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        mapView.onPause();
    }

    private void stopLocationUpdates() {
        if (mFusedLocationClient != null)
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

}
