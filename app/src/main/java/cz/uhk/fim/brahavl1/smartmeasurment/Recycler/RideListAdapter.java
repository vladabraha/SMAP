package cz.uhk.fim.brahavl1.smartmeasurment.Recycler;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import cz.uhk.fim.brahavl1.smartmeasurment.Model.Ride;
import cz.uhk.fim.brahavl1.smartmeasurment.R;

public class RideListAdapter extends RecyclerView.Adapter<RideListAdapter.RideListViewHolder> {

    private List<Ride> rideList; //seznam, ktery se bude vykreslovat


    // zde se natahnout jednotlivy prvky, ktery se budou plnit datama - upravit tedy podle obsahu radku jednotliveho recyclerview
    public static class RideListViewHolder extends RecyclerView.ViewHolder {
        // natahnou se data z jednotliveho xml
        public TextView rideName, date;
        public RelativeLayout viewBackground, viewForeground;

        public RideListViewHolder(View itemView) {
            super(itemView);
            rideName = itemView.findViewById(R.id.text_name);
            date = itemView.findViewById(R.id.text_date);
            viewBackground = itemView.findViewById(R.id.view_background);
            viewForeground = itemView.findViewById(R.id.view_foreground);
        }

    }

    // konstruktor na predani dat
    public RideListAdapter(List<Ride> rideList) {
        this.rideList = rideList;
    }

    // Zde se definuje, kterej recycler se ma naplnit daty
    @Override
    public RideListViewHolder onCreateViewHolder(ViewGroup parent,
                                                 int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ride_list_recycler_row, parent, false);

        return new RideListViewHolder(v);
    }

    // Zde probíhá naplnění jednotlivého řádku z seznamu, který jsme z konstruktoru zíksali
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

    public void removeItem(int position) {
        rideList.remove(position);
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(position);
    }

    public void restoreItem(Ride ride, int position) {
        rideList.add(position, ride);
        // notify item added by position
        notifyItemInserted(position);
    }
//zde muze byt jeste interface pro komunikaci s aktivitou (napr. pokud by bylo v radku recyclerview vice tlacitek apod.

}
