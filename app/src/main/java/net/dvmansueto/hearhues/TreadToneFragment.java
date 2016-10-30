package net.dvmansueto.hearhues;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import static android.content.Context.LOCATION_SERVICE;


/**
 * A simple {@link Fragment} subclass.
 */
public class TreadToneFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "TreadTunes";
    private static final int LOCATION_REQUEST_CALLBACK_CODE = 42;

    //TODO: pref candidate
    private static final int LOCATION_USE_BY_TIME = 1000 * 30; // short because expect small travel distances

    private ScalarTone mScalarTone;
    private ToneGenerator mToneGenerator;

    private LocTone mLocTone;
    private LocView mLocView;

    private boolean mSettingDatum = true; // so will take first fix as datum

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    @Override
    public void onClick( View view) {
        Log.d(TAG, "onClick: " + view.getId());
        switch( view.getId()) {
            case R.id.tread_tune_ivbtn_location:
                setDatum();
                break;
        }
    }

    public TreadToneFragment() {
        // Required empty public constructor
    }

    public static TreadToneFragment newInstance() {
        return new TreadToneFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tread_tone, container, false);
    }

    private Location mLocation;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLocationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if ( isBetterLocation( location, mLocation)) {
                    mLocation = location;
                    Log.d( TAG, "Have new location: " + location.toString());
                    if (mSettingDatum) {
                        Log.d( TAG, "new LocTone");
                        mLocTone = new LocTone( mLocation);
                        mSettingDatum = false;
                    }
                    else {
                        Log.d(TAG, "updating LocTone");
                        mLocTone.updateLoc(mLocation);
                    }
                        Log.d( TAG, "updating LocView");
                        mLocView.newScalarCoord(
                                (float) mLocTone.getFrequency(), (float) mLocTone.getAmplitude());
                        mToneGenerator.setFrequency( mScalarTone.scalarToTone( mLocTone.getFrequency()));
                        mToneGenerator.setAmplitude( mLocTone.getAmplitude());
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
    }

    /**
     * Only ask for permission once.
     */
    private boolean pestering;

    @Override
    public void onResume() {
        super.onResume();
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setTitle( R.string.title_fragment_tread_tune);

        // check for permission; ask for it if needed; establish GPS listener; exit to drawer if refused.
        checkLocationPermission();

        ApplicationSingleton applicationSingleton = (ApplicationSingleton) getActivity().getApplicationContext();
        mScalarTone = applicationSingleton.getScalarTone();
        mToneGenerator = applicationSingleton.getToneGenerator();
//        mToneGenerator = new ToneGenerator();
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
        mToneGenerator.setPlaybackMode( AudioTrack.MODE_STREAM);
        mToneGenerator.setPlaybackFactor( 0.0625);
        mToneGenerator.setPlayContinuously( true);

        mLocView = (LocView) getActivity().findViewById( R.id.tread_tune_loc_view);
//        mLocView.setTouchAllowed( true);
//        mLocView.setLocViewListener(new LocView.LocViewListener() {
//            @Override
//            public void newFrequency(double frequency) {
//                mToneGenerator.setFrequency( mScalarTone.scalarToTone( frequency));
//            }
//            @Override
//            public void newAmplitude(double amplitude) {
//                mToneGenerator.setAmplitude( amplitude);
//            }
//        });

    }

    @Override
    public void onPause() {
        super.onPause();

        if ( ActivityCompat.checkSelfPermission( getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    private void setDatum() {
        mSettingDatum = true;
    }

    private void initGpsLoc() {
        // check setting is enabled
        if ( !mLocationManager.isProviderEnabled( LocationManager.GPS_PROVIDER)) {
            Log.d( TAG, "gps setting disabled");
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder( getActivity());
            alertDialogBuilder.setTitle( R.string.alert_enable_location_setting_title)
                    .setMessage( R.string.alert_enable_location_setting_message)
                    .setPositiveButton( R.string.alert_enable_location_setting_positive,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface dialog, int which) {
                            Intent intent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity( intent);
                        }
                    })
                    .setNegativeButton( R.string.alert_enable_location_setting_negative,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface dialog, int which) {
                            // user cancelled the dialog
                            exitToNavigationDrawer();
                        }
                    });
            alertDialogBuilder.create().show();
        }
        //noinspection MissingPermission: definitely have permission at this point...
        mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
    }

    private void checkLocationPermission() {
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;
        int callbackCode = LOCATION_REQUEST_CALLBACK_CODE;

        if ( ActivityCompat.checkSelfPermission(
                getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
            // check if Android thinks we should explain to the user why we need this permission
            // ( ie it would seem out-of-context to user otherwise)
            if ( ActivityCompat.shouldShowRequestPermissionRationale( getActivity(), permission)) {
                Toast.makeText( getActivity(),
                        getString( R.string.toast_tread_tune_location_rationale),
                        Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions( getActivity(), new String[] { permission}, callbackCode);
            } else {
                ActivityCompat.requestPermissions( getActivity(), new String[] { permission}, callbackCode);
            }
        }
        else {
            // we have permission!
            initGpsLoc();
        }
    }

    @Override
    public void onRequestPermissionsResult( int returnedCode, @NonNull String[] permissions,
                                            @NonNull int[] grantedResults) {
        Log.d( TAG, "switching " + Integer.toString( returnedCode));

        switch ( returnedCode) {
            case LOCATION_REQUEST_CALLBACK_CODE: {

                // If request was rejected, the result arrays will be empty.
                if (grantedResults.length == 0
                        || grantedResults[0] != PackageManager.PERMISSION_GRANTED) {

                    Log.d(TAG, "gps permission rejected");
                    // permission refused, display a dialog and exit to nav drawer
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogBuilder.setTitle(R.string.alert_location_permission_refused_title)
                            .setMessage(R.string.alert_location_permission_refused_message)
                            .setNeutralButton(R.string.alert_location_permission_refused_negative,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // user cancelled the dialog
                                            exitToNavigationDrawer();
                                        }
                                    });
                    alertDialogBuilder.create().show();
                } else {
                    // otherwise we have permission
                    Log.d(TAG, "gps permission accepted");
                    initGpsLoc();
                }
            }
            default:
                super.onRequestPermissionsResult( returnedCode, permissions, grantedResults);
        }
    }

    private void exitToNavigationDrawer() {
        // cast to avoid 'static' complaint
        ((MainActivity) getActivity()).openNavigationDrawer();
        Log.d( TAG, "Exited to nav drawer");
        onPause();
    }

    //////// GPS METHODS

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > LOCATION_USE_BY_TIME;
        boolean isSignificantlyOlder = timeDelta < -LOCATION_USE_BY_TIME;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
