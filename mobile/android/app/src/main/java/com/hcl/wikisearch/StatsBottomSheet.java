package com.hcl.wikisearch;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.hcl.wikisearch.models.StatsData;

import java.util.Locale;

public class StatsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "StatsBottomSheet";

    private LinearLayout mLayoutLoading;
    private LinearLayout mLayoutError;
    private NestedScrollView mScrollStats;
    private TextView mTextError;
    private MaterialButton mBtnRetry;
    private ImageButton mBtnRefresh;

    private TextView mTextArticles;
    private TextView mTextVocab;
    private TextView mTextHit;
    private TextView mTextMrr;
    private TextView mTextP50;
    private TextView mTextP95;
    private TextView mTextCompression;
    private TextView mTextIndexSize;

    public static StatsBottomSheet newInstance() {
        return new StatsBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLayoutLoading = view.findViewById(R.id.layout_stats_loading);
        mLayoutError = view.findViewById(R.id.layout_stats_error);
        mScrollStats = view.findViewById(R.id.scroll_stats);
        mTextError = view.findViewById(R.id.text_stats_error);
        mBtnRetry = view.findViewById(R.id.btn_retry_stats);
        mBtnRefresh = view.findViewById(R.id.btn_refresh_stats);

        mTextArticles = view.findViewById(R.id.text_stat_articles);
        mTextVocab = view.findViewById(R.id.text_stat_vocab);
        mTextHit = view.findViewById(R.id.text_stat_hit);
        mTextMrr = view.findViewById(R.id.text_stat_mrr);
        mTextP50 = view.findViewById(R.id.text_stat_p50);
        mTextP95 = view.findViewById(R.id.text_stat_p95);
        mTextCompression = view.findViewById(R.id.text_stat_compression);
        mTextIndexSize = view.findViewById(R.id.text_stat_index_size);

        mBtnRefresh.setOnClickListener(v -> loadStats());
        mBtnRetry.setOnClickListener(v -> loadStats());

        loadStats();
    }

    private void loadStats() {
        showLoading();
        ApiClient.getInstance(requireContext()).getStats(new ApiClient.ApiCallback<StatsData>() {
            @Override
            public void onSuccess(StatsData stats) {
                if (!isAdded()) return;
                showContent();
                populateData(stats);
            }

            @Override
            public void onError(String message, Throwable throwable) {
                if (!isAdded()) return;
                showError(message);
            }
        });
    }

    private void populateData(StatsData stats) {
        if (stats == null) return;
        mTextArticles.setText(String.format(Locale.getDefault(), "%,d", stats.getTotalArticles()));
        mTextVocab.setText(String.format(Locale.getDefault(), "%,d", stats.getVocabularySize()));
        mTextHit.setText(stats.getHitAt10());
        mTextMrr.setText(String.format(Locale.getDefault(), "%.3f", stats.getMrr()));
        mTextP50.setText(String.format(Locale.getDefault(), "%.1f ms", stats.getLatencyP50Ms()));
        mTextP95.setText(String.format(Locale.getDefault(), "%.1f ms", stats.getLatencyP95Ms()));
        mTextCompression.setText(stats.getCompressionSavings());

        if (stats.getTotalIndexSizeMb() > 0) {
            mTextIndexSize.setText(String.format(Locale.getDefault(),
                    "Total Index: %.1f MB (Postings: %.1f MB, Docs: %.1f MB)",
                    stats.getTotalIndexSizeMb(), stats.getPostingsSizeMb(), stats.getDocStoreSizeMb()));
        } else {
            mTextIndexSize.setText(String.format(Locale.getDefault(), "Average Doc Length: %.1f words", stats.getAvgDocLength()));
        }
    }

    private void showLoading() {
        mLayoutLoading.setVisibility(View.VISIBLE);
        mLayoutError.setVisibility(View.GONE);
        mScrollStats.setVisibility(View.GONE);
    }

    private void showContent() {
        mLayoutLoading.setVisibility(View.GONE);
        mLayoutError.setVisibility(View.GONE);
        mScrollStats.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        mLayoutLoading.setVisibility(View.GONE);
        mLayoutError.setVisibility(View.VISIBLE);
        mScrollStats.setVisibility(View.GONE);
        mTextError.setText(message != null ? message : getString(R.string.status_error));
    }
}
