package net.dvmansueto.hearhues;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Dave on 28/10/16.
 */

public class LocView extends View {

    private static final String TAG = "LocView";

    private static final float TICK_WIDTH_PERCENT = 4;
    private static final int FREQ_TICKS = 12;
    private static final int AMP_TICKS = 10;

    private static final float AXIS_STROKE_WIDTH = 2f;
    private static final float TICK_STROKE_WIDTH = 1f;

    private static final float NEW_COORD_RADIUS = 4f;
    private static final float OLD_COORD_RADIUS = 2f;

    private boolean mIsPortrait;

    private Paint mAxisPaint;
    private Paint mTickPaint;

    private Paint mNewCoordPaint;
    private Paint mOldCoordPaint;
    private float mNewCoordRadius;
    private float mOldCoordRadius;

    private float[] mAxes;
    private float[] mTicks;

    private int mWidth;
    private int mHeight;
    private float mDp;

    private double mMaxFreq = 1760; // A6
    private double mMinFreq = 880; // A5
    private double mMaxAmp = 100;
    private double mMinAmp = 0;

    private double mNewLat = 0;
    private double mNewLong = 0;

    private double mOldLat = 0;
    private double mOldLong = 0;

    private LocViewListener mLocViewListener;

    /**
     * Constructs a new LocView.
     * @param context the context in which LocView will be displayed.
     */
    public LocView(Context context, AttributeSet attributes) {
        super( context, attributes);

        // is this necessary?
        mLocViewListener = null;

        mAxisPaint = new Paint();
        mAxisPaint.setAntiAlias( false); // ordinate lines!
        mAxisPaint.setColor( Color.BLACK);
        mAxisPaint.setStyle( Paint.Style.STROKE);
        mAxisPaint.setStrokeWidth( AXIS_STROKE_WIDTH);

        mTickPaint = new Paint();
        mTickPaint.setAntiAlias( false); // ordinate lines!
        mTickPaint.setColor( Color.BLACK);
        mTickPaint.setStyle( Paint.Style.STROKE);
        mTickPaint.setStrokeWidth( TICK_STROKE_WIDTH);

        mNewCoordPaint = new Paint();
        mNewCoordPaint.setAntiAlias( true);
        mNewCoordPaint.setColor( Color.RED);
        mNewCoordPaint.setStyle( Paint.Style.FILL);

        mOldCoordPaint = new Paint();
        mOldCoordPaint.setAntiAlias( true);
        mOldCoordPaint.setColor( Color.GRAY);
        mOldCoordPaint.setStyle( Paint.Style.FILL);
    }

    /**
     * Draws the axes and coords on the canvas.
     * @param canvas the canvas to draw on.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw( canvas);

        canvas.drawLines( mAxes, mAxisPaint);
        canvas.drawLines( mTicks, mTickPaint);

        canvas.drawCircle( (float) mOldLong, (float) mOldLat, mOldCoordRadius, mOldCoordPaint);
        canvas.drawCircle( (float) mNewLong, (float) mNewLat, mNewCoordRadius, mNewCoordPaint);
    }

    /**
     * Recalculates {@link #mWidth}, {@link #mHeight},
     * calls {@link #updateAxes()} to update {@link #mAxes} & {@link #mTicks}.
     * @param w the new width
     * @param h the new height
     * @param oldw the old width, will be 0 if uninitialised, not used.
     * @param oldh the old height, will be 0 if uninitialised, not used.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // convert into density-independent pixels
//        mWidth = getWidth() / (int) getResources().getDisplayMetrics().density;
//        mHeight = getHeight() / (int) getResources().getDisplayMetrics().density;
        mWidth = getWidth();
        mHeight = getHeight();

        mIsPortrait = mHeight > mWidth;

        mDp = getResources().getDisplayMetrics().density;

//        mBitmap = Bitmap.createBitmap( w, h, Bitmap.Config.ARGB_8888);

//        // create a 'dp' equivalent
//        float xdpi = getResources().getDisplayMetrics().xdpi;
//        float ydpi = getResources().getDisplayMetrics().ydpi;
//        mDpi = xdpi > ydpi ? xdpi : ydpi;


        mOldCoordRadius = OLD_COORD_RADIUS * mDp;
        mNewCoordRadius = NEW_COORD_RADIUS * mDp;

        updateAxes();

//
//        Log.d( TAG, "  width: " + Integer.toString( w) + "  height: " + Integer.toString(h));
//        Log.d( TAG, " mWidth: " + Integer.toString( mWidth) + " mHeight: " + Integer.toString( mHeight));
//        Log.d( TAG, "   mDpi: " + Float.toString( mDpi));
//        Log.d( TAG, "density: " + Float.toString( getResources().getDisplayMetrics().density));
//        Log.d( TAG, "   dDpi: " + Float.toString( getResources().getDisplayMetrics().densityDpi));
//        Log.d( TAG, "scaledD: " + Float.toString( getResources().getDisplayMetrics().scaledDensity));
//        Log.d( TAG, "density: " + Float.toString( getResources().getDisplayMetrics().));
//        Log.d( TAG, "   xdpi: " + Float.toString( getResources().getDisplayMetrics().xdpi));
//        Log.d( TAG, "   ydpi: " + Float.toString( getResources().getDisplayMetrics().ydpi));
    }



    /**
     * Creates the arrays from which to draw the axes and their ticks,
     * also updates their paints' stroke widths.
     */
    //TODO: fix linear error between ticks and axes.
    private void updateAxes() {

        // change the width of the lines
        mAxisPaint.setStrokeWidth( AXIS_STROKE_WIDTH * mDp);
        mTickPaint.setStrokeWidth( TICK_STROKE_WIDTH * mDp);

        // create arrays
        float[] xAxis = { 0, mHeight / 2, mWidth, mHeight / 2};
        float[] yAxis = { mWidth / 2, 0, mWidth / 2, mHeight};

        mAxes = concat( xAxis, yAxis);

        float offset = ( TICK_STROKE_WIDTH * mDp) / 2;
        float tick = mIsPortrait ? mWidth  * TICK_WIDTH_PERCENT / 100
                : mHeight * TICK_WIDTH_PERCENT / 100;

        float step = mWidth / FREQ_TICKS;
        float start = ( mHeight - tick) / 2;
        float stop = start + tick;
        float[] xTicks = new float[ 5 * FREQ_TICKS];
        for (int i = 0; i <= FREQ_TICKS; i++ ) {
            xTicks[ i * 4 + 0] = i * step + offset;
            xTicks[ i * 4 + 1] = start;
            xTicks[ i * 4 + 2] = i * step + offset;
            xTicks[ i * 4 + 3] = stop;
        }

        step = mHeight / AMP_TICKS;
        start = ( mWidth - tick) / 2;
        stop = start + tick;
        float[] yTicks = new float[ 5 * AMP_TICKS];
        for (int i = 0; i <= AMP_TICKS; i++ ) {
            yTicks[ i * 4 + 0] = start;
            yTicks[ i * 4 + 1] = i * step + offset;
            yTicks[ i * 4 + 2] = stop;
            yTicks[ i * 4 + 3] = i * step + offset;
        }

        mTicks = concat( xTicks, yTicks);
    }

    /**
     * Method to concatenate two float arrays without depending on ArrayUtils.
     * @param a the first float[]
     * @param b the second float []
     * @return a float[] concatenated from a then b
     */
    private float[] concat( float[] a, float[] b) {
        // based on http://stackoverflow.com/a/80503
        int aLen = a.length;
        int bLen = b.length;
        float[] c = new float [ aLen + bLen];
        System.arraycopy( a, 0, c, 0, aLen);
        System.arraycopy( b, 0, c, aLen, bLen);
        return c;
    }

    /**
     * Interprets touch, updates old and new coordinates, and throws frequency and amplitude
     * percentages.
     * @param x the horizontal position
     * @param y the vertical position
     */
    private void parseTouch( float x, float y) {
        // impose limits since you can drag out-of-bounds of the view...
        if ( x < 0) x = 0;
        if ( x > mWidth) x = mWidth;
        if ( y < 0) y = 0;
        if ( y > mHeight) y = mHeight;

        mOldLat = mNewLat;
        mOldLong = mNewLong;
        mNewLat = y;
        mNewLong = x;

        mLocViewListener.newFrequency( x / mWidth);
        mLocViewListener.newAmplitude( ( mHeight - y) / mHeight); // since y starts from the top

    }

    /**
     * Switches the motion event, passing touch x & y coordinates to
     * {@link #parseTouch(float, float)}.
     * @param motionEvent the incoming touch event
     * @return true if the x & y coords were passed to parseTouch.
     */
    @Override
    public boolean onTouchEvent( MotionEvent motionEvent) {
        // shamelessly stolen from
        // https://examples.javacodegeeks.com/android/core/graphics/canvas-graphics/android-canvas-example/
        float x = motionEvent.getX();
        float y = motionEvent.getY();

        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                parseTouch(x, y);
                invalidate();
                return true;
        }
        return false;
    }

    public interface LocViewListener {
        void newFrequency( double frequency);
        void newAmplitude( double amplitude);
    }

    public void setLocViewListener( LocViewListener locViewListener) {
        mLocViewListener = locViewListener;
    }
}
