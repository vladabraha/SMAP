package cz.uhk.fim.brahavl1.smartmeasurment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import timber.log.Timber;

public class SaveDialogFragment extends DialogFragment {

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeDialogListener {
        void onDialogPositiveClick(DialogFragment dialog, String rideName);
//        public void onDialogNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) context;
        } catch (ClassCastException e) {
            Timber.tag("error").d("acitivy doesnt impement intrerface");
        }
    }

    @SuppressLint("InflateParams")
    @Override
    @NotNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_save_ride, null))
                .setPositiveButton("save ride", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Dialog d = (Dialog) dialog;
                        EditText editRideName = (EditText) d.findViewById(R.id.textRideName);
                        String rideName = String.valueOf(editRideName.getText());
                        mListener.onDialogPositiveClick(SaveDialogFragment.this, rideName);
                    }
                })
                .setNegativeButton("cancel", (dialog, id) -> {

                });

        return builder.create();
    }

}
