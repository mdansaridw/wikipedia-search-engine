"""
Positional Tokenizer & Query Lexer (Method B: Compiled C-Regex)
Extracts lowercase alphanumeric tokens and tracks strictly contiguous
word positions for Tier A+ exact-phrase search and BM25 ranking.
"""

import re
from typing import List, Tuple

# Compiled C-level regular expression matching sequences of alphanumeric characters.
# Skips punctuation, brackets, quotes, formatting symbols, and markdown noise.
TOKEN_PATTERN = re.compile(r'[a-z0-9]+')

# Regular expression to extract quoted phrases, e.g. "artificial intelligence"
QUOTED_PHRASE_PATTERN = re.compile(r'"([^"]+)"')


def tokenize_with_positions(text: str) -> List[Tuple[str, int]]:
    """
    Tokenizes raw text into a list of (token, position) tuples.
    Positions are strictly 0-indexed integers: 0, 1, 2, 3...
    
    Example:
        Input:  "Albert Einstein was born in Ulm, Germany!"
        Output: [("albert", 0), ("einstein", 1), ("was", 2), ("born", 3),
                 ("in", 4), ("ulm", 5), ("germany", 6)]
    """
    if not text:
        return []
    
    # Convert to lowercase and find all regex match objects
    lower_text = text.lower()
    tokens_with_positions = []
    
    # enumerate() assigns the exact sequential word position (0, 1, 2, ...)
    for pos, match in enumerate(TOKEN_PATTERN.finditer(lower_text)):
        tokens_with_positions.append((match.group(), pos))
        
    return tokens_with_positions


def tokenize_terms(text: str) -> List[str]:
    """
    Fast extraction of words only (without position integers).
    Used for query parsing, title matching, and vocabulary counting.
    """
    if not text:
        return []
    return TOKEN_PATTERN.findall(text.lower())


def parse_query(raw_query: str) -> Tuple[List[str], List[List[str]]]:
    """
    Parses a user query into freeform keywords and exact quoted phrases.
    
    Examples:
        - 'machine learning' 
          -> keywords: ['machine', 'learning'], phrases: []
        - '"solar system" planets'
          -> keywords: ['planets'], phrases: [['solar', 'system']]
    """
    if not raw_query:
        return [], []
        
    # Extract any quoted phrases: "quantum physics" -> ['quantum physics']
    quoted = QUOTED_PHRASE_PATTERN.findall(raw_query)
    phrases = []
    for q in quoted:
        phrase_terms = tokenize_terms(q)
        if len(phrase_terms) > 1:
            phrases.append(phrase_terms)
            
    # Remove the quoted phrases from the raw query to find remaining freeform terms
    unquoted_text = QUOTED_PHRASE_PATTERN.sub(' ', raw_query)
    keywords = tokenize_terms(unquoted_text)
    
    return keywords, phrases
