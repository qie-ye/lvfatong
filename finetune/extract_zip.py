# -*- coding: utf-8 -*-
import zipfile
import os
from pathlib import Path

zip_file = Path("data/CAIL2018_ALL_DATA.zip")
extract_dir = Path("data/CAIL2018")

if not zip_file.exists():
    print(f"Zip file not found: {zip_file}")
    exit(1)

extract_dir.mkdir(parents=True, exist_ok=True)

print(f"Extracting {zip_file} to {extract_dir}...")

with zipfile.ZipFile(zip_file, 'r') as zip_ref:
    zip_ref.extractall(extract_dir)

print("Extraction complete!")

# List extracted files
for root, dirs, files in os.walk(extract_dir):
    level = root.replace(str(extract_dir), '').count(os.sep)
    indent = ' ' * 2 * level
    print(f'{indent}{os.path.basename(root)}/')
    subindent = ' ' * 2 * (level + 1)
    for file in files[:10]:  # Only show first 10 files
        print(f'{subindent}{file}')
    if len(files) > 10:
        print(f'{subindent}... and {len(files) - 10} more files')
