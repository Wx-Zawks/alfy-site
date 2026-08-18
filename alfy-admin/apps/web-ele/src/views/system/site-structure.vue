<script lang="ts" setup>
import type { NavigationRecord, PageHeroRecord } from '#/api';

import { computed, onMounted, reactive, ref } from 'vue';

import {
  ElButton,
  ElCard,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElPopconfirm,
  ElSelect,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag,
} from 'element-plus';

import {
  changePageHeroStatus,
  deleteNavigation,
  deletePageHero,
  listMedia,
  listNavigations,
  listPageHeroes,
  saveNavigation,
  savePageHero,
} from '#/api';
import { mediaIdFromUrl } from '#/data/cms-adapter';

const activeTab = ref('navigation');
const area = ref<'FOOTER' | 'HEADER'>('HEADER');
const loading = ref(false);
const saving = ref(false);
const navigationDialog = ref(false);
const heroDialog = ref(false);
const navigations = ref<NavigationRecord[]>([]);
const heroes = ref<PageHeroRecord[]>([]);
const mediaOptions = ref<Array<{ id: number; label: string; url: string }>>([]);

const navigationForm = reactive({
  enabled: true,
  highlighted: false,
  id: 0,
  label: '',
  parentId: undefined as number | undefined,
  sortOrder: 0,
  target: '',
  version: undefined as number | undefined,
});
const heroForm = reactive({
  backgroundImageUrl: '',
  eyebrow: '',
  highlightText: '',
  id: 0,
  mobileBackgroundImageUrl: '',
  pageKey: '',
  primaryActionLabel: '',
  primaryActionTarget: '',
  secondaryActionLabel: '',
  secondaryActionTarget: '',
  status: 'DRAFT',
  summary: '',
  title: '',
  version: undefined as number | undefined,
});

const parentOptions = computed(() =>
  navigations.value.filter((item) => item.id !== navigationForm.id),
);
const parentIdById = computed(() => {
  const values = new Map<number, number>();
  const visit = (rows: NavigationRecord[], parentId?: number) => {
    for (const row of rows) {
      if (parentId) values.set(row.id, parentId);
      visit(row.children || [], row.id);
    }
  };
  visit(navigations.value);
  return values;
});

async function load() {
  loading.value = true;
  try {
    const [navigationRows, heroRows, mediaRows] = await Promise.all([
      listNavigations(area.value),
      listPageHeroes(),
      listMedia(),
    ]);
    navigations.value = navigationRows;
    heroes.value = heroRows;
    mediaOptions.value = mediaRows
      .filter((item) => item.mediaType === 'IMAGE')
      .map((item) => ({
        id: item.id,
        label: item.originalFilename,
        url: item.adminUrl,
      }));
  } finally {
    loading.value = false;
  }
}

async function changeArea() {
  loading.value = true;
  try {
    navigations.value = await listNavigations(area.value);
  } finally {
    loading.value = false;
  }
}

function openNavigation(value?: unknown, parentId?: number) {
  const row = value as NavigationRecord | undefined;
  Object.assign(navigationForm, {
    enabled: row?.enabled ?? true,
    highlighted: row?.highlighted ?? false,
    id: row?.id ?? 0,
    label: row?.label ?? '',
    parentId: parentId ?? (row ? parentIdById.value.get(row.id) : undefined),
    sortOrder: row?.sortOrder ?? 0,
    target: row?.target ?? '',
    version: row?.version,
  });
  navigationDialog.value = true;
}

async function persistNavigation(
  row: NavigationRecord | typeof navigationForm = navigationForm,
  close = true,
) {
  const saved = await saveNavigation(row.id || null, {
    enabled: row.enabled,
    highlighted: row.highlighted,
    label: row.label,
    navigationArea: area.value,
    parentId:
      'parentId' in row
        ? row.parentId || null
        : parentIdById.value.get(row.id) || null,
    sortOrder: row.sortOrder,
    target: row.target,
    version: row.version,
  });
  if (close) navigationDialog.value = false;
  await changeArea();
  return saved;
}

async function submitNavigation() {
  if (!navigationForm.label.trim() || !navigationForm.target.trim()) {
    ElMessage.warning('请填写导航文字和链接');
    return;
  }
  saving.value = true;
  try {
    await persistNavigation();
    ElMessage.success('导航已保存');
  } finally {
    saving.value = false;
  }
}

async function toggleNavigation(value: unknown) {
  const row = value as NavigationRecord;
  await persistNavigation(row, false);
  ElMessage.success('导航状态已更新');
}

async function removeNavigation(id: number) {
  await deleteNavigation(id);
  await changeArea();
  ElMessage.success('导航已删除');
}

function openHero(value?: unknown) {
  const row = value as PageHeroRecord | undefined;
  Object.assign(heroForm, {
    backgroundImageUrl: row?.backgroundImageUrl || '',
    eyebrow: row?.eyebrow || '',
    highlightText: row?.highlightText || '',
    id: row?.id || 0,
    mobileBackgroundImageUrl: row?.mobileBackgroundImageUrl || '',
    pageKey: row?.pageKey || '',
    primaryActionLabel: row?.primaryAction?.label || '',
    primaryActionTarget: row?.primaryAction?.target || '',
    secondaryActionLabel: row?.secondaryAction?.label || '',
    secondaryActionTarget: row?.secondaryAction?.target || '',
    status: row?.status || 'DRAFT',
    summary: row?.summary || '',
    title: row?.title || '',
    version: row?.version,
  });
  heroDialog.value = true;
}

async function submitHero() {
  if (!heroForm.pageKey.trim() || !heroForm.title.trim()) {
    ElMessage.warning('请填写页面标识和标题');
    return;
  }
  saving.value = true;
  try {
    const saved = await savePageHero(heroForm.id || null, {
      backgroundMediaId: mediaIdFromUrl(heroForm.backgroundImageUrl) ?? null,
      eyebrow: heroForm.eyebrow || null,
      highlightText: heroForm.highlightText || null,
      mobileBackgroundMediaId:
        mediaIdFromUrl(heroForm.mobileBackgroundImageUrl) ?? null,
      pageKey: heroForm.pageKey,
      primaryActionLabel: heroForm.primaryActionLabel || null,
      primaryActionTarget: heroForm.primaryActionTarget || null,
      secondaryActionLabel: heroForm.secondaryActionLabel || null,
      secondaryActionTarget: heroForm.secondaryActionTarget || null,
      summary: heroForm.summary || null,
      title: heroForm.title,
      version: heroForm.version,
    });
    if (heroForm.status !== 'DRAFT') {
      await changePageHeroStatus(
        saved.id,
        heroForm.status === 'PUBLISHED' ? 'published' : 'offline',
      );
    }
    heroDialog.value = false;
    await load();
    ElMessage.success('页面首屏已保存');
  } finally {
    saving.value = false;
  }
}

async function setHeroStatus(value: unknown, status: 'offline' | 'published') {
  const row = value as PageHeroRecord;
  await changePageHeroStatus(row.id, status);
  await load();
  ElMessage.success(
    status === 'published' ? '页面首屏已发布' : '页面首屏已下线',
  );
}

async function removeHero(id: number) {
  await deletePageHero(id);
  await load();
  ElMessage.success('页面首屏已删除');
}

onMounted(load);
</script>

<template>
  <div class="structure-page">
    <section class="page-header">
      <div>
        <p>SITE STRUCTURE</p>
        <h1>站点结构</h1>
        <span>维护页头页脚导航，以及各频道页面的顶部首屏</span>
      </div>
    </section>

    <ElCard class="main-card" shadow="never" v-loading="loading">
      <ElTabs v-model="activeTab">
        <ElTabPane label="导航管理" name="navigation">
          <div class="toolbar">
            <ElSelect v-model="area" @change="changeArea">
              <ElOption label="页头导航" value="HEADER" />
              <ElOption label="页脚导航" value="FOOTER" />
            </ElSelect>
            <ElButton type="primary" @click="openNavigation()">
              新增导航
            </ElButton>
          </div>
          <ElTable
            :data="navigations"
            :tree-props="{ children: 'children' }"
            row-key="id"
          >
            <ElTableColumn label="显示文字" min-width="180" prop="label" />
            <ElTableColumn label="目标地址" min-width="260" prop="target" />
            <ElTableColumn label="排序" prop="sortOrder" width="90" />
            <ElTableColumn label="高亮" width="90">
              <template #default="{ row }">
                <ElTag v-if="row.highlighted" type="danger">高亮</ElTag>
                <span v-else>-</span>
              </template>
            </ElTableColumn>
            <ElTableColumn label="启用" width="90">
              <template #default="{ row }">
                <ElSwitch
                  v-model="row.enabled"
                  @change="toggleNavigation(row)"
                />
              </template>
            </ElTableColumn>
            <ElTableColumn fixed="right" label="操作" width="220">
              <template #default="{ row }">
                <ElButton link type="primary" @click="openNavigation(row)">
                  编辑
                </ElButton>
                <ElButton link @click="openNavigation(undefined, row.id)">
                  新增子项
                </ElButton>
                <ElPopconfirm
                  title="确定删除该导航？"
                  @confirm="removeNavigation(row.id)"
                >
                  <template #reference>
                    <ElButton link type="danger">删除</ElButton>
                  </template>
                </ElPopconfirm>
              </template>
            </ElTableColumn>
          </ElTable>
        </ElTabPane>

        <ElTabPane label="页面首屏" name="heroes">
          <div class="toolbar">
            <span>pageKey 与官网公开路径约定保持一致</span>
            <ElButton type="primary" @click="openHero()">新增页面首屏</ElButton>
          </div>
          <ElTable :data="heroes" row-key="id">
            <ElTableColumn label="pageKey" min-width="150" prop="pageKey" />
            <ElTableColumn label="标题" min-width="240" prop="title" />
            <ElTableColumn label="状态" width="100">
              <template #default="{ row }">
                <ElTag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
                  {{ row.status }}
                </ElTag>
              </template>
            </ElTableColumn>
            <ElTableColumn fixed="right" label="操作" width="240">
              <template #default="{ row }">
                <ElButton link type="primary" @click="openHero(row)">
                  编辑
                </ElButton>
                <ElButton
                  v-if="row.status !== 'PUBLISHED'"
                  link
                  type="success"
                  @click="setHeroStatus(row, 'published')"
                >
                  发布
                </ElButton>
                <ElButton v-else link @click="setHeroStatus(row, 'offline')">
                  下线
                </ElButton>
                <ElPopconfirm
                  title="确定删除该首屏？"
                  @confirm="removeHero(row.id)"
                >
                  <template #reference>
                    <ElButton link type="danger">删除</ElButton>
                  </template>
                </ElPopconfirm>
              </template>
            </ElTableColumn>
          </ElTable>
        </ElTabPane>
      </ElTabs>
    </ElCard>

    <ElDialog
      v-model="navigationDialog"
      :close-on-click-modal="false"
      :title="navigationForm.id ? '编辑导航' : '新增导航'"
      width="600px"
    >
      <ElForm :model="navigationForm" label-position="top">
        <ElFormItem label="显示文字" required>
          <ElInput v-model="navigationForm.label" />
        </ElFormItem>
        <ElFormItem label="目标地址" required>
          <ElInput
            v-model="navigationForm.target"
            placeholder="/products 或完整网址"
          />
        </ElFormItem>
        <ElFormItem label="父级导航">
          <ElSelect
            v-model="navigationForm.parentId"
            clearable
            style="width: 100%"
          >
            <ElOption
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="排序">
          <ElInputNumber v-model="navigationForm.sortOrder" :min="0" />
        </ElFormItem>
        <ElFormItem label="显示设置">
          <ElSwitch
            v-model="navigationForm.enabled"
            active-text="启用"
          /><ElSwitch v-model="navigationForm.highlighted" active-text="高亮" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="navigationDialog = false">取消</ElButton><ElButton :loading="saving" type="primary" @click="submitNavigation">
          保存导航
        </ElButton>
      </template>
    </ElDialog>

    <ElDialog
      v-model="heroDialog"
      :close-on-click-modal="false"
      :title="heroForm.id ? '编辑页面首屏' : '新增页面首屏'"
      width="760px"
    >
      <ElForm :model="heroForm" label-position="top">
        <ElFormItem label="页面标识 pageKey" required>
          <ElInput v-model="heroForm.pageKey" placeholder="例如 about-us" />
        </ElFormItem>
        <ElFormItem label="栏目小标题">
          <ElInput v-model="heroForm.eyebrow" />
        </ElFormItem>
        <ElFormItem label="主标题" required>
          <ElInput v-model="heroForm.title" />
        </ElFormItem>
        <ElFormItem label="强调文字">
          <ElInput v-model="heroForm.highlightText" />
        </ElFormItem>
        <ElFormItem label="摘要">
          <ElInput v-model="heroForm.summary" :rows="3" type="textarea" />
        </ElFormItem>
        <ElFormItem label="桌面背景图">
          <ElSelect
            v-model="heroForm.backgroundImageUrl"
            clearable
            filterable
            style="width: 100%"
          >
            <ElOption
              v-for="item in mediaOptions"
              :key="item.id"
              :label="item.label"
              :value="item.url"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="手机背景图">
          <ElSelect
            v-model="heroForm.mobileBackgroundImageUrl"
            clearable
            filterable
            style="width: 100%"
          >
            <ElOption
              v-for="item in mediaOptions"
              :key="item.id"
              :label="item.label"
              :value="item.url"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="主按钮">
          <ElInput
            v-model="heroForm.primaryActionLabel"
            placeholder="按钮文字"
          /><ElInput
            v-model="heroForm.primaryActionTarget"
            placeholder="按钮链接"
          />
        </ElFormItem>
        <ElFormItem label="次按钮">
          <ElInput
            v-model="heroForm.secondaryActionLabel"
            placeholder="按钮文字"
          /><ElInput
            v-model="heroForm.secondaryActionTarget"
            placeholder="按钮链接"
          />
        </ElFormItem>
        <ElFormItem label="状态">
          <ElSelect v-model="heroForm.status">
            <ElOption label="草稿" value="DRAFT" /><ElOption
              label="发布"
              value="PUBLISHED"
            /><ElOption label="下线" value="OFFLINE" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="heroDialog = false">取消</ElButton><ElButton :loading="saving" type="primary" @click="submitHero">
          保存首屏
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.structure-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7f8;
}

.page-header {
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

.main-card {
  margin-top: 16px;
  border-radius: 16px;
}

.toolbar {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.toolbar > span {
  color: #7d8c91;
}

.el-form-item .el-switch + .el-switch {
  margin-left: 22px;
}

.el-form-item .el-input + .el-input {
  margin-top: 10px;
}

@media (max-width: 760px) {
  .structure-page {
    padding: 14px;
  }

  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
