<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  ElButton,
  ElCard,
  ElCol,
  ElProgress,
  ElRow,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import { listContent, listInquiries, listMedia } from '#/api';
import { cmsState } from '#/data/cms';
import { contentFromBackend } from '#/data/cms-adapter';

const router = useRouter();
const loading = ref(false);

const metrics = computed(() => [
  {
    label: '已发布内容',
    value: cmsState.content.filter((item) => item.status === 'published')
      .length,
    note: '官网当前可见',
    tone: 'teal',
  },
  {
    label: '待处理询盘',
    value: cmsState.inquiries.filter((item) => item.status === 'NEW').length,
    note: '建议今日跟进',
    tone: 'orange',
  },
  {
    label: '内容草稿',
    value: cmsState.content.filter((item) => item.status === 'draft').length,
    note: '等待审核发布',
    tone: 'blue',
  },
  {
    label: '素材文件',
    value: cmsState.media.length,
    note: '图片与文档',
    tone: 'slate',
  },
]);

const latestContent = computed(() =>
  [...cmsState.content].sort((a, b) => b.id - a.id).slice(0, 6),
);

const completeness = computed(() => {
  const groups = [
    'products',
    'scenes',
    'cases',
    'technologies',
    'articles',
    'pages',
  ];
  return Math.round(
    (groups.filter((resource) =>
      cmsState.content.some(
        (item) => item.resource === resource && item.status === 'published',
      ),
    ).length /
      groups.length) *
      100,
  );
});

const inquiryStatus: Record<
  string,
  { label: string; type: 'info' | 'success' | 'warning' }
> = {
  CLOSED: { label: '已关闭', type: 'success' },
  CONTACTED: { label: '已联系', type: 'warning' },
  NEW: { label: '待处理', type: 'warning' },
  QUALIFIED: { label: '有效商机', type: 'success' },
  SPAM: { label: '无效', type: 'info' },
};

function getInquiryStatus(value: string) {
  return inquiryStatus[value] || { label: '未知', type: 'info' as const };
}

function readableSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

async function load() {
  loading.value = true;
  try {
    const resources = [
      'articles',
      'banners',
      'cases',
      'pages',
      'partners',
      'products',
      'scenes',
      'technologies',
    ] as const;
    const [groups, inquiries, media] = await Promise.all([
      Promise.all(
        resources.map(async (resource) => {
          const items = await listContent(resource);
          return items.map((item) => contentFromBackend(resource, item));
        }),
      ),
      listInquiries(),
      listMedia(),
    ]);
    cmsState.content.splice(0, cmsState.content.length, ...groups.flat());
    cmsState.inquiries.splice(
      0,
      cmsState.inquiries.length,
      ...inquiries.map((item) => ({
        assignee: '',
        company: item.company || '',
        createdAt: item.createdAt,
        email: item.email || '',
        followUp: item.adminNote || '',
        id: item.id,
        inquiryNo: item.inquiryNo,
        message: item.message || '',
        name: item.name,
        phone: item.phone || '',
        source: item.sourceUrl || '',
        status: item.status as 'NEW',
        type: item.inquiryType,
      })),
    );
    cmsState.media.splice(
      0,
      cmsState.media.length,
      ...media.map((item) => ({
        alt: item.altText || '',
        createdAt: item.createdAt,
        id: item.id,
        name: item.originalFilename,
        size: readableSize(item.fileSize),
        type: item.mediaType.toLowerCase() as 'document' | 'image' | 'video',
        url: item.adminUrl,
      })),
    );
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="dashboard-page" v-loading="loading">
    <section class="dashboard-hero">
      <div>
        <p class="eyebrow">ALFY CONTENT OPERATIONS</p>
        <h1>下午好，内容团队</h1>
        <p>官网一期内容中心运行正常。优先处理新询盘和待发布资料。</p>
      </div>
      <div class="hero-actions">
        <ElButton plain size="large" @click="router.push('/content/articles')">
          发布新闻
        </ElButton>
        <ElButton
          size="large"
          type="primary"
          @click="router.push('/content/products')"
        >
          管理产品
        </ElButton>
      </div>
    </section>

    <ElRow :gutter="16" class="metric-row">
      <ElCol
        v-for="item in metrics"
        :key="item.label"
        :lg="6"
        :sm="12"
        :xs="24"
      >
        <ElCard class="metric-card" shadow="never">
          <div :class="[item.tone]" class="metric-mark"></div>
          <div>
            <span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.note }}</small>
          </div>
        </ElCard>
      </ElCol>
    </ElRow>

    <ElRow :gutter="16">
      <ElCol :lg="16" :xs="24">
        <ElCard class="panel" shadow="never">
          <template #header>
            <div class="panel-title">
              <div>
                <strong>最近更新</strong><span>跨模块内容变更记录</span>
              </div>
              <ElButton
                link
                type="primary"
                @click="router.push('/content/products')"
              >
                查看内容中心
              </ElButton>
            </div>
          </template>
          <ElTable :data="latestContent">
            <ElTableColumn label="内容" min-width="260">
              <template #default="{ row }">
                <div class="table-title">
                  <strong>{{ row.title }}</strong><small>{{ row.category }}</small>
                </div>
              </template>
            </ElTableColumn>
            <ElTableColumn label="模块" prop="resource" width="110" />
            <ElTableColumn label="状态" width="100">
              <template #default="{ row }">
                <ElTag
                  :type="row.status === 'published' ? 'success' : 'warning'"
                >
                  {{ row.status === 'published' ? '已发布' : '草稿' }}
                </ElTag>
              </template>
            </ElTableColumn>
            <ElTableColumn label="更新时间" min-width="150" prop="updatedAt" />
          </ElTable>
        </ElCard>
      </ElCol>
      <ElCol :lg="8" :xs="24">
        <ElCard class="panel progress-panel" shadow="never">
          <template #header>
            <div class="panel-title">
              <div>
                <strong>一期内容完整度</strong><span>按核心栏目统计</span>
              </div>
            </div>
          </template>
          <ElProgress
            :percentage="completeness"
            :stroke-width="12"
            color="#0f6b70"
          />
          <ul>
            <li><span>产品资料与分类</span><b>已录入</b></li>
            <li><span>应用场景和案例</span><b>待审核</b></li>
            <li><span>历史新闻资料</span><b>需补充</b></li>
            <li><span>联系方式与备案</span><b>待确认</b></li>
          </ul>
          <ElButton
            class="full-button"
            @click="router.push('/system/settings')"
          >
            完善站点资料
          </ElButton>
        </ElCard>
      </ElCol>
    </ElRow>

    <ElCard class="panel inquiry-panel" shadow="never">
      <template #header>
        <div class="panel-title">
          <div>
            <strong>最新询盘</strong><span>来自官网表单的客户需求</span>
          </div>
          <ElButton
            link
            type="primary"
            @click="router.push('/interaction/inquiries')"
          >
            进入询盘中心
          </ElButton>
        </div>
      </template>
      <ElTable :data="cmsState.inquiries.slice(0, 4)">
        <ElTableColumn label="联系人" prop="name" width="110" />
        <ElTableColumn label="公司" min-width="190" prop="company" />
        <ElTableColumn label="需求类型" prop="type" width="120" />
        <ElTableColumn label="需求摘要" min-width="280" prop="message" />
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }">
            <ElTag :type="getInquiryStatus(row.status).type">
              {{ getInquiryStatus(row.status).label }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="提交时间" min-width="150" prop="createdAt" />
      </ElTable>
    </ElCard>
  </div>
</template>

<style scoped>
.dashboard-page {
  min-height: 100%;
  padding: 24px;
  background: #f4f7f7;
}

.dashboard-hero {
  display: flex;
  gap: 24px;
  align-items: flex-end;
  justify-content: space-between;
  min-height: 196px;
  padding: 34px 36px;
  color: #fff;
  background:
    radial-gradient(circle at 82% 10%, rgb(51 174 167 / 32%), transparent 30%),
    linear-gradient(125deg, #071f29, #0b3f49 62%, #0f6267);
  border-radius: 20px;
  box-shadow: 0 22px 55px rgb(5 38 46 / 18%);
}

.dashboard-hero h1 {
  margin: 6px 0 10px;
  font-size: clamp(28px, 3vw, 38px);
  font-weight: 650;
  letter-spacing: -0.03em;
}

.dashboard-hero p:last-child {
  margin: 0;
  color: rgb(255 255 255 / 68%);
}

.eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  color: #6fd0c6;
  letter-spacing: 0.2em;
}

.hero-actions {
  display: flex;
  gap: 10px;
}

.dashboard-hero :deep(.el-button--primary) {
  --el-button-bg-color: #e85d45;
  --el-button-border-color: #e85d45;
}

.dashboard-hero :deep(.el-button.is-plain) {
  color: #fff;
  background: rgb(255 255 255 / 8%);
  border-color: rgb(255 255 255 / 25%);
}

.metric-row {
  margin-top: 16px;
}

.metric-card {
  position: relative;
  margin-bottom: 16px;
  overflow: hidden;
  border: 1px solid #e4eaeb;
  border-radius: 16px;
}

.metric-card :deep(.el-card__body) {
  display: flex;
  gap: 15px;
  padding: 20px;
}

.metric-mark {
  width: 6px;
  height: 68px;
  background: #0f6b70;
  border-radius: 10px;
}

.metric-mark.orange {
  background: #e85d45;
}

.metric-mark.blue {
  background: #3d79b8;
}

.metric-mark.slate {
  background: #718187;
}

.metric-card span,
.metric-card strong,
.metric-card small {
  display: block;
}

.metric-card span {
  font-size: 13px;
  color: #718187;
}

.metric-card strong {
  margin: 1px 0;
  font-size: 30px;
  color: #143239;
}

.metric-card small {
  color: #9aa4a7;
}

.panel {
  margin-bottom: 16px;
  border: 1px solid #e4eaeb;
  border-radius: 16px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title strong,
.panel-title span {
  display: block;
}

.panel-title strong {
  font-size: 16px;
  color: #143239;
}

.panel-title span {
  margin-top: 3px;
  font-size: 12px;
  color: #899699;
}

.table-title strong,
.table-title small {
  display: block;
}

.table-title small {
  margin-top: 4px;
  color: #879397;
}

.progress-panel ul {
  padding: 0;
  margin: 22px 0;
  list-style: none;
}

.progress-panel li {
  display: flex;
  justify-content: space-between;
  padding: 11px 0;
  color: #66767b;
  border-bottom: 1px solid #edf0f1;
}

.progress-panel li b {
  font-size: 12px;
  color: #173b42;
}

.full-button {
  width: 100%;
}

@media (max-width: 760px) {
  .dashboard-page {
    padding: 14px;
  }

  .dashboard-hero {
    flex-direction: column;
    align-items: flex-start;
    min-height: auto;
    padding: 24px;
  }

  .hero-actions {
    width: 100%;
  }

  .hero-actions :deep(.el-button) {
    flex: 1;
  }
}
</style>
