<template>
  <div class="editor">
    <div class="pane pane-input">
      <div class="pane-head">
        <div class="tools">
          <button class="tool" title="上传本地图片" @click="pickImage">🖼 图片</button>
          <button class="tool" title="插入图片链接" @click="insertImageUrl">🔗 图片链接</button>
          <button class="tool" title="加粗" @click="wrapSelection('**')">B</button>
          <button class="tool" title="行内代码" @click="wrapSelection('`')">&lt;/&gt;</button>
        </div>
        <span class="meta">{{ lineCount }} 行 · {{ charCount }} 字</span>
      </div>

      <div
        class="input-wrap"
        :class="{ 'is-drag': draggingImage }"
        @dragover.prevent="onDragOver"
        @dragleave.prevent="onDragLeave"
        @drop.prevent="onDrop"
      >
        <textarea
          ref="textareaRef"
          v-model="text"
          class="input"
          spellcheck="false"
          placeholder="在这里开始写 Markdown……
# 一级标题
## 二级标题
> 引用
**加粗**  *斜体*  `行内代码`
- 列表
$$E=mc^2$$
- 代码块用三个反引号包裹

提示：图片可直接拖进来，或粘贴截图自动上传
"
          @paste="handlePaste"
          @keydown.tab.prevent="handleTab"
        ></textarea>

        <div v-if="draggingImage" class="drop-mask">松开鼠标上传图片</div>
        <div v-if="uploading" class="uploading">
          <span class="spinner"></span> 图片上传中…
        </div>
      </div>

      <input
        ref="imageInputRef"
        type="file"
        accept="image/*"
        multiple
        class="hidden-file"
        @change="onPickImages"
      />
    </div>

    <div class="pane pane-preview">
      <div class="pane-head">
        <span>实时预览</span>
        <span class="meta">公众号排版</span>
      </div>
      <div ref="previewScrollRef" class="preview-scroll">
        <div
          ref="previewRef"
          class="markdown-body mn-theme"
          :class="`mn-t-${theme}`"
          v-html="html"
        ></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import markdownItKatex from 'markdown-it-katex'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import 'katex/dist/katex.min.css'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  modelValue: { type: String, default: '' },
  syncScroll: { type: Boolean, default: true },
  theme: { type: String, default: 'blue' }
})
const emit = defineEmits(['update:modelValue', 'upload-image', 'change'])

const text = ref(props.modelValue || '')
const textareaRef = ref(null)
const previewRef = ref(null)
const previewScrollRef = ref(null)

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value
        }</code></pre>`
      } catch (e) { /* fallthrough */ }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

md.use(markdownItKatex, { throwOnError: false, errorColor: ' #cc0000' })

const html = ref('')

function render() {
  try {
    let out = md.render(text.value || '')
    // 外链新窗口打开
    out = out.replace(/<a href="(http[^"]+)"/g, '<a href="$1" target="_blank" rel="noopener noreferrer"')
    html.value = out
  } catch (e) {
    html.value = `<pre>渲染失败：${e.message}</pre>`
  }
}

render()

const lineCount = computed(() => (text.value ? text.value.split('\n').length : 0))
const charCount = computed(() => text.value.length)

watch(() => props.modelValue, (v) => {
  if (v !== text.value) {
    text.value = v || ''
    render()
  }
})

watch(text, (v) => {
  emit('update:modelValue', v)
  emit('change', v)
  render()
})

/* ---------- 同步滚动 ---------- */
let scrollLock = false

function syncScroll(fromEl, toEl) {
  if (!props.syncScroll || scrollLock || !fromEl || !toEl) return
  const fromMax = fromEl.scrollHeight - fromEl.clientHeight
  const toMax = toEl.scrollHeight - toEl.clientHeight
  if (fromMax <= 0 || toMax <= 0) return
  scrollLock = true
  toEl.scrollTop = (fromEl.scrollTop / fromMax) * toMax
  setTimeout(() => { scrollLock = false }, 60)
}

function onInputScroll() {
  syncScroll(textareaRef.value, previewScrollRef.value)
}
function onPreviewScroll() {
  syncScroll(previewScrollRef.value, textareaRef.value)
}

onMounted(() => {
  textareaRef.value?.addEventListener('scroll', onInputScroll, { passive: true })
  previewScrollRef.value?.addEventListener('scroll', onPreviewScroll, { passive: true })
})

onBeforeUnmount(() => {
  textareaRef.value?.removeEventListener('scroll', onInputScroll)
  previewScrollRef.value?.removeEventListener('scroll', onPreviewScroll)
})

/* ---------- 编辑操作 ---------- */
function insertAtCursor(str) {
  const el = textareaRef.value
  if (!el) return
  const start = el.selectionStart
  const end = el.selectionEnd
  text.value = text.value.slice(0, start) + str + text.value.slice(end)
  nextTick(() => {
    el.selectionStart = el.selectionEnd = start + str.length
    el.focus()
  })
}

function handleTab() {
  insertAtCursor('  ')
}

/* ---------- 图片上传 ---------- */
const imageInputRef = ref(null)
const uploading = ref(false)
const draggingImage = ref(false)
let dragTimer = null

function pickImage() {
  imageInputRef.value?.click()
}

function onPickImages(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  uploadImages(files)
}

function onDragOver(e) {
  if (Array.from(e.dataTransfer?.types || []).includes('Files')) {
    draggingImage.value = true
    clearTimeout(dragTimer)
  }
}

function onDragLeave() {
  dragTimer = setTimeout(() => { draggingImage.value = false }, 120)
}

function onDrop(e) {
  clearTimeout(dragTimer)
  draggingImage.value = false
  uploadImages(Array.from(e.dataTransfer?.files || []))
}

/* 通过父组件上传，返回图片 URL */
function uploadOne(file) {
  return new Promise((resolve) => {
    emit('upload-image', file, (url) => resolve(url || null))
  })
}

async function uploadImages(files) {
  const images = files.filter((f) => f.type.startsWith('image/'))
  if (!images.length) {
    return ElMessage.warning('请选择图片文件（png / jpg / gif / webp 等）')
  }
  if (uploading.value) return

  uploading.value = true
  try {
    for (const file of images) {
      if (file.size > 10 * 1024 * 1024) {
        ElMessage.warning(`${file.name} 超过 10MB，已跳过`)
        continue
      }
      const url = await uploadOne(file)
      if (url) {
        const alt = file.name.replace(/\.[^.]+$/, '') || '图片'
        insertAtCursor(`\n![${alt}](${url})\n`)
      } else {
        ElMessage.error(`${file.name} 上传失败`)
      }
    }
  } finally {
    uploading.value = false
  }
}

async function handlePaste(e) {
  const items = e.clipboardData?.items || []
  for (const item of items) {
    if (item.type && item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (!file) continue
      if (uploading.value) return
      uploading.value = true
      try {
        const url = await uploadOne(file)
        if (url) {
          insertAtCursor(`\n![图片](${url})\n`)
        } else {
          ElMessage.error('图片上传失败')
        }
      } finally {
        uploading.value = false
      }
      return
    }
  }
}

/* ---------- 其它快捷插入 ---------- */
async function insertImageUrl() {
  try {
    const { value } = await ElMessageBox.prompt('输入图片地址（支持任意图床）', '插入图片链接', {
      confirmButtonText: '插入',
      cancelButtonText: '取消',
      inputPlaceholder: 'https://example.com/a.png'
    })
    const url = (value || '').trim()
    if (!url) return
    if (!/^https?:\/\//i.test(url)) {
      return ElMessage.warning('请输入以 http:// 或 https:// 开头的图片地址')
    }
    insertAtCursor(`\n![图片](${url})\n`)
  } catch (e) {
    /* 取消 */
  }
}

function wrapSelection(marker) {
  const el = textareaRef.value
  if (!el) return
  const start = el.selectionStart
  const end = el.selectionEnd
  const selected = text.value.slice(start, end) || '文字'
  text.value = text.value.slice(0, start) + marker + selected + marker + text.value.slice(end)
  nextTick(() => {
    el.focus()
    el.selectionStart = start + marker.length
    el.selectionEnd = start + marker.length + selected.length
  })
}

function getHtml() {
  return previewRef.value ? previewRef.value.innerHTML : ''
}

/* 暴露真实预览节点，供「复制到公众号」做样式内联化 */
function getPreviewEl() {
  return previewRef.value
}

defineExpose({ getHtml, getPreviewEl, insertAtCursor, pickImage })
</script>

<style scoped>
.editor {
  display: flex;
  flex: 1;
  min-height: 0;
  height: 100%;
}

.pane {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
  background: #fff;
}

.pane-input { border-right: 1px solid var(--mn-border); }

.pane-head {
  height: 34px;
  flex: 0 0 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  font-size: 12px;
  color: var(--mn-text-sub);
  background: #fafbfc;
  border-bottom: 1px solid var(--mn-border);
}

.input-wrap {
  position: relative;
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
}

.input-wrap.is-drag { box-shadow: inset 0 0 0 2px #3b82f6; }

.drop-mask {
  position: absolute;
  inset: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed #3b82f6;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.08);
  color: #3b82f6;
  font-size: 14px;
  pointer-events: none;
  z-index: 5;
}

.uploading {
  position: absolute;
  right: 16px;
  bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  background: rgba(59, 130, 246, 0.12);
  color: #3b82f6;
  font-size: 12px;
  pointer-events: none;
}

.spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(59, 130, 246, 0.35);
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.hidden-file { display: none; }

.tools { display: flex; align-items: center; gap: 4px; }

.tool {
  height: 24px;
  padding: 0 8px;
  border: 1px solid var(--mn-border);
  border-radius: 6px;
  background: #fff;
  color: #5c6370;
  font-size: 12px;
  line-height: 22px;
  cursor: pointer;
}

.tool:hover { border-color: #3b82f6; color: #3b82f6; background: #f5f9ff; }

html[data-theme="dark"] .tool { background: #232a35; border-color: #2c333d; color: #a9b1bd; }
html[data-theme="dark"] .tool:hover { color: #6aa9ff; border-color: #6aa9ff; background: #1d2430; }

.input {
  flex: 1;
  width: 100%;
  border: 0;
  outline: none;
  resize: none;
  padding: 20px 22px 60px;
  font-family: Menlo, Consolas, "Courier New", monospace;
  font-size: 14px;
  line-height: 1.75;
  color: #2b2b2b;
  background: #fff;
}

.preview-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px 80px;
  background: #fff;
}

html[data-theme="dark"] .pane,
html[data-theme="dark"] .input,
html[data-theme="dark"] .preview-scroll { background: #16181d; }
html[data-theme="dark"] .pane-head { background: #1b1f27; border-color: #2c333d; }
html[data-theme="dark"] .input { color: #d6d9de; }
html[data-theme="dark"] .pane-input { border-color: #2c333d; }
</style>
