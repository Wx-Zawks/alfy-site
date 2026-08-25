<script lang="ts" setup>
import type {
  MediaRecord,
  TechnologyBlockRecord,
  TechnologyPageRecord,
} from '#/api';

import { computed, reactive, ref } from 'vue';

import {
  ElButton,
  ElCard,
  ElCol,
  ElDialog,
  ElDivider,
  ElForm,
  ElFormItem,
  ElImage,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElRow,
  ElSelect,
  ElTag,
} from 'element-plus';

import {
  changeTechnologyPageStatus,
  getMediaPreviewUrl,
  getTechnologyPage,
  listMedia,
  listTechnologyPages,
  saveTechnologyPage,
} from '#/api';
import RichTextMediaEditor from '#/components/rich-text-media-editor.vue';

interface PageDefinition {
  description: string;
  fallbackTitle: string;
  key: string;
  label: string;
  overview?: boolean;
}

interface TechnologyBlockForm {
  description: string;
  imageMediaId?: number;
  title: string;
}

interface TechnologyForm {
  capabilityRows: TechnologyBlockForm[];
  contentHtml: string;
  ctaLabel: string;
  ctaTarget: string;
  eyebrow: string;
  heroMediaId?: number;
  highlightText: string;
  pageKey: string;
  pillars: TechnologyBlockForm[];
  seoDescription: string;
  seoKeywords: string;
  seoTitle: string;
  sortOrder: number;
  summary: string;
  title: string;
  version?: number;
}

interface MediaOption {
  id: number;
  name: string;
  previewSourceUrl: string;
  previewUrl: string;
}

const definitions: PageDefinition[] = [
  {
    description: '控制技术栏目主视觉、总览标题与首页技术概览。',
    fallbackTitle: '中南大学气凝胶成果转化核心平台',
    key: 'technology',
    label: '技术总览',
    overview: true,
  },
  {
    description: '常压干燥、双纳米结构与气凝胶材料制备。',
    fallbackTitle: '新一代气凝胶常压干燥技术',
    key: 'aerogel-material',
    label: '气凝胶材料技术',
  },
  {
    description: '粉碎、分散、结构保护与复合产品开发。',
    fallbackTitle: '气凝胶复合材料制备技术',
    key: 'aerogel-composite',
    label: '气凝胶复合产品技术',
  },
  {
    description: '固废资源化、功能材料与产业化装备等延伸能力。',
    fallbackTitle: '面向产业应用的延伸技术能力',
    key: 'other',
    label: '其他技术',
  },
];

const emptyBlock = (): TechnologyBlockForm => ({
  description: '',
  imageMediaId: undefined,
  title: '',
});

const emptyForm = (): TechnologyForm => ({
  capabilityRows: [],
  contentHtml: '',
  ctaLabel: '',
  ctaTarget: '',
  eyebrow: '',
  heroMediaId: undefined,
  highlightText: '',
  pageKey: 'technology',
  pillars: [],
  seoDescription: '',
  seoKeywords: '奥飞新材,气凝胶',
  seoTitle: '',
  sortOrder: 0,
  summary: '',
  title: '',
  version: undefined,
});

const pages = ref<TechnologyPageRecord[]>([]);
const loading = ref(false);
const saving = ref(false);
const statusBusyKey = ref('');
const dialogVisible = ref(false);
const mediaOptions = ref<MediaOption[]>([]);
const mediaLoaded = ref(false);
const form = reactive<TechnologyForm>(emptyForm());

const pageMap = computed(
  () => new Map(pages.value.map((item) => [item.pageKey, item])),
);
const overviewCard = computed(() => ({
  definition: definitions[0]!,
  page: pageMap.value.get('technology'),
}));
const detailCards = computed(() =>
  definitions.slice(1).map((definition) => ({
    definition,
    page: pageMap.value.get(definition.key),
  })),
);
const heroPreview = computed(
  () =>
    mediaOptions.value.find((item) => item.id === form.heroMediaId)
      ?.previewUrl || '',
);
const publishedCount = computed(
  () => pages.value.filter((item) => item.status === 'PUBLISHED').length,
);

function normalizeBlocks(value?: TechnologyBlockRecord[]) {
  if (!Array.isArray(value)) return [];
  return value.map((item) => ({
    description: String(item?.description || ''),
    imageMediaId: item?.imageMediaId || undefined,
    title: String(item?.title || ''),
  }));
}

function blockPreview(mediaId?: number) {
  return (
    mediaOptions.value.find((item) => item.id === mediaId)?.previewUrl || ''
  );
}

function statusLabel(value?: string) {
  if (value === 'PUBLISHED') return '已发布';
  if (value === 'OFFLINE') return '已下线';
  return '草稿';
}

function statusType(value?: string) {
  if (value === 'PUBLISHED') return 'success';
  if (value === 'OFFLINE') return 'info';
  return 'warning';
}

async function load() {
  loading.value = true;
  try {
    pages.value = await listTechnologyPages();
  } finally {
    loading.value = false;
  }
}

async function ensureMediaOptions() {
  if (mediaLoaded.value) return;
  const mediaRecords = await listMedia('', { page: 1, size: 100 });
  const records = mediaRecords.filter(
    (item: MediaRecord) => item.mediaType === 'IMAGE',
  );
  mediaOptions.value = records.map((item) => ({
    id: item.id,
    name: item.originalFilename,
    previewSourceUrl: item.thumbnailUrl || item.adminUrl,
    previewUrl: '',
  }));
  mediaLoaded.value = true;
}

async function loadSelectedMediaPreviews() {
  const selectedIds = new Set(
    [
      form.heroMediaId,
      ...form.capabilityRows.map((item) => item.imageMediaId),
      ...form.pillars.map((item) => item.imageMediaId),
    ].filter((id): id is number => Boolean(id)),
  );
  await Promise.all(
    mediaOptions.value
      .filter((item) => selectedIds.has(item.id) && !item.previewUrl)
      .map(async (item) => {
        item.previewUrl = await getMediaPreviewUrl(
          item.previewSourceUrl,
        ).catch(() => '');
      }),
  );
}

function fillForm(definition: PageDefinition, page?: TechnologyPageRecord) {
  Object.assign(form, emptyForm(), {
    capabilityRows: normalizeBlocks(page?.capabilityRows),
    contentHtml: page?.contentHtml || '',
    ctaLabel: page?.action?.label || '',
    ctaTarget: page?.action?.target || '',
    eyebrow: page?.eyebrow || definition.label,
    heroMediaId: page?.heroMediaId || undefined,
    highlightText: page?.highlightText || '',
    pageKey: definition.key,
    pillars: normalizeBlocks(page?.pillars),
    seoDescription: page?.seoDescription || page?.summary || '',
    seoKeywords: page?.seoKeywords || '奥飞新材,气凝胶',
    seoTitle: page?.seoTitle || '',
    sortOrder: Number(page?.sortOrder ?? definitions.indexOf(definition)),
    summary: page?.summary || '',
    title: page?.title || definition.fallbackTitle,
    version: page?.version,
  });
}

async function openEditor(
  definition: PageDefinition,
  current?: TechnologyPageRecord,
) {
  loading.value = true;
  try {
    const [page] = await Promise.all([
      current ? getTechnologyPage(definition.key) : Promise.resolve(undefined),
      ensureMediaOptions(),
    ]);
    fillForm(definition, page);
    dialogVisible.value = true;
    void loadSelectedMediaPreviews();
  } finally {
    loading.value = false;
  }
}

function addBlock(target: 'capabilityRows' | 'pillars') {
  form[target].push(emptyBlock());
}

function removeBlock(target: 'capabilityRows' | 'pillars', index: number) {
  form[target].splice(index, 1);
}

async function save() {
  if (!form.title.trim() || !form.summary.trim()) {
    ElMessage.warning('请填写页面标题和简介');
    return;
  }
  saving.value = true;
  try {
    await saveTechnologyPage(form.pageKey, {
      capabilityRows: form.capabilityRows.filter(
        (item) => item.title.trim() || item.description.trim(),
      ),
      contentHtml: form.contentHtml || null,
      ctaLabel: form.ctaLabel || null,
      ctaTarget: form.ctaTarget || null,
      eyebrow: form.eyebrow || null,
      heroMediaId: form.heroMediaId || null,
      highlightText: form.highlightText || null,
      pillars: form.pillars.filter(
        (item) => item.title.trim() || item.description.trim(),
      ),
      seoDescription: form.seoDescription || null,
      seoKeywords: form.seoKeywords || null,
      seoTitle: form.seoTitle || null,
      sortOrder: form.sortOrder,
      summary: form.summary,
      title: form.title,
      version: form.version,
    });
    dialogVisible.value = false;
    await load();
    ElMessage.success('技术页面已保存');
  } finally {
    saving.value = false;
  }
}

async function changeStatus(
  page: TechnologyPageRecord | undefined,
  next: 'offline' | 'published',
) {
  if (!page) {
    ElMessage.warning('请先保存页面内容');
    return;
  }
  statusBusyKey.value = page.pageKey;
  try {
    await changeTechnologyPageStatus(page.pageKey, next);
    await load();
    ElMessage.success(next === 'published' ? '页面已发布' : '页面已下线');
  } finally {
    statusBusyKey.value = '';
  }
}

void load();
</script>

<template>
  <div class="technology-admin-page">
    <section class="technology-admin-hero">
      <div>
        <p>TECHNOLOGY CONTENT</p>
        <h1>技术页面管理</h1>
        <span>
          维护技术总览与三个技术单页。编辑时始终读取最新版本，
          避免旧表单覆盖其他管理员的内容。
        </span>
      </div>
      <div class="hero-stat">
        <strong>{{ publishedCount }}/4</strong>
        <small>已发布页面</small>
      </div>
    </section>

    <section class="content-section">
      <div class="section-heading">
        <div>
          <span>技术栏目入口</span>
          <h2>技术总览</h2>
        </div>
      </div>
      <ElCard class="overview-card" shadow="never" v-loading="loading">
        <div class="page-card-main">
          <div class="page-index">00</div>
          <div>
            <div class="card-title-row">
              <h3>
                {{
                  overviewCard.page?.title ||
                  overviewCard.definition.fallbackTitle
                }}
              </h3>
              <ElTag :type="statusType(overviewCard.page?.status)">
                {{ statusLabel(overviewCard.page?.status) }}
              </ElTag>
            </div>
            <p>{{ overviewCard.definition.description }}</p>
            <small>/technology</small>
          </div>
        </div>
        <div class="card-actions">
          <ElButton
            type="primary"
            @click="openEditor(overviewCard.definition, overviewCard.page)"
          >
            编辑总览
          </ElButton>
          <ElButton
            v-if="overviewCard.page?.status !== 'PUBLISHED'"
            :loading="statusBusyKey === 'technology'"
            type="success"
            @click="changeStatus(overviewCard.page, 'published')"
          >
            发布
          </ElButton>
          <ElButton
            v-else
            :loading="statusBusyKey === 'technology'"
            @click="changeStatus(overviewCard.page, 'offline')"
          >
            下线
          </ElButton>
        </div>
      </ElCard>
    </section>

    <section class="content-section">
      <div class="section-heading">
        <div>
          <span>其他栏目</span>
          <h2>三个可编辑技术单页</h2>
        </div>
        <p>每个页面均可独立编辑、发布或下线，并拥有独立 SEO 信息。</p>
      </div>
      <div class="page-grid" v-loading="loading">
        <ElCard
          v-for="(card, index) in detailCards"
          :key="card.definition.key"
          class="page-card"
          shadow="never"
        >
          <div class="page-index">0{{ index + 1 }}</div>
          <div class="card-title-row">
            <h3>{{ card.definition.label }}</h3>
            <ElTag :type="statusType(card.page?.status)">
              {{ statusLabel(card.page?.status) }}
            </ElTag>
          </div>
          <strong>{{
            card.page?.title || card.definition.fallbackTitle
          }}</strong>
          <p>{{ card.definition.description }}</p>
          <small>/technology/{{ card.definition.key }}</small>
          <div class="card-actions">
            <ElButton
              type="primary"
              @click="openEditor(card.definition, card.page)"
            >
              编辑内容
            </ElButton>
            <ElButton
              v-if="card.page?.status !== 'PUBLISHED'"
              :loading="statusBusyKey === card.definition.key"
              type="success"
              @click="changeStatus(card.page, 'published')"
            >
              发布
            </ElButton>
            <ElButton
              v-else
              :loading="statusBusyKey === card.definition.key"
              @click="changeStatus(card.page, 'offline')"
            >
              下线
            </ElButton>
          </div>
        </ElCard>
      </div>
    </section>

    <ElDialog
      v-model="dialogVisible"
      :close-on-click-modal="false"
      :title="`编辑${definitions.find((item) => item.key === form.pageKey)?.label || '技术页面'}`"
      destroy-on-close
      width="900px"
    >
      <ElForm :model="form" label-position="top">
        <ElRow :gutter="18">
          <ElCol :md="16" :xs="24">
            <ElFormItem label="页面标题" required>
              <ElInput v-model="form.title" maxlength="80" show-word-limit />
            </ElFormItem>
          </ElCol>
          <ElCol :md="8" :xs="24">
            <ElFormItem label="页面标识">
              <ElInput :model-value="form.pageKey" disabled />
            </ElFormItem>
          </ElCol>
          <ElCol :md="12" :xs="24">
            <ElFormItem label="栏目小标题">
              <ElInput v-model="form.eyebrow" maxlength="30" show-word-limit />
            </ElFormItem>
          </ElCol>
          <ElCol :md="12" :xs="24">
            <ElFormItem label="强调标题">
              <ElInput
                v-model="form.highlightText"
                maxlength="40"
                show-word-limit
              />
            </ElFormItem>
          </ElCol>
          <ElCol :span="24">
            <ElFormItem label="页面简介" required>
              <ElInput
                v-model="form.summary"
                :rows="4"
                maxlength="1000"
                show-word-limit
                type="textarea"
              />
            </ElFormItem>
          </ElCol>
          <ElCol :md="16" :xs="24">
            <ElFormItem label="主视觉图片">
              <ElSelect
                v-model="form.heroMediaId"
                clearable
                filterable
                placeholder="从素材库选择图片"
                style="width: 100%"
                @change="loadSelectedMediaPreviews"
              >
                <ElOption
                  v-for="item in mediaOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </ElSelect>
            </ElFormItem>
          </ElCol>
          <ElCol :md="8" :xs="24">
            <ElFormItem label="显示顺序">
              <ElInputNumber
                v-model="form.sortOrder"
                :min="0"
                style="width: 100%"
              />
            </ElFormItem>
          </ElCol>
          <ElCol v-if="heroPreview" :span="24">
            <ElImage
              :preview-src-list="[heroPreview]"
              :src="heroPreview"
              class="hero-preview"
              fit="cover"
            />
          </ElCol>
        </ElRow>

        <ElDivider content-position="left">核心内容模块</ElDivider>
        <p class="field-help">
          这些模块会显示为页面上半部分的重点技术卡片。每个模块可独立选择素材库图片。
        </p>
        <div
          v-for="(block, index) in form.capabilityRows"
          :key="`capability-${index}`"
          class="block-editor capability-block-editor"
        >
          <div class="block-copy-editor">
            <ElInput
              v-model="block.title"
              :aria-label="`模块 ${index + 1} 标题`"
              :placeholder="`模块 ${index + 1} 标题`"
            />
            <ElInput
              v-model="block.description"
              :aria-label="`模块 ${index + 1} 说明`"
              :rows="3"
              placeholder="模块说明"
              type="textarea"
            />
          </div>
          <div class="block-media-editor">
            <ElSelect
              v-model="block.imageMediaId"
              :aria-label="`模块 ${index + 1} 图片`"
              clearable
              filterable
              placeholder="选择模块图片"
              style="width: 100%"
              @change="loadSelectedMediaPreviews"
            >
              <ElOption
                v-for="item in mediaOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </ElSelect>
            <ElImage
              v-if="blockPreview(block.imageMediaId)"
              :alt="block.title || `模块 ${index + 1} 图片预览`"
              :preview-src-list="[blockPreview(block.imageMediaId)]"
              :src="blockPreview(block.imageMediaId)"
              class="block-image-preview"
              fit="cover"
            />
          </div>
          <ElButton
            plain
            type="danger"
            @click="removeBlock('capabilityRows', index)"
          >
            删除
          </ElButton>
        </div>
        <ElButton plain @click="addBlock('capabilityRows')">
          + 添加内容模块
        </ElButton>

        <ElDivider content-position="left">技术亮点 / 应用方向</ElDivider>
        <div
          v-for="(block, index) in form.pillars"
          :key="`pillar-${index}`"
          class="block-editor"
        >
          <ElInput
            v-model="block.title"
            :placeholder="`亮点 ${index + 1} 标题`"
          />
          <ElInput
            v-model="block.description"
            :rows="2"
            placeholder="亮点说明"
            type="textarea"
          />
          <ElButton plain type="danger" @click="removeBlock('pillars', index)">
            删除
          </ElButton>
        </div>
        <ElButton plain @click="addBlock('pillars')">+ 添加技术亮点</ElButton>

        <ElDivider content-position="left">正文与行动按钮</ElDivider>
        <ElFormItem label="补充正文">
          <RichTextMediaEditor
            v-model="form.contentHtml"
            :min-height="240"
            placeholder="请输入补充正文；可设置字体、字号、颜色、对齐方式，并从工具栏插入图片"
          />
        </ElFormItem>
        <ElRow :gutter="18">
          <ElCol :md="8" :xs="24">
            <ElFormItem label="按钮文字">
              <ElInput v-model="form.ctaLabel" placeholder="获取技术资料" />
            </ElFormItem>
          </ElCol>
          <ElCol :md="16" :xs="24">
            <ElFormItem label="按钮链接">
              <ElInput v-model="form.ctaTarget" placeholder="/cooperation" />
            </ElFormItem>
          </ElCol>
        </ElRow>

        <ElDivider content-position="left">SEO 设置</ElDivider>
        <ElFormItem label="SEO 标题">
          <ElInput v-model="form.seoTitle" maxlength="255" show-word-limit />
        </ElFormItem>
        <ElFormItem label="SEO 描述">
          <ElInput
            v-model="form.seoDescription"
            :rows="2"
            maxlength="500"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="SEO 关键词">
          <ElInput v-model="form.seoKeywords" placeholder="使用英文逗号分隔" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton :loading="saving" type="primary" @click="save">
          保存内容
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.technology-admin-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7f8;
}

.technology-admin-hero {
  display: flex;
  gap: 30px;
  align-items: center;
  justify-content: space-between;
  padding: 30px;
  color: #fff;
  background:
    radial-gradient(circle at 85% 20%, rgb(61 169 165 / 38%), transparent 24%),
    linear-gradient(120deg, #092c36, #0e5960 66%, #1b7780);
  border-radius: 18px;
  box-shadow: 0 18px 50px rgb(9 44 54 / 16%);
}

.technology-admin-hero p {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  color: #79d2c8;
  letter-spacing: 0.18em;
}

.technology-admin-hero h1 {
  margin: 5px 0 8px;
  font-size: 28px;
  font-weight: 650;
}

.technology-admin-hero span {
  display: block;
  max-width: 720px;
  line-height: 1.7;
  color: rgb(255 255 255 / 72%);
}

.hero-stat {
  display: flex;
  flex-direction: column;
  min-width: 130px;
  padding: 18px 22px;
  background: rgb(255 255 255 / 8%);
  border: 1px solid rgb(255 255 255 / 18%);
  border-radius: 14px;
}

.hero-stat strong {
  font-size: 28px;
}

.hero-stat small {
  color: rgb(255 255 255 / 68%);
}

.content-section {
  margin-top: 24px;
}

.section-heading {
  display: flex;
  gap: 20px;
  align-items: end;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-heading span {
  font-size: 11px;
  font-weight: 750;
  color: #0e7776;
  letter-spacing: 0.12em;
}

.section-heading h2 {
  margin: 3px 0 0;
  font-size: 20px;
  color: #17343b;
}

.section-heading > p {
  margin: 0;
  font-size: 13px;
  color: #7e8a8f;
}

.overview-card,
.page-card {
  border: 1px solid #e2e8ea;
  border-radius: 16px;
}

.overview-card :deep(.el-card__body) {
  display: flex;
  gap: 24px;
  align-items: center;
  justify-content: space-between;
  padding: 24px;
}

.page-card-main {
  display: flex;
  gap: 18px;
  align-items: center;
}

.page-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.page-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  min-height: 270px;
  padding: 22px;
}

.page-index {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 42px;
  height: 42px;
  font-size: 12px;
  font-weight: 800;
  color: #fff;
  background: #0e7776;
  border-radius: 10px;
}

.card-title-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.card-title-row h3 {
  margin: 0;
  font-size: 17px;
  color: #17343b;
}

.page-card > strong {
  margin-top: 18px;
  font-size: 14px;
  color: #334e55;
}

.overview-card p,
.page-card p {
  margin: 8px 0;
  line-height: 1.65;
  color: #718187;
}

.overview-card small,
.page-card small {
  color: #a0aaad;
}

.card-actions {
  display: flex;
  gap: 8px;
  padding-top: 22px;
  margin-top: auto;
}

.hero-preview {
  width: 240px;
  height: 128px;
  margin-bottom: 18px;
  border-radius: 10px;
}

.field-help {
  margin: -4px 0 14px;
  font-size: 12px;
  color: #89969a;
}

.block-editor {
  display: grid;
  grid-template-columns: minmax(180px, 0.7fr) minmax(260px, 1.5fr) auto;
  gap: 10px;
  align-items: start;
  padding: 12px;
  margin-bottom: 10px;
  background: #f6f8f8;
  border-radius: 10px;
}

.capability-block-editor {
  grid-template-columns: minmax(280px, 1fr) 220px auto;
}

.block-copy-editor,
.block-media-editor {
  display: grid;
  gap: 10px;
}

.block-image-preview {
  width: 100%;
  height: 118px;
  border-radius: 8px;
}

@media (max-width: 900px) {
  .page-grid {
    grid-template-columns: 1fr;
  }

  .page-card :deep(.el-card__body) {
    min-height: auto;
  }

  .block-editor {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .technology-admin-page {
    padding: 14px;
  }

  .technology-admin-hero,
  .overview-card :deep(.el-card__body),
  .section-heading {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
