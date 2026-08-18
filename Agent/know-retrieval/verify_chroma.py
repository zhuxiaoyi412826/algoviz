import chromadb
from chromadb.config import Settings

client = chromadb.PersistentClient(
    path="data/chroma_db",
    settings=Settings(anonymized_telemetry=False, allow_reset=True),
)

col = client.get_collection("interview_problems")
print(f"Collection: {col.name}")
print(f"Vector count: {col.count()}")

samples = col.get(limit=3, include=["documents", "metadatas"])
ids = samples.get("ids", [])
metas = samples.get("metadatas", [])
docs = samples.get("documents", [])

for i in range(min(3, len(ids))):
    print(f"\n  [{i}] id={ids[i]}")
    if i < len(metas) and metas[i]:
        m = metas[i]
        print(f"      title: {m.get('title', 'N/A')}")
        print(f"      category: {m.get('category', 'N/A')}")
        print(f"      tags: {m.get('tags', 'N/A')}")
        print(f"      difficulty: {m.get('difficulty', 'N/A')}")
    if i < len(docs) and docs[i]:
        print(f"      doc: {docs[i][:100]}...")
