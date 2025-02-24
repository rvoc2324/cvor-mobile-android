package com.rvoc.cvorapp.views;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;
import android.view.animation.DecelerateInterpolator;

public class RotatingSpan extends ReplacementSpan {
    private float rotation = 0f;
    private final int color;

    public RotatingSpan(int color, Runnable onAnimationEnd) {
        this.color = color;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(1000); // 1 second spin
        animator.setInterpolator(new DecelerateInterpolator()); // Smooth slow-down
        animator.addUpdateListener(animation -> rotation = (float) animation.getAnimatedValue());

        // Stop animation and trigger the callback when done
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onAnimationEnd != null) {
                    onAnimationEnd.run();
                }
            }
        });

        animator.start();
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return (int) paint.measureText(text, start, end);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        canvas.save();
        float centerX = x + (paint.measureText(text, start, end) / 2);
        float centerY = y - (paint.descent() + paint.ascent()) / 2;

        // Apply rotation
        canvas.rotate(rotation, centerX, centerY);

        // Change color
        int oldColor = paint.getColor();
        paint.setColor(color);
        canvas.drawText(text, start, end, x, y, paint);
        paint.setColor(oldColor); // Restore original color

        canvas.restore();
    }
}
