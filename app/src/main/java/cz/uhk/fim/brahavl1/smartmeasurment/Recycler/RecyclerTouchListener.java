package cz.uhk.fim.brahavl1.smartmeasurment.Recycler;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

    private GestureDetector gestureDetector;
    private ClickListener clickListener;

    public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {
        this.clickListener = clickListener;

        //listener - z onInterceptTouchEvent prijde motin event a podle něho se rozpozna, jestli se jedna o sigle tap, nebo long press
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (child != null && clickListener != null) {
                    clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child));
                }
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (child != null && clickListener != null) {
                    clickListener.onClick(child, recyclerView.getChildAdapterPosition(child));
                }
                return true;
            }
        });
    }

    /**
     * onInterceptToucheEvent se zavola vzdycky pri kliknuti do Recycleru
     * gesture detector preda udalost vyse a na zaklade motion eventu pak je rozhodnutu o kterou aktivitu se jedna
     *
     * @param recyclerView recycler, na kterym se má rozhodnout (Univerzalni, funguje snad na všechny recyclery!)
     * @param motionEvent  (preda udalost, kterou prstem uzivatel provedl
     * @return vraci, jestli se na neco kliklo, nevyuzijeme
     */
    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent motionEvent) {
        gestureDetector.onTouchEvent(motionEvent);
        return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent motionEvent) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean b) {

    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }
}
