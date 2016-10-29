package net.dvmansueto.hearhues;

import android.app.Fragment;
import android.media.AudioTrack;
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

    private ScalarTone mScalarTone;
    private HueView mHueView;
    private LocView mLocView;
    private ToneGenerator mToneGenerator;

    private int mCurrentView;
    private static final int HUE_VIEW = 1;
    private static final int LOC_VIEW = 2;

    public TinkerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d( TAG, " onActivityCreated.");
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

        mHueView = (HueView) activity.findViewById( R.id.tinker_hue_view);

        ApplicationSingleton applicationSingleton = (ApplicationSingleton) getActivity().getApplicationContext();
        mScalarTone = applicationSingleton.getScalarTone();
        mToneGenerator = applicationSingleton.getToneGenerator();
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
        mLocView.setTouchAllowed( true);
        mLocView.setLocViewListener(new LocView.LocViewListener() {
            @Override
            public void newFrequency(double frequency) {
                if ( mCurrentView != LOC_VIEW) setCurrentView( LOC_VIEW);
                mToneGenerator.setFrequency( mScalarTone.scalarToTone( frequency));
//                mToneGenerator.play();
            }
            @Override
            public void newAmplitude(double amplitude) {
                if ( mCurrentView != LOC_VIEW) setCurrentView( LOC_VIEW);
                mToneGenerator.setAmplitude( amplitude);
//                mToneGenerator.play();
            }
        });
    }


    @Override
    public void onStop() {
        if ( mCurrentView != HUE_VIEW) setCurrentView( HUE_VIEW); // restore normal activity.
        ApplicationSingleton applicationSingleton = (ApplicationSingleton) getActivity().getApplicationContext();
        applicationSingleton.setScalarTone( mScalarTone);
        applicationSingleton.setToneGenerator( mToneGenerator);
        super.onStop();
    }

    /**
     * Allows for changing ToneGenerator parameters based on view user is interacting with.
     * @param view the view to optimise for.
     */
    private void setCurrentView( int view) {
        switch ( view) {
            case HUE_VIEW:
                optimiseForHueView();
                mCurrentView = HUE_VIEW;
                break;
            case LOC_VIEW:
                optimiseForLocView();
                mCurrentView = LOC_VIEW;
                break;
        }
    }

    private void optimiseForLocView() {
        mToneGenerator.setPlaybackMode ( AudioTrack.MODE_STREAM);
        mToneGenerator.setPlaybackFactor( 0.0625); // much shorter playback times
        mToneGenerator.setPlayContinuously( true);
        mToneGenerator.setRampingUp( true);
        mToneGenerator.setRampingDown( true);
    }

    private void optimiseForHueView() {
        mToneGenerator.setPlaybackMode ( AudioTrack.MODE_STATIC);
        mToneGenerator.setPlaybackFactor( 1); // much shorter playback times
        mToneGenerator.setPlayContinuously( false);
        mToneGenerator.setRampingUp( true);
        mToneGenerator.setRampingDown( true);
    }
}
