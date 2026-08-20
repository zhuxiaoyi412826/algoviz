<script setup lang="ts">
import { ref } from 'vue'
import { Link } from '@element-plus/icons-vue'

// Kibana 7.12.1 嵌入配置
// embed=true        隐藏 Kibana 顶栏/侧边栏，仅保留图表
// refreshInterval   每 10 秒自动刷新（数据实时同步）
// time range        默认展示最近 1 小时
const KIBANA_BASE = 'http://localhost:5601'
const DASHBOARD_ID = 'algoviz-resource-monitor'
const embedUrl = `${KIBANA_BASE}/app/kibana#/dashboard/${DASHBOARD_ID}?embed=true&_g=(refreshInterval:(pause:!f,value:10000),time:(from:now-1h,mode:quick,to:now))`

const loading = ref(true)
const handleLoad = () => { loading.value = false }
const openInKibana = () => { window.open(`${KIBANA_BASE}/app/kibana#/dashboard/${DASHBOARD_ID}`, '_blank') }
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>资源占用监控</h2>
      <div class="header-actions">
        <el-tag type="info" effect="plain">数据源：Metricbeat-7.12.1</el-tag>
        <el-tag type="success" effect="plain">10 秒自动刷新</el-tag>
        <el-button type="primary" size="small" :icon="Link" @click="openInKibana">在 Kibana 中打开</el-button>
      </div>
    </div>
    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px"
      title="展示 Metricbeat 采集的真实主机指标：CPU / 内存 / 磁盘 IO / 磁盘剩余空间 / 网络吞吐 / 进程资源 TOP / 系统负载 / OOM 风险"
      description="「磁盘 IO」需以管理员权限运行 Metricbeat 才有数据；「系统负载」为 Linux 指标，Windows 下无数据。图表可拖拽选择时间范围、点击图例筛选。" />
    <div class="iframe-box" v-loading="loading" element-loading-text="正在加载 Kibana 实时看板...">
      <iframe :src="embedUrl" class="kibana-frame" frameborder="0" allow="fullscreen" @load="handleLoad"
        title="资源占用监控（Metricbeat）"></iframe>
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
