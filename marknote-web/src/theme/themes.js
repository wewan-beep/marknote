/**
 * 公众号排版主题清单
 * id 会被拼成 .mn-t-{id} 类名，样式定义在 src/styles/themes.css
 */
export const THEMES = [
  { id: 'blue', name: '蓝调', color: '#3b82f6', desc: '默认蓝，稳重通用' },
  { id: 'green', name: '青野', color: '#10b981', desc: '清爽绿，适合技术随笔' },
  { id: 'orange', name: '暖橙', color: '#f59e0b', desc: '暖色调，适合生活记录' },
  { id: 'purple', name: '紫罗兰', color: '#8b5cf6', desc: '紫色块标题，偏文艺' },
  { id: 'ink', name: '墨印', color: '#1a1a1a', desc: '衬线报刊风，适合长文' },
  { id: 'minimal', name: '极简', color: '#9ca3af', desc: '去装饰，只留文字层级' },
  { id: 'tech', name: '科技', color: '#2563eb', desc: '深蓝卡片标题，适合技术文档' },
  { id: 'paper', name: '暖纸', color: '#c08a3e', desc: '米色纸感 + 宋体，适合散文' },
  { id: 'night', name: '夜色', color: '#1c212b', desc: '深色底卡片，适合评测/极客' },
  { id: 'candy', name: '糖果', color: '#ff5c8a', desc: '粉色圆角，适合种草笔记' }
]

export const DEFAULT_THEME = 'blue'

export function themeName(id) {
  const hit = THEMES.find((t) => t.id === id)
  return hit ? hit.name : '蓝调'
}
