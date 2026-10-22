"""
High-Performance Search & BM25 Ranking Engine with Positional Phrase Search
- Dual search modes: Probabilistic BM25 keyword search + Exact-phrase positional verification.
- Title weight multiplier (3.0x) + Exact title match bonus for maximum MRR / Hit@10.
- Top-K Min-Heap selection (K=10) for sub-3ms query latency.
- Dynamic 160-character snippet extraction around matched terms.
"""

import os
import sys
import math
import struct
import heapq
import time
from array import array
from typing import List, Dict, Tuple, Optional

# Ensure project root is in sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from engine.doc_store import DocStoreReader
from engine.tokenizer import tokenize_terms, parse_query
from engine.compressor import vbyte_decode, delta_decode


class SearchEngine:
    def __init__(self, index_dir: str):
        self.index_dir = index_dir
        self.terms_dict_path = os.path.join(index_dir, "terms.dict")
        self.postings_path = os.path.join(index_dir, "postings.bin")
        self.meta_path = os.path.join(index_dir, "doc_meta.bin")
        
        print("Initializing Search Engine & loading index structures...")
        start_time = time.time()
        
        # 1. Initialize Document Store Reader for snippets and full text
        self.doc_store = DocStoreReader(index_dir)
        
        # 2. Open postings.bin in binary read mode
        self.postings_file = open(self.postings_path, "rb")
        
        # 3. Load doc metadata (total_docs, avgdl, doc_lengths)
        self._load_metadata()
        
        # 4. Load terms lexicon dictionary into memory
        self._load_terms_dict()
        
        init_time = (time.time() - start_time) * 1000
        print(f"Search Engine ready! Loaded {len(self.terms):,} terms & {self.N:,} docs in {init_time:.1f}ms.\n")

    def _load_metadata(self):
        """Loads collection size N, average document length avgdl, and doc length table."""
        with open(self.meta_path, "rb") as f:
            header = f.read(8)
            self.N, self.avgdl = struct.unpack('<If', header)
            
            raw_doc_ids = array('I')
            raw_doc_ids.fromfile(f, self.N)
            
            self.doc_lengths = array('I')
            self.doc_lengths.fromfile(f, self.N)
            
        # Map doc_id to its index in doc_lengths for O(1) length lookup
        self.id_to_idx = {doc_id: i for i, doc_id in enumerate(raw_doc_ids)}

    def _load_terms_dict(self):
        """
        Loads terms.dict into memory:
        term -> (document_frequency, byte_offset, byte_length)
        """
        self.terms: Dict[str, Tuple[int, int, int]] = {}
        with open(self.terms_dict_path, "rb") as f:
            total_terms = struct.unpack('<I', f.read(4))[0]
            for _ in range(total_terms):
                term_len = struct.unpack('<H', f.read(2))[0]
                term = f.read(term_len).decode('utf-8')
                df, offset, length = struct.unpack('<IQI', f.read(16))
                self.terms[term] = (df, offset, length)

    def get_postings(self, term: str) -> List[Tuple[int, int, List[int]]]:
        """
        Retrieves and decompresses posting list for a term:
        Returns list of (doc_id, tf, [positions...])
        """
        entry = self.terms.get(term)
        if not entry:
            return []
            
        df, offset, length = entry
        self.postings_file.seek(offset)
        compressed_bytes = self.postings_file.read(length)
        
        # VByte decompression
        numbers = vbyte_decode(compressed_bytes)
        
        # Unpack delta-encoded doc_ids and positions
        postings = []
        i = 0
        last_doc_id = 0
        while i < len(numbers):
            delta_id = numbers[i]
            doc_id = last_doc_id + delta_id
            last_doc_id = doc_id
            tf = numbers[i + 1]
            i += 2
            
            pos_deltas = numbers[i:i + tf]
            positions = delta_decode(pos_deltas)
            i += tf
            
            postings.append((doc_id, tf, positions))
            
        return postings

    def compute_idf(self, df: int) -> float:
        """Standard Robertson-Spärck Jones probabilistic BM25 IDF formula."""
        return math.log(((self.N - df + 0.5) / (df + 0.5)) + 1.0)

    def search(self, query: str, mode: str = "keyword", limit: int = 10) -> Dict:
        """
        Executes search across the corpus with millisecond latency.
        - mode="keyword": BM25 scoring with title boost.
        - mode="phrase": Exact-phrase positional verification.
        """
        t0 = time.perf_counter_ns()
        
        # Parse query for keywords and quoted exact phrases
        keywords, phrases = parse_query(query)
        if mode == "phrase" and not phrases and keywords:
            # If user explicitly selected phrase mode, treat the whole query as a phrase
            phrases = [keywords]
            
        k1 = 1.2
        b = 0.75
        scores = {}
        phrase_matched_docs = set()
        
        # Handle Exact-Phrase Search (Two-Pointer Positional Intersection)
        if phrases:
            for phrase in phrases:
                matched_in_phrase = self._match_exact_phrase(phrase)
                if not phrase_matched_docs:
                    phrase_matched_docs = matched_in_phrase
                else:
                    phrase_matched_docs &= matched_in_phrase
                    
            if mode == "phrase" and not phrase_matched_docs:
                elapsed_ms = (time.perf_counter_ns() - t0) / 1_000_000
                return {"query": query, "mode": mode, "latency_ms": round(elapsed_ms, 2), "total_hits": 0, "results": []}

        # Collect terms to score
        all_terms = keywords[:]
        for phrase in phrases:
            all_terms.extend(phrase)
        all_terms = list(dict.fromkeys(all_terms))  # Deduplicate terms
        
        # BM25 Keyword Scoring
        clean_query_str = " ".join(all_terms)
        
        # Stopword optimization: If a query has multiple terms, ignore ultra-frequent terms (DF > 35% of corpus)
        # such as "of", "the", "in", "is" to prevent evaluating 220,000 irrelevant documents in Python.
        active_terms = all_terms
        if len(all_terms) > 1 and mode != "phrase":
            filtered = [t for t in all_terms if self.terms.get(t, (0,0,0))[0] < 0.35 * self.N]
            if filtered:
                active_terms = filtered

        for term in active_terms:
            entry = self.terms.get(term)
            if not entry:
                continue
                
            df, _, _ = entry
            idf = self.compute_idf(df)
            postings = self.get_postings(term)
            
            for doc_id, tf, positions in postings:
                if mode == "phrase" and doc_id not in phrase_matched_docs:
                    continue
                    
                idx = self.id_to_idx.get(doc_id)
                doc_len = self.doc_lengths[idx] if idx is not None else self.avgdl
                
                # BM25 saturation and length normalization
                numerator = tf * (k1 + 1.0)
                denominator = tf + k1 * (1.0 - b + b * (doc_len / self.avgdl))
                bm25_term_score = idf * (numerator / denominator)
                
                # Title Boost (Decision 5): Fast title match bonus
                title = self.doc_store.get_title(doc_id) or ""
                title_lower = title.lower()
                
                if term in title_lower:
                    bm25_term_score *= 3.0  # 3.0x Title Multiplier Boost
                    
                # Exact title match bonus
                if title_lower == clean_query_str or title_lower == query.strip().lower():
                    bm25_term_score += 50.0  # Guarantees canonical page hits Rank #1
                    
                # Phrase proximity bonus if doc matches exact phrase
                if doc_id in phrase_matched_docs:
                    bm25_term_score += 25.0
                    
                scores[doc_id] = scores.get(doc_id, 0.0) + bm25_term_score

        # Top-K Min-Heap selection (K=limit)
        min_heap = []
        for doc_id, score in scores.items():
            if len(min_heap) < limit:
                heapq.heappush(min_heap, (score, doc_id))
            else:
                if score > min_heap[0][0]:
                    heapq.heapreplace(min_heap, (score, doc_id))
                    
        # Sort top-K in descending score order
        top_k = sorted(min_heap, key=lambda x: x[0], reverse=True)
        
        # Build results with dynamic snippets
        results = []
        for rank, (score, doc_id) in enumerate(top_k, start=1):
            doc_data = self.doc_store.get(doc_id)
            if doc_data:
                title, text = doc_data
                snippet = self._generate_snippet(text, all_terms)
                results.append({
                    "rank": rank,
                    "doc_id": doc_id,
                    "title": title,
                    "score": round(score, 2),
                    "snippet": snippet
                })
                
        elapsed_ms = (time.perf_counter_ns() - t0) / 1_000_000
        
        return {
            "query": query,
            "mode": mode,
            "latency_ms": round(elapsed_ms, 2),
            "total_hits": len(scores),
            "results": results
        }

    def _match_exact_phrase(self, phrase_terms: List[str]) -> set:
        """
        Two-Pointer Positional Intersection for exact phrase matching.
        Verifies that consecutive words appear at offset + 1.
        """
        if len(phrase_terms) < 2:
            return set()
            
        # Get postings for all words in the phrase
        term_postings = []
        for term in phrase_terms:
            postings = self.get_postings(term)
            if not postings:
                return set()  # Word doesn't exist in corpus -> phrase cannot match
            # Create dict: doc_id -> sorted positions
            term_postings.append({doc_id: pos for doc_id, _, pos in postings})
            
        # Intersect doc_ids that contain all terms
        common_docs = set(term_postings[0].keys())
        for tp in term_postings[1:]:
            common_docs &= set(tp.keys())
            if not common_docs:
                return set()
                
        # For each candidate doc, verify consecutive positions
        phrase_matches = set()
        for doc_id in common_docs:
            # Start with positions of the first word
            valid_positions = term_postings[0][doc_id]
            
            for word_idx in range(1, len(phrase_terms)):
                next_positions = term_postings[word_idx][doc_id]
                new_valid = []
                
                # Two-pointer linear scan checking: pos_next == pos_prev + 1
                i, j = 0, 0
                while i < len(valid_positions) and j < len(next_positions):
                    target = valid_positions[i] + 1
                    if next_positions[j] == target:
                        new_valid.append(next_positions[j])
                        i += 1
                        j += 1
                    elif next_positions[j] < target:
                        j += 1
                    else:
                        i += 1
                        
                valid_positions = new_valid
                if not valid_positions:
                    break
                    
            if valid_positions:
                phrase_matches.add(doc_id)
                
        return phrase_matches

    def _generate_snippet(self, text: str, query_terms: List[str], window_size: int = 160) -> str:
        """
        Extracts a clean, dynamic 160-character snippet window around the first
        match of any query term.
        """
        if not text:
            return ""
            
        lower_text = text.lower()
        first_match_idx = -1
        
        for term in query_terms:
            idx = lower_text.find(term)
            if idx != -1:
                if first_match_idx == -1 or idx < first_match_idx:
                    first_match_idx = idx
                    
        if first_match_idx == -1:
            # Fallback to the first window_size characters of the article
            snippet = text[:window_size].strip()
            return snippet + "..." if len(text) > window_size else snippet
            
        start = max(0, first_match_idx - 40)
        end = min(len(text), start + window_size)
        
        # Adjust start and end to word boundaries
        if start > 0:
            space_idx = text.find(' ', start)
            if space_idx != -1 and space_idx < start + 15:
                start = space_idx + 1
                
        snippet = text[start:end].replace('\n', ' ').strip()
        prefix = "..." if start > 0 else ""
        suffix = "..." if end < len(text) else ""
        return f"{prefix}{snippet}{suffix}"

    def close(self):
        self.postings_file.close()
        self.doc_store.close()
