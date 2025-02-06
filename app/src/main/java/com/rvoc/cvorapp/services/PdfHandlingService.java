package com.rvoc.cvorapp.services;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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

    // Combine multiple PDFs into one
    public File combinePDF(@NonNull List<Uri> inputFiles, @NonNull File outputFile) throws Exception {
        PDFMergerUtility mergerUtility = new PDFMergerUtility();

        for (Uri uri : inputFiles) {
            byte[] pdfBytes = readUriToByteArray(uri);
            mergerUtility.addSource(new ByteArrayInputStream(pdfBytes));
        }

        // Set the destination file and merge the documents
        mergerUtility.setDestinationFileName(outputFile.getPath());
        mergerUtility.mergeDocuments(null);

        Log.d(TAG, "PDF merge completed successfully.");
        return outputFile;
    }

    public File convertImagesToPDF(@NonNull List<Uri> imageUris, @NonNull File outputFile) throws Exception {
        Log.d(TAG, "PDF Service - Converting Images to PDF");

        try (PDDocument document = new PDDocument()) {
            for (Uri uri : imageUris) {
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                    if (inputStream == null) {
                        throw new IOException("Unable to open input stream for URI: " + uri);
                    }

                    // Create PDImageXObject directly from input stream
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                            document,
                            readStream(inputStream),
                            "image"
                    );

                    // Get original image dimensions
                    float imageWidth = pdImage.getWidth();
                    float imageHeight = pdImage.getHeight();

                    // Define PDF page size (A4)
                    float pageWidth = PDRectangle.A4.getWidth();
                    float pageHeight = PDRectangle.A4.getHeight();

                    // Scale image to fit within the PDF page while maintaining aspect ratio
                    float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);
                    float scaledWidth = imageWidth * scale;
                    float scaledHeight = imageHeight * scale;

                    // Create a new page with A4 dimensions
                    PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                    document.addPage(page);

                    // Add image to the center of the page
                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                        float xOffset = (pageWidth - scaledWidth) / 2;
                        float yOffset = (pageHeight - scaledHeight) / 2;
                        contentStream.drawImage(pdImage, xOffset, yOffset, scaledWidth, scaledHeight);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing image: " + uri, e);
                    throw new IOException("Failed to process image: " + uri.toString(), e);
                }
            }

            // Save the document
            document.save(outputFile);
        }

        Log.d(TAG, "PDF Service - Conversion Completed");
        return outputFile;
    }

    // Split a pdf file
    public static List<File> splitPDF(@NonNull Context context, @NonNull Uri inputFileUri, @NonNull File outputDir) throws Exception {
        List<File> splitFiles = new ArrayList<>();

        // Ensure output directory exists
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
        }

        try (InputStream inputStream = context.getContentResolver().openInputStream(inputFileUri)) {
            if (inputStream == null) {
                throw new IOException("Unable to open input stream for URI: " + inputFileUri);
            }

            try (PDDocument document = PDDocument.load(inputStream)) {
                int totalPages = document.getNumberOfPages();
                Log.d(TAG, "Total pages in PDF: " + totalPages);

                for (int i = 0; i < totalPages; i++) {
                    try (PDDocument singlePageDoc = new PDDocument()) {
                        singlePageDoc.addPage(document.getPage(i));

                        File outputFile = new File(outputDir, "page_" + (i + 1) + ".pdf");
                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            singlePageDoc.save(fos);
                        }

                        splitFiles.add(outputFile);
                        Log.d(TAG, "Created split PDF: " + outputFile.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error splitting PDF file: " + inputFileUri, e);
            throw e;
        }

        return splitFiles;
    }

    // Decrypt a PDF document if it's password-protected
    public Uri decryptPDF(@NonNull Activity activity, @NonNull InputStream inputStream) throws Exception {
        PDDocument document = null;

        while (document == null) {
            String password = promptForPassword(activity);

            if (password == null) {
                // User canceled, destroy the activity and notify them
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Password is required to proceed.", Toast.LENGTH_SHORT).show()
                    );
                    ((Activity) context).finish();
                }
                throw new IOException("User canceled password prompt.");
            }

            try {
                document = PDDocument.load(inputStream, password);
                if (document.isEncrypted()) {
                    document.setAllSecurityToBeRemoved(true);
                }
            } catch (IOException e) {
                // Handle incorrect password
                if (e.getMessage() != null && e.getMessage().contains("password")) {
                    showToast(activity);
                } else {
                    throw new IOException("Failed to decrypt PDF.", e);
                }
            }
        }
        try {

            File decryptedFile = new File(context.getCacheDir(), "decrypted_" + System.currentTimeMillis() + ".pdf");
            document.save(decryptedFile);

            return FileProvider.getUriForFile(context, "com.rvoc.cvorapp.fileprovider", decryptedFile);
        } catch (IOException e) {
            Log.e(TAG, "Error saving decrypted PDF", e);
            return null;
        }
    }

    // Password prompt
    private String promptForPassword(@NonNull Activity activity) {
        final String[] password = new String[1];
        final CountDownLatch latch = new CountDownLatch(1);

        activity.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Your file is password protected, enter password to continue.");

            // Add an EditText to the dialog
            final EditText input = new EditText(activity);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                password[0] = input.getText().toString();
                latch.countDown(); // Release the latch
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                password[0] = null; // User canceled
                latch.countDown(); // Release the latch
            });

            builder.setCancelable(false); // Ensure the dialog cannot be dismissed by clicking outside
            builder.show();
        });

        try {
            latch.await(); // Wait until the user responds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        return password[0];
    }

    // Helper to show a toast
    private void showToast(@NonNull Activity activity) {
        activity.runOnUiThread(() ->
                Toast.makeText(context, "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show()
        );
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
