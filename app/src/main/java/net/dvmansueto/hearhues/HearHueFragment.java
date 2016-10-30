package net.dvmansueto.hearhues;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioTrack;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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

    private CamTone mCamTone;

    //// Palette fields

//    /**
//     * Override {@link Palette#DEFAULT_RESIZE_BITMAP_AREA} (default: 160 * 160), reduce to speed-up
//     * conversion time.
//     * Doesn't seem to have much affect on time, so increase to get more colours!
//     */
//
//    private static final int RESIZE_BITMAP_AREA = 256 * 256;

    private ScalarTone mScalarTone;
    private SharedPreferences mSharedPreferences;
    private int mResizeBitmapArea;
    private int mCalculateNumberColors;
    private boolean mAskedForPermission;

//    /**
//     * Override {@link Palette#DEFAULT_CALCULATE_NUMBER_COLORS} (default: 16, recommended:
//     * ~10 for landscape, ~24 for faces), reduce to speed-up conversion time.
//     * Doesn't seem to have much affect on time, so increase to get more colours!
//     */
//    private static final int CALCULATE_NUMBER_COLORS = 24;

    /**
     * A {@link HueTone} object.
     */
    private HueTone mHueTone;

    /**
     * A {@link ToneGenerator} object.
     */
    private ToneGenerator mToneGenerator;

    /**
     * Whether the tone is currently playing or not.
     */
    private boolean mPlaying;


    /**
     * Updates the {@link #mHueTone}, then updates the UI and tone generation accordingly.
     * @param swatch the new hue to use.
     */
    public void updateHueTone( Palette.Swatch swatch) {

        // update HueTone
        mHueTone.setHue(swatch);
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

    private void playStop() {
        if ( !mPlaying) {
            mToneGenerator.play();
        } else {
            mToneGenerator.stop();
        }
    }

    /**
     * Begins the Palette operation on the photograph. Called when a JPEG image has just been saved
     * in
     */
    private void processHueTone( File file) {
        Bitmap bitmap = BitmapFactory.decodeFile( file.toString());
        // asynchronous palette processing
        Palette.from( bitmap).
                maximumColorCount( mCalculateNumberColors).resizeBitmapArea( mResizeBitmapArea).
                generate(new Palette.PaletteAsyncListener() {
                    public void onGenerated(Palette palette) {
//                StringBuilder stringBuilder = new StringBuilder();
//                if ( palette.getVibrantSwatch() != null) stringBuilder.append( "Vibrant , ");
//                if ( palette.getLightVibrantSwatch() != null) stringBuilder.append( "Light Vibrant, ");
//                if ( palette.getDarkVibrantSwatch() != null) stringBuilder.append( "Dark Vibrant, ");
//                if ( palette.getDominantSwatch() != null) stringBuilder.append( "Dominant");
//                Log.d( TAG, stringBuilder.toString());
                        // prefer user preference
                        switch (mSharedPreferences.getString(getString(R.string.prefs_palette_swatch_key),
                                getString(R.string.prefs_palette_swatch_default))) {
                            case "Dominant":
                                Log.d(TAG, "Dominant");
                                updateHueTone(palette.getDominantSwatch());
                                return;
                            case "Vibrant":
                                Log.d(TAG, "Vibrant");
                                if (palette.getVibrantSwatch() != null) {
                                    updateHueTone(palette.getVibrantSwatch());
                                }
                                break;
                            case "Light Vibrant":
                                Log.d(TAG, "Light Vibrant");
                                if (palette.getLightVibrantSwatch() != null) {
                                    updateHueTone(palette.getLightVibrantSwatch());
                                }
                                break;
                            case "Dark Vibrant":
                                Log.d(TAG, "Dark Vibrant");
                                if (palette.getDarkVibrantSwatch() != null) {
                                    updateHueTone(palette.getDarkVibrantSwatch());
                                }
                                break;
                        }
                        // else try for light and settle for dominant
                        if (palette.getVibrantSwatch() != null) {
                            updateHueTone(palette.getVibrantSwatch());
                        } else if (palette.getLightVibrantSwatch() != null) {
                            updateHueTone(palette.getLightVibrantSwatch());
                        } else if (palette.getDarkVibrantSwatch() != null) {
                            updateHueTone(palette.getDarkVibrantSwatch());
                        } else {
                            updateHueTone(palette.getDominantSwatch());
                        }
                    }
                });
    }



    public HearHueFragment() {
        // require empty constructor
    }

    public static HearHueFragment newInstance() {
        return new HearHueFragment();
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
        return inflater.inflate( R.layout.fragment_hear_hue, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        view.findViewById( R.id.HH_btn_playStop).setOnClickListener( this);
        view.findViewById( R.id.HH_btn_toggleCamera).setOnClickListener( this);
        view.findViewById( R.id.HH_btn_capturePhoto).setOnClickListener( this);
    }

    //TODO: consider replacing fetching prefs with a listener, or something!
    @Override
    public void onResume() {
        super.onResume();

        ApplicationSingleton applicationSingleton = (ApplicationSingleton) getActivity().getApplicationContext();
        mScalarTone = applicationSingleton.getScalarTone();
        mToneGenerator = applicationSingleton.getToneGenerator();
        mToneGenerator.setPlaybackFactor( 1);
        mToneGenerator.setPlaybackMode(AudioTrack.MODE_STATIC);
        mToneGenerator.setAmplitude( 1);

        mHueTone = new HueTone ( mScalarTone);
        mCamTone = new CamTone( getActivity(), CAMERA_REQUEST_CODE);
        mCamTone.setCamToneListener( new CamTone.CamToneListener() {
            @Override
            public void fatalError() {
                exitToNavigationDrawer();
            }

            @Override
            public void errorDialog(String message) {
                ErrorDialog.newInstance( message).show( getChildFragmentManager(), FRAGMENT_DIALOG);
            }

            @Override
            public void newCapture(File capture) {
                processHueTone( capture);
            }

            @Override
            public void requestPermission(String permission, int returnedCode) {
                if ( !mAskedForPermission && ActivityCompat.checkSelfPermission(
                        getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d( TAG, "asking for permission");
                    // Toast causing screen overlay issue!
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

        mToneGenerator.setToneGeneratorListener( new ToneGenerator.ToneGeneratorListener() {
            @Override
            public void startedPlaying() {
                mPlaying = true;
                Activity activity = getActivity();
                ImageView imageView = (ImageView) activity.findViewById( R.id.HH_btn_playStop);
                imageView.setImageResource( R.drawable.ic_pause_circle_outline_48);
            }
            @Override
            public void stoppedPlaying() {
                mPlaying = false;
                Activity activity = getActivity();
                ImageView imageView = (ImageView) activity.findViewById( R.id.HH_btn_playStop);
                imageView.setImageResource( R.drawable.ic_play_circle_outline_48);
            }
        });

        mCamTone.initFile();
        mCamTone.initPreview();
        updateUi();

        AppCompatActivity activity = (AppCompatActivity) getActivity();
//        noinspection ConstantConditions
        activity.getSupportActionBar().setTitle( R.string.title_fragment_hear_hue);
        mCamTone.startBackgroundThread();


        // update SharedPreferences every time fragment resumes
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( getActivity());
//        mSharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        switch( mSharedPreferences.getString(
                getString( R.string.prefs_palette_area_key),
                getString( R.string.prefs_palette_area_default))) {
            case "Very Low":
                mResizeBitmapArea = 20 * 20;
                break;
            case "Low":
                mResizeBitmapArea = 80 * 80;
                break;
            case "Medium":
                mResizeBitmapArea = 160 * 160;
                break;
            case "High":
                mResizeBitmapArea = 320 * 320;
                break;
            case "Very High":
                mResizeBitmapArea = 640 * 640;
                break;
        }

        switch( mSharedPreferences.getString(
                getString( R.string.prefs_palette_number_key),
                getString( R.string.prefs_palette_number_default))) {
            case "Very Low":
                mCalculateNumberColors = 4;
                break;
            case "Low":
                mCalculateNumberColors = 8;
                break;
            case "Medium":
                mCalculateNumberColors = 16;
                break;
            case "High":
                mCalculateNumberColors = 24;
                break;
            case "Very High":
                mCalculateNumberColors = 48;
                break;
        }

        mCamTone.setSavingFiles( mSharedPreferences.getBoolean(
                getString( R.string.prefs_save_files_key),
                parseBoolean(getString( R.string.prefs_save_files_default))));

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
        ApplicationSingleton applicationSingleton = (ApplicationSingleton) getActivity().getApplicationContext();
        applicationSingleton.setScalarTone( mScalarTone);
        applicationSingleton.setToneGenerator( mToneGenerator);
        super.onStop();
    }

//    private void requestCameraPermission() {
//        if (FragmentCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
//            new ConfirmationDialog().show(getSupportFragmentManager(), FRAGMENT_DIALOG);
//        } else {
//            FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
//                    REQUEST_CAMERA_PERMISSION);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == REQUEST_CAMERA_PERMISSION) {
//            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                ErrorDialog.newInstance(getString(R.string.request_permission))
//                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
//            }
//        } else {
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
//    }



    @Override
    public void onClick( View view) {
        Log.d(TAG, "onClick: " + view.getId());
        switch( view.getId()) {
            case R.id.HH_btn_playStop: {
                playStop();
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

//    @Override
//    public void onRequestPermissionsResult( int returnedCode, @NonNull String[] permissions,
//                                            @NonNull int[] grantedResults) {
//        Log.d( TAG, "results back");
//        mCamTone.onRequestPermissionsResult( returnedCode, permissions, grantedResults);
//    }

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
        Log.d( TAG, "Exited to nav drawer");
        onPause();
    }
}
