package net.dvmansueto.hearhues;

import android.media.AudioTrack;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A way for the user to muck around with things.
 */
public class TinkerFragment extends Fragment {

    /** Indicates the user is interacting with HueView */
    private static final int HUE_VIEW = 1;

    /** Indicates the user is interacting with LocView */
    private static final int LOC_VIEW = 2;

    /** Provides scalar:tone functions */
    private ScalarTone mScalarTone;

    /** Displays a hue map for the user to click, interfaces with a HueTone */
    //TODO: implement HueView
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private HueView mHueView;

    /** Displays a plot for the user to click, makes cool noises */
    @SuppressWarnings("FieldCanBeLocal")
    private LocView mLocView;

    /** Generates tones. Is a global variable. */
    private ToneGenerator mToneGenerator;

    /** Identifies which of the two views the user is interacting with */
    private int mCurrentView;


    //------------------------
    // Constructor
    //------------------------

    public TinkerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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

        //noinspection ConstantConditions
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.fragment_tinker_title);

        mHueView = (HueView) getActivity().findViewById( R.id.tinker_hue_view);

        ApplicationContext applicationContext = (ApplicationContext) getActivity().getApplicationContext();
        mScalarTone = applicationContext.getScalarTone();
        mToneGenerator = applicationContext.getToneGenerator();
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
            public void newScalarCoords( double[] newScalarCoords) {
                if ( mCurrentView != LOC_VIEW) setCurrentView( LOC_VIEW);
                mToneGenerator.setAmplitude( newScalarCoords[ 1]); // amp first as freq plays
                mToneGenerator.setFrequency( mScalarTone.scalarToTone( newScalarCoords[ 0]));
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if ( mCurrentView != HUE_VIEW) setCurrentView( HUE_VIEW); // restore normal activity.
        ApplicationContext applicationContext = (ApplicationContext) getActivity().getApplicationContext();
        applicationContext.setScalarTone( mScalarTone);
        applicationContext.setToneGenerator( mToneGenerator);
    }

    @Override
    public void onStop() {
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

    /**
     * Optimises the Tone Generator for looped short bursts.
     */
    private void optimiseForLocView() {
        mToneGenerator.setPlaybackMode ( AudioTrack.MODE_STREAM);
        mToneGenerator.setPlaybackFactor( 0.0625); // much shorter playback times
        mToneGenerator.setPlayContinuously( true);
        mToneGenerator.setRampingUp( true);
        mToneGenerator.setRampingDown( true);
    }

    /**
     * Optimises the Tone Generator for single long notes.
     */
    private void optimiseForHueView() {
        mToneGenerator.setPlaybackMode ( AudioTrack.MODE_STATIC);
        mToneGenerator.setPlaybackFactor( 1); // much shorter playback times
        mToneGenerator.setPlayContinuously( false);
        mToneGenerator.setRampingUp( true);
        mToneGenerator.setRampingDown( true);
    }
}
