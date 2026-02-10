package com.leafiq.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageUtils {

    private static final int MAX_DIMENSION = 1024;
    private static final int JPEG_QUALITY = 80;

    /**
     * Compress and resize a photo, return base64 string ready for API.
     */
    public static String prepareForApi(Context context, Uri imageUri) throws IOException {
        Bitmap original = getBitmapFromUri(context, imageUri);
        if (original == null) {
            throw new IOException("Failed to decode image");
        }

        Bitmap resized = resizeBitmap(original, MAX_DIMENSION);

        // Pre-size for 1024px JPEG: avoids repeated reallocation from 32-byte default
        // 1024px JPEG @ 80% quality: ~150-300KB compressed
        // Base64 expansion adds ~33%: ~200-400KB total
        // 512KB initial capacity eliminates reallocations for most images
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512 * 1024);
        resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);

        if (original != resized) {
            original.recycle();
        }
        resized.recycle();

        // Safety check: prevent OOM from unexpectedly large images
        if (baos.size() > 5 * 1024 * 1024) {
            throw new IOException("Image too large for API upload (" + (baos.size() / 1024) + " KB)");
        }

        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /**
     * Save a photo to internal app storage, return the file path.
     */
    public static String savePhoto(Context context, Uri sourceUri, String plantId)
            throws IOException {
        File dir = new File(context.getFilesDir(), "plant_photos");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filename = plantId + "_" + System.currentTimeMillis() + ".jpg";
        File dest = new File(dir, filename);

        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(dest)) {
            if (in == null) {
                throw new IOException("Cannot open input stream for URI");
            }
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }

        return dest.getAbsolutePath();
    }

    /**
     * Create a thumbnail for the plant card display.
     */
    public static String saveThumbnail(Context context, Uri sourceUri, String plantId)
            throws IOException {
        File dir = new File(context.getFilesDir(), "thumbnails");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Bitmap original = getBitmapFromUri(context, sourceUri);
        if (original == null) {
            throw new IOException("Failed to decode image");
        }

        Bitmap thumbnail = resizeBitmap(original, 256);

        String filename = plantId + "_thumb.jpg";
        File dest = new File(dir, filename);

        try (FileOutputStream out = new FileOutputStream(dest)) {
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, out);
        }

        if (original != thumbnail) {
            original.recycle();
        }
        thumbnail.recycle();

        return dest.getAbsolutePath();
    }

    private static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(input);
        }
    }

    private static Bitmap resizeBitmap(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap;
        }

        float scale = Math.min(
            (float) maxDimension / width,
            (float) maxDimension / height
        );

        return Bitmap.createScaledBitmap(
            bitmap,
            Math.round(width * scale),
            Math.round(height * scale),
            true
        );
    }
}
