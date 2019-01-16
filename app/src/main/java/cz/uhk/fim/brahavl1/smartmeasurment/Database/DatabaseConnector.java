package cz.uhk.fim.brahavl1.smartmeasurment.Database;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.uhk.fim.brahavl1.smartmeasurment.Model.Ride;
import cz.uhk.fim.brahavl1.smartmeasurment.Model.Settings;

public class DatabaseConnector {

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = database.getReference("ride");

    public DatabaseConnector() {
    }

    public void saveRide(Ride ride) {
        DatabaseReference saveRef = myRef.child(String.valueOf(ride.getDate()));
        saveRef.setValue(ride);
    }

    public void removeRide(Ride ride){
        DatabaseReference delRef = myRef.child(String.valueOf(ride.getDate()));
       delRef.removeValue();

    }

    public void saveSettings(Double min, Double max, List<Ride> rideList) {
        myRef = database.getReference("settings");
        List<Date> listDate = new ArrayList<>();
        for (Ride ride : rideList){
            listDate.add(ride.getDate());
        }
        myRef.setValue(new Settings(min,max,listDate));
    }

}
