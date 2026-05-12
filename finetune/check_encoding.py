import json
from pathlib import Path

batch_file = Path(__file__).parent / "data" / "batches" / "batch_01.json"
if not batch_file.exists():
    print(f"文件不存在：{batch_file}")
    exit(1)

with open(batch_file, "r", encoding="utf-8") as f:
    data = json.load(f)

item = data[0]
print("instruction:", repr(item["instruction"][:30]))
print("input:", repr(item["input"]))
print("output:", repr(item["output"][:50]))
print()
print("文件编码验证通过！中文正常。")