<script lang="ts" setup>
import type { ContentItem, ContentResource, ContentStatus } from '#/data/cms';

import { computed, reactive, ref, watch } from 'vue';

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

import { cmsState, nextId, resourceMeta } from '#/data/cms';

const props = defineProps<{ resource: ContentResource }>();

const keyword = ref('');
const status = ref<'all' | ContentStatus>('all');
const category = ref('all');
const dialogVisible = ref(false);
const activeId = ref<null | number>(null);

const meta = computed(() => resourceMeta[props.resource]);
const resourceItems = computed(() =>
  cmsState.content.filter((item) => item.resource === props.resource),
);
const filteredItems = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return resourceItems.value
    .filter((item) => status.value === 'all' || item.status === status.value)
    .filter((item) => category.value === 'all' || item.category === category.value)
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
  featured: resourceItems.value.filter((item) => item.featured).length,
  published: resourceItems.value.filter((item) => item.status === 'published').length,
  total: resourceItems.value.length,
}));

const emptyForm = (): ContentItem => ({
  category: meta.value.categories[0] || '未分类',
  cover: '',
  createdAt: '',
  featured: false,
  id: 0,
  resource: props.resource,
  seoDescription: '',
  seoKeywords: '奥飞新材,气凝胶',
  seoTitle: '',
  slug: '',
  sortOrder: resourceItems.value.length + 1,
  status: 'draft',
  summary: '',
  title: '',
  updatedAt: '',
});

const form = reactive<ContentItem>(emptyForm());

watch(
  () => props.resource,
  () => {
    keyword.value = '';
    status.value = 'all';
    category.value = 'all';
  },
);

function formatNow() {
  return new Intl.DateTimeFormat('zh-CN', {
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
    .format(new Date())
    .replaceAll('/', '-');
}

function openCreate() {
  activeId.value = null;
  Object.assign(form, emptyForm());
  dialogVisible.value = true;
}

function openEdit(value: unknown) {
  const item = value as ContentItem;
  activeId.value = item.id;
  Object.assign(form, JSON.parse(JSON.stringify(item)));
  dialogVisible.value = true;
}

function save() {
  if (!form.title.trim() || !form.slug.trim()) {
    ElMessage.warning('请填写标题和 URL slug');
    return;
  }
  const duplicate = resourceItems.value.some(
    (item) => item.slug === form.slug.trim() && item.id !== activeId.value,
  );
  if (duplicate) {
    ElMessage.warning('该 slug 已存在，请更换');
    return;
  }

  const time = formatNow();
  if (activeId.value) {
    const current = cmsState.content.find((item) => item.id === activeId.value);
    if (current) Object.assign(current, form, { updatedAt: time });
  } else {
    cmsState.content.push({
      ...JSON.parse(JSON.stringify(form)),
      createdAt: time,
      id: nextId(cmsState.content),
      resource: props.resource,
      updatedAt: time,
    });
  }
  dialogVisible.value = false;
  ElMessage.success(activeId.value ? '内容已更新' : '内容已创建');
}

function changeStatus(value: unknown, next: ContentStatus) {
  const item = value as ContentItem;
  item.status = next;
  item.updatedAt = formatNow();
  ElMessage.success(next === 'published' ? '已发布到官网数据源' : '状态已更新');
}

function remove(value: unknown) {
  const item = value as ContentItem;
  const index = cmsState.content.findIndex((candidate) => candidate.id === item.id);
  if (index >= 0) cmsState.content.splice(index, 1);
  ElMessage.success('已移入回收站');
}

function statusType(value: ContentStatus) {
  return value === 'published' ? 'success' : value === 'draft' ? 'warning' : 'info';
}

function statusLabel(value: ContentStatus) {
  return value === 'published' ? '已发布' : value === 'draft' ? '草稿' : '已下线';
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
      <ElButton class="brand-button" size="large" type="primary" @click="openCreate">
        新建{{ meta.label }}
      </ElButton>
    </section>

    <ElRow :gutter="16" class="stats-grid">
      <ElCol :lg="6" :sm="12" :xs="24"><ElCard shadow="never"><span>全部内容</span><strong>{{ stats.total }}</strong></ElCard></ElCol>
      <ElCol :lg="6" :sm="12" :xs="24"><ElCard shadow="never"><span>已发布</span><strong>{{ stats.published }}</strong></ElCard></ElCol>
      <ElCol :lg="6" :sm="12" :xs="24"><ElCard shadow="never"><span>草稿</span><strong>{{ stats.draft }}</strong></ElCard></ElCol>
      <ElCol :lg="6" :sm="12" :xs="24"><ElCard shadow="never"><span>首页推荐</span><strong>{{ stats.featured }}</strong></ElCard></ElCol>
    </ElRow>

    <ElCard class="content-card" shadow="never">
      <div class="filter-row">
        <ElInput v-model="keyword" clearable placeholder="搜索标题、slug 或摘要" />
        <ElSelect v-model="category" aria-label="按分类筛选">
          <ElOption label="全部分类" value="all" />
          <ElOption v-for="item in meta.categories" :key="item" :label="item" :value="item" />
        </ElSelect>
        <ElSelect v-model="status" aria-label="按状态筛选">
          <ElOption label="全部状态" value="all" />
          <ElOption label="已发布" value="published" />
          <ElOption label="草稿" value="draft" />
          <ElOption label="已下线" value="offline" />
        </ElSelect>
      </div>

      <ElTable :data="filteredItems" row-key="id">
        <ElTableColumn label="内容" min-width="320">
          <template #default="{ row }">
            <div class="content-cell">
              <ElImage v-if="row.cover" :preview-src-list="[row.cover]" :src="row.cover" fit="cover" />
              <div class="cover-placeholder" v-else>{{ row.title.slice(0, 1) }}</div>
              <div><strong>{{ row.title }}</strong><small>/{{ row.slug }}</small></div>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="分类" min-width="120" prop="category" />
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }"><ElTag :type="statusType(row.status)">{{ statusLabel(row.status) }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn label="推荐" width="80">
          <template #default="{ row }"><ElTag v-if="row.featured" effect="plain" type="danger">置顶</ElTag><span v-else>-</span></template>
        </ElTableColumn>
        <ElTableColumn label="排序" prop="sortOrder" width="80" />
        <ElTableColumn label="更新时间" min-width="150" prop="updatedAt" />
        <ElTableColumn fixed="right" label="操作" width="230">
          <template #default="{ row }">
            <ElButton link type="primary" @click="openEdit(row)">编辑</ElButton>
            <ElButton v-if="row.status !== 'published'" link type="success" @click="changeStatus(row, 'published')">发布</ElButton>
            <ElButton v-else link @click="changeStatus(row, 'offline')">下线</ElButton>
            <ElPopconfirm title="内容将移入回收站，确定继续？" @confirm="remove(row)">
              <template #reference><ElButton link type="danger">删除</ElButton></template>
            </ElPopconfirm>
          </template>
        </ElTableColumn>
      </ElTable>
    </ElCard>

    <ElDialog v-model="dialogVisible" :title="activeId ? `编辑${meta.label}` : `新建${meta.label}`" destroy-on-close width="760px">
      <ElForm :model="form" label-position="top">
        <ElRow :gutter="18">
          <ElCol :md="16" :xs="24"><ElFormItem label="标题" required><ElInput v-model="form.title" maxlength="80" show-word-limit /></ElFormItem></ElCol>
          <ElCol :md="8" :xs="24"><ElFormItem label="分类"><ElSelect v-model="form.category" style="width: 100%"><ElOption v-for="item in meta.categories" :key="item" :label="item" :value="item" /></ElSelect></ElFormItem></ElCol>
          <ElCol :md="16" :xs="24"><ElFormItem label="URL slug" required><ElInput v-model="form.slug" placeholder="例如 aerogel-powder" /></ElFormItem></ElCol>
          <ElCol :md="8" :xs="24"><ElFormItem label="排序"><ElInputNumber v-model="form.sortOrder" :min="0" style="width: 100%" /></ElFormItem></ElCol>
          <ElCol :span="24"><ElFormItem label="封面图片地址"><ElInput v-model="form.cover" placeholder="/images/example.jpg 或完整 URL" /></ElFormItem></ElCol>
          <ElCol :span="24"><ElFormItem label="摘要"><ElInput v-model="form.summary" :rows="3" maxlength="240" show-word-limit type="textarea" /></ElFormItem></ElCol>
          <ElCol :md="8" :xs="24"><ElFormItem label="状态"><ElSelect v-model="form.status" style="width: 100%"><ElOption label="草稿" value="draft" /><ElOption label="已发布" value="published" /><ElOption label="已下线" value="offline" /></ElSelect></ElFormItem></ElCol>
          <ElCol :md="8" :xs="24"><ElFormItem label="首页推荐"><ElSwitch v-model="form.featured" active-text="推荐" inactive-text="普通" /></ElFormItem></ElCol>
        </ElRow>
        <ElDivider content-position="left">SEO 设置</ElDivider>
        <ElFormItem label="SEO 标题"><ElInput v-model="form.seoTitle" maxlength="70" show-word-limit /></ElFormItem>
        <ElFormItem label="SEO 描述"><ElInput v-model="form.seoDescription" :rows="2" maxlength="160" show-word-limit type="textarea" /></ElFormItem>
        <ElFormItem label="SEO 关键词"><ElInput v-model="form.seoKeywords" placeholder="使用英文逗号分隔" /></ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="save">保存内容</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.cms-page { min-height: 100%; padding: 24px; background: #f5f7f8; }
.cms-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; padding: 28px 30px; border-radius: 18px; color: #fff; background: linear-gradient(120deg, #092c36, #0e5960 65%, #1b7780); box-shadow: 0 18px 50px rgb(9 44 54 / 16%); }
.cms-hero h1 { margin: 4px 0 8px; font-size: 28px; font-weight: 650; letter-spacing: -.02em; }
.cms-hero p:last-child { max-width: 720px; margin: 0; color: rgb(255 255 255 / 72%); }
.cms-eyebrow { margin: 0; color: #79d2c8 !important; font-size: 11px; font-weight: 700; letter-spacing: .18em; }
.brand-button { --el-button-bg-color: #e65b43; --el-button-border-color: #e65b43; --el-button-hover-bg-color: #f16a52; --el-button-hover-border-color: #f16a52; }
.stats-grid { margin-top: 16px; }
.stats-grid :deep(.el-card) { margin-bottom: 16px; border: 1px solid #e6ebed; border-radius: 14px; }
.stats-grid :deep(.el-card__body) { display: flex; align-items: center; justify-content: space-between; padding: 18px 20px; }
.stats-grid span { color: #708087; font-size: 13px; }
.stats-grid strong { color: #14343b; font-size: 26px; }
.content-card { border: 1px solid #e6ebed; border-radius: 16px; }
.filter-row { display: grid; grid-template-columns: minmax(240px, 1fr) 180px 160px; gap: 12px; margin-bottom: 18px; }
.content-cell { display: flex; align-items: center; gap: 12px; }
.content-cell :deep(.el-image), .cover-placeholder { width: 58px; height: 44px; flex: 0 0 58px; border-radius: 8px; overflow: hidden; }
.cover-placeholder { display: grid; place-items: center; color: #0e5960; background: #dcefed; font-weight: 700; }
.content-cell strong, .content-cell small { display: block; }
.content-cell strong { color: #18343b; }
.content-cell small { margin-top: 4px; color: #8a979b; }
@media (max-width: 760px) {
  .cms-page { padding: 14px; }
  .cms-hero { align-items: flex-start; flex-direction: column; padding: 22px; }
  .filter-row { grid-template-columns: 1fr; }
}
</style>
