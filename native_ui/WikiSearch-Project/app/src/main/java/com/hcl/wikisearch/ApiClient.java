package com.hcl.wikisearch;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.hcl.wikisearch.models.ArticleData;
import com.hcl.wikisearch.models.ExplainData;
import com.hcl.wikisearch.models.SearchResponse;
import com.hcl.wikisearch.models.StatsData;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class ApiClient {
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String message, Throwable throwable);
    }

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 25000;

    private static ApiClient sInstance;
    private final Context mContext;
    private final ExecutorService mExecutor;
    private final Handler mMainHandler;
    private final AtomicLong mSearchSequence = new AtomicLong(0);

    private ApiClient(Context context) {
        mContext = context.getApplicationContext();
        mExecutor = Executors.newFixedThreadPool(4);
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ApiClient(context);
        }
        return sInstance;
    }

    private String getBaseUrl() {
        return PreferencesManager.getInstance(mContext).getBaseUrl();
    }

    /**
     * Search endpoint with sequence-id tracking to suppress stale responses.
     */
    public long search(String query, String mode, int limit, ApiCallback<SearchResponse> callback) {
        final long requestId = mSearchSequence.incrementAndGet();
        mExecutor.execute(() -> {
            try {
                String encodedQ = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
                String encodedMode = URLEncoder.encode(mode, StandardCharsets.UTF_8.name());
                String urlStr = getBaseUrl() + "/api/search?q=" + encodedQ + "&mode=" + encodedMode + "&limit=" + limit;

                String jsonStr = executeHttpGet(urlStr);
                JSONObject json = new JSONObject(jsonStr);
                SearchResponse response = SearchResponse.fromJson(json);

                mMainHandler.post(() -> {
                    // Check if a newer search request has already superseded this one
                    if (requestId == mSearchSequence.get()) {
                        callback.onSuccess(response);
                    }
                });
            } catch (Exception e) {
                mMainHandler.post(() -> {
                    if (requestId == mSearchSequence.get()) {
                        callback.onError(formatError(e), e);
                    }
                });
            }
        });
        return requestId;
    }

    /**
     * 8-Step Engine Inspector explanation endpoint.
     */
    public void explain(String query, String mode, ApiCallback<ExplainData> callback) {
        mExecutor.execute(() -> {
            try {
                String encodedQ = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
                String encodedMode = URLEncoder.encode(mode, StandardCharsets.UTF_8.name());
                String urlStr = getBaseUrl() + "/api/explain?q=" + encodedQ + "&mode=" + encodedMode;

                String jsonStr = executeHttpGet(urlStr);
                JSONObject json = new JSONObject(jsonStr);
                ExplainData response = ExplainData.fromJson(json);

                mMainHandler.post(() -> callback.onSuccess(response));
            } catch (Exception e) {
                mMainHandler.post(() -> callback.onError(formatError(e), e));
            }
        });
    }

    /**
     * Article Reader endpoint.
     */
    public void getArticle(long docId, ApiCallback<ArticleData> callback) {
        mExecutor.execute(() -> {
            try {
                String urlStr = getBaseUrl() + "/api/article/" + docId;
                String jsonStr = executeHttpGet(urlStr);
                JSONObject json = new JSONObject(jsonStr);
                ArticleData response = ArticleData.fromJson(json);

                mMainHandler.post(() -> callback.onSuccess(response));
            } catch (Exception e) {
                mMainHandler.post(() -> callback.onError(formatError(e), e));
            }
        });
    }

    /**
     * Search Engine statistics endpoint.
     */
    public void getStats(ApiCallback<StatsData> callback) {
        mExecutor.execute(() -> {
            try {
                String urlStr = getBaseUrl() + "/api/stats";
                String jsonStr = executeHttpGet(urlStr);
                JSONObject json = new JSONObject(jsonStr);
                StatsData response = StatsData.fromJson(json);

                mMainHandler.post(() -> callback.onSuccess(response));
            } catch (Exception e) {
                mMainHandler.post(() -> callback.onError(formatError(e), e));
            }
        });
    }

    /**
     * Test connection to a specific base URL.
     */
    public void testConnection(String targetBaseUrl, ApiCallback<StatsData> callback) {
        mExecutor.execute(() -> {
            try {
                String normalized = PreferencesManager.normalizeUrl(targetBaseUrl);
                String urlStr = normalized + "/api/stats";
                String jsonStr = executeHttpGet(urlStr);
                JSONObject json = new JSONObject(jsonStr);
                StatsData response = StatsData.fromJson(json);

                mMainHandler.post(() -> callback.onSuccess(response));
            } catch (Exception e) {
                mMainHandler.post(() -> callback.onError(formatError(e), e));
            }
        });
    }

    private String executeHttpGet(String urlString) throws Exception {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "WikiSearch-Android/1.0");

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if (is == null) {
                throw new Exception("HTTP " + responseCode + " with empty response");
            }

            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }

            if (responseCode < 200 || responseCode >= 300) {
                throw new Exception("Server returned HTTP " + responseCode + ": " + sb.toString().trim());
            }

            return sb.toString();
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception ignored) {}
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String formatError(Exception e) {
        if (e == null) return "Unknown error occurred";
        String msg = e.getMessage();
        if (msg != null && !msg.isEmpty()) {
            return msg;
        }
        return e.getClass().getSimpleName();
    }
}
