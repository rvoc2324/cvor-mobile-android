package com.rvoc.cvorapp.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;

import com.rvoc.cvorapp.utils.FileUtils;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import com.tom_roush.pdfbox.util.Matrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Service for applying watermark text to PDF and image files.
 */
@Singleton
public class WatermarkService {

    private final Context context;

    @Inject
    public WatermarkService(@ApplicationContext Context context) {
        this.context = context;
    }

    @Inject
    PdfHandlingService pdfHandlingService;
    private static final String TAG = "WatermarkService";

    /**
     * Applies a watermark to an image file (JPEG/PNG).
     *
     * @param inputUri      The Uri of the input image file.
     * @param watermarkText The watermark text.
     * @return The watermarked image file.
     * @throws Exception If there are errors in processing the file.
     */
    public File applyWatermarkImage(Uri inputUri, String watermarkText, Integer opacity, Integer fontSize, Boolean repeat) throws Exception {
        String originalFileName = FileUtils.getFileNameFromUri(context, inputUri);
        String fileName = "CVOR_" + originalFileName;
        File outputFile = new File(context.getCacheDir(), fileName);
        Log.d(TAG, "Watermark service started.");

        // Default Integer parameters are null
        opacity = (opacity != null) ? opacity : 40; // Default alpha to 40
        fontSize = (fontSize != null) ? fontSize : 18; // Default text size to 18
        boolean repeatWatermark = (repeat != null) ? repeat : true; // Default to repeat

        int maxWidth = 1024; // Target width
        int maxHeight = 1024; // Target height

        try (InputStream inputStream = context.getContentResolver().openInputStream(inputUri)) {
            Bitmap originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream);

            int originalWidth = originalBitmap.getWidth();
            int originalHeight = originalBitmap.getHeight();

            float scale = Math.min((float) maxWidth / originalWidth, (float) maxHeight / originalHeight);

            if (scale < 1) { // Resize only if the image is large
                int newWidth = Math.round(originalWidth * scale);
                int newHeight = Math.round(originalHeight * scale);
                originalBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
            }

            // Create a mutable bitmap to draw the watermark
            Bitmap watermarkedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(watermarkedBitmap);

            // int watermarkColour = getWatermarkColour(watermarkedBitmap);

            // Set up paint for watermark text
            Paint paint = new Paint();
            // paint.setColor(watermarkColour);
            paint.setColor(Color.GRAY);
            paint.setAlpha((opacity * 255) / 100); // Opacity from 0 to 255
            paint.setTextSize(fontSize);
            paint.setAntiAlias(true);

            // Calculate text width and height
            float textWidth = paint.measureText(watermarkText);
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            float textHeight = fontMetrics.descent - fontMetrics.ascent;
            Log.d(TAG, "Watermark service 2.");

            if (repeatWatermark) {

                float horizontalSpacing = textWidth * 0.2f; // Adjust horizontal gap
                float verticalSpacing = textHeight * 0.4f; // Adjust vertical gap

                // Draw watermark text repeatedly across the image
                float y = textHeight;
                boolean shiftRow = false; // To alternate row shifting
                while (y < watermarkedBitmap.getHeight() + textHeight) {
                    float x = shiftRow ? ((textWidth + horizontalSpacing) / 2) : 0;
                    while (x < watermarkedBitmap.getWidth() + textWidth) {
                        canvas.drawText(watermarkText, x, y, paint);
                        x += textWidth + horizontalSpacing; // Add spacing between watermarks
                    }
                    y += textHeight + verticalSpacing; // Add spacing between rows
                    shiftRow = !shiftRow;
                }
            } else {
                // Get center of the image
                float centerX = watermarkedBitmap.getWidth() / 2f;
                float centerY = watermarkedBitmap.getHeight() / 2f;
                float angle = 315f; // Rotation angle in degrees

                canvas.save(); // Save current state of the canvas

                // Rotate around the center of the image
                canvas.rotate(angle, centerX, centerY);

                // Draw the watermark text at the rotated position
                canvas.drawText(watermarkText, centerX - (textWidth / 2), centerY + (textHeight / 2), paint);

                canvas.restore(); // Restore canvas to avoid affecting other drawings
            }


            // **Add static watermark at the bottom of the image**
            Paint staticPaint = new Paint();
            staticPaint.setColor(Color.DKGRAY);
            staticPaint.setAlpha(180); // Slightly darker opacity
            staticPaint.setTextSize(20); // Fixed font size
            staticPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            staticPaint.setAntiAlias(true);

            String staticWatermark = "Smart share by CVOR";
            float staticTextWidth = staticPaint.measureText(staticWatermark);
            float staticX = (watermarkedBitmap.getWidth() - staticTextWidth) / 2;
            float staticY = watermarkedBitmap.getHeight() - 30; // Margin from the bottom

            canvas.drawText(staticWatermark, staticX, staticY, staticPaint);

            // Save the watermarked bitmap to file
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
                Log.d(TAG, "Watermark service completed successfully.");
                Log.d(TAG, "Watermark service 3.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying watermark to image: " + e.getMessage(), e);
            throw new Exception("Failed to process the image file.", e);
        }
        return outputFile;
    }

    /**
     * Applies a watermark to a PDF file.
     *
     * @param inputUri The Uri of the input PDF file.
     * @return The watermarked PDF file.
     * @throws Exception If there are errors in processing the file.
     */
    public File applyWatermarkPDF(Uri inputUri, String watermarkText, Integer opacity, Integer fontSize, Boolean repeat) throws Exception {
        String originalFileName = FileUtils.getFileNameFromUri(context, inputUri);
        String fileName = "CVOR_" + opacity + "_" + originalFileName;
        File outputFile = new File(context.getCacheDir(), fileName);
        Log.d(TAG, "Watermark service 4.");

        // Set defaults for nullable parameters
        opacity = (opacity != null) ? opacity : 100; // Default opacity to 40
        fontSize = (fontSize != null) ? fontSize : 18; // Default font size to 18
        boolean repeatWatermark = (repeat != null) ? repeat : true; // Default to repeating watermark
        Log.d(TAG, "Watermark service 5.");

        float alphaValue = opacity / 100.0f; // Convert opacity to range 0.0-1.0

        try (InputStream inputStream = context.getContentResolver().openInputStream(inputUri);
             PDDocument document = PDDocument.load(inputStream)) {

            PDType1Font font = PDType1Font.HELVETICA;
            PDType1Font staticFont = PDType1Font.HELVETICA_BOLD;
            Log.d(TAG, "Watermark service 6.");

            for (PDPage page : document.getPages()) {
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float textWidth = font.getStringWidth(watermarkText) / 1000 * fontSize;
                float staticTextWidth = staticFont.getStringWidth("Smart share by CVOR") / 1000 * 12; // Fixed size

                try (PDPageContentStream contentStream = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    // Set transparency
                    PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                    graphicsState.setNonStrokingAlphaConstant(alphaValue);
                    page.getResources().put(COSName.getPDFName("TransparentState"), graphicsState);
                    contentStream.setGraphicsStateParameters(graphicsState);

                    // Set font and color
                    contentStream.setFont(font, fontSize);
                    contentStream.setNonStrokingColor(0.7f);

                    if (repeatWatermark) {
                        // Render watermark repeatedly across the page
                        float ySpacing = pageHeight / 10; // Adjust vertical spacing
                        float xSpacing = textWidth + 20; // Adjust horizontal spacing
                        boolean shiftRow = false; // Toggle shifting every row

                        for (float y = 0; y < pageHeight; y += ySpacing) {
                            float xStart = shiftRow ? xSpacing / 2 : 0; // Shift alternate rows

                            for (float x = xStart; x < pageWidth; x += xSpacing) {

                                contentStream.beginText();
                                contentStream.newLineAtOffset(x, y);
                                contentStream.showText(watermarkText);
                                contentStream.endText();
                            }
                            shiftRow = !shiftRow;
                        }
                    } else {
                        // Render watermark in the center of the page with rotation
                        float centerX = pageWidth / 2;
                        float centerY = pageHeight / 2;
                        float angle = (float) Math.toRadians(45); // Rotate 45 degrees

                        contentStream.saveGraphicsState(); // Save current graphics state

                        // Apply rotation transformation
                        contentStream.transform(Matrix.getTranslateInstance(centerX, centerY));
                        contentStream.transform(Matrix.getRotateInstance(angle, 0, 0));

                        contentStream.beginText();
                        contentStream.newLineAtOffset(-textWidth / 2, 0);
                        contentStream.showText(watermarkText);
                        contentStream.endText();

                        contentStream.restoreGraphicsState(); // Restore previous state
                    }

                    // **Add static watermark at the bottom of the page**
                    contentStream.setFont(staticFont, 12); // Set smaller font size for static watermark
                    contentStream.setNonStrokingColor(0.5f); // Slightly lighter color

                    float staticX = (pageWidth - staticTextWidth) / 2;
                    float staticY = 20; // Fixed margin from bottom
                    contentStream.beginText();
                    contentStream.newLineAtOffset(staticX, staticY);
                    contentStream.showText("Smart share by CVOR");
                    contentStream.endText();
                }
            }

            // Save the watermarked PDF
            document.save(outputFile);
            Log.d(TAG, "Watermark service completed.");
        } catch (Exception e) {
            Log.e(TAG, "Error applying watermark to PDF", e);
            throw new IOException("Failed to apply watermark to PDF.", e);
        }

        return outputFile;
    }

    private int getWatermarkColour(Bitmap bitmap) {
        long totalBrightness = 0;
        int avgBrightness;
        int watermarkColor;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int totalPixels = width * height;

        // Sample every 10th pixel to speed up processing
        for (int y = 0; y < height; y += 10) {
            for (int x = 0; x < width; x += 10) {
                int pixel = bitmap.getPixel(x, y);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                int brightness = (red + green + blue) / 3; // Average brightness per pixel
                totalBrightness += brightness;
            }
        }
        avgBrightness = (int) (totalBrightness / (totalPixels / 100)); // Get the average brightness

        if (avgBrightness > 180) {  // Very bright image
            watermarkColor = Color.rgb(68, 68, 68); // Dark gray (#444444)
        } else if (avgBrightness > 100) {  // Medium brightness (multi-color)
            watermarkColor = Color.rgb(136, 136, 136); // Neutral gray (#888888)
        } else {  // Dark image
            watermarkColor = Color.rgb(221, 221, 221); // Light gray (#DDDDDD)
        }

        return watermarkColor;
    }

}