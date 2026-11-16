<template>
  <div class="layout" :class="{ 'is-dark': isDark }">
    <!-- 顶栏 -->
    <header class="topbar">
      <div class="brand">
        <div class="logo">M</div>
        <span class="brand-name">MarkNote</span>
      </div>

      <div class="title-box">
        <input
          ref="titleInputRef"
          v-model="title"
          class="title-input"
          maxlength="80"
          placeholder="无标题笔记"
          title="直接改标题，回车立即保存"
          @input="onTitleInput"
          @keydown.enter.prevent="commitTitle"
          @change="onTitleChange"
        />
        <span class="save-state" :class="saveState.cls">{{ saveState.text }}</span>
      </div>

      <div class="actions">
        <el-button size="small" @click="createNote">新建</el-button>
        <el-button size="small" :loading="importing" @click="pickImport">导入 MD</el-button>
        <input
          ref="fileInputRef"
          type="file"
          accept=".md,.markdown,.txt"
          multiple
          class="hidden-file"
          @change="onFilesSelected"
        />
        <el-button size="small" :loading="saving" @click="saveNow">保存</el-button>
        <el-button size="small" @click="exportMd">导出 MD</el-button>

        <el-dropdown trigger="click" @command="pickTheme" class="theme-pick">
          <el-button size="small">
            <span class="theme-dot" :style="{ background: currentThemeColor }"></span>
            {{ currentThemeName }}
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="t in THEMES" :key="t.id" :command="t.id">
                <span class="theme-dot" :style="{ background: t.color }"></span>
                <span class="theme-name">{{ t.name }}</span>
                <span class="theme-desc">{{ t.desc }}</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button size="small" type="primary" plain @click="copyHtml">复制公众号 HTML</el-button>
        <el-switch v-model="syncScroll" size="small" active-text="同步滚动" class="switch" />
        <el-button size="small" @click="toggleTheme">{{ isDark ? '浅色' : '暗色' }}</el-button>
        <el-button size="small" @click="openHistory">历史版本</el-button>
        <el-button size="small" text @click="logout">退出</el-button>
      </div>
    </header>

    <div class="body">
      <!-- 侧边栏 -->
      <aside class="sidebar">
        <div class="sidebar-search">
          <el-input
            v-model="keyword"
            size="small"
            placeholder="搜索笔记标题"
            clearable
            @input="debouncedLoadList"
          />
        </div>

        <div class="sidebar-tabs">
          <span :class="{ active: tab === 'all' }" @click="switchTab('all')">全部 ({{ totalCount }})</span>
          <span :class="{ active: tab === 'trash' }" @click="switchTab('trash')">回收站</span>
        </div>

        <div
          class="note-list"
          :class="{ dragging }"
          @dragover.prevent="onDragOver"
          @dragleave.prevent="onDragLeave"
          @drop.prevent="onDrop"
        >
          <div v-if="dragging" class="drop-hint">松开鼠标即可导入 .md 文件</div>
          <div
            v-for="item in noteList"
            :key="item.id"
            class="note-item"
            :class="{ active: currentNote && currentNote.id === item.id }"
            @click="onItemClick(item)"
          >
            <div class="note-main" @dblclick.stop="renameNote(item)">
              <div class="note-title">{{ item.title || '无标题笔记' }}</div>
              <div class="note-time">{{ formatTime(item.updateTime) }} · {{ charLen(item.content) }} 字</div>
            </div>
            <div class="note-ops" @click.stop>
              <template v-if="tab === 'all'">
                <span class="op" title="重命名" @click="renameNote(item)">✏️</span>
                <span class="op" title="移入回收站" @click="moveToTrash(item)">🗑</span>
              </template>
              <template v-else>
                <span class="op" @click="restoreItem(item)">恢复</span>
                <span class="op danger" @click="removeItem(item)">彻底删除</span>
              </template>
            </div>
          </div>

          <div v-if="!noteList.length" class="empty">
            {{ tab === 'all' ? '还没有笔记，点右上角「新建」开始写' : '回收站是空的' }}
          </div>
        </div>

        <div class="sidebar-foot">
          <span>{{ userInfo?.nickname || userInfo?.username || '未登录' }}</span>
          <span>v1.0</span>
        </div>
      </aside>

      <!-- 编辑区 -->
      <main class="main">
        <div v-if="!currentNote" class="welcome">
          <div class="welcome-inner">
            <h2>选择一篇笔记，或者新建一篇</h2>
            <p>左侧是 Markdown 源码，右侧是公众号风格实时预览；粘贴截图会自动上传并插入图片链接。</p>
            <el-button type="primary" @click="createNote">新建笔记</el-button>
          </div>
        </div>
        <MarkEditor
          v-else
          ref="editorRef"
          v-model="currentNote.content"
          :sync-scroll="syncScroll"
          :theme="articleTheme"
          @change="onContentChange"
          @upload-image="handleUploadImage"
        />
      </main>
    </div>

    <!-- 历史版本 -->
    <el-dialog v-model="historyVisible" title="历史版本" width="860px" top="6vh">
      <div class="history">
        <div class="history-list">
          <div
            v-for="(h, i) in historyList"
            :key="h.id"
            class="history-item"
            :class="{ active: historyIndex === i }"
            @click="historyIndex = i"
          >
            <div class="h-time">{{ formatTime(h.createTime) }}</div>
            <div class="h-title">{{ h.title || '无标题笔记' }}</div>
          </div>
          <el-empty v-if="!historyList.length" description="暂无历史版本" :image-size="70" />
        </div>
        <div class="history-preview">
          <pre v-if="historyIndex >= 0 && historyList[historyIndex]">{{ historyList[historyIndex].content }}</pre>
          <div v-else class="history-tip">选择左侧一条历史版本查看内容</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="historyVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="historyIndex < 0" @click="restoreHistory">恢复到该版本</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkEditor from '@/components/MarkEditor.vue'
import {
  getNoteList, addNote, updateNote, deleteNote, restoreNote,
  removeNote, getNote, getHistoryList
} from '@/api/note'
import { uploadImage } from '@/api/image'
import { useUserStore } from '@/store/user'
import { THEMES, DEFAULT_THEME, themeName } from '@/theme/themes'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const editorRef = ref(null)
const noteList = ref([])
const currentNote = ref(null)
const title = ref('无标题笔记')
const titleInputRef = ref(null)
const keyword = ref('')
const tab = ref('all')
const syncScroll = ref(true)
const isDark = ref(localStorage.getItem('mn_theme') === 'dark')
const saving = ref(false)

/* 导入相关 */
const fileInputRef = ref(null)
const importing = ref(false)
const dragging = ref(false)
let dragTimer = null

const saveState = reactive({ text: '已保存', cls: 'ok' })
const totalCount = computed(() => (tab.value === 'all' ? noteList.value.length : totalCountCache.value))

let totalCountCache = 0
let saveTimer = null
let listTimer = null
let dirty = false

/* ---------- 主题 ---------- */
function applyTheme() {
  if (isDark.value) {
    document.documentElement.setAttribute('data-theme', 'dark')
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
  localStorage.setItem('mn_theme', isDark.value ? 'dark' : 'light')
}
function toggleTheme() {
  isDark.value = !isDark.value
  applyTheme()
}

/* ---------- 文章排版主题 ---------- */
function readArticleTheme() {
  const saved = localStorage.getItem('mn_article_theme')
  return THEMES.some((t) => t.id === saved) ? saved : DEFAULT_THEME
}
const articleTheme = ref(readArticleTheme())
const currentThemeName = computed(() => themeName(articleTheme.value))
const currentThemeColor = computed(
  () => (THEMES.find((t) => t.id === articleTheme.value) || THEMES[0]).color
)

function pickTheme(id) {
  articleTheme.value = id
  localStorage.setItem('mn_article_theme', id)
  ElMessage.success(`已切换到「${themeName(id)}」主题`)
}

/* ---------- 工具 ---------- */
function formatTime(t) {
  if (!t) return '刚刚'
  const d = new Date(String(t).replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return String(t)
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  const pad = (n) => String(n).padStart(2, '0')
  if (sameDay) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
function charLen(str) {
  return str ? str.replace(/\s/g, '').length : 0
}
function setSaveState(text, cls) {
  saveState.text = text
  saveState.cls = cls
}

/* ---------- 列表 ---------- */
async function loadList() {
  const res = await getNoteList({
    isDeleted: tab.value === 'trash' ? 1 : 0,
    keyword: keyword.value || undefined
  })
  noteList.value = (res.data || []).slice()
  if (tab.value === 'all' && !keyword.value) totalCountCache = noteList.value.length
}

function debouncedLoadList() {
  clearTimeout(listTimer)
  listTimer = setTimeout(loadList, 300)
}

function switchTab(t) {
  if (t === tab.value) return
  flushSave()
  tab.value = t
  if (t === 'trash') {
    currentNote.value = null
  }
  loadList()
}

async function onItemClick(item) {
  if (tab.value !== 'all') return
  await flushSave()
  const res = await getNote(item.id)
  currentNote.value = res.data
  title.value = currentNote.value.title || '无标题笔记'
  setSaveState('已保存', 'ok')
}

async function createNote() {
  await flushSave()
  const res = await addNote({ title: '无标题笔记', content: '# 无标题笔记\n\n开始写点什么吧……\n' })
  await loadList()
  currentNote.value = res.data
  title.value = currentNote.value.title || '无标题笔记'
  setSaveState('已保存', 'ok')
  // 新建后把光标放到标题框并全选，直接输入就是新标题
  await nextTick()
  titleInputRef.value?.focus()
  titleInputRef.value?.select()
}

/* ---------- 导入 Markdown ---------- */
const IMPORT_EXT = /\.(md|markdown|mdown|txt)$/i
const MAX_SIZE = 5 * 1024 * 1024

function pickImport() {
  fileInputRef.value?.click()
}

function onFilesSelected(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  importFiles(files)
}

/* 拖拽导入 */
function onDragOver(e) {
  if (e.dataTransfer && Array.from(e.dataTransfer.types || []).includes('Files')) {
    dragging.value = true
    clearTimeout(dragTimer)
  }
}
function onDragLeave() {
  dragTimer = setTimeout(() => { dragging.value = false }, 120)
}
function onDrop(e) {
  dragging.value = false
  clearTimeout(dragTimer)
  const files = Array.from((e.dataTransfer && e.dataTransfer.files) || [])
  importFiles(files)
}

function readFileText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsText(file, 'utf-8')
  })
}

/* 标题优先取第一个 # 一级标题，否则用文件名 */
function pickTitle(fileName, content) {
  const m = content.match(/^\s*#\s+(.+?)\s*$/m)
  if (m && m[1].trim()) {
    return m[1].trim().slice(0, 80)
  }
  return fileName.replace(IMPORT_EXT, '').slice(0, 80) || '无标题笔记'
}

async function importFiles(files) {
  if (importing.value || !files.length) return

  const valid = files.filter((f) => IMPORT_EXT.test(f.name))
  const oversize = valid.filter((f) => f.size > MAX_SIZE)
  const skipped = files.length - valid.length

  if (!valid.length) {
    return ElMessage.warning('请选择 .md / .markdown / .txt 文件')
  }
  if (oversize.length) {
    ElMessage.warning(`${oversize.length} 个文件超过 5MB 已跳过：${oversize.map((f) => f.name).join('、')}`)
  }

  const toImport = valid.filter((f) => f.size <= MAX_SIZE)
  if (!toImport.length) return

  importing.value = true
  let lastId = null
  let success = 0
  const failed = []
  try {
    await flushSave()
    if (tab.value !== 'all') {
      tab.value = 'all'
      keyword.value = ''
    }

    for (const file of toImport) {
      try {
        const content = await readFileText(file)
        const res = await addNote({ title: pickTitle(file.name, content), content })
        if (res && res.data) {
          lastId = res.data.id
          success += 1
        }
      } catch (err) {
        failed.push(file.name)
      }
    }

    await loadList()

    if (lastId) {
      const item = noteList.value.find((n) => n.id === lastId)
      if (item) await onItemClick(item)
    }

    if (success) ElMessage.success(`已导入 ${success} 篇 Markdown 笔记`)
    if (failed.length) ElMessage.error(`导入失败：${failed.join('、')}`)
    if (skipped > 0) ElMessage.info(`已忽略 ${skipped} 个非 Markdown 文件`)
  } finally {
    importing.value = false
  }
}

async function moveToTrash(item) {
  try {
    await ElMessageBox.confirm(`确定把「${item.title}」移入回收站？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteNote({ id: item.id })
  if (currentNote.value && currentNote.value.id === item.id) {
    currentNote.value = null
  }
  ElMessage.success('已移入回收站')
  loadList()
}

async function restoreItem(item) {
  await restoreNote({ id: item.id })
  ElMessage.success('已恢复')
  loadList()
}

async function removeItem(item) {
  try {
    await ElMessageBox.confirm('彻底删除后不可恢复，是否继续？', '警告', { type: 'error' })
  } catch (e) {
    return
  }
  await removeNote({ id: item.id })
  ElMessage.success('已彻底删除')
  loadList()
}

/* ---------- 保存 ---------- */
function onContentChange() {
  if (!currentNote.value) return
  dirty = true
  setSaveState('正在输入…', 'warn')
  clearTimeout(saveTimer)
  saveTimer = setTimeout(saveNow, 1200)
}

function onTitleInput() {
  if (!currentNote.value) return
  dirty = true
  setSaveState('正在输入…', 'warn')
  clearTimeout(saveTimer)
  saveTimer = setTimeout(saveNow, 900)
}

function onTitleChange() {
  if (!currentNote.value) return
  dirty = true
  clearTimeout(saveTimer)
  saveTimer = setTimeout(saveNow, 800)
}

/* 回车 / 失焦立即落库 */
async function commitTitle() {
  if (!currentNote.value) return
  syncHeading(title.value)
  dirty = true
  clearTimeout(saveTimer)
  await saveNow()
  titleInputRef.value?.blur()
}

/**
 * 标题还是自动生成的占位标题时，同步替换正文第一行 H1，
 * 避免出现「标题叫 A、正文第一行还是无标题笔记」的割裂
 */
function syncHeading(newTitle) {
  if (!currentNote.value) return
  const old = currentNote.value.title || '无标题笔记'
  const next = (newTitle || '').trim() || '无标题笔记'
  if (next === old) return
  const content = currentNote.value.content || ''
  const firstLine = content.split('\n')[0] || ''
  if (firstLine.trim() === `# ${old}`.trim()) {
    currentNote.value.content = `# ${next}${content.slice(firstLine.length)}`
    dirty = true
  }
}

/* 侧边栏重命名 */
async function renameNote(item) {
  let value
  try {
    const r = await ElMessageBox.prompt('输入新的标题', '重命名笔记', {
      inputValue: item.title || '无标题笔记',
      inputPlaceholder: '最多 80 个字',
      inputValidator: (v) => {
        const s = (v || '').trim()
        if (!s) return '标题不能为空'
        if (s.length > 80) return '标题不能超过 80 个字'
        return true
      },
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    value = r.value.trim()
  } catch (e) {
    return
  }

  try {
    await updateNote({ id: item.id, title: value })
    item.title = value
    if (currentNote.value && currentNote.value.id === item.id) {
      syncHeading(value)
      currentNote.value.title = value
      title.value = value
    }
    ElMessage.success('标题已更新')
    loadList()
  } catch (e) {
    ElMessage.error('重命名失败')
  }
}

async function saveNow() {
  if (!currentNote.value || !dirty) {
    if (currentNote.value) setSaveState('已保存', 'ok')
    return
  }
  clearTimeout(saveTimer)
  saving.value = true
  setSaveState('保存中…', 'warn')
  try {
    syncHeading(title.value)
    await updateNote({
      id: currentNote.value.id,
      title: title.value || '无标题笔记',
      content: currentNote.value.content || ''
    })
    currentNote.value.title = title.value
    dirty = false
    setSaveState(`已保存 ${new Date().toLocaleTimeString()}`, 'ok')
    loadList()
  } catch (e) {
    setSaveState('保存失败', 'err')
  } finally {
    saving.value = false
  }
}

async function flushSave() {
  if (dirty) {
    await saveNow()
  }
}

/* ---------- 图片上传 ---------- */
async function handleUploadImage(file, done) {
  try {
    const res = await uploadImage(file)
    done(res.data || null)
  } catch (e) {
    done(null)
  }
}

/* ---------- 导出 / 复制 ---------- */
function exportMd() {
  if (!currentNote.value) return ElMessage.warning('请先选择一篇笔记')
  const blob = new Blob([currentNote.value.content || ''], { type: 'text/markdown;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `${(title.value || 'note').replace(/[\\/:*?"<>|]/g, '_')}.md`
  a.click()
  URL.revokeObjectURL(a.href)
}

/* ---------- 复制到公众号：DOM 样式内联化 + 富文本剪贴板 ---------- */
function styleOf(el, cs) {
  const s = []
  const tag = el.tagName.toLowerCase()

  const color = cs.color
  if (color && color !== 'rgba(0, 0, 0, 0)') s.push(`color:${color}`)

  const bg = cs.backgroundColor
  if (bg && bg !== 'rgba(0, 0, 0, 0)' && bg !== 'transparent') s.push(`background-color:${bg}`)

  const fs = cs.fontSize
  if (fs) s.push(`font-size:${fs}`)

  const fw = cs.fontWeight
  if (fw && fw !== '400' && fw !== 'normal') s.push(`font-weight:${Number(fw) >= 600 ? 'bold' : fw}`)

  if (cs.fontStyle && cs.fontStyle !== 'normal') s.push(`font-style:${cs.fontStyle}`)

  const lh = cs.lineHeight
  if (lh && lh !== 'normal') s.push(`line-height:${lh}`)

  if (cs.letterSpacing && cs.letterSpacing !== 'normal') s.push(`letter-spacing:${cs.letterSpacing}`)

  if (['center', 'right', 'justify'].includes(cs.textAlign)) s.push(`text-align:${cs.textAlign}`)

  if (cs.textDecorationLine && cs.textDecorationLine !== 'none' && tag !== 'a') {
    s.push(`text-decoration:${cs.textDecorationLine}`)
  }

  // 外边距
  const mt = cs.marginTop
  const mr = cs.marginRight
  const mb = cs.marginBottom
  const ml = cs.marginLeft
  if (![mt, mr, mb, ml].every((v) => v === '0px')) {
    s.push(`margin:${mt} ${mr} ${mb} ${ml}`)
  }

  // 内边距
  const pt = cs.paddingTop
  const pr = cs.paddingRight
  const pb = cs.paddingBottom
  const pl = cs.paddingLeft
  if (![pt, pr, pb, pl].every((v) => v === '0px')) {
    s.push(`padding:${pt} ${pr} ${pb} ${pl}`)
  }

  // 四边边框
  ;['Top', 'Right', 'Bottom', 'Left'].forEach((side) => {
    const w = cs[`border${side}Width`]
    const st = cs[`border${side}Style`]
    const c = cs[`border${side}Color`]
    if (w && w !== '0px' && st && st !== 'none') {
      s.push(`border-${side.toLowerCase()}:${w} ${st} ${c}`)
    }
  })

  const br = cs.borderTopLeftRadius
  if (br && br !== '0px') s.push(`border-radius:${br}`)

  if (tag === 'ul' || tag === 'ol') {
    s.push('padding-left:26px')
    const lst = cs.listStyleType
    if (lst && lst !== 'none') s.push(`list-style-type:${lst}`)
  }

  if (tag === 'img') {
    s.push('max-width:100%')
    s.push('height:auto')
    s.push('display:block')
    s.push('margin:12px auto')
  }

  if (tag === 'table') {
    s.push('border-collapse:collapse')
    s.push('width:100%')
  }

  if (tag === 'th' || tag === 'td') {
    if (cs.verticalAlign && cs.verticalAlign !== 'baseline') {
      s.push(`vertical-align:${cs.verticalAlign}`)
    }
  }

  if (cs.fontFamily && (tag === 'code' || tag === 'pre')) {
    s.push(`font-family:${cs.fontFamily}`)
  }

  return s.join(';')
}

/**
 * 把预览 DOM 转成公众号能吃的内联样式 HTML
 * @returns {{html: string, mathCount: number}}
 */
function buildWechatHtml() {
  const root = editorRef.value?.getPreviewEl?.()
  if (!root) return { html: '', mathCount: 0 }

  // 放到真实文档里量样式（隐藏但可布局）
  const holder = document.createElement('div')
  holder.setAttribute('aria-hidden', 'true')
  holder.style.cssText =
    'position:fixed;left:-9999px;top:0;width:760px;pointer-events:none;z-index:-1;'
  const clone = root.cloneNode(true)
  holder.appendChild(clone)
  document.body.appendChild(holder)

  let mathCount = 0
  const rootParts = []

  try {
    // 根节点的主题样式（颜色、背景、内边距、字体）要带到公众号外层
    const rootCs = window.getComputedStyle(clone)
    rootParts.push(`color:${rootCs.color}`, `font-size:${rootCs.fontSize}`)
    rootParts.push(`font-family:${rootCs.fontFamily}`)
    if (rootCs.backgroundColor && rootCs.backgroundColor !== 'rgba(0, 0, 0, 0)') {
      rootParts.push(`background-color:${rootCs.backgroundColor}`)
    }
    if (rootCs.lineHeight && rootCs.lineHeight !== 'normal') rootParts.push(`line-height:${rootCs.lineHeight}`)
    if (rootCs.letterSpacing && rootCs.letterSpacing !== 'normal') rootParts.push(`letter-spacing:${rootCs.letterSpacing}`)
    if (rootCs.paddingTop !== '0px' || rootCs.paddingLeft !== '0px') {
      rootParts.push(
        `padding:${rootCs.paddingTop} ${rootCs.paddingRight} ${rootCs.paddingBottom} ${rootCs.paddingLeft}`
      )
    }
    if (rootCs.borderTopLeftRadius && rootCs.borderTopLeftRadius !== '0px') {
      rootParts.push(`border-radius:${rootCs.borderTopLeftRadius}`)
    }

    // 1) 代码块：公众号不支持 pre，转 section，换行用 <br>，保留 token 颜色
    clone.querySelectorAll('pre').forEach((pre) => {
      const sec = document.createElement('section')
      sec.setAttribute(
        'style',
        'margin:16px 0;padding:14px 16px;background-color:#f7f8fa;border:1px solid #eceef2;' +
          'border-radius:8px;font-family:Menlo,Consolas,monospace;font-size:13px;line-height:1.7;' +
          'color:#24292e;word-break:break-all;'
      )
      const code = pre.querySelector('code') || pre

      const toBr = (node) => {
        Array.from(node.childNodes).forEach((child) => {
          if (child.nodeType === 3) {
            if (child.nodeValue.includes('\n')) {
              const parts = child.nodeValue.split('\n')
              const frag = document.createDocumentFragment()
              parts.forEach((p, i) => {
                if (i > 0) frag.appendChild(document.createElement('br'))
                if (p) frag.appendChild(document.createTextNode(p))
              })
              child.replaceWith(frag)
            }
          } else {
            toBr(child)
          }
        })
      }
      toBr(code)

      while (code.firstChild) sec.appendChild(code.firstChild)
      pre.replaceWith(sec)
    })

    // 2) 公式：公众号不支持 KaTeX 排版，降级为 TeX 源码文本
    clone.querySelectorAll('.katex').forEach((el) => {
      mathCount += 1
      const tex = el.querySelector('annotation')?.textContent || ''
      const span = document.createElement('span')
      span.setAttribute(
        'style',
        'color:#3b82f6;font-family:Menlo,Consolas,monospace;font-size:13px;'
      )
      span.textContent = tex ? ` ${tex} ` : el.textContent
      el.replaceWith(span)
    })

    // 3) 先统一采集计算样式（此时 class 还在，后代选择器才生效），再写内联
    const pending = []
    clone.querySelectorAll('*').forEach((el) => {
      const cs = window.getComputedStyle(el)
      pending.push({ el, inline: styleOf(el, cs) })
    })

    pending.forEach(({ el, inline }) => {
      const existing = el.getAttribute('style') || ''
      el.removeAttribute('class')
      el.setAttribute('style', `${existing}${existing && inline ? ';' : ''}${inline}`)
    })
  } finally {
    document.body.removeChild(holder)
  }

  const wrapper = `${rootParts.join(';')};word-break:break-word;`
  return { html: `<section style="${wrapper}">${clone.innerHTML}</section>`, mathCount }
}

/* 富文本写剪贴板：text/html + text/plain 双格式 */
async function writeClipboard(html, plain) {
  if (navigator.clipboard && window.ClipboardItem) {
    try {
      await navigator.clipboard.write([
        new ClipboardItem({
          'text/html': new Blob([html], { type: 'text/html' }),
          'text/plain': new Blob([plain], { type: 'text/plain' })
        })
      ])
      return true
    } catch (e) {
      // 继续走旧方案
    }
  }
  return legacyRichCopy(html)
}

/* 兜底：选中隐藏容器内容，用 execCommand 复制富文本 */
function legacyRichCopy(html) {
  const box = document.createElement('div')
  box.style.cssText = 'position:fixed;left:-9999px;top:0;width:760px;'
  box.innerHTML = html
  document.body.appendChild(box)

  const range = document.createRange()
  range.selectNodeContents(box)
  const sel = window.getSelection()
  sel.removeAllRanges()
  sel.addRange(range)

  let ok = false
  try {
    ok = document.execCommand('copy')
  } catch (e) {
    ok = false
  }

  sel.removeAllRanges()
  document.body.removeChild(box)
  return ok
}

async function copyHtml() {
  if (!currentNote.value) return ElMessage.warning('请先选择一篇笔记')

  const { html, mathCount } = buildWechatHtml()
  if (!html) return ElMessage.error('预览为空，没有可复制的内容')

  const ok = await writeClipboard(html, currentNote.value.content || '')
  if (!ok) {
    return ElMessage.error('复制失败：请允许浏览器访问剪贴板，或手动在预览区全选复制')
  }

  ElMessage.success({
    message: `已复制「${currentThemeName.value}」主题（富文本 + 内联样式），可直接粘贴到公众号编辑器`,
    duration: 3000
  })

  if (mathCount > 0) {
    ElMessage.warning({
      message: `文中 ${mathCount} 处公式在公众号不支持渲染，已降级为 TeX 文本，建议改用截图插入`,
      duration: 4000
    })
  }
}

/* ---------- 历史版本 ---------- */
const historyVisible = ref(false)
const historyList = ref([])
const historyIndex = ref(-1)

async function openHistory() {
  if (!currentNote.value) return ElMessage.warning('请先选择一篇笔记')
  const res = await getHistoryList(currentNote.value.id)
  historyList.value = res.data || []
  historyIndex.value = historyList.value.length ? 0 : -1
  historyVisible.value = true
}

function restoreHistory() {
  const h = historyList.value[historyIndex.value]
  if (!h || !currentNote.value) return
  currentNote.value.content = h.content
  title.value = h.title || title.value
  historyVisible.value = false
  dirty = true
  saveNow()
  ElMessage.success('已恢复，正在保存')
}

/* ---------- 其它 ---------- */
function logout() {
  flushSave().finally(() => {
    userStore.logout()
    router.push('/login')
  })
}

function handleBeforeUnload(e) {
  if (dirty) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onMounted(() => {
  applyTheme()
  loadList().then(() => {
    if (noteList.value.length) onItemClick(noteList.value[0])
  })
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  clearTimeout(saveTimer)
})
</script>

<style scoped>
.layout {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  color: #2b2b2b;
}

.layout.is-dark { background: #16181d; color: #d6d9de; }

.topbar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 14px;
  height: 54px;
  flex: 0 0 54px;
  border-bottom: 1px solid var(--mn-border);
  background: #fff;
}

.is-dark .topbar { background: #1b1f27; border-color: #2c333d; }

.brand { display: flex; align-items: center; gap: 8px; }

.logo {
  width: 28px; height: 28px; border-radius: 8px;
  background: linear-gradient(135deg, #3b82f6, #22c55e);
  color: #fff; font-weight: 700; display: flex; align-items: center; justify-content: center;
}

.brand-name { font-weight: 700; font-size: 15px; letter-spacing: 0.4px; }

.title-box { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 200px; }

.title-input {
  flex: 1; min-width: 140px;
  border: 0; outline: none; background: transparent;
  font-size: 15px; font-weight: 600; color: inherit;
  padding: 6px 8px; border-radius: 6px;
}
.title-input:hover { background: #f7f9fc; }
.is-dark .title-input:hover { background: #232a35; }
.title-input:focus { background: #f5f7fa; box-shadow: 0 0 0 1px #dbe4f3 inset; }
.is-dark .title-input:focus { background: #232a35; box-shadow: 0 0 0 1px #2c333d inset; }

.save-state { font-size: 12px; color: #9aa0a8; white-space: nowrap; }
.save-state.ok { color: #16a34a; }
.save-state.warn { color: #eab308; }
.save-state.err { color: #ef4444; }

.actions { display: flex; align-items: center; gap: 6px; flex: 0 1 auto; min-width: 0; overflow-x: auto; scrollbar-width: none; }
.actions::-webkit-scrollbar { height: 0; }
.actions .el-button { flex: 0 0 auto; }
.switch { margin-right: 4px; }
.theme-pick .el-button { display: inline-flex; align-items: center; gap: 6px; }
.theme-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex: 0 0 auto;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.08) inset;
}
.theme-name { min-width: 52px; display: inline-block; }
.theme-desc { color: #9aa2ad; font-size: 12px; margin-left: 10px; }

.body { display: flex; flex: 1; min-height: 0; }

.sidebar {
  width: 250px;
  flex: 0 0 250px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-right: 1px solid var(--mn-border);
  background: var(--mn-sidebar);
}

.is-dark .sidebar { background: #1a1e25; border-color: #2c333d; }

.sidebar-search { padding: 10px 10px 6px; }

.sidebar-tabs {
  display: flex; gap: 14px; padding: 4px 14px 8px;
  font-size: 12px; color: #8a8f99;
}
.sidebar-tabs span { cursor: pointer; padding-bottom: 4px; border-bottom: 2px solid transparent; }
.sidebar-tabs span.active { color: #3b82f6; border-color: #3b82f6; font-weight: 600; }

.note-list { flex: 1; overflow-y: auto; padding: 0 8px 8px; position: relative; }
.note-list.dragging { outline: 2px dashed #3b82f6; outline-offset: -4px; background: rgba(59, 130, 246, 0.06); }

.drop-hint {
  padding: 10px; margin: 4px 2px 8px;
  border: 1px dashed #3b82f6; border-radius: 8px;
  font-size: 12px; color: #3b82f6; text-align: center;
  background: rgba(59, 130, 246, 0.08);
}

.hidden-file { display: none; }

.note-item {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 10px; border-radius: 8px; cursor: pointer;
  margin-bottom: 4px;
}
.note-item:hover { background: var(--mn-hover); }
.is-dark .note-item:hover { background: #232a35; }
.note-item.active { background: #e8f0ff; }
.is-dark .note-item.active { background: #23304a; }

.note-main { flex: 1; min-width: 0; }
.note-title {
  font-size: 13px; font-weight: 500;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.note-time { font-size: 11px; color: #9aa0a8; margin-top: 2px; }

.note-ops { display: flex; gap: 6px; opacity: 0; transition: 0.15s; }
.note-item:hover .note-ops { opacity: 1; }
.op { font-size: 11px; color: #3b82f6; cursor: pointer; }
.op.danger { color: #ef4444; }

.empty { padding: 30px 12px; text-align: center; font-size: 12px; color: #9aa0a8; }

.sidebar-foot {
  display: flex; justify-content: space-between;
  padding: 8px 12px; font-size: 11px; color: #9aa0a8;
  border-top: 1px solid var(--mn-border);
}
.is-dark .sidebar-foot { border-color: #2c333d; }

.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }

.welcome { flex: 1; display: flex; align-items: center; justify-content: center; }
.welcome-inner { max-width: 420px; text-align: center; }
.welcome-inner h2 { margin: 0 0 10px; font-size: 18px; }
.welcome-inner p { margin: 0 0 18px; font-size: 13px; color: #8a8f99; line-height: 1.8; }

.history { display: flex; gap: 12px; height: 60vh; }
.history-list { width: 240px; overflow-y: auto; border-right: 1px solid #eee; padding-right: 10px; }
.history-item { padding: 8px 10px; border-radius: 6px; cursor: pointer; }
.history-item:hover { background: #f5f7fa; }
.history-item.active { background: #e8f0ff; }
.h-time { font-size: 12px; color: #3b82f6; }
.h-title { font-size: 12px; color: #666; margin-top: 2px; }
.history-preview { flex: 1; overflow: auto; }
.history-preview pre {
  margin: 0; padding: 12px; background: #f7f8fa; border-radius: 8px;
  font-size: 13px; line-height: 1.7; white-space: pre-wrap; word-break: break-word;
}
.history-tip { color: #9aa0a8; font-size: 13px; padding: 20px; }
</style>
