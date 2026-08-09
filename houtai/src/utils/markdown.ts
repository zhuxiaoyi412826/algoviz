import { marked } from 'marked'

marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: false,
  mangle: false
})

function preprocessMarkdown(str: string): string {
  if (!str) return ''
  let s = String(str)

  s = s.replace(/\r\n?/g, '\n')

  // 先提取代码块占位，避免误处理代码块内的内容
  const codeBlocks: string[] = []
  s = s.replace(/```[\s\S]*?```/g, (m) => {
    codeBlocks.push(m)
    return '\x00CODEBLOCK' + (codeBlocks.length - 1) + '\x00'
  })

  // 在关键分隔符前插入换行（仅对非代码块区域生效）
  s = s.replace(/\s*(#{1,6})\s*(?=[^#\s])/g, '\n$1 ')
  s = s.replace(/(\S)\s+(?=\*\*[^*]{2,}\*\*)/g, '$1\n\n')
  s = s.replace(/(\S)\s+(?=\|[-\s])/g, '$1\n')
  s = s.replace(/(\s)-\s+/g, '\n- ')
  s = s.replace(/(\s)(\d+)\.\s+(?=\d+\.)/g, '\n$2. ')
  s = s.replace(/(\S)(`[^`]+`)/g, '$1 $2')
  s = s.replace(/(`[^`]+`)(\S)/g, '$1 $2')

  // 恢复代码块
  s = s.replace(/\x00CODEBLOCK(\d+)\x00/g, (_m, idx) => {
    return '\n' + codeBlocks[parseInt(idx)] + '\n'
  })

  // 确保代码块标记独占一行
  s = s.replace(/\n```\s*\n/g, '\n```\n')
  s = s.replace(/\n```([\s\S]*?)```\n/g, (_m, code) => {
    return '\n```\n' + code.trim() + '\n```\n'
  })

  // 清理多余空行
  s = s.replace(/\n{3,}/g, '\n\n')

  return s
}

export function renderMarkdown(str: string): string {
  if (!str) return ''
  const processed = preprocessMarkdown(str)
  try {
    return marked.parse(processed) as string
  } catch (e) {
    console.warn('Markdown 解析失败:', e)
    return '<p>' + str + '</p>'
  }
}
