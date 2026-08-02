<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem,
  ElInput, ElSelect, ElOption, ElMessage, ElMessageBox, ElPagination,
  ElInputNumber, ElRadioGroup, ElRadio, ElDivider, ElCheckboxGroup, ElCheckbox,
  ElEmpty
} from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, Upload, Download, View, MagicStick, Promotion } from '@element-plus/icons-vue'
import type { OJProblem } from '@/types'

const allProblems = ref<OJProblem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('新增题目')

/** 批量导入（支持 Excel / MD / JSON） */
const importVisible = ref(false)
const importLoading = ref(false)
const importOverwrite = ref(false)
const importFormat = ref<'excel' | 'md' | 'json'>('excel')

const searchForm = reactive({
  keyword: '',
  difficulty: '',
  status: ''
})

const formData = reactive<Partial<OJProblem>>({
  id: '',
  problemNo: '',
  title: '',
  difficulty: 'medium',
  tags: [],
  description: '',
  template: {},
  status: 'offline'
})

const formRef = ref()
const currentProblem = ref<OJProblem | null>(null)

const rules = {
  problemNo: [{ required: true, message: '请输入题号', trigger: 'blur' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  description: [{ required: true, message: '请输入题目描述', trigger: 'blur' }]
}

const difficultyOptions = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' }
]

const statusOptions = [
  { label: '上线', value: 'online' },
  { label: '下线', value: 'offline' }
]

const tagOptions = [
  '数组', '链表', '树', '图', '动态规划', '回溯', '贪心', '分治', '排序', '查找', '字符串', '数学'
]

const languageOptions = [
  { label: 'Python', value: 'python' },
  { label: 'Java', value: 'java' },
  { label: 'C++', value: 'cpp' },
  { label: 'JavaScript', value: 'javascript' }
]

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const pagedProblems = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return allProblems.value.slice(start, start + pageSize.value)
})

onMounted(() => {
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (searchForm.keyword) params.append('keyword', searchForm.keyword)
    if (searchForm.difficulty) params.append('difficulty', searchForm.difficulty)
    
    const response = await fetch(`http://localhost/api/problems/all?${params.toString()}`)
    const data = await response.json()
    
    if (data.success) {
      allProblems.value = data.problems.map((item: any) => ({
        id: item.id,
        problemNo: item.problemNo,
        title: item.title,
        difficulty: item.difficulty,
        tags: item.tags ? item.tags.split(',') : [],
        description: item.description,
        template: { java: item.template || '' },
        status: item.status === 'ACTIVE' ? 'online' : 'offline',
        submissionCount: item.submissionCount || 0,
        acRate: item.acRate || 0,
        createTime: item.createdAt,
        updateTime: item.updatedAt
      }))
      total.value = data.count
      // 数据变化 → 如果当前页超出范围，回到第 1 页
      const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
      if (page.value > maxPage) page.value = 1
    } else {
      ElMessage.error('加载题目失败')
    }
  } catch (error) {
    console.error('加载题目失败:', error)
    ElMessage.error('加载题目失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.difficulty = ''
  searchForm.status = ''
  page.value = 1
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增题目'
  Object.keys(formData).forEach(key => {
    (formData as any)[key] = key === 'difficulty' ? 'medium' : key === 'tags' ? [] : key === 'template' ? {} : key === 'status' ? 'offline' : ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: OJProblem) => {
  dialogTitle.value = '编辑题目'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleView = (row: OJProblem) => {
  currentProblem.value = row
  detailVisible.value = true
}

const handleDelete = async (row: OJProblem) => {
  try {
    await ElMessageBox.confirm('确定要删除该题目吗？', '提示', { type: 'warning' })
    
    const response = await fetch(`http://localhost/api/problems/${row.id}`, {
      method: 'DELETE'
    })
    const data = await response.json()
    
    if (data.success) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch {}
}

const handleToggleStatus = async (row: OJProblem) => {
  const newStatus = row.status === 'online' ? 'offline' : 'online'
  try {
    await ElMessageBox.confirm(`确定要${newStatus === 'online' ? '上线' : '下线'}该题目吗？`, '提示', { type: 'warning' })
    
    const response = await fetch(`http://localhost/api/problems/${row.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus === 'online' ? 'ACTIVE' : 'INACTIVE' })
    })
    const data = await response.json()
    
    if (data.success) {
      ElMessage.success('操作成功')
      loadData()
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch {}
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    
    try {
      const submitData = {
        problemNo: formData.problemNo,
        title: formData.title,
        difficulty: formData.difficulty,
        tags: (formData.tags || []).join(','),
        description: formData.description,
        template: formData.template?.java || '',
        status: formData.status === 'online' ? 'ACTIVE' : 'INACTIVE'
      }
      
      let response
      if (formData.id) {
        // 更新题目
        response = await fetch(`http://localhost/api/problems/${formData.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(submitData)
        })
      } else {
        // 新增题目
        response = await fetch('http://localhost/api/problems', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(submitData)
        })
      }
      
      const data = await response.json()
      if (data.success) {
        ElMessage.success(dialogTitle.value + '成功')
        dialogVisible.value = false
        loadData()
      } else {
        ElMessage.error(data.message || '操作失败')
      }
    } catch (error) {
      console.error('保存题目失败:', error)
      ElMessage.error('保存题目失败')
    }
  })
}

const handleBatchImport = () => {
  importFormat.value = 'excel'
  importVisible.value = true
}

/** 通用导入处理器：根据格式分发 */
const handleFileChange = async (uploadFile: any) => {
  if (!uploadFile || !uploadFile.raw) return
  const raw: File = uploadFile.raw

  const format = importFormat.value
  const extMap: Record<string, string> = { excel: 'xlsx|xls', md: 'md|markdown', json: 'json' }
  const ext = extMap[format]
  if (!new RegExp(`\\.(${ext})$`, 'i').test(raw.name)) {
    const labels: Record<string, string> = { excel: '.xlsx / .xls', md: '.md / .markdown', json: '.json' }
    ElMessage.error(`仅支持 ${labels[format]} 文件`)
    return
  }
  if (raw.size > 10 * 1024 * 1024) {
    ElMessage.error('文件超过 10MB')
    return
  }

  importLoading.value = true
  try {
    const fd = new FormData()
    fd.append('file', raw)
    fd.append('overwriteOnConflict', String(importOverwrite.value))

    const epMap: Record<string, string> = {
      excel: '/api/problems/import-excel',
      md: '/api/problems/import-md',
      json: '/api/problems/import-json'
    }
    const res = await fetch(`http://localhost${epMap[format]}`, {
      method: 'POST',
      body: fd
    })
    const data = await res.json()
    if (data.success || data.successCount > 0) {
      const failMsg = data.failedCount > 0 ? `，失败 ${data.failedCount} 道` : ''
      ElMessage.success(`导入完成：成功 ${data.successCount} 道${failMsg}`)
      if (data.failedReasons && data.failedReasons.length > 0) {
        console.warn('导入失败原因：', data.failedReasons)
      }
      importVisible.value = false
      loadProblems()
    } else {
      ElMessage.error(data.message || '导入失败')
    }
  } catch (e) {
    ElMessage.error('导入异常：' + (e as Error).message)
  } finally {
    importLoading.value = false
  }
}

/** 下载对应格式模板 */
const downloadTemplate = () => {
  const format = importFormat.value
  const templates: Record<string, { name: string; content: string; type: string }> = {
    excel: { name: 'CSV模板.xlsx', content: '/CSV模板.xlsx', type: 'file' },
    md: {
      name: '题目模板.md',
      type: 'text',
      content: `# 题号: 1500
# 标题: 设计一个有getMin功能的栈
# 难度: easy
# 标签: 栈,数据结构
# 题目描述:
实现一个特殊的栈，在实现栈的基本功能的基础上，再实现返回栈中最小元素的操作。
# 输入格式:
第一行输入操作数 n，接下来每行输入操作。
# 输出格式:
根据操作输出对应结果。
# 样例输入:
push 3
push 2
getMin
# 样例输出:
3
2
# 解题提示:
使用两个栈，一个存储数据，一个存储当前最小值。
# 代码模板:
public class Solution {
    public void push(int x) {}
    public void pop() {}
    public int top() {}
    public int getMin() {}
}
---
# 题号: 1501
# 标题: 用两个栈实现队列
# 难度: easy
# 标签: 栈,队列
# 题目描述:
用两个栈来实现一个队列，完成队列的 Push 和 Pop 操作。
# 代码模板:
public class Solution {
    public void push(int node) {}
    public int pop() {}
}
`
    },
    json: {
      name: '题目模板.json',
      type: 'text',
      content: JSON.stringify({
        "version": "1.0",
        "generatedAt": "2026-08-01",
        "totalProblems": 2,
        "problemRange": { "from": 1, "to": 2 },
        "problems": [
          {
            "id": 1,
            "title": "合并两个有序链表",
            "difficulty": "简单",
            "category": "链表",
            "problem": {
              "description": "将两个升序链表合并为一个新的升序链表并返回。",
              "inputFormat": "l1: 第一个升序链表的头节点\nl2: 第二个升序链表的头节点",
              "outputFormat": "合并后的升序链表的头节点",
              "constraints": ["两个链表的节点数目范围：[0, 50]", "节点值范围：[-100, 100]", "l1 和 l2 均按非递减顺序排列"],
              "examples": [
                { "input": "l1=[1,2,4], l2=[1,3,4]", "output": "[1,1,2,3,4,4]", "explanation": "两个有序链表交替合并" },
                { "input": "l1=[], l2=[]", "output": "[]", "explanation": "两个空链表" }
              ]
            },
            "solutions": [
              {
                "name": "虚拟头节点+双指针",
                "approach": "使用虚拟头节点简化边界处理，双指针依次比较两个链表当前节点的值，将较小的接入结果链表。",
                "code": "class Solution {\n    public ListNode mergeTwoLists(ListNode l1, ListNode l2) {\n        ListNode dummy = new ListNode(-1), cur = dummy;\n        while (l1 != null && l2 != null) {\n            if (l1.val <= l2.val) { cur.next = l1; l1 = l1.next; }\n            else { cur.next = l2; l2 = l2.next; }\n            cur = cur.next;\n        }\n        cur.next = (l1 != null) ? l1 : l2;\n        return dummy.next;\n    }\n}",
                "timeComplexity": "O(m + n)",
                "spaceComplexity": "O(1)",
                "tags": ["双指针", "链表"]
              }
            ],
            "comparison": null,
            "testCases": [
              { "id": 1, "description": "正常合并两个有序链表", "input": "[1,2,4], [1,3,4]", "expectedOutput": "[1,1,2,3,4,4]" },
              { "id": 2, "description": "两个空链表合并", "input": "null, null", "expectedOutput": "[]" }
            ],
            "testResult": { "total": 10, "passed": 10, "allPassed": true, "crossVerified": false, "solutionCount": 1 },
            "summary": "本题核心是虚拟头节点+双指针模式，易错点在于忘记处理某个链表先遍历完的情况。"
          },
          {
            "id": 2,
            "title": "全排列",
            "difficulty": "中等",
            "category": "回溯",
            "problem": {
              "description": "给定一个没有重复数字的序列，返回其所有可能的全排列。",
              "inputFormat": "nums: 整数数组",
              "outputFormat": "所有全排列组成的列表",
              "constraints": ["数组长度范围：[0, 6]", "数组元素互不相同"],
              "examples": [
                { "input": "[1,2,3]", "output": "[[1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]]", "explanation": "共 6 种排列" }
              ]
            },
            "solutions": [
              {
                "name": "used数组回溯",
                "approach": "维护 used 数组标记已选元素，逐位确定放哪个数。",
                "code": "class Solution {\n    public List<List<Integer>> permute(int[] nums) {\n        List<List<Integer>> res = new ArrayList<>();\n        backtrack(nums, new boolean[nums.length], new ArrayList<>(), res);\n        return res;\n    }\n    void backtrack(int[] nums, boolean[] used, List<Integer> path, List<List<Integer>> res) {\n        if (path.size() == nums.length) { res.add(new ArrayList<>(path)); return; }\n        for (int i = 0; i < nums.length; i++) {\n            if (used[i]) continue;\n            used[i] = true; path.add(nums[i]);\n            backtrack(nums, used, path, res);\n            path.remove(path.size() - 1); used[i] = false;\n        }\n    }\n}",
                "timeComplexity": "O(n × n!)",
                "spaceComplexity": "O(n)",
                "tags": ["回溯", "used数组"]
              },
              {
                "name": "交换元素法",
                "approach": "通过交换数组元素固定位置，不需要 used 数组，空间更优。",
                "code": "class Solution {\n    public List<List<Integer>> permute(int[] nums) {\n        List<List<Integer>> res = new ArrayList<>();\n        dfs(nums, 0, res);\n        return res;\n    }\n    void dfs(int[] nums, int start, List<List<Integer>> res) {\n        if (start == nums.length) { List<Integer> path = new ArrayList<>(); for (int x : nums) path.add(x); res.add(path); return; }\n        for (int i = start; i < nums.length; i++) {\n            swap(nums, start, i); dfs(nums, start + 1, res); swap(nums, start, i);\n        }\n    }\n    void swap(int[] nums, int a, int b) { int t = nums[a]; nums[a] = nums[b]; nums[b] = t; }\n}",
                "timeComplexity": "O(n × n!)",
                "spaceComplexity": "O(1)",
                "tags": ["回溯", "in-place"]
              }
            ],
            "comparison": {
              "table": [
                { "solution": "used数组回溯", "timeComplexity": "O(n × n!)", "spaceComplexity": "O(n)", "useCase": "逻辑清晰，适合初学者理解回溯框架", "limitation": "额外 O(n) 布尔数组" },
                { "solution": "交换元素法", "timeComplexity": "O(n × n!)", "spaceComplexity": "O(1)", "useCase": "追求空间最优，代码更简洁", "limitation": "需要转为 List 操作" }
              ],
              "performanceNote": "时间复杂度相同，交换法省去 used 数组 O(n) 空间。",
              "interviewTip": "先写 used 数组法，再提出交换法作为空间优化。"
            },
            "testCases": [
              { "id": 1, "description": "三元素全排列", "input": "[1,2,3]", "expectedOutput": "6 种排列" },
              { "id": 2, "description": "空数组", "input": "[]", "expectedOutput": "[[]]" }
            ],
            "testResult": { "total": 24, "passed": 24, "allPassed": true, "crossVerified": true, "solutionCount": 2 },
            "summary": "回溯经典题，掌握 used 数组法与交换法两种写法。"
          }
        ]
      }, null, 2)
    }
  }

  const t = templates[format]
  if (t.type === 'file') {
    const a = document.createElement('a')
    a.href = t.content
    a.download = t.name
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
  } else {
    const blob = new Blob([t.content], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = t.name
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }
  ElMessage.success('模板已开始下载')
}

/** 下载 Excel/CSV 模板文件（静态 xlsx） */
const downloadExcelTemplate = () => {
  const a = document.createElement('a')
  a.href = '/CSV模板.xlsx'
  a.download = 'CSV模板.xlsx'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  ElMessage.success('模板已开始下载')
}

const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

const getDifficultyType = (difficulty: string) => {
  const map: Record<string, any> = { easy: 'success', medium: 'warning', hard: 'danger' }
  return map[difficulty] || 'info'
}

const getDifficultyLabel = (difficulty: string) => {
  const map: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难' }
  return map[difficulty] || difficulty
}

/* ================== AI 生成题目 ================== */

const aiDialogVisible = ref(false)
const aiPreviewVisible = ref(false)
const aiGenerating = ref(false)
const aiProgressText = ref('')
const aiStreamContent = ref('')   // 流式累积的原始文本（用于调试展示）
const overwriteOnConflict = ref(false)

// AI 配置表单
const aiConfigForm = reactive({
  knowledgePoints: [] as string[],
  difficulty: 'medium',
  count: 3,
  language: 'general',
  style: 'standard',
  additionalRequirements: ''
})

// AI 生成的题目列表
const aiGeneratedProblems = ref<any[]>([])

// AI 单题编辑弹窗
const aiEditVisible = ref(false)
const aiEditForm = ref({
  draftId: '',
  problemNo: '',
  title: '',
  difficulty: 'medium',
  tags: '',
  description: '',
  inputFormat: '',
  outputFormat: '',
  sampleInput: '',
  sampleOutput: '',
  hint: '',
  template: ''
})

// 所有可用的标签候选（与现有 tagOptions 兼容 + 扩展）
const aiTagOptions = [
  '数组', '链表', '栈', '队列', '哈希表', '树', '二叉树', '图', '堆', '并查集',
  '动态规划', '回溯', '贪心', '分治', '排序', '查找', '二分查找', '字符串', '数学',
  '位运算', '模拟', '递归', '广度优先搜索', '深度优先搜索', '滑动窗口', '前缀和', '双指针'
]

const aiDifficultyOptions = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' }
]

const aiLanguageOptions = [
  { label: '通用编程题', value: 'general' },
  { label: 'Java 专项', value: 'java' },
  { label: 'Python 专项', value: 'python' },
  { label: 'C++ 专项', value: 'cpp' },
  { label: 'JavaScript 专项', value: 'javascript' }
]

const aiStyleOptions = [
  { label: '常规题', value: 'standard' },
  { label: '变式题', value: 'variant' },
  { label: '场景应用题', value: 'scenario' }
]

/** 打开 AI 配置弹窗 */
const handleAIGenerate = () => {
  aiConfigForm.knowledgePoints = []
  aiConfigForm.difficulty = 'medium'
  aiConfigForm.count = 3
  aiConfigForm.language = 'general'
  aiConfigForm.style = 'standard'
  aiConfigForm.additionalRequirements = ''
  aiGeneratedProblems.value = []
  aiStreamContent.value = ''
  aiDialogVisible.value = true
}

/** 调用 AI 生成题目（SSE 流式） */
const handleAIGenerateSubmit = () => {
  if (aiConfigForm.count < 1 || aiConfigForm.count > 10) {
    ElMessage.warning('题目数量需在 1~10 之间')
    return
  }
  if (aiConfigForm.knowledgePoints.length === 0) {
    ElMessage.warning('请至少选择 1 个知识点')
    return
  }

  aiGenerating.value = true
  aiProgressText.value = '正在调用 DeepSeek 生成题目...'
  aiStreamContent.value = ''
  aiGeneratedProblems.value = []

  // 准备 fetch + ReadableStream
  fetch('http://localhost/api/ai/generate-problems/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(aiConfigForm)
  }).then(async response => {
    if (!response.ok) {
      throw new Error('HTTP ' + response.status)
    }
    const reader = response.body!.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let finalResult: any = null

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // SSE 事件以 \n\n 分隔
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const event of events) {
        const lines = event.split('\n')
        let eventName = 'message'
        let dataStr = ''
        for (const line of lines) {
          if (line.startsWith('event:')) eventName = line.substring(6).trim()
          else if (line.startsWith('data:')) dataStr += line.substring(5).trim()
        }
        if (!dataStr) continue

        if (eventName === 'status') {
          aiProgressText.value = JSON.parse(dataStr).message || '生成中...'
        } else if (eventName === 'delta') {
          aiStreamContent.value += dataStr
        } else if (eventName === 'done') {
          finalResult = JSON.parse(dataStr)
        } else if (eventName === 'error') {
          ElMessage.error('生成失败: ' + (JSON.parse(dataStr).message || ''))
        }
      }
    }

    if (finalResult && finalResult.success) {
      aiGeneratedProblems.value = finalResult.problems.map((p: any) => ({
        ...p,
        selected: true   // 默认全部选中
      }))
      aiProgressText.value = `成功生成 ${finalResult.problems.length} 道题目`
      ElMessage.success('生成完成')
      // 关闭配置弹窗，打开预览弹窗
      aiDialogVisible.value = false
      aiPreviewVisible.value = true
    } else {
      ElMessage.error(finalResult?.message || '生成失败，请重试')
    }
  }).catch(err => {
    console.error('AI 生成异常:', err)
    ElMessage.error('AI 生成失败: ' + err.message)
  }).finally(() => {
    aiGenerating.value = false
  })
}

/** 删除单道草稿 */
const handleRemoveGenerated = (index: number) => {
  aiGeneratedProblems.value.splice(index, 1)
}

/** 一键批量添加到题库（逐题调单题入库 API，避开 batch 接口的 500） */
const handleBatchAddToLibrary = async () => {
  const selected = aiGeneratedProblems.value.filter(p => p.selected)
  if (selected.length === 0) {
    ElMessage.warning('请至少勾选 1 道题目')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定将选中的 ${selected.length} 道题目添加到题库？`,
      '批量入库',
      { type: 'info' }
    )
  } catch { return }

  // === 逐题调单题入库接口：POST /api/problems ===
  // 该接口已实现"题号为空/冲突 → 自动分配新题号"逻辑
  let successCount = 0
  let failedCount = 0
  const failedReasons: string[] = []
  const succeededDraftIds = new Set<string>()

  ElMessage.info(`开始入库 ${selected.length} 道题...`)

  for (let i = 0; i < selected.length; i++) {
    const p = selected[i]
    try {
      // 组装和 OJProblem 实体一致的 payload
      const fullDescription = buildAIDescription(p)

      const body = {
        problemNo: p.problemNo || '',  // 空 → 后端自动分配
        title: p.title,
        difficulty: p.difficulty || 'medium',
        tags: p.tags || '',
        description: fullDescription,
        template: p.template || '',
        status: 'ACTIVE'
      }

      const response = await fetch('http://localhost/api/problems', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      })
      const data = await response.json()
      if (data.success) {
        successCount++
        if (p.draftId) succeededDraftIds.add(p.draftId)
      } else {
        failedCount++
        failedReasons.push(`第 ${i + 1} 道「${p.title}」：${data.message || '未知错误'}`)
      }
    } catch (err) {
      failedCount++
      failedReasons.push(`第 ${i + 1} 道「${p.title}」：${(err as Error).message}`)
      console.error('入库异常:', err)
    }
  }

  // 显示结果
  ElMessageBox.alert(
    `成功入库 ${successCount} 道，失败 ${failedCount} 道${failedReasons.length ? '\n\n失败原因：\n' + failedReasons.join('\n') : ''}`,
    '批量入库完成',
    { type: successCount > 0 ? 'success' : 'error' }
  )

  // 移除入库成功的草稿
  if (succeededDraftIds.size > 0) {
    aiGeneratedProblems.value = aiGeneratedProblems.value.filter(
      p => !succeededDraftIds.has(p.draftId)
    )
    loadData()
    if (aiGeneratedProblems.value.length === 0) {
      aiPreviewVisible.value = false
    }
  }
}

/** 把 AI 生成的字段拼成结构化描述（与手动编辑的题面一致） */
const buildAIDescription = (p: any): string => {
  const parts: string[] = []
  if (p.description) parts.push(p.description)
  if (p.inputFormat) parts.push(`**输入格式**\n\n${p.inputFormat}`)
  if (p.outputFormat) parts.push(`**输出格式**\n\n${p.outputFormat}`)
  if (p.sampleInput) parts.push(`**样例输入**\n\n\`\`\`\n${p.sampleInput}\n\`\`\``)
  if (p.sampleOutput) parts.push(`**样例输出**\n\n\`\`\`\n${p.sampleOutput}\n\`\`\``)
  if (p.hint) parts.push(`**解题提示**\n\n${p.hint}`)
  return parts.join('\n\n')
}

/** 复制 AI 生成的题目到普通编辑弹窗（单题编辑入口） */
const handleEditGenerated = (p: any) => {
  // 复制一份到 AI 专用编辑表单（深拷贝，避免双向影响草稿）
  aiEditForm.value = {
    draftId: p.draftId,
    problemNo: p.problemNo || '',
    title: p.title || '',
    difficulty: p.difficulty || 'medium',
    tags: p.tags || '',
    description: p.description || '',
    inputFormat: p.inputFormat || '',
    outputFormat: p.outputFormat || '',
    sampleInput: p.sampleInput || '',
    sampleOutput: p.sampleOutput || '',
    hint: p.hint || '',
    template: p.template || ''
  }
  aiEditVisible.value = true
}

/** 保存 AI 编辑 → 写回草稿卡片 → 关闭编辑弹窗（仍在 AI 预览页） */
const saveAIEdit = () => {
  if (!aiEditForm.value.title) {
    ElMessage.warning('请输入题目标题')
    return
  }
  const target = aiGeneratedProblems.value.find(p => p.draftId === aiEditForm.value.draftId)
  if (target) {
    target.problemNo    = aiEditForm.value.problemNo
    target.title        = aiEditForm.value.title
    target.difficulty   = aiEditForm.value.difficulty
    target.tags         = aiEditForm.value.tags
    target.description  = aiEditForm.value.description
    target.inputFormat  = aiEditForm.value.inputFormat
    target.outputFormat = aiEditForm.value.outputFormat
    target.sampleInput  = aiEditForm.value.sampleInput
    target.sampleOutput = aiEditForm.value.sampleOutput
    target.hint         = aiEditForm.value.hint
    target.template     = aiEditForm.value.template
  }
  ElMessage.success('已保存到草稿（点击「批量添加」时生效）')
  aiEditVisible.value = false
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>OJ题目管理</h2>
      <div>
        <el-popover
          placement="bottom-start"
          :width="380"
          trigger="hover"
          popper-class="import-rule-popover"
        >
          <template #reference>
            <el-button :icon="Upload" @click="handleBatchImport">批量导入</el-button>
          </template>
          <div class="import-rule">
            <h4>📋 导入 Excel 题目规则</h4>
            <ul>
              <li>支持 <b>.xlsx / .xls</b> 格式（CSV 需先另存为 xlsx）</li>
              <li><b>必填列</b>：<span class="req">题号</span>、<span class="req">标题</span>、<span class="req">难度</span></li>
              <li><b>选填列</b>：标签 / 题目描述 / 输入格式 / 输出格式 / 样例输入 / 样例输出 / 解题提示 / 代码模板</li>
              <li>难度值：<code>easy</code> / <code>medium</code> / <code>hard</code></li>
              <li>标签：英文逗号分隔，如 <code>数组,哈希表</code></li>
              <li>题号冲突时：默认<b>自动重新分配</b>（勾选下方「覆盖」可改为覆盖更新）</li>
              <li>第一行 = 表头，第二行起 = 题目数据</li>
            </ul>
            <el-button type="primary" link :icon="Download" @click="downloadExcelTemplate">
              下载 CSV 模板
            </el-button>
          </div>
        </el-popover>
        <el-button :icon="Download" @click="handleExport">导出</el-button>
        <el-button type="success" :icon="MagicStick" @click="handleAIGenerate">AI 生成题目</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增题目</el-button>
      </div>
    </div>

    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.keyword" placeholder="题号/标题/标签" clearable style="width: 200px" />
        <el-select v-model="searchForm.difficulty" placeholder="难度" clearable style="width: 100px">
          <el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 100px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="pagedProblems" v-loading="loading" stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="problemNo" label="题号" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="difficulty" label="难度" width="80">
          <template #default="{ row }">
            <el-tag :type="getDifficultyType(row.difficulty)" size="small">
              {{ getDifficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="tag in row.tags" :key="tag" size="small" style="margin-right: 4px">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submissionCount" label="提交数" width="80" />
        <el-table-column prop="acRate" label="通过率" width="80">
          <template #default="{ row }">
            <span :class="row.acRate >= 50 ? 'color-success' : row.acRate >= 30 ? 'color-warning' : 'color-danger'">
              {{ row.acRate }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              inline-prompt
              active-value="online"
              inactive-value="offline"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="110" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="(p) => { page = p }"
          @size-change="(s) => { pageSize = s; page = 1 }"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" @closed="formRef?.resetFields()">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="题号" prop="problemNo">
          <el-input v-model="formData.problemNo" :disabled="!!formData.id" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-radio-group v-model="formData.difficulty">
            <el-radio v-for="item in difficultyOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="formData.tags" multiple placeholder="选择标签" style="width: 100%">
            <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="6" placeholder="支持Markdown格式" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio value="online">上线</el-radio>
            <el-radio value="offline">下线</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="题目详情" width="800px">
      <template v-if="currentProblem">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="题号">{{ currentProblem.problemNo }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ currentProblem.title }}</el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-tag :type="getDifficultyType(currentProblem.difficulty)" size="small">
              {{ getDifficultyLabel(currentProblem.difficulty) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentProblem.status === 'online' ? '上线' : '下线' }}</el-descriptions-item>
          <el-descriptions-item label="提交数">{{ currentProblem.submissionCount }}</el-descriptions-item>
          <el-descriptions-item label="通过率">{{ currentProblem.acRate }}%</el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <el-tag v-for="tag in currentProblem.tags" :key="tag" size="small" style="margin-right: 4px">{{ tag }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">题目描述</el-divider>
        <div class="problem-description">{{ currentProblem.description }}</div>

        <el-divider content-position="left">代码模板</el-divider>
        <el-tabs v-if="Object.keys(currentProblem.template || {}).length > 0">
          <el-tab-pane v-for="lang in languageOptions" :key="lang.value" :label="lang.label" :name="lang.value">
            <pre class="code-block">{{ currentProblem.template?.[lang.value] || '暂无模板' }}</pre>
          </el-tab-pane>
        </el-tabs>
        <el-empty v-else description="暂无代码模板" />
      </template>
    </el-dialog>

    <!-- ============ AI 生成题目 - 配置弹窗 ============ -->
    <el-dialog
      v-model="aiDialogVisible"
      title="AI 生成题目 - 配置参数"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px" :disabled="aiGenerating">
        <el-form-item label="知识点 / 标签" required>
          <el-select
            v-model="aiConfigForm.knowledgePoints"
            multiple
            filterable
            allow-create
            placeholder="选择或输入知识点（至少 1 个）"
            style="width: 100%"
          >
            <el-option v-for="tag in aiTagOptions" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="aiConfigForm.difficulty">
            <el-radio v-for="item in aiDifficultyOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题目数量">
          <el-input-number v-model="aiConfigForm.count" :min="1" :max="10" />
          <span style="margin-left: 12px; color: #909399; font-size: 12px">建议 1~5 道，避免超时</span>
        </el-form-item>
        <el-form-item label="语言 / 题型">
          <el-select v-model="aiConfigForm.language" style="width: 100%">
            <el-option v-for="item in aiLanguageOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目风格">
          <el-radio-group v-model="aiConfigForm.style">
            <el-radio v-for="item in aiStyleOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="额外要求">
          <el-input
            v-model="aiConfigForm.additionalRequirements"
            type="textarea"
            :rows="3"
            placeholder="可选：例如数据范围、特殊约束等"
          />
        </el-form-item>
        <el-form-item v-if="aiGenerating" label="进度">
          <el-progress :percentage="100" :indeterminate="true" :duration="1" />
          <div style="margin-top: 8px; color: #409eff; font-size: 13px">{{ aiProgressText }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiDialogVisible = false" :disabled="aiGenerating">取消</el-button>
        <el-button type="primary" :loading="aiGenerating" :icon="MagicStick" @click="handleAIGenerateSubmit">
          {{ aiGenerating ? '生成中...' : '开始生成' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ============ AI 生成题目 - 预览与编辑弹窗 ============ -->
    <el-dialog
      v-model="aiPreviewVisible"
      title="AI 生成结果预览"
      width="1100px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div class="ai-preview-toolbar">
        <div>
          <el-checkbox v-model="overwriteOnConflict">题号冲突时覆盖</el-checkbox>
          <span style="margin-left: 16px; color: #909399; font-size: 12px">
            已勾选 {{ aiGeneratedProblems.filter(p => p.selected).length }} / {{ aiGeneratedProblems.length }} 道
          </span>
        </div>
        <div>
          <el-button @click="aiDialogVisible = true; aiPreviewVisible = false">重新生成</el-button>
          <el-button type="primary" :icon="Promotion" @click="handleBatchAddToLibrary">
            批量添加到题库
          </el-button>
        </div>
      </div>

      <div v-if="aiGeneratedProblems.length === 0" style="padding: 60px 0">
        <el-empty description="暂无生成结果" />
      </div>

      <div v-else class="ai-problem-list">
        <div v-for="(p, idx) in aiGeneratedProblems" :key="p.draftId" class="ai-problem-card">
          <div class="card-header">
            <el-checkbox v-model="p.selected" />
            <span class="problem-no">{{ p.problemNo || `待分配（#${idx+1}）` }}</span>
            <el-input v-model="p.title" class="title-input" placeholder="题目标题" />
            <el-select v-model="p.difficulty" style="width: 100px">
              <el-option v-for="item in aiDifficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button link type="danger" @click="handleRemoveGenerated(idx)">删除</el-button>
            <el-button link type="primary" @click="handleEditGenerated(p)">单题编辑</el-button>
          </div>

          <div class="card-body">
            <el-form label-width="80px" size="small">
              <el-form-item label="标签">
                <el-select
                  v-model="p.tags"
                  multiple
                  filterable
                  allow-create
                  placeholder="逗号分隔或从候选中选择"
                  style="width: 100%"
                >
                  <el-option v-for="tag in aiTagOptions" :key="tag" :label="tag" :value="tag" />
                </el-select>
              </el-form-item>

              <el-form-item label="题目描述">
                <el-input
                  v-model="p.description"
                  type="textarea"
                  :rows="4"
                  placeholder="支持 HTML 片段，<p>/<strong>/<code>/<ul>/<li> 等"
                />
              </el-form-item>

              <div class="form-row">
                <el-form-item label="输入格式" style="flex: 1">
                  <el-input v-model="p.inputFormat" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item label="输出格式" style="flex: 1; margin-left: 12px">
                  <el-input v-model="p.outputFormat" type="textarea" :rows="2" />
                </el-form-item>
              </div>

              <div class="form-row">
                <el-form-item label="样例输入" style="flex: 1">
                  <el-input v-model="p.sampleInput" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item label="样例输出" style="flex: 1; margin-left: 12px">
                  <el-input v-model="p.sampleOutput" type="textarea" :rows="2" />
                </el-form-item>
              </div>

              <el-form-item label="解题提示">
                <el-input v-model="p.hint" type="textarea" :rows="2" />
              </el-form-item>

              <el-form-item label="代码模板">
                <el-input
                  v-model="p.template"
                  type="textarea"
                  :rows="6"
                  :placeholder="`${p.language} 代码模板`"
                  style="font-family: 'Fira Code', Consolas, monospace; font-size: 12px"
                />
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="aiPreviewVisible = false">关闭</el-button>
        <el-button type="primary" :icon="Promotion" @click="handleBatchAddToLibrary">
          批量添加 {{ aiGeneratedProblems.filter(p => p.selected).length }} 道到题库
        </el-button>
      </template>
    </el-dialog>

    <!-- AI 单题编辑弹窗（在 AI 预览页内打开，保存后返回 AI 预览） -->
    <el-dialog
      v-model="aiEditVisible"
      title="单题编辑（AI 草稿）"
      width="900px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px" size="default">
        <el-form-item label="题号">
          <el-input v-model="aiEditForm.problemNo" placeholder="留空则入库时由后端自动分配（推荐）" />
          <div style="color:#909399;font-size:12px;margin-top:4px">
            提示：留空 → 入库时自动取数据库最大题号 + 1
          </div>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="aiEditForm.title" placeholder="题目标题" />
        </el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="aiEditForm.difficulty">
            <el-radio-button value="easy">简单</el-radio-button>
            <el-radio-button value="medium">中等</el-radio-button>
            <el-radio-button value="hard">困难</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="aiEditForm.tags" placeholder="多个标签用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="题面描述">
          <el-input v-model="aiEditForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="输入格式">
          <el-input v-model="aiEditForm.inputFormat" />
        </el-form-item>
        <el-form-item label="输出格式">
          <el-input v-model="aiEditForm.outputFormat" />
        </el-form-item>
        <el-form-item label="样例输入">
          <el-input v-model="aiEditForm.sampleInput" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="样例输出">
          <el-input v-model="aiEditForm.sampleOutput" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="解题提示">
          <el-input v-model="aiEditForm.hint" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="代码模板">
          <el-input v-model="aiEditForm.template" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAIEdit">保存到草稿</el-button>
      </template>
    </el-dialog>

    <!-- ============ 批量导入弹窗（支持 Excel / MD / JSON） ============ -->
    <el-dialog
      v-model="importVisible"
      title="批量导入题目"
      width="640px"
      :close-on-click-modal="false"
    >
      <!-- 格式选择 -->
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        <template #title>
          <div style="display:flex;align-items:center;gap:12px">
            <span>请选择导入格式：</span>
            <el-radio-group v-model="importFormat" size="default">
              <el-radio-button value="excel">
                <span style="display:flex;align-items:center;gap:4px">
                  <el-icon><Upload /></el-icon> Excel
                </span>
              </el-radio-button>
              <el-radio-button value="md">
                <span style="display:flex;align-items:center;gap:4px">
                  <el-icon><View /></el-icon> MD 文档
                </span>
              </el-radio-button>
              <el-radio-button value="json">
                <span style="display:flex;align-items:center;gap:4px">
                  <el-icon><MagicStick /></el-icon> JSON
                </span>
              </el-radio-button>
            </el-radio-group>
          </div>
        </template>
      </el-alert>

      <el-form label-width="120px" size="default">
        <el-form-item label="题号冲突处理">
          <el-switch
            v-model="importOverwrite"
            active-text="覆盖（题号冲突时更新）"
            inactive-text="自动重新分配（题号冲突时新建）"
          />
          <div style="color:#909399;font-size:12px;margin-top:4px">
            推荐保持 <b>关闭</b>：避免误覆盖已有题目
          </div>
        </el-form-item>

        <!-- Excel 上传 -->
        <el-form-item v-if="importFormat === 'excel'" label="选择 Excel">
          <el-upload
            action=""
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
            :on-exceed="() => ElMessage.warning('只支持 1 个文件')"
          >
            <el-button type="primary" :icon="Upload">点击选择 Excel 文件</el-button>
            <template #tip>
              <div style="color:#909399;font-size:12px;margin-top:8px">
                支持 .xlsx / .xls，最大 10MB。<b>不知道格式？</b>
                <el-button type="primary" link size="small" @click="downloadTemplate">下载模板</el-button>
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- MD 上传 -->
        <el-form-item v-if="importFormat === 'md'" label="选择 MD">
          <el-upload
            action=""
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".md,.markdown"
            :on-change="handleFileChange"
            :on-exceed="() => ElMessage.warning('只支持 1 个文件')"
          >
            <el-button type="primary" :icon="Upload">点击选择 MD 文件</el-button>
            <template #tip>
              <div style="color:#909399;font-size:12px;margin-top:8px">
                支持 .md / .markdown，最大 10MB。格式说明见模板。
                <el-button type="primary" link size="small" @click="downloadTemplate">下载模板</el-button>
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- JSON 上传 -->
        <el-form-item v-if="importFormat === 'json'" label="选择 JSON">
          <el-upload
            action=""
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".json"
            :on-change="handleFileChange"
            :on-exceed="() => ElMessage.warning('只支持 1 个文件')"
          >
            <el-button type="primary" :icon="Upload">点击选择 JSON 文件</el-button>
            <template #tip>
              <div style="color:#909399;font-size:12px;margin-top:8px">
                支持 .json 格式（数组或 { problems: [...] } 包装对象），最大 10MB。
                <el-button type="primary" link size="small" @click="downloadTemplate">下载模板</el-button>
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="importVisible = false" :disabled="importLoading">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.color-success { color: #67c23a; }
.color-warning { color: #e6a23c; }
.color-danger  { color: #f56c6c; }

.problem-description {
  white-space: pre-wrap;
  line-height: 1.6;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
  min-height: 60px;
}

.code-block {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 4px;
  font-family: 'Fira Code', Consolas, monospace;
  font-size: 13px;
  overflow-x: auto;
  white-space: pre;
  margin: 0;
}

/* ============ AI 生成题目 - 预览弹窗样式 ============ */
.ai-preview-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 16px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 16px;
}

.ai-problem-list {
  max-height: 65vh;
  overflow-y: auto;
  padding-right: 8px;
}

.import-rule {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;

  h4 {
    margin: 0 0 10px 0;
    color: #303133;
    font-size: 15px;
  }

  ul {
    margin: 0 0 10px 0;
    padding-left: 18px;
  }

  li {
    margin: 4px 0;
  }

  code {
    background: #f5f7fa;
    padding: 1px 6px;
    border-radius: 3px;
    font-size: 12px;
    color: #d63384;
  }

  .req {
    color: #f56c6c;
    font-weight: bold;
  }
}

.ai-problem-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 16px;
  transition: all 0.2s;
  background: #fff;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: #f5f7fa;
    border-bottom: 1px solid #ebeef5;
    border-radius: 6px 6px 0 0;

    .problem-no {
      font-family: 'Fira Code', Consolas, monospace;
      font-weight: bold;
      color: #409eff;
      min-width: 50px;
    }

    .title-input {
      flex: 1;
    }
  }

  .card-body {
    padding: 16px 20px;
  }
}

.form-row {
  display: flex;
  gap: 0;
  width: 100%;
}
</style>
