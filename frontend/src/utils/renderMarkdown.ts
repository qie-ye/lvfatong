/**
 * Markdown 渲染工具 — 法律咨询和意见书共用
 * 支持：结构化答案（事实分析/法律适用/结论等卡片）、基础 Markdown（加粗/标题/列表/换行）
 */

export function renderMarkdown(text: string): string {
  if (!text) return ''
  const structured = renderStructuredAnswer(text)
  if (structured) return structured

  return renderBasicMarkdown(text)
}

function renderStructuredAnswer(text: string): string {
  const source = text.replace(/\r\n/g, '\n')
  const lines = source.split('\n')
  // Match headings like: "1. **事实分析**：" or "**法律适用**：" or "# 结论：" or "事实分析："
  const headingRe = /^\s*(?:#{1,6}\s*)?(?:\d+[\.、．]?\s*)?\*{0,2}\s*(事实分析|法律适用|法律依据|结论|建议|处理建议|风险提示)\s*\*{0,2}\s*[：:]\s*(.*)$/

  const intro: string[] = []
  const sections: Array<{ title: string; body: string[] }> = []
  let current: { title: string; body: string[] } | null = null

  for (const rawLine of lines) {
    const line = rawLine.trimEnd()
    const m = line.match(headingRe)
    if (m) {
      if (current) sections.push(current)
      current = { title: m[1], body: m[2] ? [m[2]] : [] }
      continue
    }
    if (current) {
      current.body.push(line)
    } else {
      intro.push(line)
    }
  }
  if (current) sections.push(current)
  if (sections.length === 0) return ''

  const introText = intro.join('\n').trim()
  const introHtml = introText ? `<div class="md-intro">${renderBasicMarkdown(introText)}</div>` : ''
  const sectionHtml = sections.map(s => {
    const body = s.body.join('\n').trim()
    return `<div class="md-section"><div class="md-section-title">${s.title}</div><div class="md-section-body">${renderBasicMarkdown(body)}</div></div>`
  }).join('')

  return introHtml + sectionHtml
}

function renderBasicMarkdown(text: string): string {
  if (!text) return ''
  const normalized = text
    .replace(/\s*(#{1,6}\s*[一二三四五六七八九十0-9]+[、\.．])/g, '\n\n$1')
    .replace(/([。！？；])\s*([一二三四五六七八九十]+、)/g, '$1\n\n$2')
    .replace(/([。！？；])\s*(\d+[\.、．])/g, '$1\n\n$2')
    .replace(/\s*(\d+[\.、]\s*[^：:\n]{1,20}[：:])/g, '\n\n$1')
    .replace(/\r\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')

  // Escape HTML first
  const escaped = normalized
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  return escaped
    .replace(/^###\s*(.+)$/gm, '<div class="md-h3">$1</div>')
    .replace(/^##\s*(.+)$/gm, '<div class="md-h2">$1</div>')
    .replace(/^#\s*(.+)$/gm, '<div class="md-h1">$1</div>')
    .replace(/^([一二三四五六七八九十]+、[^\n]*)$/gm, '<div class="md-h3">$1</div>')
    .replace(/^(\d+[\.、．]\s*[^\n：:]{1,30}[：:])$/gm, '<div class="md-h3">$1</div>')
    .replace(/^\s*[-*]\s+(.+)$/gm, '<div class="md-li">• $1</div>')
    .replace(/^\s*(\d+)[\.\)]\s+(.+)$/gm, '<div class="md-li"><span class="md-idx">$1.</span> $2</div>')
    .replace(/^\*\*(.*?)\*\*$/gm, '<div class="md-bold-line"><strong>$1</strong></div>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/(^|<br>)(\d+[\.、]\s*[^：:<br>]{1,20}[：:])/g, '$1<strong>$2</strong>')
    .replace(/\n{2,}/g, '<br><br>')
    .replace(/\n/g, '<br>')
}
