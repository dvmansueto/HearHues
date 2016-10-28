package net.dvmansueto.hearhues;


import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class TinkerFragment extends Fragment {

    private static final String TAG = "TinkerFragment";

    public TinkerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LocView locView = (LocView) getActivity().findViewById( R.id.tinker_loc_view);
        locView.setLocViewListener(new LocView.LocViewListener() {
            @Override
            public void newFrequency(double frequency) {
                Log.d( TAG, "f: " + Double.toString( frequency));
            }
            @Override
            public void newAmplitude(double amplitude) {
                Log.d( TAG, "a: " + Double.toString( amplitude));
            }
        });
    }

    @Override
    public View onCreateView( LayoutInflater inflater,
                              ViewGroup container,
                              Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tinker, container, false);
    }


    @Override
    public void onResume() {
        super.onResume();
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setTitle(R.string.fragment_tinker_title);


    }

}
