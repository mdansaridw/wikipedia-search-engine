"""
Wikipedia Search Engine - One-Click Server Launcher
Detects local Wi-Fi IP address and launches the FastAPI daemon
ready for physical Android phones and browsers to connect.
"""

import os
import sys
import socket
import uvicorn

def get_local_ip():
    """Detects the laptop's primary local network IP address."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Does not actually connect, just resolves the local interface used for routing
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

def main():
    local_ip = get_local_ip()
    port = 8000
    
    print("\n" + "=" * 65)
    print("  🚀 WIKIPEDIA LOCAL SEARCH ENGINE — SERVER LAUNCHER")
    print("=" * 65)
    print(f"  • Local Machine Access: http://localhost:{port}")
    print(f"  • Mobile Phone Access:  http://{local_ip}:{port}")
    print(f"  • Android Emulator:     http://10.0.2.2:{port}")
    print("-" * 65)
    print(f"  CONNECT YOUR PHONE: Ensure your phone and laptop are on the")
    print(f"  same Wi-Fi, then open: http://{local_ip}:{port} on your phone!")
    print("=" * 65 + "\n")
    
    uvicorn.run("backend.main:app", host="0.0.0.0", port=port, reload=False)

if __name__ == "__main__":
    main()
