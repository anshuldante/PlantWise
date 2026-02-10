package com.leafiq.app.util;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Shared utility for applying system bar WindowInsets as padding.
 * Used for edge-to-edge layout support across all scrollable screens.
 */
public class WindowInsetsHelper {

    /**
     * Apply system bar bottom insets as padding to a view.
     * Preserves existing top padding. Sets left, right, and bottom from system bars.
     * Returns CONSUMED to prevent double-application in view hierarchy.
     *
     * Use for: ScrollView, NestedScrollView, RecyclerView content areas.
     */
    public static void applyBottomInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                insets.left,
                v.getPaddingTop(),
                insets.right,
                insets.bottom
            );
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * Apply system bar bottom insets as additional padding on top of existing padding.
     * Useful when the view already has its own padding (e.g., RecyclerView with 8dp padding).
     *
     * @param view The view to apply insets to
     * @param existingBottomPadding The view's intended bottom padding in pixels (added to inset)
     */
    public static void applyBottomInsetsWithPadding(View view, int existingBottomPadding) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                v.getPaddingLeft(),
                v.getPaddingTop(),
                v.getPaddingRight(),
                insets.bottom + existingBottomPadding
            );
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
