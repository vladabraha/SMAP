package cz.uhk.fim.brahavl1.smartmeasurment;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.List;

public class RideOverview extends AppCompatActivity implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private DatabaseConnector databaseConnector = new DatabaseConnector();

    private List<Ride> rideList = new ArrayList<>();

    private LinearLayout linearLayout;

    boolean finishedCalculation = false;
    List<Double> minMaxOfAllRide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_overview);

        recyclerView = findViewById(R.id.ride_list_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);

        //registruje touchHelper na recyclerView
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        //vytažení layoutu pro snackbar (aby vedel kde se ma vytvorit)
        linearLayout = findViewById(R.id.linearLayoutRideOverView);


        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("ride");

        // Read from the database
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    rideList.add(postSnapshot.getValue(Ride.class));

                    // specify an adapter (see also next example)
                    mAdapter = new RideListAdapter(rideList);
                    recyclerView.setAdapter(mAdapter);
                    minMaxOfAllRide = new ComputeDataForHeatMap().doInBackground(rideList);
//                    Log.d("hoo", "vysledek je " + String.valueOf(minMaxOfAllRide.get(0)) + " " + String.valueOf(minMaxOfAllRide.get(1)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
            }
        });

        //prida RecyclerTouchListener - posloucha události na recycleru - mělo by umožnovat poslouchat libovolny recycler
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Ride ride = rideList.get(position);
//                Toast.makeText(getApplicationContext(), ride.getName() + " is selected!", Toast.LENGTH_SHORT).show();

                Intent rideDetail = new Intent(RideOverview.this, RideDetail.class);
                rideDetail.putExtra("ride",ride);
                rideDetail.putExtra("min",minMaxOfAllRide.get(0));
                rideDetail.putExtra("max",minMaxOfAllRide.get(1));
                startActivity(rideDetail);
            }

            @Override
            public void onLongClick(View view, int position) {
//                Ride ride = rideList.get(position);
//                Toast.makeText(getApplicationContext(), ride.getName() + " is selected!", Toast.LENGTH_SHORT).show();
            }
        }));

    }

    /** implementace rozhrani z RecyclerTouchHelperu
     * callback when recycler view is swiped
     * item will be removed on swiped
     * undo option will be provided in snackbar to restore the item
     */
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof RideListAdapter.RideListViewHolder) {
            // get the removed item name to display it in snack bar
            String name = rideList.get(viewHolder.getAdapterPosition()).getName();

            // backup of removed item for undo purpose
            final Ride deletedItem = rideList.get(viewHolder.getAdapterPosition());
            final int deletedIndex = viewHolder.getAdapterPosition();

            // remove the item from recycler view - po provedeni gesta
            ((RideListAdapter) mAdapter).removeItem(viewHolder.getAdapterPosition());


            // showing snack bar with Undo option
            Snackbar snackbar = Snackbar
                    .make(linearLayout, name + " ride has been deleted", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", view -> {
                 // tady se udělá akce po kliknuti na tlačítko undo
                ((RideListAdapter) mAdapter).restoreItem(deletedItem, deletedIndex);
                databaseConnector.saveRide(deletedItem);

            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
//            Log.d("hoo",String.valueOf(viewHolder.getAdapterPosition()));
            databaseConnector.removeRide(deletedItem);
        }
    }

    //TODO HLEDANI MAXIMA A MINIMA V PŘÍPADĚ VELKÉHO MNOŽSTVÍ DAT OPTIMALIZOVAT!!  A NEFUNGUJE
    //co ma prijit, progress a co se ma vratit z AsyncTasku
    private class ComputeDataForHeatMap extends AsyncTask<List<Ride>, Integer, List<Double>> {

        //hledame 90 percentil,
        @Override
        protected List<Double> doInBackground(List<Ride>... lists) {
            int accelerometrDataSize = 0;
            for (Ride ride : rideList){
                accelerometrDataSize += ride.getAccelerometerData().size();
            }
            double[] points = new double[accelerometrDataSize];
            int increment = 0;
            for (Ride ride: rideList){
                for (Float point : ride.getAccelerometerData()){
                    points[increment] = (Double.valueOf(point));
                    increment++;
                    float progress = (increment / accelerometrDataSize) * 100;
                    Integer i = new Integer(Math.round(progress));
                    publishProgress(i);
                }
            }

            double maximum = StatUtils.max(points);
            double minimun = StatUtils.min(points);
            List<Double> list = new ArrayList<>();
            list.add(minimun);
            list.add(maximum);

            return list;
        }

        protected void onProgressUpdate(Integer... progress) {

//            Log.d("hoo", "tak už je hotovo " + String.valueOf(progress[0]));
        }

        protected void onPostExecute(List<Double> list) {
            finishedCalculation = true;
            Log.d("hoo", "tak už");
        }



    }


}

