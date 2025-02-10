package com.rvoc.cvorapp.services;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
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
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

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
    public List<File> splitPDF(@NonNull Uri inputFileUri, @NonNull File outputDir) throws Exception {
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

    // Compress a PDF file
    public File compressPDF(@NonNull Uri inputFileUri, @NonNull File outputFile, Integer dpi) throws Exception {
        Log.d(TAG, "Starting PDF compression...");

        try (InputStream inputStream = context.getContentResolver().openInputStream(inputFileUri);
             PDDocument document = PDDocument.load(inputStream, MemoryUsageSetting.setupTempFileOnly())) {

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            PDDocument compressedDoc = new PDDocument();

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                dpi = (dpi != null) ? dpi : 100; // Adjust DPI for compression (Lower DPI = more compression); (70 to 300)
                float imageQuality;

                imageQuality = (dpi < 100) ? 0.4f : (dpi < 150) ? 0.7f : 0.8f;

                // Render page to Bitmap
                Bitmap renderedImage = pdfRenderer.renderImage(i, dpi / 72.0f);

                // Create new compressed page
                PDPage newPage = new PDPage(page.getMediaBox());
                compressedDoc.addPage(newPage);

                // Convert Bitmap to compressed JPEG and add it to new document
                PDPageContentStream contentStream = new PDPageContentStream(compressedDoc, newPage);
                contentStream.drawImage(JPEGFactory.createFromImage(compressedDoc, renderedImage, imageQuality), 0, 0);
                contentStream.close();
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


    /*private void requestPassword(@NonNull Activity activity, @NonNull Uri fileUri, @NonNull Consumer<String> passwordConsumer) {

        String fileName = FileUtils.getFileNameFromUri(context, fileUri);
        Log.d(TAG, "PDF Decryption 4.");
        activity.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme);
            builder.setTitle("Enter Password for: " + fileName);

            final EditText input = new EditText(activity);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(input);
            Log.d(TAG, "PDF Decryption 5.");

            builder.setPositiveButton("OK", (dialog, which) -> passwordConsumer.accept(input.getText().toString()));
            builder.setNegativeButton("Cancel", (dialog, which) -> passwordConsumer.accept(null));
            Log.d(TAG, "PDF Decryption 6.");

            builder.setCancelable(false);
            builder.show();
            Log.d(TAG, "PDF Decryption 8.");
        });
    }*/

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


    // Helper to show a toast
    private void showToast(@NonNull Activity activity) {
        Log.d(TAG, "PDF Decryption 16.");
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
