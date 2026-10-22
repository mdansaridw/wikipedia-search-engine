import pandas as pd
import os

OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_FILE = os.path.join(OUTPUT_DIR, "simplewiki.parquet")

df = pd.read_parquet(OUTPUT_FILE)
pd.set_option('display.max_columns', None)
print(df.head())