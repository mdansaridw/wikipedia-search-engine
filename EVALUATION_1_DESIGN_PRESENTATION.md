# 🚀 HCL HACKATHON — EVALUATION 1: SYSTEM DESIGN & ARCHITECTURE LAYOUT

**Problem 2: Wikipedia Search Engine ("Find the right page among millions, in milliseconds")**  
**Presentation Time**: 5 Minutes Pitch + 3 Minutes Q&A  
**Target**: Demonstrate absolute technical mastery, systems thinking, and a clear path to 100/100 points.

---

## 1. Executive Summary & Problem Scope

We are building a **custom-coded, high-performance local Wikipedia search engine** designed from first principles with **zero reliance on Lucene or Elasticsearch**.

### Core Capabilities Being Delivered:
1. **Dual Search Modes**:
   - **BM25 Keyword Search** with term frequency saturation, document length normalization, and title boosting.
   - **Exact-Phrase Search** powered by a **positional inverted index** with strict consecutive-word validation.
2. **Systems-Level Efficiency**:
   - Memory-efficient streaming ingestion (bounds RAM usage below 500 MB).
   - **Tier D Index Compression**: Delta encoding + Variable-Byte (VByte) compression to shrink on-disk footprint by >60%.
   - Memory-mapped files (`mmap`) for instant sub-10ms lookup times.
3. **Rigorous Measurement**:
   - Automated benchmarking harness reporting **Hit@10, MRR (Mean Reciprocal Rank), p50 and p95 query latency** across 100+ queries.
4. **End-to-End Delivery**:
   - A fast, decoupled **FastAPI local search daemon** serving a polished **Android APK** over local Wi-Fi.

---

## 2. System Architecture Diagram

```
[ ANDROID MOBILE CLIENT (APK) ]
  ├── Instant Search Bar (Keywords & "Exact Phrases")
  ├── Latency & Score Badges (e.g., "Rank #1 • 8.4ms • BM25: 14.2")
  ├── Dynamic Snippet with Keyword Highlighting
  └── Full Article Reader Modal
            │
            ▼ (HTTP REST over Local Wi-Fi: e.g., http://192.168.1.X:8000)
[ FASTAPI LOCAL SEARCH SERVER ]
  ├── GET /search?q={query}&mode={keyword|phrase}&limit=10
  ├── GET /article/{id}
  └── GET /benchmarks/stats
            │
  ┌─────────┴───────────────────────────────────────────┐
  │                                                     │
  ▼                                                     ▼
[ ONLINE QUERY PROCESSOR ]                 [ OFFLINE INDEX PIPELINE ]
  ├── Query Lexer & Quoted-Phrase Parser     ├── Parquet / XML Streaming Ingester
  ├── O(1) Lexicon Dictionary Lookup         ├── Tokenizer, Case Normalizer & Filter
  ├── Postings Intersector (Phrases)         ├── Positional Inverted Indexer (SPIMI)
  ├── BM25 Scorer with Title Boost           ├── Delta & Variable-Byte (VByte) Compressor
  ├── Top-K Min-Heap (K=10)                  └── Forward Document Store & Metadata Table
  └── Dynamic Snippet Window Extractor                  │
            │                                           │
            └─────────────────┬─────────────────────────┘
                              ▼
                   [ ON-DISK STORAGE LAYER ]
                     ├── terms.dict    (Lexicon & File Offsets)
                     ├── postings.bin  (Compressed DocIDs + Positions)
                     ├── doc_meta.bin  (Doc Lengths & AvgDL)
                     └── docs.store    (Cleaned Article Text & Titles)
```

---

## 3. How the System Works: Two Operational Flows

### A. The Offline Indexing Flow (High Throughput & Low Memory)
1. **Streaming Ingestion**: The corpus reader streams articles in batches from the Parquet dump (Tier A: 269k articles) or raw XML (Tier B). It never loads the entire corpus into memory.
2. **Positional Tokenization**: Each document is parsed into tokens, recording not just the document ID and count, but the exact 0-indexed position:
   $$\text{Index Entry}: \text{term} \longrightarrow \big[ (\text{doc\_id}_1, [pos_1, pos_2]), (\text{doc\_id}_2, [pos_3]), \dots \big]$$
3. **Chunked Inversion (SPIMI)**: When memory fills to a defined threshold (e.g. 200MB), an inverted block is sorted and flushed to a temporary segment on disk.
4. **Compression & Final Merge**: All segments merge into a single `postings.bin` using:
   - **Sorted DocID Delta Encoding**: Store $D_i - D_{i-1}$ rather than raw 32-bit integers.
   - **Variable-Byte (VByte) Encoding**: 1-byte representation for small values, slashing posting size.
5. **Metadata Generation**: Calculates total collection size $N$ and average document length $\text{avgdl}$, saving to `doc_meta.bin`.

### B. The Online Query Serving Flow (Sub-10ms Latency)
1. **Query Parsing**: The user enters either freeform terms (`machine learning`) or quoted phrases (`"artificial intelligence"`).
2. **Fast Dictionary Lookup**: Terms are resolved against `terms.dict` to get the exact byte offset in `postings.bin`.
3. **Posting Traversal**:
   - **For Keyword Queries**: Reads posting lists for each query term and accumulates BM25 scores directly into a size-10 Min-Heap.
   - **For Exact-Phrase Queries**: Performs a linear positional merge on documents containing all terms:
     $$\text{Match iff } \exists p \in P_{t1} \text{ such that } (p + 1) \in P_{t2}, \dots, (p + k - 1) \in P_{tk}$$
4. **Scoring & Ranking**:
   - BM25 score computed with $k_1 = 1.2$ and $b = 0.75$.
   - **Title Multiplier**: Hits occurring in the article title receive a $3.0\times$ weight boost.
5. **Dynamic Snippet Generation**: Computes a 160-character sliding window around the highest-density match in the article text.
6. **Delivery**: Returns ranked JSON results with latency, rank, score, title, snippet, and doc ID to the Android APK.

---

## 4. Key Engineering Decisions & Why We Win

| Decision | Alternative Considered | Why Our Choice Is Superior |
| :--- | :--- | :--- |
| **Custom Python Engine with NumPy/Struct** | Pre-built Lucene / Elasticsearch | **Strict hackathon rule compliance**; full transparency over every CPU cycle and byte layout. |
| **Positional Inverted Index** | Bag-of-words (Term Frequency only) | Unlocks **Tier A+ Exact Phrase Search** without requiring expensive secondary scans of raw article text. |
| **VByte + Delta Compression** | Raw 32-bit / 64-bit integer arrays | Saves **60–75% disk space**, maximizes OS page cache efficiency, directly scoring Tier D bonus points. |
| **FastAPI + Polished Android APK** | CLI-only or Web-only | Fulfills mandatory APK requirement while keeping heavy computation on the laptop; provides live visual latency proof. |
| **Fail-Safe Offline Mode** | Relying on live Wikipedia APIs | Zero external network risk during hidden-query judging; runs 100% locally. |

---

## 5. Metrics & Benchmark Plan (Honest Numbers)

We will provide a self-contained benchmark script `run_benchmark.py` that judges can execute with one command:
- **Hit@10**: Tests 100 canonical Wikipedia queries (e.g., "Albert Einstein", "Theory of Relativity", "Photosynthesis") and checks if the ground truth page is in Top 10.
- **MRR (Mean Reciprocal Rank)**: Computes $\frac{1}{|Q|} \sum_{i=1}^{|Q|} \frac{1}{\text{rank}_i}$.
- **Latency**: Measures p50 and p95 across 100+ warm and cold queries using `time.perf_counter_ns()`.
- **System Footprint**: Automatically reports Index Build Time, Uncompressed Corpus Size vs. Compressed Index Size on disk, and Index Ratio.

---

## 6. Speaking Script for Evaluation 1 (Follow This Word-for-Word)

> *"Good morning judges. We are presenting our architecture for Problem 2: The Wikipedia Search Engine.*
>
> *Our design objective is to answer keyword and exact-phrase queries across hundreds of thousands of Wikipedia articles in single-digit milliseconds, using entirely our own custom indexing and ranking algorithms.*
>
> *Here is how our system is engineered:*
>
> *First, for the index structure, rather than a simple bag-of-words index, we engineered a **Positional Inverted Index**. Each term maps to sorted document IDs and exact word positions. This allows our engine to perform both probabilistic **BM25 keyword search** and deterministic **exact-phrase matching** via positional intersection, completely satisfying Tier A and Tier A+.*
>
> *Second, for storage and systems efficiency, we implement **Tier D compression** using delta-encoding combined with Variable-Byte packing. This shrinks our index by over 60%, allowing it to be memory-mapped directly into the OS page cache for sub-10ms query latencies.*
>
> *Third, our ranking formula is an optimized BM25 with length normalization ($b=0.75$), term saturation ($k_1=1.2$), and an additional title-proximity bonus that optimizes our MRR and Hit@10 metrics.*
>
> *Finally, our user interface is a native Android APK connected to our local search server over Wi-Fi, featuring real-time latency telemetry, snippet highlights, and an article reader.*
>
> *We have mapped all 100 points of the judging rubric into our architecture. Immediately following this evaluation, we will download the Tier A Parquet dataset and begin building the core index and benchmark pipeline."*
