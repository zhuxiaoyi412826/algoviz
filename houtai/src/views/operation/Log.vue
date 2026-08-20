<script setup lang="ts">
import { ref } from 'vue'
import { Link } from '@element-plus/icons-vue'

// Kibana 7.12.1 嵌入配置（Fluentd 采集的 algoviz-logs-* 日志）
const KIBANA_BASE = 'http://localhost:5601'
const LOG_INDEX_ID = '11bfd630-9a1d-11f1-8dab-a3f859b1cb0c' // algoviz-logs-* index pattern

// 日志明细（Discover）：列 = 时间/级别/Logger/消息/TraceID，按时间倒序，5 秒自动刷新
// 保留 Kibana 查询栏，支持在 iframe 内直接用 KQL 过滤（如 level:ERROR）
const discoverUrl = `${KIBANA_BASE}/app/kibana#/discover?_a=(columns:!('logtime','level','logger','message','trace_id'),index:'${LOG_INDEX_ID}',interval:auto,query:(language:kuery,query:''),sort:!('@timestamp',desc))&_g=(refreshInterval:(pause:!f,value:5000),time:(from:now-1h,mode:quick,to:now))`

const loading = ref(true)
const handleLoad = () => { loading.value = false }
const openInKibana = () => { window.open(discoverUrl, '_blank') }
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>日志管理</h2>
      <div class="header-actions">
        <el-tag type="info" effect="plain">数据源：Fluentd → algoviz-logs</el-tag>
        <el-tag type="success" effect="plain">5 秒自动刷新</el-tag>
        <el-button type="primary" size="small" :icon="Link" @click="openInKibana">在 Kibana Discover 中打开</el-button>
      </div>
    </div>
    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px"
      title="展示 Fluentd 采集的后端真实日志明细流，支持 KQL 查询（如 level:ERROR、message:*超时*）"
      description="保留 Kibana 查询工具栏，可直接输入关键词过滤；点击行可展开完整字段（线程、主机、服务名等）。" />
    <div class="iframe-box" v-loading="loading" element-loading-text="正在加载日志明细...">
      <iframe :src="discoverUrl" class="kibana-frame" frameborder="0" @load="handleLoad"
        title="日志明细（Fluentd）"></iframe>
    </div>
  </div>
</template>

<style scoped>
.header-actions { display: flex; gap: 8px; align-items: center }
.page-container { min-height: calc(100vh + 200px); }
.iframe-box {
  position: relative;
  height: calc(100vh - 40px);
  background: #fff;
  border-radius: 4px;
  box-shadow: var(--shadow-base);
  overflow: hidden;
}
.kibana-frame { width: 100%; height: 100%; border: 0; display: block }
</style>
