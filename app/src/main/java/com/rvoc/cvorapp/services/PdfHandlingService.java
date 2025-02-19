package com.rvoc.cvorapp.services;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.core.util.Consumer;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.DialogLayoutBinding;
import com.rvoc.cvorapp.utils.FileUtils;
import com.rvoc.cvorapp.utils.ImageUtils;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.rendering.ImageType;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class PdfHandlingService {

    private final Context context;

    private static final String TAG = "PDF Service";
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 50 MB, you can change this limit.

    @Inject
    public PdfHandlingService(@ApplicationContext Context context) {
        this.context = context;
    }

    public interface PasswordCallback {
        void onPasswordEntered(@NonNull Uri decryptedUri);
        void onPasswordCancelled();
    }

    // Combine multiple PDFs into one
    public File combinePDF(@NonNull List<Uri> inputFiles, @NonNull File outputFile) throws Exception {
        Log.d(TAG, "PDF Service - Combining PDFs with optimized memory usage");

        // Threshold for switching between memory optimization strategies
        final long LARGE_FILE_THRESHOLD_BYTES = 20 * 1024 * 1024; // 20MB

        // Create the merged PDF document
        try (PDDocument mergedDocument = new PDDocument()) {
            for (Uri uri : inputFiles) {
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                    if (inputStream == null) {
                        throw new IOException("Unable to open input stream for URI: " + uri);
                    }

                    // Check the file size
                    long fileSize = getFileSize(uri);

                    // Load the document
                    try (PDDocument document = PDDocument.load(inputStream)) {
                        if (fileSize > LARGE_FILE_THRESHOLD_BYTES) {
                            Log.d(TAG, "Processing large file: " + uri);
                            // For large files, add pages one-by-one
                            addPagesIndividually(document, mergedDocument);
                        } else {
                            Log.d(TAG, "Processing small file: " + uri);
                            // For small files, add pages more efficiently
                            addAllPagesToMergedDocument(document, mergedDocument);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading or merging PDF from URI: " + uri, e);
                    throw new IOException("Failed to read or merge PDF file: " + uri, e);
                }
            }

            // Save the merged document
            mergedDocument.save(outputFile);
        }

        Log.d(TAG, "PDF merge completed successfully.");
        return outputFile;
    }

    /**
     * Adds all pages from the source document to the merged document (small file optimization).
     */
    private void addAllPagesToMergedDocument(PDDocument sourceDocument, PDDocument mergedDocument) throws IOException {
        for (PDPage page : sourceDocument.getPages()) {
            mergedDocument.addPage(new PDPage(page.getCOSObject())); // Clone the page to avoid closure issues
        }
    }

    /**
     * Adds pages one-by-one to optimize memory usage for large files.
     */
    private void addPagesIndividually(PDDocument sourceDocument, PDDocument mergedDocument) throws IOException {
        for (int i = 0; i < sourceDocument.getNumberOfPages(); i++) {
            mergedDocument.addPage(new PDPage(sourceDocument.getPage(i).getCOSObject())); // Clone page for safety
        }
    }

    /**
     * Utility method to get the file size from a URI.
     */
    private long getFileSize(Uri uri) throws IOException {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                long size = cursor.getLong(sizeIndex);
                cursor.close();
                return size;
            }
            cursor.close();
        }
        throw new IOException("Unable to determine file size for URI: " + uri);
    }

    //Converting images to a PDF
    public File convertImagesToPDF(@NonNull List<Uri> imageUris, @NonNull File outputFile) throws Exception {
        Log.d(TAG, "PDF Service - Converting Images to PDF");

        try (PDDocument document = new PDDocument()) {
            for (Uri uri : imageUris) {
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                    if (inputStream == null) {
                        throw new IOException("Unable to open input stream for URI: " + uri);
                    }

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                    /*// Enhance image quality and decode it as a bitmap
                    Bitmap enhancedBitmap = ImageUtils.enhanceImageQuality(readStream(inputStream));
                    enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);*/


                    byte[] buffer = new byte[8192]; // 8KB buffer size
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                    byte[] imageData = byteArrayOutputStream.toByteArray();

                    // Create PDImageXObject from the byte array
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageData, "image");

                    // Get the dimensions of the image
                    float imageWidth = pdImage.getWidth();
                    float imageHeight = pdImage.getHeight();

                    // Define PDF page size (A4)
                    float pageWidth = PDRectangle.A4.getWidth();
                    float pageHeight = PDRectangle.A4.getHeight();

                    // Scale the image to fit within the PDF page while maintaining the aspect ratio
                    float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);
                    float scaledWidth = imageWidth * scale;
                    float scaledHeight = imageHeight * scale;

                    // Create a new PDF page with A4 dimensions
                    PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                    document.addPage(page);

                    // Add the image to the page
                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.OVERWRITE, true, true)) {
                        // Center the image on the page
                        float xOffset = (pageWidth - scaledWidth) / 2;
                        float yOffset = (pageHeight - scaledHeight) / 2;
                        contentStream.drawImage(pdImage, xOffset, yOffset, scaledWidth, scaledHeight);
                    }

                    // Explicitly clear memory for large images to reduce memory pressure
                    // enhancedBitmap.recycle();
                    // byteArrayOutputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error processing image: " + uri, e);
                    throw new IOException("Failed to process image: " + uri.toString(), e);
                }
            }

            // Save the document incrementally to minimize memory usage
            document.save(outputFile);
        }

        Log.d(TAG, "PDF Service - Conversion Completed");
        return outputFile;
    }


    // Split a PDF file
    public List<File> splitPDF(@NonNull PDDocument document, @NonNull File outputDir) throws Exception {
        List<File> splitFiles = new ArrayList<>();

        // Ensure output directory exists
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
        }

        int totalPages = document.getNumberOfPages();
        Log.d(TAG, "Total pages in PDF: " + totalPages);

        // Split the PDF into individual pages
        for (int i = 0; i < totalPages; i++) {
            try (PDDocument singlePageDoc = new PDDocument()) {
                singlePageDoc.addPage(document.getPage(i));

                File outputFile = new File(outputDir, "CVOR_split_" + (i + 1) + ".pdf");
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    singlePageDoc.save(fos);
                }

                splitFiles.add(outputFile);
                Log.d(TAG, "Created split PDF: " + outputFile.getAbsolutePath());
            }
        }
        return splitFiles;
    }


    // Compress a PDF file
    public File compressPDF(@NonNull Uri inputFileUri, @NonNull File outputFile, String quality) throws Exception {
        Log.d(TAG, "Starting PDF compression...");

        // Map quality to downscale factor
        float imageQuality;
        float downscaleFactor = switch (quality.toLowerCase()) {
            case "high" -> {
                imageQuality = 0.8f;
                yield 1.0f;
            }
            case "low" -> {
                imageQuality = 0.4f;
                yield 0.5f;
            }
            default -> {
                imageQuality = 0.6f;
                yield 0.75f;
            }
        };  // Used to adjust the DPI relative to original

        try (InputStream inputStream = context.getContentResolver().openInputStream(inputFileUri);
             PDDocument document = PDDocument.load(inputStream, MemoryUsageSetting.setupTempFileOnly())) {

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            PDDocument compressedDoc = new PDDocument();

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);

                // Calculate original DPI (based on the MediaBox size and standard 8.5x11 inches for Letter size)
                float pageWidthInPoints = page.getMediaBox().getWidth();  // Points (1/72 inch)
                float pageHeightInPoints = page.getMediaBox().getHeight();
                float assumedPageWidthInInches = 8.5f;  // Assuming Letter size
                float originalDpi = pageWidthInPoints / assumedPageWidthInInches;

                // Downscale the DPI based on quality
                int effectiveDpi = Math.round(originalDpi * downscaleFactor);
                effectiveDpi = Math.min(effectiveDpi, 150);  // Cap to 150 DPI to prevent excessive quality

                // Render page to Bitmap
                Bitmap renderedImage = pdfRenderer.renderImage(i, effectiveDpi / 72.0f, ImageType.RGB);


                // Create new compressed page
                PDPage newPage = new PDPage(page.getCropBox());  // Use crop box for better fit
                compressedDoc.addPage(newPage);

                // Convert Bitmap to compressed JPEG and add it to the new document
                try (PDPageContentStream contentStream = new PDPageContentStream(compressedDoc, newPage)) {
                    contentStream.drawImage(JPEGFactory.createFromImage(compressedDoc, renderedImage, imageQuality), 0, 0);
                }
            }

            // Optimize PDF & remove metadata
            compressedDoc.setAllSecurityToBeRemoved(true);
            compressedDoc.save(new FileOutputStream(outputFile));
            compressedDoc.close();

            Log.d(TAG, "PDF compression complete. Saved at: " + outputFile.getAbsolutePath());
            return outputFile;
        }
    }


    // Decrypt a PDF document if it's password-protected
    public void decryptPDF(@NonNull Uri fileUri, @NonNull Activity activity, @NonNull PasswordCallback callback) {
        requestPassword(activity, fileUri, password -> {
            if (password == null || password.isEmpty()) {
                Log.d(TAG, "PDF Decryption 1: User cancelled password entry.");
                callback.onPasswordCancelled();
                return;
            }

            try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                 PDDocument document = PDDocument.load(inputStream, password)) {

                Log.d(TAG, "PDF Decryption 2: Document opened with password.");

                document.setAllSecurityToBeRemoved(true);
                // Save the decrypted PDF
                File decryptedFile = new File(context.getCacheDir(), "decrypted_" + System.currentTimeMillis() + ".pdf");
                document.save(decryptedFile);
                document.close();

                // Return the result via callback
                Uri decryptedUri = FileProvider.getUriForFile(context, "com.rvoc.cvorapp.fileprovider", decryptedFile);
                callback.onPasswordEntered(decryptedUri);
                Log.d(TAG, "PDF Decryption 3: Decryption successful.");

            } catch (InvalidPasswordException e) {
                Log.e(TAG, "PDF decryption failed: Incorrect password.");
                Toast.makeText(context, "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show();
                decryptPDF(fileUri, activity, callback);  // **Retry password entry**
            } catch (IOException e) {
                Log.e(TAG, "Error decrypting PDF", e);
                callback.onPasswordCancelled();
            }
        });
    }
    private void requestPassword(@NonNull Activity activity, @NonNull Uri fileUri, @NonNull Consumer<String> passwordConsumer) {
        String fileName = FileUtils.getFileNameFromUri(context, fileUri);
        Log.d(TAG, "PDF Decryption 4.");

        activity.runOnUiThread(() -> {
            LayoutInflater inflater = LayoutInflater.from(activity);
            DialogLayoutBinding binding = DialogLayoutBinding.inflate(inflater);

            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme)
                    .setView(binding.getRoot())
                    .setCancelable(false);

            // Customize message
            binding.dialogMessage.setText(activity.getString(R.string.password_prompt, fileName));
            binding.inputField.setVisibility(View.VISIBLE);
            binding.inputField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.positiveButton.setText(R.string.ok);
            binding.negativeButton.setText(R.string.cancel);

            AlertDialog dialog = builder.create();

            // Button click listeners
            binding.positiveButton.setOnClickListener(v -> {
                dialog.dismiss();
                passwordConsumer.accept(binding.inputField.getText().toString());
            });

            binding.negativeButton.setOnClickListener(v -> {
                dialog.dismiss();
                passwordConsumer.accept(null);
            });

            dialog.show();
            Log.d(TAG, "PDF Decryption 8.");
        });
    }

    public PDDocument checkPDFValidForSplit(@NonNull Uri inputFileUri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(inputFileUri)) {
            if (inputStream == null) {
                throw new IOException("Unable to open input stream for URI: " + inputFileUri);
            }

            PDDocument document = PDDocument.load(inputStream);
            if (document.getNumberOfPages() > 25) {
                document.close(); // Ensure it's closed if invalid
                return null;
            }

            return document; // Return only if valid
        } catch (Exception e) {
            Log.e(TAG, "Error loading PDF for URI: " + inputFileUri, e);
            throw e;
        }
    }

    // Helper to read InputStream into a byte array
    private byte[] readStream(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Log.d(TAG, "PDF Service 4.");
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * Reads a PDF from a URI into a byte array (efficient, avoids stream closing issues).
     */
    private byte[] readUriToByteArray(Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            if (inputStream == null) {
                throw new IOException("Unable to open input stream for URI: " + uri);
            }

            byte[] buffer = new byte[8192]; // 8KB buffer for speed
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            return outputStream.toByteArray();
        }
    }

    // Check if a file is large, and if so, handle it in the background (Worker)
    public boolean isLargeFile(File file) {
        return file.length() > MAX_FILE_SIZE;
    }

    // Worker implementation for processing large PDFs (stub, you need to integrate WorkManager)
    public void processLargePDFInBackground(File file) {
        if (isLargeFile(file)) {
            // Create and enqueue a Worker using WorkManager or use a background thread to process
            // Implement the Worker for long-running tasks like PDF merging
            // WorkManager.enqueue(new OneTimeWorkRequest.Builder(LargeFileWorker.class).build());
        }
    }
}
