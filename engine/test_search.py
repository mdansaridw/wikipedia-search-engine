"""
Search Engine Test Harness
Runs live queries against the 270,509-article index and reports
retrieval latency, top ranks, and snippets.
"""

import os
import sys
import time

if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

# Ensure project root is in sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from engine.searcher import SearchEngine

def run_tests():
    engine = SearchEngine("index_data")
    
    test_queries = [
        ("Albert Einstein", "keyword"),
        ("Quantum mechanics", "keyword"),
        ("solar system", "phrase"),
        ("artificial intelligence", "phrase"),
        ("Theory of relativity", "keyword")
    ]
    
    for q, mode in test_queries:
        print(f"\n" + "=" * 60)
        print(f"QUERY: '{q}' [Mode: {mode.upper()}]")
        print("=" * 60)
        
        result = engine.search(q, mode=mode, limit=5)
        print(f"[SPEED] Latency: {result['latency_ms']} ms | Total Hits: {result['total_hits']:,}")
        
        for r in result['results']:
            print(f"  Rank #{r['rank']} | Score: {r['score']} | Title: {r['title']}")
            print(f"    Snippet: {r['snippet']}")
            
    engine.close()

if __name__ == "__main__":
    run_tests()
