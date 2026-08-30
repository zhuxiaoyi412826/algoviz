# -*- coding: utf-8 -*-
"""
一次性模型转换脚本：把本地 HuggingFace 缓存的 bge-large-zh-v1.5 导出为 ONNX（model.onnx + vocab.txt）。
生成目录：C:/Users/Administrator/.cache/huggingface/hub/java-bge-large-zh-v1.5/（与 application.yml 对应）

用法：
    python export_onnx.py

前置依赖（只需已有 torch + transformers，无需 optimum）：
    pip show torch transformers   # 确认已安装

实现：
    直接调用 torch.onnx.export 导出 BertModel（输出 last_hidden_state），
    不依赖 optimum / transformers.onnx（它们的 API 已在新版本中变更或移除）。
"""
import glob
import os
import shutil
import sys

CACHE_MODEL = r"C:\Users\Administrator\.cache\huggingface\hub\bge-large-zh-v1.5"
HERE = os.path.dirname(os.path.abspath(__file__))
# 产物保存到独立的 Java 模型目录（与原 HF 模型分开）：
# C:/Users/Administrator/.cache/huggingface/hub/java-bge-large-zh-v1.5/model.onnx + vocab.txt
OUT_DIR = r"C:\Users\Administrator\.cache\huggingface\hub\java-bge-large-zh-v1.5"


def find_model_src() -> str:
    """定位 HF 缓存里的实际模型目录（snapshots/<hash>）。"""
    if os.path.isdir(CACHE_MODEL):
        snapshots = sorted(glob.glob(os.path.join(CACHE_MODEL, "snapshots", "*")),
                           key=os.path.getmtime, reverse=True)
        if snapshots:
            return snapshots[0]
        return CACHE_MODEL
    raise SystemExit(f"模型缓存路径不存在: {CACHE_MODEL}")


def export_with_torch(src: str, dst: str) -> bool:
    """用 torch.onnx.export 直接导出（最稳，不依赖 optimum）。"""
    try:
        import torch
        from transformers import AutoModel, AutoTokenizer

        print("[torch] 加载模型...")
        model = AutoModel.from_pretrained(src, local_files_only=True)
        tokenizer = AutoTokenizer.from_pretrained(src, local_files_only=True)
        model.eval()

        # 包装 forward，只输出 last_hidden_state（Java 端做 mean pooling）
        class ExportModel(torch.nn.Module):
            def __init__(self, m):
                super().__init__()
                self.m = m

            def forward(self, input_ids, attention_mask, token_type_ids):
                return self.m(input_ids=input_ids, attention_mask=attention_mask,
                              token_type_ids=token_type_ids).last_hidden_state

        dummy = tokenizer("算法 动态规划", return_tensors="pt",
                          max_length=128, padding="max_length", truncation=True)

        os.makedirs(dst, exist_ok=True)
        onnx_path = os.path.join(dst, "model.onnx")
        print("[torch] 导出 ONNX（326M 参数，可能需要一两分钟）...")
        torch.onnx.export(
            ExportModel(model),
            (dummy["input_ids"], dummy["attention_mask"], dummy["token_type_ids"]),
            onnx_path,
            input_names=["input_ids", "attention_mask", "token_type_ids"],
            output_names=["last_hidden_state"],
            dynamic_axes={
                "input_ids": {0: "batch", 1: "seq"},
                "attention_mask": {0: "batch", 1: "seq"},
                "token_type_ids": {0: "batch", 1: "seq"},
                "last_hidden_state": {0: "batch", 1: "seq"},
            },
            opset_version=13,
            do_constant_folding=True,
        )
        # 保存 tokenizer（含 vocab.txt）
        tokenizer.save_pretrained(dst)
        print("[torch] 导出成功:", onnx_path)
        merge_external_data(dst)
        return os.path.exists(onnx_path)
    except Exception as e:  # noqa
        print("[torch] 导出失败:", e)
        return False


def merge_external_data(dst: str):
    """torch 可能把大权重存成外部文件 model.onnx.data，Java onnxruntime 加载不稳定。
    用 onnx 库把外部数据合并回 model.onnx 单文件，并删除 .data。"""
    onnx_path = os.path.join(dst, "model.onnx")
    data_path = os.path.join(dst, "model.onnx.data")
    if not os.path.exists(data_path):
        return
    try:
        import onnx
        print("[onnx] 合并外部数据到单文件 model.onnx ...")
        model = onnx.load(onnx_path)
        # onnxruntime Java 1.17 最高支持 IR version 9，torch 新版导出 IR10 → 降级
        if getattr(model, "ir_version", 9) > 9:
            model.ir_version = 9
        onnx.save(model, onnx_path)  # 默认内联所有权重，输出单文件
        try:
            os.remove(data_path)
        except OSError:
            pass
        print(f"[onnx] 合并完成: model.onnx 单文件 ({os.path.getsize(onnx_path) / 1024 / 1024:.1f} MB, IR={model.ir_version})")
    except Exception as e:  # noqa
        print("[onnx] 合并失败（请手动处理）:", e)


def export_with_optimum_cli(src: str, dst: str) -> bool:
    """备用：optimum-cli 命令行（若 torch 导出失败且环境有可用 optimum）。"""
    try:
        import subprocess
        os.makedirs(dst, exist_ok=True)
        for cmd in ([sys.executable, "-m", "optimum.exporters.onnx"],
                    ["optimum-cli", "export", "onnx"]):
            try:
                r = subprocess.run(
                    cmd + ["--model", src, "--task", "feature-extraction", "--opset", "13", dst],
                    capture_output=True, text=True, encoding="utf-8", errors="replace")
                if r.returncode == 0 and os.path.exists(os.path.join(dst, "model.onnx")):
                    print("[optimum-cli] 导出成功")
                    return True
            except Exception as e:  # noqa
                print("[optimum-cli] 不可用:", e)
    except Exception as e:  # noqa
        print("[optimum-cli] 导出失败:", e)
    return False


def ensure_vocab(src: str, dst: str):
    """确保 dst 里有 vocab.txt（Java 端 tokenize 需要）；缺则从源模型复制。"""
    target = os.path.join(dst, "vocab.txt")
    if os.path.exists(target):
        print(f"OK: vocab.txt ({os.path.getsize(target)} bytes)")
        return
    candidates = [
        os.path.join(src, "vocab.txt"),
        glob.glob(os.path.join(CACHE_MODEL, "**", "vocab.txt"), recursive=True),
    ]
    for cand in candidates:
        if isinstance(cand, list):
            cand = cand[0] if cand else None
        if cand and os.path.exists(cand):
            shutil.copy2(cand, target)
            print(f"OK: vocab.txt 已复制 ({os.path.getsize(target)} bytes)")
            return
    print("警告：未找到 vocab.txt（词表），Java 端 tokenize 将无法工作")


def main():
    src = find_model_src()
    dst = os.path.abspath(OUT_DIR)
    print("模型源目录:", src)
    print("输出目录:", dst)

    ok = export_with_torch(src, dst) or export_with_optimum_cli(src, dst)
    if not ok:
        print("")
        print("导出失败。请确认已安装：pip install torch transformers onnx onnxruntime")
        sys.exit(1)

    # 校验产物
    onnx_path = os.path.join(dst, "model.onnx")
    if os.path.exists(onnx_path):
        print(f"OK: model.onnx ({os.path.getsize(onnx_path) / 1024 / 1024:.1f} MB)")
    else:
        print("警告：未找到 model.onnx")
    ensure_vocab(src, dst)
    print("完成。产物目录:", dst)


if __name__ == "__main__":
    main()

