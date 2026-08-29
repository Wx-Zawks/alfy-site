<script lang="ts" setup>
import type { CaseCategoryRecord } from '#/api';

import { onMounted, reactive, ref } from 'vue';

import {
  ElButton,
  ElCard,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElPopconfirm,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import {
  deleteCaseCategory,
  listCaseCategories,
  saveCaseCategory,
} from '#/api';

const rows = ref<CaseCategoryRecord[]>([]);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const form = reactive({
  enabled: true,
  id: 0,
  name: '',
  slug: '',
  sortOrder: 0,
  summary: '',
  version: undefined as number | undefined,
});

async function load() {
  loading.value = true;
  try {
    rows.value = await listCaseCategories();
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, {
    enabled: true,
    id: 0,
    name: '',
    slug: '',
    sortOrder: rows.value.length * 10 + 10,
    summary: '',
    version: undefined,
  });
  dialogVisible.value = true;
}

function openEdit(value: unknown) {
  Object.assign(form, value as CaseCategoryRecord);
  dialogVisible.value = true;
}

async function save() {
  if (!form.name.trim() || !form.slug.trim()) {
    ElMessage.warning('请填写分类名称和 slug');
    return;
  }
  saving.value = true;
  try {
    await saveCaseCategory(form.id || null, {
      enabled: form.enabled,
      name: form.name,
      slug: form.slug,
      sortOrder: form.sortOrder,
      summary: form.summary || null,
      version: form.version,
    });
    dialogVisible.value = false;
    await load();
    ElMessage.success('案例分类已保存');
  } finally {
    saving.value = false;
  }
}

async function remove(id: number) {
  await deleteCaseCategory(id);
  await load();
  ElMessage.success('案例分类已删除');
}

onMounted(load);
</script>

<template>
  <div class="category-page">
    <section class="page-header">
      <div>
        <p>CASE TAXONOMY</p>
        <h1>案例分类</h1>
        <span>独立维护典型案例分类，不再与应用场景名称绑定</span>
      </div>
      <ElButton size="large" type="primary" @click="openCreate">
        新建分类
      </ElButton>
    </section>

    <ElCard class="table-card" shadow="never" v-loading="loading">
      <ElTable :data="rows" row-key="id">
        <ElTableColumn label="分类名称" min-width="180" prop="name" />
        <ElTableColumn label="slug" min-width="180" prop="slug" />
        <ElTableColumn
          label="简介"
          min-width="280"
          prop="summary"
          show-overflow-tooltip
        />
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }">
            <ElTag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '停用' }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="排序" prop="sortOrder" width="90" />
        <ElTableColumn fixed="right" label="操作" width="150">
          <template #default="{ row }">
            <ElButton link type="primary" @click="openEdit(row)">编辑</ElButton>
            <ElPopconfirm
              title="仅空分类可以删除，确定继续？"
              @confirm="remove(row.id)"
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
      :close-on-click-modal="false"
      :title="form.id ? '编辑案例分类' : '新建案例分类'"
      width="620px"
    >
      <ElForm :model="form" label-position="top">
        <ElFormItem label="分类名称" required>
          <ElInput v-model="form.name" maxlength="100" />
        </ElFormItem>
        <ElFormItem label="slug" required>
          <ElInput v-model="form.slug" placeholder="例如 building-projects" />
        </ElFormItem>
        <ElFormItem label="简介">
          <ElInput v-model="form.summary" :rows="3" type="textarea" />
        </ElFormItem>
        <ElFormItem label="排序">
          <ElInputNumber v-model="form.sortOrder" :min="0" />
        </ElFormItem>
        <ElFormItem label="启用">
          <ElSwitch v-model="form.enabled" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton :loading="saving" type="primary" @click="save">
          保存分类
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.category-page {
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
  background: linear-gradient(125deg, #102a35, #7d2428);
  border-radius: 18px;
}

.page-header p {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  color: #ff9b9f;
  letter-spacing: 0.18em;
}

.page-header h1 {
  margin: 5px 0;
  font-size: 28px;
}

.page-header span {
  color: rgb(255 255 255 / 68%);
}

.table-card {
  margin-top: 16px;
  border-radius: 16px;
}

@media (max-width: 760px) {
  .category-page {
    padding: 14px;
  }

  .page-header {
    flex-direction: column;
    gap: 18px;
    align-items: flex-start;
  }
}
</style>
