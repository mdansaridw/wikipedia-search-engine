package com.hcl.wikisearch.models;

import org.json.JSONObject;

import java.io.Serializable;

public class ArticleData implements Serializable {
    private final long docId;
    private final String title;
    private final String url;
    private final String text;
    private final int wordCount;
    private final int readingTimeMinutes;

    public ArticleData(long docId, String title, String url, String text) {
        this.docId = docId;
        this.title = title != null ? title : "";
        this.url = url != null ? url : "";
        this.text = text != null ? text : "";

        // Calculate word count and estimated reading time (standard 200 WPM)
        int words = 0;
        if (!this.text.isEmpty()) {
            String[] tokens = this.text.trim().split("\\s+");
            words = tokens.length;
        }
        this.wordCount = words;
        this.readingTimeMinutes = Math.max(1, (int) Math.ceil((double) words / 200.0));
    }

    public static ArticleData fromJson(JSONObject json) {
        if (json == null) return null;
        long docId = json.optLong("doc_id", 0);
        String title = json.optString("title", "Untitled Article");
        String url = json.optString("url", "");
        String text = json.optString("text", "");
        return new ArticleData(docId, title, url, text);
    }

    public long getDocId() {
        return docId;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getText() {
        return text;
    }

    public int getWordCount() {
        return wordCount;
    }

    public int getReadingTimeMinutes() {
        return readingTimeMinutes;
    }
}
