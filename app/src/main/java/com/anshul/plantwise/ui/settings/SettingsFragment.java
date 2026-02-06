package com.anshul.plantwise.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.anshul.plantwise.R;
import com.anshul.plantwise.util.KeystoreHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment {

    private TextInputEditText apiKeyEdit;
    private MaterialButton saveButton;
    private TextView statusText;
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

        apiKeyEdit = view.findViewById(R.id.edit_api_key);
        saveButton = view.findViewById(R.id.btn_save_key);
        statusText = view.findViewById(R.id.api_key_status);

        updateStatus();

        saveButton.setOnClickListener(v -> saveApiKey());
    }

    private void updateStatus() {
        if (keystoreHelper.hasApiKey()) {
            statusText.setText("API key is configured");
            // Show masked key hint
            String key = keystoreHelper.getApiKey();
            if (key != null && key.length() > 8) {
                apiKeyEdit.setHint("sk-..." + key.substring(key.length() - 4));
            }
        } else {
            statusText.setText("No API key configured");
        }
    }

    private void saveApiKey() {
        String key = apiKeyEdit.getText() != null ? apiKeyEdit.getText().toString().trim() : "";
        if (key.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an API key", Toast.LENGTH_SHORT).show();
            return;
        }

        keystoreHelper.saveApiKey(key);
        apiKeyEdit.setText("");
        Toast.makeText(requireContext(), R.string.api_key_saved, Toast.LENGTH_SHORT).show();
        updateStatus();
    }
}
