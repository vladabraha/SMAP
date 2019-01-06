package cz.uhk.fim.brahavl1.smartmeasurment;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.here.android.mpa.common.GeoCoordinate;

import java.util.ArrayList;
import java.util.List;

public class ForegroundService extends Service {
    int mStartMode;       // indicates how to behave if the service is killed
    boolean mAllowRebind; // indicates whether onRebind should be used
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest = new LocationRequest();

    private List<GeoCoordinate> points = new ArrayList<>();

    Callbacks activity; //tímhle se můžou volat metody v interfacu

    public ForegroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        ForegroundService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ForegroundService.this;
        }
    }

    @Override
    public void onCreate() {
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
                    points.add(new GeoCoordinate(location.getLatitude(), location.getLongitude()));
                    activity.updateClient(points);
                }
            }
        };

        //inicializace provideru - bez toho neběží aktualizace polohy!
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        startForegroundService();
        createNotificationChannel();
        return mStartMode;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
        stopLocationUpdates();
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        stopLocationUpdates();
    }

    public void createLocationRequest() {

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

        task.addOnSuccessListener(locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // Tady se to rozjede
            startLocationUpdates();
        });
    }

    private void startLocationUpdates() {
        if (mFusedLocationClient != null && mLocationCallback != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null);
        }
    }

    private void stopLocationUpdates() {
        if (mFusedLocationClient != null)
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * slouží pro implementaci rozhrani - servisa má refereci na aktivitu, kterou si připojí
     *
     * @param activity
     */
    //Here Activity register to the service as Callbacks client
    public void registerClient(Activity activity) {
        this.activity = (Callbacks) activity;
    }


    /**
     * tady se definují metody, které se můžou odstud volat v domácí aktivitě
     */
    //callbacks interface for communication with service clients!
    public interface Callbacks {
        public void updateClient(List<GeoCoordinate> points);
    }

    /* Used to build and start foreground service. */
    private void startForegroundService() {
        createNotificationChannel(); //tohle se doporučuje volat jak jen to jde - kdyžtak neudělá nic

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent1 = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        //tady je důležité hodit správné channel_id pro zobrazení notifikace
        Notification notification = new NotificationCompat.Builder(this, "My_channel_ID")
                .setContentTitle("Smart Measurement")
                .setContentText("právě běží aktualizace polohy")
                .setSmallIcon(R.drawable.ic_gps_fixed_black_24dp)
                .setContentIntent(pendingIntent1)
                .setPriority(NotificationCompat.PRIORITY_HIGH) //NUTNO PRO PODPORU NOTIFIKACE NA ANDROID 7 A NIŽŠÍ
                .build();

        startForeground(1, notification);

    }

    /**
     * Vytvori channel pro Android 8.0+ (slouží pro službu běžící na popředí
     */
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Spusteni trackovani";
            String description = "smartmeasurment";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("My_channel_ID", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

    }

    /**
     * zastaví službu - musí se volat při ukončení aktivity, jinak jede služba na věky věkků
     */
    public void stopService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }
}
