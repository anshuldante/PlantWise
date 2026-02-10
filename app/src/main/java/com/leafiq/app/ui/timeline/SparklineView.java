package com.leafiq.app.ui.timeline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.leafiq.app.R;
import com.leafiq.app.util.HealthUtils;

import java.util.List;

/**
 * Custom view that renders a sparkline showing health score trend over time.
 * <p>
 * Design decisions (from Plan 05):
 * - Non-interactive (no touch handling)
 * - Single stroke line, no axes/labels/markers/gradients/animations
 * - Canvas/Path only (NO charting libraries)
 * - Color determined by latest health status
 * - Hidden when less than 2 data points
 * <p>
 * Performance:
 * - Paint and Path allocated in constructor (NOT onDraw)
 * - Early return in onDraw if insufficient data
 */
public class SparklineView extends View {

    private static final float STROKE_WIDTH_DP = 3f;
    private static final float PADDING_DP = 8f;

    private final Paint paint;
    private final Path path;
    private List<Integer> scores;
    private int latestHealthScore;

    public SparklineView(Context context) {
        this(context, null);
    }

    public SparklineView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SparklineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Initialize Paint in constructor (not onDraw) to avoid allocations during drawing
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dpToPx(STROKE_WIDTH_DP));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        // Initialize Path in constructor
        path = new Path();
    }

    /**
     * Sets the health score data to visualize.
     * <p>
     * Hides view if less than 2 data points.
     * Color is determined from latest health score using HealthUtils.
     *
     * @param scores            List of health scores (0-10 scale), chronological order
     * @param latestHealthScore Latest health score for color determination
     */
    public void setData(List<Integer> scores, int latestHealthScore) {
        this.scores = scores;
        this.latestHealthScore = latestHealthScore;

        // Determine color from latest health status
        int colorRes = HealthUtils.getHealthColorRes(latestHealthScore);
        paint.setColor(ContextCompat.getColor(getContext(), colorRes));

        // Show/hide based on data availability
        if (scores == null || scores.size() < 2) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Default height: 48dp
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) dpToPx(48);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Early return if insufficient data
        if (scores == null || scores.size() < 2) {
            return;
        }

        // Calculate drawing area with padding
        float paddingPx = dpToPx(PADDING_DP);
        float drawWidth = getWidth() - (2 * paddingPx);
        float drawHeight = getHeight() - (2 * paddingPx);

        if (drawWidth <= 0 || drawHeight <= 0) {
            return;
        }

        // Find min/max for normalization
        int min = 0;
        int max = 10;

        // Build path with normalized scores
        path.reset();
        int numPoints = scores.size();
        float xStep = drawWidth / (numPoints - 1);

        for (int i = 0; i < numPoints; i++) {
            int score = scores.get(i);
            // Clamp score to 0-10 range
            score = Math.max(min, Math.min(max, score));

            // Calculate position
            float x = paddingPx + (i * xStep);
            // Invert Y (0 is top, higher score should be higher on screen)
            float normalizedScore = (float) (score - min) / (max - min);
            float y = paddingPx + drawHeight - (normalizedScore * drawHeight);

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        // Draw the path
        canvas.drawPath(path, paint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
