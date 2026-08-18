"""
Chroma 向量库数据恢复脚本
从 MySQL 获取所有面试题，重新生成向量并写入 Chroma

用法:
    python restore_chroma.py
"""
import os
import sys
import time

# 添加当前目录到路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# ==================== 配置 ====================
MYSQL_HOST = "localhost"
MYSQL_PORT = 3306
MYSQL_USER = "root"
MYSQL_PASSWORD = "412826"
MYSQL_DB = "algoviz"

CHROMA_PATH = "data/chroma_db"
COLLECTION_NAME = "interview_problems"

# ==================== 依赖检查 ====================
try:
    import pymysql
except ImportError:
    print("请先安装 pymysql: pip install pymysql")
    sys.exit(1)

try:
    import chromadb
    from chromadb.config import Settings
except ImportError:
    print("请先安装 chromadb: pip install chromadb")
    sys.exit(1)

try:
    from app.embeddings import embed_text
except ImportError:
    print("无法导入 embeddings 模块，请确保在 know-retrieval 目录下运行")
    sys.exit(1)

# ==================== 主逻辑 ====================
def main():
    print("=" * 60)
    print("  Chroma 向量库数据恢复")
    print("=" * 60)

    # 1. 连接 MySQL
    print("\n[1/4] 连接 MySQL 数据库...")
    try:
        conn = pymysql.connect(
            host=MYSQL_HOST, port=MYSQL_PORT,
            user=MYSQL_USER, password=MYSQL_PASSWORD,
            database=MYSQL_DB, charset="utf8mb4"
        )
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        cursor.execute("SELECT COUNT(*) as cnt FROM interview_problem WHERE is_deleted = 0")
        total = cursor.fetchone()["cnt"]
        print(f"  MySQL 中共有 {total} 道面试题")
    except Exception as e:
        print(f"  MySQL 连接失败: {e}")
        return

    # 2. 获取所有题目
    print("\n[2/4] 获取面试题数据...")
    cursor.execute("""
        SELECT id, problem_no, title, tags, category, difficulty, description, solution
        FROM interview_problem
        WHERE is_deleted = 0
        ORDER BY id ASC
    """)
    problems = cursor.fetchall()
    print(f"  获取到 {len(problems)} 道题目")
    conn.close()

    if not problems:
        print("  没有题目数据，退出")
        return

    # 3. 连接 Chroma
    print("\n[3/4] 连接 Chroma 向量库...")
    client = chromadb.PersistentClient(
        path=CHROMA_PATH,
        settings=Settings(anonymized_telemetry=False, allow_reset=True),
    )

    # 删除旧 collection（如果存在）
    try:
        client.delete_collection(COLLECTION_NAME)
        print(f"  已删除旧 collection: {COLLECTION_NAME}")
    except Exception:
        pass

    # 创建新 collection
    collection = client.create_collection(
        name=COLLECTION_NAME,
        metadata={"hnsw:space": "cosine", "description": "面试题目向量"},
    )
    print(f"  已创建新 collection: {COLLECTION_NAME}")

    # 4. 批量向量化并写入
    print(f"\n[4/4] 向量化并写入 {len(problems)} 道题目...")
    print("  （使用 bge-small-zh-v1.5 模型，512 维向量）")

    success = 0
    failed = 0
    batch_size = 50
    start_time = time.time()

    def build_text(p):
        parts = []
        if p.get("title"):
            parts.append(f"题目：{p['title']}")
        if p.get("category"):
            parts.append(f"分类：{p['category']}")
        if p.get("tags"):
            parts.append(f"标签：{p['tags']}")
        if p.get("difficulty"):
            parts.append(f"难度：{p['difficulty']}")
        if p.get("description"):
            desc = p["description"]
            if len(desc) > 800:
                desc = desc[:800]
            parts.append(f"描述：{desc}")
        if p.get("solution"):
            sol = p["solution"]
            if len(sol) > 500:
                sol = sol[:500]
            parts.append(f"解答：{sol}")
        return "\n".join(parts)

    def build_metadata(p):
        return {
            "problem_id": p["id"],
            "problem_no": p.get("problem_no", ""),
            "title": p.get("title", ""),
            "tags": p.get("tags", ""),
            "category": p.get("category", ""),
            "difficulty": p.get("difficulty", ""),
        }

    # 分批处理
    for i in range(0, len(problems), batch_size):
        batch = problems[i:i + batch_size]
        batch_ids = []
        batch_embeddings = []
        batch_documents = []
        batch_metadatas = []

        for p in batch:
            try:
                text = build_text(p)
                if not text.strip():
                    failed += 1
                    continue

                # 生成向量
                vec = embed_text(text)

                batch_ids.append(str(p["id"]))
                batch_embeddings.append(vec)
                batch_documents.append(text)
                batch_metadatas.append(build_metadata(p))
                success += 1
            except Exception as e:
                failed += 1
                if failed <= 5:  # 只打印前 5 个失败
                    print(f"  [失败] id={p['id']} title={p.get('title', '')[:30]}: {e}")

        # 写入 Chroma
        if batch_ids:
            try:
                collection.upsert(
                    ids=batch_ids,
                    embeddings=batch_embeddings,
                    documents=batch_documents,
                    metadatas=batch_metadatas,
                )
            except Exception as e:
                print(f"  [批次写入失败] {e}")

        progress = min(i + batch_size, len(problems))
        elapsed = time.time() - start_time
        if elapsed > 0:
            rate = progress / elapsed
            eta = (len(problems) - progress) / rate if rate > 0 else 0
            print(f"  进度: {progress}/{len(problems)} | 速率: {rate:.1f} 题/秒 | 预计剩余: {eta:.0f}秒")

    # 完成
    elapsed = time.time() - start_time
    final_count = collection.count()

    print("\n" + "=" * 60)
    print(f"  恢复完成！")
    print(f"  成功: {success} | 失败: {failed}")
    print(f"  Chroma 中向量总数: {final_count}")
    print(f"  耗时: {elapsed:.1f} 秒")
    print("=" * 60)


if __name__ == "__main__":
    main()
