package com.hcl.wikisearch;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.hcl.wikisearch.models.ArticleData;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ArticleReaderActivity extends AppCompatActivity {

    public static final String EXTRA_DOC_ID = "extra_doc_id";
    public static final String EXTRA_INITIAL_TITLE = "extra_initial_title";

    private long mDocId;
    private String mArticleUrl;

    private NestedScrollView mScrollArticle;
    private LinearLayout mLayoutLoading;
    private LinearLayout mLayoutError;

    private TextView mTextTitle;
    private TextView mTextReadingTime;
    private TextView mTextDocId;
    private TextView mTextBody;
    private TextView mTextErrorMessage;
    private MaterialButton mBtnOpenBrowser;
    private MaterialButton mBtnRetry;

    public static Intent createIntent(Context context, long docId, String title) {
        Intent intent = new Intent(context, ArticleReaderActivity.class);
        intent.putExtra(EXTRA_DOC_ID, docId);
        intent.putExtra(EXTRA_INITIAL_TITLE, title);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_reader);

        mDocId = getIntent().getLongExtra(EXTRA_DOC_ID, 0);
        String initialTitle = getIntent().getStringExtra(EXTRA_INITIAL_TITLE);

        Toolbar toolbar = findViewById(R.id.toolbar_article);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        mScrollArticle = findViewById(R.id.scroll_article);
        mLayoutLoading = findViewById(R.id.layout_loading);
        mLayoutError = findViewById(R.id.layout_error);

        mTextTitle = findViewById(R.id.text_article_title);
        mTextReadingTime = findViewById(R.id.text_article_reading_time);
        mTextDocId = findViewById(R.id.text_article_doc_id);
        mTextBody = findViewById(R.id.text_article_body);
        mTextErrorMessage = findViewById(R.id.text_error_message);
        mBtnOpenBrowser = findViewById(R.id.btn_open_browser);
        mBtnRetry = findViewById(R.id.btn_retry);

        if (initialTitle != null && !initialTitle.isEmpty()) {
            mTextTitle.setText(initialTitle);
        }
        mTextDocId.setText(String.format(Locale.getDefault(), "Doc ID: #%d", mDocId));

        mBtnOpenBrowser.setOnClickListener(v -> openArticleInBrowser());
        mBtnRetry.setOnClickListener(v -> loadArticle());

        loadArticle();
    }

    private void loadArticle() {
        showLoading();

        ApiClient.getInstance(this).getArticle(mDocId, new ApiClient.ApiCallback<ArticleData>() {
            @Override
            public void onSuccess(ArticleData article) {
                if (isFinishing() || isDestroyed()) return;
                showContent();
                displayArticle(article);
            }

            @Override
            public void onError(String message, Throwable throwable) {
                if (isFinishing() || isDestroyed()) return;
                showError(message);
            }
        });
    }

    private void displayArticle(ArticleData article) {
        if (article == null) {
            showError("No content returned for this article.");
            return;
        }

        mTextTitle.setText(article.getTitle());
        mTextDocId.setText(String.format(Locale.getDefault(), "Doc ID: #%d", article.getDocId()));
        mTextReadingTime.setText(String.format(Locale.getDefault(), "%d min read · %,d words",
                article.getReadingTimeMinutes(), article.getWordCount()));

        mTextBody.setText(article.getText());
        mArticleUrl = article.getUrl();

        // If no URL returned, prepare Wikipedia Simple fallback URL
        if (mArticleUrl == null || mArticleUrl.isEmpty()) {
            try {
                String encodedTitle = URLEncoder.encode(article.getTitle().replace(' ', '_'), StandardCharsets.UTF_8.name());
                mArticleUrl = "https://simple.wikipedia.org/wiki/" + encodedTitle;
            } catch (Exception ignored) {}
        }
    }

    private void openArticleInBrowser() {
        if (mArticleUrl == null || mArticleUrl.isEmpty()) {
            Toast.makeText(this, "No article URL available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Security requirement: Validate URI scheme (http/https only) and host
        try {
            URI uri = new URI(mArticleUrl);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                Toast.makeText(this, "Untrusted URL scheme", Toast.LENGTH_SHORT).show();
                return;
            }
            if (uri.getHost() == null || uri.getHost().isEmpty()) {
                Toast.makeText(this, "Invalid article URL", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mArticleUrl));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading() {
        mLayoutLoading.setVisibility(View.VISIBLE);
        mLayoutError.setVisibility(View.GONE);
        mScrollArticle.setVisibility(View.GONE);
    }

    private void showContent() {
        mLayoutLoading.setVisibility(View.GONE);
        mLayoutError.setVisibility(View.GONE);
        mScrollArticle.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        mLayoutLoading.setVisibility(View.GONE);
        mLayoutError.setVisibility(View.VISIBLE);
        mScrollArticle.setVisibility(View.GONE);
        mTextErrorMessage.setText(message != null ? message : "Failed to load article");
    }
}
