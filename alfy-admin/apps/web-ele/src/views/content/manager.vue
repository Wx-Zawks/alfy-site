<script lang="ts" setup>
import type { ArticleCategoryRecord } from '#/api';
import type { ContentItem, ContentResource, ContentStatus } from '#/data/cms';

import { computed, reactive, ref, toRaw, watch } from 'vue';

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
  ElPopconfirm,
  ElRow,
  ElSelect,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import {
  changeContentStatus,
  deleteContent,
  getContent,
  getMediaPreviewUrl,
  listArticleCategories,
  listContent,
  listMedia,
  listProductCategories,
  saveContent,
} from '#/api';
import { cmsState, resourceMeta } from '#/data/cms';
import {
  contentFromBackend,
  contentPayload,
  mediaIdFromUrl,
} from '#/data/cms-adapter';

const props = defineProps<{ resource: ContentResource }>();

const keyword = ref('');
const status = ref<'all' | ContentStatus>('all');
const category = ref('all');
const dialogVisible = ref(false);
const activeId = ref<null | number>(null);
const loading = ref(false);
const saving = ref(false);
const referenceOptions = ref<Array<{ id: number; name: string }>>([]);
const relatedOptions = ref<Array<{ id: number; name: string }>>([]);
const originalStatus = ref<ContentStatus>('draft');
const featuresText = ref('');
const specificationsText = ref('');
const capabilityRowsText = ref('');
const pillarsText = ref('');

const meta = computed(() => resourceMeta[props.resource]);
const isHomePlacementResource = computed(() => props.resource === 'articles');
const supportsDelete = computed(() => props.resource !== 'technologies');
const categoryOptions = computed(() =>
  referenceOptions.value.length > 0
    ? referenceOptions.value.map((item) => item.name)
    : meta.value.categories,
);
const resourceItems = computed(() =>
  cmsState.content.filter((item) => item.resource === props.resource),
);
const filteredItems = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return resourceItems.value
    .filter((item) => status.value === 'all' || item.status === status.value)
    .filter(
      (item) => category.value === 'all' || item.category === category.value,
    )
    .filter(
      (item) =>
        !query ||
        item.title.toLowerCase().includes(query) ||
        item.slug.toLowerCase().includes(query) ||
        item.summary.toLowerCase().includes(query),
    )
    .sort((a, b) => a.sortOrder - b.sortOrder);
});

const stats = computed(() => ({
  draft: resourceItems.value.filter((item) => item.status === 'draft').length,
  featured: resourceItems.value.filter((item) =>
    isHomePlacementResource.value
      ? item.homePinned || item.showOnHome
      : item.featured,
  ).length,
  published: resourceItems.value.filter((item) => item.status === 'published')
    .length,
  total: resourceItems.value.length,
}));

const imageOptions = computed(() =>
  cmsState.media.filter((asset) => asset.type === 'image'),
);

const emptyForm = (): ContentItem => ({
  category: categoryOptions.value[0] || '未分类',
  categoryId: undefined,
  categoryIds: [],
  contentHtml: '',
  cover: '',
  coverMediaId: undefined,
  createdAt: '',
  eyebrow: '',
  featured: false,
  highlightTitle: '',
  homePinned: false,
  homeSortOrder: resourceItems.value.length + 1,
  id: 0,
  mobileCover: '',
  mobileMediaId: undefined,
  primaryActionLabel: '',
  primaryActionLink: '',
  raw: {},
  resource: props.resource,
  secondaryActionLabel: '',
  secondaryActionLink: '',
  seoDescription: '',
  seoKeywords: '奥飞新材,气凝胶',
  seoTitle: '',
  showOnHome: false,
  slug: '',
  sortOrder: resourceItems.value.length + 1,
  status: 'draft',
  summary: '',
  title: '',
  updatedAt: '',
  version: undefined,
});

const form = reactive<ContentItem>(emptyForm());
const rawForm = computed<Record<string, any>>(() => form.raw || {});

function replaceResource(items: ContentItem[]) {
  const retained = cmsState.content.filter(
    (item) => item.resource !== props.resource,
  );
  cmsState.content.splice(0, cmsState.content.length, ...retained, ...items);
}

function readableSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

async function loadReferences() {
  referenceOptions.value = [];
  relatedOptions.value = [];
  const media = await listMedia();
  const mappedMedia = await Promise.all(
    media.map(async (item) => ({
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
  cmsState.media.splice(0, cmsState.media.length, ...mappedMedia);

  switch (props.resource) {
    case 'articles': {
      const flatten = (
        values: ArticleCategoryRecord[],
      ): ArticleCategoryRecord[] =>
        values.flatMap((item) => [item, ...flatten(item.children || [])]);
      const values = flatten(await listArticleCategories());
      referenceOptions.value = values.map((item) => ({
        id: item.id,
        name: item.name,
      }));

      break;
    }
    case 'cases': {
      const [values, products] = await Promise.all([
        listContent('scenes'),
        listContent('products'),
      ]);
      referenceOptions.value = values.map((item) => ({
        id: Number(item.id),
        name: String(item.name || item.title || item.slug),
      }));
      relatedOptions.value = products.map((item) => ({
        id: Number(item.id),
        name: String(item.name || item.title || item.slug),
      }));

      break;
    }
    case 'products': {
      const [values, scenes] = await Promise.all([
        listProductCategories(),
        listContent('scenes'),
      ]);
      referenceOptions.value = values.map((item) => ({
        id: item.id,
        name: item.name,
      }));
      relatedOptions.value = scenes.map((item) => ({
        id: Number(item.id),
        name: String(item.name || item.title || item.slug),
      }));

      break;
    }
    default: {
      referenceOptions.value = [];
      relatedOptions.value = [];
    }
  }
}

async function load() {
  loading.value = true;
  try {
    const values = await listContent(props.resource);
    // Content is the primary data for this page. Render it before loading
    // media and category helpers so an auxiliary request cannot blank the list.
    replaceResource(
      values.map((item) => contentFromBackend(props.resource, item)),
    );

    try {
      await loadReferences();
      const names = new Map(
        referenceOptions.value.map((item) => [item.id, item.name]),
      );
      replaceResource(
        values.map((item) => contentFromBackend(props.resource, item, names)),
      );
    } catch (error) {
      console.warn('辅助素材或分类加载失败，内容列表仍保持可用', error);
    }
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.resource,
  async () => {
    keyword.value = '';
    status.value = 'all';
    category.value = 'all';
    await load();
  },
  { immediate: true },
);

function applyReference() {
  if (props.resource === 'articles' && form.categoryIds?.length) {
    form.categoryId = form.categoryIds[0];
    form.category =
      referenceOptions.value.find((item) => item.id === form.categoryId)
        ?.name || form.category;
    return;
  }
  const selected = referenceOptions.value.find(
    (item) => item.name === form.category,
  );
  form.categoryId = selected?.id;
  form.categoryIds = selected ? [selected.id] : [];
}

function resetAdvancedFields() {
  featuresText.value = '';
  specificationsText.value = '';
  capabilityRowsText.value = '';
  pillarsText.value = '';
}

function loadAdvancedFields(item: ContentItem) {
  const raw = item.raw || {};
  featuresText.value = Array.isArray(raw.features)
    ? raw.features.join('\n')
    : '';
  specificationsText.value = raw.specifications
    ? JSON.stringify(raw.specifications, null, 2)
    : '';
  capabilityRowsText.value = raw.capabilityRows
    ? JSON.stringify(raw.capabilityRows, null, 2)
    : '';
  pillarsText.value = raw.pillars ? JSON.stringify(raw.pillars, null, 2) : '';
}

function applyAdvancedFields() {
  if (props.resource === 'products') {
    rawForm.value.features = featuresText.value
      .split(/[,，\n]/)
      .map((item) => item.trim())
      .filter(Boolean);
  }
  try {
    if (props.resource === 'products') {
      rawForm.value.specifications = specificationsText.value.trim()
        ? JSON.parse(specificationsText.value)
        : null;
    }
    if (props.resource === 'technologies') {
      rawForm.value.capabilityRows = capabilityRowsText.value.trim()
        ? JSON.parse(capabilityRowsText.value)
        : [];
      rawForm.value.pillars = pillarsText.value.trim()
        ? JSON.parse(pillarsText.value)
        : [];
    }
  } catch {
    ElMessage.warning('JSON 字段格式不正确，请检查后重试');
    return false;
  }
  return true;
}

function previewUrl(value: string) {
  const id = mediaIdFromUrl(value);
  return cmsState.media.find((item) => item.id === id)?.url || value;
}

function openCreate() {
  const current = resourceItems.value[0];
  if (props.resource === 'technologies' && current) {
    void openEdit(current);
    return;
  }
  activeId.value = null;
  originalStatus.value = 'draft';
  Object.assign(form, emptyForm());
  resetAdvancedFields();
  dialogVisible.value = true;
}

async function openEdit(value: unknown) {
  const item = value as ContentItem;
  activeId.value = item.id;
  loading.value = true;
  try {
    const detail = await getContent(props.resource, item.id);
    const names = new Map(
      referenceOptions.value.map((option) => [option.id, option.name]),
    );
    const mapped = contentFromBackend(props.resource, detail, names);
    originalStatus.value = mapped.status;
    Object.assign(form, structuredClone(toRaw(mapped)));
    loadAdvancedFields(mapped);
    dialogVisible.value = true;
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (
    !form.title.trim() ||
    (!['banners', 'technologies'].includes(props.resource) && !form.slug.trim())
  ) {
    ElMessage.warning(
      props.resource === 'banners' || props.resource === 'technologies'
        ? '请填写标题'
        : '请填写标题和 URL slug',
    );
    return;
  }
  const duplicate = resourceItems.value.some(
    (item) => item.slug === form.slug.trim() && item.id !== activeId.value,
  );
  if (!['banners', 'technologies'].includes(props.resource) && duplicate) {
    ElMessage.warning('该 slug 已存在，请更换');
    return;
  }

  applyReference();
  if (!applyAdvancedFields()) return;
  if (
    ['articles', 'cases', 'products'].includes(props.resource) &&
    !form.categoryId
  ) {
    ElMessage.warning('请先创建并选择有效分类');
    return;
  }

  saving.value = true;
  try {
    const saved = await saveContent(
      props.resource,
      activeId.value,
      contentPayload(props.resource, form),
    );
    const savedId = Number(saved.id || activeId.value || 0);
    if (
      savedId &&
      form.status !== originalStatus.value &&
      form.status !== 'draft'
    ) {
      await changeContentStatus(
        props.resource,
        savedId,
        form.status === 'published' ? 'published' : 'offline',
      );
    }
    dialogVisible.value = false;
    await load();
    ElMessage.success(activeId.value ? '内容已更新' : '内容已创建');
  } finally {
    saving.value = false;
  }
}

async function changeStatus(value: unknown, next: ContentStatus) {
  const item = value as ContentItem;
  await changeContentStatus(
    props.resource,
    item.id,
    next === 'published' ? 'published' : 'offline',
  );
  await load();
  ElMessage.success(next === 'published' ? '内容已发布' : '内容已下线');
}

async function remove(value: unknown) {
  const item = value as ContentItem;
  await deleteContent(props.resource, item.id);
  await load();
  ElMessage.success('内容已删除');
}

function statusType(value: ContentStatus) {
  if (value === 'published') return 'success';
  if (value === 'draft') return 'warning';
  return 'info';
}

function statusLabel(value: ContentStatus) {
  if (value === 'published') return '已发布';
  if (value === 'draft') return '草稿';
  return '已下线';
}

function homePlacementLabel(value: unknown) {
  const item = value as ContentItem;
  if (props.resource === 'articles') {
    if (item.homePinned) return '主置顶';
    if (item.showOnHome) return '首页新闻';
    return '';
  }
  return item.featured ? '推荐' : '';
}
</script>

<template>
  <div class="cms-page">
    <section class="cms-hero">
      <div>
        <p class="cms-eyebrow">CONTENT CENTER</p>
        <h1>{{ meta.label }}管理</h1>
        <p>{{ meta.description }}</p>
      </div>
      <ElButton
        :loading="loading"
        class="brand-button"
        size="large"
        type="primary"
        @click="openCreate"
      >
        {{
          props.resource === 'technologies' ? '编辑技术页' : `新建${meta.label}`
        }}
      </ElButton>
    </section>

    <ElRow :gutter="16" class="stats-grid">
      <ElCol :lg="6" :sm="12" :xs="24">
        <ElCard shadow="never">
          <span>全部内容</span><strong>{{ stats.total }}</strong>
        </ElCard>
      </ElCol>
      <ElCol :lg="6" :sm="12" :xs="24">
        <ElCard shadow="never">
          <span>已发布</span><strong>{{ stats.published }}</strong>
        </ElCard>
      </ElCol>
      <ElCol :lg="6" :sm="12" :xs="24">
        <ElCard shadow="never">
          <span>草稿</span><strong>{{ stats.draft }}</strong>
        </ElCard>
      </ElCol>
      <ElCol :lg="6" :sm="12" :xs="24">
        <ElCard shadow="never">
          <span>{{ isHomePlacementResource ? '首页展示' : '首页推荐' }}</span><strong>{{ stats.featured }}</strong>
        </ElCard>
      </ElCol>
    </ElRow>

    <ElCard class="content-card" shadow="never">
      <div class="filter-row">
        <ElInput
          v-model="keyword"
          clearable
          placeholder="搜索标题、slug 或摘要"
        />
        <ElSelect v-model="category" aria-label="按分类筛选">
          <ElOption label="全部分类" value="all" />
          <ElOption
            v-for="item in categoryOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </ElSelect>
        <ElSelect v-model="status" aria-label="按状态筛选">
          <ElOption label="全部状态" value="all" />
          <ElOption label="已发布" value="published" />
          <ElOption label="草稿" value="draft" />
          <ElOption label="已下线" value="offline" />
        </ElSelect>
      </div>

      <ElTable :data="filteredItems" row-key="id" v-loading="loading">
        <ElTableColumn label="内容" min-width="320">
          <template #default="{ row }">
            <div class="content-cell">
              <ElImage
                v-if="row.cover"
                :preview-src-list="[previewUrl(row.cover)]"
                :src="previewUrl(row.cover)"
                fit="cover"
              />
              <div v-else class="cover-placeholder">
                {{ row.title.slice(0, 1) }}
              </div>
              <div>
                <strong>{{ row.title }}</strong><small>/{{ row.slug }}</small>
              </div>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="分类" min-width="120" prop="category" />
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }">
            <ElTag :type="statusType(row.status)">
              {{ statusLabel(row.status) }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn
          :label="isHomePlacementResource ? '首页位置' : '推荐'"
          width="90"
        >
          <template #default="{ row }">
            <ElTag v-if="homePlacementLabel(row)" effect="plain" type="danger">
              {{ homePlacementLabel(row) }}
</ElTag><span v-else>-</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="排序" prop="sortOrder" width="80" />
        <ElTableColumn label="更新时间" min-width="150" prop="updatedAt" />
        <ElTableColumn fixed="right" label="操作" width="230">
          <template #default="{ row }">
            <ElButton link type="primary" @click="openEdit(row)">编辑</ElButton>
            <ElButton
              v-if="row.status !== 'published'"
              link
              type="success"
              @click="changeStatus(row, 'published')"
            >
              发布
            </ElButton>
            <ElButton v-else link @click="changeStatus(row, 'offline')">
              下线
            </ElButton>
            <ElPopconfirm
              v-if="supportsDelete"
              title="内容将移入回收站，确定继续？"
              @confirm="remove(row)"
            >
              <template #reference>
                <ElButton link type="danger">删除</ElButton>
              </template>
            </ElPopconfirm>
          </template>
        </ElTableColumn>
      </ElTable>
    </ElCard>

    <ElDialog
      v-model="dialogVisible"
      :title="activeId ? `编辑${meta.label}` : `新建${meta.label}`"
      destroy-on-close
      width="760px"
    >
      <ElForm :model="form" label-position="top">
        <ElRow :gutter="18">
          <ElCol :md="16" :xs="24">
            <ElFormItem label="标题" required>
              <ElInput v-model="form.title" maxlength="80" show-word-limit />
            </ElFormItem>
          </ElCol>
          <ElCol :md="8" :xs="24">
            <ElFormItem label="分类">
              <ElSelect
                v-if="props.resource === 'articles'"
                v-model="form.categoryIds"
                multiple
                placeholder="至少选择一个文章分类"
                style="width: 100%"
              >
                <ElOption
                  v-for="item in referenceOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </ElSelect>
              <ElSelect v-else v-model="form.category" style="width: 100%">
                <ElOption
                  v-for="item in categoryOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </ElSelect>
            </ElFormItem>
          </ElCol>
          <ElCol :md="16" :xs="24">
            <ElFormItem label="URL slug" required>
              <ElInput v-model="form.slug" placeholder="例如 aerogel-powder" />
            </ElFormItem>
          </ElCol>
          <ElCol :md="8" :xs="24">
            <ElFormItem label="排序">
              <ElInputNumber
                v-model="form.sortOrder"
                :min="0"
                style="width: 100%"
              />
            </ElFormItem>
          </ElCol>
          <template v-if="props.resource === 'banners'">
            <ElCol :md="12" :xs="24">
              <ElFormItem label="栏目小标题">
                <ElInput
                  v-model="form.eyebrow"
                  maxlength="30"
                  placeholder="例如 技术发布 · 成果转化"
                  show-word-limit
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="12" :xs="24">
              <ElFormItem label="红色强调标题">
                <ElInput
                  v-model="form.highlightTitle"
                  maxlength="40"
                  show-word-limit
                />
              </ElFormItem>
            </ElCol>
          </template>
          <ElCol :md="props.resource === 'banners' ? 12 : 24" :xs="24">
            <ElFormItem
              :label="
                props.resource === 'banners' ? 'PC 端背景图片' : '封面图片'
              "
            >
              <ElSelect
                v-model="form.cover"
                allow-create
                clearable
                filterable
                placeholder="从素材库选择或粘贴图片地址"
                style="width: 100%"
              >
                <ElOption
                  v-for="asset in imageOptions"
                  :key="asset.id"
                  :label="asset.name"
                  :value="asset.sourceUrl || asset.url"
                />
              </ElSelect>
            </ElFormItem>
          </ElCol>
          <ElCol v-if="props.resource === 'banners'" :md="12" :xs="24">
            <ElFormItem label="手机端背景图片">
              <ElSelect
                v-model="form.mobileCover"
                allow-create
                clearable
                filterable
                placeholder="不设置时使用 PC 端图片"
                style="width: 100%"
              >
                <ElOption
                  v-for="asset in imageOptions"
                  :key="asset.id"
                  :label="asset.name"
                  :value="asset.sourceUrl || asset.url"
                />
              </ElSelect>
            </ElFormItem>
          </ElCol>
          <ElCol v-if="form.cover" :span="24">
            <ElFormItem label="图片预览">
              <ElImage
                :preview-src-list="[previewUrl(form.cover)]"
                :src="previewUrl(form.cover)"
                fit="cover"
                style="width: 220px; height: 120px; border-radius: 10px"
              />
            </ElFormItem>
          </ElCol>
          <ElCol :span="24">
            <ElFormItem label="摘要">
              <ElInput
                v-model="form.summary"
                :rows="3"
                maxlength="240"
                show-word-limit
                type="textarea"
              />
            </ElFormItem>
          </ElCol>
          <template v-if="props.resource === 'products'">
            <ElCol :span="24">
              <ElFormItem label="产品特点（每行或逗号分隔一项）">
                <ElInput v-model="featuresText" :rows="4" type="textarea" />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="产品参数 JSON">
                <ElInput
                  v-model="specificationsText"
                  :rows="6"
                  type="textarea"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="关联应用场景">
                <ElSelect
                  v-model="rawForm.sceneIds"
                  clearable
                  multiple
                  style="width: 100%"
                >
                  <ElOption
                    v-for="item in relatedOptions"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </ElSelect>
              </ElFormItem>
            </ElCol>
          </template>
          <template v-if="props.resource === 'scenes'">
            <ElCol :span="24">
              <ElFormItem label="场景口号">
                <ElInput v-model="rawForm.slogan" />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="行业痛点">
                <ElInput
                  v-model="rawForm.painPoint"
                  :rows="4"
                  type="textarea"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="解决方案">
                <ElInput v-model="rawForm.solution" :rows="4" type="textarea" />
              </ElFormItem>
            </ElCol>
          </template>
          <template v-if="props.resource === 'cases'">
            <ElCol :md="12" :xs="24">
              <ElFormItem label="客户名称">
                <ElInput v-model="rawForm.clientName" />
              </ElFormItem>
            </ElCol>
            <ElCol :md="12" :xs="24">
              <ElFormItem label="项目地点">
                <ElInput v-model="rawForm.location" />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="项目背景">
                <ElInput
                  v-model="rawForm.background"
                  :rows="3"
                  type="textarea"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="客户需求">
                <ElInput
                  v-model="rawForm.customerNeed"
                  :rows="3"
                  type="textarea"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="解决方案">
                <ElInput v-model="rawForm.solution" :rows="3" type="textarea" />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="实施过程">
                <ElInput
                  v-model="rawForm.implementation"
                  :rows="3"
                  type="textarea"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="成果总结">
                <ElInput
                  v-model="rawForm.resultSummary"
                  :rows="3"
                  type="textarea"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="关联产品">
                <ElSelect
                  v-model="rawForm.productIds"
                  clearable
                  multiple
                  style="width: 100%"
                >
                  <ElOption
                    v-for="item in relatedOptions"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </ElSelect>
              </ElFormItem>
            </ElCol>
          </template>
          <template v-if="props.resource === 'articles'">
            <ElCol :md="12" :xs="24">
              <ElFormItem label="作者">
                <ElInput v-model="rawForm.authorName" />
              </ElFormItem>
            </ElCol>
            <ElCol :md="12" :xs="24">
              <ElFormItem label="来源链接">
                <ElInput v-model="rawForm.sourceUrl" />
              </ElFormItem>
            </ElCol>
          </template>
          <ElCol v-if="props.resource === 'partners'" :span="24">
            <ElFormItem label="合作伙伴网站">
              <ElInput v-model="rawForm.websiteUrl" />
            </ElFormItem>
          </ElCol>
          <template v-if="props.resource === 'technologies'">
            <ElCol :span="24">
              <ElFormItem label="能力表格 JSON">
                <ElInput
                  v-model="capabilityRowsText"
                  :rows="6"
                  type="textarea"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="技术支柱 JSON">
                <ElInput v-model="pillarsText" :rows="6" type="textarea" />
              </ElFormItem>
            </ElCol>
            <ElCol :md="8" :xs="24">
              <ElFormItem label="按钮文字">
                <ElInput v-model="form.primaryActionLabel" />
              </ElFormItem>
            </ElCol>
            <ElCol :md="16" :xs="24">
              <ElFormItem label="按钮链接">
                <ElInput v-model="form.primaryActionLink" />
              </ElFormItem>
            </ElCol>
          </template>
          <ElCol
            v-if="!['banners', 'partners'].includes(props.resource)"
            :span="24"
          >
            <ElFormItem label="正文 HTML">
              <ElInput
                v-model="form.contentHtml"
                :rows="8"
                placeholder="填写正文 HTML；后端保存时会进行安全清洗"
                type="textarea"
              />
            </ElFormItem>
          </ElCol>
          <template v-if="props.resource === 'banners'">
            <ElCol :md="8" :xs="24">
              <ElFormItem label="主按钮文字">
                <ElInput
                  v-model="form.primaryActionLabel"
                  placeholder="查看应用案例"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="16" :xs="24">
              <ElFormItem label="主按钮链接">
                <ElInput
                  v-model="form.primaryActionLink"
                  placeholder="/applications"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="8" :xs="24">
              <ElFormItem label="次按钮文字">
                <ElInput
                  v-model="form.secondaryActionLabel"
                  placeholder="获取项目方案"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="16" :xs="24">
              <ElFormItem label="次按钮链接">
                <ElInput
                  v-model="form.secondaryActionLink"
                  placeholder="/cooperation"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="12" :xs="24">
              <ElFormItem label="开始展示时间">
                <ElInput
                  v-model="rawForm.startsAt"
                  placeholder="2026-07-22T08:00:00"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="12" :xs="24">
              <ElFormItem label="结束展示时间">
                <ElInput
                  v-model="rawForm.endsAt"
                  placeholder="2026-08-22T08:00:00"
                />
              </ElFormItem>
            </ElCol>
          </template>
          <ElCol :md="8" :xs="24">
            <ElFormItem label="状态">
              <ElSelect v-model="form.status" style="width: 100%">
                <ElOption label="草稿" value="draft" /><ElOption
                  label="已发布"
                  value="published"
                /><ElOption label="已下线" value="offline" />
              </ElSelect>
            </ElFormItem>
          </ElCol>
          <template v-if="isHomePlacementResource">
            <ElCol :md="8" :xs="24">
              <ElFormItem label="首页展示">
                <ElSwitch
                  v-model="form.showOnHome"
                  active-text="显示"
                  inactive-text="隐藏"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="8" :xs="24">
              <ElFormItem
                :label="
                  props.resource === 'articles' ? '首页主置顶' : '首页置顶'
                "
              >
                <ElSwitch
                  v-model="form.homePinned"
                  active-text="置顶"
                  inactive-text="普通"
                />
              </ElFormItem>
            </ElCol>
          </template>
          <ElCol v-else :md="8" :xs="24">
            <ElFormItem label="首页推荐">
              <ElSwitch
                v-model="form.featured"
                active-text="推荐"
                inactive-text="普通"
              />
            </ElFormItem>
          </ElCol>
        </ElRow>
        <ElDivider content-position="left">SEO 设置</ElDivider>
        <ElFormItem label="SEO 标题">
          <ElInput v-model="form.seoTitle" maxlength="70" show-word-limit />
        </ElFormItem>
        <ElFormItem label="SEO 描述">
          <ElInput
            v-model="form.seoDescription"
            :rows="2"
            maxlength="160"
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
.cms-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7f8;
}

.cms-hero {
  display: flex;
  gap: 24px;
  align-items: flex-end;
  justify-content: space-between;
  padding: 28px 30px;
  color: #fff;
  background: linear-gradient(120deg, #092c36, #0e5960 65%, #1b7780);
  border-radius: 18px;
  box-shadow: 0 18px 50px rgb(9 44 54 / 16%);
}

.cms-hero h1 {
  margin: 4px 0 8px;
  font-size: 28px;
  font-weight: 650;
  letter-spacing: -0.02em;
}

.cms-hero p:last-child {
  max-width: 720px;
  margin: 0;
  color: rgb(255 255 255 / 72%);
}

.cms-eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  color: #79d2c8 !important;
  letter-spacing: 0.18em;
}

.brand-button {
  --el-button-bg-color: #e65b43;
  --el-button-border-color: #e65b43;
  --el-button-hover-bg-color: #f16a52;
  --el-button-hover-border-color: #f16a52;
}

.stats-grid {
  margin-top: 16px;
}

.stats-grid :deep(.el-card) {
  margin-bottom: 16px;
  border: 1px solid #e6ebed;
  border-radius: 14px;
}

.stats-grid :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
}

.stats-grid span {
  font-size: 13px;
  color: #708087;
}

.stats-grid strong {
  font-size: 26px;
  color: #14343b;
}

.content-card {
  border: 1px solid #e6ebed;
  border-radius: 16px;
}

.filter-row {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 180px 160px;
  gap: 12px;
  margin-bottom: 18px;
}

.content-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.content-cell :deep(.el-image),
.cover-placeholder {
  flex: 0 0 58px;
  width: 58px;
  height: 44px;
  overflow: hidden;
  border-radius: 8px;
}

.cover-placeholder {
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #0e5960;
  background: #dcefed;
}

.content-cell strong,
.content-cell small {
  display: block;
}

.content-cell strong {
  color: #18343b;
}

.content-cell small {
  margin-top: 4px;
  color: #8a979b;
}

@media (max-width: 760px) {
  .cms-page {
    padding: 14px;
  }

  .cms-hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 22px;
  }

  .filter-row {
    grid-template-columns: 1fr;
  }
}
</style>
