import json
from pathlib import Path

dataset_dir = Path(__file__).parent / "data"
DATA_CONFIG = "dataset_info.json"
path = dataset_dir / DATA_CONFIG
print("Reading from:", path.resolve())
print("File exists:", path.exists())

if os.path.exists(path):
    with open(path, encoding="utf-8") as f:
        d = json.load(f)
    print("Total datasets:", len(d))
    print("lawgpt_batch_1:", "lawgpt_batch_1" in d)
    if "lawgpt_batch_1" in d:
        print("Config:", json.dumps(d["lawgpt_batch_1"], ensure_ascii=False))
