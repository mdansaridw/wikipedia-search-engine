"""
Tier D Index Compression: Delta Encoding & Variable-Byte (VByte) Packing
Compresses posting lists and positional sequences by 65-75% on disk
while maintaining sub-millisecond in-memory decompression speed.
"""

from typing import List


def delta_encode(sorted_numbers: List[int]) -> List[int]:
    """
    Converts a strictly increasing list of integers into difference gaps (D-gaps).
    
    Example:
        Input:  [100000, 100004, 100009, 100015, 100018]
        Output: [100000, 4, 5, 6, 3]
    """
    if not sorted_numbers:
        return []
    
    deltas = [sorted_numbers[0]]
    for i in range(1, len(sorted_numbers)):
        deltas.append(sorted_numbers[i] - sorted_numbers[i - 1])
    return deltas


def delta_decode(deltas: List[int]) -> List[int]:
    """
    Restores the original sorted numbers from difference gaps via running sum.
    
    Example:
        Input:  [100000, 4, 5, 6, 3]
        Output: [100000, 100004, 100009, 100015, 100018]
    """
    if not deltas:
        return []
    
    numbers = [deltas[0]]
    current = deltas[0]
    for d in deltas[1:]:
        current += d
        numbers.append(current)
    return numbers


def vbyte_encode(numbers: List[int]) -> bytes:
    """
    Packs a list of non-negative integers into Variable-Byte format.
    - Each byte stores 7 bits of data.
    - The 8th bit (most significant bit: 0x80) is a flag:
      * 1 = this is the last byte of the number
      * 0 = more bytes follow
      
    Numbers < 128 take only 1 single byte instead of 4 bytes (75% savings).
    """
    byte_stream = bytearray()
    
    for n in numbers:
        # Loop until number is smaller than 128 (7 bits)
        while n >= 128:
            # Append lower 7 bits with 8th bit as 0 (continuation flag)
            byte_stream.append(n & 0x7F)
            n >>= 7
        # Final byte has the 8th bit set to 1 (0x80) marking the end of this integer
        byte_stream.append(n | 0x80)
        
    return bytes(byte_stream)


def vbyte_decode(data: bytes) -> List[int]:
    """
    Unpacks Variable-Byte encoded bytes back into a list of integers.
    """
    numbers = []
    current_num = 0
    shift = 0
    
    for b in data:
        if b < 128:
            # 8th bit is 0: more bytes follow for this number
            current_num |= (b << shift)
            shift += 7
        else:
            # 8th bit is 1: this is the final byte of the number
            current_num |= ((b & 0x7F) << shift)
            numbers.append(current_num)
            current_num = 0
            shift = 0
            
    return numbers


def compress_posting_ids(sorted_doc_ids: List[int]) -> bytes:
    """Convenience helper: Delta-encodes then VByte-compresses a list of doc IDs."""
    deltas = delta_encode(sorted_doc_ids)
    return vbyte_encode(deltas)


def decompress_posting_ids(compressed_bytes: bytes) -> List[int]:
    """Convenience helper: VByte-decompresses then delta-decodes back to sorted doc IDs."""
    deltas = vbyte_decode(compressed_bytes)
    return delta_decode(deltas)
