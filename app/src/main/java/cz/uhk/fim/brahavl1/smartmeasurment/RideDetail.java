package cz.uhk.fim.brahavl1.smartmeasurment;

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

import java.util.ArrayList;
import java.util.List;

public class RideDetail extends AppCompatActivity {

    private Map map;
    private List<GeoCoordinate> locationPoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_detail);

        Ride ride = (Ride) getIntent().getExtras().get("ride");
        try{
            if (ride.getLocationPoints() != null){
                for (Coordinate coordinate : ride.getLocationPoints()){
//                    Log.d("hoo","getLatitude je " + coordinate.getLatitude());
//                    Log.d("hoo","getLongitude je " +  coordinate.getLongtitude());
                    locationPoints.add(new GeoCoordinate(coordinate.getLatitude(), coordinate.getLongtitude(), 10));
                }
            }
        }catch (NullPointerException e){
            Toast.makeText(this,"tento zaznam nema data o jizde", Toast.LENGTH_LONG).show();
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

                        map.setCenter(new GeoCoordinate(locationPoints.get(0).getLatitude(),
                                locationPoints.get(0).getLongitude()), Map.Animation.NONE);
//                        Log.d("hoo","getLatitude je " + locationPoints.get(0).getLatitude());
//                        Log.d("hoo","getLongitude je " +  locationPoints.get(0).getLongitude());
                        map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);

                        GeoPolyline polyline = new GeoPolyline(locationPoints);
                        MapPolyline mapPolyline = new MapPolyline(polyline);
                        mapPolyline.setLineColor(Color.RED);
                        mapPolyline.setLineWidth(12);
                        map.addMapObject(mapPolyline);
                    }
                } else {
                    System.out.println("ERROR: Cannot initialize Map Fragment");
                }
            }
        });


    }
}
