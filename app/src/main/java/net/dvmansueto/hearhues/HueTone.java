package net.dvmansueto.hearhues;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;

import java.util.Arrays;

import static java.lang.Integer.parseInt;

/**
 * Provides HueTone objects with both colour and sound attributes.
 */
public final class HueTone {
    private int mRed;   // mRed is RGB Red [0...255]
    private int mGreen; // mGreen is RGB Green [0...255]
    private int mBlue;  // mBlue is RGB Blue [0...255]
    private int mRgb;   // mRgb is AARRGGBB
    private float[] mHsl;   // mHsl is [0] Hue [0...360), [1] Saturation [0...1], [2] Lightness [0...1]
    private float mHue;     // mHue is HSL Hue [0...1)
    private float mTone;    // mTone is frequency corresponding to hue

    //// Constructors

    // Default constructor
    public HueTone() {
        updateHueTone( 0);
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

    // Converts from tone to colour and back again...
    /**
     * Creates a HueTone from a tone float.
     * @param tone frequency (Hertz) of the tone.
     */
    public HueTone( @NonNull float tone) {
        updateHueTone( toneToColorInt( tone));
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
        mRed = Color.red( color);
        mGreen = Color.green( color);
        mBlue = Color.blue( color);
        mRgb = color;
        ColorUtils.colorToHSL( mRgb, mHsl);
        mHue = mHsl[0] / 360;
        mTone = hueToTone( mHue);
    }


    //// Mutators

    /**
     *
     * @param swatch
     */
    public void setHue( @NonNull Palette.Swatch swatch) {
        updateHueTone( swatch.getRgb());
    }

    public void setTone( @NonNull float tone) {
        updateHueTone( toneToColorInt( tone));
    }

    //// Accessors
    @ColorInt
    public int getRgb() {
        return mRgb;
    }

    public float[] getHsl() {
        return mHsl;
    }

    public float getHue() {
        return mHue;
    }

    public float getTone() {
        return mTone;
    }

    public String getColorString() {
        return "#" + Integer.toHexString( mRgb).substring( 0, 6).toUpperCase();
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append(" [Hue: ").append(Float.toString(getHue())).append(']')
                .append(" [Tone: ").append(Float.toString(getTone())).append(']')
                .append(" [RGB: #").append(Integer.toHexString(getRgb())).append(']')
                .append(" [HSL: ").append(Arrays.toString(getHsl())).append(']')
                .toString();
    }


    //// Helpors

    /**
     * Converts a tone to a hue.
     * @param tone frequency (Hertz) to convert
     * @return the corresponding hue [0...1)
     */
    private float toneToHue(float tone) {
        float hue = 0;
        return hue;
    }

    /**
     * Converts a hue to a tone.
     * @param hue the hue [0...1) to convert
     * @return the corresponding frequency (Hertz)
     */
    private float hueToTone(float hue) {
        float tone = 0;
        return tone;
    }

    /**
     * Converts a tone to a @ColorInt
     * @param tone frequency (Hertz) to convert
     * @return ColorInt
     */
    @ColorInt
    private int toneToColorInt( float tone) {
        float hue = toneToHue( tone);
        float[] hsl = new float[] { hue * 360, 1, ( float) 0.5 };
        return ColorUtils.HSLToColor( hsl);
    }
}