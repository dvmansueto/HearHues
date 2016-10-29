package net.dvmansueto.hearhues;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
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

    /**
     * The current volume, muted or otherwise, of the audio track.
     */
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
//    private boolean mPlaying;

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

    /**
     * For
     */
    private AsyncPlaybackTask mAsyncPlaybackTask = new AsyncPlaybackTask();
    private AsyncGenerateTask mAsyncGenerateTask = new AsyncGenerateTask();


    //--------------------------------
    // Listener
    //--------------------------------

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


    //--------------------------------
    // Constructor
    //--------------------------------

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
//        mPlaying = false;
        mPlayContinuously = false;
    }


    //--------------------------------
    // Helpers
    //--------------------------------

    /**
     * Starts tone generation.
     */
    void play() {
//        if ( mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
//            stop();
//        }
        mAudioTrack.write( mToneBytes, 0, mToneBytes.length);
        mAsyncPlaybackTask = new AsyncPlaybackTask();
        mAsyncPlaybackTask.execute();
        mListener.startedPlaying();
    }


    /**
     * Cleanly stops tone generation.
     */
    void stop() {
        if ( mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mAsyncPlaybackTask.cancel( true);
        }
        if ( mListener != null) mListener.stoppedPlaying();
    }

    /**
     * Flushes the tone out of the device's playback buffer.
     */
    void flush() {
        mAudioTrack.flush();
    }


    //--------------------------------
    // Accessors
    //--------------------------------

    /**
     * @return whether the {@link ToneGenerator} is muted (silent) or not.
     */
    boolean getMuted() {
        return mMuted;
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
                " [PlayState: " + Integer.toString( mAudioTrack.getPlayState()) + ']' +
                " [Continuous: " + Boolean.toString( mPlayContinuously) + ']' +
                " [Ramp Up: " + Boolean.toString( mRampingUp) + ']' +
                " [Ramp Down: " + Boolean.toString( mRampingDown) + ']';
    }


    //--------------------------------
    // Mutators
    //--------------------------------

    /**
     * Sets the volume of {@link #mAudioTrack} based on {@link #mMuted} and {@link #mAmplitude}.
     */
    private void setVolume() {
        // via variable as AudioTrack doesn't return volume
        mVolume = mMuted ? 0 : linToLog( mAmplitude);
        mAudioTrack.setVolume( (float) mVolume);
    }

    /**
     * Converts a linear value [0...1] to a logarithmic value [0...1].
     * @param x the linear value to convert
     * @return a logarithmic value corresponding to the input value.
     */
    private double linToLog( double x) {
        return 0.01 * Math.pow( 10.0, 2.0 * x);
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

        // cancel any existing tone generation task and start a new one
        mAsyncGenerateTask.cancel( true);
        mAsyncGenerateTask = new AsyncGenerateTask();
        mAsyncGenerateTask.execute();
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


    //--------------------------------
    // Utilities
    //--------------------------------

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
                if ( mPlayContinuously) play();
            }
        });
    }

    private void generateTone() {

        // generate the sound samples
        for (int i = 0; i < mToneSamples.length; ++i) {
            mToneSamples[ i] = Math.sin( i * 2 * Math.PI * mFrequency / SAMPLE_RATE);
        }

        // generate the sound bytes
        for (int i = 0, j = 0; i < mToneSamples.length; i++ ) {

            short sample;
            if ( i < mToneSamples.length * RAMP_PERCENT && mRampingUp) {
                sample = (short) ( mToneSamples[ i] * TWO_TO_THE_POWER_OF_FIFTEEN *
                        i / ( mToneSamples.length * RAMP_PERCENT));
            }
            else if ( i > mToneSamples.length * ( 1 - RAMP_PERCENT) && mRampingDown) {
                sample = (short) ( mToneSamples[ i] * TWO_TO_THE_POWER_OF_FIFTEEN *
                        ( mToneSamples.length - i) / ( mToneSamples.length * RAMP_PERCENT));
            }
            else {
                sample = (short) ( mToneSamples[ i] * TWO_TO_THE_POWER_OF_FIFTEEN);
            }

            mToneBytes[ j++] = (byte) ( sample & 0x00FF);
            mToneBytes[ j++] = (byte) ( ( sample & 0xFF00) >>> 8);
        }
    }

    /**
     * Plays the tune in an asynchronous task when executed.
     */
    private class AsyncPlaybackTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mAudioTrack.play();
            return null;
        }

        @Override
        protected void onCancelled() {
            mAudioTrack.stop();
            super.onCancelled();
        }
    }


    /**
     * Generates the tone samples in an asynchronous task when executed.
     */
    private class AsyncGenerateTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            generateTone();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            play();
            super.onPostExecute(aVoid);
        }
    }
}
