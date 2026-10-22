package com.hcl.wikisearch;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hcl.wikisearch.models.ExplainData;

import java.util.Locale;

public class EngineInspectorAdapter extends RecyclerView.Adapter<EngineInspectorAdapter.StepViewHolder> {

    private ExplainData mExplainData;
    private final boolean[] mExpanded = new boolean[8];

    public EngineInspectorAdapter() {
        // Expand the first step by default
        mExpanded[0] = true;
    }

    public void setExplainData(ExplainData data) {
        mExplainData = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inspector_step, parent, false);
        return new StepViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return 8; // 8 chronological pipeline stages
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        int stepNumber = position + 1;
        holder.textStepNumber.setText(String.valueOf(stepNumber));

        // Apply 8-Step Engine Inspector Accent Colors
        int bgResId;
        int fgResId;
        switch (stepNumber) {
            case 1: bgResId = R.color.step1_badge_bg; fgResId = R.color.step1_fg; break;
            case 2: bgResId = R.color.step2_badge_bg; fgResId = R.color.step2_fg; break;
            case 3: bgResId = R.color.step3_badge_bg; fgResId = R.color.step3_fg; break;
            case 4: bgResId = R.color.step4_badge_bg; fgResId = R.color.step4_fg; break;
            case 5: bgResId = R.color.step5_badge_bg; fgResId = R.color.step5_fg; break;
            case 6: bgResId = R.color.step6_badge_bg; fgResId = R.color.step6_fg; break;
            case 7: bgResId = R.color.step7_badge_bg; fgResId = R.color.step7_fg; break;
            case 8:
            default: bgResId = R.color.step8_badge_bg; fgResId = R.color.step8_fg; break;
        }

        int bgColor = ContextCompat.getColor(context, bgResId);
        int fgColor = ContextCompat.getColor(context, fgResId);

        GradientDrawable badgeDrawable = new GradientDrawable();
        badgeDrawable.setShape(GradientDrawable.OVAL);
        badgeDrawable.setColor(bgColor);
        holder.textStepNumber.setBackground(badgeDrawable);
        holder.textStepNumber.setTextColor(fgColor);

        // Bind step summary & content
        boolean isExpanded = mExpanded[position];
        holder.iconChevron.setImageResource(isExpanded ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);
        holder.divider.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.layoutDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        bindStepData(holder, position, context);

        holder.itemView.setOnClickListener(v -> {
            mExpanded[position] = !mExpanded[position];
            notifyItemChanged(position);
        });
    }

    private void bindStepData(StepViewHolder holder, int position, Context context) {
        holder.containerFields.removeAllViews();

        if (mExplainData == null) {
            bindDefaultStep(holder, position, context);
            return;
        }

        switch (position) {
            case 0: // Step 1: Tokenizer
                bindTokenizer(holder, mExplainData.getStep1(), context);
                break;
            case 1: // Step 2: Lexicon
                bindLexicon(holder, mExplainData.getStep2(), context);
                break;
            case 2: // Step 3: Compression
                bindCompression(holder, mExplainData.getStep3(), context);
                break;
            case 3: // Step 4: Phrase Intersection
                bindPhraseIntersection(holder, mExplainData.getStep4(), context);
                break;
            case 4: // Step 5: BM25 Scoring
                bindBm25(holder, mExplainData.getStep5(), context);
                break;
            case 5: // Step 6: Min-Heap
                bindMinHeap(holder, mExplainData.getStep6(), context);
                break;
            case 6: // Step 7: Document Store
                bindDocStore(holder, mExplainData.getStep7(), context);
                break;
            case 7: // Step 8: Snippet Generation
                bindSnippet(holder, mExplainData.getStep8(), context);
                break;
        }
    }

    private void bindDefaultStep(StepViewHolder holder, int position, Context context) {
        switch (position) {
            case 0:
                holder.textStepName.setText(R.string.step1_name);
                holder.textStepSummary.setText("Query Input & Positional Tokenizer (Regex)");
                holder.textStepLatency.setText("Ready");
                holder.textDetailTitle.setText("1. Query Input & Positional Tokenizer");
                addPropertyRow(holder.containerFields, context, "Component", "Compiled C-Regex Positional Tokenizer", false);
                addPropertyRow(holder.containerFields, context, "Rule", "re.compile(r'[a-z0-9]+') (0-based word offsets)", true);
                addPropertyRow(holder.containerFields, context, "Function", "Normalizes casing, strips punctuation, and extracts word positions for phrase validation.", false);
                addPropertyRow(holder.containerFields, context, "Status", "Awaiting search query", false);
                break;
            case 1:
                holder.textStepName.setText(R.string.step2_name);
                holder.textStepSummary.setText("Lexicon Binary Search (`terms.dict`) & Postings Lookup");
                holder.textStepLatency.setText("Ready");
                holder.textDetailTitle.setText("2. Lexicon Binary Search (`terms.dict`)");
                addPropertyRow(holder.containerFields, context, "Vocabulary Size", "1,160,628 unique Wikipedia terms", false);
                addPropertyRow(holder.containerFields, context, "Lookup Algorithm", "O(log V) Binary Search over sorted in-memory dictionary", false);
                addPropertyRow(holder.containerFields, context, "IDF Formula", "IDF = ln(1 + (N - DF + 0.5) / (DF + 0.5)) [N = 270,509]", true);
                addPropertyRow(holder.containerFields, context, "Status", "Awaiting search query", false);
                break;
            case 2:
                holder.textStepName.setText(R.string.step3_name);
                holder.textStepSummary.setText("Delta-Encoding + Variable-Byte (VByte) Decompression");
                holder.textStepLatency.setText("Ready");
                holder.textDetailTitle.setText("3. VByte & Delta Decompression (`postings.bin`)");
                addPropertyRow(holder.containerFields, context, "Algorithm", "Tier D: Delta-Encoding (D_i = D_{i-1} + Delta_i) + VByte", false);
                addPropertyRow(holder.containerFields, context, "Bit-Packing", "7 data bits + 1 continuation bit (MSB)", false);
                addPropertyRow(holder.containerFields, context, "Compression Ratio", "69.0% Space Savings (~6.2M integers/sec throughput)", false);
                addPropertyRow(holder.containerFields, context, "Status", "Awaiting search query", false);
                break;
            case 3:
                holder.textStepName.setText(R.string.step4_name);
                holder.textStepSummary.setText("Two-Pointer Positional Exact-Phrase Verification");
                holder.textStepLatency.setText("Ready");
                holder.textDetailTitle.setText("4. Two-Pointer Positional Phrase Verification (Tier A+)");
                addPropertyRow(holder.containerFields, context, "Algorithm", "Two-Pointer Linear Scan", false);
                addPropertyRow(holder.containerFields, context, "Verification Condition", "pos(next_word) == pos(current_word) + 1", true);
                addPropertyRow(holder.containerFields, context, "Execution", "Active in Exact Phrase mode; bypassed in Keyword mode.", false);
                addPropertyRow(holder.containerFields, context, "Status", "Awaiting search query", false);
                break;
            case 4:
                holder.textStepName.setText(R.string.step5_name);
                holder.textStepSummary.setText("Okapi BM25 Document Relevance & Title Multipliers");
                holder.textStepLatency.setText("Ready");
                holder.textDetailTitle.setText("5. Okapi BM25 Relevance Scoring & Title Multipliers");
                addPropertyRow(holder.containerFields, context, "Formula", "Score(D, Q) = Sum[ IDF * (TF * (k1 + 1)) / (TF + k1 * (1 - b + b * (|D| / avgdl))) ] * TitleBoost", true);
                addPropertyRow(holder.containerFields, context, "Hyperparameters", "k1 = 1.2, b = 0.75, avgdl = 308.5 words", false);
                addPropertyRow(holder.containerFields, context, "Title Multiplier", "3.0x Boost for title match; +50.0 Canonical Bonus", false);
                addPropertyRow(holder.containerFields, context, "Status", "Awaiting search query", false);
                break;
            case 5:
                holder.textStepName.setText(R.string.step6_name);
                holder.textStepSummary.setText("Bounded Binary Min-Heap Top-K Selection");
                holder.textStepLatency.setText("Ready");
                holder.textDetailTitle.setText("6. Top-K Min-Heap Selection (Winner Crowned)");
                addPropertyRow(holder.containerFields, context, "Algorithm", "Bounded Binary Min-Heap (heapq.heapreplace)", false);
                addPropertyRow(holder.containerFields, context, "Complexity", "O(N log K) with K=10 (~3.1x faster than full quicksort)", false);
                addPropertyRow(holder.containerFields, context, "Function", "Maintains top 10 scoring documents while pruning remaining candidate stream.", false);
                addPropertyRow(holder.containerFields, context, "Status", "Awaiting search query", false);
                break;
            case 6:
                holder.textStepName.setText(R.string.step7_name);
                holder.textStepSummary.setText("Zero-DB Direct Binary Disk Jump (`docs.bin`)");
                holder.textStepLatency.setText("Ready");
                holder.textDetailTitle.setText("7. Binary Document Store Direct Disk Jump (`docs.bin`)");
                addPropertyRow(holder.containerFields, context, "Mechanism", "file.seek(start_offset) -> file.read(length) (Zero DB overhead)", true);
                addPropertyRow(holder.containerFields, context, "RAM Footprint", "4.13 MB memory-mapped offset array for all 270,509 docs", false);
                addPropertyRow(holder.containerFields, context, "Average Latency", "~0.06 ms (60 microseconds) direct NVMe seek", false);
                addPropertyRow(holder.containerFields, context, "Status", "Awaiting search query", false);
                break;
            case 7:
            default:
                holder.textStepName.setText(R.string.step8_name);
                holder.textStepSummary.setText("Dynamic Sliding-Window Term Density & Highlighting");
                holder.textStepLatency.setText("Ready");
                holder.textDetailTitle.setText("8. Dynamic Snippet Extractor & Term Highlighting");
                addPropertyRow(holder.containerFields, context, "Algorithm", "Sliding Window Term Density Maximization", false);
                addPropertyRow(holder.containerFields, context, "Window Size", "160 characters with whitespace boundary snapping", false);
                addPropertyRow(holder.containerFields, context, "Highlighting", "Surrounds keyword matches with Markdown bold tags (**term**)", false);
                addPropertyRow(holder.containerFields, context, "Status", "Awaiting search query", false);
                break;
        }
    }

    private void bindTokenizer(StepViewHolder holder, ExplainData.TokenizerStep step, Context context) {
        holder.textStepName.setText(R.string.step1_name);
        if (step == null) {
            holder.textStepSummary.setText(R.string.not_provided);
            holder.textStepLatency.setText("—");
            return;
        }
        holder.textStepSummary.setText(String.format(Locale.getDefault(), "%d token%s extracted (%s)",
                step.tokensWithPositions.size(), step.tokensWithPositions.size() == 1 ? "" : "s", step.queryMode));
        holder.textStepLatency.setText(String.format(Locale.getDefault(), "%.1f µs", step.latencyUs));
        holder.textDetailTitle.setText(step.title);

        addPropertyRow(holder.containerFields, context, "Raw Query", step.rawQuery, false);
        addPropertyRow(holder.containerFields, context, "Engine Rule", step.engineRule, true);
        addPropertyRow(holder.containerFields, context, "Case Folding", step.caseFolding, false);
        addPropertyRow(holder.containerFields, context, "Punctuation Handling", step.punctuationHandling, false);
        addPropertyRow(holder.containerFields, context, "Query Mode", step.queryMode, false);
        addPropertyRow(holder.containerFields, context, "Detected Intent", step.detectedIntent, false);

        if (!step.tokensWithPositions.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ExplainData.TokenPosition tp : step.tokensWithPositions) {
                sb.append("'").append(tp.token).append("' [pos: ").append(tp.position).append("]   ");
            }
            addPropertyRow(holder.containerFields, context, "Positional Tokens", sb.toString().trim(), true);
        }
    }

    private void bindLexicon(StepViewHolder holder, ExplainData.LexiconStep step, Context context) {
        holder.textStepName.setText(R.string.step2_name);
        if (step == null) {
            holder.textStepSummary.setText(R.string.not_provided);
            holder.textStepLatency.setText("—");
            return;
        }
        holder.textStepSummary.setText(String.format(Locale.getDefault(), "%d term%s analyzed in dictionary",
                step.terms.size(), step.terms.size() == 1 ? "" : "s"));
        holder.textStepLatency.setText(String.format(Locale.getDefault(), "%.3f ms", step.latencyMs));
        holder.textDetailTitle.setText(step.title);

        addPropertyRow(holder.containerFields, context, "Vocabulary Size", step.vocabularySize, false);
        addPropertyRow(holder.containerFields, context, "Lookup Algorithm", step.lookupAlgorithm, false);
        addPropertyRow(holder.containerFields, context, "IDF Formula", step.formula, true);

        for (ExplainData.LexiconTerm term : step.terms) {
            addDivider(holder.containerFields, context);
            addPropertyRow(holder.containerFields, context, "Term: " + term.term,
                    String.format(Locale.getDefault(), "DF: %,d (%s) · IDF: %.3f · Rarity: %s",
                            term.df, term.percentageOfCorpus, term.idf, term.rarity), false);
            addPropertyRow(holder.containerFields, context, "Postings Offset",
                    String.format(Locale.getDefault(), "%s (%,d bytes / %.2f KB)",
                            term.postingsOffset, term.chunkSizeBytes, term.chunkSizeKb), true);
        }
    }

    private void bindCompression(StepViewHolder holder, ExplainData.CompressionStep step, Context context) {
        holder.textStepName.setText(R.string.step3_name);
        if (step == null) {
            holder.textStepSummary.setText(R.string.not_provided);
            holder.textStepLatency.setText("—");
            return;
        }
        holder.textStepSummary.setText(String.format(Locale.getDefault(), "%s (%s)", step.algorithm, step.diskSavings));
        holder.textStepLatency.setText(String.format(Locale.getDefault(), "%.2f ms", step.latencyMs));
        holder.textDetailTitle.setText(step.title);

        addPropertyRow(holder.containerFields, context, "Algorithm", step.algorithm, false);
        addPropertyRow(holder.containerFields, context, "Bit-Packing Scheme", step.bitPackingScheme, false);
        addPropertyRow(holder.containerFields, context, "Compressed Bytes Read", step.totalCompressedBytesRead, false);
        addPropertyRow(holder.containerFields, context, "Uncompressed Footprint", step.uncompressedFootprintEstimate, false);
        addPropertyRow(holder.containerFields, context, "Disk Space Savings", step.diskSavings, false);
        addPropertyRow(holder.containerFields, context, "Decompression Throughput", step.decompressionThroughput, false);
        if (step.bitVisualizationSample != null && !step.bitVisualizationSample.isEmpty()) {
            addCodeBlock(holder.containerFields, context, "Bit Visualization Sample", step.bitVisualizationSample);
        }
    }

    private void bindPhraseIntersection(StepViewHolder holder, ExplainData.PhraseIntersectionStep step, Context context) {
        holder.textStepName.setText(R.string.step4_name);
        if (step == null) {
            holder.textStepSummary.setText(R.string.not_provided);
            holder.textStepLatency.setText("—");
            return;
        }
        holder.textStepSummary.setText(step.tested
                ? String.format(Locale.getDefault(), "Exact Match Verified (%d eliminated)", step.falsePositivesEliminated)
                : "Not applicable (Keyword Search Mode)");
        holder.textStepLatency.setText(String.format(Locale.getDefault(), "%.3f ms", step.latencyMs));
        holder.textDetailTitle.setText(step.title);

        addPropertyRow(holder.containerFields, context, "Verification Status",
                step.tested ? "Performed Two-Pointer Linear Verification" : "Not Run (Query mode is Keyword, not Exact Phrase)", false);
        addPropertyRow(holder.containerFields, context, "Algorithm", step.algorithm, false);
        addPropertyRow(holder.containerFields, context, "Condition", step.condition, true);
        if (step.tested) {
            addPropertyRow(holder.containerFields, context, "Matched Articles", String.valueOf(step.matchedArticlesCount), false);
            addPropertyRow(holder.containerFields, context, "False Positives Eliminated", String.valueOf(step.falsePositivesEliminated), false);
        }
    }

    private void bindBm25(StepViewHolder holder, ExplainData.Bm25Step step, Context context) {
        holder.textStepName.setText(R.string.step5_name);
        if (step == null) {
            holder.textStepSummary.setText(R.string.not_provided);
            holder.textStepLatency.setText("—");
            return;
        }
        holder.textStepSummary.setText(String.format(Locale.getDefault(), "Top Doc: '%s' (Score: %.2f)",
                step.winningDocTitle, step.finalScore));
        holder.textStepLatency.setText(String.format(Locale.getDefault(), "%.2f ms", step.latencyMs));
        holder.textDetailTitle.setText(step.title);

        addPropertyRow(holder.containerFields, context, "Winning Document",
                String.format(Locale.getDefault(), "%s (Doc ID: #%d)", step.winningDocTitle, step.winningDocId), false);
        addPropertyRow(holder.containerFields, context, "Final Score", String.format(Locale.getDefault(), "%.2f", step.finalScore), true);
        addPropertyRow(holder.containerFields, context, "Formula", step.formula, true);
        addPropertyRow(holder.containerFields, context, "Document Length", String.format(Locale.getDefault(), "%,d words", step.docLength), false);
        addPropertyRow(holder.containerFields, context, "Length Penalty", String.format(Locale.getDefault(), "%.2f", step.lengthPenalty), false);
        addPropertyRow(holder.containerFields, context, "Canonical Bonus", step.canonicalBonus, false);
        addPropertyRow(holder.containerFields, context, "Proximity Bonus", step.phraseProximityBonus, false);

        for (ExplainData.TermCalculation tc : step.termCalculations) {
            addDivider(holder.containerFields, context);
            addPropertyRow(holder.containerFields, context, "Term Calculation: " + tc.term,
                    String.format(Locale.getDefault(), "TF: %,d · IDF: %.3f · Raw BM25: %.2f · Score: %.2f (%s)",
                            tc.tf, tc.idf, tc.rawBm25, tc.termScore, tc.titleBoost), false);
        }
    }

    private void bindMinHeap(StepViewHolder holder, ExplainData.MinHeapStep step, Context context) {
        holder.textStepName.setText(R.string.step6_name);
        if (step == null) {
            holder.textStepSummary.setText(R.string.not_provided);
            holder.textStepLatency.setText("—");
            return;
        }
        holder.textStepSummary.setText(String.format(Locale.getDefault(), "Streamed %,d · Pruned %,d candidates",
                step.totalCandidatesStreamed, step.candidatesPrunedAndDiscarded));
        holder.textStepLatency.setText(String.format(Locale.getDefault(), "%.2f ms", step.latencyMs));
        holder.textDetailTitle.setText(step.title);

        addPropertyRow(holder.containerFields, context, "Algorithm", step.algorithm, false);
        addPropertyRow(holder.containerFields, context, "Candidates Streamed", String.format(Locale.getDefault(), "%,d", step.totalCandidatesStreamed), false);
        addPropertyRow(holder.containerFields, context, "Candidates Discarded", String.format(Locale.getDefault(), "%,d", step.candidatesPrunedAndDiscarded), false);
        addPropertyRow(holder.containerFields, context, "Efficiency Speedup", step.speedupVsFullSort, false);

        if (!step.leaderboard.isEmpty()) {
            addDivider(holder.containerFields, context);
            addPropertyRow(holder.containerFields, context, "Top Candidates Leaderboard", "", false);
            for (ExplainData.LeaderboardCandidate cand : step.leaderboard) {
                addPropertyRow(holder.containerFields, context,
                        String.format(Locale.getDefault(), "#%d  %s", cand.rank, cand.title),
                        String.format(Locale.getDefault(), "Score: %.2f  (Doc #%d)", cand.score, cand.docId), true);
            }
        }
    }

    private void bindDocStore(StepViewHolder holder, ExplainData.DocStoreStep step, Context context) {
        holder.textStepName.setText(R.string.step7_name);
        if (step == null) {
            holder.textStepSummary.setText(R.string.not_provided);
            holder.textStepLatency.setText("—");
            return;
        }
        holder.textStepSummary.setText(String.format(Locale.getDefault(), "Zero-DB Disk Jump: %s", step.directDiskSeekLatency));
        holder.textStepLatency.setText(step.directDiskSeekLatency.isEmpty() ? "—" : step.directDiskSeekLatency);
        holder.textDetailTitle.setText(step.title);

        addPropertyRow(holder.containerFields, context, "Input Winner", step.inputWinnerFromStep6, false);
        addPropertyRow(holder.containerFields, context, "Address Table Entry", step.addressTableEntry, false);
        addPropertyRow(holder.containerFields, context, "Disk Byte Span", step.diskByteSpan, true);
        addPropertyRow(holder.containerFields, context, "Payload Size",
                String.format(Locale.getDefault(), "%,d bytes (%.2f KB)", step.payloadSizeBytes, step.payloadSizeKb), false);
        addPropertyRow(holder.containerFields, context, "Article Length",
                String.format(Locale.getDefault(), "%,d words · %,d chars", step.articleLengthWords, step.articleLengthChars), false);
        addPropertyRow(holder.containerFields, context, "I/O Mechanism", step.ioMechanism, false);
        addPropertyRow(holder.containerFields, context, "RAM Footprint", step.ramFootprint, false);
        if (step.rawBytePreview != null && !step.rawBytePreview.isEmpty()) {
            addCodeBlock(holder.containerFields, context, "Raw Disk Byte Preview", step.rawBytePreview);
        }
    }

    private void bindSnippet(StepViewHolder holder, ExplainData.SnippetStep step, Context context) {
        holder.textStepName.setText(R.string.step8_name);
        if (step == null) {
            holder.textStepSummary.setText(R.string.not_provided);
            holder.textStepLatency.setText("—");
            return;
        }
        holder.textStepSummary.setText(String.format(Locale.getDefault(), "Sliding Window: %d chars", step.windowSizeChars));
        holder.textStepLatency.setText(String.format(Locale.getDefault(), "%.2f ms", step.latencyMs));
        holder.textDetailTitle.setText(step.title);

        addPropertyRow(holder.containerFields, context, "Source Article", step.sourceArticle, false);
        addPropertyRow(holder.containerFields, context, "Source Text Length", String.format(Locale.getDefault(), "%,d chars", step.sourceTextLength), false);
        addPropertyRow(holder.containerFields, context, "Algorithm", step.algorithm, false);
        addPropertyRow(holder.containerFields, context, "Window Size", step.windowSizeChars + " characters", false);
        addPropertyRow(holder.containerFields, context, "Word Boundary Snapping", step.wordBoundarySnapping, false);
        if (step.finalRenderedSnippet != null && !step.finalRenderedSnippet.isEmpty()) {
            addCodeBlock(holder.containerFields, context, "Final Rendered Snippet", step.finalRenderedSnippet);
        }
    }

    private void addPropertyRow(ViewGroup container, Context context, String label, String value, boolean isMonospace) {
        if (value == null || value.isEmpty()) {
            if (label == null || label.isEmpty()) return;
            TextView lblView = new TextView(context);
            lblView.setText(label);
            lblView.setTextSize(13);
            lblView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            lblView.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
            lblView.setPadding(0, 12, 0, 4);
            container.addView(lblView);
            return;
        }

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 6, 0, 6);

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        labelView.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
        row.addView(labelView);

        TextView valView = new TextView(context);
        valView.setText(value);
        valView.setTextSize(13);
        valView.setTypeface(isMonospace ? Typeface.MONOSPACE : Typeface.SANS_SERIF);
        valView.setTextColor(ContextCompat.getColor(context, isMonospace ? R.color.primary_accent : R.color.primary_text));
        valView.setTextIsSelectable(true);
        row.addView(valView);

        container.addView(row);
    }

    private void addCodeBlock(ViewGroup container, Context context, String label, String code) {
        LinearLayout block = new LinearLayout(context);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, 8, 0, 8);

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        labelView.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
        block.addView(labelView);

        TextView codeView = new TextView(context);
        codeView.setText(code);
        codeView.setTextSize(12);
        codeView.setTypeface(Typeface.MONOSPACE);
        codeView.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
        codeView.setBackgroundResource(R.drawable.bg_search_box);
        codeView.setPadding(16, 12, 16, 12);
        codeView.setTextIsSelectable(true);
        block.addView(codeView);

        container.addView(block);
    }

    private void addDivider(ViewGroup container, Context context) {
        View divider = new View(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, 8, 0, 8);
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.border_separator));
        container.addView(divider);
    }

    static class StepViewHolder extends RecyclerView.ViewHolder {
        final TextView textStepNumber;
        final TextView textStepName;
        final TextView textStepSummary;
        final TextView textStepLatency;
        final ImageView iconChevron;
        final View divider;
        final LinearLayout layoutDetails;
        final TextView textDetailTitle;
        final LinearLayout containerFields;

        StepViewHolder(@NonNull View itemView) {
            super(itemView);
            textStepNumber = itemView.findViewById(R.id.text_step_number);
            textStepName = itemView.findViewById(R.id.text_step_name);
            textStepSummary = itemView.findViewById(R.id.text_step_summary);
            textStepLatency = itemView.findViewById(R.id.text_step_latency);
            iconChevron = itemView.findViewById(R.id.icon_chevron);
            divider = itemView.findViewById(R.id.step_divider);
            layoutDetails = itemView.findViewById(R.id.layout_step_details);
            textDetailTitle = itemView.findViewById(R.id.text_step_detail_title);
            containerFields = itemView.findViewById(R.id.container_fields);
        }
    }
}
