<script lang="ts" setup>
import type { ContentItem } from '#/data/cms';

import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

import { useSortable } from '@vben/hooks';

import {
  ElAlert,
  ElButton,
  ElCard,
  ElEmpty,
  ElImage,
  ElMessage,
  ElSwitch,
  ElTag,
} from 'element-plus';

import { listCaseCategories, listContent, saveContent } from '#/api';
import { cmsState } from '#/data/cms';
import { contentFromBackend, contentPayload } from '#/data/cms-adapter';

const sortContainer = ref<HTMLElement | null>(null);
const loading = ref(false);
let sortableInstance: { destroy: () => void } | undefined;

const allCases = computed(() =>
  cmsState.content.filter((item) => item.resource === 'cases'),
);
const pinnedCase = computed(() =>
  allCases.value.find((item) => item.homePinned),
);
const orderedCases = computed(() =>
  allCases.value
    .filter((item) => !item.homePinned)
    .sort((a, b) => a.homeSortOrder - b.homeSortOrder || a.id - b.id),
);
const visibleCases = computed(() =>
  allCases.value.filter(
    (item) => item.status === 'published' && item.showOnHome,
  ),
);
const orderedVisibleCases = computed(() =>
  orderedCases.value.filter(
    (item) => item.status === 'published' && item.showOnHome,
  ),
);
const previewPinned = computed(() => {
  const current = pinnedCase.value;
  if (current?.status === 'published' && current.showOnHome) return current;
  return orderedVisibleCases.value[0];
});
const previewCards = computed(() =>
  orderedVisibleCases.value
    .filter((item) => item.id !== previewPinned.value?.id)
    .slice(0, 3),
);

async function load() {
  loading.value = true;
  try {
    const [cases, categories] = await Promise.all([
      listContent('cases'),
      listCaseCategories(),
    ]);
    const names = new Map(
      categories.map((item) => [Number(item.id), item.name]),
    );
    const mapped = cases.map((item) =>
      contentFromBackend('cases', item, names),
    );
    const retained = cmsState.content.filter(
      (item) => item.resource !== 'cases',
    );
    cmsState.content.splice(0, cmsState.content.length, ...retained, ...mapped);
  } finally {
    loading.value = false;
  }
}

async function persist(item: ContentItem) {
  await saveContent('cases', item.id, contentPayload('cases', item));
}

function previewUrl(value: string) {
  return value;
}

function statusLabel(status: ContentItem['status']) {
  if (status === 'published') return '已发布';
  if (status === 'draft') return '草稿';
  return '已下线';
}

function statusType(status: ContentItem['status']) {
  if (status === 'published') return 'success';
  if (status === 'draft') return 'warning';
  return 'info';
}

async function setPinned(item: ContentItem) {
  if (item.status !== 'published') {
    ElMessage.warning('案例发布后才能在首页推荐');
    return;
  }
  const shouldCancel = item.homePinned;
  for (const candidate of allCases.value) {
    candidate.homePinned = false;
    candidate.showOnHome = false;
    candidate.featured = false;
    if (candidate.id === item.id && !shouldCancel) {
      candidate.homePinned = true;
      candidate.showOnHome = true;
      candidate.featured = true;
    }
    await persist(candidate);
  }
  await load();
  ElMessage.success(
    shouldCancel ? '已取消首页推荐' : `“${item.title}”已设为首页推荐案例`,
  );
}

async function handleVisibility(item: ContentItem) {
  item.homePinned = item.showOnHome;
  item.featured = item.showOnHome;
  await persist(item);
  await load();
  ElMessage.success(item.showOnHome ? '已加入首页展示' : '已从首页展示中移除');
}

function normalizeOrder(items: ContentItem[]) {
  items.forEach((item, index) => {
    item.homeSortOrder = (index + 1) * 10;
    item.sortOrder = item.homeSortOrder;
  });
}

onMounted(async () => {
  await load();
  await nextTick();
  if (!sortContainer.value) return;
  const { initializeSortable } = useSortable(sortContainer.value, {
    chosenClass: 'case-row-chosen',
    dragClass: 'case-row-dragging',
    ghostClass: 'case-row-ghost',
    handle: '.drag-handle',
    async onEnd(event) {
      const oldIndex = event.oldIndex;
      const newIndex = event.newIndex;
      if (
        oldIndex === undefined ||
        newIndex === undefined ||
        oldIndex === newIndex
      )
        return;
      const nextOrder = [...orderedCases.value];
      const [moved] = nextOrder.splice(oldIndex, 1);
      if (!moved) return;
      nextOrder.splice(newIndex, 0, moved);
      normalizeOrder(nextOrder);
      for (const item of nextOrder) await persist(item);
      await load();
      ElMessage.success('首页案例顺序已保存到后端');
    },
  });
  sortableInstance = await initializeSortable();
});

onBeforeUnmount(() => sortableInstance?.destroy());
</script>

<template>
  <div class="home-cases-page">
    <section class="page-header">
      <div>
        <p>FEATURED CASES</p>
        <h1>典型案例展示</h1>
        <span>控制首页案例的大图置顶、展示状态和卡片顺序</span>
      </div>
      <div class="summary">
        <div>
          <b>{{ allCases.length }}</b><span>全部案例</span>
        </div>
        <div>
          <b>{{ visibleCases.length }}</b><span>首页展示</span>
        </div>
        <div>
          <b>{{ pinnedCase ? 1 : 0 }}</b><span>置顶案例</span>
        </div>
      </div>
    </section>

    <ElAlert
      :closable="false"
      class="mode-alert"
      show-icon
      title="仅已发布案例能进入首页；首页最多置顶一个案例，拖动左侧手柄可调整其余卡片顺序。"
      type="info"
    />

    <div class="case-layout" v-loading="loading">
      <div class="manager-column">
        <ElCard class="pinned-card" shadow="never">
          <div class="card-title">
            <div>
              <strong>首页置顶案例</strong><span>显示在“典型案例”区块的大图位置</span>
            </div>
            <ElTag v-if="pinnedCase" effect="dark" type="danger">置顶</ElTag>
          </div>
          <article v-if="pinnedCase" class="pinned-row">
            <ElImage
              v-if="pinnedCase.cover"
              :preview-src-list="[pinnedCase.cover]"
              :src="previewUrl(pinnedCase.cover)"
              fit="cover"
            />
            <div v-else class="cover-placeholder">
              {{ pinnedCase.title.slice(0, 1) }}
            </div>
            <div class="case-copy">
              <div>
                <ElTag size="small">{{ pinnedCase.category }}</ElTag><ElTag :type="statusType(pinnedCase.status)" size="small">
                  {{ statusLabel(pinnedCase.status) }}
                </ElTag>
              </div>
              <h3>{{ pinnedCase.title }}</h3>
              <p>{{ pinnedCase.summary }}</p>
            </div>
            <div class="row-actions">
              <ElSwitch
                v-model="pinnedCase.showOnHome"
                active-text="首页显示"
                @change="handleVisibility(pinnedCase)"
              />
              <ElButton plain type="danger" @click="setPinned(pinnedCase)">
                取消置顶
              </ElButton>
            </div>
          </article>
          <ElEmpty
            v-else
            :image-size="80"
            description="暂未设置置顶案例；首页将使用排序第一的已发布案例"
          />
        </ElCard>

        <ElCard class="sort-card" shadow="never">
          <div class="card-title">
            <div>
              <strong>其他案例与排序</strong><span>排序数字由拖拽自动维护，越靠上越优先展示</span>
            </div>
            <ElTag effect="plain">拖拽排序</ElTag>
          </div>
          <div ref="sortContainer" class="case-sort-list">
            <article
              v-for="item in orderedCases"
              :key="item.id"
              :data-id="item.id"
              class="case-row"
            >
              <button
                aria-label="拖动案例调整顺序"
                class="drag-handle"
                title="拖动排序"
                type="button"
              >
                <span></span><span></span><span></span>
              </button>
              <ElImage
                v-if="item.cover"
                :src="previewUrl(item.cover)"
                fit="cover"
              />
              <div v-else class="cover-placeholder">
                {{ item.title.slice(0, 1) }}
              </div>
              <div class="case-copy">
                <div>
                  <ElTag effect="plain" size="small">{{ item.category }}</ElTag>
                  <ElTag :type="statusType(item.status)" size="small">
                    {{ statusLabel(item.status) }}
                  </ElTag>
                </div>
                <h3>{{ item.title }}</h3>
                <p>{{ item.summary }}</p>
              </div>
              <span class="sort-value">排序 {{ item.homeSortOrder }}</span>
              <div class="row-actions">
                <ElSwitch
                  v-model="item.showOnHome"
                  :disabled="item.status !== 'published'"
                  active-text="首页显示"
                  inactive-text="不显示"
                  @change="handleVisibility(item)"
                />
                <ElButton
                  :disabled="item.status !== 'published'"
                  plain
                  type="danger"
                  @click="setPinned(item)"
                >
                  设为置顶
                </ElButton>
              </div>
            </article>
          </div>
          <ElEmpty
            v-if="orderedCases.length === 0"
            description="暂无其他案例"
          />
        </ElCard>
      </div>

      <ElCard class="preview-card" shadow="never">
        <div class="card-title">
          <div>
            <strong>首页效果预览</strong><span>最多预览 1 个大案例和 3 个案例卡片</span>
          </div>
          <ElTag :type="visibleCases.length > 0 ? 'success' : 'warning'">
            {{ visibleCases.length > 0 ? '有内容' : '待配置' }}
          </ElTag>
        </div>
        <div v-if="previewPinned" class="homepage-preview">
          <article class="feature-preview">
            <ElImage
              v-if="previewPinned.cover"
              :src="previewUrl(previewPinned.cover)"
              fit="cover"
            />
            <div class="preview-overlay">
              <span>{{ previewPinned.category }}</span>
              <h3>{{ previewPinned.title }}</h3>
              <p>{{ previewPinned.summary }}</p>
            </div>
          </article>
          <div class="card-preview-grid">
            <article v-for="item in previewCards" :key="item.id">
              <ElImage
                v-if="item.cover"
                :src="previewUrl(item.cover)"
                fit="cover"
              />
              <div>
                <b>{{ item.title }}</b><span>查看案例 ↗</span>
              </div>
            </article>
          </div>
        </div>
        <ElEmpty v-else description="请先发布案例并开启首页显示" />
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
.home-cases-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7f8;
}

.page-header {
  display: flex;
  gap: 24px;
  align-items: flex-end;
  justify-content: space-between;
  padding: 28px 30px;
  color: #fff;
  background:
    radial-gradient(circle at 76% 12%, rgb(76 185 173 / 25%), transparent 26%),
    linear-gradient(125deg, #071f29, #0d555c);
  border-radius: 18px;
  box-shadow: 0 18px 48px rgb(8 54 61 / 16%);
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

.page-header > div > span {
  color: rgb(255 255 255 / 68%);
}

.summary {
  display: flex;
  gap: 8px;
}

.summary div {
  min-width: 92px;
  padding: 12px 16px;
  background: rgb(255 255 255 / 7%);
  border: 1px solid rgb(255 255 255 / 14%);
  border-radius: 12px;
}

.summary b,
.summary span {
  display: block;
}

.summary b {
  font-size: 23px;
}

.summary span {
  margin-top: 2px;
  font-size: 11px;
  color: rgb(255 255 255 / 60%);
}

.mode-alert {
  margin-top: 16px;
}

.case-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(330px, 0.8fr);
  gap: 16px;
  margin-top: 16px;
}

.manager-column {
  min-width: 0;
}

.pinned-card,
.sort-card,
.preview-card {
  border: 1px solid #e4eaec;
  border-radius: 16px;
}

.sort-card {
  margin-top: 16px;
}

.card-title {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.card-title strong,
.card-title span {
  display: block;
}

.card-title strong {
  font-size: 17px;
  color: #17363d;
}

.card-title span {
  margin-top: 4px;
  font-size: 12px;
  color: #879499;
}

.pinned-row,
.case-row {
  display: grid;
  gap: 14px;
  align-items: center;
  background: #fff;
  border: 1px solid #e5ebec;
  border-radius: 13px;
}

.pinned-row {
  grid-template-columns: 150px minmax(0, 1fr) auto;
  padding: 12px;
  background: #fffafa;
  border-color: #f0cdc7;
}

.pinned-row :deep(.el-image) {
  width: 150px;
  height: 102px;
  border-radius: 9px;
}

.case-sort-list {
  display: grid;
  gap: 10px;
}

.case-row {
  grid-template-columns: 24px 88px minmax(180px, 1fr) auto auto;
  padding: 10px 12px;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.case-row :deep(.el-image),
.case-row > .cover-placeholder {
  width: 88px;
  height: 66px;
  border-radius: 8px;
}

.cover-placeholder {
  display: grid;
  place-items: center;
  font-size: 24px;
  font-weight: 700;
  color: #0e5960;
  background: #dcefed;
}

.drag-handle {
  display: grid;
  gap: 4px;
  place-content: center;
  width: 24px;
  height: 42px;
  color: #9aa7aa;
  cursor: grab;
  background: transparent;
  border: 0;
}

.drag-handle:active {
  cursor: grabbing;
}

.drag-handle span {
  width: 15px;
  height: 2px;
  background: currentcolor;
  border-radius: 2px;
}

.case-copy {
  min-width: 0;
}

.case-copy > div {
  display: flex;
  gap: 6px;
}

.case-copy h3 {
  margin: 7px 0 3px;
  overflow: hidden;
  font-size: 15px;
  color: #203c42;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.case-copy p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  font-size: 12px;
  line-height: 1.5;
  color: #7c8b90;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.sort-value {
  font-size: 12px;
  color: #8b989c;
  white-space: nowrap;
}

.row-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pinned-row .row-actions {
  flex-direction: column;
  align-items: flex-end;
}

.case-row-ghost {
  opacity: 0.35;
}

.case-row-chosen {
  border-color: #67b8af;
  box-shadow: 0 8px 24px rgb(13 98 102 / 14%);
}

.case-row-dragging {
  background: #f4fbfa;
}

.preview-card {
  position: sticky;
  top: 16px;
  align-self: start;
}

.homepage-preview {
  display: grid;
  gap: 12px;
}

.feature-preview {
  position: relative;
  min-height: 280px;
  overflow: hidden;
  color: #fff;
  background: #0a3340;
  border-radius: 13px;
}

.feature-preview :deep(.el-image) {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.feature-preview::after {
  position: absolute;
  inset: 0;
  content: '';
  background: linear-gradient(0deg, rgb(3 24 33 / 92%), rgb(3 24 33 / 10%));
}

.preview-overlay {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 1;
  padding: 24px;
}

.preview-overlay span {
  font-size: 11px;
  color: #78d2c8;
}

.preview-overlay h3 {
  margin: 6px 0;
  font-size: 22px;
}

.preview-overlay p {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: rgb(255 255 255 / 68%);
}

.card-preview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.card-preview-grid article {
  overflow: hidden;
  border: 1px solid #e6ebec;
  border-radius: 10px;
}

.card-preview-grid :deep(.el-image) {
  width: 100%;
  height: 72px;
}

.card-preview-grid article > div {
  padding: 9px;
}

.card-preview-grid b,
.card-preview-grid span {
  display: block;
}

.card-preview-grid b {
  overflow: hidden;
  font-size: 11px;
  color: #29444a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-preview-grid span {
  margin-top: 5px;
  font-size: 9px;
  color: #d95a45;
}

@media (max-width: 1200px) {
  .case-layout {
    grid-template-columns: 1fr;
  }

  .preview-card {
    position: static;
  }
}

@media (max-width: 820px) {
  .home-cases-page {
    padding: 14px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    padding: 22px;
  }

  .summary {
    width: 100%;
  }

  .summary div {
    flex: 1;
    min-width: 0;
  }

  .case-row {
    grid-template-columns: 24px 72px minmax(0, 1fr);
  }

  .case-row :deep(.el-image),
  .case-row > .cover-placeholder {
    width: 72px;
    height: 58px;
  }

  .case-row .sort-value {
    display: none;
  }

  .case-row .row-actions {
    grid-column: 2 / -1;
    justify-content: space-between;
  }

  .pinned-row {
    grid-template-columns: 110px minmax(0, 1fr);
  }

  .pinned-row :deep(.el-image) {
    width: 110px;
    height: 82px;
  }

  .pinned-row .row-actions {
    flex-direction: row;
    grid-column: 1 / -1;
    align-items: center;
    justify-content: space-between;
  }
}
</style>
