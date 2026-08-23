<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElForm, ElFormItem, ElInput, ElSwitch,
  ElButton, ElCard, ElTabs, ElTabPane, ElMessage, ElDivider, ElColorPicker,
  ElUpload, ElImage
} from 'element-plus'
import { Check, Refresh, Upload as UploadIcon } from '@element-plus/icons-vue'
import { systemConfigApi } from '@/api/system'
import type { SystemConfig } from '@/types'
import type { UploadRequestOptions } from 'element-plus'

const activeTab = ref('basic')

// ========== 基础配置（与后端 keys 一一对应）==========
const basicForm = reactive({
  site_name: 'AlgoViz',
  site_logo: '',
  icp_number: '豫ICP备12345678号',
  copyright: '© 2026 AlgoViz',
  github_link: 'https://github.com/',
  site_slogan: 'AlgoViz - 数据结构与算法可视化学习平台 · 让学习变得更有趣',
  // 兼容原有占位键（不展示但保留避免报错）
  site_title: '算法可视化平台',
  site_domain: 'https://algo.example.com',
  icpNumber: '',
  siteTitle: '',
  siteDomain: '',
  siteLogo: '',
  githubLink: '',
  siteSlogan: ''
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
const saving = ref(false)
const uploadLoading = ref(false)

/**
 * 把后端 List<SystemConfig>（key/value 结构）平铺到 basicForm
 */
const applyConfigs = (list: SystemConfig[]) => {
  const map: Record<string, string> = {}
  list.forEach(c => { map[c.key] = c.value ?? '' })
  basicForm.site_name = map.site_name ?? 'AlgoViz'
  basicForm.site_logo = map.site_logo ?? ''
  basicForm.icp_number = map.icp_number ?? '豫ICP备12345678号'
  basicForm.copyright = map.copyright ?? '© 2026 AlgoViz'
  basicForm.github_link = map.github_link ?? ''
  basicForm.site_slogan = map.site_slogan ?? 'AlgoViz - 数据结构与算法可视化学习平台 · 让学习变得更有趣'
}

const loadConfigs = async () => {
  loading.value = true
  try {
    const list = await systemConfigApi.getAll()
    applyConfigs(Array.isArray(list) ? list : [])
  } catch (e: any) {
    ElMessage.error(e?.message || '加载配置失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadConfigs)

/**
 * 保存：只提交 6 个基础配置
 */
const handleSave = async () => {
  saving.value = true
  try {
    await systemConfigApi.update({
      site_name: basicForm.site_name,
      site_logo: basicForm.site_logo,
      icp_number: basicForm.icp_number,
      copyright: basicForm.copyright,
      github_link: basicForm.github_link,
      site_slogan: basicForm.site_slogan
    })
    ElMessage.success('保存成功，前台页脚将立即更新')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleReset = () => {
  loadConfigs()
  ElMessage.info('已重置为服务器上的值')
}

/**
 * Logo 上传（使用 Element Plus 自定义请求）
 */
const customUploadRequest = async (options: UploadRequestOptions) => {
  uploadLoading.value = true
  try {
    const res: any = await systemConfigApi.uploadLogo(options.file)
    // 后端 FileUpload 返回的 downloadUrl 形如 /api/file/download/{id}
    // 作为 logo src，直接接 API BASE 即可，Vue request 已经拼了 BASE_URL
    const base = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost'
    const url = (res?.downloadUrl) ? (base + res.downloadUrl) : ''
    if (url) {
      basicForm.site_logo = url
      ElMessage.success('Logo 上传成功，已自动填入（记得点击"保存配置"）')
    } else {
      throw new Error('返回中未找到 URL')
    }
    options.onSuccess?.(res)
  } catch (e: any) {
    ElMessage.error(e?.message || 'Logo 上传失败')
    options.onError?.(e)
  } finally {
    uploadLoading.value = false
  }
}

/**
 * 手动粘贴 URL 时确保链接以 http 开头
 */
const ensureLogoUrlPrefix = () => {
  const v = basicForm.site_logo.trim()
  if (v && !v.startsWith('http') && !v.startsWith('/') && !v.startsWith('data:')) {
    basicForm.site_logo = 'https://' + v
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>系统配置</h2>
      <div>
        <el-button :icon="Refresh" @click="handleReset" :loading="loading">重置</el-button>
        <el-button type="primary" :icon="Check" :loading="saving || loading" @click="handleSave">保存配置</el-button>
      </div>
    </div>

    <div class="card-container">
      <el-tabs v-model="activeTab" tab-position="left" style="min-height: 600px">
        <!-- ===================== 基础配置 ===================== -->
        <el-tab-pane label="基础配置" name="basic">
          <el-form :model="basicForm" label-width="140px" style="max-width: 760px">
            <el-divider content-position="left">网站信息</el-divider>

            <el-form-item label="站点名称">
              <el-input v-model="basicForm.site_name" placeholder="前台显示的站点名" maxlength="40" />
              <div class="form-tip">对应前台页脚/导航的品牌名称</div>
            </el-form-item>

            <el-form-item label="网站Logo">
              <div class="logo-input-wrap">
                <el-input
                  v-model="basicForm.site_logo"
                  placeholder="请输入 Logo URL 或点击右侧上传"
                  style="flex:1"
                  @blur="ensureLogoUrlPrefix"
                >
                  <template #append>
                    <el-upload
                      :show-file-list="false"
                      accept="image/png,image/jpeg,image/gif,image/svg+xml,image/webp,image/x-icon"
                      :http-request="customUploadRequest"
                    >
                      <el-button :icon="UploadIcon" :loading="uploadLoading">上传</el-button>
                    </el-upload>
                  </template>
                </el-input>
                <div v-if="basicForm.site_logo" class="logo-preview">
                  <span class="form-tip" style="margin-right:8px">预览：</span>
                  <el-image
                    :src="basicForm.site_logo"
                    :preview-src-list="[basicForm.site_logo]"
                    fit="contain"
                    style="width: 64px; height: 64px; border:1px solid var(--el-border-color-light); border-radius:6px; background:#fff;"
                  >
                    <template #error>
                      <span style="color:#ef4444;font-size:12px">图片无法加载</span>
                    </template>
                  </el-image>
                </div>
              </div>
              <div class="form-tip">支持直接填 URL（http/https），或点击"上传"选择本地图片。推荐 512×512，PNG/SVG 透明背景。</div>
            </el-form-item>

            <el-form-item label="ICP备案号">
              <el-input v-model="basicForm.icp_number" placeholder="例如：豫ICP备12345678号" maxlength="50" />
              <div class="form-tip">将显示在页脚"ICP备案：XXX"处，并自动链到 beian.miit.gov.cn</div>
            </el-form-item>

            <el-form-item label="版权信息">
              <el-input v-model="basicForm.copyright" placeholder="例如：© 2026 AlgoViz" maxlength="100" />
            </el-form-item>

            <el-form-item label="GitHub链接">
              <el-input v-model="basicForm.github_link" placeholder="https://github.com/your-org/your-repo" maxlength="200" />
              <div class="form-tip">留空则不显示；填写后在页脚显示 GitHub 图标跳转</div>
            </el-form-item>

            <el-form-item label="站点标语">
              <el-input
                v-model="basicForm.site_slogan"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 3 }"
                placeholder="例如：AlgoViz - 数据结构与算法可视化学习平台 · 让学习变得更有趣"
                maxlength="100"
                show-word-limit
              />
              <div class="form-tip">显示在页脚顶部，一句话介绍站点</div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ===================== 主题配置 ===================== -->
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

        <!-- ===================== 接口配置 ===================== -->
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

        <!-- ===================== 高级配置 ===================== -->
        <el-tab-pane label="高级配置" name="advanced">
          <el-form label-width="140px" style="max-width: 700px">
            <el-divider content-position="left">功能开关</el-divider>
            <el-form-item label="用户注册"><el-switch /></el-form-item>
            <el-form-item label="微信登录"><el-switch /></el-form-item>
            <el-form-item label="AI助手"><el-switch /></el-form-item>
            <el-form-item label="OJ在线评测"><el-switch /></el-form-item>
            <el-form-item label="数据可视化"><el-switch /></el-form-item>

            <el-divider content-position="left">性能优化</el-divider>
            <el-form-item label="开启缓存"><el-switch v-model="performanceForm.enableCache" /></el-form-item>
            <el-form-item label="缓存时间">
              <el-input-number v-model="performanceForm.cacheTime" :min="300" :max="86400" />
              <span style="margin-left: 8px">秒</span>
            </el-form-item>
            <el-form-item label="开启压缩"><el-switch v-model="performanceForm.enableCompression" /></el-form-item>
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
  display: block;
  margin-top: 4px;
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

.logo-input-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.logo-preview {
  display: flex;
  align-items: center;
}
</style>
