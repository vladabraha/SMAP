package cz.uhk.fim.brahavl1.smartmeasurment.Fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cz.uhk.fim.brahavl1.smartmeasurment.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class HereMapsFragment extends Fragment {


    public HereMapsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_here_maps, container, false);
    }

}
