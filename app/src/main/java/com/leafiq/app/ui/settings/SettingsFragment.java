package com.leafiq.app.ui.settings;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.leafiq.app.R;
import com.leafiq.app.util.KeystoreHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private RadioGroup providerGroup;
    private MaterialRadioButton radioGemini;
    private MaterialRadioButton radioOpenAI;
    private MaterialRadioButton radioClaude;
    private TextInputEditText apiKeyEdit;
    private MaterialButton saveButton;
    private TextView statusText;
    private TextView apiKeyInfo;
    private LinearLayout reminderTimeRow;
    private TextView reminderTimeValue;
    private SwitchMaterial pauseRemindersSwitch;
    private KeystoreHelper keystoreHelper;
    private View encryptionErrorBanner;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        keystoreHelper = new KeystoreHelper(requireContext());

        encryptionErrorBanner = view.findViewById(R.id.encryption_error_banner);

        providerGroup = view.findViewById(R.id.radio_provider);
        radioGemini = view.findViewById(R.id.radio_gemini);
        radioOpenAI = view.findViewById(R.id.radio_openai);
        radioClaude = view.findViewById(R.id.radio_claude);
        apiKeyEdit = view.findViewById(R.id.edit_api_key);
        saveButton = view.findViewById(R.id.btn_save_key);
        statusText = view.findViewById(R.id.api_key_status);
        apiKeyInfo = view.findViewById(R.id.api_key_info);
        reminderTimeRow = view.findViewById(R.id.reminder_time_row);
        reminderTimeValue = view.findViewById(R.id.reminder_time_value);
        pauseRemindersSwitch = view.findViewById(R.id.pause_reminders_switch);

        // Set current provider selection
        String currentProvider = keystoreHelper.getProvider();
        if (KeystoreHelper.PROVIDER_CLAUDE.equals(currentProvider)) {
            radioClaude.setChecked(true);
        } else if (KeystoreHelper.PROVIDER_OPENAI.equals(currentProvider)) {
            radioOpenAI.setChecked(true);
        } else {
            radioGemini.setChecked(true);
        }

        updateInfoText();
        updateStatus();
        updateReminderSettings();

        // Check encryption health and show banner if unhealthy
        if (!keystoreHelper.isEncryptionHealthy()) {
            encryptionErrorBanner.setVisibility(View.VISIBLE);
            saveButton.setEnabled(false);
            apiKeyEdit.setEnabled(false);
        }

        // Set up dismiss button for encryption error banner
        view.findViewById(R.id.btn_dismiss_encryption_banner).setOnClickListener(v -> {
            encryptionErrorBanner.setVisibility(View.GONE);
        });

        providerGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateInfoText();
            updateStatusForSelectedProvider();
        });

        saveButton.setOnClickListener(v -> saveSettings());

        // Reminder time picker
        reminderTimeRow.setOnClickListener(v -> showTimePicker());

        // Pause reminders switch
        pauseRemindersSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            keystoreHelper.setRemindersPaused(isChecked);
        });
    }

    private void updateReminderSettings() {
        // Load and display preferred reminder time
        int[] time = keystoreHelper.getPreferredReminderTime();
        reminderTimeValue.setText(formatTime(time[0], time[1]));

        // Load pause state
        pauseRemindersSwitch.setChecked(keystoreHelper.areRemindersPaused());
    }

    private void showTimePicker() {
        int[] currentTime = keystoreHelper.getPreferredReminderTime();
        TimePickerDialog picker = new TimePickerDialog(
            requireContext(),
            (view, hourOfDay, minute) -> {
                keystoreHelper.savePreferredReminderTime(hourOfDay, minute);
                reminderTimeValue.setText(formatTime(hourOfDay, minute));
            },
            currentTime[0],
            currentTime[1],
            false // Use 12-hour format by default (system setting can override)
        );
        picker.show();
    }

    private String formatTime(int hour, int minute) {
        // Format time as 12-hour with AM/PM
        String period = "AM";
        int displayHour = hour;

        if (hour == 0) {
            displayHour = 12; // Midnight
        } else if (hour == 12) {
            period = "PM"; // Noon
        } else if (hour > 12) {
            displayHour = hour - 12;
            period = "PM";
        }

        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, period);
    }

    private void updateInfoText() {
        int checkedId = providerGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_gemini) {
            apiKeyInfo.setText(R.string.api_key_info_gemini);
        } else if (checkedId == R.id.radio_claude) {
            apiKeyInfo.setText(R.string.api_key_info_claude);
        } else {
            apiKeyInfo.setText(R.string.api_key_info_openai);
        }
    }

    private void updateStatusForSelectedProvider() {
        String provider = getSelectedProvider();
        String providerName = getProviderDisplayName(provider);

        if (keystoreHelper.hasApiKeyForProvider(provider)) {
            statusText.setText(providerName + " - API key configured");
        } else {
            statusText.setText(providerName + " - No API key configured");
        }
        // Clear the hint since we're looking at a potentially different provider
        apiKeyEdit.setHint(R.string.enter_api_key);
    }

    private String getSelectedProvider() {
        int checkedId = providerGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_gemini) {
            return KeystoreHelper.PROVIDER_GEMINI;
        } else if (checkedId == R.id.radio_claude) {
            return KeystoreHelper.PROVIDER_CLAUDE;
        } else {
            return KeystoreHelper.PROVIDER_OPENAI;
        }
    }

    private void updateStatus() {
        String provider = keystoreHelper.getProvider();
        String providerName = getProviderDisplayName(provider);

        if (keystoreHelper.hasApiKey()) {
            statusText.setText("Using " + providerName + " - API key configured");
            // Show masked key hint
            String key = keystoreHelper.getApiKey();
            if (key != null && key.length() > 8) {
                apiKeyEdit.setHint("..." + key.substring(key.length() - 4));
            }
        } else {
            statusText.setText("No API key configured");
        }
    }

    private String getProviderDisplayName(String provider) {
        if (KeystoreHelper.PROVIDER_GEMINI.equals(provider)) {
            return "Gemini";
        } else if (KeystoreHelper.PROVIDER_CLAUDE.equals(provider)) {
            return "Claude";
        } else {
            return "ChatGPT";
        }
    }

    private void saveSettings() {
        String provider = getSelectedProvider();
        keystoreHelper.saveProvider(provider);

        // Save API key if provided
        String key = apiKeyEdit.getText() != null ? apiKeyEdit.getText().toString().trim() : "";
        if (!key.isEmpty()) {
            try {
                keystoreHelper.saveApiKey(key);
                apiKeyEdit.setText("");
            } catch (IllegalStateException e) {
                Toast.makeText(requireContext(), R.string.secure_storage_unavailable, Toast.LENGTH_LONG).show();
                return;
            }
        }

        Toast.makeText(requireContext(), R.string.api_key_saved, Toast.LENGTH_SHORT).show();
        updateStatus();
    }
}
