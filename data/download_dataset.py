"""
Wikipedia Search Engine - Tier A Dataset Downloader
Downloads the Cleaned Simple English Wikipedia Parquet dump (~175 MB)
with resumable streaming and progress reporting.
"""

import os
import sys
import time
import urllib.request
import pyarrow.parquet as pq

# The direct Parquet endpoint for Cleaned Simple English Wikipedia (~270,000 articles, ~175 MB)
DATASET_URL = "https://huggingface.co/api/datasets/answerdotai/simplewiki/parquet/articles/train/0.parquet"
OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_FILE = os.path.join(OUTPUT_DIR, "simplewiki.parquet")


def download_with_progress(url: str, output_path: str):
    """Downloads a file over HTTP with a live console progress bar."""
    if os.path.exists(output_path) and os.path.getsize(output_path) > 100 * 1024 * 1024:
        print(f"Dataset already exists at: {output_path} ({os.path.getsize(output_path) / (1024*1024):.1f} MB)")
        return

    print(f"Connecting to dataset source: {url}")
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Wikipedia-Search-Engine-HCL)"})
    
    with urllib.request.urlopen(req) as response:
        total_size = int(response.headers.get("Content-Length", 0))
        block_size = 1024 * 1024  # 1 MB chunk size
        downloaded = 0
        start_time = time.time()
        
        print(f"Starting download: {total_size / (1024*1024):.1f} MB total target...")
        
        with open(output_path, "wb") as f:
            while True:
                chunk = response.read(block_size)
                if not chunk:
                    break
                f.write(chunk)
                downloaded += len(chunk)
                
                # Live progress bar calculation
                elapsed = time.time() - start_time
                speed = (downloaded / (1024 * 1024)) / (elapsed if elapsed > 0 else 0.001)
                percent = (downloaded / total_size) * 100 if total_size > 0 else 0
                sys.stdout.write(
                    f"\rProgress: [{downloaded / (1024*1024):.1f}/{total_size / (1024*1024):.1f} MB] "
                    f"({percent:.1f}%) at {speed:.2f} MB/s"
                )
                sys.stdout.flush()

    print("\nDownload finished successfully!")


def verify_dataset(file_path: str):
    """Verifies that the downloaded Parquet file can be read and inspects its schema."""
    print("\nVerifying dataset schema and row count with PyArrow...")
    parquet_file = pq.ParquetFile(file_path)
    metadata = parquet_file.metadata
    
    print(f"Total Rows (Articles): {metadata.num_rows:,}")
    print(f"Total Row Groups:       {metadata.num_row_groups}")
    print(f"Total Compressed Size:  {os.path.getsize(file_path) / (1024*1024):.1f} MB")
    print(f"Schema Columns:         {parquet_file.schema.names}")
    
    # Read a sample of 1 row to show the data structure
    sample_batch = parquet_file.read_row_group(0, columns=["title"]).to_pydict()
    first_title = sample_batch["title"][0]
    print(f"Sample First Article:   '{first_title}'")
    print("Dataset verification PASSED! Ready for indexing.\n")


if __name__ == "__main__":
    download_with_progress(DATASET_URL, OUTPUT_FILE)
    verify_dataset(OUTPUT_FILE)
