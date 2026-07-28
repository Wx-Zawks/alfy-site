<script lang="ts" setup>
import type { ContentPageRecord, MediaRecord } from '#/api';

import { computed, onBeforeUnmount, reactive, ref } from 'vue';

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
  changeContentPageStatus,
  getContentPage,
  getMediaPreviewUrl,
  listContentPages,
  listMedia,
  saveContentPage,
} from '#/api';

interface PageDefinition {
  description: string;
  fallbackTitle: string;
  key: string;
  label: string;
  overview?: boolean;
  path: string;
}

interface ContentBlock {
  description: string;
  title: string;
}

interface CooperationContentData {
  actionLabel?: string;
  eyebrow?: string;
  faqEyebrow?: string;
  faqTitle?: string;
  faqs?: ContentBlock[];
  highlightText?: string;
  process?: ContentBlock[];
  processEyebrow?: string;
  processTitle?: string;
  sectionEyebrow?: string;
  sectionTitle?: string;
}

interface CooperationForm {
  actionLabel: string;
  contentHtml: string;
  coverMediaId?: number;
  eyebrow: string;
  faqEyebrow: string;
  faqTitle: string;
  faqs: ContentBlock[];
  highlightText: string;
  id?: number;
  pageKey: string;
  process: ContentBlock[];
  processEyebrow: string;
  processTitle: string;
  sectionEyebrow: string;
  sectionTitle: string;
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
  previewUrl: string;
}

const definitions: PageDefinition[] = [
  {
    description: '合作栏目入口，维护主视觉、三类合作入口、合作流程与常见问题。',
    fallbackTitle: '开放共赢，共建',
    key: 'cooperation',
    label: '合作综合介绍',
    overview: true,
    path: '/cooperation',
  },
  {
    description: '面向区域客户、项目服务与建材渠道伙伴的合作说明。',
    fallbackTitle: '经销商合作',
    key: 'cooperation-dealer',
    label: '经销商合作',
    path: '/cooperation/dealer',
  },
  {
    description: '面向气凝胶粉体、分散体和复合体系的联合开发说明。',
    fallbackTitle: '复合产品开发模式',
    key: 'cooperation-product-development',
    label: '复合产品开发',
    path: '/cooperation/product-development',
  },
  {
    description: '面向科研、材料、制造、设计与工程资源的产业协同说明。',
    fallbackTitle: '产业链合作模式',
    key: 'cooperation-industry-chain',
    label: '产业链合作',
    path: '/cooperation/industry-chain',
  },
];

const defaultProcess = (): ContentBlock[] => [
  { description: '明确市场、场景与合作目标。', title: '需求沟通' },
  { description: '确认双方资源和合作边界。', title: '能力评估' },
  { description: '通过样品、小试或项目验证。', title: '方案验证' },
  { description: '明确交付、支持和复盘机制。', title: '合作落地' },
];

const defaultFaqs = (): ContentBlock[] => [
  {
    description:
      '提交目标区域、现有渠道、服务团队和重点客户类型，商务团队评估后安排沟通。',
    title: '如何申请成为区域经销合作伙伴？',
  },
  {
    description: '建议提供目标产品体系、关键性能、基材、工艺条件和计划周期。',
    title: '联合开发需要提供哪些信息？',
  },
  {
    description: '可根据项目情况讨论样品、小试或联合测试方式。',
    title: '是否可以先做样品或小试？',
  },
];

const emptyBlock = (): ContentBlock => ({ description: '', title: '' });
const emptyForm = (): CooperationForm => ({
  actionLabel: '发起合作咨询',
  contentHtml: '',
  coverMediaId: undefined,
  eyebrow: 'OPEN COLLABORATION',
  faqEyebrow: '常见问题',
  faqTitle: '合作前，您可能关心',
  faqs: defaultFaqs(),
  highlightText: '',
  id: undefined,
  pageKey: 'cooperation',
  process: defaultProcess(),
  processEyebrow: '合作流程',
  processTitle: '让合作预期清晰可见',
  sectionEyebrow: '合作模式',
  sectionTitle: '选择适合双方资源的合作方式',
  seoDescription: '',
  seoKeywords: '奥飞新材,商务合作,气凝胶',
  seoTitle: '',
  sortOrder: 20,
  summary: '',
  title: '',
  version: undefined,
});

const pages = ref<ContentPageRecord[]>([]);
const loading = ref(false);
const saving = ref(false);
const statusBusyKey = ref('');
const dialogVisible = ref(false);
const mediaOptions = ref<MediaOption[]>([]);
const mediaLoaded = ref(false);
const form = reactive<CooperationForm>(emptyForm());

const pageMap = computed(
  () => new Map(pages.value.map((item) => [item.pageKey, item])),
);
const overviewCard = computed(() => ({
  definition: definitions[0]!,
  page: pageMap.value.get('cooperation'),
}));
const detailCards = computed(() =>
  definitions.slice(1).map((definition) => ({
    definition,
    page: pageMap.value.get(definition.key),
  })),
);
const currentDefinition = computed(() =>
  definitions.find((item) => item.key === form.pageKey),
);
const isOverview = computed(() => Boolean(currentDefinition.value?.overview));
const coverPreview = computed(
  () =>
    mediaOptions.value.find((item) => item.id === form.coverMediaId)
      ?.previewUrl || '',
);
const publishedCount = computed(
  () => pages.value.filter((item) => item.status === 'PUBLISHED').length,
);

function normalizeBlocks(value: unknown, fallback: () => ContentBlock[]) {
  if (!Array.isArray(value)) return fallback();
  return value.map((item) => {
    const block = (item || {}) as Record<string, unknown>;
    return {
      description: String(block.description || ''),
      title: String(block.title || ''),
    };
  });
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
    const keys = new Set(definitions.map((item) => item.key));
    const contentPages = await listContentPages();
    pages.value = contentPages.filter((item) => keys.has(item.pageKey));
  } finally {
    loading.value = false;
  }
}

async function ensureMediaOptions() {
  if (mediaLoaded.value) return;
  const mediaRecords = await listMedia();
  const records = mediaRecords.filter(
    (item: MediaRecord) => item.mediaType === 'IMAGE',
  );
  mediaOptions.value = await Promise.all(
    records.map(async (item) => ({
      id: item.id,
      name: item.originalFilename,
      previewUrl: await getMediaPreviewUrl(item.adminUrl).catch(() => ''),
    })),
  );
  mediaLoaded.value = true;
}

function fillForm(definition: PageDefinition, page?: ContentPageRecord) {
  const data = (page?.contentData || {}) as CooperationContentData;
  Object.assign(form, emptyForm(), {
    actionLabel:
      data.actionLabel || (definition.overview ? '发起合作咨询' : '咨询合作'),
    contentHtml: page?.contentHtml || '',
    coverMediaId: page?.coverMediaId || undefined,
    eyebrow:
      data.eyebrow ||
      (definition.overview ? 'OPEN COLLABORATION' : definition.label),
    faqEyebrow: data.faqEyebrow || '常见问题',
    faqTitle: data.faqTitle || '合作前，您可能关心',
    faqs: normalizeBlocks(data.faqs, defaultFaqs),
    highlightText: data.highlightText || '',
    id: page?.id,
    pageKey: definition.key,
    process: normalizeBlocks(data.process, defaultProcess),
    processEyebrow: data.processEyebrow || '合作流程',
    processTitle: data.processTitle || '让合作预期清晰可见',
    sectionEyebrow: data.sectionEyebrow || '合作模式',
    sectionTitle: data.sectionTitle || '选择适合双方资源的合作方式',
    seoDescription: page?.seoDescription || page?.summary || '',
    seoKeywords: page?.seoKeywords || '奥飞新材,商务合作,气凝胶',
    seoTitle: page?.seoTitle || '',
    sortOrder: Number(page?.sortOrder ?? definitions.indexOf(definition) + 20),
    summary: page?.summary || '',
    title: page?.title || definition.fallbackTitle,
    version: page?.version,
  });
}

async function openEditor(
  definition: PageDefinition,
  current?: ContentPageRecord,
) {
  loading.value = true;
  try {
    const [page] = await Promise.all([
      current ? getContentPage(current.id) : Promise.resolve(undefined),
      ensureMediaOptions(),
    ]);
    fillForm(definition, page);
    dialogVisible.value = true;
  } finally {
    loading.value = false;
  }
}

function addBlock(target: 'faqs' | 'process') {
  form[target].push(emptyBlock());
}

function removeBlock(target: 'faqs' | 'process', index: number) {
  form[target].splice(index, 1);
}

function cleanBlocks(value: ContentBlock[]) {
  return value.filter((item) => item.title.trim() || item.description.trim());
}

async function save() {
  if (!form.title.trim() || !form.summary.trim()) {
    ElMessage.warning('请填写页面标题和简介');
    return;
  }
  saving.value = true;
  try {
    const contentData: CooperationContentData = {
      actionLabel: form.actionLabel || undefined,
      eyebrow: form.eyebrow || undefined,
      highlightText: form.highlightText || undefined,
    };
    if (isOverview.value) {
      Object.assign(contentData, {
        faqEyebrow: form.faqEyebrow || undefined,
        faqTitle: form.faqTitle || undefined,
        faqs: cleanBlocks(form.faqs),
        process: cleanBlocks(form.process),
        processEyebrow: form.processEyebrow || undefined,
        processTitle: form.processTitle || undefined,
        sectionEyebrow: form.sectionEyebrow || undefined,
        sectionTitle: form.sectionTitle || undefined,
      });
    }

    await saveContentPage(form.id || null, {
      category: isOverview.value ? '商务合作' : '合作模式',
      contentData,
      contentHtml: form.contentHtml || null,
      coverMediaId: form.coverMediaId || null,
      featured: isOverview.value,
      pageKey: form.pageKey,
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
    ElMessage.success('合作页面已保存');
  } finally {
    saving.value = false;
  }
}

async function changeStatus(
  page: ContentPageRecord | undefined,
  next: 'offline' | 'published',
) {
  if (!page) {
    ElMessage.warning('请先保存页面内容');
    return;
  }
  statusBusyKey.value = page.pageKey;
  try {
    await changeContentPageStatus(page.id, next);
    await load();
    ElMessage.success(next === 'published' ? '页面已发布' : '页面已下线');
  } finally {
    statusBusyKey.value = '';
  }
}

onBeforeUnmount(() => {
  mediaOptions.value.forEach((item) => {
    if (item.previewUrl.startsWith('blob:'))
      URL.revokeObjectURL(item.previewUrl);
  });
});

void load();
</script>

<template>
  <div class="cooperation-admin-page">
    <section class="cooperation-admin-hero">
      <div>
        <p>COOPERATION CONTENT</p>
        <h1>合作页面管理</h1>
        <span>
          维护合作综合介绍与三个合作模式单页。每个页面可独立编辑、发布或下线；
          此处内容不会修改首页底部写死的合作卡片。
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
          <span>合作栏目入口</span>
          <h2>合作综合介绍页面</h2>
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
            <small>{{ overviewCard.definition.path }}</small>
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
            :loading="statusBusyKey === 'cooperation'"
            type="success"
            @click="changeStatus(overviewCard.page, 'published')"
          >
            发布
          </ElButton>
          <ElButton
            v-else
            :loading="statusBusyKey === 'cooperation'"
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
          <span>合作模式</span>
          <h2>三个可编辑合作单页</h2>
        </div>
        <p>总览页的三张合作卡片会自动读取这些已发布单页。</p>
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
          <small>{{ card.definition.path }}</small>
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
      :title="`编辑${currentDefinition?.label || '合作页面'}`"
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
              <ElInput v-model="form.eyebrow" maxlength="40" show-word-limit />
            </ElFormItem>
          </ElCol>
          <ElCol :md="12" :xs="24">
            <ElFormItem label="红色强调标题">
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
                :rows="3"
                maxlength="1000"
                show-word-limit
                type="textarea"
              />
            </ElFormItem>
          </ElCol>
          <ElCol :md="16" :xs="24">
            <ElFormItem label="主视觉图片">
              <ElSelect
                v-model="form.coverMediaId"
                clearable
                filterable
                placeholder="从素材库选择图片"
                style="width: 100%"
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
          <ElCol v-if="coverPreview" :span="24">
            <ElImage
              :preview-src-list="[coverPreview]"
              :src="coverPreview"
              class="hero-preview"
              fit="cover"
            />
          </ElCol>
        </ElRow>

        <template v-if="isOverview">
          <ElDivider content-position="left">合作模式区</ElDivider>
          <ElRow :gutter="18">
            <ElCol :md="8" :xs="24">
              <ElFormItem label="区块小标题">
                <ElInput v-model="form.sectionEyebrow" />
              </ElFormItem>
            </ElCol>
            <ElCol :md="16" :xs="24">
              <ElFormItem label="区块标题">
                <ElInput v-model="form.sectionTitle" />
              </ElFormItem>
            </ElCol>
          </ElRow>
          <p class="field-help">
            三张模式卡片的标题和简介来自下面三个合作单页，无需重复填写。
          </p>

          <ElDivider content-position="left">合作流程</ElDivider>
          <ElRow :gutter="18">
            <ElCol :md="8" :xs="24">
              <ElFormItem label="区块小标题">
                <ElInput v-model="form.processEyebrow" />
              </ElFormItem>
            </ElCol>
            <ElCol :md="16" :xs="24">
              <ElFormItem label="区块标题">
                <ElInput v-model="form.processTitle" />
              </ElFormItem>
            </ElCol>
          </ElRow>
          <div
            v-for="(block, index) in form.process"
            :key="`process-${index}`"
            class="block-editor"
          >
            <ElInput
              v-model="block.title"
              :placeholder="`步骤 ${index + 1} 标题`"
            />
            <ElInput
              v-model="block.description"
              :rows="2"
              placeholder="步骤说明"
              type="textarea"
            />
            <ElButton
              plain
              type="danger"
              @click="removeBlock('process', index)"
            >
              删除
            </ElButton>
          </div>
          <ElButton plain @click="addBlock('process')">+ 添加合作步骤</ElButton>

          <ElDivider content-position="left">常见问题</ElDivider>
          <ElRow :gutter="18">
            <ElCol :md="8" :xs="24">
              <ElFormItem label="区块小标题">
                <ElInput v-model="form.faqEyebrow" />
              </ElFormItem>
            </ElCol>
            <ElCol :md="16" :xs="24">
              <ElFormItem label="区块标题">
                <ElInput v-model="form.faqTitle" />
              </ElFormItem>
            </ElCol>
          </ElRow>
          <div
            v-for="(block, index) in form.faqs"
            :key="`faq-${index}`"
            class="block-editor"
          >
            <ElInput v-model="block.title" :placeholder="`问题 ${index + 1}`" />
            <ElInput
              v-model="block.description"
              :rows="2"
              placeholder="回答内容"
              type="textarea"
            />
            <ElButton plain type="danger" @click="removeBlock('faqs', index)">
              删除
            </ElButton>
          </div>
          <ElButton plain @click="addBlock('faqs')">+ 添加常见问题</ElButton>
        </template>

        <ElDivider content-position="left">正文与咨询按钮</ElDivider>
        <ElFormItem :label="isOverview ? '补充正文 HTML' : '页面正文 HTML'">
          <ElInput
            v-model="form.contentHtml"
            :rows="10"
            placeholder="可填写段落、标题、列表和链接；后端保存时会安全清洗"
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="咨询按钮文字">
          <ElInput v-model="form.actionLabel" placeholder="发起合作咨询" />
        </ElFormItem>

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
.cooperation-admin-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7f8;
}

.cooperation-admin-hero {
  display: flex;
  gap: 30px;
  align-items: center;
  justify-content: space-between;
  padding: 30px;
  color: #fff;
  background:
    radial-gradient(circle at 84% 18%, rgb(239 75 69 / 38%), transparent 25%),
    linear-gradient(120deg, #241f32, #44364d 62%, #8e373c);
  border-radius: 18px;
  box-shadow: 0 18px 50px rgb(36 31 50 / 16%);
}

.cooperation-admin-hero p {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  color: #ffaaa2;
  letter-spacing: 0.18em;
}

.cooperation-admin-hero h1 {
  margin: 5px 0 8px;
  font-size: 28px;
  font-weight: 650;
}

.cooperation-admin-hero span {
  display: block;
  max-width: 720px;
  line-height: 1.7;
  color: rgb(255 255 255 / 76%);
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
  color: #bb403e;
  letter-spacing: 0.12em;
}

.section-heading h2 {
  margin: 3px 0 0;
  font-size: 20px;
  color: #312a3d;
}

.section-heading > p {
  margin: 0;
  font-size: 13px;
  color: #7c8791;
}

.overview-card :deep(.el-card__body) {
  display: flex;
  gap: 24px;
  align-items: center;
  justify-content: space-between;
  padding: 26px;
}

.page-card-main {
  display: flex;
  gap: 18px;
  align-items: center;
}

.page-index {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 38px;
  height: 38px;
  font-size: 12px;
  font-weight: 750;
  color: #fff;
  background: #b63e3b;
  border-radius: 10px;
}

.card-title-row {
  display: flex;
  gap: 14px;
  align-items: center;
  justify-content: space-between;
}

.card-title-row h3 {
  margin: 0;
  font-size: 16px;
  color: #303741;
}

.page-card-main p,
.page-card p {
  margin: 8px 0;
  line-height: 1.55;
  color: #77828d;
}

.page-card-main small,
.page-card small {
  color: #a0abb4;
}

.page-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.page-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  min-height: 220px;
  padding: 20px;
}

.page-card > :deep(.el-card__body) > strong {
  margin-top: 12px;
  color: #303741;
}

.card-actions {
  display: flex;
  gap: 8px;
  padding-top: 20px;
  margin-top: auto;
}

.block-editor {
  display: grid;
  grid-template-columns: minmax(180px, 0.7fr) minmax(260px, 1.4fr) auto;
  gap: 10px;
  margin-bottom: 10px;
}

.field-help {
  margin: -4px 0 14px;
  font-size: 13px;
  color: #85909a;
}

.hero-preview {
  width: 100%;
  height: 220px;
  border-radius: 10px;
}

@media (max-width: 1100px) {
  .page-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .cooperation-admin-hero,
  .overview-card :deep(.el-card__body),
  .section-heading {
    flex-direction: column;
    align-items: flex-start;
  }

  .block-editor {
    grid-template-columns: 1fr;
  }
}
</style>
