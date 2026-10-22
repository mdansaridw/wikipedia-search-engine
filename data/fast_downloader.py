"""
High-Speed Parallel Multi-Threaded Range Downloader with Auto-Retry
Bypasses single-thread CDN bandwidth throttling by downloading
multiple byte ranges concurrently with automatic socket reconnection.
"""

import os
import sys
import time
import urllib.request
import concurrent.futures
import pyarrow.parquet as pq

DATASET_URL = "https://huggingface.co/api/datasets/answerdotai/simplewiki/parquet/articles/train/0.parquet"
OUTPUT_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "simplewiki.parquet")
NUM_THREADS = 8  # 8 balanced streams prevents socket drops while maximizing throughput


def get_direct_url_and_size(url: str):
    """Follows redirects to obtain the direct CDN URL and Content-Length."""
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=15) as res:
        direct_url = res.geturl()
        total_size = int(res.headers.get("Content-Length", 0))
    return direct_url, total_size


def download_range(direct_url: str, start: int, end: int, output_file: str, part_num: int):
    """Downloads a specific byte range with automatic retry on timeout."""
    headers = {
        "User-Agent": "Mozilla/5.0",
        "Range": f"bytes={start}-{end}"
    }
    target_bytes = end - start + 1
    
    for attempt in range(5):
        try:
            req = urllib.request.Request(direct_url, headers=headers)
            with urllib.request.urlopen(req, timeout=60) as response:
                received = 0
                with open(output_file, "r+b") as f:
                    f.seek(start)
                    while received < target_bytes:
                        block = response.read(min(256 * 1024, target_bytes - received))
                        if not block:
                            break
                        f.write(block)
                        received += len(block)
                return received
        except Exception as e:
            if attempt == 4:
                raise
            time.sleep(1 + attempt)


def parallel_download(url: str, output_path: str, num_threads: int = 8):
    print("Connecting to dataset source...")
    direct_url, total_size = get_direct_url_and_size(url)
    print(f"Target size: {total_size / (1024*1024):.1f} MB across {num_threads} concurrent worker streams.")
    
    # Pre-allocate output file size
    with open(output_path, "wb") as f:
        f.truncate(total_size)
        
    chunk_size = total_size // num_threads
    ranges = []
    for i in range(num_threads):
        start = i * chunk_size
        end = total_size - 1 if i == num_threads - 1 else (start + chunk_size - 1)
        ranges.append((start, end, i))
        
    start_time = time.time()
    downloaded = 0
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=num_threads) as executor:
        futures = {
            executor.submit(download_range, direct_url, r[0], r[1], output_path, r[2]): r
            for r in ranges
        }
        for future in concurrent.futures.as_completed(futures):
            bytes_done = future.result()
            downloaded += bytes_done
            elapsed = time.time() - start_time
            speed = (downloaded / (1024 * 1024)) / (elapsed if elapsed > 0 else 0.001)
            pct = (downloaded / total_size) * 100
            sys.stdout.write(f"\rProgress: [{downloaded / (1024*1024):.1f}/{total_size / (1024*1024):.1f} MB] ({pct:.1f}%) at {speed:.2f} MB/s")
            sys.stdout.flush()
            
    total_time = time.time() - start_time
    print(f"\nDownload completed successfully in {total_time:.1f}s ({((total_size / (1024*1024)) / total_time):.2f} MB/s)!")


def verify_dataset(file_path: str):
    print("\nVerifying dataset schema and row count with PyArrow...")
    parquet_file = pq.ParquetFile(file_path)
    metadata = parquet_file.metadata
    
    print(f"Total Rows (Articles): {metadata.num_rows:,}")
    print(f"Total Row Groups:       {metadata.num_row_groups}")
    print(f"Total Compressed Size:  {os.path.getsize(file_path) / (1024*1024):.1f} MB")
    print(f"Schema Columns:         {parquet_file.schema.names}")
    
    sample_batch = parquet_file.read_row_group(0, columns=["title"]).to_pydict()
    first_title = sample_batch["title"][0]
    print(f"Sample First Article:   '{first_title}'")
    print("Dataset verification PASSED! Ready for indexing.\n")


if __name__ == "__main__":
    parallel_download(DATASET_URL, OUTPUT_FILE, NUM_THREADS)
    verify_dataset(OUTPUT_FILE)
