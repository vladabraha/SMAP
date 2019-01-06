package cz.uhk.fim.brahavl1.smartmeasurment;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

class DatabaseConnector{

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();


    DatabaseConnector() {

    }

     void writeToDatabase(){
        // Write a message to the database
//        FirebaseDatabase database = FirebaseDatabase.getInstance();
//        DatabaseReference myRef = database.getReference("message");

//        myRef.setValue("Hello, World!");
//        mDatabase.child("users").child(userId).setValue(user);
        DatabaseReference myRef = database.getReference("message");
        DatabaseReference usersRef = myRef.child("users");

        usersRef.setValue("test");
    }


    void saveRide(Ride ride){

        DatabaseReference myRef = database.getReference("ride");
        DatabaseReference saveRef = myRef.child(String.valueOf(ride.getDate()));

//        Log.d("bagr", ride.getName());
//        Log.d("bagr", String.valueOf(ride.getAccelerometerData().size()));
//        Log.d("bagr", String.valueOf(ride.getTestPoints().size()));
//        Log.d("bagr", String.valueOf(ride.getDate()));
        saveRef.setValue(ride);

    }

    void readFromDatabase(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");
        // Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d("TAG", "Value is: " + value);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
            }
        });
    }
}
