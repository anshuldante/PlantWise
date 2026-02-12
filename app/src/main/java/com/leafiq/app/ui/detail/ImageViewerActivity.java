package com.leafiq.app.ui.detail;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.leafiq.app.R;

import java.io.File;

/**
 * Full-screen image viewer with pinch-to-zoom support.
 * Tap anywhere to close. No share button (per user decision).
 */
public class ImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "image_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive mode
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        setContentView(R.layout.activity_image_viewer);

        String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        if (imagePath == null) {
            finish();
            return;
        }

        PhotoView photoView = findViewById(R.id.photo_view);

        // Load full-resolution photo
        Glide.with(this)
            .load(new File(imagePath))
            .into(photoView);

        // Tap anywhere to close (per user decision: tap to close)
        photoView.setOnClickListener(v -> finish());
    }
}
