"""
Render Keep-Alive Daemon
Pings the Render live backend every 10 minutes to prevent the free-tier service from sleeping.
"""

import urllib.request
import time
from datetime import datetime

URL = "https://wikipedia-search-engine-55an.onrender.com/api/stats"
INTERVAL_SECONDS = 600  # 10 minutes (Render sleeps after 15 minutes)

def ping():
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    try:
        t0 = time.time()
        req = urllib.request.Request(URL, headers={"User-Agent": "RenderKeepAlive/1.0"})
        with urllib.request.urlopen(req, timeout=45) as resp:
            elapsed = round((time.time() - t0) * 1000, 1)
            print(f"[{now}] Ping OK (HTTP {resp.getcode()}) in {elapsed} ms")
    except Exception as e:
        print(f"[{now}] Ping error: {e}")

if __name__ == "__main__":
    print(f"Starting Render Keep-Alive Daemon for: {URL}")
    print(f"Ping interval: {INTERVAL_SECONDS} seconds (10 mins)")
    while True:
        ping()
        time.sleep(INTERVAL_SECONDS)
