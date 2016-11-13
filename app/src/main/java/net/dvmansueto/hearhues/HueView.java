package net.dvmansueto.hearhues;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.View;

/**
 * A touchable HSL view with hue callbacks.
 */
public class HueView extends View {

    private static final int HUE_MAX = 360;
    private static final int LUM_MAX = 100;

    /** Constant maximum saturation */
    private static final float SAT = 1;

    private float mDp;

    private Paint[][] mHlPaint;

    private int mWidth;
    private int mHeight;

    /**
     * Constructs a new HueView.
     *
     * @param context the context in which LocView will be displayed.
     * @param attributes the attributes...?
     */
    public HueView(Context context, AttributeSet attributes) {
        super(context, attributes);

        // to convert to display-independent pixels (dip/dp)
        mDp = getResources().getDisplayMetrics().density;

        mHlPaint = new Paint[ HUE_MAX][ LUM_MAX];
        // for each hue in x (horizontal, left to right)
        for ( int x = 0; x < HUE_MAX; x++) {
            // for each lum in y (vertical, top to bottom)
            for ( int y = 0; y < LUM_MAX; y++) {
                mHlPaint[ x][ y] = new Paint();
                // HSL: [0] = hue [0...360), [1] = sat [0...1], [2] = lum [0...1]
                mHlPaint[ x][ y].setColor( ColorUtils.HSLToColor(
                        new float[] { (float) x, SAT, 1 - ((float) y) / LUM_MAX}));
                mHlPaint[ x][ y].setStyle( Paint.Style.FILL);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw( canvas);

        // draw the hue/lum view
        // for each hue in x (horizontal, left to right)
        for ( int x = 0; x < 360; x++) {
            // for each lum in y (vertical, top to bottom)
            for ( int y = 0; y < 100; y++) {
                // left, right, top, bottom, Paint
                canvas.drawRect( x * mDp, y * mDp, x * mDp + mDp, y * mDp + mDp, mHlPaint[ x][ y]);
//                canvas.drawRect( x * mDp, x * mDp + mDp, y * mDp, y * mDp + mDp, mHlPaint[ x][ y]);
            }
        }

    }

//    /**
//     * Recalculates {@link #mWidth}, {@link #mHeight},
//     * @param w the new width
//     * @param h the new height
//     * @param oldw the old width, will be 0 if uninitialised, not used.
//     * @param oldh the old height, will be 0 if uninitialised, not used.
//     */
//    @Override
//    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//        super.onSizeChanged(w, h, oldw, oldh);
//
//        mWidth = getWidth();
//        mHeight = getHeight();
//
////        mIsPortrait = mHeight > mWidth;
//
//
//
////        for ( int i = 0; i < OLD_COORD_COUNT; i++) {
////            mOldCoordRadii[ i] *= mDp;
////        }
////        mNewCoordRadius = NEW_COORD_RADIUS * mDp;
////
////        updateAxes();
//    }

}
