package com.hcl.wikisearch.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchResponse implements Serializable {
    private final String query;
    private final String mode;
    private final long totalHits;
    private final double latencyMs;
    private final List<SearchResult> results;

    public SearchResponse(String query, String mode, long totalHits, double latencyMs, List<SearchResult> results) {
        this.query = query != null ? query : "";
        this.mode = mode != null ? mode : "keyword";
        this.totalHits = totalHits;
        this.latencyMs = latencyMs;
        this.results = results != null ? results : Collections.emptyList();
    }

    public static SearchResponse fromJson(JSONObject json) {
        if (json == null) return null;
        String query = json.optString("query", "");
        String mode = json.optString("mode", "keyword");
        long totalHits = json.optLong("total_hits", 0);
        double latencyMs = json.optDouble("latency_ms", 0.0);

        List<SearchResult> list = new ArrayList<>();
        JSONArray resultsArray = json.optJSONArray("results");
        if (resultsArray != null) {
            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject item = resultsArray.optJSONObject(i);
                if (item != null) {
                    SearchResult sr = SearchResult.fromJson(item);
                    if (sr != null) {
                        list.add(sr);
                    }
                }
            }
        }
        return new SearchResponse(query, mode, totalHits, latencyMs, list);
    }

    public String getQuery() {
        return query;
    }

    public String getMode() {
        return mode;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public double getLatencyMs() {
        return latencyMs;
    }

    public List<SearchResult> getResults() {
        return results;
    }
}
