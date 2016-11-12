package net.dvmansueto.hearhues;

import android.location.Location;

import java.util.Locale;

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

    /** longitudinal radius of the earth at datum latitude, in metres */
    private final double mLatitudinalRadius;

    /** The origin/datum {@link Location} */
    private final Location mOrigin;

    /** The present/current {@link Location} */
    private Location mLocation;



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
        mLocation = mOrigin = origin;
        mLatitudinalRadius = computeLongitudinalRadius();
    }


    //--------------------------------
    // Mutators
    //--------------------------------

    /**
     * Updates the {@link LocTone} with a new {@link Location}.
     * @param location the new location
     */
    void setLocation( Location location) {
        mLocation = location;
    }


    //--------------------------------
    // Accessors
    //--------------------------------

    /**
     * Calculates the scalar position of this latitude relative to the window.
     * @return the position relative to the window [0...1]
     */
    double getScalarLatitude() {
        return deltaToScalar( latitudeToDelta( mLocation.getLatitude()), mLatitudeRange);
    }

    /**
     * Calculates the scalar position of this longitude relative to the window.
     * @return the position relative to the window [0...1]
     */
    double getScalarLongitude() {
        return deltaToScalar( longitudeToDelta( mLocation.getLongitude()), mLongitudeRange);
    }

    /**
     * Returns the location formatted as a decimal-degree string
     * @return the decimal-degree "latitude, longitude" string
     */
    String toDegreeString() {
        return Location.convert( mLocation.getLatitude(), Location.FORMAT_DEGREES) + ", " +
                Location.convert( mLocation.getLongitude(), Location.FORMAT_DEGREES);
    }

    String toCoordString() {
        return String.format(Locale.getDefault(), "%5.2f %5.2f",
                latitudeToDelta( mLocation.getLatitude()), longitudeToDelta( mLocation.getLongitude()));
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
        double angle = (latitude - mOrigin.getLatitude()) * Math.PI / 180.0;

        // arc length = angle * ( 2 * PI * radius)
        return 2.0 * Math.PI * mLatitudinalRadius * angle;
    }

    /**
     * Computes the distance from this longitude to the origin.
     * @param longitude the new longitude, in degrees
     * @return the distance between the new longitude and origin, in metres
     */
    private double longitudeToDelta(double longitude) {
        double angle = ( longitude - mOrigin.getLongitude()) * Math.PI / 180.0;
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
        final double cosP = Math.cos( mOrigin.getLatitude() * Math.PI / 180.0);
        final double sinP = Math.sin( mOrigin.getLatitude() * Math.PI / 180.0);
        return Math.sqrt( ( ( a * a * cosP) * ( a * a * cosP) + ( b * b * sinP) * ( b * b * sinP))
            / ( ( a * cosP) * ( a * cosP) + ( b * sinP) * ( b * sinP)));
    }
}
