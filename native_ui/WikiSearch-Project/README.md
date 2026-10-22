# WikiSearch — Android Studio Native Wikipedia Search Engine Client

**WikiSearch** is a production-quality native Android application built in Java 17 that interfaces with a high-performance custom Wikipedia Search Engine backend. It showcases clean software architecture, Okapi BM25 relevance scoring, exact positional phrase intersection, an **8-Step Engine Inspector**, article reading with estimated read times, live search engine statistics, and customizable network and appearance settings.

---

## Architecture & Technology Stack

- **Platform:** Native Android (Min SDK 24, Compile SDK 34, Target SDK 34)
- **Language:** Java 17
- **UI Framework:** Native Android Views & XML Layouts (`ConstraintLayout`, `CoordinatorLayout`, `RecyclerView`, `MaterialComponents`)
- **Design System:** **Mint & Graphite** color system with comprehensive Day/Night theme qualifiers
- **Networking:** Native `HttpURLConnection` with multi-threaded background `ExecutorService` and main-thread `Handler` callbacks (zero heavy third-party networking overhead)
- **Data Serialization:** Robust, defensive parsing using `org.json.JSONObject` resilient against API evolution
- **Package Name:** `com.hcl.wikisearch`

---

## Features

1. **Dual Search Modes:**
   - **BM25 Keywords (`mode=keyword`):** Okapi BM25 relevance ranking with length normalization and term frequency weighting.
   - **Exact Phrase (`mode=phrase`):** Two-pointer linear positional phrase verification ensuring strict adjacency (`pos(next) == pos(current) + 1`).
2. **Real-time Latency & Result Summary:**
   - Populated directly from backend responses (e.g. `⚡ 3.4 ms · 1,420 hits`), preventing fabricated metrics.
3. **Quick Topic Chips:**
   - Fast exploration for prominent Wikipedia topics: *Albert Einstein*, *Solar system*, *Quantum mechanics*, *Theory of relativity*.
4. **8-Step Engine Inspector:**
   - Chronological breakdown of search execution pipeline:
     1. **Tokenizer:** Positional regex tokenization and case-folding.
     2. **Lexicon:** Dictionary binary search with postings offsets and chunk sizes.
     3. **Compression:** Delta + Variable-Byte (VByte) decompression throughput and savings.
     4. **Phrase Intersection:** Two-pointer positional verification and false-positive elimination.
     5. **BM25 Scoring:** Okapi BM25 formula, length penalty, term scores, and title multipliers.
     6. **Min-Heap:** Bounded binary min-heap selection ($O(N \log K)$ vs full quicksort) with candidate leaderboard.
     7. **Document Store:** Direct binary disk jump seek latency, byte spans, and raw previews.
     8. **Snippet Generation:** Sliding window density maximization and whole-word boundary snapping.
5. **Article Reader Activity:**
   - Full article view with readable typography, estimated reading time, word count, doc ID, and secure "Open in Wikipedia" browser intent (with scheme and host validation).
6. **Live Engine Statistics Bottom Sheet:**
   - Fetches live `/api/stats` reporting total articles, vocabulary size, Hit@10, Mean Reciprocal Rank (MRR), p50/p95 latency, and compression savings.
7. **Configurable Settings Dialog:**
   - Seamlessly toggle between Cloud server (`https://wikipedia-search-engine-55an.onrender.com`) and Custom/Local Development IP (e.g. `http://192.168.1.10:8000`).
   - Built-in "Test Connection" tool to ping candidate backends with live round-trip latency reporting.
   - Appearance theme switcher (System Default, Light Theme, Dark Theme).
   - Scoped Android Network Security Config permitting HTTP only for trusted local IP ranges.

---

## Project Structure

```text
WikiSearch/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/hcl/wikisearch/
│       │   │   ├── MainActivity.java              # Main interface, search bar, mode toggle, dual tabs
│       │   │   ├── ArticleReaderActivity.java     # Dedicated full-article reading experience
│       │   │   ├── StatsBottomSheet.java          # Bottom sheet modal for live engine metrics
│       │   │   ├── SettingsDialogFragment.java    # Dialog for URL configuration and theme switching
│       │   │   ├── SearchResultAdapter.java       # RecyclerView adapter for search hit cards
│       │   │   ├── EngineInspectorAdapter.java    # 8-step expandable inspector adapter
│       │   │   ├── ApiClient.java                 # Centralized HttpURLConnection client with thread pool
│       │   │   ├── PreferencesManager.java        # SharedPreferences wrapper with URL normalization
│       │   │   └── models/
│       │   │       ├── SearchResult.java          # Single search hit model
│       │   │       ├── SearchResponse.java        # Complete search response model
│       │   │       ├── ExplainData.java           # 8 pipeline stages structured model
│       │   │       ├── ArticleData.java           # Full article model with reading time calculation
│       │   │       └── StatsData.java             # Search engine health and stats model
│       │   └── res/
│       │       ├── drawable/                      # Custom vector icons and shape drawables
│       │       ├── layout/                        # XML layouts for screens, dialogs, and items
│       │       ├── values/                        # Light theme colors (Mint & Graphite), strings, styles
│       │       ├── values-night/                  # Dark theme colors (Mint & Graphite)
│       │       └── xml/                           # Scoped network security config
│       └── test/java/com/hcl/wikisearch/
│           └── WikiSearchTest.java                # Comprehensive JVM unit tests
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
    └── libs.versions.toml
```

---

## Build & Run Instructions

### 1. Requirements
- JDK 17 (Java 17)
- Android SDK with Platform 34+ installed
- Android Studio Iguana / Jellyfish / Meerkat or later

### 2. Building the Project
From the project root:
```bash
./gradlew app:assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### 3. Running Unit Tests
```bash
./gradlew app:testDebugUnitTest
```
Runs 6 automated tests validating JSON parsing across all 4 endpoints, URL normalization, and reading time estimation.

### 4. Deploying to Device / Emulator
```bash
./gradlew app:installDebug
```
Or open the project in Android Studio, select your device or emulator, and click **Run** (Shift+F10).

---

## Backend Configuration & Local Setup

### Default Cloud Endpoint
By default, WikiSearch connects to:
`https://wikipedia-search-engine-55an.onrender.com`

*Note on Cloud Cold Starts:* If the Render cloud instance is dormant, the first request may take a few moments to spin up. WikiSearch displays a helpful status message informing the user while the server wakes up.

### Local Development Setup
If you run the search engine locally on your development machine (e.g. `uvicorn main:app --host 0.0.0.0 --port 8000`):
1. In the app, tap the **Settings** gear icon in the top right.
2. Select **Custom / Local Development**.
3. Enter your development computer's LAN IP (e.g. `http://192.168.1.10:8000` or `http://10.0.2.2:8000` for Android Emulator).
4. Tap **Test Connection** to verify network reachability.
5. Tap **Save Settings**.
