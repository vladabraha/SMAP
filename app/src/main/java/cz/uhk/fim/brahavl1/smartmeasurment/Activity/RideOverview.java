package cz.uhk.fim.brahavl1.smartmeasurment.Activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.MenuItem;
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

import cz.uhk.fim.brahavl1.smartmeasurment.Database.DatabaseConnector;
import cz.uhk.fim.brahavl1.smartmeasurment.Model.Ride;
import cz.uhk.fim.brahavl1.smartmeasurment.Model.Settings;
import cz.uhk.fim.brahavl1.smartmeasurment.R;
import cz.uhk.fim.brahavl1.smartmeasurment.Recycler.RecyclerItemTouchHelper;
import cz.uhk.fim.brahavl1.smartmeasurment.Recycler.RecyclerTouchListener;
import cz.uhk.fim.brahavl1.smartmeasurment.Recycler.RideListAdapter;

public class RideOverview extends AppCompatActivity implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener, NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private DatabaseConnector databaseConnector = new DatabaseConnector();

    private List<Ride> rideList = new ArrayList<>();
    private Settings settings;

    private LinearLayout linearLayout;

    boolean finishedCalculation = false;
    List<Double> minMaxOfAllRide;

    private DrawerLayout drawer;
    private NavigationView navigationView;

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
        DatabaseReference myRef = database.getReference("Settings");

        // Read from the database
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    settings = postSnapshot.getValue(Settings.class);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
            }
        });


        myRef = database.getReference("ride");

        // Read from the database
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                boolean isSettingsSame = true;
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    rideList.add(postSnapshot.getValue(Ride.class));

                    // specify an adapter (see also next example)
                    mAdapter = new RideListAdapter(rideList);
                    recyclerView.setAdapter(mAdapter);

                    //Pro vyhledání maxima a minima v novém datasetu (slouží pro optimalizaci, aby se nemusely procházet všechna data)
                    isSettingsSame = true;
//                    List <Ride> missingRideInMinMaxCompute = new ArrayList<>();
                    if (settings != null){
                        for (Ride ride : rideList){
                            if (!settings.getListOfLastRide().contains(ride.getDate())){
                                isSettingsSame = false;
//                                missingRideInMinMaxCompute.add(ride);
                            }else{
                                isSettingsSame = true;
                            }
                        }
                    }else{
                        isSettingsSame = false;
                    }
                }
                if (!isSettingsSame){
                    minMaxOfAllRide = new ComputeDataForHeatMap().doInBackground(rideList);
                }else {
                    minMaxOfAllRide.add(settings.getMinOfAllRides());
                    minMaxOfAllRide.add(settings.getMaxOfAllRides());
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

                if (settings != null){
                    double historyMax = settings.getMaxOfAllRides();
                    if (historyMax > minMaxOfAllRide.get(1)){
                        minMaxOfAllRide.set(0, settings.getMinOfAllRides());
                        minMaxOfAllRide.set(1, settings.getMaxOfAllRides());
                    }
                }
                databaseConnector.saveSettings(minMaxOfAllRide.get(0), minMaxOfAllRide.get(1), rideList);
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
        //------------------------------------------------------------------------
        // NAVIGATION DRAWER MENU
        drawer = findViewById(R.id.drawer_layout);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true); //hodi do levyho horniho rohu definovanou ikkonu (hamburger menu)
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(1).setChecked(true);
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
            Intent notificationIntent = new Intent(this, HereMapsMeasurement.class);
            startActivityForResult(notificationIntent, 1);
        } else if (id == R.id.nav_overview) {
            Intent rideOverview = new Intent(this, RideOverview.class);
            startActivityForResult(rideOverview, 2);
        } else if (id == R.id.nav_settings) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        navigationView.getMenu().getItem(1).setChecked(true);
    }

    //---------------------------------------------------------------------------------------------
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

//            List<Ride>[] ridesForCompute = lists;
//
//            //hledame 90 percentil
//            List<Ride> utilList = new ArrayList(Arrays.asList(ridesForCompute));
//
//            List<Float> accelData = new ArrayList<>();
//            for (Ride ride : utilList){
//                accelData.addAll(ride.getAccelerometerData());
//            }
//            Collections.sort(accelData);

            double minimum = StatUtils.min(points);
            double maximum = StatUtils.max(points);
//           double maximum = accelData.get((int)((accelData.size() - 1) * 0.8));


            List<Double> list = new ArrayList<>();
            list.add(minimum);
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

