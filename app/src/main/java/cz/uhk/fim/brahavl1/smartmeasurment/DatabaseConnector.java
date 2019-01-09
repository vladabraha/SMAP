package cz.uhk.fim.brahavl1.smartmeasurment;

import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

class DatabaseConnector {

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = database.getReference("ride");
    DatabaseConnector() {
    }

    void saveRide(Ride ride) {
        DatabaseReference saveRef = myRef.child(String.valueOf(ride.getDate()));
        saveRef.setValue(ride);
    }

    void removeRide(Ride ride){
        DatabaseReference delRef = myRef.child(String.valueOf(ride.getDate()));
       delRef.removeValue();

    }
}
