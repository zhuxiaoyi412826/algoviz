<template>
  <div class="page-container">
    <el-tabs v-model="activeTab">
      <!-- ==================== Tab 1: 敏感词库 ==================== -->
      <el-tab-pane label="敏感词库" name="words">
        <el-card shadow="never">
          <div class="toolbar">
            <el-input v-model="wordQuery.keyword" placeholder="搜索敏感词" clearable style="width: 200px"
                      @keyup.enter="loadWords" />
            <el-select v-model="wordQuery.category" placeholder="分类" clearable style="width: 130px">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select v-model="wordQuery.level" placeholder="等级" clearable style="width: 130px">
              <el-option label="HIGH(拦截)" value="HIGH" />
              <el-option label="MEDIUM(待审)" value="MEDIUM" />
              <el-option label="LOW(记录)" value="LOW" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="loadWords">查询</el-button>
            <el-button :icon="Plus" @click="openWordDialog()">新增</el-button>
            <el-button :icon="Upload" @click="batchVisible = true">批量导入</el-button>
            <el-button type="warning" :icon="Refresh" @click="handleRefreshCache">刷新缓存</el-button>
            <el-button type="success" :icon="Promotion" @click="openPublishDialog">发布版本</el-button>
          </div>

          <el-table :data="wordList" v-loading="wordLoading" border stripe @selection-change="(s: any[]) => (wordSelection = s)">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="word" label="敏感词" min-width="140" />
            <el-table-column prop="category" label="分类" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="categoryType(row.category)">{{ row.category }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="level" label="等级" width="150">
              <template #default="{ row }">
                <el-tag size="small" :type="levelType(row.level)">{{ levelText(row.level) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="enabled" label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="row.enabled === 1 ? 'success' : 'info'">
                  {{ row.enabled === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="更新时间" width="170" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openWordDialog(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDeleteWord(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-button size="small" type="danger" :disabled="!wordSelection.length"
                       @click="handleDeleteWordsBatch">删除选中({{ wordSelection.length }})</el-button>
            <el-pagination background layout="total, sizes, prev, pager, next" :total="wordTotal"
                           v-model:current-page="wordQuery.page" v-model:page-size="wordQuery.pageSize"
                           :page-sizes="[20, 50, 100, 200, 500, 1000]"
                           @current-change="loadWords" @size-change="handleWordSizeChange" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ==================== Tab 2: 版本管理 ==================== -->
      <el-tab-pane label="版本管理" name="versions">
        <el-card shadow="never">
          <div class="toolbar">
            <span class="tip-text">每次「发布版本」会把当前工作区词库冻结为快照；回滚将用快照覆盖当前词库</span>
            <el-button :icon="Refresh" @click="loadVersions">刷新</el-button>
          </div>
          <el-table :data="versionList" v-loading="versionLoading" border stripe>
            <el-table-column prop="versionNo" label="版本号" width="90">
              <template #default="{ row }">
                <el-tag>v{{ row.versionNo }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="wordCount" label="词条数" width="90" />
            <el-table-column prop="remark" label="版本说明" min-width="220" />
            <el-table-column prop="createdBy" label="发布人" width="110" />
            <el-table-column prop="createTime" label="发布时间" width="180" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-popconfirm :title="`确认回滚到 v${row.versionNo}？当前词库将被覆盖`" @confirm="handleRollback(row)">
                  <template #reference>
                    <el-button link type="warning" size="small">回滚到此版本</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ==================== Tab 3: 危险代码规则 ==================== -->
      <el-tab-pane label="危险代码规则" name="rules">
        <el-card shadow="never">
          <div class="toolbar">
            <el-input v-model="ruleQuery.keyword" placeholder="规则名/编码" clearable style="width: 180px"
                      @keyup.enter="loadRules" />
            <el-select v-model="ruleQuery.language" placeholder="语言" clearable style="width: 110px">
              <el-option v-for="l in ['ALL', 'JAVA', 'PYTHON', 'JS', 'CPP']" :key="l" :label="l" :value="l" />
            </el-select>
            <el-select v-model="ruleQuery.riskLevel" placeholder="风险" clearable style="width: 110px">
              <el-option label="HIGH" value="HIGH" />
              <el-option label="MEDIUM" value="MEDIUM" />
              <el-option label="LOW" value="LOW" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="loadRules">查询</el-button>
            <el-button :icon="Plus" @click="openRuleDialog()">新增规则</el-button>
          </div>
          <el-table :data="ruleList" v-loading="ruleLoading" border stripe>
            <el-table-column prop="ruleCode" label="编码" width="90" />
            <el-table-column prop="ruleName" label="规则名称" min-width="130" />
            <el-table-column prop="language" label="语言" width="80" />
            <el-table-column prop="ruleContent" label="正则/关键词" min-width="200" show-overflow-tooltip />
            <el-table-column prop="riskLevel" label="风险" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="levelType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="score" label="分值" width="70" />
            <el-table-column prop="description" label="说明" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openRuleDialog(row)">编辑</el-button>
                <el-popconfirm title="确认删除该规则？" @confirm="handleDeleteRule(row)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination background layout="total, prev, pager, next" :total="ruleTotal"
                           v-model:current-page="ruleQuery.page" :page-size="ruleQuery.pageSize"
                           @current-change="loadRules" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ==================== Tab 4: 检测测试 ==================== -->
      <el-tab-pane label="检测测试" name="test">
        <el-card shadow="never">
          <el-form label-width="70px">
            <el-form-item label="标题">
              <el-input v-model="testForm.title" placeholder="如：动态规划入门（可选）" />
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="testForm.content" type="textarea" :rows="6"
                        placeholder="粘贴题目描述/代码，如：本题可用赌博网站推广…… 或 Runtime.getRuntime().exec(&quot;rm -rf /&quot;)" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="testing" :icon="Search" @click="handleTestDetect">开始检测</el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="testResult" :closable="false" :type="testResult.hit
            ? (testResult.preCheck === 'BLOCK' ? 'error' : 'warning') : 'success'"
            :title="`命中: ${testResult.hit ? '是' : '否'} | 风险等级: ${testResult.riskLevel} | 总分: ${testResult.totalScore} | 处置: ${{ blocked: '拦截', pending: '待人工审核', logonly: '仅记录', none: '放行' }[testResult.auditStatus as string] || testResult.auditStatus}`" />
          <el-table v-if="testResult && testResult.hits?.length" :data="testResult.hits" border style="margin-top: 12px">
            <el-table-column prop="type" label="类型" width="130">
              <template #default="{ row }">
                <el-tag size="small" :type="row.type === 'DANGEROUS_CODE' ? 'danger' : 'warning'">
                  {{ row.type === 'DANGEROUS_CODE' ? '危险代码' : '敏感词' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ruleName" label="命中项" min-width="140" />
            <el-table-column prop="hitContent" label="上下文" min-width="160" />
            <el-table-column prop="score" label="得分" width="80" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 敏感词编辑对话框 -->
    <el-dialog v-model="wordDialog.visible" :title="wordDialog.form.id ? '编辑敏感词' : '新增敏感词'" width="460px">
      <el-form :model="wordDialog.form" label-width="80px">
        <el-form-item label="敏感词">
          <el-input v-model="wordDialog.form.word" placeholder="输入敏感词" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="wordDialog.form.category" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-radio-group v-model="wordDialog.form.level">
            <el-radio value="HIGH">HIGH 拦截</el-radio>
            <el-radio value="MEDIUM">MEDIUM 待审</el-radio>
            <el-radio value="LOW">LOW 记录</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="wordDialog.form.enabled" :active-value="1" :inactive-value="0"
                     active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="wordDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveWord">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="batchVisible" title="批量导入敏感词" width="560px">
      <el-alert type="info" :closable="false"
                title="文件导入：词之间用空格 / 中英文逗号 / 换行分割；手动输入：每行一个词，支持「词,分类,等级」格式（默认 OTHER/MEDIUM）"
                style="margin-bottom: 12px" />
      <div class="file-import-bar">
        <input ref="fileInputRef" type="file" accept=".txt" style="display: none" @change="handleFileChange" />
        <el-button :icon="Upload" @click="fileInputRef?.click()">导入 txt 文件</el-button>
        <el-select v-model="batchFileCategory" style="width: 160px">
          <el-option v-for="c in categories" :key="c" :label="`文件词分类: ${c}`" :value="c" />
        </el-select>
        <el-select v-model="batchFileLevel" style="width: 160px">
          <el-option label="文件词等级: HIGH" value="HIGH" />
          <el-option label="文件词等级: MEDIUM" value="MEDIUM" />
          <el-option label="文件词等级: LOW" value="LOW" />
        </el-select>
      </div>
      <el-input v-model="batchText" type="textarea" :rows="10"
                placeholder="赌博&#10;刷单,ADVERTISING,MEDIUM&#10;加微信,ADVERTISING,LOW" />
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleBatchImport">导入</el-button>
      </template>
    </el-dialog>

    <!-- 发布版本对话框 -->
    <el-dialog v-model="publishVisible" title="发布词库版本" width="440px">
      <el-form label-width="80px">
        <el-form-item label="版本说明">
          <el-input v-model="publishRemark" type="textarea" :rows="3" placeholder="如：新增 5 个广告类敏感词" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handlePublish">发布</el-button>
      </template>
    </el-dialog>

    <!-- 规则编辑对话框 -->
    <el-dialog v-model="ruleDialog.visible" :title="ruleDialog.form.id ? '编辑规则' : '新增规则'" width="560px">
      <el-form :model="ruleDialog.form" label-width="90px">
        <el-form-item label="规则编码">
          <el-input v-model="ruleDialog.form.ruleCode" placeholder="如 DC-011" />
        </el-form-item>
        <el-form-item label="规则名称">
          <el-input v-model="ruleDialog.form.ruleName" placeholder="如：检测命令执行" />
        </el-form-item>
        <el-form-item label="适用语言">
          <el-select v-model="ruleDialog.form.language" style="width: 100%">
            <el-option v-for="l in ['ALL', 'JAVA', 'PYTHON', 'JS', 'CPP']" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则类型">
          <el-radio-group v-model="ruleDialog.form.ruleType">
            <el-radio value="REGEX">正则</el-radio>
            <el-radio value="KEYWORD">关键词</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="规则内容">
          <el-input v-model="ruleDialog.form.ruleContent" type="textarea" :rows="3"
                    placeholder="Java 正则语法，如 os\.system\s*\(" />
        </el-form-item>
        <el-form-item label="风险等级">
          <el-radio-group v-model="ruleDialog.form.riskLevel">
            <el-radio value="HIGH">HIGH</el-radio>
            <el-radio value="MEDIUM">MEDIUM</el-radio>
            <el-radio value="LOW">LOW</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="ruleDialog.form.score" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="ruleDialog.form.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ElCard, ElTabs, ElTabPane, ElTable, ElTableColumn, ElButton, ElInput, ElSelect,
  ElOption, ElTag, ElPagination, ElDialog, ElForm, ElFormItem, ElRadioGroup, ElRadio,
  ElSwitch, ElInputNumber, ElPopconfirm, ElAlert
} from 'element-plus'
import { Search, Plus, Refresh, Upload, Promotion, Delete } from '@element-plus/icons-vue'
import { auditApi } from '@/api/audit'

defineOptions({ name: 'SensitiveWordManage' })

const activeTab = ref('words')
const categories = ['ABUSE', 'POLITICS', 'ADVERTISING', 'PORN', 'OTHER']
const saving = ref(false)

const levelText = (l: string) => ({ HIGH: 'HIGH 拦截', MEDIUM: 'MEDIUM 待审', LOW: 'LOW 记录' } as any)[l] || l
const levelType = (l: string) => ({ HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' } as any)[l] || 'info'
const categoryType = (c: string) => ({
  ABUSE: 'danger', POLITICS: 'warning', ADVERTISING: 'primary', PORN: 'danger', OTHER: 'info'
} as any)[c] || 'info'

// ===== 敏感词 =====
const wordLoading = ref(false)
const wordList = ref<any[]>([])
const wordTotal = ref(0)
const wordSelection = ref<any[]>([])
const wordQuery = reactive({ keyword: '', category: '', level: '', page: 1, pageSize: 20 })

const loadWords = async () => {
  wordLoading.value = true
  try {
    const r: any = await auditApi.words(wordQuery)
    wordList.value = r?.list || []
    wordTotal.value = r?.total || 0
  } finally {
    wordLoading.value = false
  }
}

/** 每页大小变化：回到第 1 页重新加载 */
const handleWordSizeChange = () => {
  wordQuery.page = 1
  loadWords()
}

const wordDialog = reactive({
  visible: false,
  form: { id: null as any, word: '', category: 'OTHER', level: 'MEDIUM', enabled: 1 }
})

const openWordDialog = (row?: any) => {
  wordDialog.form = row
    ? { id: row.id, word: row.word, category: row.category, level: row.level, enabled: row.enabled }
    : { id: null, word: '', category: 'OTHER', level: 'MEDIUM', enabled: 1 }
  wordDialog.visible = true
}

const handleSaveWord = async () => {
  if (!wordDialog.form.word.trim()) return ElMessage.warning('请输入敏感词')
  saving.value = true
  try {
    await auditApi.saveWord(wordDialog.form)
    ElMessage.success('保存成功')
    wordDialog.visible = false
    loadWords()
  } finally {
    saving.value = false
  }
}

const handleDeleteWord = async (row: any) => {
  await auditApi.deleteWord(row.id)
  ElMessage.success('已删除')
  loadWords()
}

const handleDeleteWordsBatch = async () => {
  await auditApi.deleteWordsBatch(wordSelection.value.map((w: any) => w.id))
  ElMessage.success('批量删除成功')
  loadWords()
}

const handleRefreshCache = async () => {
  const msg: any = await auditApi.refreshCache()
  ElMessage.success(msg || '已刷新')
}

// ===== 批量导入 =====
const batchVisible = ref(false)
const batchText = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)
const batchFileCategory = ref('OTHER')
const batchFileLevel = ref('MEDIUM')

/** txt 文件解析：词之间用 空格/中英文逗号/顿号/分号/换行/制表符 分割，去重后追加到文本框 */
const applyFileText = (text: string) => {
  const tokens = text.split(/[\s,，、;；]+/).map(s => s.trim()).filter(Boolean)
  if (!tokens.length) return ElMessage.warning('文件中未解析到关键词')
  // 与文本框已有词去重（取每行首个逗号前的词）
  const existing = new Set(
    batchText.value.split('\n').map(l => l.split(',')[0]?.trim()).filter(Boolean)
  )
  const fresh = [...new Set(tokens)].filter(t => !existing.has(t))
  if (!fresh.length) return ElMessage.warning('文件中的词均已存在，重复已跳过')
  // 追加为「词,分类,等级」行，导入时按行格式正确解析，且可继续手动编辑
  const lines = fresh.map(w => `${w},${batchFileCategory.value},${batchFileLevel.value}`)
  batchText.value = batchText.value.trim()
    ? batchText.value.replace(/\s+$/, '') + '\n' + lines.join('\n')
    : lines.join('\n')
  ElMessage.success(`文件解析到 ${tokens.length} 个词，去重后新增 ${fresh.length} 条，确认后点击「导入」`)
}

const handleFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.txt')) {
    ElMessage.warning('仅支持 .txt 文件')
    input.value = ''
    return
  }
  const readAs = (encoding: string, retryGbk: boolean) => {
    const reader = new FileReader()
    reader.onload = () => {
      const text = String(reader.result || '')
      // UTF-8 读取出现乱码替换符时，可能是 GBK 编码（Windows 记事本默认），换 GBK 重读
      if (retryGbk && text.includes('\uFFFD')) return readAs('gbk', false)
      applyFileText(text)
    }
    reader.onerror = () => ElMessage.error('文件读取失败')
    reader.readAsText(file, encoding)
  }
  readAs('utf-8', true)
  input.value = ''
}

const handleBatchImport = async () => {
  const lines = batchText.value.split('\n').map(s => s.trim()).filter(Boolean)
  if (!lines.length) return ElMessage.warning('请输入内容')
  const words = lines.map(line => {
    const [word, category, level] = line.split(',').map(s => s?.trim())
    return { word, category: category || 'OTHER', level: level || 'MEDIUM', enabled: 1 }
  })
  saving.value = true
  try {
    const r: any = await auditApi.saveWordsBatch(words)
    ElMessage.success(`成功导入 ${r} 条`)
    batchVisible.value = false
    batchText.value = ''
    loadWords()
  } finally {
    saving.value = false
  }
}

// ===== 版本 =====
const versionLoading = ref(false)
const versionList = ref<any[]>([])
const publishVisible = ref(false)
const publishRemark = ref('')

const loadVersions = async () => {
  versionLoading.value = true
  try {
    const r = await auditApi.versions() as unknown as any[]
    versionList.value = r || []
  } finally {
    versionLoading.value = false
  }
}

const openPublishDialog = () => {
  publishRemark.value = ''
  publishVisible.value = true
}

const handlePublish = async () => {
  saving.value = true
  try {
    const v: any = await auditApi.publishVersion(publishRemark.value)
    ElMessage.success(`版本 v${v?.versionNo} 发布成功（${v?.wordCount} 条词）`)
    publishVisible.value = false
  } finally {
    saving.value = false
  }
}

const handleRollback = async (row: any) => {
  const msg: any = await auditApi.rollbackVersion(row.versionNo)
  ElMessage.success(msg || '已回滚')
  loadWords()
  loadVersions()
}

// ===== 危险代码规则 =====
const ruleLoading = ref(false)
const ruleList = ref<any[]>([])
const ruleTotal = ref(0)
const ruleQuery = reactive({ keyword: '', language: '', riskLevel: '', page: 1, pageSize: 20 })

const loadRules = async () => {
  ruleLoading.value = true
  try {
    const r: any = await auditApi.rules(ruleQuery)
    ruleList.value = r?.list || []
    ruleTotal.value = r?.total || 0
  } finally {
    ruleLoading.value = false
  }
}

const ruleDialog = reactive({
  visible: false,
  form: { id: null as any, ruleCode: '', ruleName: '', language: 'ALL', ruleType: 'REGEX', ruleContent: '', riskLevel: 'HIGH', score: 80, description: '' }
})

const openRuleDialog = (row?: any) => {
  ruleDialog.form = row
    ? { ...row }
    : { id: null, ruleCode: '', ruleName: '', language: 'ALL', ruleType: 'REGEX', ruleContent: '', riskLevel: 'HIGH', score: 80, description: '' }
  ruleDialog.visible = true
}

const handleSaveRule = async () => {
  saving.value = true
  try {
    await auditApi.saveRule(ruleDialog.form)
    ElMessage.success('保存成功')
    ruleDialog.visible = false
    loadRules()
  } finally {
    saving.value = false
  }
}

const handleDeleteRule = async (row: any) => {
  await auditApi.deleteRule(row.id)
  ElMessage.success('已删除')
  loadRules()
}

// ===== 检测测试 =====
const testing = ref(false)
const testForm = reactive({ title: '', content: '' })
const testResult = ref<any>(null)

const handleTestDetect = async () => {
  if (!testForm.content.trim() && !testForm.title.trim()) return ElMessage.warning('请输入内容')
  testing.value = true
  try {
    testResult.value = await auditApi.detect({ ...testForm })
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  loadWords()
  loadVersions()
  loadRules()
})
</script>

<style scoped>
.page-container { padding: 4px; }
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 14px; align-items: center; }
.pager { display: flex; justify-content: space-between; align-items: center; margin-top: 14px; }
.tip-text { color: #909399; font-size: 13px; }
.file-import-bar { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 10px; }
</style>
