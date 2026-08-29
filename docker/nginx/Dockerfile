# ====== 阶段1: 构建后台 Vue3 管理界面 ======
# houtai 使用 Vite 8 / TS 6，要求 Node >= 20.19；node:18 无法构建
FROM node:22-alpine AS houtai-builder
WORKDIR /build

# 国内镜像加速
RUN npm config set registry https://registry.npmmirror.com

# context 根 = 仓库根（AlgoVize/），COPY 路径不再带 AlgoVize/ 前缀
COPY houtai/package.json houtai/package-lock.json* ./
RUN npm install

COPY houtai/ .
RUN npm run build
# 产物：/build/dist

# ====== 阶段2: Nginx 运行（托管前台 + 后台 dist + SSL 终端 + /api 反代） ======
FROM nginx:1.25-alpine

# 清空默认 server 块
RUN rm -f /etc/nginx/conf.d/default.conf

# 自定义站点配置
COPY docker/nginx/nginx.conf /etc/nginx/conf.d/dsaol.conf

# 前台静态页（原生 HTML）
COPY qianduan/ /usr/share/nginx/html/

# 后台管理 dist（由阶段1构建）
COPY --from=houtai-builder /build/dist/ /usr/share/nginx/admin/

EXPOSE 80 443
