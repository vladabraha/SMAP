package cz.uhk.fim.brahavl1.smartmeasurment;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class RideListAdapter extends RecyclerView.Adapter<RideListAdapter.RideListViewHolder> {

    private List<Ride> rideList;

    // Provide a reference to the views for each data item
// Complex data items may need more than one view per item, and
// you provide access to all the views for a data item in a view holder
    public static class RideListViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView rideName, date;

        public RideListViewHolder(View itemView) {
            super(itemView);
            rideName = itemView.findViewById(R.id.text_name);
            date = itemView.findViewById(R.id.text_date);
        }

    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public RideListAdapter(List<Ride> rideList) {
        this.rideList = rideList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RideListViewHolder onCreateViewHolder(ViewGroup parent,
                                           int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ride_list_recycler_row, parent, false);

        return new RideListViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RideListViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Ride ride = rideList.get(position);
        holder.rideName.setText(ride.getName());
        holder.date.setText(String.valueOf(ride.getDate()));

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return rideList.size();
    }
}
