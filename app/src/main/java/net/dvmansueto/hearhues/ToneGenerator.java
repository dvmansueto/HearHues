package net.dvmansueto.hearhues;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Created by Dave on 13/10/16.
 *
 * using http://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
 *  originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
 *  and modified by Steve Pomeroy <steve@staticfree.info>
 *  plus https://gist.github.com/SuspendedPhan/7596139
 */

public class ToneGenerator {
    private static final int SAMPLE_RATE = 9600;
    private static final double LEAD_IN_MILLISECONDS = 0.35;
    private static final double LEAD_OUT_MILLISECONDS = 0.15;
    private static final int MAXIMUM_LOOP_SECONDS = 3;
    private static final int MAGIC_INT = 32767; // 2^15 for 16-bit WAV
    private static final double DEFAULT_FREQUENCY = 440;
    private static final double DEFAULT_AMPLITUDE = 0.85;
    private static final boolean DEFAULT_MUTED = true;
    private double mTone; // Hertz
    private double mAmplitude; // [0...1]
    private boolean mMuted;
    private double[] mSamples;
    private int mSampleIdx;
    private byte[] mSoundBytes;


    public ToneGenerator() {
        mTone = DEFAULT_FREQUENCY;
        mAmplitude = DEFAULT_AMPLITUDE;
        mMuted = DEFAULT_MUTED;
    }

    public ToneGenerator( float tone) {
        mTone = tone;
        mAmplitude = DEFAULT_AMPLITUDE;
        mMuted = DEFAULT_MUTED;
    }

    public ToneGenerator( float tone, boolean muted) {
        mTone = tone;
        mAmplitude = DEFAULT_AMPLITUDE;
        mMuted = muted;
    }


    public void setTone( float tone) {
        mTone = tone;
        startTone();
    }


    public void setAmplitude( double amplitude) {
        mAmplitude = amplitude;
    }

    public void setMuted( boolean muted) {
        mMuted = muted;
    }


    public double getAmplitude() {
        return mAmplitude;
    }

    public boolean getMuted() {
        return mMuted;
    }

    public void startTone() {

        // lead out of the last tone gracefully
        leadOut();

        for ( int i = 0; i < SAMPLE_RATE; ++i) {
            mSamples[ i] = Math.sin( ( 2 * Math.PI - 0.001) * i / ( SAMPLE_RATE / mTone));
        }

        // lead in to new tone
        leadIn();

        // play back tone for MAXIMUM_SECONDS
        loopTone();
    }

    private void leadIn() {
        int samples = (int) ( SAMPLE_RATE * LEAD_IN_MILLISECONDS);
        mSoundBytes = new byte[ 2 * samples];
        int idx = 0;

        for ( int i = 0; i < samples; i++) {
            final short val = (short) ( ( mSamples[i] * MAGIC_INT) * i / samples);
            // 16-bit WAV PCM is little endian, so reverse byte order
            mSoundBytes[ idx++] = (byte) ( val & 0x00FF); // bit-mask to get low byte first
            mSoundBytes[ idx++] = (byte) ( ( val & 0xFF00) >>> 8 ); // bit-mask and shift for high byte
        }

        generateTone();
    }

    private void loopTone() {
        mSoundBytes = new byte[ 2 * SAMPLE_RATE];
        int idx = 0;

        for ( int i = 0; i < SAMPLE_RATE; i++) {
            final short val = (short) ( mSamples[i] * MAGIC_INT);
            mSoundBytes[ idx++] = (byte) ( val & 0x00FF);
            mSoundBytes[ idx++] = (byte) ( ( val & 0xFF00) >>> 8 );
        }

        for ( int i = 0; i < MAXIMUM_LOOP_SECONDS; i++) {
            generateTone();
        }
    }


    private void leadOut() {
        int samples = (int) ( SAMPLE_RATE * LEAD_OUT_MILLISECONDS);
        mSoundBytes = new byte[ 2 * samples];
        int idx = 0;

        for ( int i = 0; i < samples; i++) {
            // scale to minimum amplitude
            final short val = (short) ( (mSamples[i] * MAGIC_INT) * ( samples - i) / samples);
            // 16-bit WAV PCM is little endian, so reverse byte order
            mSoundBytes[ idx++] = (byte) ( val & 0x00FF); // bit-mask to get low byte first
            mSoundBytes[ idx++] = (byte) ( ( val & 0xFF00) >>> 8 ); // bit-mask and shift for high byte
        }
    }

    /**
     * Generates audio.
     * Source: http://stackoverflow.com/a/3731075
     */
    private void generateTone() {
        final AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mSoundBytes.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(mSoundBytes, 0, mSoundBytes.length);
        audioTrack.play();
    }
}
