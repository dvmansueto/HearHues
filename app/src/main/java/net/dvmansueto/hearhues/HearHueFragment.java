package net.dvmansueto.hearhues;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
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
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
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

    // extending Google's Camera2Basic sample code

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "HearHueFragment";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera( width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    //NEW:
    /**
     * ID of the front facing {@link CameraDevice}
     */
    private String mFrontCameraId;

    //NEW:
    /**
     * ID of the back facing {@link CameraDevice}
     */
    private String mBackCameraId;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
            Log.d( TAG, "Camera preview now for ID " + mCameraId);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This is the output file for our picture.
     */
    private File mFile;

    /**
     * This is the output file directory.
     */
    private File mFileDirectory;

    /**
     * This is the output file prefix.
     */
    String mFilePrefix;

    /**
     * This is the output file serial number.
     */
    int mFileNumber;

    /**
     * This is the output file type.
     */
    private String mFileType;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }

    };

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPhoto();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPhoto();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPhoto();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight,
                                          Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    //// Palette fields

//    /**
//     * Override {@link Palette#DEFAULT_RESIZE_BITMAP_AREA} (default: 160 * 160), reduce to speed-up
//     * conversion time.
//     * Doesn't seem to have much affect on time, so increase to get more colours!
//     */
//
//    private static final int RESIZE_BITMAP_AREA = 256 * 256;

    private SharedPreferences mSharedPreferences;
    private int mResizeBitmapArea;
    private int mCalculateNumberColors;
    private boolean mSavingPhotos;

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
        mToneGenerator.setTone( mHueTone.getTone());

        // let rip!
        mToneGenerator.startTone();
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
        textView.setText( mHueTone.getHueString());
        // update tone text display
        textView = (TextView) activity.findViewById( R.id.HH_tv_tone);
        textView.setText( mHueTone.getToneString());
    }

    private void playStop() {
        if ( !mPlaying) {
            mToneGenerator.startTone();
        } else {
            mToneGenerator.stopTone();
        }
    }

    /**
     * Begins the Palette operation on the photograph. Called when a JPEG image has just been saved
     * in {@link #mFile}.
     */
    private void processHueTone() {
        Bitmap bitmap = BitmapFactory.decodeFile( mFile.toString());
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
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.HH_aftv_cameraPreview);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mHueTone = new HueTone ( getActivity());
        mToneGenerator = new ToneGenerator();

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

        initFile();
        initCamera();
        updateUi();

    }

    @Override
    public void onResume() {
        super.onResume();

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        //noinspection ConstantConditions
        activity.getSupportActionBar().setTitle( R.string.title_fragment_hear_hue);
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if ( mTextureView.isAvailable()) {
            openCamera( mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
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

        mSavingPhotos = mSharedPreferences.getBoolean(
                getString( R.string.prefs_save_files_key),
                parseBoolean(getString( R.string.prefs_save_files_default)));

        mToneGenerator.setPlaybackSeconds( Double.parseDouble( mSharedPreferences.getString(
                getString( R.string.prefs_playback_seconds_key),
                getString( R.string.prefs_playback_seconds_default))));

        mHueTone.setFrequencies(
                Double.parseDouble( mSharedPreferences.getString(
                        getString( R.string.prefs_generator_base_frequency_key),
                        getString( R.string.prefs_generator_base_frequency_default)
                )),
                Double.parseDouble( mSharedPreferences.getString(
                        getString( R.string.prefs_generator_peak_frequency_key),
                        getString( R.string.prefs_generator_peak_frequency_default)
                )));


    }


    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void requestCameraPermission() {
        if (FragmentCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Initialises the camera.
     */
    private void initCamera() {
        findCameraIds();
    }

    /**
     * Finds the front and back facing cameras IDs:
     *  • {@link HearHueFragment#mBackCameraId}
     *  • {@link HearHueFragment#mFrontCameraId}
     *  • {@link HearHueFragment#mCameraId} (defaults to back camera, else front)
     */
    private void findCameraIds() {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            // look for front and back cameras
            for( String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics( cameraId);

                Integer facing = characteristics.get( CameraCharacteristics.LENS_FACING);
                if ( facing == null ) {
                    continue;
                }
                if ( facing == CameraCharacteristics.LENS_FACING_BACK) {
                    mBackCameraId = cameraId;
                    Log.d( TAG, "Found a back camera");
                } else if ( facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    mFrontCameraId = cameraId;
                    Log.d( TAG, "Found a front camera");
                } else {
                    Log.d( TAG, "Found a mystery camera");
                }
            }
            // hide the camera toggle button if there's only one camera
            if ( mBackCameraId == null || mFrontCameraId == null) {
                activity.findViewById( R.id.HH_btn_toggleCamera).setVisibility( View.INVISIBLE);
            }
            // set mCameraId to back camera by default
            if ( mBackCameraId != null) {
                mCameraId = mBackCameraId;
            } else if ( mFrontCameraId != null) {
                mCameraId = mFrontCameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    //TODO: explore generic 'switch camera' icon, avoid 'current state'/'next state' confusion.
    /**
     * Toggles between front and back cameras, changes the button icon.
     */
    private void toggleCamera() {
        closeCamera();
        ImageView imageView = (ImageView) getActivity().findViewById( R.id.HH_btn_toggleCamera);
        if (Objects.equals( mCameraId, mBackCameraId)) {
            mCameraId = mFrontCameraId;
            imageView.setImageResource( R.drawable.ic_camera_front_48);
            Log.d(TAG, "Toggling from back to front camera");
        } else {
            mCameraId = mBackCameraId;
            imageView.setImageResource( R.drawable.ic_camera_back_48);
            Log.d(TAG, "Toggling from front to back camera");
        }
        setUpCameraOutputs( mTextureView.getWidth(), mTextureView.getHeight());
        openCamera( mTextureView.getWidth(), mTextureView.getHeight());
    }

    /**
     * Sets up member variables related to camera, using current {@link #mCameraId}.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination") // rotated height and width
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics( mCameraId);

            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                // from when this was looping through cameraId's
//                if (map == null) {
//                    continue;
//                }

            // For still image captures, we use the largest available size.
            assert map != null;
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());
            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                    ImageFormat.JPEG, /*maxImages*/2);
            mImageReader.setOnImageAvailableListener(
                    mOnImageAvailableListener, mBackgroundHandler);

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            //noinspection ConstantConditions
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            boolean swappedDimensions = false;
            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                        swappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                        swappedDimensions = true;
                    }
                    break;
                default:
                    Log.e(TAG, "Display rotation is invalid: " + displayRotation);
            }

            Point displaySize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
            int rotatedPreviewWidth = width;
            int rotatedPreviewHeight = height;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedPreviewWidth = height;
                rotatedPreviewHeight = width;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
            }

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                    maxPreviewHeight, largest);

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }

            // Check if the flash is supported.
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mFlashSupported = available == null ? false : available;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * Opens the {@link #mCameraId} camera.
     */
    private void openCamera( int width, int height) {

        if ( ContextCompat.checkSelfPermission( getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        setUpCameraOutputs( width, height);
        configureTransform( width, height);

        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService( Context.CAMERA_SERVICE);
        try {
            if ( !mCameraOpenCloseLock.tryAcquire( 2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera( mCameraId, mStateCallback, mBackgroundHandler);
        } catch ( CameraAccessException e) {
            e.printStackTrace();
        } catch  (InterruptedException e) {
            throw new RuntimeException( "Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Initiate a still image capture.
     */
    private void capturePhoto() {
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still photograph. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPhoto() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("Saved: " + mFile);
                    Log.d(TAG, mFile.toString());

                    processHueTone();

                    if (mSavingPhotos) {
                        incrementFile();
                    } else {
                        //noinspection ResultOfMethodCallIgnored
                        mFile.delete();
                    }

                    unlockFocus();

                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick( View view) {
        Log.d(TAG, "onClick: " + view.getId());
        switch( view.getId()) {
            case R.id.HH_btn_playStop: {
                playStop();
                break;
            }
            case R.id.HH_btn_capturePhoto: {
                capturePhoto();
                break;
            }
            case R.id.HH_btn_toggleCamera: {
                toggleCamera();
                break;
            }
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * Initialises the output file {@link HearHueFragment#mFile} in accordance with Design rule for
     * Camera File system (DCF) v2.0 (2010), file names: DSC_XXXX.jpg.
     * Makes the {@link HearHueFragment#mFileDirectory}
     * Defines the {@link HearHueFragment#mFilePrefix}
     * Finds the latest {@link HearHueFragment#mFileNumber}
     */
    @SuppressLint("DefaultLocale") // since is only zero pad format for plain integers
    private void initFile() {
        // the file directory, following
        mFileDirectory = new File( getActivity().getExternalFilesDir( null) + "/DCIM/100HHUE/");
        //TODO: probably should check mkdir is actually successful.
        //noinspection ResultOfMethodCallIgnored
        mFileDirectory.mkdirs();
        mFilePrefix = "DSC_";
        mFileNumber = 1;
        mFileType = ".jpg";
        File[] files = mFileDirectory.listFiles();
        // find latest serial number if there already files here
        if ( files.length > 0) {
            for (File file : files) {
                String name = file.getName();
                // ensure appropriate file to compare
                if ( name.substring(0, 4).equalsIgnoreCase(mFilePrefix) &&
                        name.substring(8, 12).equalsIgnoreCase(mFileType)) {
                    // find largest
                    int number = Integer.parseInt(name.substring(4, 8));
                    if (number > mFileNumber) {
                        mFileNumber = number;
                    }
                }
            }
            mFileNumber++; // increment to unused serial number
        }
        mFile = new File( mFileDirectory,
                mFilePrefix + String.format("%04d", mFileNumber) + mFileType);
    }

    /**
     * Increments {@link HearHueFragment#mFileNumber} and updates {@link HearHueFragment#mFile}.
     */
    @SuppressLint("DefaultLocale") // since is only zero pad format
    private void incrementFile() {
        mFile = new File( mFileDirectory,
                mFilePrefix + String.format( "%04d", ++mFileNumber) + mFileType);
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
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

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final android.app.Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentCompat.requestPermissions(parent,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

}
