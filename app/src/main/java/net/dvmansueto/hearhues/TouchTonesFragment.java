package net.dvmansueto.hearhues;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Dave on 28/10/16.
 */

public class TouchTonesFragment extends Fragment {

    public TouchTonesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate( R.layout.fragment_touch_tones, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();


    }
}
