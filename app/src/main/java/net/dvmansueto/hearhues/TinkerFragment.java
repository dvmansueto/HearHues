package net.dvmansueto.hearhues;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class TinkerFragment extends Fragment {

    private static final String TAG = "TinkerFragment";

    private ScalarTone mScalarTone;
    private HueView mHueView;
    private LocView mLocView;
    private ToneGenerator mToneGenerator;

    public TinkerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d( TAG, " onActivityCreated.");

        mHueView = (HueView) getActivity().findViewById( R.id.tinker_hue_view);

        ApplicationSingleton applicationSingleton = (ApplicationSingleton) getActivity().getApplicationContext();
        mScalarTone = applicationSingleton.getScalarTone();
        mToneGenerator = applicationSingleton.getToneGenerator();
        mToneGenerator.playContinuously( false);
        mToneGenerator.setPlaybackSeconds( 200/1000);
        mToneGenerator.setToneGeneratorListener( new ToneGenerator.ToneGeneratorListener() {
            @Override
            public void startedPlaying() {
//                mPlaying = true;
//                Activity activity = getActivity();
//                ImageView imageView = (ImageView) activity.findViewById( R.id.HH_btn_playStop);
//                imageView.setImageResource( R.drawable.ic_pause_circle_outline_48);
            }
            @Override
            public void stoppedPlaying() {
//                mPlaying = false;
//                Activity activity = getActivity();
//                ImageView imageView = (ImageView) activity.findViewById( R.id.HH_btn_playStop);
//                imageView.setImageResource( R.drawable.ic_play_circle_outline_48);
            }
        });

        mLocView = (LocView) getActivity().findViewById( R.id.tinker_loc_view);
        mLocView.setLocViewListener(new LocView.LocViewListener() {
            @Override
            public void newFrequency(double frequency) {
                Log.d( TAG, "f: " + Double.toString( frequency));
                mToneGenerator.setFrequency( mScalarTone.scalarToTone( frequency));
                mToneGenerator.startTone();
            }
            @Override
            public void newAmplitude(double amplitude) {
                Log.d( TAG, "a: " + Double.toString( amplitude));
                mToneGenerator.setAmplitude( amplitude);
                mToneGenerator.startTone();
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


    @Override
    public void onStop() {
        ApplicationSingleton applicationSingleton = (ApplicationSingleton) getActivity().getApplicationContext();
        applicationSingleton.setScalarTone( mScalarTone);
        applicationSingleton.setToneGenerator( mToneGenerator);
        super.onStop();
    }

}
