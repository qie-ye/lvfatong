import json
from pathlib import Path

batch_file = Path(__file__).parent / "data" / "batches" / "batch_01.json"
if not batch_file.exists():
    print(f"文件不存在：{batch_file}")
    exit(1)

with open(batch_file, "r", encoding="utf-8") as f:
    data = json.load(f)

# 检查instruction和input的唯一性
instructions = set()
inputs = set()
output_lens = []

for item in data:
    instructions.add(item["instruction"])
    inputs.add(item["input"])
    output_lens.append(len(item["output"]))

print("唯一instruction数量:", len(instructions))
print("唯一input(问题)数量:", len(inputs))
print("output平均长度:", sum(output_lens)/len(output_lens))
print()

# 列出所有唯一的问题
print("所有问题类型:")
for q in sorted(inputs):
    print("  -", q)

print()
print("前5条output样本:")
for i in range(5):
    print("--- 第{}条 output前200字 ---".format(i+1))
    print(data[i]["output"][:200])
    print()