<script setup lang="ts">
/**
 * 更新日志编辑 / 新增页（复用通用 MdSplitEditor 组件）
 * -----------------------------------------------------
 * 顶部：专属业务表单区（版本号 / 更新类型 / 发布日期 / 状态 / 摘要 / 模块 / 已知问题折叠）
 * 中部：MdSplitEditor 通用 Markdown 实时分屏编辑器，编辑 details 字段
 * 顶部文案：title = 新增更新日志 / 编辑更新日志（用户要求的"更新日志"主题）
 */
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Close, Warning } from '@element-plus/icons-vue'
import MdSplitEditor from '@/components/common/MdSplitEditor.vue'
import { changelogApi } from '@/api/system'
import dayjs from 'dayjs'
import type { Changelog } from '@/types'

const route = useRoute()
const idParam = route.params.id as string | undefined
const isEdit = route.path.includes('/edit/') && !!idParam
const standalone = !!route.meta?.standalone

const title = ref(isEdit ? '编辑更新日志' : '新增更新日志')
const loading = ref(false)
const saving = ref(false)

const form = reactive<{
  version: string
  type: Changelog['type']
  summary: string
  releaseDate: string
  modulesText: string
  knownIssues: string
  issuesTitle: string
  status: 0 | 1
  expandIssues: boolean | number[]
  details: string   // 直接用 v-model 绑 MdSplitEditor，不再走 detailsVditor
}>({
  version: '',
  type: 'new',
  summary: '',
  releaseDate: dayjs().format('YYYY-MM-DD'),
  modulesText: '',
  knownIssues: '',
  issuesTitle: '已知问题',
  status: 1,
  expandIssues: [],
  details: '',
})

const detailsEditorRef = ref<InstanceType<typeof MdSplitEditor> | null>(null)

/* ---------- 工具函数 ---------- */
function notifyOpener(type: 'saved' | 'canceled', message?: string) {
  try {
    if (window.opener && !window.opener.closed) {
      window.opener.postMessage(
        { __from: 'changelog-editor', type, message },
        window.location.origin
      )
    }
  } catch { /* ignore */ }
}

function goBackOrClose() {
  try {
    if (window.opener && !window.opener.closed) {
      setTimeout(() => window.close(), 120)
      return
    }
  } catch { /* ignore */ }
  window.location.href = (import.meta as any).env?.BASE_URL
    ? `${(import.meta as any).env.BASE_URL.replace(/\/$/, '')}/extension/changelog`
    : '/extension/changelog'
}

onMounted(async () => {
  document.title = `${title.value} - 算法可视化平台管理后台`
  if (isEdit) {
    loading.value = true
    try {
      const detail = (await changelogApi.getDetail(Number(idParam))) as unknown as Changelog
      form.version = detail.version
      form.type = detail.type
      form.summary = detail.summary
      form.releaseDate = detail.releaseDate
      form.issuesTitle = detail.issuesTitle || '已知问题'
      form.status = (detail.status === 0 ? 0 : 1)
      try {
        const arr: string[] = JSON.parse(detail.modules || '[]')
        form.modulesText = Array.isArray(arr) ? arr.join('\n') : ''
      } catch {
        form.modulesText = ''
      }
      form.knownIssues = detail.knownIssues || ''
      form.details = detail.details || ''
      if ((form.knownIssues || '').trim()) {
        form.expandIssues = ['issues']
      }
    } catch (e: any) {
      ElMessage.error(e?.message || '加载记录失败')
    } finally {
      loading.value = false
    }
  } else {
    form.details = [
      '## 本次更新摘要',
      '',
      '- 新增 XXX 页面，包含 A/B/C 三个功能',
      '- 优化 XXX 交互体验',
      '- 修复 XXX BUG',
      '',
      '## 注意',
      '',
      '> 若需要用户清除浏览器缓存，请在此注明。',
    ].join('\n')
  }
})

onBeforeUnmount(() => { /* MdSplitEditor 自己清理监听器 */ })

async function handleSave() {
  if (!form.version.trim()) { ElMessage.warning('请填写版本号，例如 v1.2.3'); return }
  if (!form.releaseDate)  { ElMessage.warning('请选择发布日期'); return }
  if (!form.details.trim()) {
    ElMessage.warning('更新说明（Markdown）不能为空')
    return
  }
  const modulesArr = Array.from(new Set(
    form.modulesText.split('\n').map(s => s.trim()).filter(Boolean)
  ))
  const payload: Partial<Changelog> = {
    version: form.version.trim(),
    type: form.type,
    summary: form.summary.trim(),
    releaseDate: form.releaseDate,
    modules: JSON.stringify(modulesArr),
    details: form.details,
    knownIssues: form.knownIssues.trim(),
    issuesTitle: form.issuesTitle.trim() || '已知问题',
    status: form.status,
  }
  saving.value = true
  try {
    if (isEdit) {
      await changelogApi.update(Number(idParam), payload)
      const msg = '保存成功，更新日志已更新'
      notifyOpener('saved', msg)
      ElMessage.success(msg)
    } else {
      const r = (await changelogApi.create(payload)) as unknown as Changelog
      const msg = `新增成功，记录 ID=${r?.id}`
      notifyOpener('saved', msg)
      ElMessage.success(msg)
    }
    goBackOrClose()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  notifyOpener('canceled')
  goBackOrClose()
}
</script>

<template>
  <div
    class="changelog-editor-page"
    :class="{ standalone }"
  >
    <div class="editor-topbar">
      <div class="editor-topbar__title">
        <span class="dot" />
        <span>{{ title }}</span>
        <span class="sub">
          · 左 Markdown 源码 / 右实时渲染 &nbsp;|&nbsp; 保存后自动关闭当前页面
        </span>
      </div>
      <div class="editor-topbar__actions">
        <el-button :icon="Close" @click="handleCancel">取消返回</el-button>
        <el-button type="primary" :icon="Check" :loading="saving || loading" @click="handleSave">
          保存并关闭
        </el-button>
      </div>
    </div>

    <div class="editor-body" v-loading="loading">
      <!-- 元信息表单：更新日志专属业务区 -->
      <div class="form-meta">
        <el-form label-width="100px" size="default">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="版本号" required>
                <el-input v-model="form.version" placeholder="如 v1.2.3" maxlength="32" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="更新类型" required>
                <el-select v-model="form.type" style="width:100%">
                  <el-option label="新增" value="new" />
                  <el-option label="优化" value="optimize" />
                  <el-option label="修复" value="fix" />
                  <el-option label="紧急更新" value="urgent" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="发布日期" required>
                <el-date-picker
                  v-model="form.releaseDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择发布日期"
                  style="width:100%"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="发布状态">
                <el-radio-group v-model="form.status">
                  <el-radio :value="1">已发布</el-radio>
                  <el-radio :value="0">草稿</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="16">
              <el-form-item label="更新摘要">
                <el-input
                  v-model="form.summary"
                  placeholder="一句话说明本次更新（前台卡片顶部短摘要）"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="功能模块">
                <el-input
                  v-model="form.modulesText"
                  type="textarea"
                  :rows="1"
                  placeholder="每行一个模块名，例：个人中心 / 商品页面 / 头像系统"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- 更新说明：复用通用 MdSplitEditor（纯编辑器，不管业务保存） -->
      <div class="editor-block">
        <div class="editor-block__title">
          更新说明（Markdown · 左右分屏）
          <span class="hint">左栏输入源码，右栏即时渲染；中间分隔条支持左右拖拽；滚动自动同步</span>
        </div>
        <MdSplitEditor
          ref="detailsEditorRef"
          v-model="form.details"
          :height="640"
          placeholder="左侧输入 Markdown 源码，右侧会即时渲染成 HTML 预览：\n\n## 本次更新摘要\n\n- 新增 XXX 功能\n- 优化 XXX 体验"
        />
      </div>

      <!-- 已知问题：折叠收起 -->
      <div class="editor-block editor-block--folded">
        <el-collapse v-model="form.expandIssues">
          <el-collapse-item name="issues">
            <template #title>
              <div class="issues-title">
                <el-icon :size="16" color="#e6a23c"><Warning /></el-icon>
                <span>
                  已知问题 / {{ form.issuesTitle || '注意事项' }}
                  <span v-if="form.knownIssues.trim()" class="issues-dot">已填写</span>
                  <span v-else class="issues-dot issues-dot--muted">留空前台显示"暂无已知问题"</span>
                </span>
              </div>
            </template>
            <el-form label-width="90px" size="default" style="padding-top:6px">
              <el-form-item label="自定义标题">
                <el-input
                  v-model="form.issuesTitle"
                  placeholder="例如：注意事项 / 已知问题 / 温馨提示"
                  maxlength="16"
                  style="max-width: 420px"
                />
              </el-form-item>
              <el-form-item label="Markdown 内容">
                <el-input
                  v-model="form.knownIssues"
                  type="textarea"
                  :rows="6"
                  placeholder="Markdown 文本，例如：&#10;- 老版本用户需重新登录一次&#10;> 若出现 502 请 2 分钟后重试"
                />
              </el-form-item>
              <div class="issues-tips">
                <b>支持：</b>
                <code>## 标题</code>
                <code>- 列表</code>
                <code>**粗体**</code>
                <code>*斜体*</code>
                <code>`代码`</code>
                <code>``` ``` 代码块</code>
                <code>> 引用</code>
                <span class="tips-sep">|</span>
                前台展示时会再用 marked 渲染成 HTML
              </div>
            </el-form>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ---- 页面级布局 ---- */
.changelog-editor-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}
.changelog-editor-page.standalone {
  width: 100%;
}
.editor-topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
}
.editor-topbar__title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex-wrap: wrap;
}
.editor-topbar__title .dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #7c3aed, #a855f7);
}
.editor-topbar__title .sub {
  font-size: 12px;
  color: #909399;
  font-weight: 400;
}
.editor-body {
  flex: 1;
  padding: 18px 24px 40px;
  max-width: 1500px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}
.form-meta {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px 4px;
  border: 1px solid #ebeef5;
  margin-bottom: 14px;
}
.editor-block {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px 18px;
  border: 1px solid #ebeef5;
  margin-bottom: 14px;
}
.editor-block__title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
  padding-left: 2px;
  line-height: 1.6;
}
.editor-block__title .hint {
  display: inline-block;
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
  font-weight: 400;
}
.editor-block--folded { padding: 2px 10px; }

/* ---- 折叠面板样式 ---- */
.issues-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.issues-dot {
  margin-left: 10px;
  padding: 0 8px;
  height: 18px;
  line-height: 18px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 400;
  color: #fff;
  background: linear-gradient(135deg, #e6a23c, #f08a24);
}
.issues-dot--muted {
  background: #f2f6fc;
  color: #8a94a6;
}
.issues-tips {
  padding: 8px 0 0 90px;
  color: #909399;
  font-size: 12px;
  line-height: 1.9;
}
.issues-tips code {
  background: #f6f8fa;
  border: 1px solid #eaecef;
  padding: 1px 6px;
  border-radius: 4px;
  margin: 0 2px;
  color: #7928ca;
  font-family: 'Fira Code', Consolas, monospace;
  font-size: 11px;
}
.issues-tips .tips-sep {
  margin: 0 8px;
  color: #dcdfe6;
}
</style>
