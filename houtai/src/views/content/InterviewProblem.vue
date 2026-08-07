<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem,
  ElInput, ElSelect, ElOption, ElMessage, ElMessageBox, ElPagination,
  ElInputNumber, ElRadioGroup, ElRadio, ElDivider, ElCheckbox, ElSwitch,
  ElDescriptions, ElDescriptionsItem, ElTabs, ElTabPane, ElEmpty, ElUpload
} from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, Upload, Download, View, MagicStick, Promotion, FullScreen, Aim } from '@element-plus/icons-vue'

interface InterviewProblem {
  id: number
  problemNo: string
  title: string
  difficulty: 'easy' | 'medium' | 'hard'
  tags: string[]
  category: string
  description: string
  inputFormat: string
  outputFormat: string
  solution: string
  isFrequent: boolean
  status: 'online' | 'offline'
  viewCount: number
  createTime: string
}

const mockProblems: InterviewProblem[] = [
  { id: 1, problemNo: 'MS001', title: '两数之和', difficulty: 'easy', tags: ['数组', '哈希表'], category: '数组', description: '给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出和为目标值 target 的那两个整数，并返回它们的数组下标。\n\n你可以假设每种输入只会对应一个答案，并且你不能使用两次相同的元素。\n\n返回的下标可以是任意顺序。', inputFormat: '数组 nums，整数 target', outputFormat: '两个索引的数组', solution: '使用哈希表，一次遍历。遍历数组时，对于每个元素，检查 target - nums[i] 是否在哈希表中。如果存在，直接返回两个索引；否则将当前元素及其索引存入哈希表。时间复杂度 O(n)，空间复杂度 O(n)。', isFrequent: true, status: 'online', viewCount: 1256, createTime: '2026-08-01 10:00:00' },
  { id: 2, problemNo: 'MS002', title: '反转链表', difficulty: 'easy', tags: ['链表', '递归'], category: '链表', description: '给你单链表的头节点 head ，请你反转链表，并返回反转后的链表。', inputFormat: '链表头节点 head', outputFormat: '反转后的链表头节点', solution: '迭代法：使用三个指针 prev、curr、next。遍历链表，每次将 curr.next 指向 prev，然后依次推进指针。时间复杂度 O(n)，空间复杂度 O(1)。\n\n递归法：先递归到链表尾部，然后在回溯时反转指针。时间复杂度 O(n)，空间复杂度 O(n)。', isFrequent: true, status: 'online', viewCount: 980, createTime: '2026-08-01 10:05:00' },
  { id: 3, problemNo: 'MS003', title: 'LRU 缓存', difficulty: 'medium', tags: ['链表', '哈希表', '设计'], category: '设计', description: '请你设计并实现一个满足 LRU 缓存约束的数据结构。你需要实现 LRUCache 类：\n\n- LRUCache(int capacity) 以正整数作为容量 capacity 初始化 LRU 缓存\n- int get(int key) 如果关键字 key 存在于缓存中，则返回关键字的值，否则返回 -1\n- void put(int key, int value) 如果关键字 key 已经存在，则变更其数据值 value；如果不存在，则向缓存中插入该组 key-value。如果插入操作导致关键字数量超过 capacity，则应该逐出最久未使用的关键字。\n\n函数 get 和 put 必须以 O(1) 的平均时间复杂度运行。', inputFormat: '操作序列', outputFormat: '操作结果序列', solution: '使用哈希表 + 双向链表实现。哈希表以 O(1) 查找节点，双向链表维护访问顺序。每次 get/put 时，将节点移到链表头部；超容量时，删除链表尾部节点。', isFrequent: true, status: 'online', viewCount: 756, createTime: '2026-08-02 09:30:00' },
  { id: 4, problemNo: 'MS004', title: '无重复字符的最长子串', difficulty: 'medium', tags: ['字符串', '滑动窗口'], category: '字符串', description: '给定一个字符串 s，找出其中不含有重复字符的最长子串的长度。', inputFormat: '字符串 s', outputFormat: '最长子串长度', solution: '滑动窗口 + 哈希表。维护一个窗口 [left, right]，使用哈希表记录每个字符最近出现的位置。当遇到重复字符时，移动 left 指针跳过重复字符。时间复杂度 O(n)。', isFrequent: true, status: 'online', viewCount: 634, createTime: '2026-08-02 14:20:00' },
  { id: 5, problemNo: 'MS005', title: '二叉树的层序遍历', difficulty: 'medium', tags: ['树', '广度优先搜索'], category: '树', description: '给你二叉树的根节点 root，返回其节点值的层序遍历。（即逐层地，从左到右访问所有节点）。', inputFormat: '二叉树根节点 root', outputFormat: '层序遍历结果数组', solution: '使用队列进行 BFS。首先将根节点入队，然后循环取出当前层的所有节点，将它们的值存入结果，并将它们的子节点入队。时间复杂度 O(n)，空间复杂度 O(n)。', isFrequent: false, status: 'online', viewCount: 423, createTime: '2026-08-03 11:00:00' },
  { id: 6, problemNo: 'MS006', title: '合并区间', difficulty: 'medium', tags: ['数组', '排序'], category: '数组', description: '以数组 intervals 表示若干个区间的集合，其中单个区间为 intervals[i] = [starti, endi]。请你合并所有重叠的区间，并返回一个不重叠的区间数组，该数组需恰好覆盖输入中的所有区间。', inputFormat: '区间数组 intervals', outputFormat: '合并后的区间数组', solution: '先按区间起始位置排序，然后遍历排序后的区间。如果当前区间与结果数组中最后一个区间重叠，合并它们；否则直接添加。时间复杂度 O(n log n)。', isFrequent: false, status: 'online', viewCount: 356, createTime: '2026-08-03 16:45:00' },
  { id: 7, problemNo: 'MS007', title: '接雨水', difficulty: 'hard', tags: ['数组', '双指针', '动态规划'], category: '数组', description: '给定 n 个非负整数表示每个宽度为 1 的柱子的高度图，计算按此排列的柱子，下雨之后能接多少雨水。', inputFormat: '高度数组 height', outputFormat: '能接的雨水量', solution: '方法一：双指针法。从两端向中间靠拢，每次移动较矮的指针，累加水量。时间复杂度 O(n)，空间复杂度 O(1)。\n\n方法二：动态规划。分别计算左右两侧最高柱子，取较小值减去当前高度。时间复杂度 O(n)，空间复杂度 O(n)。\n\n方法三：单调栈。维护一个单调递减栈，遇到较高柱子时结算水量。', isFrequent: true, status: 'online', viewCount: 289, createTime: '2026-08-04 08:30:00' },
  { id: 8, problemNo: 'MS008', title: '正则表达式匹配', difficulty: 'hard', tags: ['字符串', '动态规划'], category: '动态规划', description: '给你一个字符串 s 和一个字符规律 p，请你来实现一个支持 \'.\' 和 \'*\' 的正则表达式匹配。\n\n\'.\' 匹配任意单个字符\n\'*\' 匹配零个或多个前面的那一个元素\n所谓匹配，是要涵盖整个字符串 s 的，而不是部分字符串。', inputFormat: '字符串 s 和模式 p', outputFormat: '是否匹配（true/false）', solution: '动态规划。dp[i][j] 表示 s[0:i] 与 p[0:j] 是否匹配。考虑三种情况：字符相等、p 为 \'.\'、p 为 \'*\'（需要匹配零次或多次）。时间复杂度 O(m*n)，空间复杂度 O(m*n)。', isFrequent: false, status: 'online', viewCount: 178, createTime: '2026-08-04 13:15:00' },
  { id: 9, problemNo: 'MS009', title: '最长回文子串', difficulty: 'medium', tags: ['字符串', '动态规划'], category: '字符串', description: '给你一个字符串 s，找到 s 中最长的回文子串。', inputFormat: '字符串 s', outputFormat: '最长回文子串', solution: '方法一：中心扩展法。从每个位置向两边扩展，分别处理奇数和偶数长度的回文。时间复杂度 O(n²)，空间复杂度 O(1)。\n\n方法二：动态规划。dp[i][j] 表示 s[i:j+1] 是否为回文串。时间复杂度 O(n²)，空间复杂度 O(n²)。\n\n方法三：Manacher 算法，时间复杂度 O(n)。', isFrequent: true, status: 'online', viewCount: 567, createTime: '2026-08-05 10:20:00' },
  { id: 10, problemNo: 'MS010', title: '数组中的第K个最大元素', difficulty: 'medium', tags: ['数组', '堆', '快速选择'], category: '数组', description: '给定整数数组 nums 和整数 k，请返回数组中第 k 个最大的元素。\n\n请注意，你需要找的是数组排序后的第 k 个最大的元素，而不是第 k 个不同的元素。你必须设计并实现时间复杂度为 O(n) 的算法解决此问题。', inputFormat: '数组 nums 和整数 k', outputFormat: '第 k 个最大的元素', solution: '方法一：小顶堆。维护一个大小为 k 的小顶堆，遍历数组时不断更新堆顶。时间复杂度 O(n log k)，空间复杂度 O(k)。\n\n方法二：快速选择（基于快速排序的分区思想）。每次 partition 后判断 pivot 位置与 k 的关系，递归处理一边。平均时间复杂度 O(n)。\n\n方法三：使用系统排序后取倒数第 k 个，时间复杂度 O(n log n)。', isFrequent: true, status: 'online', viewCount: 445, createTime: '2026-08-05 15:40:00' }
]

const allProblems = ref<InterviewProblem[]>([])
const allProblemsCache = ref<InterviewProblem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogFullscreen = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('新增面试题')

const importVisible = ref(false)
const importLoading = ref(false)
const importOverwrite = ref(false)

const searchForm = reactive({
  keyword: '',
  difficulty: '',
  status: '',
  tag: '',
  category: ''
})

const formData = reactive<Partial<InterviewProblem>>({
  id: undefined,
  problemNo: '',
  title: '',
  difficulty: 'medium',
  tags: [],
  category: '',
  description: '',
  inputFormat: '',
  outputFormat: '',
  solution: '',
  isFrequent: false,
  status: 'offline'
})

const formRef = ref()
const currentProblem = ref<InterviewProblem | null>(null)

const rules = {
  problemNo: [{ required: true, message: '请输入题号', trigger: 'blur' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  description: [{ required: true, message: '请输入题目描述', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
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
  '数组', '链表', '栈', '队列', '哈希表', '树', '二叉树', '图', '堆', '并查集',
  '动态规划', '回溯', '贪心', '分治', '排序', '查找', '二分查找', '字符串', '数学',
  '位运算', '模拟', '递归', '广度优先搜索', '深度优先搜索', '滑动窗口', '前缀和', '双指针', '设计'
]

const categoryOptions = [
  { label: '数组', value: '数组' },
  { label: '链表', value: '链表' },
  { label: '栈', value: '栈' },
  { label: '队列', value: '队列' },
  { label: '树', value: '树' },
  { label: '图', value: '图' },
  { label: '动态规划', value: '动态规划' },
  { label: '回溯', value: '回溯' },
  { label: '贪心', value: '贪心' },
  { label: '分治', value: '分治' },
  { label: '排序', value: '排序' },
  { label: '查找', value: '查找' },
  { label: '字符串', value: '字符串' },
  { label: '数学', value: '数学' },
  { label: '设计', value: '设计' }
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
    await new Promise(r => setTimeout(r, 300))
    allProblemsCache.value = JSON.parse(JSON.stringify(mockProblems))
    applyFilter()
  } catch (error) {
    console.error('加载面试题失败:', error)
    ElMessage.error('加载面试题失败')
  } finally {
    loading.value = false
  }
}

const applyFilter = () => {
  const kw = searchForm.keyword.trim().toLowerCase()
  const diff = searchForm.difficulty
  const st = searchForm.status
  const tag = searchForm.tag
  const cat = searchForm.category

  allProblems.value = allProblemsCache.value.filter(p => {
    if (kw) {
      const hit =
        (p.problemNo || '').toLowerCase().includes(kw) ||
        (p.title || '').toLowerCase().includes(kw) ||
        (p.tags || []).some(t => t.toLowerCase().includes(kw))
      if (!hit) return false
    }
    if (diff && p.difficulty !== diff) return false
    if (st && p.status !== st) return false
    if (tag && !(p.tags || []).includes(tag)) return false
    if (cat && p.category !== cat) return false
    return true
  })
  total.value = allProblems.value.length
  const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
  if (page.value > maxPage) page.value = 1
}

const handleSearch = () => {
  page.value = 1
  applyFilter()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.difficulty = ''
  searchForm.status = ''
  searchForm.tag = ''
  searchForm.category = ''
  page.value = 1
  applyFilter()
}

const handleAdd = () => {
  dialogTitle.value = '新增面试题'
  dialogFullscreen.value = false
  Object.assign(formData, {
    id: undefined,
    problemNo: '',
    title: '',
    difficulty: 'medium',
    tags: [],
    category: '',
    description: '',
    inputFormat: '',
    outputFormat: '',
    solution: '',
    isFrequent: false,
    status: 'offline'
  })
  dialogVisible.value = true
}

const handleEdit = (row: InterviewProblem) => {
  dialogTitle.value = '编辑面试题'
  dialogFullscreen.value = false
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleView = (row: InterviewProblem) => {
  currentProblem.value = row
  detailVisible.value = true
}

const handleDelete = async (row: InterviewProblem) => {
  try {
    await ElMessageBox.confirm(`确定要删除题目「${row.title}」吗？`, '提示', { type: 'warning' })
    const idx = allProblemsCache.value.findIndex(p => p.id === row.id)
    if (idx > -1) {
      allProblemsCache.value.splice(idx, 1)
      applyFilter()
      ElMessage.success('删除成功')
    }
  } catch {}
}

const handleToggleStatus = async (row: InterviewProblem) => {
  const targetStatus = row.status
  const statusLabel = targetStatus === 'online' ? '上线' : '下线'
  try {
    await ElMessageBox.confirm(`确定要${statusLabel}该题目吗？`, '提示', { type: 'warning' })
    ElMessage.success(`${statusLabel}成功`)
  } catch {
    row.status = targetStatus === 'online' ? 'offline' : 'online'
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    try {
      const now = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')

      if (formData.id) {
        const idx = allProblemsCache.value.findIndex(p => p.id === formData.id)
        if (idx > -1) {
          allProblemsCache.value[idx] = {
            ...allProblemsCache.value[idx],
            problemNo: formData.problemNo!,
            title: formData.title!,
            difficulty: formData.difficulty!,
            tags: formData.tags!,
            category: formData.category!,
            description: formData.description!,
            inputFormat: formData.inputFormat || '',
            outputFormat: formData.outputFormat || '',
            solution: formData.solution || '',
            isFrequent: formData.isFrequent || false,
            status: formData.status!
          }
        }
        ElMessage.success('编辑成功')
      } else {
        const newId = allProblemsCache.value.length > 0
          ? Math.max(...allProblemsCache.value.map(p => p.id)) + 1
          : 1
        allProblemsCache.value.unshift({
          id: newId,
          problemNo: formData.problemNo!,
          title: formData.title!,
          difficulty: formData.difficulty!,
          tags: formData.tags!,
          category: formData.category!,
          description: formData.description!,
          inputFormat: formData.inputFormat || '',
          outputFormat: formData.outputFormat || '',
          solution: formData.solution || '',
          isFrequent: formData.isFrequent || false,
          status: formData.status!,
          viewCount: 0,
          createTime: now
        })
        ElMessage.success('新增成功')
      }

      dialogVisible.value = false
      applyFilter()
    } catch (error) {
      console.error('保存面试题失败:', error)
      ElMessage.error('保存失败')
    }
  })
}

const onDialogClosed = () => {
  formRef.value?.resetFields()
  dialogFullscreen.value = false
}

const handleBatchImport = () => {
  importOverwrite.value = false
  importVisible.value = true
}

const handleFileChange = async (uploadFile: any) => {
  if (!uploadFile || !uploadFile.raw) return
  const raw: File = uploadFile.raw

  if (!/\.json$/i.test(raw.name)) {
    ElMessage.error('仅支持 .json 文件')
    return
  }
  if (raw.size > 10 * 1024 * 1024) {
    ElMessage.error('文件超过 10MB')
    return
  }

  importLoading.value = true
  try {
    const text = await raw.text()
    const data = JSON.parse(text)
    const problems: InterviewProblem[] = Array.isArray(data) ? data : (data.problems || [])

    if (problems.length === 0) {
      ElMessage.warning('文件中没有题目数据')
      return
    }

    let successCount = 0
    let failCount = 0

    for (const p of problems) {
      if (!p.problemNo || !p.title) {
        failCount++
        continue
      }

      const existing = allProblemsCache.value.find(x => x.problemNo === p.problemNo)
      if (existing && !importOverwrite.value) {
        failCount++
        continue
      }

      if (existing && importOverwrite.value) {
        Object.assign(existing, {
          title: p.title,
          difficulty: p.difficulty || 'medium',
          tags: p.tags || [],
          category: p.category || '',
          description: p.description || '',
          inputFormat: p.inputFormat || '',
          outputFormat: p.outputFormat || '',
          solution: p.solution || '',
          isFrequent: p.isFrequent || false,
          status: p.status || 'offline'
        })
      } else {
        const newId = allProblemsCache.value.length > 0
          ? Math.max(...allProblemsCache.value.map(x => x.id)) + 1
          : allProblems.value.length + 1
        allProblemsCache.value.push({
          id: newId,
          problemNo: p.problemNo,
          title: p.title,
          difficulty: p.difficulty || 'medium',
          tags: p.tags || [],
          category: p.category || '',
          description: p.description || '',
          inputFormat: p.inputFormat || '',
          outputFormat: p.outputFormat || '',
          solution: p.solution || '',
          isFrequent: p.isFrequent || false,
          status: p.status || 'offline',
          viewCount: 0,
          createTime: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
        })
      }
      successCount++
    }

    ElMessage.success(`导入完成：成功 ${successCount} 道${failCount > 0 ? `，失败 ${failCount} 道` : ''}`)
    importVisible.value = false
    applyFilter()
  } catch (e) {
    ElMessage.error('导入异常：' + (e as Error).message)
  } finally {
    importLoading.value = false
  }
}

const downloadTemplate = () => {
  const template = [
    {
      problemNo: 'MS001',
      title: '示例题目',
      difficulty: 'easy',
      tags: ['数组', '哈希表'],
      category: '数组',
      description: '题目描述（支持 Markdown 格式）',
      inputFormat: '输入格式说明',
      outputFormat: '输出格式说明',
      solution: '参考答案',
      isFrequent: true,
      status: 'online'
    }
  ]
  const blob = new Blob([JSON.stringify(template, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '面试题目模板.json'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('模板已开始下载')
}

const handleExport = () => {
  const exportData = allProblems.value.map(p => ({
    problemNo: p.problemNo,
    title: p.title,
    difficulty: p.difficulty,
    tags: p.tags,
    category: p.category,
    description: p.description,
    inputFormat: p.inputFormat,
    outputFormat: p.outputFormat,
    solution: p.solution,
    isFrequent: p.isFrequent,
    status: p.status,
    viewCount: p.viewCount,
    createTime: p.createTime
  }))

  const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `interview_problems_${new Date().getTime()}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('JSON 文件导出成功')
}

const getDifficultyType = (difficulty: string) => {
  const map: Record<string, string> = { easy: 'success', medium: 'warning', hard: 'danger' }
  return map[difficulty] || 'info'
}

const getDifficultyLabel = (difficulty: string) => {
  const map: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难' }
  return map[difficulty] || difficulty
}

const getCategoryLabel = (category: string) => {
  return categoryOptions.find(c => c.value === category)?.label || category
}

/* ================== AI 生成 ================== */

const aiDialogVisible = ref(false)
const aiPreviewVisible = ref(false)
const aiGenerating = ref(false)
const aiProgressText = ref('')
const aiGeneratedProblems = ref<any[]>([])

const aiConfigForm = reactive({
  knowledgePoints: [] as string[],
  difficulty: 'medium',
  count: 3,
  category: '',
  additionalRequirements: ''
})

const aiEditVisible = ref(false)
const aiEditForm = ref({
  draftId: '',
  problemNo: '',
  title: '',
  difficulty: 'medium',
  tags: '',
  category: '',
  description: '',
  inputFormat: '',
  outputFormat: '',
  solution: '',
  isFrequent: false
})

const aiTagOptions = [
  '数组', '链表', '栈', '队列', '哈希表', '树', '二叉树', '图', '堆', '并查集',
  '动态规划', '回溯', '贪心', '分治', '排序', '查找', '二分查找', '字符串', '数学',
  '位运算', '模拟', '递归', '广度优先搜索', '深度优先搜索', '滑动窗口', '前缀和', '双指针', '设计'
]

const aiDifficultyOptions = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' }
]

const handleAIGenerate = () => {
  aiConfigForm.knowledgePoints = []
  aiConfigForm.difficulty = 'medium'
  aiConfigForm.count = 3
  aiConfigForm.category = ''
  aiConfigForm.additionalRequirements = ''
  aiGeneratedProblems.value = []
  aiDialogVisible.value = true
}

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
  aiProgressText.value = '正在生成面试题...'
  aiGeneratedProblems.value = []

  setTimeout(() => {
    const mockResult = []
    const diffLabels: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难' }
    const cat = aiConfigForm.category || aiConfigForm.knowledgePoints[0] || '数组'

    for (let i = 0; i < aiConfigForm.count; i++) {
      const diff = aiConfigForm.difficulty
      mockResult.push({
        draftId: `draft_${Date.now()}_${i}`,
        problemNo: '',
        title: `${cat}相关面试题 ${i + 1}`,
        difficulty: diff,
        tags: aiConfigForm.knowledgePoints.join(','),
        category: cat,
        description: `这是一道关于${cat}的${diffLabels[diff]}难度面试题。\n\n## 题目描述\n\n请实现相关功能。`,
        inputFormat: '输入参数说明',
        outputFormat: '输出结果说明',
        solution: '## 解题思路\n\n分析问题，确定算法策略。\n\n## 参考实现\n\n提供完整的代码实现。',
        isFrequent: Math.random() > 0.5,
        selected: true
      })
    }

    aiGeneratedProblems.value = mockResult
    aiProgressText.value = `成功生成 ${mockResult.length} 道面试题`
    aiGenerating.value = false
    aiDialogVisible.value = false
    aiPreviewVisible.value = true
    ElMessage.success('生成完成')
  }, 1500)
}

const handleRemoveGenerated = (index: number) => {
  aiGeneratedProblems.value.splice(index, 1)
}

const handleBatchAddToLibrary = async () => {
  const selected = aiGeneratedProblems.value.filter(p => p.selected)
  if (selected.length === 0) {
    ElMessage.warning('请至少勾选 1 道题目')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定将选中的 ${selected.length} 道面试题添加到题库？`,
      '批量入库',
      { type: 'info' }
    )
  } catch { return }

  let successCount = 0
  let failedCount = 0

  for (const p of selected) {
    try {
      const now = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
      const newId = allProblemsCache.value.length > 0
        ? Math.max(...allProblemsCache.value.map(x => x.id)) + 1
        : allProblems.value.length + 1

      allProblemsCache.value.unshift({
        id: newId,
        problemNo: p.problemNo || `MS${String(newId).padStart(3, '0')}`,
        title: p.title,
        difficulty: p.difficulty || 'medium',
        tags: (p.tags || '').split(',').filter(Boolean),
        category: p.category || '',
        description: p.description || '',
        inputFormat: p.inputFormat || '',
        outputFormat: p.outputFormat || '',
        solution: p.solution || '',
        isFrequent: p.isFrequent || false,
        status: 'offline',
        viewCount: 0,
        createTime: now
      })
      successCount++
    } catch (err) {
      failedCount++
      console.error('入库异常:', err)
    }
  }

  ElMessageBox.alert(
    `成功入库 ${successCount} 道，失败 ${failedCount} 道`,
    '批量入库完成',
    { type: successCount > 0 ? 'success' : 'error' }
  )

  if (successCount > 0) {
    aiGeneratedProblems.value = aiGeneratedProblems.value.filter(p => !p.selected)
    applyFilter()
    if (aiGeneratedProblems.value.length === 0) {
      aiPreviewVisible.value = false
    }
  }
}

const handleEditGenerated = (p: any) => {
  aiEditForm.value = {
    draftId: p.draftId,
    problemNo: p.problemNo || '',
    title: p.title || '',
    difficulty: p.difficulty || 'medium',
    tags: p.tags || '',
    category: p.category || '',
    description: p.description || '',
    inputFormat: p.inputFormat || '',
    outputFormat: p.outputFormat || '',
    solution: p.solution || '',
    isFrequent: p.isFrequent || false
  }
  aiEditVisible.value = true
}

const saveAIEdit = () => {
  if (!aiEditForm.value.title) {
    ElMessage.warning('请输入题目标题')
    return
  }
  const target = aiGeneratedProblems.value.find(p => p.draftId === aiEditForm.value.draftId)
  if (target) {
    target.problemNo = aiEditForm.value.problemNo
    target.title = aiEditForm.value.title
    target.difficulty = aiEditForm.value.difficulty
    target.tags = aiEditForm.value.tags
    target.category = aiEditForm.value.category
    target.description = aiEditForm.value.description
    target.inputFormat = aiEditForm.value.inputFormat
    target.outputFormat = aiEditForm.value.outputFormat
    target.solution = aiEditForm.value.solution
    target.isFrequent = aiEditForm.value.isFrequent
  }
  ElMessage.success('已保存到草稿')
  aiEditVisible.value = false
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>面试题目管理</h2>
      <div>
        <el-button :icon="Upload" @click="handleBatchImport">批量导入</el-button>
        <el-button :icon="Download" @click="handleExport">导出</el-button>
        <el-button type="success" :icon="MagicStick" @click="handleAIGenerate">AI 生成题目</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增面试题</el-button>
      </div>
    </div>

    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.keyword" placeholder="题号/标题/标签" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-select v-model="searchForm.tag" placeholder="标签" clearable filterable style="width: 140px">
          <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
        </el-select>
        <el-select v-model="searchForm.difficulty" placeholder="难度" clearable style="width: 100px">
          <el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="searchForm.category" placeholder="分类" clearable style="width: 120px">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 100px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="pagedProblems" v-loading="loading" stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="problemNo" label="题号" width="80" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="difficulty" label="难度" width="80">
          <template #default="{ row }">
            <el-tag :type="getDifficultyType(row.difficulty)" size="small">
              {{ getDifficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="tag in row.tags" :key="tag" size="small" style="margin-right: 4px; margin-bottom: 2px">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ getCategoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isFrequent" label="高频" width="60" align="center">
          <template #default="{ row }">
            <span v-if="row.isFrequent" style="color: #e6a23c; font-weight: bold;">✓</span>
            <span v-else style="color: #c0c4cc;">-</span>
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
        <el-table-column prop="viewCount" label="浏览数" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="140" />
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
          @current-change="(p: number) => { page = p }"
          @size-change="(s: number) => { pageSize = s; page = 1 }"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="720px"
      :fullscreen="dialogFullscreen"
      :top="'calc(10vh - 30px)'"
      @closed="onDialogClosed"
    >
      <template #header>
        <div class="dialog-header">
          <span>{{ dialogTitle }}</span>
          <el-button
            :icon="dialogFullscreen ? Aim : FullScreen"
            circle
            size="small"
            @click="dialogFullscreen = !dialogFullscreen"
          />
        </div>
      </template>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="题号" prop="problemNo">
          <el-input v-model="formData.problemNo" placeholder="如 MS001" :disabled="!!formData.id" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入题目标题" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-radio-group v-model="formData.difficulty">
            <el-radio v-for="item in difficultyOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="formData.tags" multiple filterable allow-create placeholder="选择或输入标签" style="width: 100%">
            <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="6" placeholder="支持 Markdown 格式" />
        </el-form-item>
        <el-form-item label="输入格式">
          <el-input v-model="formData.inputFormat" type="textarea" :rows="2" placeholder="描述输入参数的格式" />
        </el-form-item>
        <el-form-item label="输出格式">
          <el-input v-model="formData.outputFormat" type="textarea" :rows="2" placeholder="描述输出结果的格式" />
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input v-model="formData.solution" type="textarea" :rows="6" placeholder="支持 Markdown 格式，包含解题思路和代码实现" style="font-family: 'Fira Code', Consolas, monospace; font-size: 13px" />
        </el-form-item>
        <el-form-item label="高频标记">
          <el-switch v-model="formData.isFrequent" active-text="高频" inactive-text="普通" />
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="面试题详情" width="800px">
      <template v-if="currentProblem">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="题号">{{ currentProblem.problemNo }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ currentProblem.title }}</el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-tag :type="getDifficultyType(currentProblem.difficulty)" size="small">
              {{ getDifficultyLabel(currentProblem.difficulty) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="分类">
            <el-tag size="small" effect="plain">{{ getCategoryLabel(currentProblem.category) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="高频">
            <span v-if="currentProblem.isFrequent" style="color: #e6a23c; font-weight: bold;">✓ 高频</span>
            <span v-else>普通</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentProblem.status === 'online' ? '上线' : '下线' }}</el-descriptions-item>
          <el-descriptions-item label="浏览数">{{ currentProblem.viewCount }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentProblem.createTime }}</el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <el-tag v-for="tag in currentProblem.tags" :key="tag" size="small" style="margin-right: 4px">{{ tag }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">题目描述</el-divider>
        <div class="problem-description">{{ currentProblem.description }}</div>

        <template v-if="currentProblem.inputFormat">
          <el-divider content-position="left">输入格式</el-divider>
          <div class="problem-description">{{ currentProblem.inputFormat }}</div>
        </template>

        <template v-if="currentProblem.outputFormat">
          <el-divider content-position="left">输出格式</el-divider>
          <div class="problem-description">{{ currentProblem.outputFormat }}</div>
        </template>

        <el-divider content-position="left">参考答案</el-divider>
        <pre class="code-block">{{ currentProblem.solution || '暂无参考答案' }}</pre>
      </template>
    </el-dialog>

    <!-- AI 生成配置弹窗 -->
    <el-dialog
      v-model="aiDialogVisible"
      title="AI 生成面试题 - 配置参数"
      width="600px"
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
        <el-form-item label="分类">
          <el-select v-model="aiConfigForm.category" placeholder="选择题目分类（可选）" clearable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="额外要求">
          <el-input
            v-model="aiConfigForm.additionalRequirements"
            type="textarea"
            :rows="3"
            placeholder="可选：例如特定考点、难度要求等"
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

    <!-- AI 生成结果预览弹窗 -->
    <el-dialog
      v-model="aiPreviewVisible"
      title="AI 生成结果预览"
      width="1100px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div class="ai-preview-toolbar">
        <div>
          <span style="color: #909399; font-size: 12px">
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
                <el-input v-model="p.tags" placeholder="多个标签用英文逗号分隔" />
              </el-form-item>
              <el-form-item label="分类">
                <el-select v-model="p.category" placeholder="选择分类" style="width: 100%">
                  <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="题目描述">
                <el-input
                  v-model="p.description"
                  type="textarea"
                  :rows="4"
                  placeholder="支持 Markdown 格式"
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
              <el-form-item label="参考答案">
                <el-input
                  v-model="p.solution"
                  type="textarea"
                  :rows="5"
                  placeholder="解题思路和代码实现"
                  style="font-family: 'Fira Code', Consolas, monospace; font-size: 12px"
                />
              </el-form-item>
              <el-form-item label="高频标记">
                <el-switch v-model="p.isFrequent" size="small" />
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

    <!-- AI 单题编辑弹窗 -->
    <el-dialog
      v-model="aiEditVisible"
      title="单题编辑（AI 草稿）"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px" size="default">
        <el-form-item label="题号">
          <el-input v-model="aiEditForm.problemNo" placeholder="留空则入库时由系统自动分配" />
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
        <el-form-item label="分类">
          <el-select v-model="aiEditForm.category" placeholder="选择分类" style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目描述">
          <el-input v-model="aiEditForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="输入格式">
          <el-input v-model="aiEditForm.inputFormat" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="输出格式">
          <el-input v-model="aiEditForm.outputFormat" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input v-model="aiEditForm.solution" type="textarea" :rows="6" style="font-family: 'Fira Code', Consolas, monospace; font-size: 13px" />
        </el-form-item>
        <el-form-item label="高频标记">
          <el-switch v-model="aiEditForm.isFrequent" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAIEdit">保存到草稿</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog
      v-model="importVisible"
      title="批量导入面试题"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        <template #title>
          仅支持 JSON 格式导入。请确保文件格式符合模板要求。
        </template>
      </el-alert>

      <el-form label-width="120px">
        <el-form-item label="题号冲突处理">
          <el-switch
            v-model="importOverwrite"
            active-text="覆盖（题号冲突时更新）"
            inactive-text="跳过（题号冲突时跳过）"
          />
          <div style="color:#909399;font-size:12px;margin-top:4px">
            推荐保持 <b>关闭</b>：避免误覆盖已有题目
          </div>
        </el-form-item>

        <el-form-item label="选择 JSON 文件">
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
.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  font-size: 16px;
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

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
  white-space: pre-wrap;
  margin: 0;
}

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
