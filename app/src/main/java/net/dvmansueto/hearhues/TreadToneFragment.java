package net.dvmansueto.hearhues;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import static android.content.Context.LOCATION_SERVICE;


/**
 * A fragment which shows location on a lat/long axis, and converts lat/long to freq/amp, which
 * it displays and plays.
 */
public class TreadToneFragment extends Fragment implements View.OnClickListener {

    /** For {@link Log} */
    private static final String TAG = "TreadTunes";

    /** Callback matchup code for permission request */
    private static final int LOCATION_REQUEST_CALLBACK_CODE = 42;

    /** Provides scalar:tone functions */
    private ScalarTone mScalarTone;

    /** Generates tones... */
    private ToneGenerator mToneGenerator;

    /** Provides location methods */
    private LocTone mLocTone;

    /** Creates the axis view */
    private LocView mLocView;

    /** User's preferred window height, in metres */
    private double mPreferredWindowHeight;

    /** User's preferred window width, in meters */
    private double mPreferredWindowWidth;

    /** User's preferred location reliability timeout, in seconds */
    private int mPreferredLocationTimeout;

    /** User's preferred location accuracy, in metres */
    private int mPreferredLocationAccuracy;

    /** Whether to take the next location as the 'origin' point */
    private boolean mSettingOrigin;

    /** ...Manages Locations? */
    private LocationManager mLocationManager;

    /** Listens for location changes */
    private LocationListener mLocationListener;

    /** Last good location... */
    private Location mLastGoodLocation;

    /**
     * A fragment which shows location on a lat/long axis, and converts lat/long to freq/amp, which
     * it displays and plays.
     */
    public TreadToneFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tread_tone, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        // prepare to capture button presses
        view.findViewById( R.id.tread_tone_ivbtn_origin).setOnClickListener( this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLocationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // check if this location is better, using Android example code
                if ( isBetterLocation( location, mLastGoodLocation)) {
                    mLastGoodLocation = location;

                    // will be true after onResume and when user clicks the button
                    if (mSettingOrigin) {
                        mLocTone = new LocTone(
                                mPreferredWindowHeight, mPreferredWindowWidth, location);
                        mSettingOrigin = false;
                    } else {
                        mLocTone.setLocation( location);
                    }

                    double scalarLatitude = mLocTone.getScalarLatitude();
                    double scalarLongitude = mLocTone.getScalarLongitude();

                    // update axis view
                    mLocView.newScalarCoords( (float) scalarLongitude, (float) scalarLatitude);

                    // frequency passes through mScalarTone to apply sharedPreference ranges
                    double frequency = mScalarTone.scalarToTone( scalarLongitude);
                    //noinspection UnnecessaryLocalVariable for compilers, sure, but helps humans
                    double amplitude = scalarLatitude;

                    mToneGenerator.setFrequency( frequency); // plays noise!
                    mToneGenerator.setAmplitude( amplitude);

                    updateToneText( frequency, amplitude);
                    updateLocText( mLocTone.toDegreeString(), mLocTone.toCoordString());
                }
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
    }

    /**
     * Only ask for permission once.
     */
    private boolean askedForPermission;

    @Override
    public void onResume() {
        super.onResume();

        //noinspection ConstantConditions
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setIcon( R.drawable.ic_tread_24);
        actionBar.setTitle( R.string.title_fragment_tread_tone);

        // first location onResume is taken as initial datum/origin
        mSettingOrigin = true;

        if ( !askedForPermission) {
            // check for permission; ask for it if needed; establish GPS listener; exit to drawer if refused.
            checkLocationPermission();
        }

        // redefine variables from shared preferences every resume
        // no point using a listener, as can't change preferences while this fragment is active
        // and need to define them each resume anyway
        initialiseFromSharedPreferences();
        Log.d( TAG, "should have prefs now");
        Log.d( TAG, "mPreferredWindowHeight:" + Double.toString( mPreferredWindowHeight));
        Log.d( TAG, "mPreferredWindowWidth:" + Double.toString( mPreferredWindowWidth));
        Log.d( TAG, "mPreferredLocationTimeout:" + Double.toString( mPreferredLocationTimeout));

        // mScalarTone and mToneGenerator are 'global' objects, retrieved from ApplicationContext
        ApplicationContext applicationContext = (ApplicationContext) getActivity().getApplicationContext();
        mScalarTone = applicationContext.getScalarTone();
        mToneGenerator = applicationContext.getToneGenerator();

        // configure for looped short bursts
        mToneGenerator.setAmplitude( 1);
        mToneGenerator.setPlaybackMode( AudioTrack.MODE_STREAM);
        mToneGenerator.setPlaybackFactor( 0.0625);
        mToneGenerator.setPlayContinuously( true);

        mLocView = (LocView) getActivity().findViewById( R.id.tread_tone_loc_view);
    }

    @Override
    public void onPause() {
        super.onPause();

        // release location retrieval!
        if ( ActivityCompat.checkSelfPermission( getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    @Override
    public void onClick( View view) {
        Log.d(TAG, "onClick: " + view.getId());
        switch( view.getId()) {
            case R.id.tread_tone_ivbtn_origin:
                mSettingOrigin = true;
                break;
        }
    }


    //------------------------
    // Utilities
    //------------------------

    private void initialiseFromSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mPreferredWindowHeight = Double.parseDouble( sharedPreferences.getString(
                getString( R.string.prefs_tread_tone_window_height_key),
                getString( R.string.prefs_tread_tone_window_height_default)));
        mPreferredWindowWidth = Double.parseDouble( sharedPreferences.getString(
                getString( R.string.prefs_tread_tone_window_width_key),
                getString( R.string.prefs_tread_tone_window_width_default)));
        mPreferredLocationTimeout = Integer.parseInt( sharedPreferences.getString(
                getString( R.string.prefs_tread_tone_location_timeout_key),
                getString( R.string.prefs_tread_tone_location_timeout_default)));
        mPreferredLocationAccuracy = Integer.parseInt( sharedPreferences.getString(
                getString( R.string.prefs_tread_tone_location_accuracy_key),
                getString( R.string.prefs_tread_tone_location_accuracy_default)));
    }

    /**
     * Displays the tone as frequency & note and the volume on the Tone TextView.
     */
    private void updateToneText(double frequency, double amplitude) {
        String ampString = String.format( Locale.getDefault(), "%3d", (int) ( amplitude * 100));
        String toneString = String.format( Locale.getDefault(), "%7.2f", frequency) + " Hz";
        String noteString = mScalarTone.toneToNoteString( frequency);
        TextView textView = (TextView) getActivity().findViewById( R.id.tread_tone_tv_tone);
        textView.setText( toneString + " (" + noteString + "), " + ampString + "%" );
    }

    /**
     * Displays the tone as frequency and note and the volume on the Tone TextView.
     */
    private void updateLocText(String degrees, String coords) {
        TextView textView = (TextView) getActivity().findViewById( R.id.tread_tone_tv_location);
        textView.setText( degrees + " " + coords);
    }

    private void exitToNavigationDrawer() {
        // cast to avoid 'static' complaint
        ((MainActivity) getActivity()).openNavigationDrawer();
        Log.d( TAG, "Exiting to nav drawer");
        onPause();
    }

    //// GPS Methods

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > mPreferredLocationTimeout;
        boolean isSignificantlyOlder = timeDelta < -mPreferredLocationTimeout;
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
        boolean isSignificantlyLessAccurate = accuracyDelta > mPreferredLocationAccuracy;

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
                askedForPermission = true;
            } else {
                ActivityCompat.requestPermissions( getActivity(), new String[] { permission}, callbackCode);
                askedForPermission = true;
            }
        }
        else {
            // we have permission!
            initGpsLoc();
        }
    }
}
