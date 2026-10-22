package com.hcl.wikisearch.models;

import org.json.JSONObject;

import java.io.Serializable;

public class StatsData implements Serializable {
    private final String status;
    private final long totalArticles;
    private final long vocabularySize;
    private final double avgDocLength;
    private final String hitAt10;
    private final double mrr;
    private final double latencyP50Ms;
    private final double latencyP95Ms;
    private final double postingsSizeMb;
    private final double termsDictSizeMb;
    private final double docStoreSizeMb;
    private final double totalIndexSizeMb;
    private final String compressionSavings;

    public StatsData(String status, long totalArticles, long vocabularySize, double avgDocLength,
                     String hitAt10, double mrr, double latencyP50Ms, double latencyP95Ms,
                     double postingsSizeMb, double termsDictSizeMb, double docStoreSizeMb,
                     double totalIndexSizeMb, String compressionSavings) {
        this.status = status != null ? status : "online";
        this.totalArticles = totalArticles;
        this.vocabularySize = vocabularySize;
        this.avgDocLength = avgDocLength;
        this.hitAt10 = hitAt10 != null ? hitAt10 : "99.0%";
        this.mrr = mrr;
        this.latencyP50Ms = latencyP50Ms;
        this.latencyP95Ms = latencyP95Ms;
        this.postingsSizeMb = postingsSizeMb;
        this.termsDictSizeMb = termsDictSizeMb;
        this.docStoreSizeMb = docStoreSizeMb;
        this.totalIndexSizeMb = totalIndexSizeMb;
        this.compressionSavings = compressionSavings != null ? compressionSavings : "";
    }

    public static StatsData fromJson(JSONObject json) {
        if (json == null) return null;
        String status = json.optString("status", "online");
        long totalArticles = json.optLong("total_articles", 0);
        long vocabularySize = json.optLong("vocabulary_size", 0);
        double avgDocLength = json.optDouble("avg_doc_length", 0.0);
        String hitAt10 = json.optString("hit_at_10", "");
        double mrr = json.optDouble("mrr", 0.0);
        double p50 = json.optDouble("latency_p50_ms", 0.0);
        double p95 = json.optDouble("latency_p95_ms", 0.0);
        double postingsMb = json.optDouble("postings_size_mb", 0.0);
        double termsMb = json.optDouble("terms_dict_size_mb", 0.0);
        double docStoreMb = json.optDouble("doc_store_size_mb", 0.0);
        double totalIndexMb = json.optDouble("total_index_size_mb", 0.0);
        String compressionSavings = json.optString("compression_savings", "");

        return new StatsData(status, totalArticles, vocabularySize, avgDocLength,
                hitAt10, mrr, p50, p95, postingsMb, termsMb, docStoreMb, totalIndexMb, compressionSavings);
    }

    public String getStatus() { return status; }
    public long getTotalArticles() { return totalArticles; }
    public long getVocabularySize() { return vocabularySize; }
    public double getAvgDocLength() { return avgDocLength; }
    public String getHitAt10() { return hitAt10; }
    public double getMrr() { return mrr; }
    public double getLatencyP50Ms() { return latencyP50Ms; }
    public double getLatencyP95Ms() { return latencyP95Ms; }
    public double getPostingsSizeMb() { return postingsSizeMb; }
    public double getTermsDictSizeMb() { return termsDictSizeMb; }
    public double getDocStoreSizeMb() { return docStoreSizeMb; }
    public double getTotalIndexSizeMb() { return totalIndexSizeMb; }
    public String getCompressionSavings() { return compressionSavings; }
}
