package com.rvoc.cvorapp.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB;

import java.io.File;
import java.io.FileOutputStream;
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
     * @param inputUri The Uri of the input image file.
     * @param watermarkText  The watermark text.
     * @return The watermarked image file.
     * @throws Exception If there are errors in processing the file.
     */
    public File applyWatermarkImage(Uri inputUri,  String watermarkText) throws Exception {
        File outputFile = new File(context.getCacheDir(), "watermarked_image.png");

        try (InputStream inputStream = context.getContentResolver().openInputStream(inputUri)) {
            Bitmap originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream);

            // Create a mutable bitmap to draw the watermark
            Bitmap watermarkedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(watermarkedBitmap);

            // Set up paint for watermark text
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setAlpha(100); // 40% transparency
            paint.setTextSize(36);
            paint.setAntiAlias(true);

            // Calculate text width and height
            float textWidth = paint.measureText(watermarkText);
            float textHeight = paint.getTextSize();

            // Draw watermark text across the image
            float y = textHeight;
            while (y < watermarkedBitmap.getHeight() + textHeight) {
                float x = 0;
                while (x < watermarkedBitmap.getWidth() + textWidth) {
                    canvas.drawText(watermarkText, x, y, paint);
                    x += textWidth;
                }
                y += textHeight;
            }

            // Save the watermarked bitmap to file
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                watermarkedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
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
    public File applyWatermarkPDF(Uri inputUri, String watermarkText) throws Exception {
        File outputFile = new File(context.getCacheDir(), "watermarked_document.pdf");

        PDDocument document = null;

        try (InputStream inputStream = context.getContentResolver().openInputStream(inputUri)) {
            try {
                // Try loading the PDF (throws an exception if encrypted)
                document = PDDocument.load(inputStream);
            } catch (Exception e) {
                Log.w(TAG, "PDF might be encrypted. Attempting decryption...");
                // Attempt to decrypt the PDF
                try (InputStream retryInputStream = context.getContentResolver().openInputStream(inputUri)) {
                    if (retryInputStream != null) {
                        document = pdfHandlingService.decryptPDF(retryInputStream);
                    }
                } catch (Exception decryptionException) {
                    Log.e(TAG, "Failed to decrypt the PDF: " + decryptionException.getMessage(), decryptionException);
                    throw new Exception("The PDF is encrypted and could not be processed.", decryptionException);
                }
            }

            // Apply watermark to each page
            if (document != null) {
                for (PDPage page : document.getPages()) {
                    try (PDPageContentStream contentStream = new PDPageContentStream(
                            document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                        // Set transparency for the watermark
                        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                        graphicsState.setNonStrokingAlphaConstant(0.2f); // 20% opacity
                        contentStream.setGraphicsStateParameters(graphicsState);

                        // Set font and color
                        float fontSize = 40;
                        contentStream.setFont(PDType1Font.HELVETICA, fontSize);
                        contentStream.setNonStrokingColor(new PDColor(new float[]{0.588f, 0.588f, 0.588f}, PDDeviceRGB.INSTANCE)); // Light gray

                        // Calculate page dimensions and watermark metrics
                        float pageWidth = page.getMediaBox().getWidth();
                        float pageHeight = page.getMediaBox().getHeight();
                        float textWidth = PDType1Font.HELVETICA.getStringWidth(watermarkText) / 1000 * fontSize;

                        // Render watermark text diagonally across the page
                        for (float y = 0; y < pageHeight; y += 100) {
                            for (float x = 0; x < pageWidth; x += textWidth + 50) {
                                contentStream.beginText();
                                contentStream.newLineAtOffset(x, y);
                                contentStream.showText(watermarkText);
                                contentStream.endText();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error applying watermark to PDF page: " + e.getMessage(), e);
                        throw new Exception("Failed to process the PDF file.", e);
                    }
                }
            }

            // Save the watermarked PDF
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                if (document != null) {
                    document.save(out);
                }
            }
        } finally {
            if (document != null) {
                document.close();
            }
        }
        return outputFile;
    }
}
