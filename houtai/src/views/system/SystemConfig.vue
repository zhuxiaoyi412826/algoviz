<script setup lang="ts">
import { ref, reactive } from 'vue'
import {
  ElForm, ElFormItem, ElInput, ElSwitch,
  ElButton, ElCard, ElTabs, ElTabPane, ElMessage, ElDivider, ElColorPicker
} from 'element-plus'
import { Check, Refresh } from '@element-plus/icons-vue'

const activeTab = ref('basic')

const basicForm = reactive({
  siteTitle: '算法可视化平台',
  siteDomain: 'https://algo.example.com',
  siteLogo: '',
  icpNumber: '京ICP备XXXXXXXX号',
  copyright: '© 2024 算法可视化平台'
})

const themeForm = reactive({
  primaryColor: '#409eff',
  defaultTheme: 'light',
  enableDarkMode: true,
  sidebarTheme: 'dark',
  compactMode: false
})

const apiForm = reactive({
  backendUrl: 'http://localhost',
  corsProxy: 'http://localhost:3001',
  uploadMaxSize: 10,
  uploadAllowedTypes: 'jpg,png,gif,pdf,zip'
})

const performanceForm = reactive({
  enableCache: true,
  cacheTime: 3600,
  enableCompression: true
})

const loading = ref(false)

const handleSave = async () => {
  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('保存成功')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  ElMessage.info('已重置为默认值')
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>系统配置</h2>
      <div>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        <el-button type="primary" :icon="Check" :loading="loading" @click="handleSave">保存配置</el-button>
      </div>
    </div>

    <div class="card-container">
      <el-tabs v-model="activeTab" tab-position="left" style="min-height: 600px">
        <el-tab-pane label="基础配置" name="basic">
          <el-form :model="basicForm" label-width="140px" style="max-width: 700px">
            <el-divider content-position="left">网站信息</el-divider>
            <el-form-item label="网站标题">
              <el-input v-model="basicForm.siteTitle" placeholder="请输入网站标题" />
            </el-form-item>
            <el-form-item label="网站域名">
              <el-input v-model="basicForm.siteDomain" placeholder="请输入网站域名" />
            </el-form-item>
            <el-form-item label="网站Logo">
              <el-input v-model="basicForm.siteLogo" placeholder="请输入Logo URL">
                <template #append>
                  <el-button>上传</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="ICP备案号">
              <el-input v-model="basicForm.icpNumber" placeholder="请输入ICP备案号" />
            </el-form-item>
            <el-form-item label="版权信息">
              <el-input v-model="basicForm.copyright" placeholder="请输入版权信息" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="主题配置" name="theme">
          <el-form :model="themeForm" label-width="140px" style="max-width: 700px">
            <el-divider content-position="left">主题设置</el-divider>
            <el-form-item label="主题色">
              <el-color-picker v-model="themeForm.primaryColor" />
              <span class="color-value">{{ themeForm.primaryColor }}</span>
            </el-form-item>
            <el-form-item label="默认主题">
              <el-radio-group v-model="themeForm.defaultTheme">
                <el-radio value="light">浅色模式</el-radio>
                <el-radio value="dark">深色模式</el-radio>
                <el-radio value="auto">跟随系统</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="开启暗色模式">
              <el-switch v-model="themeForm.enableDarkMode" />
            </el-form-item>
            <el-form-item label="侧边栏主题">
              <el-radio-group v-model="themeForm.sidebarTheme">
                <el-radio value="dark">深色</el-radio>
                <el-radio value="light">浅色</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="紧凑模式">
              <el-switch v-model="themeForm.compactMode" />
              <span class="form-tip">开启后菜单和表格将使用更紧凑的间距</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="接口配置" name="api">
          <el-form :model="apiForm" label-width="140px" style="max-width: 700px">
            <el-divider content-position="left">API设置</el-divider>
            <el-form-item label="后端接口地址">
              <el-input v-model="apiForm.backendUrl" placeholder="请输入后端接口地址" />
            </el-form-item>
            <el-form-item label="CORS代理地址">
              <el-input v-model="apiForm.corsProxy" placeholder="请输入CORS代理地址" />
            </el-form-item>
            <el-form-item label="文件上传">
              <div class="upload-config">
                <el-input-number v-model="apiForm.uploadMaxSize" :min="1" :max="100" />
                <span style="margin: 0 8px">MB</span>
                <span style="color: var(--color-text-secondary)">单文件大小限制</span>
              </div>
            </el-form-item>
            <el-form-item label="允许的文件类型">
              <el-input v-model="apiForm.uploadAllowedTypes" placeholder="请输入允许的文件类型" />
              <div class="form-tip">多个类型用逗号分隔，如：jpg,png,gif,pdf</div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="高级配置" name="advanced">
          <el-form label-width="140px" style="max-width: 700px">
            <el-divider content-position="left">功能开关</el-divider>
            <el-form-item label="用户注册">
              <el-switch />
              <span class="form-tip">关闭后新用户将无法自主注册</span>
            </el-form-item>
            <el-form-item label="微信登录">
              <el-switch />
            </el-form-item>
            <el-form-item label="AI助手">
              <el-switch />
            </el-form-item>
            <el-form-item label="OJ在线评测">
              <el-switch />
            </el-form-item>
            <el-form-item label="数据可视化">
              <el-switch />
            </el-form-item>

            <el-divider content-position="left">性能优化</el-divider>
            <el-form-item label="开启缓存">
              <el-switch v-model="performanceForm.enableCache" />
            </el-form-item>
            <el-form-item label="缓存时间">
              <el-input-number v-model="performanceForm.cacheTime" :min="300" :max="86400" />
              <span style="margin-left: 8px">秒</span>
            </el-form-item>
            <el-form-item label="开启压缩">
              <el-switch v-model="performanceForm.enableCompression" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<style scoped lang="scss">
.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.color-value {
  margin-left: 12px;
  font-size: 12px;
  color: var(--color-text-secondary);
  font-family: monospace;
}

.upload-config {
  display: flex;
  align-items: center;
}
</style>
