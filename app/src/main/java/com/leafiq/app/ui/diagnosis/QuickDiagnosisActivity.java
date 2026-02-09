package com.leafiq.app.ui.diagnosis;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.leafiq.app.LeafIQApplication;
import com.leafiq.app.R;
import com.leafiq.app.ai.AIProvider;
import com.leafiq.app.ai.AIProviderFactory;
import com.leafiq.app.ai.PromptBuilder;
import com.leafiq.app.data.model.PlantAnalysisResult;
import com.leafiq.app.ui.camera.CameraActivity;
import com.leafiq.app.util.HealthUtils;
import com.leafiq.app.util.ImageUtils;
import com.leafiq.app.util.KeystoreHelper;
import com.leafiq.app.util.PhotoQualityChecker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuickDiagnosisActivity extends AppCompatActivity {

    private static final String TAG = "QuickDiagnosisActivity";

    private PreviewView cameraPreview;
    private FloatingActionButton fabCapture;
    private View loadingOverlay;
    private View resultCard;

    private ImageView diagnosisPhoto;
    private TextView healthScore;
    private TextView healthLabel;
    private TextView healthSummary;
    private LinearLayout issuesContainer;
    private LinearLayout actionsContainer;
    private MaterialButton btnFullAnalysis;
    private MaterialButton btnDismiss;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private Uri capturedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_diagnosis);

        initViews();
        setupButtons();

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    private void initViews() {
        cameraPreview = findViewById(R.id.camera_preview);
        fabCapture = findViewById(R.id.fab_capture);
        loadingOverlay = findViewById(R.id.loading_overlay);
        resultCard = findViewById(R.id.result_card);

        diagnosisPhoto = findViewById(R.id.diagnosis_photo);
        healthScore = findViewById(R.id.health_score);
        healthLabel = findViewById(R.id.health_label);
        healthSummary = findViewById(R.id.health_summary);
        issuesContainer = findViewById(R.id.issues_container);
        actionsContainer = findViewById(R.id.actions_container);
        btnFullAnalysis = findViewById(R.id.btn_full_analysis);
        btnDismiss = findViewById(R.id.btn_dismiss);
    }

    private void setupButtons() {
        fabCapture.setOnClickListener(v -> takePhoto());
        btnDismiss.setOnClickListener(v -> finish());
        btnFullAnalysis.setOnClickListener(v -> {
            // Launch full analysis flow
            startActivity(new Intent(this, CameraActivity.class));
            finish();
        });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Log.e(TAG, "Camera initialization failed", e);
                Toast.makeText(this, "Camera failed to start", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        File photoDir = new File(getFilesDir(), "temp_photos");
        if (!photoDir.exists()) {
            photoDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(System.currentTimeMillis());
        File photoFile = new File(photoDir, "QUICK_" + timestamp + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        capturedImageUri = Uri.fromFile(photoFile);
                        runOnUiThread(() -> checkQualityAndAnalyze());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed", exception);
                        runOnUiThread(() -> {
                            Toast.makeText(QuickDiagnosisActivity.this,
                                    "Photo capture failed", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void checkQualityAndAnalyze() {
        // Check photo quality first
        PhotoQualityChecker.QualityResult qualityResult =
                PhotoQualityChecker.checkQuality(getContentResolver(), capturedImageUri);

        if (!qualityResult.passed) {
            // Show quality warning dialog with "Use Anyway" / "Retake" options
            new AlertDialog.Builder(this)
                    .setTitle("Photo Quality Warning")
                    .setMessage(qualityResult.message)
                    .setPositiveButton("Use Anyway", (dialog, which) -> startAnalysis())
                    .setNegativeButton("Retake", (dialog, which) -> {
                        // User can take another photo
                    })
                    .setCancelable(false)
                    .show();
        } else {
            startAnalysis();
        }
    }

    private void startAnalysis() {
        // Hide capture button, show loading
        fabCapture.setVisibility(View.GONE);
        loadingOverlay.setVisibility(View.VISIBLE);

        // Run analysis on background thread
        LeafIQApplication app = (LeafIQApplication) getApplication();
        app.getAppExecutors().io().execute(() -> {
            try {
                // Convert image to base64
                String imageBase64 = ImageUtils.prepareForApi(this, capturedImageUri);

                // Get AI provider
                KeystoreHelper keystoreHelper = new KeystoreHelper(this);
                String providerName = keystoreHelper.getProvider();
                String apiKey = keystoreHelper.getApiKey();
                AIProvider provider = AIProviderFactory.create(providerName, apiKey);

                // Build quick diagnosis prompt
                String prompt = PromptBuilder.buildQuickDiagnosisPrompt();

                // Call AI provider
                PlantAnalysisResult result = provider.analyzePhoto(imageBase64, prompt);

                // Display results on UI thread
                runOnUiThread(() -> displayResults(result));

            } catch (Exception e) {
                Log.e(TAG, "Analysis failed", e);
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Analysis failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void displayResults(PlantAnalysisResult result) {
        // Hide loading, show result card
        loadingOverlay.setVisibility(View.GONE);
        resultCard.setVisibility(View.VISIBLE);

        // Set photo thumbnail
        Glide.with(this).load(capturedImageUri).centerCrop().into(diagnosisPhoto);

        // Set health score and label
        if (result.healthAssessment != null) {
            int score = result.healthAssessment.score;
            healthScore.setText(String.valueOf(score));

            // Set health score badge color
            int colorRes = HealthUtils.getHealthColorRes(score);
            GradientDrawable background = (GradientDrawable) healthScore.getBackground();
            background.setColor(ContextCompat.getColor(this, colorRes));

            // Set health label
            String label = HealthUtils.getHealthLabel(score);
            healthLabel.setText(label);
            healthLabel.setTextColor(ContextCompat.getColor(this, colorRes));

            // Set health summary
            if (result.healthAssessment.summary != null) {
                healthSummary.setText(result.healthAssessment.summary);
                healthSummary.setVisibility(View.VISIBLE);
            } else {
                healthSummary.setVisibility(View.GONE);
            }

            // Populate issues
            issuesContainer.removeAllViews();
            if (result.healthAssessment.issues != null && !result.healthAssessment.issues.isEmpty()) {
                for (PlantAnalysisResult.HealthAssessment.Issue issue : result.healthAssessment.issues) {
                    View issueView = getLayoutInflater().inflate(
                            android.R.layout.simple_list_item_2, issuesContainer, false);
                    TextView text1 = issueView.findViewById(android.R.id.text1);
                    TextView text2 = issueView.findViewById(android.R.id.text2);

                    text1.setText(issue.name + " (" + issue.severity + ")");
                    text1.setTextSize(16);
                    text1.setTextColor(ContextCompat.getColor(this,
                            getSeverityColor(issue.severity)));

                    text2.setText(issue.description);
                    text2.setTextSize(14);

                    issuesContainer.addView(issueView);
                }
            } else {
                TextView noIssues = new TextView(this);
                noIssues.setText("No issues detected");
                noIssues.setTextSize(14);
                noIssues.setPadding(0, 8, 0, 8);
                issuesContainer.addView(noIssues);
            }
        }

        // Populate actions
        actionsContainer.removeAllViews();
        if (result.immediateActions != null && !result.immediateActions.isEmpty()) {
            for (PlantAnalysisResult.ImmediateAction action : result.immediateActions) {
                View actionView = getLayoutInflater().inflate(
                        android.R.layout.simple_list_item_2, actionsContainer, false);
                TextView text1 = actionView.findViewById(android.R.id.text1);
                TextView text2 = actionView.findViewById(android.R.id.text2);

                text1.setText(action.action + " (" + action.priority + ")");
                text1.setTextSize(16);
                text1.setTextColor(ContextCompat.getColor(this,
                        getPriorityColor(action.priority)));

                text2.setText(action.detail);
                text2.setTextSize(14);

                actionsContainer.addView(actionView);
            }
        } else {
            TextView noActions = new TextView(this);
            noActions.setText("No immediate actions needed");
            noActions.setTextSize(14);
            noActions.setPadding(0, 8, 0, 8);
            actionsContainer.addView(noActions);
        }
    }

    private int getSeverityColor(String severity) {
        if (severity == null) return R.color.health_warning;
        switch (severity.toLowerCase()) {
            case "high":
                return R.color.health_bad;
            case "medium":
                return R.color.health_warning;
            case "low":
            default:
                return R.color.health_good;
        }
    }

    private int getPriorityColor(String priority) {
        if (priority == null) return R.color.health_warning;
        switch (priority.toLowerCase()) {
            case "urgent":
                return R.color.health_bad;
            case "soon":
                return R.color.health_warning;
            case "when_convenient":
            default:
                return R.color.health_good;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
