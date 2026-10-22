"""
Inverted Index Builder (SPIMI In-Memory Fast Accumulation + VByte Flush)
Streams 270,000+ Wikipedia articles from Parquet, builds the binary document
store, generates positional inverted postings with Delta-VByte compression,
and persists `terms.dict`, `postings.bin`, and `doc_meta.bin`.
"""

import os
import sys
import time
import struct
from collections import defaultdict
from array import array

# Ensure project root is in sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

import pyarrow.parquet as pq
from engine.doc_store import DocStoreWriter
from engine.tokenizer import tokenize_with_positions, tokenize_terms
from engine.compressor import vbyte_encode, delta_encode


class IndexBuilder:
    def __init__(self, parquet_path: str, output_dir: str):
        self.parquet_path = parquet_path
        self.output_dir = output_dir
        os.makedirs(output_dir, exist_ok=True)
        
        # In-memory dictionary: term -> list of (doc_id, [pos1, pos2, ...])
        self.postings = defaultdict(list)
        
        # Document metadata arrays
        self.doc_lengths = array('I')
        self.doc_ids = array('I')
        self.doc_titles = []
        self.total_docs = 0
        self.total_words = 0

    def build_index(self, max_docs: int = None):
        """
        Streams articles, populates the Document Store, builds positional postings,
        and flushes the compressed index to disk.
        """
        print(f"Starting Inverted Index Build from: {self.parquet_path}")
        start_time = time.time()
        
        # Initialize Document Store Writer
        doc_store = DocStoreWriter(self.output_dir)
        
        parquet_file = pq.ParquetFile(self.parquet_path)
        batch_count = 0
        
        # Stream in batches of 25,000 rows to keep memory usage low and smooth
        for batch in parquet_file.iter_batches(batch_size=25000, columns=['article_id', 'title', 'md']):
            batch_count += 1
            batch_dict = batch.to_pydict()
            
            ids = batch_dict['article_id']
            titles = batch_dict['title']
            texts = batch_dict['md']
            
            for i in range(len(ids)):
                doc_id = ids[i]
                title = titles[i] or ""
                text = texts[i] or ""
                
                # 1. Save raw document to binary document store
                doc_store.add_document(doc_id, title, text)
                
                # 2. Tokenize with word positions
                tokens_with_pos = tokenize_with_positions(text)
                doc_len = len(tokens_with_pos)
                
                # Record metadata
                self.doc_ids.append(doc_id)
                self.doc_lengths.append(doc_len)
                self.doc_titles.append(title)
                self.total_docs += 1
                self.total_words += doc_len
                
                # 3. Accumulate term occurrences in this document
                # Group positions by term: term -> [p1, p2, ...]
                term_positions = defaultdict(list)
                for term, pos in tokens_with_pos:
                    term_positions[term].append(pos)
                    
                for term, positions in term_positions.items():
                    # Append (doc_id, positions) to this term's posting list
                    self.postings[term].append((doc_id, positions))
                    
                if max_docs and self.total_docs >= max_docs:
                    break
                    
            elapsed = time.time() - start_time
            rate = self.total_docs / (elapsed if elapsed > 0 else 0.001)
            print(f"  Ingested {self.total_docs:,} articles ({rate:,.0f} docs/sec) | Unique Terms: {len(self.postings):,}")
            
            if max_docs and self.total_docs >= max_docs:
                break

        # Finalize and close the Document Store
        doc_store.close()
        
        ingest_time = time.time() - start_time
        print(f"\nIngestion complete in {ingest_time:.1f}s. Now flushing compressed index to disk...")
        
        # Flush the compressed index to disk
        self._flush_index(start_time)

    def _flush_index(self, overall_start_time: float):
        """
        Compresses postings using Delta-Encoding + VByte and writes:
        - `terms.dict`
        - `postings.bin`
        - `doc_meta.bin`
        """
        flush_start = time.time()
        
        terms_dict_path = os.path.join(self.output_dir, "terms.dict")
        postings_bin_path = os.path.join(self.output_dir, "postings.bin")
        meta_path = os.path.join(self.output_dir, "doc_meta.bin")
        
        postings_file = open(postings_bin_path, "wb")
        terms_dict_file = open(terms_dict_path, "wb")
        
        # 1. Write total vocabulary count in terms.dict header
        sorted_terms = sorted(self.postings.keys())
        terms_dict_file.write(struct.pack('<I', len(sorted_terms)))
        
        total_raw_bytes = 0
        total_compressed_bytes = 0
        
        # 2. Iterate through sorted terms and write compressed postings
        for term in sorted_terms:
            doc_list = self.postings[term]
            df = len(doc_list)  # Document Frequency
            
            # Start byte offset in postings.bin
            start_offset = postings_file.tell()
            
            # Extract doc_ids and delta-encode them
            raw_doc_ids = [d[0] for d in doc_list]
            doc_id_deltas = delta_encode(raw_doc_ids)
            
            # Serialize posting data:
            # For each document: [delta_doc_id] [tf] [delta_positions...]
            posting_numbers = []
            for (delta_id, (doc_id, positions)) in zip(doc_id_deltas, doc_list):
                posting_numbers.append(delta_id)
                posting_numbers.append(len(positions))  # tf
                pos_deltas = delta_encode(positions)
                posting_numbers.extend(pos_deltas)
                
            raw_bytes_len = len(posting_numbers) * 4  # size if stored as raw 32-bit uints
            compressed_bytes = vbyte_encode(posting_numbers)
            
            postings_file.write(compressed_bytes)
            chunk_length = len(compressed_bytes)
            
            total_raw_bytes += raw_bytes_len
            total_compressed_bytes += chunk_length
            
            # Write entry to terms.dict:
            # [2-byte term length (H)] + [term UTF-8 bytes] + [4-byte DF (I)] + [8-byte offset (Q)] + [4-byte len (I)]
            term_bytes = term.encode('utf-8')
            terms_dict_file.write(struct.pack('<H', len(term_bytes)))
            terms_dict_file.write(term_bytes)
            terms_dict_file.write(struct.pack('<IQI', df, start_offset, chunk_length))
            
        postings_file.close()
        terms_dict_file.close()
        
        # 3. Write document metadata (avgdl, doc lengths, collection N)
        avgdl = (self.total_words / self.total_docs) if self.total_docs > 0 else 0.0
        with open(meta_path, "wb") as f:
            # Header: [4-byte N (I)] + [4-byte float avgdl (f)]
            f.write(struct.pack('<If', self.total_docs, avgdl))
            # doc_ids array
            self.doc_ids.tofile(f)
            # doc_lengths array
            self.doc_lengths.tofile(f)
            
        total_time = time.time() - overall_start_time
        
        # Report honest numbers
        terms_dict_size = os.path.getsize(terms_dict_path) / (1024 * 1024)
        postings_size = os.path.getsize(postings_bin_path) / (1024 * 1024)
        doc_store_size = os.path.getsize(os.path.join(self.output_dir, "docs.bin")) / (1024 * 1024)
        meta_size = os.path.getsize(meta_path) / (1024 * 1024)
        total_index_size = terms_dict_size + postings_size + meta_size
        
        corpus_size = os.path.getsize(self.parquet_path) / (1024 * 1024)
        compression_ratio = (total_index_size / corpus_size) * 100 if corpus_size > 0 else 0
        vbyte_saving = 100.0 - (total_compressed_bytes / total_raw_bytes * 100.0) if total_raw_bytes > 0 else 0
        
        print("\n" + "=" * 60)
        print("[SUCCESS] INVERTED INDEX BUILD COMPLETE!")
        print("=" * 60)
        print(f"Total Articles Indexed:    {self.total_docs:,}")
        print(f"Total Unique Vocabulary:   {len(sorted_terms):,} terms")
        print(f"Total Words Indexed:       {self.total_words:,} tokens")
        print(f"Average Document Length:   {avgdl:.1f} words")
        print(f"Total Build Time:          {total_time:.1f} seconds")
        print("-" * 60)
        print(f"Raw Corpus Size (Parquet): {corpus_size:.1f} MB")
        print(f"Postings File Size:        {postings_size:.1f} MB (VByte savings: {vbyte_saving:.1f}%)")
        print(f"Terms Dictionary Size:     {terms_dict_size:.1f} MB")
        print(f"Document Store Size:       {doc_store_size:.1f} MB")
        print(f"Total Search Index Size:   {total_index_size:.1f} MB")
        print(f"Index / Corpus Ratio:      {compression_ratio:.1f}%")
        print("=" * 60 + "\n")


if __name__ == "__main__":
    parquet_input = os.path.join("data", "simplewiki.parquet")
    output_directory = "index_data"
    
    # Test on full dataset (or set max_docs=50000 for quick test)
    builder = IndexBuilder(parquet_input, output_directory)
    builder.build_index()
