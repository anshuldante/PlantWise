package com.anshul.plantwise.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.anshul.plantwise.R;
import com.anshul.plantwise.util.KeystoreHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment {

    private RadioGroup providerGroup;
    private MaterialRadioButton radioGemini;
    private MaterialRadioButton radioOpenAI;
    private MaterialRadioButton radioClaude;
    private MaterialRadioButton radioPerplexity;
    private TextInputEditText apiKeyEdit;
    private MaterialButton saveButton;
    private TextView statusText;
    private TextView apiKeyInfo;
    private KeystoreHelper keystoreHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        keystoreHelper = new KeystoreHelper(requireContext());

        providerGroup = view.findViewById(R.id.radio_provider);
        radioGemini = view.findViewById(R.id.radio_gemini);
        radioOpenAI = view.findViewById(R.id.radio_openai);
        radioClaude = view.findViewById(R.id.radio_claude);
        radioPerplexity = view.findViewById(R.id.radio_perplexity);
        apiKeyEdit = view.findViewById(R.id.edit_api_key);
        saveButton = view.findViewById(R.id.btn_save_key);
        statusText = view.findViewById(R.id.api_key_status);
        apiKeyInfo = view.findViewById(R.id.api_key_info);

        // Set current provider selection
        String currentProvider = keystoreHelper.getProvider();
        if (KeystoreHelper.PROVIDER_CLAUDE.equals(currentProvider)) {
            radioClaude.setChecked(true);
        } else if (KeystoreHelper.PROVIDER_PERPLEXITY.equals(currentProvider)) {
            radioPerplexity.setChecked(true);
        } else if (KeystoreHelper.PROVIDER_OPENAI.equals(currentProvider)) {
            radioOpenAI.setChecked(true);
        } else {
            radioGemini.setChecked(true);
        }

        updateInfoText();
        updateStatus();

        providerGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateInfoText();
        });

        saveButton.setOnClickListener(v -> saveSettings());
    }

    private void updateInfoText() {
        int checkedId = providerGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_gemini) {
            apiKeyInfo.setText(R.string.api_key_info_gemini);
        } else if (checkedId == R.id.radio_claude) {
            apiKeyInfo.setText(R.string.api_key_info_claude);
        } else if (checkedId == R.id.radio_perplexity) {
            apiKeyInfo.setText(R.string.api_key_info_perplexity);
        } else {
            apiKeyInfo.setText(R.string.api_key_info_openai);
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
        } else if (KeystoreHelper.PROVIDER_PERPLEXITY.equals(provider)) {
            return "Perplexity";
        } else {
            return "ChatGPT";
        }
    }

    private void saveSettings() {
        // Save provider
        int checkedId = providerGroup.getCheckedRadioButtonId();
        String provider;
        if (checkedId == R.id.radio_gemini) {
            provider = KeystoreHelper.PROVIDER_GEMINI;
        } else if (checkedId == R.id.radio_claude) {
            provider = KeystoreHelper.PROVIDER_CLAUDE;
        } else if (checkedId == R.id.radio_perplexity) {
            provider = KeystoreHelper.PROVIDER_PERPLEXITY;
        } else {
            provider = KeystoreHelper.PROVIDER_OPENAI;
        }
        keystoreHelper.saveProvider(provider);

        // Save API key if provided
        String key = apiKeyEdit.getText() != null ? apiKeyEdit.getText().toString().trim() : "";
        if (!key.isEmpty()) {
            keystoreHelper.saveApiKey(key);
            apiKeyEdit.setText("");
        }

        Toast.makeText(requireContext(), R.string.api_key_saved, Toast.LENGTH_SHORT).show();
        updateStatus();
    }
}
