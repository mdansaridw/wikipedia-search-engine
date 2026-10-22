package com.hcl.wikisearch;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

import java.net.URI;
import java.net.URISyntaxException;

public class PreferencesManager {
    public static final String DEFAULT_CLOUD_URL = "https://wikipedia-search-engine-55an.onrender.com";
    public static final String DEFAULT_LOCAL_URL = "http://192.168.1.10:8000";

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private static final String PREFS_NAME = "wikisearch_prefs";
    private static final String KEY_BASE_URL = "key_base_url";
    private static final String KEY_CUSTOM_URL = "key_custom_url";
    private static final String KEY_THEME_MODE = "key_theme_mode";

    private static PreferencesManager sInstance;
    private final SharedPreferences mPrefs;

    private PreferencesManager(Context context) {
        mPrefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferencesManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PreferencesManager(context);
        }
        return sInstance;
    }

    public String getBaseUrl() {
        return mPrefs.getString(KEY_BASE_URL, DEFAULT_CLOUD_URL);
    }

    public void setBaseUrl(String url) {
        mPrefs.edit().putString(KEY_BASE_URL, normalizeUrl(url)).apply();
    }

    public String getCustomUrl() {
        return mPrefs.getString(KEY_CUSTOM_URL, DEFAULT_LOCAL_URL);
    }

    public void setCustomUrl(String url) {
        mPrefs.edit().putString(KEY_CUSTOM_URL, normalizeUrl(url)).apply();
    }

    public int getThemeMode() {
        return mPrefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }

    public void setThemeMode(int mode) {
        mPrefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyTheme(mode);
    }

    public void applySavedTheme() {
        applyTheme(getThemeMode());
    }

    private void applyTheme(int mode) {
        switch (mode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static String normalizeUrl(String rawUrl) {
        if (rawUrl == null) return DEFAULT_CLOUD_URL;
        String trimmed = rawUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static boolean isValidUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return false;
        }
        String normalized = normalizeUrl(rawUrl);
        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && !uri.getHost().isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
