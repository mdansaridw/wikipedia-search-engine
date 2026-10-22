package com.hcl.wikisearch;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.hcl.wikisearch.models.ExplainData;
import com.hcl.wikisearch.models.SearchResponse;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String MODE_KEYWORD = "keyword";
    private static final String MODE_PHRASE = "phrase";

    private String mCurrentMode = MODE_KEYWORD;
    private String mCurrentQuery = "";
    private int mSelectedTab = 0; // 0 = Results, 1 = Inspector

    // Header & Controls
    private TextView mTextEngineStatus;
    private ImageButton mBtnStats;
    private ImageButton mBtnSettings;
    private EditText mEditSearch;
    private ImageButton mBtnClearSearch;
    private ProgressBar mProgressSearchBar;
    private TextView mBtnModeKeyword;
    private TextView mBtnModePhrase;
    private LinearLayout mLayoutSearchSummary;
    private TextView mTextSearchSummary;

    // Dual Tabs
    private TextView mTabResults;
    private TextView mTabInspector;

    // Content & States
    private SwipeRefreshLayout mSwipeRefresh;
    private RecyclerView mRecyclerResults;
    private RecyclerView mRecyclerInspector;
    private LinearLayout mLayoutStateInitial;
    private LinearLayout mLayoutStateLoading;
    private TextView mTextLoadingStatus;
    private TextView mTextLoadingSubtitle;
    private LinearLayout mLayoutStateEmpty;
    private LinearLayout mLayoutStateError;
    private TextView mTextErrorDetail;
    private MaterialButton mBtnRetrySearch;

    // Adapters
    private SearchResultAdapter mResultsAdapter;
    private EngineInspectorAdapter mInspectorAdapter;

    // Async request states & cached responses
    private boolean mIsSearching = false;
    private boolean mIsExplaining = false;
    private String mSearchError = null;
    private String mExplainError = null;
    private SearchResponse mLastSearchResponse;
    private ExplainData mLastExplainData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before view inflation
        PreferencesManager.getInstance(this).applySavedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupAdapters();
        setupListeners();
        setupTopicChips();

        updateModeSelection(MODE_KEYWORD);
        updateTabSelection(0);
    }

    private void initViews() {
        mTextEngineStatus = findViewById(R.id.text_engine_status);
        mBtnStats = findViewById(R.id.btn_stats);
        mBtnSettings = findViewById(R.id.btn_settings);

        mEditSearch = findViewById(R.id.edit_search);
        mBtnClearSearch = findViewById(R.id.btn_clear_search);
        mProgressSearchBar = findViewById(R.id.progress_search_bar);

        mBtnModeKeyword = findViewById(R.id.btn_mode_keyword);
        mBtnModePhrase = findViewById(R.id.btn_mode_phrase);
        mLayoutSearchSummary = findViewById(R.id.layout_search_summary);
        mTextSearchSummary = findViewById(R.id.text_search_summary);

        mTabResults = findViewById(R.id.tab_results);
        mTabInspector = findViewById(R.id.tab_inspector);

        mSwipeRefresh = findViewById(R.id.swipe_refresh);
        mRecyclerResults = findViewById(R.id.recycler_results);
        mRecyclerInspector = findViewById(R.id.recycler_inspector);

        mLayoutStateInitial = findViewById(R.id.layout_state_initial);
        mLayoutStateLoading = findViewById(R.id.layout_state_loading);
        mTextLoadingStatus = findViewById(R.id.text_loading_status);
        mTextLoadingSubtitle = findViewById(R.id.text_loading_subtitle);
        mLayoutStateEmpty = findViewById(R.id.layout_state_empty);
        mLayoutStateError = findViewById(R.id.layout_state_error);
        mTextErrorDetail = findViewById(R.id.text_error_detail);
        mBtnRetrySearch = findViewById(R.id.btn_retry_search);

        mSwipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.primary_accent));
    }

    private void setupAdapters() {
        mResultsAdapter = new SearchResultAdapter(result ->
                startActivity(ArticleReaderActivity.createIntent(this, result.getDocId(), result.getTitle())));
        mRecyclerResults.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerResults.setAdapter(mResultsAdapter);

        mInspectorAdapter = new EngineInspectorAdapter();
        mRecyclerInspector.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerInspector.setAdapter(mInspectorAdapter);
    }

    private void setupListeners() {
        // Search submit via keyboard
        mEditSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                hideKeyboard();
                return true;
            }
            return false;
        });

        // Search text watcher for clear button
        mEditSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mBtnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mBtnClearSearch.setOnClickListener(v -> {
            mEditSearch.setText("");
            mCurrentQuery = "";
            mLastSearchResponse = null;
            mLastExplainData = null;
            mSearchError = null;
            mExplainError = null;
            mResultsAdapter.setItems(null);
            mInspectorAdapter.setExplainData(null);
            mTextEngineStatus.setText(R.string.status_ready);
            mProgressSearchBar.setVisibility(View.GONE);
            updateTabUI();
        });

        // Mode buttons
        mBtnModeKeyword.setOnClickListener(v -> {
            if (!MODE_KEYWORD.equals(mCurrentMode)) {
                updateModeSelection(MODE_KEYWORD);
                if (!mCurrentQuery.isEmpty()) {
                    performSearch();
                }
            }
        });

        mBtnModePhrase.setOnClickListener(v -> {
            if (!MODE_PHRASE.equals(mCurrentMode)) {
                updateModeSelection(MODE_PHRASE);
                if (!mCurrentQuery.isEmpty()) {
                    performSearch();
                }
            }
        });

        // Dual Tabs
        mTabResults.setOnClickListener(v -> updateTabSelection(0));
        mTabInspector.setOnClickListener(v -> updateTabSelection(1));

        // SwipeRefresh
        mSwipeRefresh.setOnRefreshListener(() -> {
            if (!mCurrentQuery.isEmpty()) {
                performSearch();
            } else {
                mSwipeRefresh.setRefreshing(false);
            }
        });

        // Retry button
        mBtnRetrySearch.setOnClickListener(v -> performSearch());

        // Stats Button
        mBtnStats.setOnClickListener(v -> {
            StatsBottomSheet bottomSheet = StatsBottomSheet.newInstance();
            bottomSheet.show(getSupportFragmentManager(), StatsBottomSheet.TAG);
        });

        // Settings Button
        mBtnSettings.setOnClickListener(v -> {
            SettingsDialogFragment dialog = SettingsDialogFragment.newInstance();
            dialog.setOnSettingsSavedListener(() -> {
                if (!mCurrentQuery.isEmpty()) {
                    performSearch();
                }
            });
            dialog.show(getSupportFragmentManager(), SettingsDialogFragment.TAG);
        });
    }

    private void setupTopicChips() {
        findViewById(R.id.chip_einstein).setOnClickListener(v -> searchTopic("Albert Einstein"));
        findViewById(R.id.chip_solar_system).setOnClickListener(v -> searchTopic("Solar system"));
        findViewById(R.id.chip_quantum).setOnClickListener(v -> searchTopic("Quantum mechanics"));
        findViewById(R.id.chip_relativity).setOnClickListener(v -> searchTopic("Theory of relativity"));
    }

    private void searchTopic(String topic) {
        mEditSearch.setText(topic);
        mEditSearch.setSelection(topic.length());
        performSearch();
        hideKeyboard();
    }

    private void performSearch() {
        String query = mEditSearch.getText() != null ? mEditSearch.getText().toString().trim() : "";
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        mCurrentQuery = query;
        mIsSearching = true;
        mIsExplaining = true;
        mSearchError = null;
        mExplainError = null;
        mLastSearchResponse = null;
        mLastExplainData = null;

        mProgressSearchBar.setVisibility(View.VISIBLE);
        mTextEngineStatus.setText(R.string.status_searching);
        updateTabUI();

        // 1. Fetch Search Results
        ApiClient.getInstance(this).search(query, mCurrentMode, 10, new ApiClient.ApiCallback<SearchResponse>() {
            @Override
            public void onSuccess(SearchResponse response) {
                if (isFinishing() || isDestroyed()) return;
                mIsSearching = false;
                mLastSearchResponse = response;
                mProgressSearchBar.setVisibility(mIsExplaining ? View.VISIBLE : View.GONE);
                mSwipeRefresh.setRefreshing(false);
                mTextEngineStatus.setText(R.string.status_online);
                mTextEngineStatus.setBackgroundResource(R.drawable.bg_status_badge);

                mResultsAdapter.setItems(response.getResults());
                updateTabUI();
            }

            @Override
            public void onError(String message, Throwable throwable) {
                if (isFinishing() || isDestroyed()) return;
                mIsSearching = false;
                mSearchError = message;
                mProgressSearchBar.setVisibility(mIsExplaining ? View.VISIBLE : View.GONE);
                mSwipeRefresh.setRefreshing(false);
                mTextEngineStatus.setText(R.string.status_error);
                updateTabUI();
            }
        });

        // 2. Fetch 8-Step Engine Inspector in parallel
        ApiClient.getInstance(this).explain(query, mCurrentMode, new ApiClient.ApiCallback<ExplainData>() {
            @Override
            public void onSuccess(ExplainData explainData) {
                if (isFinishing() || isDestroyed()) return;
                mIsExplaining = false;
                mLastExplainData = explainData;
                mProgressSearchBar.setVisibility(mIsSearching ? View.VISIBLE : View.GONE);
                mSwipeRefresh.setRefreshing(false);

                mInspectorAdapter.setExplainData(explainData);
                updateTabUI();
            }

            @Override
            public void onError(String message, Throwable throwable) {
                if (isFinishing() || isDestroyed()) return;
                mIsExplaining = false;
                mExplainError = message;
                mProgressSearchBar.setVisibility(mIsSearching ? View.VISIBLE : View.GONE);
                mSwipeRefresh.setRefreshing(false);
                updateTabUI();
            }
        });
    }

    private void updateModeSelection(String mode) {
        mCurrentMode = mode;
        if (MODE_KEYWORD.equals(mode)) {
            mBtnModeKeyword.setBackgroundResource(R.drawable.bg_segment_active);
            mBtnModeKeyword.setTextColor(ContextCompat.getColor(this, R.color.white));
            mBtnModePhrase.setBackgroundResource(R.drawable.bg_segment_inactive);
            mBtnModePhrase.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));
        } else {
            mBtnModePhrase.setBackgroundResource(R.drawable.bg_segment_active);
            mBtnModePhrase.setTextColor(ContextCompat.getColor(this, R.color.white));
            mBtnModeKeyword.setBackgroundResource(R.drawable.bg_segment_inactive);
            mBtnModeKeyword.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));
        }
    }

    private void updateTabSelection(int tabIndex) {
        mSelectedTab = tabIndex;
        updateTabUI();
    }

    private void updateTabUI() {
        if (mSelectedTab == 0) {
            // === TAB 0: SEARCH RESULTS ===
            mTabResults.setBackgroundResource(R.drawable.bg_segment_active);
            mTabResults.setTextColor(ContextCompat.getColor(this, R.color.white));
            mTabInspector.setBackgroundResource(R.drawable.bg_segment_inactive);
            mTabInspector.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));

            // Summary pill
            if (mLastSearchResponse != null && !mLastSearchResponse.getResults().isEmpty()) {
                mTextSearchSummary.setText(String.format(Locale.getDefault(),
                        getString(R.string.latency_hits_format), mLastSearchResponse.getLatencyMs(), mLastSearchResponse.getTotalHits()));
                mLayoutSearchSummary.setVisibility(View.VISIBLE);
            } else {
                mLayoutSearchSummary.setVisibility(View.GONE);
            }

            // Results tab active: Inspector recycler is always hidden
            mRecyclerInspector.setVisibility(View.GONE);

            if (mCurrentQuery.isEmpty() && mLastSearchResponse == null && !mIsSearching && mSearchError == null) {
                // Initial welcome state
                mLayoutStateInitial.setVisibility(View.VISIBLE);
                mLayoutStateLoading.setVisibility(View.GONE);
                mLayoutStateEmpty.setVisibility(View.GONE);
                mLayoutStateError.setVisibility(View.GONE);
                mRecyclerResults.setVisibility(View.GONE);
            } else if (mIsSearching) {
                if (mLastSearchResponse == null) {
                    mTextLoadingStatus.setText(R.string.status_searching);
                    mTextLoadingSubtitle.setText(R.string.server_waking_up);
                    mLayoutStateLoading.setVisibility(View.VISIBLE);
                    mLayoutStateInitial.setVisibility(View.GONE);
                    mLayoutStateEmpty.setVisibility(View.GONE);
                    mLayoutStateError.setVisibility(View.GONE);
                    mRecyclerResults.setVisibility(View.GONE);
                } else {
                    // Previous results stay visible with top loading indicators
                    mLayoutStateLoading.setVisibility(View.GONE);
                    mLayoutStateInitial.setVisibility(View.GONE);
                    mLayoutStateEmpty.setVisibility(View.GONE);
                    mLayoutStateError.setVisibility(View.GONE);
                    mRecyclerResults.setVisibility(View.VISIBLE);
                }
            } else if (mSearchError != null) {
                mTextErrorDetail.setText(mSearchError);
                mLayoutStateError.setVisibility(View.VISIBLE);
                mLayoutStateLoading.setVisibility(View.GONE);
                mLayoutStateInitial.setVisibility(View.GONE);
                mLayoutStateEmpty.setVisibility(View.GONE);
                mRecyclerResults.setVisibility(View.GONE);
            } else if (mLastSearchResponse != null) {
                mLayoutStateLoading.setVisibility(View.GONE);
                mLayoutStateInitial.setVisibility(View.GONE);
                mLayoutStateError.setVisibility(View.GONE);

                if (mLastSearchResponse.getResults().isEmpty()) {
                    mLayoutStateEmpty.setVisibility(View.VISIBLE);
                    mRecyclerResults.setVisibility(View.GONE);
                } else {
                    mLayoutStateEmpty.setVisibility(View.GONE);
                    mRecyclerResults.setVisibility(View.VISIBLE);
                }
            }
        } else {
            // === TAB 1: 8-STEP ENGINE INSPECTOR ===
            mTabInspector.setBackgroundResource(R.drawable.bg_segment_active);
            mTabInspector.setTextColor(ContextCompat.getColor(this, R.color.white));
            mTabResults.setBackgroundResource(R.drawable.bg_segment_inactive);
            mTabResults.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));

            // Summary pill
            if (mLastExplainData != null) {
                mTextSearchSummary.setText(String.format(Locale.getDefault(),
                        getString(R.string.pipeline_latency_format), mLastExplainData.getTotalPipelineLatencyMs()));
                mLayoutSearchSummary.setVisibility(View.VISIBLE);
            } else {
                mLayoutSearchSummary.setVisibility(View.GONE);
            }

            // Results recycler, initial explore screen, and empty state NEVER show on Inspector tab!
            mRecyclerResults.setVisibility(View.GONE);
            mLayoutStateInitial.setVisibility(View.GONE);
            mLayoutStateEmpty.setVisibility(View.GONE);

            if (mIsExplaining && mLastExplainData == null) {
                mTextLoadingStatus.setText("Tracing 8-Step Engine Pipeline…");
                mTextLoadingSubtitle.setText("Executing positional tokenizer, lexicon lookup, and BM25 scoring…");
                mLayoutStateLoading.setVisibility(View.VISIBLE);
                mLayoutStateError.setVisibility(View.GONE);
                mRecyclerInspector.setVisibility(View.GONE);
            } else if (mExplainError != null && mLastExplainData == null) {
                mTextErrorDetail.setText(mExplainError);
                mLayoutStateError.setVisibility(View.VISIBLE);
                mLayoutStateLoading.setVisibility(View.GONE);
                mRecyclerInspector.setVisibility(View.GONE);
            } else {
                // Shows the 8-Step Engine Inspector list (either live data or ready overview)!
                mLayoutStateLoading.setVisibility(View.GONE);
                mLayoutStateError.setVisibility(View.GONE);
                mRecyclerInspector.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
