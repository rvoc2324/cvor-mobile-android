package com.rvoc.cvorapp.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class PdfHandlingService {

    private final Context context;

    private static final String TAG = "PDF Service";

    @Inject
    public PdfHandlingService(@ApplicationContext Context context) {
        this.context = context;
    }

    // Combine multiple PDFs into one
    public File combinePDF(@NonNull List<Uri> inputFiles, @NonNull File outputFile) throws Exception {
        PDFMergerUtility mergerUtility = new PDFMergerUtility();
        Log.d(TAG, "PDF Service 5.");

        for (Uri uri : inputFiles) {
            PDDocument document;
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                try {
                    // Attempt to load the PDF document
                    document = PDDocument.load(inputStream);
                    Log.d(TAG, "PDF Service 6.");
                } catch (IOException e) {
                    // If the document is password-protected, an IOException will be thrown
                    if (e.getMessage() != null && e.getMessage().contains("password") && inputStream != null) {
                        // Attempt to decrypt the document after catching the IOException
                        document = decryptPDF(inputStream);
                        Log.d(TAG, "PDF Service 7.");
                    } else {
                        // If the exception is not related to encryption, rethrow it
                        throw e;
                    }
                }

                // Add the document to the merger utility if it's valid
                if (document != null) {
                    ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
                    document.save(decryptedStream);
                    Log.d(TAG, "PDF Service 8.");
                    document.close();
                    mergerUtility.addSource(new ByteArrayInputStream(decryptedStream.toByteArray()));
                }
            } catch (Exception e) {
                Log.e("PdfHandlingService", "Error processing file", e);
                throw new IOException("Failed to process file: " + uri.toString(), e);
            }
        }

        // Set the destination file and merge the documents
        mergerUtility.setDestinationFileName(outputFile.getPath());
        mergerUtility.mergeDocuments(null);
        Log.d(TAG, "PDF Service 9.");
        return outputFile;
    }

    // Convert images to a single PDF
    public File convertImagesToPDF(@NonNull List<Uri> imageUris, @NonNull File outputFile) throws Exception {
        Log.d(TAG, "PDF Service 1.");
        try (PDDocument document = new PDDocument()) {
            for (Uri uri : imageUris) {
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                    // Convert the image to PDImageXObject
                    assert inputStream != null;
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                            document,
                            readStream(inputStream),
                            "image"
                    );
                    Log.d(TAG, "PDF Service 2.");

                    // Create a new page and add the image
                    PDPage page = new PDPage(new PDRectangle(pdImage.getWidth(), pdImage.getHeight()));
                    document.addPage(page);
                    Log.d(TAG, "PDF Service 3.");
                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                        contentStream.drawImage(pdImage, 0, 0, pdImage.getWidth(), pdImage.getHeight());
                    }
                } catch (Exception e) {
                    Log.e("PdfHandlingService", "Error processing file", e);
                    throw new IOException("Failed to process image: " + uri.toString());
                }
            }

            document.save(outputFile);
        }
        return outputFile;
    }

    // Decrypt a PDF document if it's password protected
    public PDDocument decryptPDF(@NonNull InputStream inputStream) throws Exception {
        String password = promptForPassword();
        if (password == null || password.isEmpty()) {
            throw new IOException("Password is required to decrypt the PDF.");
        }
        try {
            PDDocument decryptedDocument = PDDocument.load(inputStream, password);
            if (decryptedDocument.isEncrypted()) {
                decryptedDocument.setAllSecurityToBeRemoved(true);
            }
            return decryptedDocument;
        } catch (Exception e) {
            throw new IOException("Failed to decrypt PDF with the provided password.");
        }
    }

    // Prompt the user for a password
    private String promptForPassword() {
        final String[] password = new String[1];
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Enter PDF Password");

        // Add an EditText to the dialog
        final android.widget.EditText input = new android.widget.EditText(context);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> password[0] = input.getText().toString());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();

        return password[0];
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
}
