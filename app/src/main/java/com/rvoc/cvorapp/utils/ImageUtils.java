package com.rvoc.cvorapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.pdf.PdfRenderer;
import android.media.Image;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    public static String getThumbnailPath(Context context, Uri fileUri) {
        try {
            String mimeType = context.getContentResolver().getType(fileUri);
            Bitmap thumbnail = null;

            if (mimeType != null && mimeType.startsWith("image/")) {
                thumbnail = generateImageThumbnail(context, fileUri);
            } else if (mimeType != null && mimeType.equals("application/pdf")) {
                thumbnail = generatePDFThumbnail(context, fileUri);
            }

            if (thumbnail != null) {
                return saveThumbnailToCache(context, thumbnail, fileUri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating thumbnail", e);
        }
        return null; // Return null if thumbnail generation fails
    }

    private static Bitmap generateImageThumbnail(Context context, Uri fileUri) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), fileUri);
    }

    private static Bitmap generatePDFThumbnail(Context context, Uri fileUri) throws IOException {
        ParcelFileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(fileUri, "r");
        if (fileDescriptor != null) {
            PdfRenderer renderer = new PdfRenderer(fileDescriptor);
            PdfRenderer.Page page = renderer.openPage(0);

            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            page.close();
            renderer.close();
            fileDescriptor.close();

            return bitmap;
        }
        return null;
    }

    private static String saveThumbnailToCache(Context context, Bitmap bitmap, Uri fileUri) {
        File cacheDir = new File(context.getCacheDir(), "favourites_thumbnail");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        String fileName = fileUri.getLastPathSegment() + "_thumb.jpg";
        File thumbnailFile = new File(cacheDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(thumbnailFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            return thumbnailFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error saving thumbnail", e);
        }
        return null;
    }



    /**
     * Converts an ImageProxy to a Bitmap.
     *
     * @param image ImageProxy from CameraX.
     * @return Bitmap representation of the ImageProxy.
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    public static Bitmap imageProxyToBitmap(ImageProxy image) {
        Image imageData = image.getImage();
        if (imageData == null) {
            Log.e(TAG, "ImageProxy's underlying Image is null. Returning null.");
            return null;
        }

        Bitmap bitmap = yuvToBitmap(imageData, image.getWidth(), image.getHeight());
        if (bitmap == null) {
            Log.e(TAG, "Failed to convert ImageProxy to Bitmap. Returning null.");
            return null;
        }

        return rotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());
    }

    /**
     * Converts a YUV_420_888 Image to an RGB Bitmap.
     *
     * @param image  The YUV image to convert.
     * @param width  Image width.
     * @param height Image height.
     * @return Converted Bitmap.
     */
    private static Bitmap yuvToBitmap(Image image, int width, int height) {
        Image.Plane[] planes = image.getPlanes();
        if (planes.length < 3) {
            Log.e(TAG, "Expected 3 planes for YUV_420_888, but found " + planes.length);
            return null; // Handle this case appropriately.
        }

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y channel
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U channel
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V channel

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // Copy Y channel
        yBuffer.get(nv21, 0, ySize);

        // Interleave U and V channels
        byte[] uBytes = new byte[uSize];
        byte[] vBytes = new byte[vSize];
        uBuffer.get(uBytes);
        vBuffer.get(vBytes);

        for (int i = 0; i < uSize; i++) {
            nv21[ySize + 2 * i] = vBytes[i];
            nv21[ySize + 2 * i + 1] = uBytes[i];
        }

        return nv21ToBitmap(nv21, width, height);
    }

    /**
     * Converts NV21 byte array to a Bitmap.
     *
     * @param nv21   Byte array in NV21 format.
     * @param width  Image width.
     * @param height Image height.
     * @return Converted Bitmap.
     */
    private static Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        // android.media.ImageFormat imageFormat = new android.media.ImageFormat();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, outputStream);
        byte[] jpegData = outputStream.toByteArray();

        return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
    }

    /**
     * Rotates a Bitmap based on the given rotation degrees.
     *
     * @param bitmap          The Bitmap to rotate.
     * @param rotationDegrees Rotation degrees (e.g., 0, 90, 180, 270).
     * @return Rotated Bitmap.
     */
    private static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        Log.d(TAG, "Rotating bitmap by " + rotationDegrees + " degrees.");
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
