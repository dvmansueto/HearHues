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
    private static final double LEAD_IN_PERCENT = 0.2;
//    private static final double PLAYBACK_SECONDS = 1.6;
//    private static final int PLAYBACK_SAMPLES = (int) ( SAMPLE_RATE * PLAYBACK_SECONDS);
//    private static final double LEAD_IN_SECONDS = 0.35;
//    private static final int LEAD_IN_SAMPLES = (int) ( SAMPLE_RATE * LEAD_IN_SECONDS);
//    private static final double LEAD_OUT_SECONDS = 0.15;
//    private static final int LEAD_OUT_SAMPLES = (int) ( SAMPLE_RATE * LEAD_OUT_SECONDS);
    private static final int MAGIC_INT = 32767; // 2^15 for 16-bit WAV
//    private static final double DEFAULT_FREQUENCY = 440;
//    private static final double DEFAULT_AMPLITUDE = 0.75;
    private double mTone; // Hertz
    private double mAmplitude; // [0...1]
    private double mPlaybackSeconds;
    private int mPlaybackSamples;
    private int mLeadInSamples;
    private double[] mSamples;
    private byte[] mSoundBytes;
    private AudioTrack mAudioTrack;
    private boolean mPlaying;
    private boolean mPlayContinuously;
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
        // arbitrary defaults:
        mTone = 440.0;
        mAmplitude = 1.0;
        mPlaybackSeconds = 1.6;
        updateAudioTrack();
        mPlaying = false;
        mPlayContinuously = false;
    }

    void setTone( double tone) {
        mTone = tone;
    }

    void setPlaybackSeconds( double seconds) {
        mPlaybackSeconds = seconds;
        updateAudioTrack();
    }

    void updateAudioTrack() {
        mPlaybackSamples = (int) ( mPlaybackSeconds * SAMPLE_RATE);
        mLeadInSamples = (int) ( LEAD_IN_PERCENT * mPlaybackSamples);
        mSamples = new double[ mPlaybackSamples];
        mSoundBytes = new byte[ 2 * mPlaybackSamples];
        // create anew each time to set buffer size
        // since setter & getter methods require API v24 & v23 respectively.
        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mSoundBytes.length,
                AudioTrack.MODE_STREAM);
        mAudioTrack.setVolume( (float) mAmplitude);
        mAudioTrack.setNotificationMarkerPosition( mSoundBytes.length / 2);
        mAudioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onPeriodicNotification(AudioTrack track) {
            }

            @Override
            public void onMarkerReached(AudioTrack track) {
                Log.d(TAG, "End of audioTrack");
                mPlaying = false;
                if ( mListener != null) mListener.stoppedPlaying();
                if ( mPlayContinuously) startTone();
            }
        });
    }

    void setAmplitude( double amplitude) {
        mAmplitude = amplitude;
        mAudioTrack.setVolume( (float) amplitude);
    }

    double getAmplitude() {
        return mAmplitude;
    }

    void setFrequency( double frequency) {
        mTone = frequency;
    }

    void playContinuously( boolean state) {
        mPlayContinuously = state;
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
        for (int i = mLeadInSamples; i < mPlaybackSamples; i++) {
            final short val = (short) ( mSamples[i] * MAGIC_INT);
            mSoundBytes[ idx++] = (byte) ( val & 0x00FF);
            mSoundBytes[ idx++] = (byte) ( ( val & 0xFF00) >>> 8 );
        }

        generateTone( mSoundBytes);
    }

    /**
     * Generates audio.
     * Source: http://stackoverflow.com/a/3731075
     */
    private void generateTone( byte[] soundBytes) {
        mPlaying = true;
//        // create anew each time to set buffer size
//        // since setter & getter methods require API v24 & v23 respectively.
//        mAudioTrack = new AudioTrack(
//                AudioManager.STREAM_MUSIC,
//                SAMPLE_RATE,
//                AudioFormat.CHANNEL_OUT_MONO,
//                AudioFormat.ENCODING_PCM_16BIT,
//                mSoundBytes.length,
//                AudioTrack.MODE_STATIC);
        mAudioTrack.write( soundBytes, 0, soundBytes.length);
//        mAudioTrack.setNotificationMarkerPosition( soundBytes.length / 2);
//        mAudioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
//            @Override
//            public void onPeriodicNotification(AudioTrack track) {
//            }
//
//            @Override
//            public void onMarkerReached(AudioTrack track) {
//                Log.d(TAG, "End of audioTrack");
//                mPlaying = false;
//                if ( mListener != null) mListener.stoppedPlaying();
//                if ( mPlayContinuously) startTone();
//            }
//        });
        mAudioTrack.play();
        mListener.startedPlaying();
    }

    void flushTone() {
        mAudioTrack.flush();
    }
}
