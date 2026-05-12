import json
from pathlib import Path

batch_file = Path(__file__).parent / "data" / "batches" / "batch_01.json"
if not batch_file.exists():
    print(f"文件不存在：{batch_file}")
    exit(1)

with open(batch_file, "r", encoding="utf-8") as f:
    data = json.load(f)

print("总条数:", len(data))
print()

for i in range(3):
    item = data[i]
    print("--- 第{}条 ---".format(i+1))
    print("instruction:", item["instruction"][:80])
    print("input:", item["input"])
    out = item["output"]
    print("output:", out[:150] if len(out) > 150 else out)
    print()