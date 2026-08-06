<script lang="ts" setup>
import type { HomeSection, HomeSectionKey } from '#/data/cms';

import { computed, onMounted, reactive, ref, toRaw } from 'vue';

import {
  ElAlert,
  ElButton,
  ElCard,
  ElCol,
  ElForm,
  ElFormItem,
  ElImage,
  ElInput,
  ElMessage,
  ElOption,
  ElPopconfirm,
  ElRow,
  ElSelect,
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  deleteHomeSection,
  getMediaPreviewUrl,
  listHomeSections,
  listMedia,
  saveHomeSection,
} from '#/api';
import { cmsState } from '#/data/cms';
import { mediaIdFromUrl } from '#/data/cms-adapter';

const cloneSections = () => structuredClone(toRaw(cmsState.homePage.sections));

const activeKey = ref<HomeSectionKey>('about');
const loading = ref(false);
const saving = ref(false);
const draft = reactive<{ sections: HomeSection[] }>({
  sections: cloneSections(),
});

const activeSection = computed(
  () =>
    draft.sections.find((section) => section.key === activeKey.value) ||
    draft.sections[0]!,
);

const imageOptions = computed(() => {
  const options = new Map<string, string>();
  cmsState.media
    .filter((asset) => asset.type === 'image')
    .forEach((asset) => options.set(asset.sourceUrl || asset.url, asset.name));
  cmsState.content.forEach((item) => {
    if (item.cover && !options.has(item.cover))
      options.set(item.cover, item.title);
  });
  draft.sections.forEach((section) => {
    if (section.image && !options.has(section.image))
      options.set(section.image, `${section.label}图片`);
    if (section.mobileImage && !options.has(section.mobileImage))
      options.set(section.mobileImage, `${section.label}手机图片`);
  });
  return [...options.entries()].map(([value, label]) => ({ label, value }));
});

function readableSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

async function load() {
  loading.value = true;
  try {
    const [sections, media] = await Promise.all([
      listHomeSections(),
      listMedia(),
    ]);
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
            ? await getMediaPreviewUrl(item.adminUrl)
            : item.adminUrl,
      })),
    );
    cmsState.media
      .filter((item) => item.url.startsWith('blob:'))
      .forEach((item) => URL.revokeObjectURL(item.url));
    cmsState.media.splice(0, cmsState.media.length, ...mappedMedia);
    if (sections.length > 0) {
      const mapped: HomeSection[] = sections.map((item) => ({
        buttonLink: item.buttonTarget || '',
        buttonText: item.buttonLabel || '',
        description: item.description || '',
        enabled: item.enabled,
        eyebrow: item.eyebrow || '',
        highlight: item.highlightText || '',
        id: item.id,
        image: item.imageUrl || '',
        imageMediaId: item.imageMediaId || undefined,
        key: item.sectionKey as HomeSectionKey,
        label: item.label,
        mobileImage: item.mobileImageUrl || '',
        mobileMediaId: item.mobileMediaId || undefined,
        sortOrder: item.sortOrder,
        title: item.title,
        updatedAt: item.updatedAt,
        version: item.version,
      }));
      cmsState.homePage.sections.splice(
        0,
        cmsState.homePage.sections.length,
        ...mapped,
      );
      draft.sections.splice(
        0,
        draft.sections.length,
        ...structuredClone(mapped),
      );
      activeKey.value = mapped[0]?.key || 'about';
    }
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (!activeSection.value?.title.trim()) {
    ElMessage.warning('请填写区块标题');
    return;
  }
  saving.value = true;
  try {
    for (const [index, section] of draft.sections.entries()) {
      await saveHomeSection(section.id || null, {
        buttonLabel: section.buttonText || null,
        buttonTarget: section.buttonLink || null,
        description:
          section.key === 'about' ? section.description || null : null,
        enabled: section.enabled,
        eyebrow: section.eyebrow || null,
        highlightText: section.highlight || null,
        imageMediaId:
          section.imageMediaId ?? mediaIdFromUrl(section.image) ?? null,
        label: section.label,
        mobileMediaId:
          section.mobileMediaId ?? mediaIdFromUrl(section.mobileImage) ?? null,
        sectionKey: section.key,
        sortOrder: section.sortOrder ?? (index + 1) * 10,
        title: section.title,
        version: section.version,
      });
    }
    await load();
    ElMessage.success('首页内容配置已保存到后端');
  } finally {
    saving.value = false;
  }
}

async function removeActiveSection() {
  const current = activeSection.value;
  if (!current?.id) return;
  await deleteHomeSection(current.id);
  const index = draft.sections.findIndex((item) => item.id === current.id);
  if (index !== -1) draft.sections.splice(index, 1);
  const savedIndex = cmsState.homePage.sections.findIndex(
    (item) => item.id === current.id,
  );
  if (savedIndex !== -1) cmsState.homePage.sections.splice(savedIndex, 1);
  activeKey.value = draft.sections[0]?.key || 'about';
  ElMessage.success('首页区块已删除');
}

function restore() {
  draft.sections.splice(0, draft.sections.length, ...cloneSections());
  ElMessage.info('已恢复到上次保存的内容');
}

function previewUrl(value: string) {
  const id = mediaIdFromUrl(value);
  return cmsState.media.find((item) => item.id === id)?.url || value;
}

onMounted(load);
</script>

<template>
  <div class="home-editor-page">
    <section class="page-header">
      <div>
        <p>HOMEPAGE CONTENT</p>
        <h1>首页内容配置</h1>
        <span>固定首页版式，可维护各区块标题、图片和展示状态</span>
      </div>
      <div class="header-actions">
        <ElPopconfirm
          v-if="activeSection?.id"
          title="确定删除当前首页区块？"
          @confirm="removeActiveSection"
        >
          <template #reference>
            <ElButton plain size="large" type="danger">删除当前区块</ElButton>
          </template>
        </ElPopconfirm>
        <ElButton plain size="large" @click="restore">撤销未保存修改</ElButton>
        <ElButton :loading="saving" size="large" type="primary" @click="save">
          保存全部配置
        </ElButton>
      </div>
    </section>

    <ElAlert
      :closable="false"
      class="mode-alert"
      show-icon
      title="配置已连接后端；保存时会逐项校验乐观锁版本并写入官网数据源。"
      type="success"
    />

    <div class="editor-layout" v-loading="loading">
      <ElCard class="section-nav" shadow="never">
        <div class="nav-title">
          <strong>首页区块</strong>
          <span>{{
              draft.sections.filter((item) => item.enabled).length
            }}
            个已启用</span>
        </div>
        <button
          v-for="section in draft.sections"
          :key="section.key"
          :class="{ active: section.key === activeKey }"
          class="section-nav-item"
          type="button"
          @click="activeKey = section.key"
        >
          <span><i></i>{{ section.label }}</span>
          <ElTag
            :type="section.enabled ? 'success' : 'info'"
            effect="plain"
            size="small"
          >
            {{ section.enabled ? '显示' : '隐藏' }}
          </ElTag>
        </button>
        <div class="nav-note">
          <b>首屏轮播图</b>
          <span>请在左侧“Banner 管理”中维护每一张轮播图。</span>
        </div>
      </ElCard>

      <div class="editor-main">
        <ElCard class="form-card" shadow="never">
          <div class="card-heading">
            <div>
              <p>{{ activeSection.key.toUpperCase() }}</p>
              <h2>{{ activeSection.label }}</h2>
            </div>
            <div class="visibility-control">
              <span>首页显示</span>
              <ElSwitch v-model="activeSection.enabled" />
            </div>
          </div>

          <ElForm :model="activeSection" label-position="top">
            <ElRow :gutter="18">
              <ElCol :md="8" :xs="24">
                <ElFormItem label="栏目小标题">
                  <ElInput
                    v-model="activeSection.eyebrow"
                    maxlength="24"
                    show-word-limit
                  />
                </ElFormItem>
              </ElCol>
              <ElCol :md="10" :xs="24">
                <ElFormItem label="主标题" required>
                  <ElInput
                    v-model="activeSection.title"
                    maxlength="40"
                    show-word-limit
                  />
                </ElFormItem>
              </ElCol>
              <ElCol :md="6" :xs="24">
                <ElFormItem label="强调文字">
                  <ElInput v-model="activeSection.highlight" maxlength="24" />
                </ElFormItem>
              </ElCol>
              <ElCol v-if="activeSection.key === 'about'" :span="24">
                <ElFormItem label="介绍文字">
                  <ElInput
                    v-model="activeSection.description"
                    :rows="4"
                    maxlength="500"
                    show-word-limit
                    type="textarea"
                  />
                </ElFormItem>
              </ElCol>
              <ElCol :md="12" :xs="24">
                <ElFormItem label="PC 端展示图片">
                  <ElSelect
                    v-model="activeSection.image"
                    allow-create
                    clearable
                    filterable
                    placeholder="从素材库选择或粘贴图片地址"
                    style="width: 100%"
                  >
                    <ElOption
                      v-for="image in imageOptions"
                      :key="image.value"
                      :label="image.label"
                      :value="image.value"
                    />
                  </ElSelect>
                </ElFormItem>
              </ElCol>
              <ElCol :md="12" :xs="24">
                <ElFormItem label="手机端展示图片">
                  <ElSelect
                    v-model="activeSection.mobileImage"
                    allow-create
                    clearable
                    filterable
                    placeholder="不设置时使用 PC 端图片"
                    style="width: 100%"
                  >
                    <ElOption
                      v-for="image in imageOptions"
                      :key="image.value"
                      :label="image.label"
                      :value="image.value"
                    />
                  </ElSelect>
                </ElFormItem>
              </ElCol>
            </ElRow>
          </ElForm>
        </ElCard>

        <ElCard class="preview-card" shadow="never">
          <div class="preview-heading">
            <div>
              <strong>区块效果预览</strong><span>用于核对内容层级，实际样式以前台首页为准</span>
            </div>
            <ElTag :type="activeSection.enabled ? 'success' : 'info'">
              {{ activeSection.enabled ? '已启用' : '已隐藏' }}
            </ElTag>
          </div>
          <div
            :class="{ muted: !activeSection.enabled }"
            class="section-preview"
          >
            <div class="preview-copy">
              <p>{{ activeSection.eyebrow || '栏目小标题' }}</p>
              <h3>
                {{ activeSection.title || '区块标题' }}
                <em v-if="activeSection.highlight">{{
                  activeSection.highlight
                }}</em>
              </h3>
              <span
                v-if="
                  activeSection.key === 'about' && activeSection.description
                "
                >{{ activeSection.description }}</span>
            </div>
            <div class="preview-media">
              <ElImage
                v-if="activeSection.image"
                :preview-src-list="[previewUrl(activeSection.image)]"
                :src="previewUrl(activeSection.image)"
                fit="cover"
              />
              <div v-else>此区块暂未配置主图</div>
            </div>
          </div>
        </ElCard>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-editor-page {
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
    radial-gradient(circle at 84% 0, rgb(69 178 166 / 28%), transparent 28%),
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

.page-header span {
  color: rgb(255 255 255 / 68%);
}

.header-actions {
  display: flex;
  gap: 10px;
}

.header-actions :deep(.el-button.is-plain) {
  color: #fff;
  background: rgb(255 255 255 / 8%);
  border-color: rgb(255 255 255 / 26%);
}

.header-actions :deep(.el-button--primary) {
  --el-button-bg-color: #e85d45;
  --el-button-border-color: #e85d45;
}

.mode-alert {
  margin-top: 16px;
}

.editor-layout {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.section-nav,
.form-card,
.preview-card {
  border: 1px solid #e4eaec;
  border-radius: 16px;
}

.section-nav :deep(.el-card__body) {
  padding: 12px;
}

.nav-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 8px 14px;
}

.nav-title strong {
  font-size: 16px;
  color: #17363d;
}

.nav-title span {
  font-size: 12px;
  color: #859398;
}

.section-nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px;
  margin-bottom: 4px;
  color: #41585e;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 10px;
}

.section-nav-item > span {
  display: flex;
  gap: 9px;
  align-items: center;
  font-weight: 600;
}

.section-nav-item i {
  width: 7px;
  height: 7px;
  background: #c5d0d2;
  border-radius: 50%;
}

.section-nav-item:hover {
  background: #f2f7f7;
}

.section-nav-item.active {
  color: #0d6266;
  background: #e5f2f1;
}

.section-nav-item.active i {
  background: #e85d45;
  box-shadow: 0 0 0 4px rgb(232 93 69 / 12%);
}

.nav-note {
  padding: 14px;
  margin-top: 16px;
  background: #f5f7f8;
  border-radius: 10px;
}

.nav-note b,
.nav-note span {
  display: block;
}

.nav-note b {
  font-size: 13px;
  color: #385157;
}

.nav-note span {
  margin-top: 5px;
  font-size: 12px;
  line-height: 1.6;
  color: #859398;
}

.editor-main {
  min-width: 0;
}

.form-card :deep(.el-card__body),
.preview-card :deep(.el-card__body) {
  padding: 24px;
}

.card-heading,
.preview-heading {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 22px;
}

.card-heading p {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  color: #e85d45;
  letter-spacing: 0.14em;
}

.card-heading h2 {
  margin: 4px 0 0;
  font-size: 22px;
  color: #17363d;
}

.visibility-control {
  display: flex;
  gap: 10px;
  align-items: center;
  font-size: 13px;
  color: #64767b;
}

.preview-card {
  margin-top: 16px;
}

.preview-heading strong,
.preview-heading span {
  display: block;
}

.preview-heading strong {
  font-size: 16px;
  color: #17363d;
}

.preview-heading span {
  margin-top: 4px;
  font-size: 12px;
  color: #8a979b;
}

.section-preview {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  min-height: 280px;
  overflow: hidden;
  background: #0b3540;
  border-radius: 14px;
  transition: opacity 0.2s;
}

.section-preview.muted {
  opacity: 0.5;
}

.preview-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  min-width: 0;
  padding: 38px;
  color: #fff;
}

.preview-copy p {
  margin: 0 0 10px;
  font-size: 11px;
  font-weight: 700;
  color: #73d0c6;
  letter-spacing: 0.14em;
}

.preview-copy h3 {
  margin: 0;
  font-size: clamp(24px, 3vw, 38px);
  line-height: 1.15;
}

.preview-copy h3 em {
  display: block;
  font-style: normal;
  color: #ef624d;
}

.preview-copy > span {
  max-width: 600px;
  margin-top: 15px;
  line-height: 1.7;
  color: rgb(255 255 255 / 68%);
}

.preview-media {
  min-height: 280px;
  background: linear-gradient(145deg, #dfe9e9, #edf2f2);
}

.preview-media :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.preview-media > div {
  display: grid;
  place-items: center;
  height: 100%;
  color: #8a989b;
}

@media (max-width: 960px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }

  .section-nav :deep(.el-card__body) {
    display: flex;
    gap: 6px;
    overflow-x: auto;
  }

  .nav-title,
  .nav-note {
    display: none;
  }

  .section-nav-item {
    flex: 0 0 auto;
    gap: 14px;
    width: auto;
    margin: 0;
  }
}

@media (max-width: 760px) {
  .home-editor-page {
    padding: 14px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    padding: 22px;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions :deep(.el-button) {
    flex: 1;
  }

  .section-preview {
    grid-template-columns: 1fr;
  }

  .preview-media {
    min-height: 220px;
  }

  .preview-copy {
    padding: 28px;
  }
}
</style>
