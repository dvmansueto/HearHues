package net.dvmansueto.hearhues;

import android.media.AudioTrack;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A way for the user to muck around with things.
 */
public class TinkerFragment extends Fragment {

    /** Provides scalar:tone functions */
    private ScalarTone mScalarTone;

    /** Displays a hue map for the user to click, interfaces with a HueTone */
    //TODO: implement HueView
    private HueView mHueView;

    /** Displays a plot for the user to click, makes cool noises */
    private LocView mLocView;

    /** Generates tones. Is a global variable. */
    private ToneGenerator mToneGenerator;


    //------------------------
    // Constructor
    //------------------------

    public TinkerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView( LayoutInflater inflater,
                              ViewGroup container,
                              Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tinker, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // in onViewCreated to try and reduce number of initialisation tasks
        mHueView = (HueView) view.findViewById( R.id.tinker_hue_view);
        mLocView = (LocView) view.findViewById( R.id.tinker_loc_view);

    }

    @Override
    public void onResume() {
        super.onResume();

        //noinspection ConstantConditions
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setIcon( R.drawable.ic_tinker_24);
        actionBar.setTitle( R.string.fragment_tinker_title);

        ApplicationContext applicationContext = (ApplicationContext) getActivity().getApplicationContext();
        mScalarTone = applicationContext.getScalarTone();
        mToneGenerator = applicationContext.getToneGenerator();
        mToneGenerator.setStreamMode(); // continuous stream of short tones
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

        mHueView.setHueViewListener( new HueView.HueViewListener() {
                 @Override
                 public void newScalarCoords(double[] scalarCoords) {
                     mToneGenerator.setAmplitude( scalarCoords[ 1]);
                     mToneGenerator.setFrequency( mScalarTone.scalarToTone( scalarCoords[ 0]));
                 }
         });

        mLocView.setTouchAllowed( true);
        mLocView.setLocViewListener(new LocView.LocViewListener() {
                @Override
                public void newScalarCoords( double[] scalarCoords) {
                    mToneGenerator.setAmplitude( scalarCoords[ 1]); // amp first as freq plays
                    mToneGenerator.setFrequency( mScalarTone.scalarToTone( scalarCoords[ 0]));
                }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        ApplicationContext applicationContext = (ApplicationContext) getActivity().getApplicationContext();
        applicationContext.setScalarTone( mScalarTone);
        applicationContext.setToneGenerator( mToneGenerator);
    }

}
