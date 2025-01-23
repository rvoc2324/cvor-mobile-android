package com.rvoc.cvorapp.utils;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EdgeDetectionUtils {

    private static final String TAG = "EdgeDetectionUtil";

    /**
     * Perform edge detection and return an edge-detected Bitmap.
     *
     * @param bitmap Input image as a Bitmap.
     * @return Edge-detected Bitmap.
     */
    public static Bitmap detectEdges(Bitmap bitmap) {
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);
        Log.d(TAG, "Edge Detection 1.");

        // Convert to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY);
        Log.d(TAG, "Edge Detection 2.");

        // Apply Gaussian blur to reduce noise
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

        // Perform edge detection
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 75, 200);

        // Convert the edge-detected Mat back to a Bitmap
        Bitmap edgeBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edges, edgeBitmap);
        Log.d(TAG, "Edge Detection 3.");

        // Release resources
        src.release();
        gray.release();
        edges.release();
        Log.d(TAG, "Edge Detection 4.");

        return edgeBitmap;
    }

    /**
     * Perform edge detection and find document boundaries.
     *
     * @param bitmap Input image as a Bitmap.
     * @return List of PointF representing the document's corners if found, null otherwise.
     */
    public static List<PointF> detectDocumentEdges(Bitmap bitmap) {
        Log.d(TAG, "Edge Detection 5.");
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);

        // Convert to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY);
        Log.d(TAG, "Edge Detection 6.");

        // Apply Gaussian blur to reduce noise
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

        // Perform edge detection
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 75, 200);
        Log.d(TAG, "Edge Detection 7.");

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Sort contours by area in descending order
        contours.sort((c1, c2) -> Double.compare(Imgproc.contourArea(c2), Imgproc.contourArea(c1)));
        Log.d(TAG, "Edge Detection 8.");

        for (MatOfPoint contour : contours) {
            // Approximate the contour to a polygon
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            double perimeter = Imgproc.arcLength(contour2f, true);
            Imgproc.approxPolyDP(contour2f, approxCurve, 0.02 * perimeter, true);
            Log.d(TAG, "Edge Detection 9.");

            // Check if the approximated polygon has 4 points (quadrilateral)
            if (approxCurve.total() == 4) {
                List<PointF> points = new ArrayList<>();
                for (Point point : approxCurve.toArray()) {
                    points.add(new PointF((float) point.x, (float) point.y));
                }

                src.release();
                gray.release();
                edges.release();
                hierarchy.release();
                Log.d(TAG, "Edge Detection 10.");
                return points;
            }
        }
        Log.d(TAG, "Edge Detection 11.");
        // Release resources
        src.release();
        gray.release();
        edges.release();
        hierarchy.release();

        return null;
    }
}
