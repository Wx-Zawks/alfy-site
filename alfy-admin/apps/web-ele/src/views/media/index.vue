<script lang="ts" setup>
import type { UploadFile, UploadFiles, UploadRawFile } from 'element-plus';

import { computed, onMounted, ref } from 'vue';

import {
  ElButton,
  ElCard,
  ElEmpty,
  ElImage,
  ElInput,
  ElMessage,
  ElTag,
  ElUpload,
} from 'element-plus';

import { getMediaPreviewUrl, listMedia, uploadMedia } from '#/api';
import { cmsState } from '#/data/cms';

const MAX_FILE_SIZE = 30 * 1024 * 1024;
const ALLOWED_TYPES = new Set([
  'application/pdf',
  'image/gif',
  'image/jpeg',
  'image/png',
  'image/webp',
  'video/mp4',
]);
const keyword = ref('');
const loading = ref(false);
const uploading = ref(false);
const filtered = computed(() =>
  cmsState.media.filter((item) =>
    item.name.toLowerCase().includes(keyword.value.trim().toLowerCase()),
  ),
);

function readableSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

async function load() {
  loading.value = true;
  try {
    const values = await listMedia(keyword.value);
    const mapped = await Promise.all(
      values.map(async (item) => ({
        alt: item.altText || '',
        createdAt: item.createdAt,
        id: item.id,
        name: item.originalFilename,
        size: readableSize(item.fileSize),
        sourceUrl: item.adminUrl,
        type: item.mediaType.toLowerCase() as 'document' | 'image' | 'video',
        url:
          item.mediaType === 'IMAGE'
            ? await getMediaPreviewUrl(item.adminUrl).catch((error) => {
                console.warn(`素材 ${item.id} 的预览加载失败`, error);
                return '';
              })
            : item.adminUrl,
      })),
    );
    cmsState.media
      .filter((item) => item.url.startsWith('blob:'))
      .forEach((item) => URL.revokeObjectURL(item.url));
    cmsState.media.splice(0, cmsState.media.length, ...mapped);
  } finally {
    loading.value = false;
  }
}

async function addFile(_uploadFile: UploadFile, uploadFiles: UploadFiles) {
  const latest = uploadFiles.at(-1)?.raw as undefined | UploadRawFile;
  if (!latest) return;
  if (latest.size > MAX_FILE_SIZE) {
    ElMessage.error('单个文件不能超过 30MB');
    return;
  }
  if (!ALLOWED_TYPES.has(latest.type.toLowerCase())) {
    ElMessage.error('仅支持 JPG、PNG、WebP、GIF、MP4 和 PDF 文件');
    return;
  }
  uploading.value = true;
  try {
    await uploadMedia(latest, latest.name.replace(/\.[^.]+$/, ''));
    await load();
    ElMessage.success('素材已上传到后端');
  } finally {
    uploading.value = false;
  }
}

onMounted(load);
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
        :on-change="addFile"
        :show-file-list="false"
        accept=".jpg,.jpeg,.png,.webp,.gif,.mp4,.pdf"
      >
        <ElButton :loading="uploading" size="large" type="primary">
          上传素材
        </ElButton>
      </ElUpload>
    </section>
    <ElCard class="media-card" shadow="never" v-loading="loading">
      <div class="toolbar">
        <ElInput v-model="keyword" clearable placeholder="搜索素材名称" /><span>共 {{ filtered.length }} 个文件</span>
      </div>
      <div v-if="filtered.length > 0" class="media-grid">
        <article v-for="item in filtered" :key="item.id" class="media-item">
          <ElImage
            v-if="item.type === 'image'"
            :preview-src-list="[item.url]"
            :src="item.url"
            fit="cover"
          />
          <div v-else class="file-preview">
            <b>{{ item.type === 'video' ? 'VIDEO' : 'DOC' }}</b><span>{{ item.name.split('.').pop()?.toUpperCase() }}</span>
          </div>
          <div class="media-info">
            <strong :title="item.name">{{ item.name }}</strong><span>{{ item.size }} · {{ item.createdAt }}</span><ElInput
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
</ElTag><span class="api-note">后端暂不支持删除</span>
          </div>
        </article>
      </div>
      <ElEmpty v-else description="没有匹配的素材" />
    </ElCard>
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
.file-preview {
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
