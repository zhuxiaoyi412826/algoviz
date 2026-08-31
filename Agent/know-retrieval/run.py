"""启动脚本"""
import os
import uvicorn
from app.config import PORT

if __name__ == "__main__":
    # 默认仅绑定本机（127.0.0.1），避免暴露到所有网络接口（S8392）；
    # 需要被其他主机访问时，通过环境变量 HOST 覆盖，如 HOST=0.0.0.0
    uvicorn.run("app.main:app", host=os.getenv("HOST", "127.0.0.1"), port=PORT, reload=True)
