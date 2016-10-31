package net.dvmansueto.hearhues;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioTrack;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import static java.lang.Boolean.parseBoolean;

/**
 * Provides the HearHue interaction mode.
 * Primarily, allows user to find colours using their camera which is then converted to a
 * corresponding tone.
 * As a secondary feature, the ability to capture photographs is provided as it would be annoying to
 * be using a camera without this feature.
 * @author David Mansueto
 * @version $Id$
 * @since 0.1
 */
public class HearHueFragment extends Fragment
        implements View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback {


    private static final String TAG = "HearHueFragment";

    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int CAMERA_REQUEST_CODE = 1;

    /** Provides camera functions */
    private CamTone mCamTone;

    /** Provides scalar:tone functions */
    private ScalarTone mScalarTone;

    /** Provides hue:tone functions */
    private HueTone mHueTone;

    /** Generates tones... */
    private ToneGenerator mToneGenerator;

    /** User's preferred swatch to extract from the photograph */
    private String mPreferredSwatch;

    /** User's preferred resolution to resize photograph for extraction */
    private int mPreferredResizeBitmapArea;

    /** Users's preferred number of colours to consider when extracting */
    private int mPreferredCalculateNumberColors;

    /** User's preference of save photographs to disk or not*/
    private boolean mSavingFiles;

    /** Flag when permission has been asked for this resume to prevent cyclic requests */
    private boolean mAskedForPermission;

    //-----------------------------
    // Constructor
    //----------------------------

    public HearHueFragment() {
        // require empty constructor
    }

    @Override
    public View onCreateView( LayoutInflater inflater,
                              ViewGroup container,
                              Bundle savedInstanceState) {
        return inflater.inflate( R.layout.fragment_hear_hue, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        // prepare to capture button presses
        view.findViewById( R.id.HH_btn_playStop).setOnClickListener( this);
        view.findViewById( R.id.HH_btn_toggleCamera).setOnClickListener( this);
        view.findViewById( R.id.HH_btn_capturePhoto).setOnClickListener( this);
    }

    @Override
    public void onResume() {
        super.onResume();

        //noinspection ConstantConditions
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_hear_hue);

        // redefine variables from shared preferences every resume
        // no point using a listener, as can't change preferences while this fragment is active
        // and need to define them each resume anyway
        initialiseFromSharedPreferences();

        // mScalarTone and mToneGenerator are 'global' objects, retrieved from ApplicationContext
        ApplicationContext applicationContext = (ApplicationContext) getActivity().getApplicationContext();
        mScalarTone = applicationContext.getScalarTone();
        mToneGenerator = applicationContext.getToneGenerator();
        // configure for long single bursts
        mToneGenerator.setAmplitude( 1);
        mToneGenerator.setPlaybackMode( AudioTrack.MODE_STATIC);
        mToneGenerator.setPlaybackFactor( 1);
        mToneGenerator.setPlayContinuously( false);
        mToneGenerator.setToneGeneratorListener( new ToneGenerator.ToneGeneratorListener() {
            @Override
            public void startedPlaying() {
                Activity activity = getActivity();
                ImageView imageView = (ImageView) activity.findViewById(R.id.HH_btn_playStop);
                imageView.setImageResource(R.drawable.ic_pause_circle_outline_48);
            }

            @Override
            public void stoppedPlaying() {
                Activity activity = getActivity();
                ImageView imageView = (ImageView) activity.findViewById(R.id.HH_btn_playStop);
                imageView.setImageResource(R.drawable.ic_play_circle_outline_48);
            }
        });

        mHueTone = new HueTone(mScalarTone);
        mCamTone = new CamTone(getActivity(), CAMERA_REQUEST_CODE);
        mCamTone.setSavingFiles(mSavingFiles);
        mCamTone.initFile();
        mCamTone.initPreview();
        mCamTone.startBackgroundThread();
        mCamTone.setCamToneListener(new CamTone.CamToneListener() {
            @Override
            public void fatalError() {
                exitToNavigationDrawer();
            }

            @Override
            public void errorDialog(String message) {
                ErrorDialog.newInstance(message).show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }

            @Override
            public void newCapture(File capture) {
                processHueTone(capture);
            }

            @Override
            public void requestPermission(String permission, int returnedCode) {
                if (!mAskedForPermission && ActivityCompat.checkSelfPermission(
                        getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "asking for permission");
                    // *** Toast causing screen overlay issue!
                    // check if Android thinks we should explain to the user why we need this permission
                    // ( ie it would seem out-of-context to user otherwise)
//                    if (ActivityCompat.shouldShowRequestPermissionRationale( getActivity(), permission)) {
//                        Toast.makeText(getActivity(),
//                                getString(R.string.toast_hear_hue_camera_rational),
//                                Toast.LENGTH_LONG).show();
//                        ActivityCompat.requestPermissions(
//                                getActivity(), new String[]{permission}, returnedCode);
//                        mAskedForPermission = true;
//                    } else {
                    ActivityCompat.requestPermissions(
                            getActivity(), new String[]{permission}, returnedCode);
                    mAskedForPermission = true;
//                    }
                }
            }
        });

        updateUi();
    }

    @Override
    public void onPause() {
        mCamTone.closeCamera();
        mCamTone.stopBackgroundThread();
        super.onPause();
    }


    @Override
    public void onStop() {
        mToneGenerator.flush();
        ApplicationContext applicationContext = (ApplicationContext) getActivity().getApplicationContext();
        applicationContext.setScalarTone( mScalarTone);
        applicationContext.setToneGenerator( mToneGenerator);
        super.onStop();
    }

    @Override
    public void onClick( View view) {
        Log.d(TAG, "onClick: " + view.getId());
        switch( view.getId()) {
            case R.id.HH_btn_playStop: {
                mToneGenerator.playStop();
                break;
            }
            case R.id.HH_btn_capturePhoto: {
                mCamTone.capturePhoto();
                break;
            }
            case R.id.HH_btn_toggleCamera: {
                mCamTone.toggleCamera();
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult( int returnedCode, @NonNull String[] permissions,
                                     @NonNull int[] grantedResults) {
        Log.d( TAG, "switching " + Integer.toString( returnedCode));

        switch ( returnedCode) {
            case CAMERA_REQUEST_CODE: {

                // If request was rejected, the result arrays will be empty.
                if (grantedResults.length == 0
                        || grantedResults[0] != PackageManager.PERMISSION_GRANTED) {

                    Log.d(TAG, "camera permission rejected");
                    // permission refused, display a dialog and exit to nav drawer
                    android.support.v7.app.AlertDialog.Builder alertDialogBuilder
                            = new android.support.v7.app.AlertDialog.Builder( getActivity());
                    alertDialogBuilder.setTitle(R.string.alert_camera_permission_refused_title)
                            .setMessage(R.string.alert_camera_permission_refused_message)
                            .setNeutralButton(R.string.alert_camera_permission_refused_negative,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // user cancelled the dialog
                                            exitToNavigationDrawer();
                                        }
                                    });
                    alertDialogBuilder.create().show();
                }
                // if we have permission, open the camera
                mCamTone.openCamera();
            }
        }
    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new android.support.v7.app.AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setNeutralButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {
                                    // user cancelled the dialog
                                    ((MainActivity) getActivity()).openNavigationDrawer();
                                    onPause();
                                }
                            }
                    ).create();
        }
    }

    private void exitToNavigationDrawer() {
        // cast to avoid 'static' complaint
        ((MainActivity) getActivity()).openNavigationDrawer();
        Log.d(TAG, "Exited to nav drawer");
    }

    /**
     * Updates the {@link #mHueTone}, then updates the UI and tone generation accordingly.
     * @param color the new hue to use.
     */
    public void updateHueTone( @ColorInt int color) {

        // update HueTone
        mHueTone.setHue( color);
        updateUi();

        // update the frequency of the tone generator
        mToneGenerator.setFrequency( mHueTone.getTone());

        // let rip!
        mToneGenerator.play();
    }

    /**
     * Updates dynamic UI elements:
     *  • Tone frequency string
     *  • Hue icon colour
     *  • Hue colour string
     */
    private void updateUi() {

        // update UI
        Activity activity = getActivity();
        // update hue icon colour
        ImageView imageView = (ImageView) activity.findViewById( R.id.HH_iv_hue);
        imageView.setColorFilter( mHueTone.getRgb());
        // update hue text display
        TextView textView = (TextView) activity.findViewById( R.id.HH_tv_hue);
        textView.setText( mHueTone.toRgbString() + " (" + mHueTone.toHueString() + ")");
        // update tone text display
        textView = (TextView) activity.findViewById( R.id.HH_tv_tone);
        textView.setText( mHueTone.toToneString() + " (" + mHueTone.toNoteString() + ")");
    }

    /**
     * Begins the Palette operation on the photograph. Called when a JPEG image has just been saved
     * in
     */
    private void processHueTone( File file) {
        Bitmap bitmap = BitmapFactory.decodeFile( file.toString());

        // asynchronous palette processing
        Palette.from( bitmap).
                maximumColorCount(mPreferredCalculateNumberColors).resizeBitmapArea(mPreferredResizeBitmapArea).
                generate( new Palette.PaletteAsyncListener() {
                    public void onGenerated(Palette palette) {
                        Palette.Swatch swatch = null;
                        // try for user preference
                        switch ( mPreferredSwatch) {
                            case "Dominant":
                                Log.d(TAG, "Dominant");
                                swatch = palette.getDominantSwatch();
                                break;
                            case "Vibrant":
                                Log.d(TAG, "Vibrant");
                                if (palette.getVibrantSwatch() != null) {
                                    swatch = palette.getVibrantSwatch();
                                }
                                break;
                            case "Light Vibrant":
                                Log.d(TAG, "Light Vibrant");
                                if (palette.getLightVibrantSwatch() != null) {
                                    swatch = palette.getLightVibrantSwatch();
                                }
                                break;
                            case "Dark Vibrant":
                                Log.d(TAG, "Dark Vibrant");
                                if (palette.getDarkVibrantSwatch() != null) {
                                    swatch = palette.getDarkVibrantSwatch();
                                }
                                break;
                        }
                        // else try for a vibrant and settle for dominant
                        if ( swatch == null) {
                            if (palette.getVibrantSwatch() != null) {
                                swatch = palette.getVibrantSwatch();
                            } else if (palette.getLightVibrantSwatch() != null) {
                                swatch = palette.getLightVibrantSwatch();
                            } else if (palette.getDarkVibrantSwatch() != null) {
                                swatch = palette.getDarkVibrantSwatch();
                            } else {
                                swatch = palette.getDominantSwatch();
                            }
                        }
                        updateHueTone( swatch.getRgb());
                    }
                });
    }


    private void initialiseFromSharedPreferences() {

        // update SharedPreferences every time fragment resumes
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
//        mSharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        switch ( sharedPreferences.getString(
                getString(R.string.prefs_palette_area_key),
                getString(R.string.prefs_palette_area_default))) {
            case "Very Low":
                mPreferredResizeBitmapArea = 20 * 20;
                break;
            case "Low":
                mPreferredResizeBitmapArea = 80 * 80;
                break;
            case "Medium":
                mPreferredResizeBitmapArea = 160 * 160;
                break;
            case "High":
                mPreferredResizeBitmapArea = 320 * 320;
                break;
            case "Very High":
                mPreferredResizeBitmapArea = 640 * 640;
                break;
        }

        switch ( sharedPreferences.getString(
                getString(R.string.prefs_palette_number_key),
                getString(R.string.prefs_palette_number_default))) {
            case "Very Low":
                mPreferredCalculateNumberColors = 4;
                break;
            case "Low":
                mPreferredCalculateNumberColors = 8;
                break;
            case "Medium":
                mPreferredCalculateNumberColors = 16;
                break;
            case "High":
                mPreferredCalculateNumberColors = 24;
                break;
            case "Very High":
                mPreferredCalculateNumberColors = 48;
                break;
        }

        mPreferredSwatch = (sharedPreferences.getString(getString(R.string.prefs_palette_swatch_key),
                getString(R.string.prefs_palette_swatch_default)));

        mSavingFiles = sharedPreferences.getBoolean(
                getString(R.string.prefs_save_files_key),
                parseBoolean(getString(R.string.prefs_save_files_default)));
    }

}