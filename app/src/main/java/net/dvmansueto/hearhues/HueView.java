package net.dvmansueto.hearhues;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A touchable HSL view with hue callbacks.
 */
public class HueView extends View {

    /**
     * The maximum hue for HSL (wraps around beyond 360°)
     * Also defines number of hue steps
     * */
    private static final int HUE_MAX = 360;

    /**
     * The maximum luminance for HSL (wraps around beyond 1)
     * Also defines number of luminance steps
     * */
    private static final int LUM_MAX = 100;

    /** Constant maximum saturation */
    private static final float SAT = 1;

    /** HSL gradient bitmap */
    private Bitmap mHlBitmap;

    /** The display-dependent width of the view, in pixels */
    private int mWidth;

    /** The display-dependent height of the view, in pixels */
    private int mHeight;

    private HueViewListener mHueViewListener;

    public interface HueViewListener {
        void newScalarCoords( double[] scalarCoords);
    }

    public void setHueViewListener( HueViewListener hueViewListener) {
        mHueViewListener = hueViewListener;
    }

    /**
     * Constructs a new HueView.
     *
     * @param context the context in which LocView will be displayed.
     * @param attributes the attributes...?
     */
    public HueView(Context context, AttributeSet attributes) {
        super(context, attributes);

        // generate HSL bitmap once only.
        mHlBitmap = Bitmap.createBitmap( HUE_MAX, LUM_MAX, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas( mHlBitmap);
        for ( int x = 0; x < HUE_MAX; x++) {
            for ( int y = 0; y < LUM_MAX; y++) {
                Paint paint = new Paint();
                paint.setColor( ColorUtils.HSLToColor(
                        new float[] { (float) x, SAT, 1 - ((float) y) / LUM_MAX}));
                paint.setStyle( Paint.Style.FILL);
                canvas.drawPoint( x, y, paint);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw( canvas);

        // stretch the HSL bitmap over the canvas
        canvas.drawBitmap( mHlBitmap, null, mViewRect, null);

    }

    private RectF mViewRect;

    /**
     * @param w the new width
     * @param h the new height
     * @param oldw the old width, will be 0 if uninitialised, not used.
     * @param oldh the old height, will be 0 if uninitialised, not used.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
        mHeight = h;

        mViewRect = new RectF( 0, 0, mWidth, mHeight);
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

//        newAbsoluteCoords( x, y);

        // view is top-down, coords are bottom-up...
        if ( mHueViewListener != null ) mHueViewListener.newScalarCoords(
                new double[] { x / mWidth, ( mHeight - y) / mHeight});
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

}
