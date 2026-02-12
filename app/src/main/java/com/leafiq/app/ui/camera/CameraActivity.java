package com.leafiq.app.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.leafiq.app.R;
import com.leafiq.app.ui.analysis.AnalysisActivity;
import com.leafiq.app.util.PhotoTipsManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Camera activity for plant photo capture.
 *
 * <p><b>Phase 13 Photo Tips Integration:</b></p>
 * <ul>
 *   <li><b>Conditional tips display:</b> Photo tips bottom sheet appears in two scenarios:
 *       (1) First-ever analysis (user has never seen tips), or
 *       (2) After quality failure recorded by AnalysisActivity.</li>
 *   <li><b>Auto-suppression:</b> Tips are suppressed only when user explicitly clicks "Got It" button.
 *       Swipe-dismiss preserves tips for next camera launch (prevents accidental suppression).</li>
 *   <li><b>Quality failure reasons:</b> AnalysisActivity records issueType ("blur", "dark", "bright",
 *       "resolution") which drives contextual highlighting in the tips bottom sheet (specific guidance
 *       for the exact quality issue encountered).</li>
 *   <li><b>PhotoTipsManager integration:</b> Uses SharedPreferences-based state tracking via
 *       PhotoTipsManager.shouldShowTips() to determine when to display tips before camera start.</li>
 * </ul>
 */
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    public static final String EXTRA_PLANT_ID = "extra_plant_id";

    private static final int FLASH_MODE_OFF = 0;
    private static final int FLASH_MODE_ON = 1;
    private static final int FLASH_MODE_AUTO = 2;

    private PreviewView previewView;
    private FloatingActionButton captureButton;
    private ImageButton galleryButton;
    private ImageButton closeButton;
    private ImageButton flashButton;
    private ProgressBar progress;

    private ImageCapture imageCapture;
    private Camera camera;
    private ExecutorService cameraExecutor;
    private String plantId;
    private PhotoTipsManager tipsManager;
    private int currentFlashMode = FLASH_MODE_OFF;

    private final ActivityResultLauncher<String> requestCameraPermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                checkAndShowTipsOrStartCamera();
            } else {
                showPermissionDeniedDialog();
            }
        });

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
        registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                proceedToAnalysis(uri);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        plantId = getIntent().getStringExtra(EXTRA_PLANT_ID);

        previewView = findViewById(R.id.preview_view);
        captureButton = findViewById(R.id.btn_capture);
        galleryButton = findViewById(R.id.btn_gallery);
        closeButton = findViewById(R.id.btn_close);
        flashButton = findViewById(R.id.btn_flash);
        progress = findViewById(R.id.progress);

        cameraExecutor = Executors.newSingleThreadExecutor();
        tipsManager = new PhotoTipsManager(this);

        captureButton.setOnClickListener(v -> takePhoto());
        galleryButton.setOnClickListener(v -> openGallery());
        closeButton.setOnClickListener(v -> finish());
        flashButton.setOnClickListener(v -> toggleFlash());

        if (checkCameraPermission()) {
            checkAndShowTipsOrStartCamera();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED;
    }

    private void checkAndShowTipsOrStartCamera() {
        if (tipsManager.shouldShowTips()) {
            String failureReason = tipsManager.getQualityFailureReason();
            PhotoTipsBottomSheet sheet = PhotoTipsBottomSheet.newInstance(failureReason);
            sheet.setOnTipsDismissedListener(() -> {
                tipsManager.markTipsSeen();
                startCamera();
            });
            sheet.show(getSupportFragmentManager(), "photo_tips");
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                // Hide flash button if device has no flash
                if (!camera.getCameraInfo().hasFlashUnit()) {
                    flashButton.setVisibility(View.GONE);
                }

            } catch (Exception e) {
                Log.e(TAG, "Camera initialization failed", e);
                Toast.makeText(this, "Camera failed to start", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void toggleFlash() {
        currentFlashMode = (currentFlashMode + 1) % 3;
        switch (currentFlashMode) {
            case FLASH_MODE_OFF:
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_OFF);
                flashButton.setImageResource(R.drawable.ic_flash_off);
                flashButton.setContentDescription(getString(R.string.flash_off));
                if (camera != null) {
                    camera.getCameraControl().enableTorch(false);
                }
                break;
            case FLASH_MODE_ON:
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_ON);
                flashButton.setImageResource(R.drawable.ic_flash_on);
                flashButton.setContentDescription(getString(R.string.flash_on));
                if (camera != null) {
                    camera.getCameraControl().enableTorch(true);
                }
                break;
            case FLASH_MODE_AUTO:
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_AUTO);
                flashButton.setImageResource(R.drawable.ic_flash_auto);
                flashButton.setContentDescription(getString(R.string.flash_auto));
                if (camera != null) {
                    camera.getCameraControl().enableTorch(false);
                }
                break;
        }
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        setLoading(true);

        File photoDir = new File(getFilesDir(), "temp_photos");
        if (!photoDir.exists()) {
            photoDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
        File photoFile = new File(photoDir, "IMG_" + timestamp + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
            new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor,
            new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                    Uri savedUri = Uri.fromFile(photoFile);
                    runOnUiThread(() -> {
                        setLoading(false);
                        proceedToAnalysis(savedUri);
                    });
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e(TAG, "Photo capture failed", exception);
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(CameraActivity.this,
                            "Photo capture failed", Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    private void openGallery() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
            .build());
    }

    private void proceedToAnalysis(Uri imageUri) {
        Intent intent = new Intent(this, AnalysisActivity.class);
        intent.putExtra(AnalysisActivity.EXTRA_IMAGE_URI, imageUri.toString());
        if (plantId != null) {
            intent.putExtra(AnalysisActivity.EXTRA_PLANT_ID, plantId);
        }
        startActivity(intent);
        finish();
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("LeafIQ needs camera access to take plant photos for analysis. "
                + "You can enable it in Settings.")
            .setPositiveButton("Open Settings", (dialog, which) -> {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton("Cancel", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        captureButton.setEnabled(!loading);
        galleryButton.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
