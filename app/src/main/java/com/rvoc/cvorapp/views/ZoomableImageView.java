package com.rvoc.cvorapp.views;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

public class ZoomableImageView extends AppCompatImageView {
    private float scaleFactor = 1.0f; // Current zoom level
    private final float minScale = 1.0f; // Minimum zoom (fit to screen)
    private final float maxScale = 5.0f; // Maximum zoom level
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private final Matrix matrix = new Matrix();
    private boolean isMatrixInitialized = false;
    private final float[] matrixValues = new float[9];
    private final PointF lastTouch = new PointF(); // Stores last touch point

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (scaleFactor > minScale) {
                    resetZoom();
                } else {
                    zoomToPoint(e.getX(), e.getY());
                }
                return true;
            }
        });
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!isMatrixInitialized) {
            applyFitCenter(); // Apply fit-to-screen initially
            isMatrixInitialized = true;
        }
    }

    /**
     * Ensures the image fits the screen similar to ImageView's fitCenter mode.
     */
    private void applyFitCenter() {
        Drawable drawable = getDrawable();
        if (drawable == null) return;

        int dWidth = drawable.getIntrinsicWidth();
        int dHeight = drawable.getIntrinsicHeight();
        int vWidth = getWidth();
        int vHeight = getHeight();

        float scale = Math.min((float) vWidth / dWidth, (float) vHeight / dHeight);
        float dx = (vWidth - dWidth * scale) / 2f;
        float dy = (vHeight - dHeight * scale) / 2f;

        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);

        scaleFactor = 1.0f; // Reset zoom level
    }

    /**
     * Reset zoom back to fitCenter.
     */
    private void resetZoom() {
        scaleFactor = minScale;
        applyFitCenter();
    }

    /**
     * Zoom into the tapped position smoothly.
     */
    private void zoomToPoint(float x, float y) {
        float newScale = 2.5f; // Target zoom level
        if (scaleFactor > minScale) {
            resetZoom();
            return;
        }

        // Get current matrix values
        matrix.getValues(matrixValues);
        float currentScale = matrixValues[Matrix.MSCALE_X];

        // Compute scale factor adjustment
        float factor = newScale / currentScale;
        scaleFactor = newScale;

        // Apply zoom at touch point
        matrix.postScale(factor, factor, x, y);
        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);

        // Track last touch position for panning
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            lastTouch.set(event.getX(), event.getY());
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick(); // Ensure accessibility events are triggered
        }

        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactorDelta = detector.getScaleFactor();
            float newScale = scaleFactor * scaleFactorDelta;

            // Limit zoom levels
            if (newScale < minScale) {
                scaleFactor = minScale;
            } else scaleFactor = Math.min(newScale, maxScale);

            // Apply scaling around pinch center
            matrix.postScale(scaleFactorDelta, scaleFactorDelta, detector.getFocusX(), detector.getFocusY());
            setImageMatrix(matrix);
            return true;
        }
    }
}
