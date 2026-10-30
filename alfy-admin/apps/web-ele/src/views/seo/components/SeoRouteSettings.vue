<script lang="ts" setup>
import type { SeoRouteSettingRecord } from '#/api';

import { computed, onMounted, reactive, ref } from 'vue';

import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import { listSeoRouteSettings, saveSeoRouteSetting } from '#/api';

const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const rows = ref<SeoRouteSettingRecord[]>([]);
const form = reactive({
  displayName: '',
  indexEnabled: true,
  ogMediaId: undefined as number | undefined,
  routeKey: '',
  routePath: '',
  seoDescription: '',
  seoKeywords: '',
  seoTitle: '',
  version: 0,
});

const completedCount = computed(
  () =>
    rows.value.filter((item) => item.seoTitle && item.seoDescription).length,
);

async function load() {
  loading.value = true;
  try {
    rows.value = await listSeoRouteSettings();
  } finally {
    loading.value = false;
  }
}

function edit(value: unknown) {
  const row = value as SeoRouteSettingRecord;
  Object.assign(form, {
    ...row,
    ogMediaId: row.ogMediaId || undefined,
    seoDescription: row.seoDescription || '',
    seoKeywords: row.seoKeywords || '',
    seoTitle: row.seoTitle || '',
  });
  dialogVisible.value = true;
}

async function save() {
  saving.value = true;
  try {
    await saveSeoRouteSetting(form.routeKey, {
      indexEnabled: form.indexEnabled,
      ogMediaId: form.ogMediaId,
      seoDescription: form.seoDescription,
      seoKeywords: form.seoKeywords,
      seoTitle: form.seoTitle,
      version: form.version,
    });
    dialogVisible.value = false;
    await load();
    ElMessage.success(`${form.displayName} SEO 配置已保存`);
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="route-settings" v-loading="loading">
    <div class="tab-intro">
      <div>
        <strong>固定栏目页 SEO</strong>
        <span>首页、产品中心等固定路径由系统预置，只维护元数据，不允许在此修改
          URL。</span>
      </div>
      <ElTag type="success">
        {{ completedCount }}/{{ rows.length }} 已填写标题和描述
      </ElTag>
    </div>
    <ElTable :data="rows">
      <ElTableColumn label="页面" min-width="150" prop="displayName" />
      <ElTableColumn label="路径" min-width="150" prop="routePath" />
      <ElTableColumn
        label="SEO 标题"
        min-width="260"
        prop="seoTitle"
        show-overflow-tooltip
      />
      <ElTableColumn
        label="SEO 描述"
        min-width="320"
        prop="seoDescription"
        show-overflow-tooltip
      />
      <ElTableColumn label="索引" width="90">
        <template #default="{ row }">
          <ElTag :type="row.indexEnabled ? 'success' : 'danger'">
            {{ row.indexEnabled ? '允许' : '禁止' }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn fixed="right" label="操作" width="100">
        <template #default="{ row }">
          <ElButton link type="primary" @click="edit(row)">编辑</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <ElDialog
      v-model="dialogVisible"
      :close-on-click-modal="false"
      :title="`编辑 ${form.displayName} SEO`"
      width="680px"
    >
      <ElForm label-position="top">
        <ElFormItem label="固定路径">
          <ElInput :model-value="form.routePath" disabled />
        </ElFormItem>
        <ElFormItem label="SEO 标题">
          <ElInput v-model="form.seoTitle" maxlength="255" show-word-limit />
        </ElFormItem>
        <ElFormItem label="SEO 描述">
          <ElInput
            v-model="form.seoDescription"
            :rows="4"
            maxlength="500"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="SEO 关键词（辅助管理，不计入核心评分）">
          <ElInput v-model="form.seoKeywords" maxlength="500" show-word-limit />
        </ElFormItem>
        <ElFormItem label="Open Graph 图片媒体 ID">
          <ElInputNumber
            v-model="form.ogMediaId"
            :min="1"
            controls-position="right"
          />
        </ElFormItem>
        <ElFormItem label="允许索引">
          <ElSwitch v-model="form.indexEnabled" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton :loading="saving" type="primary" @click="save">
          保存页面配置
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.route-settings {
  padding-bottom: 20px;
}

.tab-intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 82px;
  padding: 12px 4px 20px;
}

.tab-intro strong,
.tab-intro span {
  display: block;
}

.tab-intro strong {
  font-size: 17px;
  color: #15363d;
}

.tab-intro span {
  margin-top: 5px;
  color: #819095;
}
</style>
