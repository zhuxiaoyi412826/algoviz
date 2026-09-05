import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import AutoImport from 'unplugin-auto-import/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'
import http from 'node:http'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts'
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5000,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        // 用 127.0.0.1 而非 localhost：localhost 可能解析为 ::1（IPv6），增加解析与回退开销
        target: 'http://127.0.0.1:80',
        changeOrigin: true,
        rewrite: (path) => path,
        // 禁用 keepAlive 连接复用：空闲约 10 秒后代理池中的 socket 已被对端关闭，
        // 复用陈旧 socket 会导致空闲后的首个 /api 请求卡 2~4 秒（后端/SQL 实测仅 20ms，延迟全在代理层）
        agent: new http.Agent({ keepAlive: false })
      }
    }
  },
  base: './',
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    minify: false
  }
})
