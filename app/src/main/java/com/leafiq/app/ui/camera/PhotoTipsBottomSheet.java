package com.leafiq.app.ui.camera;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.leafiq.app.R;

/**
 * Bottom sheet that displays photo tips for better analysis results.
 * Shows contextual highlighting based on quality failure reason.
 */
public class PhotoTipsBottomSheet extends BottomSheetDialogFragment {

    /**
     * Listener interface for when user dismisses tips by clicking "Got It".
     */
    public interface OnTipsDismissedListener {
        void onTipsDismissed();
    }

    private static final String ARG_FAILURE_REASON = "failure_reason";
    private OnTipsDismissedListener listener;

    /**
     * Create a new instance of PhotoTipsBottomSheet.
     * @param failureReason Quality failure type for contextual highlighting, or null for first-time tips
     * @return New instance of PhotoTipsBottomSheet
     */
    public static PhotoTipsBottomSheet newInstance(String failureReason) {
        PhotoTipsBottomSheet sheet = new PhotoTipsBottomSheet();
        Bundle args = new Bundle();
        if (failureReason != null) {
            args.putString(ARG_FAILURE_REASON, failureReason);
        }
        sheet.setArguments(args);
        return sheet;
    }

    /**
     * Set listener to be notified when user clicks "Got It".
     * @param listener Listener to notify
     */
    public void setOnTipsDismissedListener(OnTipsDismissedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_photo_tips, container, false);

        String failureReason = getArguments() != null ? getArguments().getString(ARG_FAILURE_REASON) : null;

        // Apply contextual highlighting if quality failure
        if (failureReason != null) {
            applyContextualHighlighting(view, failureReason);
        }

        MaterialButton gotItButton = view.findViewById(R.id.btn_got_it);
        gotItButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTipsDismissed();
            }
            dismiss();
        });

        return view;
    }

    /**
     * Apply contextual highlighting based on quality failure reason.
     * Bolds the specific tip that matches the quality failure.
     * @param view Root view of the bottom sheet
     * @param reason Quality failure type: "blur", "dark", "bright", or "resolution"
     */
    private void applyContextualHighlighting(View view, String reason) {
        // Failure reasons from PhotoQualityChecker.QualityResult.issueType:
        // "blur", "dark", "bright", "resolution"
        TextView tipLighting = view.findViewById(R.id.tip_lighting_text);
        TextView tipBlur = view.findViewById(R.id.tip_blur_text);
        TextView tipFraming = view.findViewById(R.id.tip_framing_text);

        if ("blur".equals(reason)) {
            tipBlur.setTypeface(tipBlur.getTypeface(), Typeface.BOLD);
        }
        if ("dark".equals(reason) || "bright".equals(reason)) {
            tipLighting.setTypeface(tipLighting.getTypeface(), Typeface.BOLD);
        }
        if ("resolution".equals(reason)) {
            tipFraming.setTypeface(tipFraming.getTypeface(), Typeface.BOLD);
        }
    }
}
