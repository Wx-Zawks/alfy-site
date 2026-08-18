<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import {
  ElButton,
  ElCard,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElPopconfirm,
  ElProgress,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag,
} from 'element-plus';

import {
  deleteRedirect,
  getContent,
  listContent,
  listNotFoundLogs,
  listRedirects,
  saveRedirect,
} from '#/api';
import { cmsState } from '#/data/cms';
import { contentFromBackend } from '#/data/cms-adapter';

const activeTab = ref('meta');
const dialogVisible = ref(false);
const loading = ref(false);
const saving = ref(false);
const form = reactive({
  enabled: true,
  id: 0,
  source: '',
  target: '',
  version: undefined as number | undefined,
});

const seoRows = computed(() =>
  cmsState.content.map((item) => ({
    ...item,
    score:
      [item.seoTitle, item.seoDescription, item.seoKeywords, item.slug].filter(
        Boolean,
      ).length * 25,
  })),
);
const averageScore = computed(() =>
  Math.round(
    seoRows.value.reduce((sum, item) => sum + item.score, 0) /
      Math.max(1, seoRows.value.length),
  ),
);
const notFoundRows = ref<
  Array<{
    count: number;
    lastSeen: string;
    path: string;
    referer?: null | string;
  }>
>([]);

async function load() {
  loading.value = true;
  try {
    const resources = [
      'articles',
      'cases',
      'pages',
      'partners',
      'products',
      'scenes',
    ] as const;
    const groups = await Promise.all(
      resources.map(async (resource) => {
        let values = await listContent(resource);
        if (resource === 'articles') {
          values = await Promise.all(
            values.map((item) => getContent(resource, Number(item.id))),
          );
        }
        return values.map((item) => contentFromBackend(resource, item));
      }),
    );
    cmsState.content.splice(0, cmsState.content.length, ...groups.flat());

    const [redirects, notFound] = await Promise.all([
      listRedirects(),
      listNotFoundLogs(),
    ]);
    notFoundRows.value = notFound;
    cmsState.redirects.splice(
      0,
      cmsState.redirects.length,
      ...redirects.map((item) => ({
        enabled: item.enabled,
        id: item.id,
        source: item.sourcePath,
        target: item.targetUrl,
        updatedAt: item.updatedAt,
        version: item.version,
      })),
    );
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, {
    enabled: true,
    id: 0,
    source: '',
    target: '',
    version: undefined,
  });
  dialogVisible.value = true;
}

function openEdit(value: unknown) {
  const item = value as {
    enabled: boolean;
    id: number;
    source: string;
    target: string;
    version?: number;
  };
  Object.assign(form, item);
  dialogVisible.value = true;
}

async function persistRedirect(
  value: {
    enabled: boolean;
    id: number;
    source: string;
    target: string;
    version?: number;
  },
  closeDialog = false,
) {
  const saved = await saveRedirect(value.id || null, {
    enabled: value.enabled,
    sourcePath: value.source,
    targetUrl: value.target,
    version: value.version,
  });
  if (closeDialog) dialogVisible.value = false;
  await load();
  return saved;
}

async function save() {
  if (!form.source.startsWith('/') || !form.target) {
    ElMessage.warning('来源路径必须以 / 开头，并填写目标地址');
    return;
  }
  saving.value = true;
  try {
    await persistRedirect(form, true);
    ElMessage.success('301 规则已保存到后端');
  } finally {
    saving.value = false;
  }
}

async function toggleRedirect(input: unknown) {
  const value = input as {
    enabled: boolean;
    id: number;
    source: string;
    target: string;
    version?: number;
  };
  await persistRedirect(value);
  ElMessage.success('规则状态已更新');
}

async function remove(id: number) {
  await deleteRedirect(id);
  await load();
  ElMessage.success('规则已删除');
}

onMounted(load);
</script>

<template>
  <div class="seo-page">
    <section class="page-header">
      <div>
        <p>SEARCH VISIBILITY</p>
        <h1>SEO 管理</h1>
        <span>检查页面元数据、管理旧域名跳转并跟踪 404 访问</span>
      </div>
      <div class="score">
        <strong>{{ averageScore }}</strong><span>平均完整度</span>
      </div>
    </section>
    <ElCard class="seo-card" shadow="never" v-loading="loading">
      <ElTabs v-model="activeTab">
        <ElTabPane label="页面 SEO" name="meta">
          <div class="tab-intro">
            <div>
              <strong>页面元数据检查</strong><span>每条内容应包含唯一标题、描述、关键词和语义化 slug</span>
            </div>
            <ElProgress :percentage="averageScore" :width="72" type="circle" />
          </div>
          <ElTable :data="seoRows">
            <ElTableColumn label="页面内容" min-width="280">
              <template #default="{ row }">
                <div class="title-cell">
                  <strong>{{ row.title }}</strong><small>/{{ row.slug }}</small>
                </div>
              </template>
            </ElTableColumn>
            <ElTableColumn
              label="SEO 标题"
              min-width="260"
              prop="seoTitle"
              show-overflow-tooltip
            />
            <ElTableColumn
              label="描述"
              min-width="300"
              prop="seoDescription"
              show-overflow-tooltip
            />
            <ElTableColumn label="完整度" width="110">
              <template #default="{ row }">
                <ElTag :type="row.score === 100 ? 'success' : 'warning'">
                  {{ row.score }}%
                </ElTag>
              </template>
            </ElTableColumn>
          </ElTable>
        </ElTabPane>
        <ElTabPane label="301 重定向" name="redirects">
          <div class="tab-intro">
            <div>
              <strong>旧 URL 映射</strong><span>旧域名上线迁移时保持长期有效，避免形成跳转链</span>
            </div>
            <ElButton type="primary" @click="openCreate">新增规则</ElButton>
          </div>
          <ElTable :data="cmsState.redirects">
            <ElTableColumn label="来源路径" min-width="260" prop="source" />
            <ElTableColumn label="目标地址" min-width="320" prop="target" />
            <ElTableColumn label="启用" width="90">
              <template #default="{ row }">
                <ElSwitch v-model="row.enabled" @change="toggleRedirect(row)" />
              </template>
            </ElTableColumn>
            <ElTableColumn label="更新时间" min-width="160" prop="updatedAt" />
            <ElTableColumn label="操作" width="140">
              <template #default="{ row }">
                <ElButton link type="primary" @click="openEdit(row)">
                  编辑
</ElButton><ElPopconfirm
                  title="确定删除该规则？"
                  @confirm="remove(row.id)"
                >
                  <template #reference>
                    <ElButton link type="danger">删除</ElButton>
                  </template>
                </ElPopconfirm>
              </template>
            </ElTableColumn>
          </ElTable>
        </ElTabPane>
        <ElTabPane label="404 记录" name="404">
          <div class="tab-intro">
            <div>
              <strong>未找到页面</strong><span>记录官网实际出现的 404 路径、来源和访问次数</span>
            </div>
          </div>
          <ElTable :data="notFoundRows">
            <ElTableColumn
              label="访问路径"
              min-width="300"
              prop="path"
            /><ElTableColumn
              label="次数"
              prop="count"
              width="100"
            /><ElTableColumn
              label="主要来源"
              min-width="180"
              prop="referer"
            /><ElTableColumn label="最近访问" min-width="180" prop="lastSeen" />
            <ElTableColumn label="建议" min-width="160">
              <template #default="{ row }">
                <ElTag :type="row.count > 10 ? 'warning' : 'info'">
                  {{ row.count > 10 ? '建议建立跳转' : '继续观察' }}
                </ElTag>
              </template>
            </ElTableColumn>
          </ElTable>
        </ElTabPane>
      </ElTabs>
    </ElCard>
    <ElDialog
      v-model="dialogVisible"
      :close-on-click-modal="false"
      :title="form.id ? '编辑 301 规则' : '新增 301 规则'"
      width="560px"
    >
      <ElForm label-position="top">
        <ElFormItem label="来源路径">
          <ElInput v-model="form.source" placeholder="/old/path" />
</ElFormItem><ElFormItem label="目标地址">
          <ElInput
            v-model="form.target"
            placeholder="/new/path 或完整网址"
          />
</ElFormItem><ElFormItem label="是否启用">
          <ElSwitch v-model="form.enabled" />
        </ElFormItem>
</ElForm><template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton><ElButton :loading="saving" type="primary" @click="save">
          保存规则
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.seo-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7f8;
}

.page-header {
  display: flex;
  align-items: center;
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

.page-header div > span {
  color: rgb(255 255 255 / 68%);
}

.score {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 18px;
  background: rgb(255 255 255 / 7%);
  border: 1px solid rgb(255 255 255 / 18%);
  border-radius: 14px;
}

.score strong {
  font-size: 34px;
}

.score span {
  font-size: 12px;
  color: rgb(255 255 255 / 65%);
}

.seo-card {
  margin-top: 16px;
  border-radius: 16px;
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

.title-cell strong,
.title-cell small {
  display: block;
}

.title-cell small {
  margin-top: 4px;
  color: #8b989c;
}

@media (max-width: 760px) {
  .seo-page {
    padding: 14px;
  }

  .page-header {
    flex-direction: column;
    gap: 18px;
    align-items: flex-start;
  }

  .score {
    justify-content: space-between;
    width: 100%;
  }

  .tab-intro {
    gap: 18px;
    align-items: flex-start;
  }
}
</style>
