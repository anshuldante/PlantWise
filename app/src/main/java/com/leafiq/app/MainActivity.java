package com.leafiq.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.leafiq.app.ai.AIProvider;
import com.leafiq.app.ai.AIProviderFactory;
import com.leafiq.app.ui.camera.CameraActivity;

import okhttp3.OkHttpClient;
import com.leafiq.app.ui.care.CareOverviewActivity;
import com.leafiq.app.ui.diagnosis.QuickDiagnosisActivity;
import com.leafiq.app.ui.library.LibraryFragment;
import com.leafiq.app.ui.settings.SettingsFragment;
import com.leafiq.app.ui.timeline.TimelineFragment;
import com.leafiq.app.util.KeystoreHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if database migration failed during app startup
        LeafIQApplication app = (LeafIQApplication) getApplication();
        if (app.isMigrationFailed()) {
            LeafIQApplication.showMigrationErrorAndExit(this);
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNav = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();
        setupLongPressGesture();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new LibraryFragment());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            // Navigate to Settings (don't change bottom nav selection)
            loadFragment(new SettingsFragment());
            return true;
        } else if (itemId == R.id.action_care_overview) {
            // Open Care Overview screen
            startActivity(new Intent(this, CareOverviewActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset to library tab when returning from camera/analysis
        if (bottomNav.getSelectedItemId() == R.id.nav_camera) {
            bottomNav.setSelectedItemId(R.id.nav_library);
        }
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_library) {
                loadFragment(new LibraryFragment());
                return true;
            } else if (itemId == R.id.nav_camera) {
                startActivity(new Intent(this, CameraActivity.class));
                return false; // Don't select camera tab, it's an activity
            } else if (itemId == R.id.nav_timeline) {
                loadFragment(new TimelineFragment());
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }

    private void setupLongPressGesture() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                // Get camera nav item bounds
                View cameraNavItem = bottomNav.findViewById(R.id.nav_camera);
                if (cameraNavItem == null) return;

                int[] location = new int[2];
                cameraNavItem.getLocationOnScreen(location);
                int left = location[0];
                int top = location[1];
                int right = left + cameraNavItem.getWidth();
                int bottom = top + cameraNavItem.getHeight();

                // Check if long-press was within camera item bounds
                float x = e.getRawX();
                float y = e.getRawY();
                if (x >= left && x <= right && y >= top && y <= bottom) {
                    startQuickDiagnosis();
                }
            }
        });

        bottomNav.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // Allow normal tap handling
        });
    }

    private void startQuickDiagnosis() {
        // Check if current provider supports vision
        KeystoreHelper keystoreHelper = new KeystoreHelper(this);
        String providerName = keystoreHelper.getProvider();
        String apiKey = keystoreHelper.getApiKey();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            Toast.makeText(this, R.string.quick_diagnosis_provider_required, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            OkHttpClient client = ((LeafIQApplication) getApplication()).getHttpClient();
            AIProvider provider = AIProviderFactory.create(providerName, apiKey, client);
            if (!provider.supportsVision()) {
                Toast.makeText(this, R.string.quick_diagnosis_provider_required, Toast.LENGTH_LONG).show();
                return;
            }

            // Launch Quick Diagnosis
            startActivity(new Intent(this, QuickDiagnosisActivity.class));
        } catch (Exception e) {
            Toast.makeText(this, R.string.quick_diagnosis_provider_required, Toast.LENGTH_LONG).show();
        }
    }
}
