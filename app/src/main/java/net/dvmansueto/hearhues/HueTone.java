package net.dvmansueto.hearhues;

import android.support.annotation.ColorInt;
import android.support.v4.graphics.ColorUtils;

import java.util.Locale;

import static android.graphics.Color.parseColor;

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

    /** Provides scalar:tone methods */
    private final ScalarTone mScalarTone;

    /** The {@link HueTone} in @ColorInt 0xAARRGGBB form */
    private int mRgb;

    /** The {@link HueTone} in HSL { Hue [0...360), Saturation [0...1], Lightness [0...1]} */
    private float[] mHsl = new float[3];

    /** The {@link HueTone} in isolated Hue [0...1] */
    private double mHue;

    /** The {@link HueTone} as a frequency (Hertz) */
    private double mTone;

    //------------------------
    // Constructor
    //------------------------

    /**
     * Creates a new {@link HueTone}
     * @param scalarTone provides access to the user's scalar:tone settings
     */
    HueTone( ScalarTone scalarTone) {
        mScalarTone = scalarTone;
        updateHueTone( parseColor( "#00FFFF")); // seed with mid-range cyan
    }


    //------------------------
    // Mutators
    //------------------------

    /**
     * (Constructor helper), resets all values after any change to HueTone.
     * @param color the ColorInt to import
     */
    private void updateHueTone( @ColorInt int color) {
        mRgb = color;
        ColorUtils.colorToHSL( mRgb, mHsl);
        mHue = mHsl[0] / 360;
        mTone = mScalarTone.scalarToTone( mHue);
    }

    /**
     * Creates a HueTone from a tone double.
     * @param tone frequency (Hertz) of the tone.
     */
    // TODO: implement a Tone to Hue activity (SeeSound, maybe?)
    @SuppressWarnings("unused")
    private void updateHueTone(double tone) {
        mTone = tone;
        mHue = mScalarTone.toneToScalar( tone);
        mHsl = new float[] { (float) ( mHue * 360), 1, (float) 0.5 }; // H [0...1], S=1, L=0.5
        mRgb = ColorUtils.HSLToColor( mHsl);
    }

    /**
     * @param color the color 0xAARRGGBB to import
     */
    void setHue(@ColorInt int color) {
        updateHueTone( color);
    }

    /**
     * @param tone the frequency (Hertz) to import
     */
    @SuppressWarnings("unused") // waiting for SeeSound!
    void setTone( double tone) {
        updateHueTone( toneToColorInt( tone));
    }


    //------------------------
    // Accessors
    //------------------------

    /**
     * @return the {@link HueTone} in @ColorInt (0xAARRGGBB) form
     */
    @ColorInt
    int getRgb() {
        return mRgb;
    }

    /**
     * @return the {@link HueTone} in frequency (Hertz) form
     */
    double getTone() {
        return mTone;
    }

    /**
     * Fetches the hue in RGB format.
     * @return the hue as a "#RRGGBB" format string.
     */
    String toRgbString() {
        return "#" + Integer.toHexString( mRgb).substring( 2, 8).toUpperCase();
    }

    /**
     * Fetches the hue in hue format.
     * @return the hue as a "xxx.x°" format string, hue [0...360).
     */
    String toHueString() {
        return String.format( Locale.getDefault(), "%4.1f", mHue * 360) + "°";
    }

    /**
     * Fetches the tone in frequency format.
     * @return the tone as a "xxxxxx.xx Hz" format string.
     */
    String toToneString() {
        // Locale.getDefault() for appropriate '.' or ',' decimal point
        return String.format(Locale.getDefault(), "%7.2f", mTone) + " Hz";
    }

    /**
     * Fetches the nearest half-note.
     * @return the nearest half-note as a formatted string.
     */
    String toNoteString() {
        return mScalarTone.toneToNoteString( mTone);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [Tone: " + Double.toString( getTone()) + ']' +
                " [RGB: #" + Integer.toHexString( getRgb()) + ']';
    }

    /**
     * Converts a tone to a @ColorInt
     * @param tone frequency (Hertz) to convert
     * @return ColorInt
     */
    @ColorInt
    private int toneToColorInt( double tone) {
        double hue = mScalarTone.toneToScalar( tone);
        float[] hsl = new float[] { (float) ( hue * 360f), 1f, 0.5f };
        return ColorUtils.HSLToColor( hsl);
    }

}