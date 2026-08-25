<script lang="ts" setup>
import type { UploadFile, UploadFiles, UploadRawFile } from 'element-plus';

import type { MediaAsset } from '#/data/cms';

import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue';

import {
  ElButton,
  ElCard,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElImage,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElPagination,
  ElTag,
  ElUpload,
} from 'element-plus';

import {
  deleteMedia,
  getMediaPreviewUrl,
  listMediaPage,
  updateMedia,
  uploadMedia,
} from '#/api';
import { cmsState } from '#/data/cms';

const MAX_FILE_SIZE = 30 * 1024 * 1024;
const PREVIEW_CONCURRENCY = 2;
const ALLOWED_TYPES = new Set([
  'application/pdf',
  'image/gif',
  'image/jpeg',
  'image/png',
  'image/webp',
  'video/mp4',
]);
const keyword = ref('');
const currentPage = ref(1);
const pageSize = 20;
const total = ref(0);
const loading = ref(false);
const uploading = ref(false);
const uploadFileList = ref<UploadFile[]>([]);
const uploadProgress = ref({ completed: 0, failed: 0, total: 0 });
let uploadSchedule: undefined | ReturnType<typeof setTimeout>;
let searchSchedule: undefined | ReturnType<typeof setTimeout>;
let previewObserver: IntersectionObserver | undefined;
const previewQueuedIds = new Set<number>();
const previewQueue: number[] = [];
const previewStatus = reactive<Record<number, 'failed' | 'loading'>>({});
let previewInFlight = 0;
const saving = ref(false);
const deletingId = ref<number>();
const editDialogVisible = ref(false);
const editingAsset = ref<MediaAsset>();
const replacementFile = ref<UploadRawFile>();
const editForm = reactive({
  altText: '',
  originalFilename: '',
});
const filtered = computed(() =>
  cmsState.media.filter((item) =>
    item.name.toLowerCase().includes(keyword.value.trim().toLowerCase()),
  ),
);
const replacementAccept = computed(() => {
  if (editingAsset.value?.type === 'image') {
    return '.jpg,.jpeg,.png,.webp,.gif';
  }
  return editingAsset.value?.type === 'video' ? '.mp4' : '.pdf';
});

function readableSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function assetTypeFromMime(type: string): MediaAsset['type'] {
  if (type.startsWith('image/')) return 'image';
  return type.startsWith('video/') ? 'video' : 'document';
}

function validateFile(file: File) {
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.error('单个文件不能超过 30MB');
    return false;
  }
  if (!ALLOWED_TYPES.has(file.type.toLowerCase())) {
    ElMessage.error('仅支持 JPG、PNG、WebP、GIF、MP4 和 PDF 文件');
    return false;
  }
  return true;
}

function queuePreview(id: number) {
  const asset = cmsState.media.find((item) => item.id === id);
  if (
    !asset ||
    asset.type !== 'image' ||
    asset.url ||
    previewQueuedIds.has(id) ||
    previewStatus[id] === 'failed' ||
    previewStatus[id] === 'loading'
  ) {
    return;
  }
  previewQueuedIds.add(id);
  previewQueue.push(id);
  void processPreviewQueue();
}

async function processPreviewQueue() {
  while (previewInFlight < PREVIEW_CONCURRENCY && previewQueue.length > 0) {
    const id = previewQueue.shift();
    if (id === undefined) continue;
    previewQueuedIds.delete(id);
    const asset = cmsState.media.find((item) => item.id === id);
    if (!asset || asset.type !== 'image' || asset.url) continue;

    previewInFlight += 1;
    previewStatus[id] = 'loading';
    void getMediaPreviewUrl(asset.previewSourceUrl || asset.sourceUrl || asset.url)
      .then((url) => {
        const current = cmsState.media.find((item) => item.id === id);
        if (current) current.url = url;
        delete previewStatus[id];
      })
      .catch((error) => {
        previewStatus[id] = 'failed';
        console.warn(`素材 ${id} 的预览加载失败`, error);
      })
      .finally(() => {
        previewInFlight -= 1;
        void processPreviewQueue();
      });
  }
}

function retryPreview(id: number) {
  delete previewStatus[id];
  queuePreview(id);
}

function observeVisiblePreviews() {
  previewObserver?.disconnect();
  previewObserver = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        const id = Number((entry.target as HTMLElement).dataset.mediaId);
        previewObserver?.unobserve(entry.target);
        if (Number.isSafeInteger(id) && id > 0) queuePreview(id);
      }
    },
    { rootMargin: '240px 0px', threshold: 0.01 },
  );
  document
    .querySelectorAll<HTMLElement>('.media-item[data-media-id]')
    .forEach((element) => previewObserver?.observe(element));
}

async function load() {
  loading.value = true;
  try {
    const result = await listMediaPage(keyword.value, {
      page: currentPage.value,
      size: pageSize,
    });
    total.value = result.total;
    const mapped = result.records.map((item) => ({
        alt: item.altText || '',
        createdAt: item.createdAt,
        id: item.id,
        name: item.originalFilename,
        previewSourceUrl: item.thumbnailUrl || item.adminUrl,
        size: readableSize(item.fileSize),
        sourceUrl: item.adminUrl,
        type: item.mediaType.toLowerCase() as 'document' | 'image' | 'video',
        // Request the protected binary only when this card approaches the
        // viewport, rather than downloading the entire library at once.
        url: item.mediaType === 'IMAGE' ? '' : item.adminUrl,
      }));
    previewQueue.splice(0);
    previewQueuedIds.clear();
    Object.keys(previewStatus).forEach((id) => delete previewStatus[Number(id)]);
    cmsState.media.splice(0, cmsState.media.length, ...mapped);
    await nextTick();
    observeVisiblePreviews();
  } finally {
    loading.value = false;
  }
}

async function processUploadQueue(uploadFiles: UploadFiles) {
  const files = uploadFiles
    .map((item) => item.raw)
    .filter((item): item is UploadRawFile => Boolean(item))
    .filter(validateFile);
  uploadFileList.value = [];
  if (files.length === 0) return;

  uploading.value = true;
  uploadProgress.value = { completed: 0, failed: 0, total: files.length };
  try {
    for (const file of files) {
      try {
        await uploadMedia(file, file.name.replace(/\.[^.]+$/, ''));
        uploadProgress.value.completed += 1;
      } catch (error) {
        console.error(`素材 ${file.name} 上传失败`, error);
        uploadProgress.value.failed += 1;
      }
    }
    await load();
    const { completed, failed, total } = uploadProgress.value;
    if (failed === 0) {
      ElMessage.success(`已上传 ${completed} 个素材`);
    } else {
      ElMessage.warning(
        `已上传 ${completed}/${total} 个素材，${failed} 个上传失败，请重试失败文件`,
      );
    }
  } finally {
    uploading.value = false;
  }
}

function addFile(_uploadFile: UploadFile, uploadFiles: UploadFiles) {
  // Element Plus calls `on-change` once per selected file. Wait until the
  // browser finishes adding this selection, then upload the whole batch once.
  if (uploading.value) return;
  if (uploadSchedule) clearTimeout(uploadSchedule);
  uploadSchedule = setTimeout(() => {
    uploadSchedule = undefined;
    if (!uploading.value && uploadFiles.length > 0) {
      void processUploadQueue(uploadFiles);
    }
  }, 0);
}

function openEdit(item: MediaAsset) {
  editingAsset.value = item;
  editForm.originalFilename = item.name;
  editForm.altText = item.alt;
  replacementFile.value = undefined;
  editDialogVisible.value = true;
}

function selectReplacement(_uploadFile: UploadFile, uploadFiles: UploadFiles) {
  const latest = uploadFiles.at(-1)?.raw as undefined | UploadRawFile;
  if (!latest || !validateFile(latest)) {
    replacementFile.value = undefined;
    return;
  }
  if (assetTypeFromMime(latest.type) !== editingAsset.value?.type) {
    ElMessage.error('替换文件必须与原素材保持相同类型');
    replacementFile.value = undefined;
    return;
  }
  replacementFile.value = latest;
}

async function saveEdit() {
  const item = editingAsset.value;
  const originalFilename = editForm.originalFilename.trim();
  if (!item || !originalFilename) {
    ElMessage.warning('请填写素材名称');
    return;
  }
  saving.value = true;
  try {
    await updateMedia(item.id, {
      altText: editForm.altText.trim(),
      file: replacementFile.value,
      originalFilename,
    });
    editDialogVisible.value = false;
    await load();
    ElMessage.success(
      replacementFile.value ? '素材文件及信息已更新' : '素材信息已更新',
    );
  } finally {
    saving.value = false;
  }
}

async function remove(item: MediaAsset) {
  try {
    await ElMessageBox.confirm(
      `删除“${item.name}”后无法恢复。正在被页面使用的素材会被后端拦截。`,
      '确认删除素材',
      {
        cancelButtonText: '取消',
        confirmButtonText: '删除',
        confirmButtonClass: 'el-button--danger',
        type: 'warning',
      },
    );
  } catch {
    return;
  }
  deletingId.value = item.id;
  try {
    await deleteMedia(item.id);
    await load();
    ElMessage.success('素材已删除');
  } finally {
    deletingId.value = undefined;
  }
}

onMounted(load);
watch(keyword, () => {
  if (searchSchedule) clearTimeout(searchSchedule);
  searchSchedule = setTimeout(() => {
    currentPage.value = 1;
    void load();
  }, 300);
});
watch(filtered, () => {
  void nextTick(observeVisiblePreviews);
});
onBeforeUnmount(() => {
  if (uploadSchedule) clearTimeout(uploadSchedule);
  if (searchSchedule) clearTimeout(searchSchedule);
  previewObserver?.disconnect();
});
</script>

<template>
  <div class="media-page">
    <section class="page-header">
      <div>
        <p>ASSET LIBRARY</p>
        <h1>素材库</h1>
        <span>统一管理官网图片、产品手册和视频资料</span>
      </div>
      <ElUpload
        :auto-upload="false"
        v-model:file-list="uploadFileList"
        multiple
        :on-change="addFile"
        :show-file-list="false"
        accept=".jpg,.jpeg,.png,.webp,.gif,.mp4,.pdf"
      >
        <ElButton :loading="uploading" size="large" type="primary">
          {{
            uploading
              ? `正在上传 ${uploadProgress.completed + uploadProgress.failed}/${uploadProgress.total}`
              : '上传素材'
          }}
        </ElButton>
      </ElUpload>
    </section>
    <ElCard class="media-card" shadow="never" v-loading="loading">
      <div class="toolbar">
        <ElInput v-model="keyword" clearable placeholder="搜索素材名称" />
        <span>共 {{ total }} 个文件</span>
      </div>
      <div v-if="filtered.length > 0" class="media-grid">
        <article
          v-for="item in filtered"
          :key="item.id"
          :data-media-id="item.id"
          class="media-item"
        >
          <ElImage
            v-if="item.type === 'image' && item.url"
            :preview-src-list="[item.url]"
            :src="item.url"
            fit="cover"
          />
          <div
            v-else-if="item.type === 'image' && previewStatus[item.id] === 'failed'"
            class="image-preview-failed"
          >
            <span>预览加载失败</span>
            <ElButton link type="primary" @click="retryPreview(item.id)">
              重试
            </ElButton>
          </div>
          <div v-else-if="item.type === 'image'" class="image-preview-loading">
            图片加载中…
          </div>
          <div v-else class="file-preview">
            <b>{{ item.type === 'video' ? 'VIDEO' : 'DOC' }}</b>
            <span>{{ item.name.split('.').pop()?.toUpperCase() }}</span>
          </div>
          <div class="media-info">
            <strong :title="item.name">{{ item.name }}</strong>
            <span>{{ item.size }} · {{ item.createdAt }}</span>
            <ElInput
              v-model="item.alt"
              disabled
              placeholder="上传时保存的素材说明"
            />
          </div>
          <div class="media-actions">
            <ElTag effect="plain">
              {{
                item.type === 'image'
                  ? '图片'
                  : item.type === 'video'
                    ? '视频'
                    : '文档'
              }}
            </ElTag>
            <div class="action-buttons">
              <ElButton link type="primary" @click="openEdit(item)">
                编辑
              </ElButton>
              <ElButton
                :loading="deletingId === item.id"
                link
                type="danger"
                @click="remove(item)"
              >
                删除
              </ElButton>
            </div>
          </div>
        </article>
      </div>
      <ElEmpty v-else description="没有匹配的素材" />
      <ElPagination
        v-if="total > pageSize"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        background
        layout="prev, pager, next"
        @current-change="load"
      />
    </ElCard>

    <ElDialog
      v-model="editDialogVisible"
      :close-on-click-modal="false"
      destroy-on-close
      title="编辑素材"
      width="560px"
    >
      <ElForm :model="editForm" label-position="top">
        <ElFormItem label="素材名称" required>
          <ElInput
            v-model="editForm.originalFilename"
            maxlength="255"
            show-word-limit
          />
        </ElFormItem>
        <ElFormItem label="素材说明">
          <ElInput
            v-model="editForm.altText"
            :rows="3"
            maxlength="255"
            placeholder="用于图片替代文本和后台检索"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="替换文件">
          <ElUpload
            :accept="replacementAccept"
            :auto-upload="false"
            :on-change="selectReplacement"
            :show-file-list="false"
          >
            <ElButton>选择替换文件</ElButton>
          </ElUpload>
          <div v-if="replacementFile" class="replacement-file">
            已选择：{{ replacementFile.name }}
          </div>
          <p class="form-tip">
            不选择文件时仅修改名称和说明；替换后素材 ID
            不变，已使用该素材的页面会自动显示新文件。
          </p>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="editDialogVisible = false">取消</ElButton>
        <ElButton :loading="saving" type="primary" @click="saveEdit">
          保存修改
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.media-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7f8;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 28px 30px;
  color: #fff;
  background: linear-gradient(125deg, #102a35, #125b61);
  border-radius: 18px;
}

.page-header p {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  color: #78d2c8;
  letter-spacing: 0.18em;
}

.page-header h1 {
  margin: 5px 0;
  font-size: 28px;
}

.page-header span {
  color: rgb(255 255 255 / 68%);
}

.page-header :deep(.el-button--primary) {
  --el-button-bg-color: #e85d45;
  --el-button-border-color: #e85d45;
}

.media-card {
  margin-top: 16px;
  border-radius: 16px;
}

.toolbar {
  display: flex;
  gap: 20px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.toolbar :deep(.el-input) {
  max-width: 420px;
}

.toolbar span {
  color: #849195;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.media-item {
  position: relative;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5eaeb;
  border-radius: 14px;
}

.media-item :deep(.el-image),
.file-preview,
.image-preview-failed,
.image-preview-loading {
  width: 100%;
  height: 165px;
}

.file-preview {
  display: grid;
  place-content: center;
  color: #0f6267;
  text-align: center;
  background: linear-gradient(145deg, #e3f0ef, #f1f5f5);
}

.image-preview-loading {
  display: grid;
  place-content: center;
  color: #a0adb1;
  font-size: 13px;
  background: #f4f6f7;
}

.image-preview-failed {
  display: grid;
  place-content: center;
  gap: 4px;
  color: #a0adb1;
  font-size: 13px;
  background: #f8f6f6;
}

.file-preview b {
  font-size: 28px;
  letter-spacing: 0.08em;
}

.file-preview span {
  margin-top: 4px;
  color: #708287;
}

.media-info {
  padding: 14px;
}

.media-info strong,
.media-info span {
  display: block;
}

.media-info strong {
  overflow: hidden;
  color: #203a40;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-info span {
  margin: 5px 0 12px;
  font-size: 12px;
  color: #89969a;
}

.media-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px 14px;
}

.action-buttons {
  display: flex;
  align-items: center;
}

.replacement-file {
  width: 100%;
  margin-top: 10px;
  font-size: 13px;
  color: #2c6d70;
}

.form-tip {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #849195;
}

@media (max-width: 1100px) {
  .media-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 760px) {
  .media-page {
    padding: 14px;
  }

  .page-header {
    flex-direction: column;
    gap: 18px;
    align-items: flex-start;
  }

  .media-grid {
    grid-template-columns: 1fr 1fr;
  }

  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar :deep(.el-input) {
    max-width: none;
  }
}

@media (max-width: 480px) {
  .media-grid {
    grid-template-columns: 1fr;
  }
}
</style>
