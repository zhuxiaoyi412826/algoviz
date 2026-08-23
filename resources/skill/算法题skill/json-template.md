# JSON 输出模板

每道题严格按照以下 JSON Schema 输出，多道题合并到一个 JSON 文件中。
适用于对接 OJ 平台判题、小程序刷题系统导入、自动化评测。

---

## 文件整体结构

```json
{
  "version": "1.0",
  "generatedAt": "YYYY-MM-DD",
  "totalProblems": 5,
  "problemRange": { "from": 1, "to": 5 },
  "problems": [ ... ]
}
```

## 单道题结构

```json
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
      {
        "input": "l1=[1,2,4], l2=[1,3,4]",
        "output": "[1,1,2,3,4,4]",
        "explanation": "两个有序链表交替合并"
      },
      {
        "input": "l1=[], l2=[]",
        "output": "[]",
        "explanation": "两个空链表"
      }
    ]
  },
  "solutions": [
    {
      "name": "虚拟头节点+双指针",
      "approach": "使用虚拟头节点简化边界处理，双指针依次比较两个链表当前节点的值，将较小的接入结果链表。",
      "code": "class Solution {\n    public ListNode mergeTwoLists(ListNode l1, ListNode l2) {\n        // 代码内容...\n    }\n}",
      "timeComplexity": "O(m + n)",
      "spaceComplexity": "O(1)",
      "tags": ["双指针", "链表"]
    }
  ],
  "comparison": null,
  "testCases": [
    {
      "id": 1,
      "description": "正常合并两个有序链表",
      "input": "[1,2,4], [1,3,4]",
      "expectedOutput": "[1,1,2,3,4,4]"
    },
    {
      "id": 2,
      "description": "两个空链表合并",
      "input": "null, null",
      "expectedOutput": "[]"
    }
  ],
  "testResult": {
    "total": 10,
    "passed": 10,
    "allPassed": true,
    "crossVerified": false,
    "solutionCount": 1
  },
  "summary": "**题目概述：** 本题属于链表/双指针类别。给定两个升序有序链表，需要合并为一个新的升序链表。核心挑战在于如何高效地遍历两个链表并处理边界情况（空链表、长度差异大等）。该问题模拟了多路归并的基础场景，是合并K个有序链表的简化版本。\n\n**解题方法：** 本题采用两种解法：(1) 虚拟头节点+双指针法——使用dummy节点简化头节点边界处理，双指针依次比较两链表当前节点值，将较小的接入结果链表，直到某一链表遍历完后直接拼接剩余部分。(2) 递归法——每次比较两链表头节点，较小节点的next指向递归合并剩余部分的结果。虚拟头节点法更直观且无递归栈开销，是工程实践中的首选方案。\n\n**时间复杂度：** 两种解法均为O(m+n)，其中m和n分别为两个链表的长度。因为每次比较操作都会将一个链表的节点接入结果，总共需要遍历m+n个节点，每次操作为O(1)的指针操作。两种解法的时间复杂度相同，差异体现在常数因子上。\n\n**空间复杂度：** 虚拟头节点法为O(1)，仅需维护dummy、p、p1、p2四个指针变量，所有操作均为原地指针重定向。递归法为O(m+n)，递归调用栈深度最多为m+n层。虚拟头节点法在空间上明显更优。\n\n**改进方向：** 当前解法已达到理论下界Ω(m+n)，因为必须遍历所有节点。若扩展到K个有序链表合并场景，可改用优先队列（最小堆）将时间复杂度优化为O(N log k)。若允许修改原链表，递归写法代码更简洁但牺牲了空间；实际工程中建议使用迭代法避免栈溢出风险。"
}
```

## 多解法题目的 solutions 和 comparison

当题目有多种解法时（中等及以上必须），`solutions` 数组包含多个对象，`comparison` 必填：

```json
{
  "solutions": [
    {
      "name": "used数组回溯",
      "approach": "维护 used 数组标记已选元素，逐位确定放哪个数。",
      "code": "class Solution {\n    public List<List<Integer>> permute(int[] nums) {\n        // 解法一代码...\n    }\n}",
      "timeComplexity": "O(n × n!)",
      "spaceComplexity": "O(n)",
      "tags": ["回溯", "used数组"]
    },
    {
      "name": "交换元素法",
      "approach": "通过交换数组元素固定位置，不需要 used 数组，空间更优。",
      "code": "class Solution {\n    public List<List<Integer>> permute(int[] nums) {\n        // 解法二代码...\n    }\n}",
      "timeComplexity": "O(n × n!)",
      "spaceComplexity": "O(1)",
      "tags": ["回溯", "in-place"]
    }
  ],
  "comparison": {
    "table": [
      {
        "solution": "used数组回溯",
        "timeComplexity": "O(n × n!)",
        "spaceComplexity": "O(n)",
        "useCase": "逻辑清晰，适合初学者理解回溯框架",
        "limitation": "额外 O(n) 布尔数组"
      },
      {
        "solution": "交换元素法",
        "timeComplexity": "O(n × n!)",
        "spaceComplexity": "O(1)",
        "useCase": "追求空间最优，代码更简洁",
        "limitation": "需要转为 List 操作"
      }
    ],
    "performanceNote": "时间复杂度相同，交换法省去 used 数组 O(n) 空间。",
    "interviewTip": "先写 used 数组法，再提出交换法作为空间优化。"
  },
  "testResult": {
    "total": 24,
    "passed": 24,
    "allPassed": true,
    "crossVerified": true,
    "solutionCount": 2
  }
}
```

## 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | int | 是 | 全局递增题号 |
| `title` | string | 是 | 题目名称 |
| `difficulty` | string | 是 | 简单 / 中等 / 困难 |
| `category` | string | 是 | 所属类别 |
| `problem.description` | string | 是 | 题目描述 |
| `problem.inputFormat` | string | 是 | 输入格式说明 |
| `problem.outputFormat` | string | 是 | 输出格式说明 |
| `problem.constraints` | string[] | 是 | 数据范围约束 |
| `problem.examples` | object[] | 是 | 示例（至少 2 个） |
| `solutions` | object[] | 是 | 解法列表（简单题 ≥1，中等 ≥2） |
| `solutions[].name` | string | 是 | 解法名称 |
| `solutions[].approach` | string | 是 | 文字解题思路（无代码） |
| `solutions[].code` | string | 是 | 完整 Java 代码（含 \n 换行） |
| `solutions[].timeComplexity` | string | 是 | 时间复杂度 |
| `solutions[].spaceComplexity` | string | 是 | 空间复杂度 |
| `solutions[].tags` | string[] | 是 | 解法标签关键词 |
| `comparison` | object/null | 中/困难必填 | 多解法对比（简单题为 null） |
| `comparison.table` | object[] | 是 | 对比表格 |
| `comparison.performanceNote` | string | 是 | 性能对比说明 |
| `comparison.interviewTip` | string | 是 | 面试建议 |
| `testCases` | object[] | 是 | 测试用例列表 |
| `testCases[].id` | int | 是 | 用例编号 |
| `testCases[].description` | string | 是 | 用例描述 |
| `testCases[].input` | string | 是 | 输入值 |
| `testCases[].expectedOutput` | string | 是 | 期望输出 |
| `testResult.total` | int | 是 | 测试总数 |
| `testResult.passed` | int | 是 | 通过数 |
| `testResult.allPassed` | boolean | 是 | 是否全部通过 |
| `testResult.crossVerified` | boolean | 是 | 是否多解法交叉验证 |
| `testResult.solutionCount` | int | 是 | 参与验证的解法数 |
| `summary` | string | 是 | 题目总结（严格五步结构，每步3-5句，整体300-500字。各步用 \n\n 分隔：**题目概述：** 算法类别+核心约束+输入特征+应用场景。**解题方法：** 所有解法名称+核心思路+最优解关键洞察。**时间复杂度：** 每种解法复杂度+推导过程+效率对比。**空间复杂度：** 每种解法空间开销+具体用途+优化技巧对比。**改进方向：** 理论下界+变体扩展+工程优化。） |

## 格式要求

1. JSON 必须是合法 UTF-8 编码
2. 代码字段是字符串，换行用 `\n` 转义，不要使用数组
3. `id` 从目标 JSON 文件中读取 max id + 1 递增
4. 简单题 `comparison` 为 null，中/困难必须填写
5. `testResult.crossVerified` 为 true 表示所有解法跑相同用例结果一致
6. 文件顶层为包装对象 `{ version, generatedAt, totalProblems, problemRange, problems }`
7. `solutions[].approach` 只写文字思路，不包含任何代码
8. `solutions[].code` 必须是完整可编译运行的 Java 代码
9. 文件末尾无多余字段
