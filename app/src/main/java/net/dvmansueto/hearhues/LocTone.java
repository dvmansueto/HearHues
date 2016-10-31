package net.dvmansueto.hearhues;

import android.location.Location;

/**
 * LocTone: idea is to establish a datum, then use relative latitude and longitude to alter the
 * amplitude and frequency (or visa versa) of a generated tone.
 * Created by Dave on 27/10/16.
 */
final class LocTone {

    /** Earth major radius, as defined by WGS84, in metres */
    private static final double EQUATORIAL_RADIUS = 6378137.0;

    /** Earth semi-major radius as defined by WGS84, in metres */
    private static final double POLAR_RADIUS = 6356752.3142;

    /** 'height' which bounds the 'view', in metres */
    private final double mLatitudeRange;

    /** 'width' which bounds the 'view', in metres */
    private final double mLongitudeRange;

    /** Latitude of origin */
    private final double mOriginLatitude;

    /** Longitude at origin */
    private final double mOriginLongitude;

    /** longitudinal radius of the earth at datum latitude, in metres */
    private final double mLatitudinalRadius;


    //--------------------------------
    // Constructor
    //--------------------------------

    /**
     * Creates a new LocTone.
     * @param origin the 'datum' position
     * @param latitudeRange the 'height' for valid coordinates, in metres
     * @param longitudeRange the 'width' for valid coordinates, in metres
     */
    LocTone( double latitudeRange, double longitudeRange, Location origin) {
        mLatitudeRange = latitudeRange;
        mLongitudeRange = longitudeRange;
        mOriginLatitude = origin.getLatitude();
        mOriginLongitude = origin.getLongitude();
        mLatitudinalRadius = computeLongitudinalRadius();
    }


    //--------------------------------
    // Helpers
    //--------------------------------

    /**
     * Calculates the scalar position of this latitude relative to the window.
     * @param latitude the latitude to compare
     * @return the position relative to the window [0...1]
     */
    double scalarLatitude(double latitude) {
        return deltaToScalar( latitudeToDelta( latitude), mLatitudeRange);
    }

    /**
     * Calculates the scalar position of this longitude relative to the window.
     * @param longitude the latitude to compare
     * @return the position relative to the window [0...1]
     */
    double scalarLongitude(double longitude) {
        return deltaToScalar( longitudeToDelta( longitude), mLongitudeRange);
    }


    //--------------------------------
    // Utilities
    //--------------------------------

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
     * Computes the distance from this latitude to the origin.
     * @param latitude the new latitude, in degrees
     * @return the distance between the new latitude and origin, in metres
     */
    private double latitudeToDelta(double latitude) {

        // convert angular separation from degrees to radians
        double angle = (latitude - mOriginLatitude) * Math.PI / 180.0;

        // arc length = angle * ( 2 * PI * radius)
        return 2.0 * Math.PI * mLatitudinalRadius * angle;
    }

    /**
     * Computes the distance from this longitude to the origin.
     * @param longitude the new longitude, in degrees
     * @return the distance between the new longitude and origin, in metres
     */
    private double longitudeToDelta(double longitude) {
        double angle = ( longitude - mOriginLongitude) * Math.PI / 180.0;
        return 2.0 * Math.PI * POLAR_RADIUS * angle;
    }

    /**
     * Computes Earth's longitudinal radius at mOriginLatitude.
     *            _______________________
     *           / (a²cosφ)² + (b²sinφ)²
     *  R(φ) =  / ----------------------
     *         √  (a•cosφ)² + (b•sinφ)²
     *
     *  a = equatorial radius, b = polar radius
     *
     * @return Earth's radius at mOriginLatitude
     */
    private double computeLongitudinalRadius() {
        final double a = EQUATORIAL_RADIUS;
        final double b = POLAR_RADIUS;
        final double cosP = Math.cos( mOriginLatitude * Math.PI / 180.0);
        final double sinP = Math.sin( mOriginLatitude * Math.PI / 180.0);
        return Math.sqrt( ( ( a * a * cosP) * ( a * a * cosP) + ( b * b * sinP) * ( b * b * sinP))
            / ( ( a * cosP) * ( a * cosP) + ( b * sinP) * ( b * sinP)));
    }
}
