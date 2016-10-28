package net.dvmansueto.hearhues;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by Dave on 28/10/16.
 */

public class LocView extends View {

    private static final String TAG = "LocView";

    /**
     * Constructs a new LocView.
     * @param context the context in which LocView will be displayed.
     */
    public LocView(Context context, AttributeSet attributes) {
        super( context, attributes);

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
    }


    private static final float TICK_WIDTH_PERCENT = 4;
    private static final int FREQ_TICKS = 12;
    private static final int AMP_TICKS = 10;

    private static final float AXIS_STROKE_WIDTH = 2f;
    private static final float TICK_STROKE_WIDTH = 1f;

    private Bitmap mBitmap;

    private Paint mAxisPaint;
    private Paint mTickPaint;

    private float[] mAxes;
    private float[] mTicks;

    private int mWidth;
    private int mHeight;
    private float mDp;

    private double mMaxFreq = 1760; // A6
    private double mMinFreq = 880; // A5
    private double mMaxAmp = 100;
    private double mMinAmp = 0;

    private double mNewLat;
    private double mNewLong;
    private double mOldLat;
    private double mOldLong;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw( canvas);

        canvas.drawLines( mAxes, mAxisPaint);
        canvas.drawLines( mTicks, mTickPaint);

    }

    /**
     * Recalculates {@link #mBitmap}, {@link #mWidth}, {@link #mHeight},
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

        mDp = getResources().getDisplayMetrics().density;

//        mBitmap = Bitmap.createBitmap( w, h, Bitmap.Config.ARGB_8888);

//        // create a 'dp' equivalent
//        float xdpi = getResources().getDisplayMetrics().xdpi;
//        float ydpi = getResources().getDisplayMetrics().ydpi;
//        mDpi = xdpi > ydpi ? xdpi : ydpi;

        updateAxes();
        updateStrokes();
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


    private void updateStrokes() {
        mAxisPaint.setStrokeWidth( AXIS_STROKE_WIDTH * mDp);
        mTickPaint.setStrokeWidth( TICK_STROKE_WIDTH * mDp);
    }

    private void updateAxes() {

        float[] xAxis = { 0, mHeight / 2, mWidth, mHeight / 2};
        float[] yAxis = { mWidth / 2, 0, mWidth / 2, mHeight};

        mAxes = concat( xAxis, yAxis);

        float step = mWidth / FREQ_TICKS;
        float tick = mHeight * TICK_WIDTH_PERCENT / 100;
        float start = ( mHeight - tick) / 2;
        float stop = start + tick;
        float[] xTicks = new float[ 5 * FREQ_TICKS];
        for (int i = 0; i <= FREQ_TICKS; i++ ) {
            xTicks[ i * 4 + 0] = i * step;
            xTicks[ i * 4 + 1] = start;
            xTicks[ i * 4 + 2] = i * step;
            xTicks[ i * 4 + 3] = stop;
        }

        step = mHeight / AMP_TICKS;
        tick = mWidth * TICK_WIDTH_PERCENT / 100;
        start = ( mWidth - tick) / 2;
        stop = start + tick;
        float[] yTicks = new float[ 5 * AMP_TICKS];
        for (int i = 0; i <= AMP_TICKS; i++ ) {
            yTicks[ i * 4 + 0] = start;
            yTicks[ i * 4 + 1] = i * step;
            yTicks[ i * 4 + 2] = stop;
            yTicks[ i * 4 + 3] = i * step;
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

}
