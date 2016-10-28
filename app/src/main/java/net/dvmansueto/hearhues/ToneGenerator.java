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

    private static final String TAG = "ToneGenerator";

    private static final int SAMPLE_RATE = 9600;
//    private static final double PLAYBACK_SECONDS = 1.6;
//    private static final int PLAYBACK_SAMPLES = (int) ( SAMPLE_RATE * PLAYBACK_SECONDS);
//    private static final double LEAD_IN_SECONDS = 0.35;
//    private static final int LEAD_IN_SAMPLES = (int) ( SAMPLE_RATE * LEAD_IN_SECONDS);
//    private static final double LEAD_OUT_SECONDS = 0.15;
//    private static final int LEAD_OUT_SAMPLES = (int) ( SAMPLE_RATE * LEAD_OUT_SECONDS);
    private static final int MAGIC_INT = 32767; // 2^15 for 16-bit WAV
//    private static final double DEFAULT_FREQUENCY = 440;
//    private static final double DEFAULT_AMPLITUDE = 0.75;
    private double mTone = 440; // Hertz
    private double mAmplitude = 0.85; // [0...1]
    private double mPlaybackSeconds = 1.6;
    private int mPlaybackSamples = (int) ( SAMPLE_RATE * mPlaybackSeconds);
    private int mLeadInSamples = (int) ( 0.2 * mPlaybackSamples);
    private double[] mSamples = new double[ mPlaybackSamples];
    private byte[] mSoundBytes = new byte[ 2 * mPlaybackSamples];
    private AudioTrack mAudioTrack;
    private boolean mPlaying;
    private ToneGeneratorListener mListener;

    /**
     *
     */
    interface ToneGeneratorListener {
        void startedPlaying();
        void stoppedPlaying();
    }

    // Assign the listener implementing events interface that will receive the events
    void setToneGeneratorListener( ToneGeneratorListener listener) {
        mListener = listener;
    }

    ToneGenerator() {
        mListener = null;
    }

    void setTone( double tone) {
        mTone = tone;
    }

    void setPlaybackSeconds( double seconds) {
        mPlaybackSeconds = seconds;
        mPlaybackSamples = (int) ( seconds * SAMPLE_RATE);
        mLeadInSamples = (int) ( 0.2 * mPlaybackSamples);
    }

    void setAmplitude( double amplitude) {
        mAmplitude = amplitude;
    }

    double getAmplitude() {
        return mAmplitude;
    }

    void setFrequency( double frequency) {

    }

    void stopTone() {
        if ( mPlaying) {
            mAudioTrack.stop();
            mListener.stoppedPlaying();
            mPlaying = false;
//            leadOut();
        }
    }

    void startTone() {

        if ( mPlaying) {
            stopTone();
        }

        for ( int i = 0; i < mSamples.length; ++i) {
            mSamples[ i] = Math.sin( ( 2 * Math.PI - 0.001) * i / ( SAMPLE_RATE / mTone));
        }

        int idx = 0;

//        // lead in gently to avoid audio artifacts (cracks)
//        for ( int i = 0; i < LEAD_IN_SAMPLES; i++) {
//            final short val = (short) ( ( mSamples[ i] * MAGIC_INT) * i / LEAD_IN_SAMPLES);
//            // 16-bit WAV PCM is little endian, so reverse byte order
//            mSoundBytes[ idx++] = (byte) ( val & 0x00FF); // bit-mask to get low byte first
//            mSoundBytes[ idx++] = (byte) ( ( val & 0xFF00) >>> 8 ); // bit-mask and shift for high byte
//        }

        // generate main playback segment
//        for (int i = LEAD_IN_SAMPLES; i < PLAYBACK_SAMPLES - LEAD_OUT_SAMPLES; i++) {
        for (int i = mLeadInSamples; i < mPlaybackSamples; i++) {
            final short val = (short) ( mSamples[i] * MAGIC_INT);
            mSoundBytes[ idx++] = (byte) ( val & 0x00FF);
            mSoundBytes[ idx++] = (byte) ( ( val & 0xFF00) >>> 8 );
        }

//        // lead out gently
//        for ( int i = PLAYBACK_SAMPLES - LEAD_OUT_SAMPLES; i < PLAYBACK_SAMPLES; i++) {
//            final short val = (short) ( (mSamples[i] * MAGIC_INT) * ( LEAD_OUT_SAMPLES - i) / LEAD_OUT_SAMPLES);
//            mSoundBytes[ idx++] = (byte) ( val & 0x00FF); // bit-mask to get low byte first
//            mSoundBytes[ idx++] = (byte) ( ( val & 0xFF00) >>> 8 ); // bit-mask and shift for high byte
//        }

        generateTone();
    }
//
//    private void leadIn() {
//        int samples = (int) ( SAMPLE_RATE * LEAD_IN_SECONDS);
//        mSoundBytes = new byte[ 2 * samples];
//        int idx = 0;
//
//        for ( int i = 0; i < samples; i++) {
//            final short val = (short) ( ( mSamples[ i] * MAGIC_INT) * i / samples);
//            // 16-bit WAV PCM is little endian, so reverse byte order
//            mSoundBytes[ idx++] = (byte) ( val & 0x00FF); // bit-mask to get low byte first
//            mSoundBytes[ idx++] = (byte) ( ( val & 0xFF00) >>> 8 ); // bit-mask and shift for high byte
//        }
//
//        generateTone();
//    }
//
//    private void loopTone() {
//        int samples = SAMPLE_RATE;
//        mSoundBytes = new byte[ 2 * SAMPLE_RATE];
//        int idx = 0;
//
//        // sample 1 seconds worth of tone
//        for ( int i = 0; i < samples; i++) {
//            final short val = (short) ( mSamples[i] * MAGIC_INT);
//            mSoundBytes[ idx++] = (byte) ( val & 0x00FF);
//            mSoundBytes[ idx++] = (byte) ( ( val & 0xFF00) >>> 8 );
//        }
//        generateTone();
//    }
//
//    private void leadOut() {
//        int samples = (int) ( SAMPLE_RATE * LEAD_OUT_SECONDS);
//        mSoundBytes = new byte[ 2 * samples];
//        int idx = 0;
//
//        for ( int i = 0; i < samples; i++) {
//            // scale to minimum amplitude
//            final short val = (short) ( (mSamples[i] * MAGIC_INT) * ( samples - i) / samples);
//            // 16-bit WAV PCM is little endian, so reverse byte order
//            mSoundBytes[ idx++] = (byte) ( val & 0x00FF); // bit-mask to get low byte first
//            mSoundBytes[ idx++] = (byte) ( ( val & 0xFF00) >>> 8 ); // bit-mask and shift for high byte
//        }
//        generateTone();
//    }

    /**
     * Generates audio.
     * Source: http://stackoverflow.com/a/3731075
     */
    private void generateTone() {
        // create anew each time to set buffer size
        // since setter & getter methods require API v24 & v23 respectively.
        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mSoundBytes.length,
                AudioTrack.MODE_STATIC);
        mAudioTrack.write(mSoundBytes, 0, mSoundBytes.length);
        mAudioTrack.setNotificationMarkerPosition(mSoundBytes.length / 2);
        mAudioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onPeriodicNotification(AudioTrack track) {
            }

            @Override
            public void onMarkerReached(AudioTrack track) {
                Log.d(TAG, "End of audioTrack");
                mPlaying = false;
                if (mListener != null)
                    mListener.stoppedPlaying();
            }
        });
        mAudioTrack.play();
        mListener.startedPlaying();
        mPlaying = true;
    }

}
