package com.leafiq.app.domain.service;

import android.content.Context;
import android.net.Uri;

import com.leafiq.app.util.ImageUtils;

import java.io.File;
import java.io.IOException;

/**
 * Domain layer wrapper for image processing operations.
 * Delegates to ImageUtils while keeping domain layer decoupled from Android context details.
 * <p>
 * Use cases:
 * - Prepare images for AI API calls (base64 encoding)
 * - Save full-resolution photos to internal storage
 * - Create thumbnails for plant cards
 */
public class ImagePreprocessor {

    private final Context context;

    /**
     * Creates an ImagePreprocessor with application context.
     *
     * @param context Application context (for ContentResolver access)
     */
    public ImagePreprocessor(Context context) {
        this.context = context;
    }

    /**
     * Prepares an image for AI API consumption.
     * Resizes to max 1024px, compresses to JPEG, and encodes to base64.
     *
     * @param imageUri URI of the source image
     * @return Base64-encoded JPEG string ready for API
     * @throws IOException if image cannot be read or processed
     */
    public String prepareForApi(Uri imageUri) throws IOException {
        return ImageUtils.prepareForApi(context, imageUri);
    }

    /**
     * Saves a photo to internal app storage.
     * Creates plant_photos directory if it doesn't exist.
     *
     * @param sourceUri URI of the source image
     * @param plantId ID of the plant (used in filename)
     * @return Absolute file path of saved photo
     * @throws IOException if image cannot be saved
     */
    public String savePhoto(Uri sourceUri, String plantId) throws IOException {
        return ImageUtils.savePhoto(context, sourceUri, plantId);
    }

    /**
     * Creates a thumbnail for plant card display.
     * Resizes to 256px and saves to thumbnails directory.
     *
     * @param sourceUri URI of the source image
     * @param plantId ID of the plant (used in filename)
     * @return Absolute file path of saved thumbnail
     * @throws IOException if thumbnail cannot be created
     */
    public String saveThumbnail(Uri sourceUri, String plantId) throws IOException {
        return ImageUtils.saveThumbnail(context, sourceUri, plantId);
    }

    /**
     * Create a medium-resolution thumbnail (300px) for library grid view.
     */
    public String saveMediumThumbnail(Uri sourceUri, String plantId) throws IOException {
        return ImageUtils.saveMediumThumbnail(context, sourceUri, plantId);
    }

    /**
     * Create a high-resolution thumbnail (800px) for detail page.
     */
    public String saveHighResThumbnail(Uri sourceUri, String plantId) throws IOException {
        return ImageUtils.saveHighResThumbnail(context, sourceUri, plantId);
    }

    /**
     * Returns the cache directory used for any temporary image processing files.
     * Used by callers to clean up temp files on error.
     *
     * @return Cache directory File object
     */
    public File getCacheDir() {
        return context.getCacheDir();
    }
}
