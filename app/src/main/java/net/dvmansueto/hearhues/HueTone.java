package net.dvmansueto.hearhues;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.util.Log;

import java.util.Arrays;
import java.util.Locale;

import static java.lang.Integer.parseInt;

/**
 * Provides HueTone objects with both colour and sound attributes.
 * Hues:
 *    0* = red
 *   60* = yellow
 *  120* = green
 *  180* = blue
 *  240* = indigo
 *  300* = pink
 *  360* = red
 */
final class HueTone {


    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "HearHue";

    private static final double BASE_FREQUENCY = 440; // A2 = 110 Hz
    private static final int HALF_STEPS_PER_RANGE = 48; // A2 -> A6 (1760 Hz)
    private static final double TWELFTH_ROOT_OF_2 = Math.pow( 2, (double) 1 / (double) 12);
    private static final double FREQUENCY_DENOMINATOR = HALF_STEPS_PER_RANGE * Math.log( TWELFTH_ROOT_OF_2);

    private int mRgb;   // mRgb is AARRGGBB
    private float[] mHsl = new float[3];   // mHsl is [0] Hue [0...360), [1] Saturation [0...1], [2] Lightness [0...1]
    private double mHue;     // mHue is HSL Hue [0...1)
    private double mTone;    // mTone is frequency corresponding to hue

    //// Constructors

    // Default constructor
    HueTone() {
        updateHueTone( BASE_FREQUENCY);
    }

    // Secondary constructors

    /**
     * Creates a HueTone from RGB component integers.
     * @param r the red component of RGB, [0...255]
     * @param g the green component of RGB, [0...255]
     * @param b the blue component of RGB, [0...255]
     */
    public HueTone( @IntRange( from = 0x0, to = 0xFF) int r,
                    @IntRange( from = 0x0, to = 0xFF) int g,
                    @IntRange( from = 0x0, to = 0xFF) int b) {
        updateHueTone( Color.rgb( r, g, b));
    }

    /**
     * Creates a HueTone from a Palette swatch.
     * @param swatch the palette swatch to import
     */
    public HueTone( @NonNull Palette.Swatch swatch) {
        updateHueTone( swatch.getRgb());
    }

    /**
     * Creates a HueTone from a HSL.
     * @param hsl 3-element array containing the HSL component floats to import
     */
    public HueTone( @NonNull float[] hsl) {
        updateHueTone( ColorUtils.HSLToColor( hsl));
    }

    /**
     * Creates a HueTone from an \"#RRGGBB\" string.
     * @param colorString \"#RRGGBB\" string to import
     */
    public HueTone( @NonNull String colorString) {
        int r = parseInt( colorString.substring( 1,2));
        int g = parseInt( colorString.substring( 3,2));
        int b = parseInt( colorString.substring( 5,2));
        updateHueTone( Color.rgb( r, g, b));
    }

    /**
     * Creates a HueTone from an @ColorInt int (#AARRGGBB).
     * @param color the ColorInt to import
     */
    public HueTone( @ColorInt int color) {
        updateHueTone( color);
    }

    // Primary Constructor / Constructor Helper
    /**
     * Primary constructor, resets all values after any change to HueTone.
     * @param color the ColorInt to import
     */
    private void updateHueTone( @ColorInt int color) {
        Log.d( TAG, "Color: " + Integer.toHexString( color));
        mRgb = color;
        ColorUtils.colorToHSL( mRgb, mHsl);
        mHue = mHsl[0] / 360;
        Log.d( TAG, "Hue:" + Double.toString( mHue));
        mTone = hueToTone( mHue);
        Log.d( TAG, "Tone:" + Double.toString( mTone));
    }

    // Overload for tone due to calculation expense of hueToTone()
    public HueTone( double tone) {
        updateHueTone( tone);
    }

    /**
     * Creates a HueTone from a tone double.
     * @param tone frequency (Hertz) of the tone.
     */
    private void updateHueTone( double tone) {
        mTone = tone;
        mHue = toneToHue( tone);
        mHsl = new float[] { (float) ( mHue * 360), 1, (float) 0.5 };
        mRgb = ColorUtils.HSLToColor( mHsl);
    }


    //// Mutators

    /**
     *
     * @param swatch the swatch to import.
     */
    void setHue(@NonNull Palette.Swatch swatch) {
        updateHueTone( swatch.getRgb());
    }

    public void setTone( double tone) {
        updateHueTone( toneToColorInt( tone));
    }

    //// Accessors
    @ColorInt
    int getRgb() {
        return mRgb;
    }

    float[] getHsl() {
        return mHsl;
    }

    double getHue() {
        return mHue;
    }

    double getTone() {
        return mTone;
    }

    String getHueString() {
        return "#" + Integer.toHexString( mRgb).substring( 0, 6).toUpperCase();
    }

    /**
     * Fetches the frequency as a formatted string.
     * @return the tone as a "xxxx.xx Hz" string.
     */
    String getToneString() {
        // Locale.getDefault() for appropriate '.' or ',' decimal point
        return String.format(Locale.getDefault(), "%4.2f", mTone) + " Hz";
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append(" [Hue: ").append(Double.toString(getHue())).append(']')
                .append(" [Tone: ").append(Double.toString(getTone())).append(']')
                .append(" [RGB: #").append(Integer.toHexString(getRgb())).append(']')
                .append(" [HSL: ").append(Arrays.toString(getHsl())).append(']')
                .toString();
    }


    //// Helpers

    /**
     * Converts a tone to a hue.
     * @param tone frequency (Hertz) to convert
     * @return the corresponding hue [0...1)
     */
    private double toneToHue( double tone) {
        return Math.log( tone / BASE_FREQUENCY) / FREQUENCY_DENOMINATOR;
    }

    /**
     * Converts a hue to a tone.
     *
     * @param hue the hue [0...1) to convert
     * @return the corresponding frequency (Hertz)
     */
    private double hueToTone( double hue) {
        Log.d( TAG, "BF: " + Double.toString( BASE_FREQUENCY));
        Log.d( TAG, "TR: " + Double.toString( TWELFTH_ROOT_OF_2));
        Log.d( TAG, "HS: " + Double.toString( HALF_STEPS_PER_RANGE));
        Log.d( TAG, "Hue: " + Double.toString( hue));
        Log.d( TAG, "Tone: " + BASE_FREQUENCY * Math.pow( TWELFTH_ROOT_OF_2, HALF_STEPS_PER_RANGE * hue));
        return BASE_FREQUENCY * Math.pow( TWELFTH_ROOT_OF_2, HALF_STEPS_PER_RANGE * hue);
    }

    /**
     * Converts a tone to a @ColorInt
     * @param tone frequency (Hertz) to convert
     * @return ColorInt
     */
    @ColorInt
    private int toneToColorInt( double tone) {
        double hue = toneToHue( tone);
        float[] hsl = new float[] { (float) ( hue * 360), 1, (float) 0.5 };
        return ColorUtils.HSLToColor( hsl);
    }


}