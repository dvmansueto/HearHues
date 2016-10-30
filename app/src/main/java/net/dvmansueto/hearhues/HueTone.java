package net.dvmansueto.hearhues;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.util.Log;

import java.util.Arrays;
import java.util.Locale;

import static android.graphics.Color.parseColor;
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
    private static final String TAG = "HueTone";

    private ScalarTone mScalarTone;

//    // all for updating incorrectly selected preferences
//    private final Context mContext;
//
//    private static final int HALF_STEPS_PER_OCTAVE = 12;
//    private static final double TWELFTH_ROOT_OF_2 = Math.pow( 2, (double) 1 / (double) 12);
//
//    private double mBaseFrequency;
//    private double mHalfStepsPerRange;

    private int mRgb;   // mRgb is 0hAARRGGBB
    private float[] mHsl = new float[3];   // mHsl is [0] Hue [0...360), [1] Saturation [0...1], [2] Lightness [0...1]
    private double mHue;     // mHue is HSL Hue [0...1)
    private double mTone;    // mTone is frequency corresponding to hue

//// this version used unicode characters which didn't match other text on the actual device,
//// so using 'hash' and 'b' version below.
////    private static final String[] NOTE_NAMES = {
////            "C₀", "C♯₀/D♭₀", "D₀", "D♯₀/E♭₀", "E₀", "F₀", "F♯₀/G♭₀", "G₀", "G♯₀/A♭₀", "A₀", "A♯₀/B♭₀", "B₀",
////            "C₁", "C♯₁/D♭₁", "D₁", "D♯₁/E♭₁", "E₁", "F₁", "F♯₁/G♭₁", "G₁", "G♯₁/A♭₁", "A₁", "A♯₁/B♭₁", "B₁",
////            "C₂", "C♯₂/D♭₂", "D₂", "D♯₂/E♭₂", "E₂", "F₂", "F♯₂/G♭₂", "G₂", "G♯₂/A♭₂", "A₂", "A♯₂/B♭₂", "B₂",
////            "C₃", "C♯₃/D♭₃", "D₃", "D♯₃/E♭₃", "E₃", "F₃", "F♯₃/G♭₃", "G₃", "G♯₃/A♭₃", "A₃", "A♯₃/B♭₃", "B₃",
////            "C₄", "C♯₄/D♭₄", "D₄", "D♯₄/E♭₄", "E₄", "F₄", "F♯₄/G♭₄", "G₄", "G♯₄/A♭₄", "A₄", "A♯₄/B♭₄", "B₄",
////            "C₅", "C♯₅/D♭₅", "D₅", "D♯₅/E♭₅", "E₅", "F₅", "F♯₅/G♭₅", "G₅", "G♯₅/A♭₅", "A₅", "A♯₅/B♭₅", "B₅",
////            "C₆", "C♯₆/D♭₆", "D₆", "D♯₆/E♭₆", "E₆", "F₆", "F♯₆/G♭₆", "G₆", "G♯₆/A♭₆", "A₆", "A♯₆/B♭₆", "B₆",
////            "C₇", "C♯₇/D♭₇", "D₇", "D♯₇/E♭₇", "E₇", "F₇", "F♯₇/G♭₇", "G₇", "G♯₇/A♭₇", "A₇", "A♯₇/B♭₇", "B₇",
////            "C₈", "C♯₈/D♭₈", "D₈", "D♯₈/E♭₈", "E₈", "F₈", "F♯₈/G♭₈", "G₈", "G♯₈/A♭₈", "A₈", "A♯₈/B♭₈", "B₈"};
//
//    private static final String[] NOTE_NAMES = {
//        "C₀", "C#₀/Db₀", "D₀", "D#₀/Eb₀", "E₀", "F₀", "F#₀/Gb₀", "G₀", "G#₀/Ab₀", "A₀", "A#₀/Bb₀", "B₀",
//        "C₁", "C#₁/Db₁", "D₁", "D#₁/Eb₁", "E₁", "F₁", "F#₁/Gb₁", "G₁", "G#₁/Ab₁", "A₁", "A#₁/Bb₁", "B₁",
//        "C₂", "C#₂/Db₂", "D₂", "D#₂/Eb₂", "E₂", "F₂", "F#₂/Gb₂", "G₂", "G#₂/Ab₂", "A₂", "A#₂/Bb₂", "B₂",
//        "C₃", "C#₃/Db₃", "D₃", "D#₃/Eb₃", "E₃", "F₃", "F#₃/Gb₃", "G₃", "G#₃/Ab₃", "A₃", "A#₃/Bb₃", "B₃",
//        "C₄", "C#₄/Db₄", "D₄", "D#₄/Eb₄", "E₄", "F₄", "F#₄/Gb₄", "G₄", "G#₄/Ab₄", "A₄", "A#₄/Bb₄", "B₄",
//        "C₅", "C#₅/Db₅", "D₅", "D#₅/Eb₅", "E₅", "F₅", "F#₅/Gb₅", "G₅", "G#₅/Ab₅", "A₅", "A#₅/Bb₅", "B₅",
//        "C₆", "C#₆/Db₆", "D₆", "D#₆/Eb₆", "E₆", "F₆", "F#₆/Gb₆", "G₆", "G#₆/Ab₆", "A₆", "A#₆/Bb₆", "B₆",
//        "C₇", "C#₇/Db₇", "D₇", "D#₇/Eb₇", "E₇", "F₇", "F#₇/Gb₇", "G₇", "G#₇/Ab₇", "A₇", "A#₇/Bb₇", "B₇",
//        "C₈", "C#₈/Db₈", "D₈", "D#₈/Eb₈", "E₈", "F₈", "F#₈/Gb₈", "G₈", "G#₈/Ab₈", "A₈", "A#₈/Bb₈", "B₈"};
//    private static final double[] NOTE_FREQS = {
//            16.35, 17.32, 18.35, 19.45, 20.6, 21.83, 23.12, 24.5, 25.96, 27.5, 29.14, 30.87,
//            32.7, 34.65, 36.71, 38.89, 41.2, 43.65, 46.25, 49, 51.91, 55, 58.27, 61.74,
//            65.41, 69.3, 73.42, 77.78, 82.41, 87.31, 92.5, 98, 103.83, 110, 116.54, 123.47,
//            130.81, 138.59, 146.83, 155.56, 164.81, 174.61, 185, 196, 207.65, 220, 233.08, 246.94,
//            261.63, 277.18, 293.66, 311.13, 329.63, 349.23, 369.99, 392, 415.3, 440, 466.16, 493.88,
//            523.25, 554.37, 587.33, 622.25, 659.25, 698.46, 739.99, 783.99, 830.61, 880, 932.33, 987.77,
//            1046.5, 1108.73, 1174.66, 1244.51, 1318.51, 1396.91, 1479.98, 1567.98, 1661.22, 1760, 1864.66, 1975.53,
//            2093, 2217.46, 2349.32, 2489.02, 2637.02, 2793.83, 2959.96, 3135.96, 3322.44, 3520, 3729.31, 3951.07,
//            4186.01, 4434.92, 4698.63, 4978.03, 5274.04, 5587.65, 5919.91, 6271.93, 6644.88, 7040, 7458.62, 7902.13};
//    private static final double NOTE_THRESHOLD = 0.3; // distance from a named note before suggesting it isn't really that note anymore.

    //// Constructors

    // Constructor
    HueTone( ScalarTone scalarTone) {
        mScalarTone = scalarTone;
        updateHueTone( parseColor( "#00FFFF")); // seed with mid-range cyan
    }


    // Constructor Helper
    /**
     * (Constructor helper), resets all values after any change to HueTone.
     * @param color the ColorInt to import
     */
    private void updateHueTone( @ColorInt int color) {
        mRgb = color;
        ColorUtils.colorToHSL( mRgb, mHsl);
        mHue = mHsl[0] / 360;
        mTone = mScalarTone.scalarToTone( mHue);
//        Log.d( TAG, "color: " + Integer.toHexString( color));
//        Log.d( TAG, "mRgb: " + Integer.toHexString( color));
//        Log.d( TAG, "toHueString: " + toHueString());
//        Log.d( TAG, "mHue:" + Double.toString( mHue));
//        Log.d( TAG, "mTone:" + Double.toString( mTone));
//        Log.d( TAG, "toneToColorInt: " + Integer.toHexString( toneToColorInt( mTone)));
//        Log.d( TAG, "toneToHue: " + Double.toString( toneToHue( mTone)));
//        Log.d( TAG, "t2h( h2t( t2h)): " + Double.toString( toneToHue( hueToTone( toneToHue( mTone)))));
//        Log.d( TAG, "h2t( t2h( h2t)): " + Double.toString( hueToTone( toneToHue( hueToTone( mHue)))));
    }

    /**
     * Creates a HueTone from a tone double.
     * @param tone frequency (Hertz) of the tone.
     */
    private void updateHueTone( double tone) {
        mTone = tone;
        mHue = mScalarTone.toneToScalar( tone);
        mHsl = new float[] { (float) ( mHue * 360), 1, (float) 0.5 }; // H [0...1], S=1, L=0.5
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

    void setTone( double tone) {
        updateHueTone( toneToColorInt( tone));
    }

//    void setFrequencies( double baseFrequency, double peakFrequency) {
//
//        if ( baseFrequency == peakFrequency) {
//            peakFrequency *= 2; // jump an octave
//            // update shared preference
//            Utils.setStringPreference( mContext,
//                    String.format( Locale.UK, "%d", (int) peakFrequency),
//                    mContext.getResources().getString( R.string.prefs_generator_peak_frequency_key));
//        }
//
//        if ( baseFrequency > peakFrequency) {
//            mBaseFrequency = peakFrequency;
//            peakFrequency = baseFrequency;
//            baseFrequency = mBaseFrequency;
//
//            // update shared preferences
//            Utils.setStringPreference( mContext,
//                    String.format( Locale.UK, "%d", (int) baseFrequency),
//                    mContext.getResources().getString( R.string.prefs_generator_base_frequency_key));
//            Utils.setStringPreference( mContext,
//                    String.format( Locale.UK, "%d", (int) peakFrequency),
//                    mContext.getResources().getString( R.string.prefs_generator_peak_frequency_key));
//        }
//
//        // find how many octaves between limits
//        // nasty bit of maths, but only runs when prefs change.
//        // (probably should look at a listener instead...)
//        int baseOctave = 0;
//        int peakOctave = 0;
//        int powerOfTwo;
//        double baseNote = 27.5; // could get using context...
//        double note;
//        for ( int i = 0; i <= 8; i++) {
//            powerOfTwo = (int) Math.pow( 2, i);
//            note = baseNote * powerOfTwo;
//            if ( baseFrequency == note) baseOctave = i;
//            if ( peakFrequency == note) peakOctave = i;
//        }
//
//        mBaseFrequency = baseFrequency;
//        mHalfStepsPerRange = ( peakOctave - baseOctave) * HALF_STEPS_PER_OCTAVE;
//
//        Log.d( TAG, "Base frequency: " + Double.toString( baseFrequency));
//        Log.d( TAG, "Peak frequency: " + Double.toString( peakFrequency));
//    }

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
        return mScalarTone.toNoteString( mTone);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [Hue: " + Double.toString( getHue()) + ']' +
                " [Tone: " + Double.toString( getTone()) + ']' +
                " [RGB: #" + Integer.toHexString( getRgb()) + ']' +
                " [HSL: " + Arrays.toString( getHsl()) + ']';
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