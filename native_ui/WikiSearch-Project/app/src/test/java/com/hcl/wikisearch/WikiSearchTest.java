package com.hcl.wikisearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.hcl.wikisearch.models.ArticleData;
import com.hcl.wikisearch.models.ExplainData;
import com.hcl.wikisearch.models.SearchResponse;
import com.hcl.wikisearch.models.SearchResult;
import com.hcl.wikisearch.models.StatsData;

import org.json.JSONObject;
import org.junit.Test;

public class WikiSearchTest {

    @Test
    public void testSearchResponseParsing() throws Exception {
        String jsonStr = "{\n" +
                "  \"query\": \"solar system\",\n" +
                "  \"mode\": \"keyword\",\n" +
                "  \"latency_ms\": 7.42,\n" +
                "  \"total_hits\": 1420,\n" +
                "  \"results\": [\n" +
                "    {\n" +
                "      \"rank\": 1,\n" +
                "      \"doc_id\": 364,\n" +
                "      \"title\": \"Solar System\",\n" +
                "      \"score\": 146.57,\n" +
                "      \"snippet\": \"The **Solar System** is a gravitationally bound system...\",\n" +
                "      \"url\": \"https://simple.wikipedia.org/wiki/Solar_System\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JSONObject json = new JSONObject(jsonStr);
        SearchResponse response = SearchResponse.fromJson(json);

        assertNotNull(response);
        assertEquals("solar system", response.getQuery());
        assertEquals("keyword", response.getMode());
        assertEquals(7.42, response.getLatencyMs(), 0.001);
        assertEquals(1420, response.getTotalHits());
        assertEquals(1, response.getResults().size());

        SearchResult item = response.getResults().get(0);
        assertEquals(1, item.getRank());
        assertEquals(364, item.getDocId());
        assertEquals("Solar System", item.getTitle());
        assertEquals(146.57, item.getScore(), 0.001);
        assertTrue(item.getSnippet().contains("Solar System"));
    }

    @Test
    public void testExplainDataParsing() throws Exception {
        String jsonStr = "{\n" +
                "  \"query\": \"solar system\",\n" +
                "  \"mode\": \"keyword\",\n" +
                "  \"total_pipeline_latency_ms\": 64.5,\n" +
                "  \"step1_tokenizer\": {\n" +
                "    \"title\": \"1. Query Input & Positional Tokenizer\",\n" +
                "    \"raw_query\": \"solar system\",\n" +
                "    \"engine_rule\": \"re.compile(r'[a-z0-9]+')\",\n" +
                "    \"tokens_with_positions\": [{\"token\": \"solar\", \"position\": 0}],\n" +
                "    \"latency_us\": 31.7\n" +
                "  },\n" +
                "  \"step2_lexicon\": {\n" +
                "    \"title\": \"2. Lexicon Binary Search\",\n" +
                "    \"vocabulary_size\": \"1,160,628 unique terms\",\n" +
                "    \"terms\": [{\"term\": \"solar\", \"df\": 1246, \"idf\": 5.38, \"postings_offset\": \"0x08BBDCC6\"}],\n" +
                "    \"latency_ms\": 0.042\n" +
                "  },\n" +
                "  \"step3_compression\": {\n" +
                "    \"title\": \"3. Compression\",\n" +
                "    \"algorithm\": \"Delta + VByte\",\n" +
                "    \"disk_savings\": \"69.0%\",\n" +
                "    \"latency_ms\": 0.85\n" +
                "  },\n" +
                "  \"step4_phrase_intersection\": {\n" +
                "    \"title\": \"4. Phrase Intersection\",\n" +
                "    \"tested\": false,\n" +
                "    \"latency_ms\": 0.002\n" +
                "  },\n" +
                "  \"step5_bm25_scoring\": {\n" +
                "    \"title\": \"5. BM25 Scoring\",\n" +
                "    \"winning_doc_title\": \"Solar System\",\n" +
                "    \"final_score\": 146.57,\n" +
                "    \"latency_ms\": 1.85\n" +
                "  },\n" +
                "  \"step6_min_heap\": {\n" +
                "    \"title\": \"6. Min-Heap\",\n" +
                "    \"algorithm\": \"Bounded Binary Min-Heap\",\n" +
                "    \"total_candidates_streamed\": 10187,\n" +
                "    \"latency_ms\": 0.08\n" +
                "  },\n" +
                "  \"step7_doc_store\": {\n" +
                "    \"title\": \"7. Doc Store\",\n" +
                "    \"direct_disk_seek_latency\": \"0.06 ms\"\n" +
                "  },\n" +
                "  \"step8_snippet\": {\n" +
                "    \"title\": \"8. Snippet\",\n" +
                "    \"window_size_chars\": 160,\n" +
                "    \"final_rendered_snippet\": \"The **Solar System** is a group...\",\n" +
                "    \"latency_ms\": 0.05\n" +
                "  }\n" +
                "}";

        JSONObject json = new JSONObject(jsonStr);
        ExplainData explain = ExplainData.fromJson(json);

        assertNotNull(explain);
        assertEquals(64.5, explain.getTotalPipelineLatencyMs(), 0.001);

        assertNotNull(explain.getStep1());
        assertEquals("re.compile(r'[a-z0-9]+')", explain.getStep1().engineRule);
        assertEquals(1, explain.getStep1().tokensWithPositions.size());

        assertNotNull(explain.getStep2());
        assertEquals("1,160,628 unique terms", explain.getStep2().vocabularySize);
        assertEquals(1, explain.getStep2().terms.size());

        assertNotNull(explain.getStep3());
        assertEquals("Delta + VByte", explain.getStep3().algorithm);

        assertNotNull(explain.getStep4());
        assertFalse(explain.getStep4().tested);

        assertNotNull(explain.getStep5());
        assertEquals("Solar System", explain.getStep5().winningDocTitle);
        assertEquals(146.57, explain.getStep5().finalScore, 0.001);

        assertNotNull(explain.getStep6());
        assertEquals(10187, explain.getStep6().totalCandidatesStreamed);

        assertNotNull(explain.getStep7());
        assertEquals("0.06 ms", explain.getStep7().directDiskSeekLatency);

        assertNotNull(explain.getStep8());
        assertEquals(160, explain.getStep8().windowSizeChars);
    }

    @Test
    public void testArticleDataParsing() throws Exception {
        String jsonStr = "{\n" +
                "  \"doc_id\": 364,\n" +
                "  \"title\": \"Solar System\",\n" +
                "  \"url\": \"https://simple.wikipedia.org/wiki/Solar_System\",\n" +
                "  \"text\": \"The Sun is in the center of the solar system. Eight planets orbit around it.\"\n" +
                "}";

        JSONObject json = new JSONObject(jsonStr);
        ArticleData article = ArticleData.fromJson(json);

        assertNotNull(article);
        assertEquals(364, article.getDocId());
        assertEquals("Solar System", article.getTitle());
        assertTrue(article.getWordCount() > 0);
        assertTrue(article.getReadingTimeMinutes() >= 1);
    }

    @Test
    public void testStatsDataParsing() throws Exception {
        String jsonStr = "{\n" +
                "  \"status\": \"online\",\n" +
                "  \"total_articles\": 270509,\n" +
                "  \"vocabulary_size\": 1160628,\n" +
                "  \"avg_doc_length\": 308.5,\n" +
                "  \"hit_at_10\": \"99.0%\",\n" +
                "  \"mrr\": 0.99,\n" +
                "  \"latency_p50_ms\": 73.5,\n" +
                "  \"latency_p95_ms\": 246.1,\n" +
                "  \"compression_savings\": \"69.0% (Delta + VByte)\"\n" +
                "}";

        JSONObject json = new JSONObject(jsonStr);
        StatsData stats = StatsData.fromJson(json);

        assertNotNull(stats);
        assertEquals(270509, stats.getTotalArticles());
        assertEquals(1160628, stats.getVocabularySize());
        assertEquals("99.0%", stats.getHitAt10());
        assertEquals(0.99, stats.getMrr(), 0.001);
        assertEquals(73.5, stats.getLatencyP50Ms(), 0.001);
        assertEquals(246.1, stats.getLatencyP95Ms(), 0.001);
    }

    @Test
    public void testUrlNormalizationAndValidation() {
        assertEquals("https://example.com", PreferencesManager.normalizeUrl("https://example.com/"));
        assertEquals("http://192.168.1.10:8000", PreferencesManager.normalizeUrl(" http://192.168.1.10:8000/// "));

        assertTrue(PreferencesManager.isValidUrl("https://wikipedia-search-engine-55an.onrender.com"));
        assertTrue(PreferencesManager.isValidUrl("http://192.168.1.10:8000"));
        assertFalse(PreferencesManager.isValidUrl("not a url"));
        assertFalse(PreferencesManager.isValidUrl("ftp://something"));
    }
}
