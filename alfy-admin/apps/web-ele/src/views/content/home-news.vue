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

import { cmsState } from '#/data/cms';

const sortContainer = ref<HTMLElement | null>(null);
let sortableInstance: { destroy: () => void } | undefined;

const allArticles = computed(() =>
  cmsState.content.filter((item) => item.resource === 'articles'),
);
const pinnedArticle = computed(() =>
  allArticles.value.find((item) => item.homePinned),
);
const orderedArticles = computed(() =>
  allArticles.value
    .filter((item) => !item.homePinned)
    .sort((a, b) => a.homeSortOrder - b.homeSortOrder || a.id - b.id),
);
const visibleArticles = computed(() =>
  allArticles.value.filter(
    (item) => item.status === 'published' && item.showOnHome,
  ),
);
const orderedVisibleArticles = computed(() =>
  orderedArticles.value.filter(
    (item) => item.status === 'published' && item.showOnHome,
  ),
);
const previewPrimary = computed(() => {
  const current = pinnedArticle.value;
  if (current?.status === 'published' && current.showOnHome) return current;
  return orderedVisibleArticles.value[0];
});
const previewRest = computed(() =>
  orderedVisibleArticles.value.filter(
    (item) => item.id !== previewPrimary.value?.id,
  ),
);
const previewSecondary = computed(() => previewRest.value[0]);
const previewList = computed(() => previewRest.value.slice(1, 3));

function statusLabel(status: ContentItem['status']) {
  return status === 'published'
    ? '已发布'
    : (status === 'draft'
      ? '草稿'
      : '已下线');
}

function statusType(status: ContentItem['status']) {
  return status === 'published'
    ? 'success'
    : (status === 'draft'
      ? 'warning'
      : 'info');
}

function setPinned(item: ContentItem) {
  if (item.status !== 'published') {
    ElMessage.warning('新闻发布后才能在首页置顶');
    return;
  }
  const shouldCancel = item.homePinned;
  allArticles.value.forEach((candidate) => {
    candidate.homePinned = false;
  });
  if (shouldCancel) {
    ElMessage.info('已取消主置顶，首页将按排序选择第一条新闻');
  } else {
    item.homePinned = true;
    item.showOnHome = true;
    ElMessage.success(`“${item.title}”已设为首页主置顶新闻`);
  }
}

function handleVisibility(item: ContentItem) {
  if (!item.showOnHome && item.homePinned) item.homePinned = false;
  ElMessage.success(item.showOnHome ? '已加入首页展示' : '已从首页展示中移除');
}

function normalizeOrder(items: ContentItem[]) {
  items.forEach((item, index) => {
    item.homeSortOrder = (index + 1) * 10;
  });
}

onMounted(async () => {
  await nextTick();
  if (!sortContainer.value) return;
  const { initializeSortable } = useSortable(sortContainer.value, {
    chosenClass: 'news-row-chosen',
    dragClass: 'news-row-dragging',
    ghostClass: 'news-row-ghost',
    handle: '.drag-handle',
    onEnd(event) {
      const oldIndex = event.oldIndex;
      const newIndex = event.newIndex;
      if (
        oldIndex === undefined ||
        newIndex === undefined ||
        oldIndex === newIndex
      )
        return;
      const nextOrder = [...orderedArticles.value];
      const [moved] = nextOrder.splice(oldIndex, 1);
      if (!moved) return;
      nextOrder.splice(newIndex, 0, moved);
      normalizeOrder(nextOrder);
      ElMessage.success('首页新闻顺序已保存');
    },
  });
  sortableInstance = await initializeSortable();
});

onBeforeUnmount(() => sortableInstance?.destroy());
</script>

<template>
  <div class="home-news-page">
    <section class="page-header">
      <div>
        <p>HOMEPAGE NEWS</p>
        <h1>新闻资讯展示</h1>
        <span>控制首页新闻资讯的主置顶、展示状态和卡片顺序</span>
      </div>
      <div class="summary">
        <div>
          <b>{{ allArticles.length }}</b><span>全部新闻</span>
        </div>
        <div>
          <b>{{ visibleArticles.length }}</b><span>首页展示</span>
        </div>
        <div>
          <b>{{ pinnedArticle ? 1 : 0 }}</b><span>主置顶</span>
        </div>
      </div>
    </section>

    <ElAlert
      :closable="false"
      class="mode-alert"
      show-icon
      title="首页最多设置一个主置顶新闻；其余新闻拖拽排序后，第 1 条进入中间重点卡片，后续新闻进入右侧列表。"
      type="info"
    />

    <div class="news-layout">
      <div class="manager-column">
        <ElCard class="pinned-card" shadow="never">
          <div class="card-title">
            <div>
              <strong>首页主置顶新闻</strong><span>显示在新闻资讯区块左侧的大图位置</span>
            </div>
            <ElTag v-if="pinnedArticle" effect="dark" type="danger">
              主置顶
            </ElTag>
          </div>
          <article v-if="pinnedArticle" class="pinned-row">
            <ElImage
              v-if="pinnedArticle.cover"
              :preview-src-list="[pinnedArticle.cover]"
              :src="pinnedArticle.cover"
              fit="cover"
            />
            <div v-else class="cover-placeholder">
              {{ pinnedArticle.title.slice(0, 1) }}
            </div>
            <div class="news-copy">
              <div>
                <ElTag size="small">{{ pinnedArticle.category }}</ElTag>
                <ElTag :type="statusType(pinnedArticle.status)" size="small">
                  {{ statusLabel(pinnedArticle.status) }}
                </ElTag>
              </div>
              <h3>{{ pinnedArticle.title }}</h3>
              <p>{{ pinnedArticle.summary }}</p>
            </div>
            <div class="row-actions">
              <ElSwitch
                v-model="pinnedArticle.showOnHome"
                active-text="首页显示"
                @change="handleVisibility(pinnedArticle)"
              />
              <ElButton plain type="danger" @click="setPinned(pinnedArticle)">
                取消置顶
              </ElButton>
            </div>
          </article>
          <ElEmpty
            v-else
            :image-size="80"
            description="暂未设置主置顶新闻；首页将使用排序第一的已发布新闻"
          />
        </ElCard>

        <ElCard class="sort-card" shadow="never">
          <div class="card-title">
            <div>
              <strong>其他新闻与排序</strong><span>拖动左侧手柄调整首页重点卡片和列表顺序</span>
            </div>
            <ElTag effect="plain">拖拽排序</ElTag>
          </div>
          <div ref="sortContainer" class="news-sort-list">
            <article
              v-for="item in orderedArticles"
              :key="item.id"
              :data-id="item.id"
              class="news-row"
            >
              <button
                aria-label="拖动新闻调整顺序"
                class="drag-handle"
                title="拖动排序"
                type="button"
              >
                <span></span><span></span><span></span>
              </button>
              <ElImage v-if="item.cover" :src="item.cover" fit="cover" />
              <div v-else class="cover-placeholder">
                {{ item.title.slice(0, 1) }}
              </div>
              <div class="news-copy">
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
                  设为主置顶
                </ElButton>
              </div>
            </article>
          </div>
          <ElEmpty
            v-if="orderedArticles.length === 0"
            description="暂无其他新闻"
          />
        </ElCard>
      </div>

      <ElCard class="preview-card" shadow="never">
        <div class="card-title">
          <div>
            <strong>首页效果预览</strong><span>对应左侧主新闻、中间重点新闻和右侧新闻列表</span>
          </div>
          <ElTag :type="visibleArticles.length > 0 ? 'success' : 'warning'">
            {{ visibleArticles.length > 0 ? '有内容' : '待配置' }}
          </ElTag>
        </div>
        <div v-if="previewPrimary" class="homepage-preview">
          <article class="primary-preview">
            <ElImage
              v-if="previewPrimary.cover"
              :src="previewPrimary.cover"
              fit="cover"
            />
            <div>
              <span>{{ previewPrimary.category }}</span>
              <h3>{{ previewPrimary.title }}</h3>
              <b>了解更多 ↗</b>
            </div>
          </article>
          <article v-if="previewSecondary" class="secondary-preview">
            <span>{{ previewSecondary.category }}</span>
            <h3>{{ previewSecondary.title }}</h3>
            <p>{{ previewSecondary.summary }}</p>
            <b>了解更多 ↗</b>
          </article>
          <div v-else class="preview-placeholder">
            排序第 1 条新闻将在这里显示
          </div>
          <div class="list-preview">
            <article v-for="item in previewList" :key="item.id">
              <ElImage v-if="item.cover" :src="item.cover" fit="cover" />
              <div>
                <span>{{ item.category }}</span>
                <h3>{{ item.title }}</h3>
                <b>查看新闻 ↗</b>
              </div>
            </article>
            <div v-if="previewList.length === 0" class="preview-placeholder">
              更多新闻将在这里显示
            </div>
          </div>
        </div>
        <ElEmpty v-else description="请先发布新闻并开启首页显示" />
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
.home-news-page {
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

.news-layout {
  display: grid;
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

.sort-card,
.preview-card {
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
.news-row {
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

.news-sort-list {
  display: grid;
  gap: 10px;
}

.news-row {
  grid-template-columns: 24px 88px minmax(220px, 1fr) auto auto;
  padding: 10px 12px;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.news-row :deep(.el-image),
.news-row > .cover-placeholder {
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

.news-copy {
  min-width: 0;
}

.news-copy > div {
  display: flex;
  gap: 6px;
}

.news-copy h3 {
  margin: 7px 0 3px;
  overflow: hidden;
  font-size: 15px;
  color: #203c42;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.news-copy p {
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

.news-row-ghost {
  opacity: 0.35;
}

.news-row-chosen {
  border-color: #67b8af;
  box-shadow: 0 8px 24px rgb(13 98 102 / 14%);
}

.news-row-dragging {
  background: #f4fbfa;
}

.homepage-preview {
  display: grid;
  grid-template-columns: 1.25fr 0.8fr 1fr;
  min-height: 390px;
  border: 1px solid #e0e5e6;
}

.primary-preview,
.secondary-preview,
.list-preview {
  min-width: 0;
}

.primary-preview {
  display: grid;
  grid-template-rows: 1fr auto;
  border-right: 1px solid #e0e5e6;
}

.primary-preview :deep(.el-image) {
  width: 100%;
  min-height: 230px;
}

.primary-preview > div {
  padding: 22px;
}

.primary-preview span,
.secondary-preview > span,
.list-preview span {
  font-size: 11px;
  font-weight: 700;
  color: #e71c28;
}

.primary-preview h3 {
  margin: 10px 0 20px;
  font-size: 23px;
  line-height: 1.3;
  color: #172126;
}

.primary-preview b,
.secondary-preview b,
.list-preview b {
  font-size: 11px;
  color: #e71c28;
}

.secondary-preview {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 28px;
  color: #fff;
  background: #ed1c24;
}

.secondary-preview > span,
.secondary-preview b {
  color: #fff;
}

.secondary-preview h3 {
  margin: 12px 0;
  font-size: 23px;
  line-height: 1.25;
}

.secondary-preview p {
  margin: 0 0 24px;
  font-size: 12px;
  line-height: 1.6;
  color: rgb(255 255 255 / 78%);
}

.list-preview {
  display: grid;
  grid-auto-rows: minmax(0, 1fr);
}

.list-preview article {
  display: grid;
  grid-template-columns: 105px minmax(0, 1fr);
  border-bottom: 1px solid #e0e5e6;
}

.list-preview :deep(.el-image) {
  width: 105px;
  height: 100%;
}

.list-preview article > div {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding: 18px;
}

.list-preview h3 {
  margin: 8px 0 16px;
  font-size: 14px;
  line-height: 1.35;
  color: #1f292d;
}

.preview-placeholder {
  display: grid;
  place-items: center;
  min-height: 160px;
  padding: 20px;
  color: #8a979b;
  text-align: center;
  background: #f7f9f9;
}

@media (max-width: 1100px) {
  .homepage-preview {
    grid-template-columns: 1fr 1fr;
  }

  .list-preview {
    grid-template-columns: 1fr 1fr;
    grid-column: 1 / -1;
  }
}

@media (max-width: 820px) {
  .home-news-page {
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

  .news-row {
    grid-template-columns: 24px 72px minmax(0, 1fr);
  }

  .news-row :deep(.el-image),
  .news-row > .cover-placeholder {
    width: 72px;
    height: 58px;
  }

  .news-row .sort-value {
    display: none;
  }

  .news-row .row-actions {
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

  .homepage-preview {
    grid-template-columns: 1fr;
  }

  .list-preview {
    grid-template-columns: 1fr;
    grid-column: auto;
  }
}
</style>
