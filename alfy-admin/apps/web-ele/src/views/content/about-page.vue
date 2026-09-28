<script lang="ts" setup>
import type { UploadFile, UploadRawFile } from 'element-plus';
import type { ContentPageRecord, MediaRecord } from '#/api';

import { computed, reactive, ref } from 'vue';

import {
  ElButton,
  ElCard,
  ElEmpty,
  ElImage,
  ElInput,
  ElMessage,
  ElOption,
  ElSelect,
  ElTag,
  ElUpload,
} from 'element-plus';

import {
  listContentPages,
  listMedia,
  saveContentPage,
  uploadMedia,
} from '#/api';

interface HistoryItemForm {
  date: string;
  imageMediaId?: number;
  imageUrl?: string;
  text: string;
  title: string;
}

interface AboutContentData {
  historyItems?: unknown;
  [key: string]: unknown;
}

interface ImageOption {
  id: number;
  name: string;
}

const MAX_IMAGE_SIZE = 30 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set([
  'image/gif',
  'image/jpeg',
  'image/png',
  'image/webp',
]);

const loading = ref(false);
const saving = ref(false);
const uploadingIndex = ref<number>();
const page = ref<ContentPageRecord>();
const originalData = ref<AboutContentData>({});
const mediaOptions = ref<ImageOption[]>([]);
const mediaLoaded = ref(false);
const form = reactive<{ historyItems: HistoryItemForm[] }>({
  historyItems: [],
});

const hasPage = computed(() => Boolean(page.value));

function emptyHistoryItem(): HistoryItemForm {
  return { date: '', imageMediaId: undefined, imageUrl: '', text: '', title: '' };
}

function normalizeHistoryItem(value: unknown): HistoryItemForm {
  const item = (value || {}) as Record<string, unknown>;
  const imageMediaId = Number(item.imageMediaId);
  return {
    date: String(item.date || ''),
    imageMediaId:
      Number.isSafeInteger(imageMediaId) && imageMediaId > 0
        ? imageMediaId
        : undefined,
    imageUrl: String(item.imageUrl || ''),
    text: String(item.text || ''),
    title: String(item.title || ''),
  };
}

function contentDataFromPage(value?: ContentPageRecord): AboutContentData {
  const data = value?.contentData;
  return data && typeof data === 'object' && !Array.isArray(data)
    ? (data as AboutContentData)
    : {};
}

function publicMediaUrl(mediaId?: number) {
  return mediaId ? `/api/v1/public/media/${mediaId}` : '';
}

function imagePreview(item: HistoryItemForm) {
  return publicMediaUrl(item.imageMediaId) || item.imageUrl || '';
}

function imageAlt(item: HistoryItemForm, index: number) {
  return item.title || item.date || `发展历程图片 ${index + 1}`;
}

function selectedImageName(mediaId?: number) {
  return mediaOptions.value.find((item) => item.id === mediaId)?.name || '';
}

async function ensureMediaOptions() {
  if (mediaLoaded.value) return;
  const records = await listMedia('', { page: 1, size: 100 });
  mediaOptions.value = records
    .filter((item: MediaRecord) => item.mediaType === 'IMAGE')
    .map((item) => ({ id: item.id, name: item.originalFilename }));
  mediaLoaded.value = true;
}

async function load() {
  loading.value = true;
  try {
    const pages = await listContentPages();
    const about = pages.find((item) => item.pageKey === 'about');
    if (!about) {
      page.value = undefined;
      form.historyItems = [];
      return;
    }
    page.value = about;
    originalData.value = contentDataFromPage(about);
    const historyItems = originalData.value.historyItems;
    form.historyItems = Array.isArray(historyItems)
      ? historyItems.map(normalizeHistoryItem)
      : [];
    await ensureMediaOptions();
  } finally {
    loading.value = false;
  }
}

function addHistoryItem() {
  form.historyItems.push(emptyHistoryItem());
}

function removeHistoryItem(index: number) {
  form.historyItems.splice(index, 1);
}

function moveHistoryItem(index: number, direction: -1 | 1) {
  const target = index + direction;
  if (target < 0 || target >= form.historyItems.length) return;
  const [item] = form.historyItems.splice(index, 1);
  form.historyItems.splice(target, 0, item!);
}

function selectImage(item: HistoryItemForm, mediaId?: number) {
  item.imageMediaId = mediaId || undefined;
  if (mediaId) item.imageUrl = publicMediaUrl(mediaId);
}

function validateImage(raw?: UploadRawFile) {
  if (!raw) return false;
  if (!ALLOWED_IMAGE_TYPES.has(raw.type)) {
    ElMessage.warning('请上传 JPG、PNG、WebP 或 GIF 图片');
    return false;
  }
  if (raw.size > MAX_IMAGE_SIZE) {
    ElMessage.warning('单张图片不能超过 30 MB');
    return false;
  }
  return true;
}

async function uploadHistoryImage(
  item: HistoryItemForm,
  index: number,
  uploadFile: UploadFile,
) {
  const raw = uploadFile.raw;
  if (!validateImage(raw)) return;
  uploadingIndex.value = index;
  try {
    const media = await uploadMedia(raw!, `${item.date || '发展历程'}图片`);
    item.imageMediaId = media.id;
    item.imageUrl = publicMediaUrl(media.id);
    if (!mediaOptions.value.some((option) => option.id === media.id)) {
      mediaOptions.value.unshift({ id: media.id, name: media.originalFilename });
    }
    ElMessage.success('图片已上传并选中');
  } finally {
    uploadingIndex.value = undefined;
  }
}

async function save() {
  if (!page.value) return;
  const invalidIndex = form.historyItems.findIndex(
    (item) => !item.date.trim() || !imagePreview(item),
  );
  if (invalidIndex !== -1) {
    ElMessage.warning(`请为第 ${invalidIndex + 1} 条填写年份/日期并选择图片`);
    return;
  }

  saving.value = true;
  try {
    await saveContentPage(page.value.id, {
      category: page.value.category || null,
      contentData: {
        ...originalData.value,
        historyItems: form.historyItems.map((item) => ({
          date: item.date.trim(),
          imageMediaId: item.imageMediaId || null,
          imageUrl: item.imageMediaId ? null : item.imageUrl || null,
          text: item.text.trim() || null,
          title: item.title.trim() || null,
        })),
      },
      contentHtml: page.value.contentHtml || null,
      coverMediaId: page.value.coverMediaId || null,
      featured: Boolean(page.value.featured),
      pageKey: page.value.pageKey,
      seoDescription: page.value.seoDescription || null,
      seoKeywords: page.value.seoKeywords || null,
      seoTitle: page.value.seoTitle || null,
      sortOrder: Number(page.value.sortOrder || 0),
      summary: page.value.summary || null,
      title: page.value.title,
      version: page.value.version,
    });
    ElMessage.success('关于我们－发展历程已保存');
    await load();
  } finally {
    saving.value = false;
  }
}

void load();
</script>

<template>
  <div class="about-page-admin">
    <section class="about-page-hero">
      <div>
        <p>ABOUT CONTENT</p>
        <h1>关于我们</h1>
        <span>维护官网“发展历程”的日期、文字、图片与展示顺序。保存后官网会自动使用最新内容。</span>
      </div>
      <ElTag type="success">{{ form.historyItems.length }} 条历程</ElTag>
    </section>

    <ElCard v-loading="loading" class="history-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h2>发展历程</h2>
            <p>图片可从素材库选择或直接上传；拖动前请使用上下按钮调整官网展示顺序。</p>
          </div>
          <ElButton :disabled="!hasPage" plain type="primary" @click="addHistoryItem">
            + 新增历程
          </ElButton>
        </div>
      </template>

      <ElEmpty v-if="!hasPage" description="未找到 about 单页记录，请先完成数据库初始化" />
      <div v-else class="history-list">
        <article v-for="(item, index) in form.historyItems" :key="index" class="history-item-editor">
          <div class="history-item-index">{{ String(index + 1).padStart(2, '0') }}</div>
          <div class="history-item-fields">
            <div class="form-grid form-grid--top">
              <ElInput v-model="item.date" maxlength="30" placeholder="年份或日期，例如 2026年" />
              <ElInput v-model="item.title" maxlength="80" placeholder="标题（可留空）" />
            </div>
            <ElInput v-model="item.text" :rows="2" maxlength="300" placeholder="说明（可留空）" type="textarea" />
            <div class="image-control">
              <ElSelect
                :model-value="item.imageMediaId"
                clearable
                filterable
                placeholder="从素材库选择图片"
                @update:model-value="(value) => selectImage(item, Number(value) || undefined)"
              >
                <ElOption v-for="option in mediaOptions" :key="option.id" :label="option.name" :value="option.id" />
              </ElSelect>
              <ElUpload
                :auto-upload="false"
                :disabled="uploadingIndex === index"
                :show-file-list="false"
                accept="image/jpeg,image/png,image/webp,image/gif"
                @change="(file) => uploadHistoryImage(item, index, file)"
              >
                <ElButton :loading="uploadingIndex === index" plain>上传新图片</ElButton>
              </ElUpload>
              <small v-if="item.imageMediaId">当前素材：{{ selectedImageName(item.imageMediaId) || `#${item.imageMediaId}` }}</small>
            </div>
          </div>
          <ElImage v-if="imagePreview(item)" :alt="imageAlt(item, index)" :preview-src-list="[imagePreview(item)]" :src="imagePreview(item)" class="history-image-preview" fit="cover" />
          <div class="history-item-actions">
            <ElButton :disabled="index === 0" circle plain @click="moveHistoryItem(index, -1)">↑</ElButton>
            <ElButton :disabled="index === form.historyItems.length - 1" circle plain @click="moveHistoryItem(index, 1)">↓</ElButton>
            <ElButton circle plain type="danger" @click="removeHistoryItem(index)">×</ElButton>
          </div>
        </article>
      </div>

      <div v-if="hasPage" class="save-bar">
        <span>官网当前状态：{{ page?.status === 'PUBLISHED' ? '已发布' : '未发布' }}</span>
        <ElButton :loading="saving" type="primary" @click="save">保存发展历程</ElButton>
      </div>
    </ElCard>
  </div>
</template>

<style scoped>
.about-page-admin { min-height: 100%; padding: 24px; background: #f5f7f8; }
.about-page-hero { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 30px; color: #fff; background: radial-gradient(circle at 82% 20%, rgb(109 182 137 / 32%), transparent 26%), linear-gradient(120deg, #123d36, #1e715f); border-radius: 18px; box-shadow: 0 18px 50px rgb(18 61 54 / 16%); }
.about-page-hero p { margin: 0; font-size: 11px; font-weight: 700; color: #a7ebc1; letter-spacing: .18em; }
.about-page-hero h1 { margin: 5px 0 8px; font-size: 28px; }
.about-page-hero span { display: block; max-width: 700px; line-height: 1.7; color: rgb(255 255 255 / 74%); }
.history-card { margin-top: 24px; border: 1px solid #e2e8ea; border-radius: 16px; }
.card-header, .save-bar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.card-header h2 { margin: 0; color: #17343b; }
.card-header p { margin: 6px 0 0; font-size: 13px; color: #7e8a8f; }
.history-list { display: grid; gap: 14px; }
.history-item-editor { display: grid; grid-template-columns: 42px minmax(0, 1fr) 190px auto; gap: 14px; align-items: start; padding: 16px; background: #f6f8f8; border: 1px solid #e6eeee; border-radius: 12px; }
.history-item-index { display: grid; place-items: center; width: 38px; height: 38px; color: #fff; font-size: 12px; font-weight: 800; background: #1e715f; border-radius: 10px; }
.history-item-fields { display: grid; gap: 10px; }
.form-grid { display: grid; grid-template-columns: minmax(150px, .5fr) minmax(220px, 1fr); gap: 10px; }
.image-control { display: grid; grid-template-columns: minmax(180px, 1fr) auto; gap: 10px; align-items: center; }
.image-control small { grid-column: 1 / -1; color: #849195; }
.history-image-preview { width: 190px; height: 128px; border-radius: 8px; background: #e8eeee; }
.history-item-actions { display: grid; gap: 8px; }
.save-bar { padding-top: 20px; margin-top: 20px; border-top: 1px solid #e7eeee; color: #718187; }
@media (max-width: 900px) { .history-item-editor { grid-template-columns: 42px minmax(0, 1fr) auto; } .history-image-preview { grid-column: 2; } }
@media (max-width: 640px) { .about-page-admin { padding: 14px; } .about-page-hero, .card-header, .save-bar { align-items: flex-start; flex-direction: column; } .history-item-editor, .form-grid, .image-control { grid-template-columns: 1fr; } .history-item-index, .history-image-preview { grid-column: auto; } .history-image-preview { width: 100%; height: 180px; } .history-item-actions { display: flex; } }
</style>
