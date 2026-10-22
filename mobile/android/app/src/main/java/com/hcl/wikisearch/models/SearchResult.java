package com.hcl.wikisearch.models;

import org.json.JSONObject;

import java.io.Serializable;

public class SearchResult implements Serializable {
    private final int rank;
    private final long docId;
    private final String title;
    private final double score;
    private final String snippet;
    private final String url;

    public SearchResult(int rank, long docId, String title, double score, String snippet, String url) {
        this.rank = rank;
        this.docId = docId;
        this.title = title != null ? title : "";
        this.score = score;
        this.snippet = snippet != null ? snippet : "";
        this.url = url != null ? url : "";
    }

    public static SearchResult fromJson(JSONObject json) {
        if (json == null) return null;
        int rank = json.optInt("rank", 0);
        long docId = json.optLong("doc_id", 0);
        String title = json.optString("title", "Untitled");
        double score = json.optDouble("score", 0.0);
        String snippet = json.optString("snippet", "");
        String url = json.optString("url", "");
        return new SearchResult(rank, docId, title, score, snippet, url);
    }

    public int getRank() {
        return rank;
    }

    public long getDocId() {
        return docId;
    }

    public String getTitle() {
        return title;
    }

    public double getScore() {
        return score;
    }

    public String getSnippet() {
        return snippet;
    }

    public String getUrl() {
        return url;
    }
}
