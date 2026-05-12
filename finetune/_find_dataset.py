import json, os

os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"

# Try different dataset names that might contain civil law content
datasets_to_try = [
    "SooMe/JEC-QA",
    "lihuiche/GaoKao-Law",
    "xusenlin/LawGPT-data",
    "qgyd2021/Chinese_Legal_QA",
    "jialai/Chinese-legal-dataset",
    "nl2go/Legal-Data",
]

for ds_name in datasets_to_try:
    print(f"\n--- Trying: {ds_name} ---")
    try:
        from datasets import load_dataset
        dataset = load_dataset(ds_name, split="train", trust_remote_code=True, streaming=True)
        count = 0
        for i, item in enumerate(dataset):
            if i >= 3:
                break
            count = i + 1
        if count > 0:
            print(f"  ✅ Available! Sample keys: {list(dataset[0].keys())}")
            # print first record preview
            first = dataset[0]
            for k, v in first.items():
                if isinstance(v, str):
                    print(f"  {k}: {v[:100]}...")
                else:
                    print(f"  {k}: {v}")
    except Exception as e:
        print(f"  ❌ {str(e)[:100]}")