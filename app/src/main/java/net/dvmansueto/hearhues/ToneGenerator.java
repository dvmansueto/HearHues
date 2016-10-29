package net.dvmansueto.hearhues;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Created by Dave on 13/10/16.
 *
 * using http://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
 *  originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
 *  and modified by Steve Pomeroy <steve@staticfree.info>
 *  plus https://gist.github.com/SuspendedPhan/7596139
 */

public class ToneGenerator {

    /**
     * For {@link Log}
     */
    private static final String TAG = "ToneGenerator";

    /**
     * Number of samples per second.
     * Somewhat arbitrary selection!
     */
    private static final int SAMPLE_RATE = 9600;

    /**
     * Percent of the total tone over which to ramp up to and down from full amplitude.
     */
    private static final double RAMP_PERCENT = 0.1;

    /**
     * 2^15 = 32767, used for 16-bit WAV
     */
    private static final int TWO_TO_THE_POWER_OF_FIFTEEN = 32767; // 2^15 for 16-bit WAV

    /**
     * Whether the {@link ToneGenerator} should be muted (silent) or not.
     */
    private boolean mMuted;

    private double mVolume;

    /**
     * The frequency of the tone, in Hz.
     */
    private double mFrequency;

    /**
     * The amplitude of the tone, [0...1].
     */
    private double mAmplitude;

    /**
     * How long a tone to generate.
     */
    private double mPlaybackSeconds;

    /**
     * Allows contextual shortening or lengthening of playback duration.
     */
    private double mPlaybackFactor;

    /**
     * Playback mode of the AudioTrack, valid options are {@link AudioTrack#MODE_STATIC} and
     * {@link AudioTrack#MODE_STREAM}.
     */
    private int mPlaybackMode;

    /**
     * The frequency and amplitude of the sound at a discrete sample of time.
     */
    private double[] mToneSamples;

    /**
     * A version of {@link #mToneSamples} modified to suit {@link AudioTrack}.
     */
    private byte[] mToneBytes;

    /**
     * The {@link AudioTrack} used by this {@link ToneGenerator}
     */
    private AudioTrack mAudioTrack;

    /**
     * Is the AudioTrack currently playing?
     */
    private boolean mPlaying;

    /**
     * Should the AudioTrack play continuously?
     */
    private boolean mPlayContinuously;

    /**
     * Custom listener.
     */
    private ToneGeneratorListener mListener;

    /**
     * Should the tone ramp up gently at the start?
     */
    private boolean mRampingUp;

    /**
     * Should the tone ramp down gently at the end?
     */
    private boolean mRampingDown;


    //// Listener

    /**
     * Custom listener, reports {@link #startedPlaying()} and {@link #stoppedPlaying()}.
     */
    interface ToneGeneratorListener {
        void startedPlaying();
        void stoppedPlaying();
    }

    /**
     * Assign a new {@link ToneGeneratorListener} to this {@link ToneGenerator}.
     * @param listener the new {@link ToneGeneratorListener}
     */
    void setToneGeneratorListener( ToneGeneratorListener listener) {
        mListener = listener;
    }


    //// Constructor

    /**
     * Creates a new {@link ToneGenerator} using arbitrary default values.
     */
    ToneGenerator() {
        mListener = null;
        mFrequency = 440.0;
        mAmplitude = 1.0;
        mPlaybackSeconds = 1.6;
        mPlaybackFactor = 1.0;
        mRampingUp = true;
        mRampingDown = true;
        mPlaybackMode = AudioTrack.MODE_STATIC;
        prepareForTone();
        mMuted = false;
        mPlaying = false;
        mPlayContinuously = false;
    }

    void logThis() {
        Log.d( TAG, "Amplitude: " + Double.toString( mAmplitude));
        Log.d( TAG, "Frequency: " + Double.toString( mFrequency));
        Log.d( TAG, "ToneSamples: " + Integer.toString( mToneSamples.length) + ", " +
                ( mToneSamples == null ? "null" : "not null"));
        Log.d( TAG, "ToneBytes: " + Integer.toString( mToneBytes.length) + ", " +
                ( mToneBytes == null ? "null" : "not null"));
        Log.d( TAG, "Duration: " + Double.toString( mPlaybackSeconds * mPlaybackFactor));
        Log.d( TAG, "Mode: " + ( mPlaybackMode == 0 ? "STATIC" : "STREAM"));
        Log.d( TAG, "Volume: " + Double.toString( mVolume));
        Log.d( TAG, "Muted: " + Boolean.toString( mMuted));
        Log.d( TAG, "Playing: " + Boolean.toString( mPlaying));
        Log.d( TAG, "Continuous: " + Boolean.toString( mPlayContinuously));
        Log.d( TAG, "Ramp Up: " + Boolean.toString( mRampingUp));
        Log.d( TAG, "Ramp Down: " + Boolean.toString( mRampingDown));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [Amplitude: " + Double.toString( mAmplitude) + ']' +
                " [Frequency: " + Double.toString( mFrequency) + ']' +
                " [ToneSamples: " + Integer.toString( mToneSamples.length) + ", " +
                ( mToneSamples == null ? "null" : "not null") + ']' +
                " [ToneBytes: " + Integer.toString( mToneBytes.length) + ", " +
                ( mToneBytes == null ? "null" : "not null") + ']' +
                " [Duration: " + Double.toString( mPlaybackSeconds * mPlaybackFactor) + ']' +
                " [Volume: " + Double.toString( mVolume) + ']' +
                " [Muted: " + Boolean.toString( mMuted) + ']' +
                " [Playing: " + Boolean.toString( mPlaying) + ']' +
                " [Continuous: " + Boolean.toString( mPlayContinuously) + ']' +
                " [Ramp Up: " + Boolean.toString( mRampingUp) + ']' +
                " [Ramp Down: " + Boolean.toString( mRampingDown) + ']';
    }

    //// Helpers

    /**
     * Starts tone generation.
     */
    void play() {

        if ( mPlaying) {
            stop();
        }

        playTone( mToneBytes);
    }

    /**
     * Cleanly stops tone generation.
     */
    void stop() {
        if ( mPlaying) {
            mAudioTrack.stop();
            mPlaying = false;
            if ( mListener != null) mListener.stoppedPlaying();
            if ( mPlayContinuously) play();
        }
    }

    /**
     * Flushes the tone out of the device's playback buffer.
     */
    void flush() {
        mAudioTrack.flush();
    }


    //// Accessors

    /**
     * @return whether the {@link ToneGenerator} is muted (silent) or not.
     */
    boolean getMuted() {
        return mMuted;
    }


    //// Mutators

    /**
     * Sets the volume of {@link #mAudioTrack} based on {@link #mMuted} and {@link #mAmplitude}.
     */
    private void setVolume() {
        Log.d( TAG, "Muted: " + Boolean.toString( mMuted));
        Log.d( TAG, "Amplitude: " + Double.toString( mAmplitude));
        Log.d( TAG, "Volume: " + Double.toString( mVolume));
        mVolume = mMuted ? 0 : mAmplitude;
        Log.d( TAG, "Volume: " + Double.toString( mVolume));
        mAudioTrack.setVolume( (float) mVolume);
    }

    /**
     * Sets whether the {@link ToneGenerator} should be muted (silent) or not.
     * @param state new muted state.
     */
    void setMuted( boolean state) {
        if ( mMuted != state) {
            mMuted = state;
            setVolume();
        }
    }

    /**
     * Sets the amplitude AKA gain AKA level of the generated tone.
     * @param amplitude the new value [0...1]
     */
    void setAmplitude( double amplitude) {
        mAmplitude = amplitude;
        setVolume();
    }


    /**
     * Sets the frequency of the generated tone.
     * @param frequency the new value (Hz)
     */
    void setFrequency( double frequency) {
        mFrequency = frequency;
        generateTone();
    }

    /**
     * Scales the playback duration.
     * @param factor the new playback scaling factor.
     */
    void setPlaybackFactor( double factor) {
        mPlaybackFactor = factor;
        prepareForTone();
    }

    /**
     * Sets the duration of the generated tone.
     * @param seconds how long to generate sound for
     */
    void setPlaybackSeconds( double seconds) {
        mPlaybackSeconds = seconds;
        prepareForTone();
    }

    /**
     * Sets the playback mode of the {@link AudioTrack}, valid options are:
     *  • {@link AudioTrack#MODE_STATIC} and
     *  • {@link AudioTrack#MODE_STREAM}.
     *
     * @param mode the new playback mode.
     */
    void setPlaybackMode( int mode) {
        if ( mode == AudioTrack.MODE_STATIC || mode == AudioTrack.MODE_STREAM)
        mPlaybackMode = mode;
    }

    /**
     * Sets the ToneGenerator to play continuously.
     * @param state whether or not to play continuously
     */
    void setPlayContinuously( boolean state) {
        mPlayContinuously = state;
        if ( mPlayContinuously) play();
    }

    /**
     * @param state whether gently increase the volume at the start of the audio track.
     */
    void setRampingUp( boolean state) {
        mRampingUp = state;
    }

    /**
     * @param state whether to gently lower the volume at the end of the audio track.
     */
    void setRampingDown( boolean state) {
        mRampingDown = state;
    }


    //// Methods

    /**
     * Prepares sound arrays ({@link #mToneSamples} & {@link #mToneBytes}) and
     * {@link #mAudioTrack}, attaches a {@link AudioTrack.OnPlaybackPositionUpdateListener}.
     */
    private void prepareForTone() {
        mToneSamples = new double[ (int) ( mPlaybackFactor * mPlaybackSeconds * SAMPLE_RATE)];
        mToneBytes = new byte[ 2 * mToneSamples.length];
        // create anew each time to set buffer size
        // since setter & getter methods require API v24 & v23 respectively.
        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mToneBytes.length,
                mPlaybackMode);
        mAudioTrack.setNotificationMarkerPosition( mToneSamples.length); // counts samples not bytes!
        mAudioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onPeriodicNotification(AudioTrack track) {
            }

            @Override
            public void onMarkerReached(AudioTrack track) {
                Log.d(TAG, "Reached end of audioTrack");
                stop();
            }
        });
    }

    /**
     * Fills the sound arrays with {@link #mFrequency} Hz tone.
     */
    private void generateTone() {

        // generate the sound samples
        for (int i = 0; i < mToneSamples.length; ++i) {
            mToneSamples[ i] = Math.sin( i * 2 * Math.PI * mFrequency / SAMPLE_RATE);
        }

        // generate the sound bytes
        for (int i = 0, j = 0; i < mToneSamples.length; i++ ) {

            short sample;
            if ( i < mToneSamples.length * RAMP_PERCENT && mRampingUp) {
                sample = (short) ( mToneSamples[ i] * TWO_TO_THE_POWER_OF_FIFTEEN);
            }
            else if ( i > mToneSamples.length * ( 1 - RAMP_PERCENT) && mRampingDown) {
                sample = (short) ( mToneSamples[ i] * TWO_TO_THE_POWER_OF_FIFTEEN);
            }
            else {
                sample = (short) ( mToneSamples[ i] * TWO_TO_THE_POWER_OF_FIFTEEN);
            }

            mToneBytes[ j++] = (byte) ( sample & 0x00FF);
            mToneBytes[ j++] = (byte) ( ( sample & 0xFF00) >>> 8);
        }
    }

    /**
     * Sets the audio track and starts playing.
     * @param soundBytes the sound to play.
     */
    private void playTone( byte[] soundBytes) {
//        logThis();
        mPlaying = true;
        mAudioTrack.write( soundBytes, 0, soundBytes.length);
        mAudioTrack.play();
        mListener.startedPlaying();
    }
}
