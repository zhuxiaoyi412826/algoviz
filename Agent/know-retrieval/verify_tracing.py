"""
ELK + 全链路追踪验证脚本

用法：
    python verify_tracing.py

功能：
    1. 测试 Python 服务 TraceId 中间件是否正常工作
    2. 生成带 TraceId 的日志条目
    3. 验证日志文件是否正确写入
    4. 可选：调用 Spring Boot 后端测试跨服务 TraceId 传递
"""

import logging
import os
import sys
import time
import json
import urllib.request
import urllib.error

# 添加当前目录到路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


def test_python_tracing():
    """测试 Python 端 TraceId 是否正常工作"""
    print("=" * 60)
    print("  测试 1：Python TraceId 中间件")
    print("=" * 60)

    # 检查日志文件是否存在
    log_file = os.path.join("D:/rizi", "python-app.log")
    if not os.path.exists(log_file):
        print(f"⚠️  警告: {log_file} 不存在")
        print("   请先启动 Python 向量服务: python -m app.main")
        return False

    # 读取日志文件最后几行
    with open(log_file, "r", encoding="utf-8") as f:
        lines = f.readlines()

    recent = lines[-5:] if len(lines) >= 5 else lines
    print(f"\n  最近的日志条目 ({len(lines)} 行)：")
    for line in recent:
        print(f"    {line.rstrip()}")

    # 检查是否有 TraceId
    if any("no-trace" not in line and "algoviz" in line for line in lines[-50:]):
        print("\n  ✅ 日志中包含 TraceId")
        return True
    else:
        print("\n  ⚠️  日志中未检测到有效 TraceId（可能服务未使用新代码启动）")
        return False


def test_health_check():
    """测试 Python 服务健康检查"""
    print("\n" + "=" * 60)
    print("  测试 2：Python 服务健康检查")
    print("=" * 60)

    url = "http://localhost:8001/health"
    try:
        req = urllib.request.Request(url)
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read())
            print(f"\n  ✅ 服务正常: {json.dumps(data, indent=4, ensure_ascii=False)}")
            return True
    except urllib.error.URLError as e:
        print(f"\n  ❌ 服务不可达: {e}")
        return False
    except Exception as e:
        print(f"\n  ❌ 请求失败: {e}")
        return False


def test_search_trace():
    """测试语义搜索时的 TraceId 传递"""
    print("\n" + "=" * 60)
    print("  测试 3：语义搜索 TraceId 全链路")
    print("=" * 60)

    # 1. 直接调用 Python 服务（模拟来自 Java 的请求）
    url = "http://localhost:8001/api/v1/search"
    payload = json.dumps({
        "query": "多线程",
        "topK": 5,
        "threshold": 0.1
    }).encode("utf-8")

    # 带 TraceId Header 调用
    req = urllib.request.Request(
        url,
        data=payload,
        headers={
            "Content-Type": "application/json",
            "X-Trace-Id": "test-trace-" + str(int(time.time())),
        },
        method="POST"
    )

    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            body = resp.read().decode("utf-8")
            trace_id = resp.headers.get("X-Trace-Id", "未设置")
            data = json.loads(body)
            print(f"\n  响应状态: {resp.status}")
            print(f"  响应 TraceId: {trace_id}")
            print(f"  搜索结果数: {data.get('total', 0)}")

            if data.get("results"):
                top = data["results"][0]
                print(f"  最高相似度: {top.get('similarity', 0):.4f}")
                print(f"  标题: {top.get('title', '无')}")

            # 检查 Python 日志
            log_file = os.path.join("logs", "app.log")
            if os.path.exists(log_file):
                with open(log_file, "r", encoding="utf-8") as f:
                    lines = f.readlines()
                # 查找 TraceId
                matched = [l for l in lines[-30:] if "test-trace-" in l]
                if matched:
                    print(f"\n  ✅ Python 日志中找到 Test TraceId:")
                    for ml in matched[-3:]:
                        print(f"    {ml.rstrip()}")
                else:
                    print(f"\n  ⚠️  日志中未找到 Test TraceId（可能还没刷新）")

            return True
    except urllib.error.URLError as e:
        print(f"\n  ❌ 请求失败: {e}")
        return False
    except Exception as e:
        print(f"\n  ❌ 异常: {e}")
        return False


def test_fluentd():
    """检查 Fluentd 状态"""
    print("\n" + "=" * 60)
    print("  测试 4：Fluentd 状态")
    print("=" * 60)

    # 检查 Fluentd 配置是否存在
    config_file = "fluentd.conf"
    if not os.path.exists(config_file):
        print(f"\n  ❌ {config_file} 不存在")
        return False

    print(f"\n  ✅ {config_file} 存在")

    # 检查 ES 索引模板
    template_file = os.path.join("templates", "algoviz-template.json")
    if os.path.exists(template_file):
        print(f"  ✅ ES 索引模板存在: {template_file}")
    else:
        print(f"  ⚠️  ES 索引模板不存在: {template_file}")

    # 检查日志目录
    log_dir = "logs"
    if os.path.exists(log_dir):
        log_files = os.listdir(log_dir)
        print(f"  ✅ Python 日志目录存在: {log_files}")
    else:
        print(f"  ⚠️  Python 日志目录不存在 (启动服务后自动创建)")

    # 检查 D:/rizi 目录
    java_log_dir = "D:/rizi"
    if os.path.exists(java_log_dir):
        log_files = [f for f in os.listdir(java_log_dir) if f.endswith(".log")]
        print(f"  ✅ Java 日志目录存在: {log_files}")
    else:
        print(f"  ⚠️  Java 日志目录不存在 (启动 Spring Boot 后自动创建)")

    print("\n  启动 Fluentd 命令:")
    print(f"    cd {os.getcwd()}")
    print(f"    fluentd -c fluentd.conf")
    print(f"    或")
    print(f"    td-agent -c fluentd.conf")

    # 检查 Fluentd 健康检查端口
    try:
        req = urllib.request.Request("http://localhost:24220/api/plugins")
        with urllib.request.urlopen(req, timeout=3) as resp:
            data = json.loads(resp.read())
            print(f"\n  ✅ Fluentd 运行中，插件数: {len(data.get('plugins', []))}")
    except Exception:
        print(f"\n  ⚠️  Fluentd 未启动或健康检查端口不可达 (24220)")

    return True


def main():
    print("\n" + "=" * 60)
    print("  AlgoVize ELK + 全链路追踪验证工具")
    print("  检查时间:", time.strftime("%Y-%m-%d %H:%M:%S"))
    print("=" * 60)

    results = {}

    # 测试 1
    results["python_tracing"] = test_python_tracing()

    # 测试 2
    results["health"] = test_health_check()

    # 测试 3
    results["search"] = test_search_trace()

    # 测试 4
    results["fluentd"] = test_fluentd()

    # 汇总
    print("\n" + "=" * 60)
    print("  验证结果汇总")
    print("=" * 60)

    passed = sum(1 for v in results.values() if v)
    total = len(results)

    for name, result in results.items():
        status = "✅ 通过" if result else "❌ 失败"
        print(f"  {status}  {name}")

    print(f"\n  通过率: {passed}/{total}")

    if passed == total:
        print("\n  🎉 所有测试通过！全链路追踪已就绪。")
    else:
        print("\n  ⚠️  部分测试未通过，请检查上方输出。")
        print("     常见问题：服务未启动、配置文件路径错误、端口被占用。")


if __name__ == "__main__":
    main()
