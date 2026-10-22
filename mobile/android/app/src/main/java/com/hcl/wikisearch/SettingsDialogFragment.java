package com.hcl.wikisearch;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hcl.wikisearch.models.StatsData;

import java.util.Locale;

public class SettingsDialogFragment extends DialogFragment {

    public static final String TAG = "SettingsDialogFragment";

    public interface OnSettingsSavedListener {
        void onSettingsSaved();
    }

    private RadioGroup mGroupBackend;
    private RadioButton mRadioCloud;
    private RadioButton mRadioLocal;
    private TextInputLayout mLayoutCustomUrl;
    private TextInputEditText mInputCustomUrl;
    private MaterialButton mBtnTestConnection;
    private TextView mTextTestStatus;

    private RadioGroup mGroupTheme;
    private RadioButton mRadioThemeSystem;
    private RadioButton mRadioThemeLight;
    private RadioButton mRadioThemeDark;

    private MaterialButton mBtnCancel;
    private MaterialButton mBtnSave;

    private OnSettingsSavedListener mListener;

    public static SettingsDialogFragment newInstance() {
        return new SettingsDialogFragment();
    }

    public void setOnSettingsSavedListener(OnSettingsSavedListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mGroupBackend = view.findViewById(R.id.group_backend);
        mRadioCloud = view.findViewById(R.id.radio_backend_cloud);
        mRadioLocal = view.findViewById(R.id.radio_backend_local);
        mLayoutCustomUrl = view.findViewById(R.id.layout_input_custom_url);
        mInputCustomUrl = view.findViewById(R.id.input_custom_url);
        mBtnTestConnection = view.findViewById(R.id.btn_test_connection);
        mTextTestStatus = view.findViewById(R.id.text_test_status);

        mGroupTheme = view.findViewById(R.id.group_theme);
        mRadioThemeSystem = view.findViewById(R.id.radio_theme_system);
        mRadioThemeLight = view.findViewById(R.id.radio_theme_light);
        mRadioThemeDark = view.findViewById(R.id.radio_theme_dark);

        mBtnCancel = view.findViewById(R.id.btn_settings_cancel);
        mBtnSave = view.findViewById(R.id.btn_settings_save);

        loadCurrentSettings();
        setupListeners();
    }

    private void loadCurrentSettings() {
        Context context = requireContext();
        PreferencesManager prefs = PreferencesManager.getInstance(context);

        String currentBaseUrl = prefs.getBaseUrl();
        String customUrl = prefs.getCustomUrl();

        mInputCustomUrl.setText(customUrl);

        if (PreferencesManager.DEFAULT_CLOUD_URL.equalsIgnoreCase(currentBaseUrl)) {
            mRadioCloud.setChecked(true);
            mLayoutCustomUrl.setEnabled(false);
        } else {
            mRadioLocal.setChecked(true);
            mLayoutCustomUrl.setEnabled(true);
        }

        int theme = prefs.getThemeMode();
        switch (theme) {
            case PreferencesManager.THEME_LIGHT:
                mRadioThemeLight.setChecked(true);
                break;
            case PreferencesManager.THEME_DARK:
                mRadioThemeDark.setChecked(true);
                break;
            case PreferencesManager.THEME_SYSTEM:
            default:
                mRadioThemeSystem.setChecked(true);
                break;
        }
    }

    private void setupListeners() {
        mGroupBackend.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isLocal = checkedId == R.id.radio_backend_local;
            mLayoutCustomUrl.setEnabled(isLocal);
            if (isLocal) {
                mInputCustomUrl.requestFocus();
            }
        });

        mBtnTestConnection.setOnClickListener(v -> testConnection());

        mBtnCancel.setOnClickListener(v -> dismiss());

        mBtnSave.setOnClickListener(v -> saveSettings());
    }

    private void testConnection() {
        String testUrl;
        if (mRadioCloud.isChecked()) {
            testUrl = PreferencesManager.DEFAULT_CLOUD_URL;
        } else {
            testUrl = mInputCustomUrl.getText() != null ? mInputCustomUrl.getText().toString().trim() : "";
            if (!PreferencesManager.isValidUrl(testUrl)) {
                mTextTestStatus.setText(R.string.status_error);
                Toast.makeText(requireContext(), "Please enter a valid HTTP/HTTPS URL", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        mTextTestStatus.setText(R.string.testing_connection);
        long startTime = System.currentTimeMillis();

        ApiClient.getInstance(requireContext()).testConnection(testUrl, new ApiClient.ApiCallback<StatsData>() {
            @Override
            public void onSuccess(StatsData result) {
                if (!isAdded()) return;
                long elapsed = System.currentTimeMillis() - startTime;
                mTextTestStatus.setText(String.format(Locale.getDefault(), getString(R.string.test_success), (double) elapsed));
            }

            @Override
            public void onError(String message, Throwable throwable) {
                if (!isAdded()) return;
                mTextTestStatus.setText(String.format(Locale.getDefault(), getString(R.string.test_failure), message));
            }
        });
    }

    private void saveSettings() {
        Context context = requireContext();
        PreferencesManager prefs = PreferencesManager.getInstance(context);

        String newUrl;
        if (mRadioCloud.isChecked()) {
            newUrl = PreferencesManager.DEFAULT_CLOUD_URL;
        } else {
            newUrl = mInputCustomUrl.getText() != null ? mInputCustomUrl.getText().toString().trim() : "";
            if (!PreferencesManager.isValidUrl(newUrl)) {
                Toast.makeText(context, "Invalid URL. Must start with http:// or https://", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.setCustomUrl(newUrl);
        }
        prefs.setBaseUrl(newUrl);

        int newTheme = PreferencesManager.THEME_SYSTEM;
        if (mRadioThemeLight.isChecked()) {
            newTheme = PreferencesManager.THEME_LIGHT;
        } else if (mRadioThemeDark.isChecked()) {
            newTheme = PreferencesManager.THEME_DARK;
        }
        prefs.setThemeMode(newTheme);

        if (mListener != null) {
            mListener.onSettingsSaved();
        }

        dismiss();
    }
}
