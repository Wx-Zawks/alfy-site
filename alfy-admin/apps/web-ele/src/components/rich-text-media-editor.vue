<script lang="ts" setup>
import type { UploadFile, UploadFiles, UploadRawFile } from 'element-plus';

import { computed, ref } from 'vue';

import {
  ElButton,
  ElDialog,
  ElEmpty,
  ElFormItem,
  ElInput,
  ElMessage,
  ElUpload,
} from 'element-plus';

import { getMediaPreviewUrl, listMedia, uploadMedia } from '#/api';
import RichTextEditor from '#/components/rich-text-editor.vue';

interface InlineImageOption {
  alt: string;
  id: number;
  name: string;
  previewUrl: string;
  sourceUrl: string;
}

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    minHeight?: number;
    modelValue?: string;
    placeholder?: string;
    showTip?: boolean;
  }>(),
  {
    disabled: false,
    minHeight: 280,
    modelValue: '',
    placeholder:
      '请输入正文；可设置字体、字号、颜色、对齐方式，并从工具栏插入图片',
    showTip: true,
  },
);

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const editorRef = ref<{ insertHtml: (html: string) => void }>();
const dialogVisible = ref(false);
const loading = ref(false);
const loaded = ref(false);
const uploading = ref(false);
const uploadQueue = ref<UploadRawFile[]>([]);
const imageOptions = ref<InlineImageOption[]>([]);
const selectedMediaId = ref<number>();
const imageAlt = ref('');
const imageCaption = ref('');
let uploadSchedule: ReturnType<typeof setTimeout> | undefined;

const MAX_INLINE_IMAGE_SIZE = 30 * 1024 * 1024;
const ALLOWED_INLINE_IMAGE_TYPES = new Set([
  'image/gif',
  'image/jpeg',
  'image/png',
  'image/webp',
]);

const selectedMedia = computed(() =>
  imageOptions.value.find((asset) => asset.id === selectedMediaId.value),
);

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function revokePreviewUrls(options: InlineImageOption[]) {
  for (const asset of options) {
    if (asset.previewUrl.startsWith('blob:')) {
      URL.revokeObjectURL(asset.previewUrl);
    }
  }
}

async function loadImageOptions() {
  loading.value = true;
  try {
    const mediaRecords = await listMedia('', { page: 1, size: 20 });
    const nextOptions = mediaRecords
      .filter((item) => item.mediaType === 'IMAGE')
      .map((item) => ({
        alt: item.altText || '',
        id: item.id,
        name: item.originalFilename,
        previewUrl: '',
        sourceUrl: item.thumbnailUrl || item.adminUrl,
      }));
    revokePreviewUrls(imageOptions.value);
    imageOptions.value = nextOptions;
    loaded.value = true;
    void loadImagePreviews();
  } catch (error) {
    // listMedia 和素材预览接口已经展示后端返回的具体错误。
    console.warn('素材库图片加载失败', error);
  } finally {
    loading.value = false;
  }
}

async function loadImagePreviews() {
  const waiting = imageOptions.value.filter((asset) => !asset.previewUrl);
  let nextIndex = 0;
  const loadOne = async () => {
    while (nextIndex < waiting.length) {
      const asset = waiting[nextIndex++];
      if (!asset) continue;
      try {
        asset.previewUrl = await getMediaPreviewUrl(asset.sourceUrl);
      } catch (error) {
        console.warn(`素材 ${asset.id} 的预览加载失败`, error);
      }
    }
  };
  // 限制并行预览请求，避免原图较多时拖慢后台的其他接口。
  await Promise.all(Array.from({ length: 2 }, loadOne));
}

function selectImage(id: number) {
  const asset = imageOptions.value.find((item) => item.id === id);
  selectedMediaId.value = id;
  imageAlt.value =
    asset?.alt || asset?.name.replace(/\.[^.]+$/, '') || '正文图片';
  imageCaption.value = '';
}

function openImagePicker() {
  selectedMediaId.value = undefined;
  imageAlt.value = '';
  imageCaption.value = '';
  dialogVisible.value = true;
  if (!loaded.value && !loading.value) void loadImageOptions();
}

function insertImage() {
  const asset = selectedMedia.value;
  if (!asset) {
    ElMessage.warning('请先选择一张图片');
    return;
  }

  const caption = imageCaption.value.trim();
  const figure = [
    '<figure>',
    `  <img src="alfy-media:${asset.id}" alt="${escapeHtml(imageAlt.value.trim() || asset.name)}" loading="lazy">`,
    caption ? `  <figcaption>${escapeHtml(caption)}</figcaption>` : '',
    '</figure>',
  ]
    .filter(Boolean)
    .join('\n');

  if (!editorRef.value) {
    ElMessage.error('正文编辑器尚未就绪，请稍后重试');
    return;
  }

  editorRef.value.insertHtml(figure);
  dialogVisible.value = false;
  ElMessage.success('图片已插入正文，保存后即可发布');
}

async function uploadInlineImage(
  uploadFile: UploadFile,
  _uploadFiles: UploadFiles,
) {
  const rawFile = uploadFile.raw;
  if (!rawFile || uploadQueue.value.some((file) => file.uid === rawFile.uid))
    return;
  uploadQueue.value.push(rawFile);
  if (uploadSchedule) clearTimeout(uploadSchedule);
  uploadSchedule = setTimeout(() => {
    uploadSchedule = undefined;
    void processUploadQueue();
  }, 0);
}

function validateImage(file: UploadRawFile) {
  if (file.size > MAX_INLINE_IMAGE_SIZE) {
    ElMessage.error('单张图片不能超过 30MB');
    return false;
  }
  if (!ALLOWED_INLINE_IMAGE_TYPES.has(file.type.toLowerCase())) {
    ElMessage.error('仅支持 JPG、PNG、WebP 和 GIF 图片');
    return false;
  }
  return true;
}

async function processUploadQueue() {
  const files = uploadQueue.value.splice(0).filter(validateImage);
  if (files.length === 0) return;
  uploading.value = true;
  try {
    let completed = 0;
    for (const file of files) {
      try {
        const saved = await uploadMedia(
          file,
          file.name.replace(/\.[^.]+$/, ''),
        );
        const asset: InlineImageOption = {
          alt: saved.altText || file.name.replace(/\.[^.]+$/, ''),
          id: saved.id,
          name: saved.originalFilename,
          previewUrl: await getMediaPreviewUrl(
            saved.thumbnailUrl || saved.adminUrl,
          ),
          sourceUrl: saved.adminUrl,
        };
        const existingIndex = imageOptions.value.findIndex(
          (item) => item.id === asset.id,
        );
        if (existingIndex !== -1) {
          const existing = imageOptions.value[existingIndex];
          if (existing?.previewUrl.startsWith('blob:')) {
            URL.revokeObjectURL(existing.previewUrl);
          }
          imageOptions.value.splice(existingIndex, 1);
        }
        imageOptions.value.unshift(asset);
        selectImage(asset.id);
        completed += 1;
      } catch (error) {
        console.warn(`图片 ${file.name} 上传失败`, error);
      }
    }
    if (completed > 0) {
      ElMessage.success(
        files.length === 1
          ? '图片已上传，请确认说明后插入正文'
          : `已上传 ${completed}/${files.length} 张图片，请选择后插入正文`,
      );
    }
  } finally {
    uploading.value = false;
    if (uploadQueue.value.length > 0) void processUploadQueue();
  }
}
</script>

<template>
  <div class="rich-text-media-editor">
    <RichTextEditor
      ref="editorRef"
      :disabled="props.disabled"
      :media-image-picker="true"
      :min-height="props.minHeight"
      :model-value="props.modelValue"
      :placeholder="props.placeholder"
      @request-image="openImagePicker"
      @update:model-value="emit('update:modelValue', $event)"
    />
    <p v-if="props.showTip" class="content-editor-tip">
      图片会插入当前光标位置；可从素材库选择或直接上传，保存后自动关联素材。
    </p>

    <ElDialog
      v-model="dialogVisible"
      append-to-body
      :close-on-click-modal="false"
      title="插入正文图片"
      width="860px"
    >
      <div class="inline-image-toolbar">
        <p>选择素材后可修改图片说明，图片将插入正文当前光标位置。</p>
        <ElUpload
          :auto-upload="false"
          :disabled="uploading"
          :on-change="uploadInlineImage"
          :show-file-list="false"
          accept=".jpg,.jpeg,.png,.webp,.gif"
          multiple
        >
          <ElButton :loading="uploading" type="primary">上传新图片</ElButton>
        </ElUpload>
      </div>

      <div v-if="loading" class="inline-image-loading">正在加载素材库…</div>
      <div v-else-if="imageOptions.length > 0" class="inline-image-grid">
        <button
          v-for="asset in imageOptions"
          :key="asset.id"
          :class="{ selected: selectedMediaId === asset.id }"
          type="button"
          @click="selectImage(asset.id)"
        >
          <img
            v-if="asset.previewUrl"
            :alt="asset.alt || asset.name"
            :src="asset.previewUrl"
          />
          <span v-else class="inline-image-placeholder">图片预览不可用</span>
          <strong :title="asset.name">{{ asset.name }}</strong>
        </button>
      </div>
      <ElEmpty v-else description="素材库暂无图片，可先上传一张图片" />

      <div v-if="selectedMedia" class="inline-image-fields">
        <ElFormItem label="图片替代文字">
          <ElInput
            v-model="imageAlt"
            maxlength="160"
            placeholder="用于无障碍访问和图片加载失败时显示"
            show-word-limit
          />
        </ElFormItem>
        <ElFormItem label="图片说明（可选）">
          <ElInput
            v-model="imageCaption"
            maxlength="200"
            placeholder="例如：项目现场或技术示意图"
            show-word-limit
          />
        </ElFormItem>
      </div>

      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton
          :disabled="!selectedMedia"
          type="primary"
          @click="insertImage"
        >
          插入当前位置
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.rich-text-media-editor {
  width: 100%;
}

.content-editor-tip {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #7c8b90;
}

.inline-image-toolbar {
  display: flex;
  gap: 20px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.inline-image-toolbar p {
  margin: 0;
  color: #65767c;
}

.inline-image-loading {
  display: grid;
  place-items: center;
  min-height: 220px;
  color: #7c8b90;
  background: #f5f8f8;
  border-radius: 10px;
}

.inline-image-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  max-height: 420px;
  padding: 2px;
  overflow-y: auto;
}

.inline-image-grid button {
  padding: 0;
  overflow: hidden;
  color: #263e44;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 2px solid transparent;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e0e7e9;
}

.inline-image-grid button.selected {
  border-color: #0e7478;
  box-shadow: 0 0 0 3px rgb(14 116 120 / 14%);
}

.inline-image-grid img,
.inline-image-placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 120px;
  font-size: 12px;
  color: #879499;
  object-fit: cover;
  background: #edf2f3;
}

.inline-image-grid strong {
  display: block;
  padding: 10px;
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inline-image-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  padding: 16px;
  margin-top: 20px;
  background: #f5f8f8;
  border-radius: 10px;
}

.inline-image-fields :deep(.el-form-item) {
  margin-bottom: 0;
}

@media (max-width: 760px) {
  .inline-image-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .inline-image-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .inline-image-fields {
    grid-template-columns: 1fr;
  }
}
</style>
