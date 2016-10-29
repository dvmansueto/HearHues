package net.dvmansueto.hearhues;

import android.location.Location;
import android.util.Log;

/**
 * LocTone: idea is to establish a datum, then use relative latitude and longitude to alter the
 * amplitude and frequency (or visa versa) of a generated tone.
 * Created by Dave on 27/10/16.
 */

final class LocTone {

    private static final String TAG = "LocTone";

    private static final double AMPLITUDE_DISTANCE = 100; // metres from datum to max amp
    private static final double FREQUENCY_DISTANCE = 100; // metres from datum to max freq

    private double mLatitudeRadius = 6378137.0; // WGS84 major (equatorial) radius
    private double mLongitudeRadius = 6356752.3142; // WGS84 semi-major (polar) radius

    private double mRefLat;
    private double mRefLong;

    private double mDeltaLat;
    private double mDeltaLong;

    private double mAmplitude;
    private double mFrequency;

    LocTone(Location datum) {
        mRefLat = datum.getLatitude();
        mRefLong = datum.getLongitude();
        mLatitudeRadius = computeLatitudeRadius();
        updateLoc( datum);
    }

    void updateLoc( Location update) {
        mDeltaLat = computeLatitudeDistance( update.getLatitude());
        mDeltaLong = computeLongitudeDistance( update.getLongitude());

        mAmplitude = deltaToScalar( mDeltaLat, AMPLITUDE_DISTANCE);
        mFrequency = deltaToScalar( mDeltaLong, FREQUENCY_DISTANCE);
    }

    double getAmplitude() {
        return mAmplitude;
    }

    double getFrequency() {
        return mFrequency;
    }

    /**
     * Converts a positive or negative delta to a [0...1] scalar centered about 0.5
     * @param delta the offest parameter [-limit...limit]
     * @param limit the (symmetrical) limit to the range
     * @return the delta as a [0...1] scalar percentage of the limit
     */
    private double deltaToScalar( double delta, double limit) {
        // for positive or zero values, return between 0.5 and 1
        if ( delta >= 0) {
            return ( delta > limit) ? 1 : 0.5 + 0.5 * ( delta / limit);
        }
        // for negative values, return between 0 and 0.5
        delta *= -1;
        return ( delta > limit) ? 0 : 0.5 * ( delta / limit);
    }

    /**
     * Computes the distance between two latitudes.
     * @param latitude the new latitude, in degrees
     * @return the distance between the new latitude and datum, in metres
     */
    private double computeLatitudeDistance( double latitude) {

        // convert angular separation from degrees to radians
        double angle = (latitude - mRefLat) * Math.PI / 180.0;

        // arc length = angle * ( 2 * PI * radius)
        return 2.0 * Math.PI * mLatitudeRadius * angle;
    }

    /**
     * Computes the distance between two longitudes.
     * @param longitude the new longitude, in degrees
     * @return the distance between the new longitude and datum, in metres
     */
    private double computeLongitudeDistance( double longitude) {
        double angle = ( longitude - mRefLong) * Math.PI / 180.0;
        return 2.0 * Math.PI * mLongitudeRadius * angle;
    }

    /**
     * Computes Earth's radius at mRefLat.
     *            _______________________
     *           / (a²cosφ)² + (b²sinφ)²
     *  R(φ) =  / ----------------------
     *         √  (a•cosφ)² + (b•sinφ)²
     *
     * @return Earth's radius at mRefLat.
     */
    private double computeLatitudeRadius() {
        double a = mLatitudeRadius; // probably a bad way of doing this, can only be used once
        double b = mLongitudeRadius;
        double cosP = Math.cos( mRefLat * Math.PI / 180.0);
        double sinP = Math.sin( mRefLat * Math.PI / 180.0);

        return Math.sqrt( ( ( a * a * cosP) * ( a * a * cosP) + ( b * b * sinP) * ( b * b * sinP))
            / ( ( a * cosP) * ( a * cosP) + ( b * sinP) * ( b * sinP)));
    }
}
