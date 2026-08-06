<script lang="ts" setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { ElMessageBox } from 'element-plus';

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    mediaImagePicker?: boolean;
    minHeight?: number;
    modelValue?: string;
    placeholder?: string;
    showImageButton?: boolean;
  }>(),
  {
    disabled: false,
    minHeight: 280,
    mediaImagePicker: false,
    modelValue: '',
    placeholder: '请输入正文内容',
    showImageButton: true,
  },
);

const emit = defineEmits<{
  requestImage: [];
  'update:modelValue': [value: string];
}>();

const editorRef = ref<HTMLElement>();
const sourceMode = ref(false);
const sourceValue = ref('');
let savedRange: Range | undefined;

const FONT_FAMILIES = {
  mono: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
  sans: 'Inter, "Noto Sans SC", "Microsoft YaHei", sans-serif',
  serif: 'Georgia, "Noto Serif SC", "Songti SC", serif',
} as const;

const FONT_SIZE_COMMANDS = {
  large: '4',
  small: '2',
  xlarge: '5',
};
const FONT_SIZES = new Set(['large', 'small', 'xlarge']);
const FONT_COLORS = new Map([
  ['#1b7f5f', 'green'],
  ['#6b7280', 'gray'],
  ['#2563eb', 'blue'],
  ['#202124', 'black'],
  ['#c62828', 'red'],
  ['#e67e22', 'orange'],
]);
const HIGHLIGHT_COLORS = new Map([
  ['#cfe8ff', 'blue'],
  ['#d8f3dc', 'green'],
  ['#fff0a6', 'yellow'],
]);
const ALIGNMENTS = new Set(['center', 'justify', 'left', 'right']);
const ALIGNABLE_TAGS = new Set([
  'BLOCKQUOTE',
  'DIV',
  'H1',
  'H2',
  'H3',
  'H4',
  'H5',
  'H6',
  'LI',
  'P',
]);

const allowedPasteTags = new Set([
  'A',
  'B',
  'BLOCKQUOTE',
  'BR',
  'CAPTION',
  'CITE',
  'CODE',
  'COL',
  'COLGROUP',
  'DD',
  'DIV',
  'DL',
  'DT',
  'EM',
  'FIGCAPTION',
  'FIGURE',
  'H1',
  'H2',
  'H3',
  'H4',
  'H5',
  'H6',
  'HR',
  'I',
  'IMG',
  'LI',
  'OL',
  'P',
  'PRE',
  'Q',
  'S',
  'SMALL',
  'SOURCE',
  'SPAN',
  'STRIKE',
  'STRONG',
  'SUB',
  'SUP',
  'TABLE',
  'TBODY',
  'TD',
  'TFOOT',
  'TH',
  'THEAD',
  'TR',
  'U',
  'UL',
  'VIDEO',
]);

function isEditorSelection(range: Range) {
  const editor = editorRef.value;
  return Boolean(editor && editor.contains(range.commonAncestorContainer));
}

function rememberSelection() {
  const selection = window.getSelection();
  if (!selection?.rangeCount) return;
  const range = selection.getRangeAt(0);
  if (isEditorSelection(range)) savedRange = range.cloneRange();
}

function restoreSelection() {
  if (!savedRange || !editorRef.value) return;
  const selection = window.getSelection();
  selection?.removeAllRanges();
  selection?.addRange(savedRange);
}

function normalizeHtml(editor: HTMLElement) {
  const hasMedia = Boolean(editor.querySelector('img, video, figure'));
  return editor.textContent?.trim() || hasMedia ? editor.innerHTML : '';
}

function emitEditorHtml() {
  const editor = editorRef.value;
  if (!editor) return;
  const html = normalizeHtml(editor);
  if (html !== props.modelValue) emit('update:modelValue', html);
  rememberSelection();
}

function normalizeColor(value: string) {
  const color = value.trim().toLowerCase().replaceAll(/\s/g, '');
  const rgb = color.match(/^rgb\((\d+),(\d+),(\d+)\)$/);
  if (!rgb) return color;
  return `#${[rgb[1], rgb[2], rgb[3]]
    .map((part) => Number(part).toString(16).padStart(2, '0'))
    .join('')}`;
}

function fontToken(value: null | string) {
  const face = value?.toLowerCase() || '';
  if (face.includes('mono') || face.includes('consolas')) return 'mono';
  if (
    face.includes('georgia') ||
    face.includes('serif') ||
    face.includes('songti')
  ) {
    return 'serif';
  }
  return 'sans';
}

function normalizeEditorMarkup() {
  const editor = editorRef.value;
  if (!editor) return;

  for (const font of editor.querySelectorAll('font')) {
    const span = document.createElement('span');
    const face = font.getAttribute('face');
    const size = font.getAttribute('size');
    const color = font.getAttribute('color');
    if (face) span.dataset.font = fontToken(face);
    if (size === '2') span.dataset.size = 'small';
    if (size === '4') span.dataset.size = 'large';
    if (size === '5') span.dataset.size = 'xlarge';
    if (color) {
      const token = FONT_COLORS.get(normalizeColor(color));
      if (token) span.dataset.color = token;
    }
    span.replaceChildren(...font.childNodes);
    font.replaceWith(span);
  }

  for (const element of editor.querySelectorAll<HTMLElement>(
    '[style], [align]',
  )) {
    const align =
      element.style.textAlign || element.getAttribute('align') || '';
    const highlight = element.style.backgroundColor;
    if (ALIGNABLE_TAGS.has(element.tagName) && ALIGNMENTS.has(align)) {
      element.dataset.align = align;
    }
    if (element.tagName === 'SPAN' && highlight) {
      const token = HIGHLIGHT_COLORS.get(normalizeColor(highlight));
      if (token) element.dataset.highlight = token;
    }
    element.removeAttribute('align');
    element.removeAttribute('style');
  }

  for (const span of editor.querySelectorAll('span')) {
    if (span.attributes.length === 0) span.replaceWith(...span.childNodes);
  }
}

function runCommand(command: string, value?: string) {
  if (props.disabled || sourceMode.value) return;
  editorRef.value?.focus();
  restoreSelection();
  document.execCommand(command, false, value);
  normalizeEditorMarkup();
  emitEditorHtml();
}

function applyBlock(event: Event) {
  const value = (event.target as HTMLSelectElement).value;
  if (value) runCommand('formatBlock', value);
  (event.target as HTMLSelectElement).value = '';
}

function applyFont(event: Event) {
  const value = (event.target as HTMLSelectElement)
    .value as keyof typeof FONT_FAMILIES;
  if (value) runCommand('fontName', FONT_FAMILIES[value]);
  (event.target as HTMLSelectElement).value = '';
}

function applyFontSize(event: Event) {
  const value = (event.target as HTMLSelectElement).value;
  const commandValue =
    FONT_SIZE_COMMANDS[value as keyof typeof FONT_SIZE_COMMANDS];
  if (commandValue) runCommand('fontSize', commandValue);
  (event.target as HTMLSelectElement).value = '';
}

function applyColor(event: Event, command: 'backColor' | 'foreColor') {
  const value = (event.target as HTMLSelectElement).value;
  if (value) runCommand(command, value);
  (event.target as HTMLSelectElement).value = '';
}

function applyAlignment(event: Event) {
  const value = (event.target as HTMLSelectElement).value;
  if (value) runCommand(value);
  (event.target as HTMLSelectElement).value = '';
}

function safeHref(value: string) {
  const href = value.trim();
  if (/^(?:https?:|mailto:|\/|#)/i.test(href)) return href;
  return `https://${href}`;
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function isSafeUrl(value: string, allowMediaReference = false) {
  const protocols = allowMediaReference
    ? /^(?:alfy-media:|https?:|\/)/i
    : /^(?:https?:|mailto:|\/|#)/i;
  return protocols.test(value.trim());
}

function isAllowedInlineDataAttribute(name: string, value: string) {
  if (name === 'data-font') return value in FONT_FAMILIES;
  if (name === 'data-size') return FONT_SIZES.has(value);
  if (name === 'data-color') return [...FONT_COLORS.values()].includes(value);
  if (name === 'data-highlight') {
    return [...HIGHLIGHT_COLORS.values()].includes(value);
  }
  return false;
}

async function insertLink() {
  if (props.disabled || sourceMode.value) return;
  rememberSelection();
  let input = '';
  try {
    const result = await ElMessageBox.prompt('请输入链接地址', '插入链接', {
      cancelButtonText: '取消',
      confirmButtonText: '插入',
      inputPlaceholder: 'https://example.com',
    });
    input = result.value;
  } catch {
    return;
  }
  if (!input?.trim()) return;

  restoreSelection();
  const href = safeHref(input);
  const selection = window.getSelection();
  if (selection?.toString()) {
    runCommand('createLink', href);
  } else {
    runCommand(
      'insertHTML',
      `<a href="${escapeHtml(href)}">${escapeHtml(input.trim())}</a>`,
    );
  }
}

async function insertImageFromUrl() {
  let input = '';
  try {
    const result = await ElMessageBox.prompt('请输入图片地址', '插入图片', {
      cancelButtonText: '取消',
      confirmButtonText: '插入',
      inputPlaceholder: 'https://example.com/image.jpg',
    });
    input = result.value;
  } catch {
    return;
  }

  const url = input.trim();
  if (!url) return;
  if (!isSafeUrl(url, true)) {
    return;
  }
  insertHtml(
    `<figure><img src="${escapeHtml(url)}" alt="" loading="lazy"></figure>`,
  );
}

function requestImage() {
  if (props.disabled || sourceMode.value) return;
  rememberSelection();
  if (props.mediaImagePicker) {
    emit('requestImage');
    return;
  }
  void insertImageFromUrl();
}

function cleanPastedHtml(html: string) {
  const template = document.createElement('template');
  template.innerHTML = html;

  for (const element of template.content.querySelectorAll('*')) {
    if (!allowedPasteTags.has(element.tagName)) {
      if (
        ['EMBED', 'IFRAME', 'OBJECT', 'SCRIPT', 'STYLE'].includes(
          element.tagName,
        )
      ) {
        element.remove();
      } else {
        element.replaceWith(...element.childNodes);
      }
      continue;
    }

    const safeAttributes = new Map<string, string>();
    const attributes = [...element.attributes];
    for (const attribute of attributes) {
      const { name, value } = attribute;
      switch (element.tagName) {
        case 'A': {
          if (name === 'href' && isSafeUrl(value)) {
            safeAttributes.set('href', value.trim());
          }
          break;
        }
        case 'BLOCKQUOTE':
        case 'DIV':
        case 'H1':
        case 'H2':
        case 'H3':
        case 'H4':
        case 'H5':
        case 'H6':
        case 'LI':
        case 'P': {
          if (name === 'data-align' && ALIGNMENTS.has(value)) {
            safeAttributes.set(name, value);
          }
          break;
        }
        case 'IMG': {
          if (name === 'src' && isSafeUrl(value, true)) {
            safeAttributes.set('src', value.trim());
          } else if (['alt', 'title'].includes(name)) {
            safeAttributes.set(name, value);
          } else if (name === 'loading' && ['eager', 'lazy'].includes(value)) {
            safeAttributes.set('loading', value);
          }
          break;
        }
        case 'SOURCE': {
          if (name === 'src' && isSafeUrl(value, true)) {
            safeAttributes.set('src', value.trim());
          } else if (name === 'type') {
            safeAttributes.set('type', value);
          }
          break;
        }
        case 'SPAN': {
          if (isAllowedInlineDataAttribute(name, value)) {
            safeAttributes.set(name, value);
          }
          break;
        }
        case 'VIDEO': {
          if (name === 'poster' && isSafeUrl(value, true)) {
            safeAttributes.set('poster', value.trim());
          } else if (name === 'controls') {
            safeAttributes.set('controls', '');
          } else if (
            name === 'preload' &&
            ['auto', 'metadata', 'none'].includes(value)
          ) {
            safeAttributes.set('preload', value);
          }
          break;
        }
      }
      element.removeAttribute(attribute.name);
    }
    for (const [name, value] of safeAttributes) {
      element.setAttribute(name, value);
    }
  }

  return template.innerHTML;
}

function handlePaste(event: ClipboardEvent) {
  if (props.disabled || sourceMode.value) return;
  event.preventDefault();
  const clipboard = event.clipboardData;
  const pastedHtml = clipboard?.getData('text/html');
  const pastedText = clipboard?.getData('text/plain') || '';
  const content = pastedHtml
    ? cleanPastedHtml(pastedHtml)
    : escapeHtml(pastedText).replaceAll(/\r?\n/g, '<br>');
  runCommand('insertHTML', content);
}

function insertHtml(html: string) {
  if (props.disabled || sourceMode.value) return;
  editorRef.value?.focus();
  restoreSelection();
  document.execCommand('insertHTML', false, cleanPastedHtml(html));
  normalizeEditorMarkup();
  emitEditorHtml();
}

function toggleSourceMode() {
  if (props.disabled) return;
  if (sourceMode.value) {
    sourceMode.value = false;
    void nextTick(() => {
      if (!editorRef.value) return;
      const safeHtml = cleanPastedHtml(sourceValue.value);
      sourceValue.value = safeHtml;
      editorRef.value.innerHTML = safeHtml;
      emitEditorHtml();
      editorRef.value.focus();
    });
    return;
  }

  sourceValue.value = editorRef.value?.innerHTML || props.modelValue || '';
  sourceMode.value = true;
}

function updateSource(value: string) {
  sourceValue.value = value;
  emit('update:modelValue', value);
}

watch(
  () => props.modelValue,
  (value = '') => {
    const nextValue = value;
    if (sourceMode.value) {
      if (sourceValue.value !== nextValue) sourceValue.value = nextValue;
      return;
    }
    if (editorRef.value && editorRef.value.innerHTML !== nextValue) {
      editorRef.value.innerHTML = nextValue;
    }
  },
);

onMounted(() => {
  if (editorRef.value) editorRef.value.innerHTML = props.modelValue || '';
  document.addEventListener('selectionchange', rememberSelection);
});

onBeforeUnmount(() => {
  document.removeEventListener('selectionchange', rememberSelection);
});

defineExpose({ insertHtml });
</script>

<template>
  <div
    :class="{ 'is-disabled': disabled, 'is-source-mode': sourceMode }"
    class="rich-text-editor"
  >
    <div aria-label="正文格式工具栏" class="rich-text-toolbar" role="toolbar">
      <button
        :disabled="disabled || sourceMode"
        aria-label="撤销"
        title="撤销"
        type="button"
        @click="runCommand('undo')"
        @mousedown.prevent
      >
        ↶
      </button>
      <button
        :disabled="disabled || sourceMode"
        aria-label="重做"
        title="重做"
        type="button"
        @click="runCommand('redo')"
        @mousedown.prevent
      >
        ↷
      </button>
      <span class="toolbar-divider"></span>
      <select
        :disabled="disabled || sourceMode"
        aria-label="字体"
        title="字体"
        @change="applyFont"
        @mousedown="rememberSelection"
      >
        <option value="">默认字体</option>
        <option value="sans">无衬线字体</option>
        <option value="serif">衬线字体</option>
        <option value="mono">等宽字体</option>
      </select>
      <select
        :disabled="disabled || sourceMode"
        aria-label="字号"
        title="字号"
        @change="applyFontSize"
        @mousedown="rememberSelection"
      >
        <option value="">字号</option>
        <option value="small">小</option>
        <option value="large">大</option>
        <option value="xlarge">特大</option>
      </select>
      <select
        :disabled="disabled || sourceMode"
        aria-label="段落格式"
        title="段落格式"
        @change="applyBlock"
        @mousedown="rememberSelection"
      >
        <option value="">段落</option>
        <option value="p">正文</option>
        <option value="h1">一级标题</option>
        <option value="h2">二级标题</option>
        <option value="h3">三级标题</option>
        <option value="blockquote">引用</option>
      </select>
      <span class="toolbar-divider"></span>
      <button
        :disabled="disabled || sourceMode"
        aria-label="加粗"
        title="加粗"
        type="button"
        @click="runCommand('bold')"
        @mousedown.prevent
      >
        <strong>B</strong>
      </button>
      <button
        :disabled="disabled || sourceMode"
        aria-label="斜体"
        title="斜体"
        type="button"
        @click="runCommand('italic')"
        @mousedown.prevent
      >
        <em>I</em>
      </button>
      <button
        :disabled="disabled || sourceMode"
        aria-label="下划线"
        title="下划线"
        type="button"
        @click="runCommand('underline')"
        @mousedown.prevent
      >
        <u>U</u>
      </button>
      <button
        :disabled="disabled || sourceMode"
        aria-label="删除线"
        title="删除线"
        type="button"
        @click="runCommand('strikeThrough')"
        @mousedown.prevent
      >
        <s>S</s>
      </button>
      <select
        :disabled="disabled || sourceMode"
        aria-label="文字颜色"
        class="color-select color-select-text"
        title="文字颜色"
        @change="applyColor($event, 'foreColor')"
        @mousedown="rememberSelection"
      >
        <option value="">文字颜色</option>
        <option value="#202124">黑色</option>
        <option value="#c62828">红色</option>
        <option value="#e67e22">橙色</option>
        <option value="#1b7f5f">绿色</option>
        <option value="#2563eb">蓝色</option>
        <option value="#6b7280">灰色</option>
      </select>
      <select
        :disabled="disabled || sourceMode"
        aria-label="文字背景色"
        class="color-select color-select-highlight"
        title="文字背景色"
        @change="applyColor($event, 'backColor')"
        @mousedown="rememberSelection"
      >
        <option value="">背景色</option>
        <option value="#fff0a6">黄色</option>
        <option value="#cfe8ff">蓝色</option>
        <option value="#d8f3dc">绿色</option>
      </select>
      <span class="toolbar-divider"></span>
      <button
        :disabled="disabled || sourceMode"
        aria-label="无序列表"
        title="无序列表"
        type="button"
        @click="runCommand('insertUnorderedList')"
        @mousedown.prevent
      >
        ☷
      </button>
      <button
        :disabled="disabled || sourceMode"
        aria-label="有序列表"
        title="有序列表"
        type="button"
        @click="runCommand('insertOrderedList')"
        @mousedown.prevent
      >
        1≡
      </button>
      <button
        :disabled="disabled || sourceMode"
        aria-label="减少缩进"
        title="减少缩进"
        type="button"
        @click="runCommand('outdent')"
        @mousedown.prevent
      >
        ≪
      </button>
      <button
        :disabled="disabled || sourceMode"
        aria-label="增加缩进"
        title="增加缩进"
        type="button"
        @click="runCommand('indent')"
        @mousedown.prevent
      >
        ≫
      </button>
      <select
        :disabled="disabled || sourceMode"
        aria-label="对齐方式"
        title="对齐方式"
        @change="applyAlignment"
        @mousedown="rememberSelection"
      >
        <option value="">对齐</option>
        <option value="justifyLeft">左对齐</option>
        <option value="justifyCenter">居中</option>
        <option value="justifyRight">右对齐</option>
        <option value="justifyFull">两端对齐</option>
      </select>
      <span class="toolbar-divider"></span>
      <button
        :disabled="disabled || sourceMode"
        aria-label="引用"
        title="引用"
        type="button"
        @click="runCommand('formatBlock', 'blockquote')"
        @mousedown.prevent
      >
        “ ”
      </button>
      <button
        :disabled="disabled || sourceMode"
        aria-label="分隔线"
        title="分隔线"
        type="button"
        @click="runCommand('insertHorizontalRule')"
        @mousedown.prevent
      >
        ━
      </button>
      <button
        :disabled="disabled || sourceMode"
        aria-label="插入链接"
        title="插入链接"
        type="button"
        @click="insertLink"
        @mousedown.prevent
      >
        🔗
      </button>
      <button
        v-if="showImageButton"
        :disabled="disabled || sourceMode"
        aria-label="插入图片"
        title="插入图片"
        type="button"
        @click="requestImage"
        @mousedown.prevent
      >
        🖼
      </button>
      <button
        :disabled="disabled || sourceMode"
        aria-label="清除格式"
        title="清除格式"
        type="button"
        @click="runCommand('removeFormat')"
        @mousedown.prevent
      >
        ⌫
      </button>
      <span class="toolbar-spacer"></span>
      <button
        :class="{ active: sourceMode }"
        :disabled="disabled"
        title="切换 HTML 源码"
        type="button"
        @click="toggleSourceMode"
      >
        &lt;/&gt;
      </button>
    </div>

    <textarea
      v-if="sourceMode"
      :disabled="disabled"
      :placeholder="placeholder"
      :style="{ minHeight: `${minHeight}px` }"
      :value="sourceValue"
      class="rich-text-source"
      @input="updateSource(($event.target as HTMLTextAreaElement).value)"
    ></textarea>
    <div
      v-else
      ref="editorRef"
      :aria-disabled="disabled"
      :contenteditable="!disabled"
      :data-placeholder="placeholder"
      :style="{ minHeight: `${minHeight}px` }"
      aria-multiline="true"
      class="rich-text-content"
      role="textbox"
      @blur="emitEditorHtml"
      @input="emitEditorHtml"
      @keyup="rememberSelection"
      @paste="handlePaste"
    ></div>
  </div>
</template>

<style scoped>
.rich-text-editor {
  width: 100%;
  overflow: hidden;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  transition: border-color var(--el-transition-duration);
}

.rich-text-editor:focus-within {
  border-color: var(--el-color-primary);
}

.rich-text-editor.is-disabled {
  color: var(--el-disabled-text-color);
  background: var(--el-disabled-bg-color);
}

.rich-text-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
  padding: 10px 12px;
  background: #f4f5f7;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.rich-text-toolbar button,
.rich-text-toolbar select {
  height: 32px;
  font: inherit;
  font-size: 14px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 5px;
}

.rich-text-toolbar button {
  min-width: 32px;
  padding: 0 8px;
  font-size: 16px;
}

.rich-text-toolbar select {
  max-width: 124px;
  padding: 0 22px 0 8px;
  font-size: 13px;
}

.rich-text-toolbar .color-select {
  max-width: 88px;
}

.rich-text-toolbar .color-select-text {
  border-bottom: 2px solid #c62828;
}

.rich-text-toolbar .color-select-highlight {
  border-bottom: 2px solid #f2cd37;
}

.rich-text-toolbar button:hover:not(:disabled),
.rich-text-toolbar button.active,
.rich-text-toolbar select:hover:not(:disabled) {
  color: var(--el-color-primary);
  background: var(--el-bg-color);
  border-color: var(--el-border-color);
}

.rich-text-toolbar button:disabled,
.rich-text-toolbar select:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  margin: 0 5px;
  background: var(--el-border-color);
}

.toolbar-spacer {
  flex: 1;
}

.rich-text-content,
.rich-text-source {
  box-sizing: border-box;
  width: 100%;
  padding: 18px 20px;
  font:
    16px/1.85 Inter,
    'Noto Sans SC',
    'Source Han Sans SC',
    'PingFang SC',
    'Microsoft YaHei UI',
    'Microsoft YaHei',
    sans-serif;
  color: #3e4750;
  resize: vertical;
  background: transparent;
  border: 0;
  outline: none;
}

.rich-text-source {
  display: block;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  line-height: 1.6;
}

.rich-text-content:empty::before {
  color: var(--el-text-color-placeholder);
  pointer-events: none;
  content: attr(data-placeholder);
}

.rich-text-content :deep(h1),
.rich-text-content :deep(h2),
.rich-text-content :deep(h3) {
  line-height: 1.35;
  color: #152f36;
  letter-spacing: -0.035em;
}

.rich-text-content :deep(h1) {
  margin: 44px 0 20px;
  font-size: 36px;
}

.rich-text-content :deep(h2) {
  margin: 42px 0 18px;
  font-size: 28px;
}

.rich-text-content :deep(h3) {
  margin: 34px 0 14px;
  font-size: 22px;
}

.rich-text-content :deep(> :first-child) {
  margin-top: 0;
}

.rich-text-content :deep(> :last-child) {
  margin-bottom: 0;
}

.rich-text-content :deep(p) {
  margin: 0 0 18px;
  color: #3e4750;
}

.rich-text-content :deep(blockquote) {
  padding: 4px 0 4px 18px;
  margin: 24px 0;
  color: #52636b;
  border-left: 3px solid #0e7478;
}

.rich-text-content :deep(ul),
.rich-text-content :deep(ol) {
  padding-left: 2em;
  margin: 18px 0;
}

.rich-text-content :deep(li) {
  margin: 6px 0;
}

.rich-text-content :deep(a) {
  color: #e51b23;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.rich-text-content :deep(hr) {
  margin: 32px 0;
  border: 0;
  border-top: 1px solid #dfe3e6;
}

.rich-text-content :deep(pre) {
  padding: 18px 20px;
  margin: 24px 0;
  overflow-x: auto;
  background: #e9edef;
}

.rich-text-content :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.rich-text-content :deep(figure) {
  margin: 32px 0;
}

.rich-text-content :deep(img),
.rich-text-content :deep(video) {
  display: block;
  width: 100%;
  max-width: 100%;
  height: auto;
}

.rich-text-content :deep(figcaption) {
  margin-top: 10px;
  font-size: 0.82rem;
  line-height: 1.6;
  color: #78868b;
  text-align: center;
}

.rich-text-content :deep([data-font='sans']) {
  font-family: Inter, 'Noto Sans SC', 'Microsoft YaHei', sans-serif;
}

.rich-text-content :deep([data-font='serif']) {
  font-family: Georgia, 'Noto Serif SC', 'Songti SC', serif;
}

.rich-text-content :deep([data-font='mono']) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.rich-text-content :deep([data-size='small']) {
  font-size: 0.875em;
}

.rich-text-content :deep([data-size='large']) {
  font-size: 1.25em;
}

.rich-text-content :deep([data-size='xlarge']) {
  font-size: 1.5em;
}

.rich-text-content :deep([data-color='black']) {
  color: #202124;
}

.rich-text-content :deep([data-color='red']) {
  color: #c62828;
}

.rich-text-content :deep([data-color='orange']) {
  color: #e67e22;
}

.rich-text-content :deep([data-color='green']) {
  color: #1b7f5f;
}

.rich-text-content :deep([data-color='blue']) {
  color: #2563eb;
}

.rich-text-content :deep([data-color='gray']) {
  color: #6b7280;
}

.rich-text-content :deep([data-highlight='yellow']) {
  background: #fff0a6;
}

.rich-text-content :deep([data-highlight='blue']) {
  background: #cfe8ff;
}

.rich-text-content :deep([data-highlight='green']) {
  background: #d8f3dc;
}

.rich-text-content :deep([data-align='center']) {
  text-align: center;
}

.rich-text-content :deep([data-align='right']) {
  text-align: right;
}

.rich-text-content :deep([data-align='justify']) {
  text-align: justify;
}

.rich-text-content :deep(table) {
  width: 100%;
  margin: 24px 0;
  border-collapse: collapse;
}

.rich-text-content :deep(td),
.rich-text-content :deep(th) {
  padding: 12px 14px;
  text-align: left;
  border: 1px solid #dfe3e6;
}

@media (max-width: 640px) {
  .toolbar-divider {
    display: none;
  }

  .toolbar-spacer {
    flex-basis: 100%;
    height: 0;
  }

  .rich-text-content,
  .rich-text-source {
    padding: 14px;
  }
}
</style>
