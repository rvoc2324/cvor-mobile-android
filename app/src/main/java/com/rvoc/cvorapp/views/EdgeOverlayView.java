package com.rvoc.cvorapp.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class EdgeOverlayView extends View {

    private final Paint paint;
    private final Path path;

    public EdgeOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // Set up the paint for the box
        paint = new Paint();
        paint.setColor(Color.BLUE); // Box color
        paint.setStyle(Paint.Style.STROKE); // Stroke only
        paint.setStrokeWidth(8f); // Border thickness
        paint.setAntiAlias(true);

        path = new Path();
    }

    /**
     * Updates the detected edges and redraws the overlay.
     *
     * @param edgePoints The points representing the detected edges.
     */
    public void updateEdges(List<PointF> edgePoints) {
        path.reset();
        if (edgePoints != null && edgePoints.size() == 4) {
            path.moveTo(edgePoints.get(0).x, edgePoints.get(0).y); // Top-left
            path.lineTo(edgePoints.get(1).x, edgePoints.get(1).y); // Top-right
            path.lineTo(edgePoints.get(2).x, edgePoints.get(2).y); // Bottom-right
            path.lineTo(edgePoints.get(3).x, edgePoints.get(3).y); // Bottom-left
            path.close();
        }
        invalidate(); // Trigger a redraw
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }
}
