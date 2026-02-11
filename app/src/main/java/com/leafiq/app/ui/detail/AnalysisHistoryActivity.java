package com.leafiq.app.ui.detail;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.leafiq.app.R;

/**
 * Full-screen analysis history list for a plant.
 * Shows all analyses chronologically with swipe-to-delete.
 *
 * Implementation stub for Phase 13 Plan 01 - will be completed in Plan 02.
 */
public class AnalysisHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_PLANT_ID = "extra_plant_id";

    private String plantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_history);

        plantId = getIntent().getStringExtra(EXTRA_PLANT_ID);
        if (plantId == null) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Analysis History");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Stub implementation - show toast
        Toast.makeText(this, "Analysis history screen - coming in plan 02", Toast.LENGTH_SHORT).show();
    }
}
