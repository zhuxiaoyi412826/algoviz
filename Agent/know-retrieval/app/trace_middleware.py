"""全链路追踪模块 - TraceId 中间件与日志配置

提供与 Java 端 TraceIdFilter/FeignTracingInterceptor 配合的 Python 实现：
1. 从 HTTP Header 提取或生成 TraceId
2. 使用 ContextVar 存储，贯穿整个请求生命周期
3. 配置日志格式自动携带 trace_id
4. 输出日志到指定文件，供 Filebeat 采集到 Elasticsearch

与 Java 端的约定：
    Header 名称：X-Trace-Id
    日志格式：时间 [TraceId] [等级] Logger - 消息
"""

import logging
import uuid
from contextvars import ContextVar
from datetime import datetime
from pathlib import Path
from typing import Optional

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.types import ASGIApp, Receive, Scope, Send

# ==================== TraceId 存储 ====================

# 使用 ContextVar 存储当前请求的 TraceId（ASGI 安全，比 threading.local 更好）
trace_id_var: ContextVar[str] = ContextVar("trace_id", default="no-trace")

# HTTP Header 名称（与 Java 端 TraceIdFilter.TRACE_HEADER 一致）
TRACE_HEADER = "X-Trace-Id"


def get_trace_id() -> str:
    """获取当前请求的 TraceId"""
    return trace_id_var.get()


def set_trace_id(trace_id: str) -> None:
    """设置当前请求的 TraceId"""
    trace_id_var.set(trace_id)


def generate_trace_id() -> str:
    """生成 16 位短 TraceId（与 Java 端格式一致）"""
    return uuid.uuid4().hex[:16]


# ==================== 日志配置 ====================

# 日志文件路径（与 Spring Boot 统一存放到 D:/rizi 目录）
LOG_DIR = Path("D:/rizi")
LOG_DIR.mkdir(parents=True, exist_ok=True)
LOG_FILE = LOG_DIR / "python-app.log"


class TraceIdFormatter(logging.Formatter):
    """自定义日志格式，自动注入当前 TraceId"""

    def format(self, record: logging.LogRecord) -> str:
        # 将 trace_id 注入 LogRecord，供 %(trace_id)s 使用
        if not hasattr(record, "trace_id"):
            record.trace_id = get_trace_id()
        return super().format(record)


def setup_logging() -> logging.Logger:
    """配置日志系统：控制台 + 文件输出，格式带 TraceId"""

    # 基础格式
    fmt = "%(asctime)s [%(trace_id)s] [%(levelname)s] %(name)s - %(message)s"
    datefmt = "%Y-%m-%d %H:%M:%S"

    # Root logger
    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)

    # 清除已有的 handler（避免重复）
    root_logger.handlers.clear()

    # 控制台 handler
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(TraceIdFormatter(fmt, datefmt))
    root_logger.addHandler(console_handler)

    # 文件 handler（供 Filebeat 采集）
    file_handler = logging.FileHandler(str(LOG_FILE), encoding="utf-8")
    file_handler.setFormatter(TraceIdFormatter(fmt, datefmt))
    file_handler.setLevel(logging.DEBUG)
    root_logger.addHandler(file_handler)

    # 获取应用 logger
    logger = logging.getLogger("algoviz")
    logger.setLevel(logging.DEBUG)

    return logger


# ==================== FastAPI 中间件 ====================

class TraceIdMiddleware(BaseHTTPMiddleware):
    """TraceId 中间件 - 提取或生成 TraceId，贯穿请求全链路

    工作流程：
    1. 请求进入时：从 Header 获取 X-Trace-Id，若无则生成新的
    2. 将 TraceId 存入 ContextVar
    3. 日志中自动携带 TraceId
    4. 请求结束时清理 ContextVar

    使用 BaseHTTPMiddleware 是为了在整个 ASGI 生命周期内保持 ContextVar。
    """

    def __init__(self, app: ASGIApp):
        super().__init__(app)

    async def dispatch(self, request: Request, call_next):
        # 1. 提取或生成 TraceId
        incoming_trace = request.headers.get(TRACE_HEADER)
        if incoming_trace:
            trace_id = incoming_trace
        else:
            trace_id = generate_trace_id()

        # 2. 存入 ContextVar
        token = trace_id_var.set(trace_id)

        try:
            # 记录请求入口日志
            logger = logging.getLogger("algoviz")
            logger.info(
                "请求开始: method=%s path=%s trace_id=%s",
                request.method, request.url.path, trace_id,
            )

            # 3. 执行下游处理
            response = await call_next(request)

            # 4. 将 TraceId 写入响应头（方便前端调试）
            response.headers[TRACE_HEADER] = trace_id

            logger.info(
                "请求完成: method=%s path=%s status=%s",
                request.method, request.url.path, response.status_code,
            )

            return response

        except Exception as exc:
            logger.error("请求异常: %s %s error=%s", request.method, request.url.path, exc, exc_info=True)
            raise
        finally:
            # 5. 清理 ContextVar
            trace_id_var.reset(token)


# ==================== 便捷装饰器 ====================

def log_with_trace(func):
    """日志装饰器 - 为被装饰的函数日志自动添加 TraceId

    用法：
        @log_with_trace
        def my_function():
            logger.info("处理数据")  # 自动带 trace_id
    """
    import functools

    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        logger = logging.getLogger("algoviz")
        logger.debug("执行函数: %s", func.__qualname__)
        result = func(*args, **kwargs)
        return result

    return wrapper
