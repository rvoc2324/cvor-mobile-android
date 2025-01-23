package com.rvoc.cvorapp.utils;

import android.graphics.Bitmap;
import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PDFRenderingUtils {

    private static final String TAG = "PDFRendering";
    private final ExecutorService executorService;

    public PDFRenderingUtils() {
        // Initialize a thread pool for rendering tasks
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Renders the entire PDF file into a list of Bitmap objects, one per page.
     *
     * @param pdfFile The PDF file to render.
     * @return A list of Bitmap objects representing each page of the PDF.
     * @throws IOException If there's an error reading the PDF file.
     */
    public List<Bitmap> renderPDF(File pdfFile) throws IOException {
        List<Bitmap> bitmaps = new ArrayList<>();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            for (int i = 0; i < pageCount; i++) {
                Bitmap pageBitmap = renderPage(pdfRenderer, i);
                bitmaps.add(pageBitmap);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error rendering PDF: " + e.getMessage(), e);
            throw e;
        }

        return bitmaps;
    }

    /**
     * Renders a specific page of the PDF as a Bitmap.
     *
     * @param pdfFile The PDF file to render.
     * @param page    The page number to render (zero-based index).
     * @return A Bitmap object representing the specified page.
     * @throws IOException If there's an error reading the PDF file.
     */
    public Bitmap renderPDFPage(File pdfFile, int page) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            return renderPage(pdfRenderer, page);
        } catch (IOException e) {
            Log.e(TAG, "Error rendering PDF page: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Renders a specific page using a PDFRenderer instance.
     *
     * @param pdfRenderer The PDFRenderer instance.
     * @param page        The page number to render.
     * @return A Bitmap object representing the page.
     * @throws IOException If there's an error rendering the page.
     */
    private Bitmap renderPage(PDFRenderer pdfRenderer, int page) throws IOException {
        return pdfRenderer.renderImageWithDPI(page, 300);
    }

    /**
     * Asynchronously renders the entire PDF file and returns the result through a callback.
     *
     * @param pdfFile  The PDF file to render.
     * @param callback A callback to handle the result or errors.
     */
    public void renderPDFAsync(File pdfFile, PDFRenderCallback callback) {
        executorService.execute(() -> {
            try {
                List<Bitmap> bitmaps = renderPDF(pdfFile);
                callback.onSuccess(bitmaps);
            } catch (IOException e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Asynchronously renders a specific page of the PDF and returns the result through a callback.
     *
     * @param pdfFile  The PDF file to render.
     * @param page     The page number to render (zero-based index).
     * @param callback A callback to handle the result or errors.
     */
    public void renderPDFPageAsync(File pdfFile, int page, PDFRenderCallback callback) {
        executorService.execute(() -> {
            try {
                Bitmap bitmap = renderPDFPage(pdfFile, page);
                callback.onSuccess(List.of(bitmap));
            } catch (IOException e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Shuts down the executor service when it's no longer needed.
     */
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Callback interface for asynchronous PDF rendering.
     */
    public interface PDFRenderCallback {
        void onSuccess(List<Bitmap> bitmaps);

        void onError(Exception e);
    }
}
