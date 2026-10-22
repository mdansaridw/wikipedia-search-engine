package com.hcl.wikisearch;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hcl.wikisearch.models.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    public interface OnResultClickListener {
        void onResultClick(SearchResult result);
    }

    private final List<SearchResult> mItems = new ArrayList<>();
    private final OnResultClickListener mListener;
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");

    public SearchResultAdapter(OnResultClickListener listener) {
        mListener = listener;
    }

    public void setItems(List<SearchResult> newItems) {
        mItems.clear();
        if (newItems != null) {
            mItems.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult item = mItems.get(position);
        Context context = holder.itemView.getContext();

        holder.textRank.setText(String.format(Locale.getDefault(), "#%d", item.getRank()));
        holder.textTitle.setText(item.getTitle());
        holder.textScore.setText(String.format(Locale.getDefault(), "Score: %.2f", item.getScore()));
        holder.textDocId.setText(String.format(Locale.getDefault(), "Doc ID: #%d", item.getDocId()));

        // Format Snippet markdown bolding (**word**)
        CharSequence formattedSnippet = parseMarkdownBold(item.getSnippet(), context);
        holder.textSnippet.setText(formattedSnippet);

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onResultClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    private CharSequence parseMarkdownBold(String rawText, Context context) {
        if (rawText == null || rawText.isEmpty()) {
            return "";
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        Matcher matcher = BOLD_PATTERN.matcher(rawText);
        int lastIndex = 0;
        int accentColor = ContextCompat.getColor(context, R.color.primary_accent);

        while (matcher.find()) {
            // Append preceding plain text
            ssb.append(rawText.substring(lastIndex, matcher.start()));

            // Append bold highlighted text
            String boldContent = matcher.group(1);
            int start = ssb.length();
            ssb.append(boldContent);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(accentColor), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            lastIndex = matcher.end();
        }

        if (lastIndex < rawText.length()) {
            ssb.append(rawText.substring(lastIndex));
        }

        return ssb;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textRank;
        final TextView textTitle;
        final TextView textScore;
        final TextView textSnippet;
        final TextView textDocId;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textRank = itemView.findViewById(R.id.text_result_rank);
            textTitle = itemView.findViewById(R.id.text_result_title);
            textScore = itemView.findViewById(R.id.text_result_score);
            textSnippet = itemView.findViewById(R.id.text_result_snippet);
            textDocId = itemView.findViewById(R.id.text_doc_id);
        }
    }
}
