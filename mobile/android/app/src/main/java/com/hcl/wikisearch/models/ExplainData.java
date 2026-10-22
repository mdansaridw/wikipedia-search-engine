package com.hcl.wikisearch.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExplainData implements Serializable {
    private final String query;
    private final String mode;
    private final double totalPipelineLatencyMs;

    private final TokenizerStep step1;
    private final LexiconStep step2;
    private final CompressionStep step3;
    private final PhraseIntersectionStep step4;
    private final Bm25Step step5;
    private final MinHeapStep step6;
    private final DocStoreStep step7;
    private final SnippetStep step8;

    public ExplainData(String query, String mode, double totalPipelineLatencyMs,
                       TokenizerStep step1, LexiconStep step2, CompressionStep step3,
                       PhraseIntersectionStep step4, Bm25Step step5, MinHeapStep step6,
                       DocStoreStep step7, SnippetStep step8) {
        this.query = query != null ? query : "";
        this.mode = mode != null ? mode : "";
        this.totalPipelineLatencyMs = totalPipelineLatencyMs;
        this.step1 = step1;
        this.step2 = step2;
        this.step3 = step3;
        this.step4 = step4;
        this.step5 = step5;
        this.step6 = step6;
        this.step7 = step7;
        this.step8 = step8;
    }

    public static ExplainData fromJson(JSONObject json) {
        if (json == null) return null;
        String query = json.optString("query", "");
        String mode = json.optString("mode", "");
        double totalLatency = json.optDouble("total_pipeline_latency_ms", 0.0);

        TokenizerStep s1 = TokenizerStep.fromJson(json.optJSONObject("step1_tokenizer"));
        LexiconStep s2 = LexiconStep.fromJson(json.optJSONObject("step2_lexicon"));
        CompressionStep s3 = CompressionStep.fromJson(json.optJSONObject("step3_compression"));
        PhraseIntersectionStep s4 = PhraseIntersectionStep.fromJson(json.optJSONObject("step4_phrase_intersection"));
        Bm25Step s5 = Bm25Step.fromJson(json.optJSONObject("step5_bm25_scoring"));
        MinHeapStep s6 = MinHeapStep.fromJson(json.optJSONObject("step6_min_heap"));
        DocStoreStep s7 = DocStoreStep.fromJson(json.optJSONObject("step7_doc_store"));
        SnippetStep s8 = SnippetStep.fromJson(json.optJSONObject("step8_snippet"));

        return new ExplainData(query, mode, totalLatency, s1, s2, s3, s4, s5, s6, s7, s8);
    }

    public String getQuery() { return query; }
    public String getMode() { return mode; }
    public double getTotalPipelineLatencyMs() { return totalPipelineLatencyMs; }
    public TokenizerStep getStep1() { return step1; }
    public LexiconStep getStep2() { return step2; }
    public CompressionStep getStep3() { return step3; }
    public PhraseIntersectionStep getStep4() { return step4; }
    public Bm25Step getStep5() { return step5; }
    public MinHeapStep getStep6() { return step6; }
    public DocStoreStep getStep7() { return step7; }
    public SnippetStep getStep8() { return step8; }

    // --- Step 1: Tokenizer ---
    public static class TokenizerStep implements Serializable {
        public final String title;
        public final String rawQuery;
        public final String engineRule;
        public final String caseFolding;
        public final String punctuationHandling;
        public final String queryMode;
        public final String detectedIntent;
        public final double latencyUs;
        public final List<TokenPosition> tokensWithPositions;
        public final List<String> extractedKeywords;

        public TokenizerStep(String title, String rawQuery, String engineRule, String caseFolding,
                             String punctuationHandling, String queryMode, String detectedIntent,
                             double latencyUs, List<TokenPosition> tokens, List<String> keywords) {
            this.title = title;
            this.rawQuery = rawQuery;
            this.engineRule = engineRule;
            this.caseFolding = caseFolding;
            this.punctuationHandling = punctuationHandling;
            this.queryMode = queryMode;
            this.detectedIntent = detectedIntent;
            this.latencyUs = latencyUs;
            this.tokensWithPositions = tokens != null ? tokens : Collections.emptyList();
            this.extractedKeywords = keywords != null ? keywords : Collections.emptyList();
        }

        public static TokenizerStep fromJson(JSONObject json) {
            if (json == null) return null;
            String title = json.optString("title", "1. Query Input & Positional Tokenizer");
            String rawQuery = json.optString("raw_query", "");
            String engineRule = json.optString("engine_rule", "");
            String caseFolding = json.optString("case_folding", "");
            String punct = json.optString("punctuation_handling", "");
            String qMode = json.optString("query_mode", "");
            String intent = json.optString("detected_intent", "");
            double latencyUs = json.optDouble("latency_us", 0.0);

            List<TokenPosition> tokens = new ArrayList<>();
            JSONArray arr = json.optJSONArray("tokens_with_positions");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.optJSONObject(i);
                    if (item != null) {
                        tokens.add(new TokenPosition(item.optString("token", ""), item.optInt("position", 0)));
                    }
                }
            }

            List<String> keywords = new ArrayList<>();
            JSONArray kwArr = json.optJSONArray("extracted_keywords");
            if (kwArr != null) {
                for (int i = 0; i < kwArr.length(); i++) {
                    keywords.add(kwArr.optString(i, ""));
                }
            }

            return new TokenizerStep(title, rawQuery, engineRule, caseFolding, punct, qMode, intent, latencyUs, tokens, keywords);
        }
    }

    public static class TokenPosition implements Serializable {
        public final String token;
        public final int position;

        public TokenPosition(String token, int position) {
            this.token = token;
            this.position = position;
        }
    }

    // --- Step 2: Lexicon ---
    public static class LexiconStep implements Serializable {
        public final String title;
        public final String vocabularySize;
        public final String lookupAlgorithm;
        public final String formula;
        public final double latencyMs;
        public final List<LexiconTerm> terms;

        public LexiconStep(String title, String vocabularySize, String lookupAlgorithm,
                           String formula, double latencyMs, List<LexiconTerm> terms) {
            this.title = title;
            this.vocabularySize = vocabularySize;
            this.lookupAlgorithm = lookupAlgorithm;
            this.formula = formula;
            this.latencyMs = latencyMs;
            this.terms = terms != null ? terms : Collections.emptyList();
        }

        public static LexiconStep fromJson(JSONObject json) {
            if (json == null) return null;
            String title = json.optString("title", "2. Lexicon Binary Search");
            String vocabSize = json.optString("vocabulary_size", "");
            String lookup = json.optString("lookup_algorithm", "");
            String formula = json.optString("formula", "");
            double latencyMs = json.optDouble("latency_ms", 0.0);

            List<LexiconTerm> terms = new ArrayList<>();
            JSONArray arr = json.optJSONArray("terms");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj != null) {
                        terms.add(new LexiconTerm(
                                obj.optString("term", ""),
                                obj.optLong("df", 0),
                                obj.optString("percentage_of_corpus", ""),
                                obj.optDouble("idf", 0.0),
                                obj.optString("rarity", ""),
                                obj.optString("postings_offset", ""),
                                obj.optLong("chunk_size_bytes", 0),
                                obj.optDouble("chunk_size_kb", 0.0)
                        ));
                    }
                }
            }
            return new LexiconStep(title, vocabSize, lookup, formula, latencyMs, terms);
        }
    }

    public static class LexiconTerm implements Serializable {
        public final String term;
        public final long df;
        public final String percentageOfCorpus;
        public final double idf;
        public final String rarity;
        public final String postingsOffset;
        public final long chunkSizeBytes;
        public final double chunkSizeKb;

        public LexiconTerm(String term, long df, String percentageOfCorpus, double idf,
                           String rarity, String postingsOffset, long chunkSizeBytes, double chunkSizeKb) {
            this.term = term;
            this.df = df;
            this.percentageOfCorpus = percentageOfCorpus;
            this.idf = idf;
            this.rarity = rarity;
            this.postingsOffset = postingsOffset;
            this.chunkSizeBytes = chunkSizeBytes;
            this.chunkSizeKb = chunkSizeKb;
        }
    }

    // --- Step 3: Compression ---
    public static class CompressionStep implements Serializable {
        public final String title;
        public final String algorithm;
        public final String bitPackingScheme;
        public final String totalCompressedBytesRead;
        public final String uncompressedFootprintEstimate;
        public final String diskSavings;
        public final String decompressionThroughput;
        public final String bitVisualizationSample;
        public final double latencyMs;

        public CompressionStep(String title, String algorithm, String bitPackingScheme,
                               String totalCompressedBytesRead, String uncompressedFootprintEstimate,
                               String diskSavings, String decompressionThroughput,
                               String bitVisualizationSample, double latencyMs) {
            this.title = title;
            this.algorithm = algorithm;
            this.bitPackingScheme = bitPackingScheme;
            this.totalCompressedBytesRead = totalCompressedBytesRead;
            this.uncompressedFootprintEstimate = uncompressedFootprintEstimate;
            this.diskSavings = diskSavings;
            this.decompressionThroughput = decompressionThroughput;
            this.bitVisualizationSample = bitVisualizationSample;
            this.latencyMs = latencyMs;
        }

        public static CompressionStep fromJson(JSONObject json) {
            if (json == null) return null;
            return new CompressionStep(
                    json.optString("title", "3. VByte & Delta Decompression"),
                    json.optString("algorithm", ""),
                    json.optString("bit_packing_scheme", ""),
                    json.optString("total_compressed_bytes_read", ""),
                    json.optString("uncompressed_footprint_estimate", ""),
                    json.optString("disk_savings", ""),
                    json.optString("decompression_throughput", ""),
                    json.optString("bit_visualization_sample", ""),
                    json.optDouble("latency_ms", 0.0)
            );
        }
    }

    // --- Step 4: Phrase Intersection ---
    public static class PhraseIntersectionStep implements Serializable {
        public final String title;
        public final boolean tested;
        public final String algorithm;
        public final String condition;
        public final String phrase;
        public final long matchedArticlesCount;
        public final long falsePositivesEliminated;
        public final double latencyMs;

        public PhraseIntersectionStep(String title, boolean tested, String algorithm,
                                      String condition, String phrase, long matchedArticlesCount,
                                      long falsePositivesEliminated, double latencyMs) {
            this.title = title;
            this.tested = tested;
            this.algorithm = algorithm;
            this.condition = condition;
            this.phrase = phrase;
            this.matchedArticlesCount = matchedArticlesCount;
            this.falsePositivesEliminated = falsePositivesEliminated;
            this.latencyMs = latencyMs;
        }

        public static PhraseIntersectionStep fromJson(JSONObject json) {
            if (json == null) return null;
            return new PhraseIntersectionStep(
                    json.optString("title", "4. Two-Pointer Positional Phrase Verification"),
                    json.optBoolean("tested", false),
                    json.optString("algorithm", ""),
                    json.optString("condition", ""),
                    json.optString("phrase", ""),
                    json.optLong("matched_articles_count", 0),
                    json.optLong("false_positives_eliminated", 0),
                    json.optDouble("latency_ms", 0.0)
            );
        }
    }

    // --- Step 5: BM25 Scoring ---
    public static class Bm25Step implements Serializable {
        public final String title;
        public final String formula;
        public final long winningDocId;
        public final String winningDocTitle;
        public final long docLength;
        public final double lengthPenalty;
        public final List<TermCalculation> termCalculations;
        public final String canonicalBonus;
        public final String phraseProximityBonus;
        public final double finalScore;
        public final double latencyMs;

        public Bm25Step(String title, String formula, long winningDocId, String winningDocTitle,
                        long docLength, double lengthPenalty, List<TermCalculation> termCalculations,
                        String canonicalBonus, String phraseProximityBonus, double finalScore, double latencyMs) {
            this.title = title;
            this.formula = formula;
            this.winningDocId = winningDocId;
            this.winningDocTitle = winningDocTitle;
            this.docLength = docLength;
            this.lengthPenalty = lengthPenalty;
            this.termCalculations = termCalculations != null ? termCalculations : Collections.emptyList();
            this.canonicalBonus = canonicalBonus;
            this.phraseProximityBonus = phraseProximityBonus;
            this.finalScore = finalScore;
            this.latencyMs = latencyMs;
        }

        public static Bm25Step fromJson(JSONObject json) {
            if (json == null) return null;
            String title = json.optString("title", "5. Okapi BM25 Relevance Scoring");
            String formula = json.optString("formula", "");
            long winDocId = json.optLong("winning_doc_id", 0);
            String winDocTitle = json.optString("winning_doc_title", "");
            long docLen = json.optLong("doc_length", 0);
            double lenPenalty = json.optDouble("length_penalty", 0.0);
            String canonicalBonus = json.optString("canonical_bonus", "");
            String proxBonus = json.optString("phrase_proximity_bonus", "");
            double finalScore = json.optDouble("final_score", 0.0);
            double latencyMs = json.optDouble("latency_ms", 0.0);

            List<TermCalculation> terms = new ArrayList<>();
            JSONArray arr = json.optJSONArray("term_calculations");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj != null) {
                        terms.add(new TermCalculation(
                                obj.optString("term", ""),
                                obj.optLong("tf", 0),
                                obj.optDouble("idf", 0.0),
                                obj.optString("title_boost", ""),
                                obj.optDouble("term_score", 0.0),
                                obj.optDouble("raw_bm25", 0.0)
                        ));
                    }
                }
            }
            return new Bm25Step(title, formula, winDocId, winDocTitle, docLen, lenPenalty, terms, canonicalBonus, proxBonus, finalScore, latencyMs);
        }
    }

    public static class TermCalculation implements Serializable {
        public final String term;
        public final long tf;
        public final double idf;
        public final String titleBoost;
        public final double termScore;
        public final double rawBm25;

        public TermCalculation(String term, long tf, double idf, String titleBoost, double termScore, double rawBm25) {
            this.term = term;
            this.tf = tf;
            this.idf = idf;
            this.titleBoost = titleBoost;
            this.termScore = termScore;
            this.rawBm25 = rawBm25;
        }
    }

    // --- Step 6: Min-Heap ---
    public static class MinHeapStep implements Serializable {
        public final String title;
        public final String algorithm;
        public final long totalCandidatesStreamed;
        public final long candidatesPrunedAndDiscarded;
        public final String speedupVsFullSort;
        public final List<LeaderboardCandidate> leaderboard;
        public final double latencyMs;

        public MinHeapStep(String title, String algorithm, long totalCandidatesStreamed,
                           long candidatesPrunedAndDiscarded, String speedupVsFullSort,
                           List<LeaderboardCandidate> leaderboard, double latencyMs) {
            this.title = title;
            this.algorithm = algorithm;
            this.totalCandidatesStreamed = totalCandidatesStreamed;
            this.candidatesPrunedAndDiscarded = candidatesPrunedAndDiscarded;
            this.speedupVsFullSort = speedupVsFullSort;
            this.leaderboard = leaderboard != null ? leaderboard : Collections.emptyList();
            this.latencyMs = latencyMs;
        }

        public static MinHeapStep fromJson(JSONObject json) {
            if (json == null) return null;
            String title = json.optString("title", "6. Top-K Min-Heap Selection");
            String algo = json.optString("algorithm", "");
            long streamed = json.optLong("total_candidates_streamed", 0);
            long pruned = json.optLong("candidates_pruned_and_discarded", 0);
            String speedup = json.optString("speedup_vs_full_sort", "");
            double latencyMs = json.optDouble("latency_ms", 0.0);

            List<LeaderboardCandidate> list = new ArrayList<>();
            JSONArray arr = json.optJSONArray("top_candidates_leaderboard");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj != null) {
                        list.add(new LeaderboardCandidate(
                                obj.optInt("rank", i + 1),
                                obj.optLong("doc_id", 0),
                                obj.optString("title", ""),
                                obj.optDouble("score", 0.0)
                        ));
                    }
                }
            }
            return new MinHeapStep(title, algo, streamed, pruned, speedup, list, latencyMs);
        }
    }

    public static class LeaderboardCandidate implements Serializable {
        public final int rank;
        public final long docId;
        public final String title;
        public final double score;

        public LeaderboardCandidate(int rank, long docId, String title, double score) {
            this.rank = rank;
            this.docId = docId;
            this.title = title;
            this.score = score;
        }
    }

    // --- Step 7: Document Store ---
    public static class DocStoreStep implements Serializable {
        public final String title;
        public final String inputWinnerFromStep6;
        public final String addressTableEntry;
        public final String diskByteSpan;
        public final long payloadSizeBytes;
        public final double payloadSizeKb;
        public final long articleLengthWords;
        public final long articleLengthChars;
        public final String ioMechanism;
        public final String ramFootprint;
        public final String rawBytePreview;
        public final String directDiskSeekLatency;

        public DocStoreStep(String title, String inputWinnerFromStep6, String addressTableEntry,
                            String diskByteSpan, long payloadSizeBytes, double payloadSizeKb,
                            long articleLengthWords, long articleLengthChars, String ioMechanism,
                            String ramFootprint, String rawBytePreview, String directDiskSeekLatency) {
            this.title = title;
            this.inputWinnerFromStep6 = inputWinnerFromStep6;
            this.addressTableEntry = addressTableEntry;
            this.diskByteSpan = diskByteSpan;
            this.payloadSizeBytes = payloadSizeBytes;
            this.payloadSizeKb = payloadSizeKb;
            this.articleLengthWords = articleLengthWords;
            this.articleLengthChars = articleLengthChars;
            this.ioMechanism = ioMechanism;
            this.ramFootprint = ramFootprint;
            this.rawBytePreview = rawBytePreview;
            this.directDiskSeekLatency = directDiskSeekLatency;
        }

        public static DocStoreStep fromJson(JSONObject json) {
            if (json == null) return null;
            return new DocStoreStep(
                    json.optString("title", "7. Binary Document Store Direct Disk Jump"),
                    json.optString("input_winner_from_step6", ""),
                    json.optString("address_table_entry", ""),
                    json.optString("disk_byte_span", ""),
                    json.optLong("payload_size_bytes", 0),
                    json.optDouble("payload_size_kb", 0.0),
                    json.optLong("article_length_words", 0),
                    json.optLong("article_length_chars", 0),
                    json.optString("io_mechanism", ""),
                    json.optString("ram_footprint", ""),
                    json.optString("raw_byte_preview", ""),
                    json.optString("direct_disk_seek_latency", "")
            );
        }
    }

    // --- Step 8: Snippet Generation ---
    public static class SnippetStep implements Serializable {
        public final String title;
        public final String sourceArticle;
        public final long sourceTextLength;
        public final String algorithm;
        public final int windowSizeChars;
        public final String wordBoundarySnapping;
        public final String finalRenderedSnippet;
        public final double latencyMs;

        public SnippetStep(String title, String sourceArticle, long sourceTextLength,
                           String algorithm, int windowSizeChars, String wordBoundarySnapping,
                           String finalRenderedSnippet, double latencyMs) {
            this.title = title;
            this.sourceArticle = sourceArticle;
            this.sourceTextLength = sourceTextLength;
            this.algorithm = algorithm;
            this.windowSizeChars = windowSizeChars;
            this.wordBoundarySnapping = wordBoundarySnapping;
            this.finalRenderedSnippet = finalRenderedSnippet;
            this.latencyMs = latencyMs;
        }

        public static SnippetStep fromJson(JSONObject json) {
            if (json == null) return null;
            return new SnippetStep(
                    json.optString("title", "8. Dynamic Snippet Extractor"),
                    json.optString("source_article", ""),
                    json.optLong("source_text_length", 0),
                    json.optString("algorithm", ""),
                    json.optInt("window_size_chars", 160),
                    json.optString("word_boundary_snapping", ""),
                    json.optString("final_rendered_snippet", ""),
                    json.optDouble("latency_ms", 0.0)
            );
        }
    }
}
