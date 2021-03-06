package net.dvmansueto.hearhues;

import android.content.Context;
import android.util.Log;

import java.util.Locale;

/**
 * Provides a consistent interaction between scalars (percentages [0...1]) and musical tones,
 * taking into account sharedPreferences (updated in MainActivity).
 * Instantiated once in ApplicationContext.
 */
class ScalarTone {

    /** For {@link Log} */
    private static final String TAG = "ScalarTone";

    /** So we can push back 'fixed' sharedPreferences if user inputs invalid combinations. */
    private final Context mContext;

    private static final int HALF_STEPS_PER_OCTAVE = 12;
    private static final double TWELFTH_ROOT_OF_2 = Math.pow( 2, 1.0 / 12.0);

    private static final String[] NOTES_AS_NAMES = {
            "A₀", "A#₀/Bb₀", "B₀", "C₁", "C#₁/Db₁", "D₁", "D#₁/Eb₁", "E₁", "F₁", "F#₁/Gb₁", "G₁", "G#₁/Ab₁",
            "A₁", "A#₁/Bb₁", "B₁", "C₂", "C#₂/Db₂", "D₂", "D#₂/Eb₂", "E₂", "F₂", "F#₂/Gb₂", "G₂", "G#₂/Ab₂",
            "A₂", "A#₂/Bb₂", "B₂", "C₃", "C#₃/Db₃", "D₃", "D#₃/Eb₃", "E₃", "F₃", "F#₃/Gb₃", "G₃", "G#₃/Ab₃",
            "A₃", "A#₃/Bb₃", "B₃", "C₄", "C#₄/Db₄", "D₄", "D#₄/Eb₄", "E₄", "F₄", "F#₄/Gb₄", "G₄", "G#₄/Ab₄",
            "A₄", "A#₄/Bb₄", "B₄", "C₅", "C#₅/Db₅", "D₅", "D#₅/Eb₅", "E₅", "F₅", "F#₅/Gb₅", "G₅", "G#₅/Ab₅",
            "A₅", "A#₅/Bb₅", "B₅", "C₆", "C#₆/Db₆", "D₆", "D#₆/Eb₆", "E₆", "F₆", "F#₆/Gb₆", "G₆", "G#₆/Ab₆",
            "A₆", "A#₆/Bb₆", "B₆", "C₇", "C#₇/Db₇", "D₇", "D#₇/Eb₇", "E₇", "F₇", "F#₇/Gb₇", "G₇", "G#₇/Ab₇",
            "A₇", "A#₇/Bb₇", "B₇", "C₈", "C#₈/Db₈", "D₈", "D#₈/Eb₈", "E₈", "F₈", "F#₈/Gb₈", "G₈", "G#₈/Ab₈",
            "A₈"};
    private static final double[] NOTES_AS_HERTZ = {
            27.5, 29.14, 30.87, 32.7, 34.65, 36.71, 38.89, 41.2, 43.65, 46.25, 49, 51.91, 55, 58.27,
            61.74, 65.41, 69.3, 73.42, 77.78, 82.41, 87.31, 92.5, 98, 103.83, 110, 116.54, 123.47,
            130.81, 138.59, 146.83, 155.56, 164.81, 174.61, 185, 196, 207.65, 220, 233.08, 246.94,
            261.63, 277.18, 293.66, 311.13, 329.63, 349.23, 369.99, 392, 415.3, 440, 466.16, 493.88,
            523.25, 554.37, 587.33, 622.25, 659.25, 698.46, 739.99, 783.99, 830.61, 880, 932.33,
            987.77, 1046.5, 1108.73, 1174.66, 1244.51, 1318.51, 1396.91, 1479.98, 1567.98, 1661.22,
            1760, 1864.66, 1975.53, 2093, 2217.46, 2349.32, 2489.02, 2637.02, 2793.83, 2959.96,
            3135.96, 3322.44, 3520, 3729.31, 3951.07, 4186.01, 4434.92, 4698.63, 4978.03, 5274.04,
            5587.65, 5919.91, 6271.93, 6644.88, 7040};

    /** How far from a named note should we decide it really isn't that note anymore? */
    private static final double NOTE_THRESHOLD = 0.3;

    /** Lowest frequency */
    private double mBaseFrequency;
    /** Number of octaves of separation * 12 half steps per octave */
    private double mHalfStepsPerRange;


    //------------------------
    // Constructor
    //------------------------

    ScalarTone(Context context) {
        mContext = context;
    }


    //------------------------
    // Accessors
    //------------------------

    /**
     * Converts a tone to a scalar/percentage.
     * @param tone frequency (Hertz) to convert
     * @return the corresponding scalar [0...1]
     */
    double toneToScalar( double tone) {
        return Math.log( tone / mBaseFrequency) /
                ( mHalfStepsPerRange * Math.log( TWELFTH_ROOT_OF_2));
    }

    /**
     * Converts a scalar/percentage to a tone.
     *
     * @param scalar the hue [0...1] to convert
     * @return the corresponding frequency (Hertz)
     */
    double scalarToTone( double scalar) {
        return mBaseFrequency * Math.pow( TWELFTH_ROOT_OF_2, mHalfStepsPerRange * scalar);
    }

    /**
     * Fetches the nearest half-note.
     * @return the nearest half-note as a formatted string.
     */
    String toneToNoteString(double tone) {
        double minDistance = 100000; // larger than highest note
        int noteIdx = 0;
        for (int i = 0; i <= NOTES_AS_HERTZ.length; i++ ) {
            double distance = Math.abs( NOTES_AS_HERTZ[ i] - tone);
            if ( distance < minDistance) {
                minDistance = distance;
                noteIdx = i;
            } else {
                break; // ordered list; increasing distance => overshoot
            }
        }

        // if note is a long way from a note, suggest it is 'between' notes
        if ( noteIdx == 0 || noteIdx == NOTES_AS_HERTZ.length) return NOTES_AS_NAMES[ noteIdx];
        if ( minDistance > NOTE_THRESHOLD * ( NOTES_AS_HERTZ[ noteIdx] - NOTES_AS_HERTZ[ noteIdx - 1])) {
            return NOTES_AS_NAMES[ noteIdx - 1] + " ~ " + NOTES_AS_NAMES[ noteIdx];
        } else if ( minDistance > NOTE_THRESHOLD * ( NOTES_AS_HERTZ[ noteIdx + 1] - NOTES_AS_HERTZ[ noteIdx])) {
            return NOTES_AS_NAMES[ noteIdx] + " ~ " + NOTES_AS_NAMES[ noteIdx + 1];
        }
        return NOTES_AS_NAMES[ noteIdx];
    }


    //------------------------
    // Mutators
    //------------------------

    /**
     * Update the range of frequencies.
     * @param baseFrequency the lowest frequency
     * @param peakFrequency the highest frequency
     */
    void setFrequencyRange( double baseFrequency, double peakFrequency) {

        // ensure they're not the same
        if ( baseFrequency == peakFrequency) {

            // fix by shifting a valid boundary out an octave
            // Could assume XML has been set as specified: base options ending an octave less than
            // peak options, and then just doubling peak because it must be at worst the second
            // highest option, but this is more robust.
            if ( peakFrequency < NOTES_AS_HERTZ[ NOTES_AS_HERTZ.length - 1]) {
                peakFrequency *= 2;
                Utils.setStringPreference( mContext,
                        String.format( Locale.UK, "%d", (int) peakFrequency),
                        mContext.getResources().getString( R.string.prefs_generator_peak_frequency_key));
            } else {
                baseFrequency /= 2;
                Utils.setStringPreference( mContext,
                        String.format( Locale.UK, "%d", (int) peakFrequency),
                        mContext.getResources().getString( R.string.prefs_generator_base_frequency_key));
            }
        }

        // ensure they're the right way around
        if ( baseFrequency > peakFrequency) {

            // fix by swapping order
            mBaseFrequency = peakFrequency;
            peakFrequency = baseFrequency;
            baseFrequency = mBaseFrequency;

            // update shared preferences
            Utils.setStringPreference( mContext,
                    String.format( Locale.UK, "%d", (int) baseFrequency),
                    mContext.getResources().getString( R.string.prefs_generator_base_frequency_key));
            Utils.setStringPreference( mContext,
                    String.format( Locale.UK, "%d", (int) peakFrequency),
                    mContext.getResources().getString( R.string.prefs_generator_peak_frequency_key));
        }

        // find how many octaves between limits
        // bit of maths, but only runs when prefs change
        int baseOctave = 0;
        int peakOctave = 0;
//        double baseNote = NOTES_AS_HERTZ[ 0];
        double note;
        for (int i = 0; i <= NOTES_AS_HERTZ.length / HALF_STEPS_PER_OCTAVE; i++) {
            note = NOTES_AS_HERTZ[ i * HALF_STEPS_PER_OCTAVE];
//            note = baseNote * (int) Math.pow( 2, i);
            if ( baseFrequency == note) baseOctave = i;
            if ( peakFrequency == note) peakOctave = i;
        }

        mBaseFrequency = baseFrequency;
        mHalfStepsPerRange = ( peakOctave - baseOctave) * HALF_STEPS_PER_OCTAVE;
    }
}
