<script lang="ts" setup>
import type { UploadFile, UploadRawFile } from 'element-plus';
import type {
  BaseFacilityRecord,
  ContentPageRecord,
  MediaRecord,
  TeamMemberRecord,
} from '#/api';

import { computed, reactive, ref } from 'vue';

import {
  ElButton,
  ElCard,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElImage,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElPopconfirm,
  ElSelect,
  ElSwitch,
  ElTag,
  ElUpload,
} from 'element-plus';

import {
  deleteBaseFacility,
  deleteTeamMember,
  listBaseFacilities,
  listContentPages,
  listMedia,
  listTeamMembers,
  saveBaseFacility,
  saveContentPage,
  saveTeamMember,
  uploadMedia,
} from '#/api';

interface HistoryItemForm {
  date: string;
  imageMediaId?: number;
  imageUrl?: string;
  text: string;
  title: string;
}

interface AboutContentData {
  historyItems?: unknown;
  [key: string]: unknown;
}

interface ImageOption {
  id: number;
  name: string;
}

interface TeamMemberForm {
  bio: string;
  enabled: boolean;
  id: null | number;
  name: string;
  photoMediaId?: number;
  photoUrl?: string;
  role: string;
  sortOrder: number;
  version?: number;
}

interface BaseFacilityForm {
  address: string;
  enabled: boolean;
  id: null | number;
  imageMediaId?: number;
  imageUrl?: string;
  name: string;
  sortOrder: number;
  version?: number;
}

const MAX_IMAGE_SIZE = 30 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set([
  'image/gif',
  'image/jpeg',
  'image/png',
  'image/webp',
]);

const loading = ref(false);
const saving = ref(false);
const uploadingIndex = ref<number>();
const page = ref<ContentPageRecord>();
const originalData = ref<AboutContentData>({});
const mediaOptions = ref<ImageOption[]>([]);
const mediaLoaded = ref(false);
const teamMembers = ref<TeamMemberRecord[]>([]);
const baseFacilities = ref<BaseFacilityRecord[]>([]);
const teamDialogVisible = ref(false);
const facilityDialogVisible = ref(false);
const savingTeam = ref(false);
const savingFacility = ref(false);
const entityUploading = ref('');
const form = reactive<{ historyItems: HistoryItemForm[] }>({
  historyItems: [],
});
const teamForm = reactive<TeamMemberForm>(emptyTeamMemberForm());
const facilityForm = reactive<BaseFacilityForm>(emptyBaseFacilityForm());

const hasPage = computed(() => Boolean(page.value));

function emptyHistoryItem(): HistoryItemForm {
  return { date: '', imageMediaId: undefined, imageUrl: '', text: '', title: '' };
}

function emptyTeamMemberForm(): TeamMemberForm {
  return {
    bio: '', enabled: true, id: null, name: '', photoMediaId: undefined,
    photoUrl: '', role: '', sortOrder: 10, version: undefined,
  };
}

function emptyBaseFacilityForm(): BaseFacilityForm {
  return {
    address: '', enabled: true, id: null, imageMediaId: undefined,
    imageUrl: '', name: '', sortOrder: 10, version: undefined,
  };
}

function normalizeHistoryItem(value: unknown): HistoryItemForm {
  const item = (value || {}) as Record<string, unknown>;
  const imageMediaId = Number(item.imageMediaId);
  return {
    date: String(item.date || ''),
    imageMediaId:
      Number.isSafeInteger(imageMediaId) && imageMediaId > 0
        ? imageMediaId
        : undefined,
    imageUrl: String(item.imageUrl || ''),
    text: String(item.text || ''),
    title: String(item.title || ''),
  };
}

function contentDataFromPage(value?: ContentPageRecord): AboutContentData {
  const data = value?.contentData;
  return data && typeof data === 'object' && !Array.isArray(data)
    ? (data as AboutContentData)
    : {};
}

function publicMediaUrl(mediaId?: number) {
  return mediaId ? `/api/v1/public/media/${mediaId}` : '';
}

function imagePreview(item: HistoryItemForm) {
  return publicMediaUrl(item.imageMediaId) || item.imageUrl || '';
}

function imageAlt(item: HistoryItemForm, index: number) {
  return item.title || item.date || `发展历程图片 ${index + 1}`;
}

function selectedImageName(mediaId?: number) {
  return mediaOptions.value.find((item) => item.id === mediaId)?.name || '';
}

async function ensureMediaOptions() {
  if (mediaLoaded.value) return;
  const records = await listMedia('', { page: 1, size: 100 });
  mediaOptions.value = records
    .filter((item: MediaRecord) => item.mediaType === 'IMAGE')
    .map((item) => ({ id: item.id, name: item.originalFilename }));
  mediaLoaded.value = true;
}

async function load() {
  loading.value = true;
  try {
    const [pages, teams, facilities] = await Promise.all([
      listContentPages(), listTeamMembers(), listBaseFacilities(),
    ]);
    teamMembers.value = teams;
    baseFacilities.value = facilities;
    const about = pages.find((item) => item.pageKey === 'about');
    if (!about) {
      page.value = undefined;
      form.historyItems = [];
      return;
    }
    page.value = about;
    originalData.value = contentDataFromPage(about);
    const historyItems = originalData.value.historyItems;
    form.historyItems = Array.isArray(historyItems)
      ? historyItems.map(normalizeHistoryItem)
      : [];
    await ensureMediaOptions();
  } finally {
    loading.value = false;
  }
}

function addHistoryItem() {
  form.historyItems.push(emptyHistoryItem());
}

function removeHistoryItem(index: number) {
  form.historyItems.splice(index, 1);
}

function moveHistoryItem(index: number, direction: -1 | 1) {
  const target = index + direction;
  if (target < 0 || target >= form.historyItems.length) return;
  const [item] = form.historyItems.splice(index, 1);
  form.historyItems.splice(target, 0, item!);
}

function selectImage(item: HistoryItemForm, mediaId?: number) {
  item.imageMediaId = mediaId || undefined;
  if (mediaId) item.imageUrl = publicMediaUrl(mediaId);
}

function validateImage(raw?: UploadRawFile) {
  if (!raw) return false;
  if (!ALLOWED_IMAGE_TYPES.has(raw.type)) {
    ElMessage.warning('请上传 JPG、PNG、WebP 或 GIF 图片');
    return false;
  }
  if (raw.size > MAX_IMAGE_SIZE) {
    ElMessage.warning('单张图片不能超过 30 MB');
    return false;
  }
  return true;
}

async function uploadHistoryImage(
  item: HistoryItemForm,
  index: number,
  uploadFile: UploadFile,
) {
  const raw = uploadFile.raw;
  if (!validateImage(raw)) return;
  uploadingIndex.value = index;
  try {
    const media = await uploadMedia(raw!, `${item.date || '发展历程'}图片`);
    item.imageMediaId = media.id;
    item.imageUrl = publicMediaUrl(media.id);
    if (!mediaOptions.value.some((option) => option.id === media.id)) {
      mediaOptions.value.unshift({ id: media.id, name: media.originalFilename });
    }
    ElMessage.success('图片已上传并选中');
  } finally {
    uploadingIndex.value = undefined;
  }
}

function addMediaOption(media: MediaRecord) {
  if (!mediaOptions.value.some((option) => option.id === media.id)) {
    mediaOptions.value.unshift({ id: media.id, name: media.originalFilename });
  }
}

function nextSortOrder(items: Array<{ sortOrder: number }>) {
  return Math.max(0, ...items.map((item) => item.sortOrder || 0)) + 10;
}

function openTeamEditor(member?: TeamMemberRecord) {
  Object.assign(teamForm, emptyTeamMemberForm(), {
    bio: member?.bio || '',
    enabled: member?.enabled ?? true,
    id: member?.id ?? null,
    name: member?.name || '',
    photoMediaId: member?.photoMediaId || undefined,
    photoUrl: member?.photoUrl || '',
    role: member?.role || '',
    sortOrder: member?.sortOrder ?? nextSortOrder(teamMembers.value),
    version: member?.version,
  });
  teamDialogVisible.value = true;
  void ensureMediaOptions();
}

function openFacilityEditor(facility?: BaseFacilityRecord) {
  Object.assign(facilityForm, emptyBaseFacilityForm(), {
    address: facility?.address || '',
    enabled: facility?.enabled ?? true,
    id: facility?.id ?? null,
    imageMediaId: facility?.imageMediaId || undefined,
    imageUrl: facility?.imageUrl || '',
    name: facility?.name || '',
    sortOrder: facility?.sortOrder ?? nextSortOrder(baseFacilities.value),
    version: facility?.version,
  });
  facilityDialogVisible.value = true;
  void ensureMediaOptions();
}

function teamImagePreview() {
  return publicMediaUrl(teamForm.photoMediaId) || teamForm.photoUrl || '';
}

function facilityImagePreview() {
  return publicMediaUrl(facilityForm.imageMediaId) || facilityForm.imageUrl || '';
}

async function uploadTeamPhoto(uploadFile: UploadFile) {
  const raw = uploadFile.raw;
  if (!validateImage(raw)) return;
  entityUploading.value = 'team';
  try {
    const media = await uploadMedia(raw!, `${teamForm.name || '团队成员'}头像`);
    teamForm.photoMediaId = media.id;
    teamForm.photoUrl = publicMediaUrl(media.id);
    addMediaOption(media);
    ElMessage.success('团队头像已上传并选中');
  } finally {
    entityUploading.value = '';
  }
}

async function uploadFacilityImage(uploadFile: UploadFile) {
  const raw = uploadFile.raw;
  if (!validateImage(raw)) return;
  entityUploading.value = 'facility';
  try {
    const media = await uploadMedia(raw!, `${facilityForm.name || '基地设施'}图片`);
    facilityForm.imageMediaId = media.id;
    facilityForm.imageUrl = publicMediaUrl(media.id);
    addMediaOption(media);
    ElMessage.success('基地图片已上传并选中');
  } finally {
    entityUploading.value = '';
  }
}

async function saveTeam() {
  if (!teamForm.role.trim() || !teamForm.name.trim()) {
    ElMessage.warning('请填写团队成员的职务和姓名');
    return;
  }
  savingTeam.value = true;
  try {
    await saveTeamMember(teamForm.id, {
      bio: teamForm.bio.trim() || null,
      enabled: teamForm.enabled,
      name: teamForm.name.trim(),
      photoMediaId: teamForm.photoMediaId || null,
      role: teamForm.role.trim(),
      sortOrder: teamForm.sortOrder,
      version: teamForm.version,
    });
    teamDialogVisible.value = false;
    ElMessage.success('团队成员已保存');
    await load();
  } finally {
    savingTeam.value = false;
  }
}

async function saveFacility() {
  if (!facilityForm.name.trim()) {
    ElMessage.warning('请填写基地名称');
    return;
  }
  savingFacility.value = true;
  try {
    await saveBaseFacility(facilityForm.id, {
      address: facilityForm.address.trim() || null,
      enabled: facilityForm.enabled,
      imageMediaId: facilityForm.imageMediaId || null,
      name: facilityForm.name.trim(),
      sortOrder: facilityForm.sortOrder,
      version: facilityForm.version,
    });
    facilityDialogVisible.value = false;
    ElMessage.success('基地设施已保存');
    await load();
  } finally {
    savingFacility.value = false;
  }
}

async function removeTeam(id: number) {
  await deleteTeamMember(id);
  ElMessage.success('团队成员已删除');
  await load();
}

async function removeFacility(id: number) {
  await deleteBaseFacility(id);
  ElMessage.success('基地设施已删除');
  await load();
}

async function save() {
  if (!page.value) return;
  const invalidIndex = form.historyItems.findIndex(
    (item) => !item.date.trim() || !imagePreview(item),
  );
  if (invalidIndex !== -1) {
    ElMessage.warning(`请为第 ${invalidIndex + 1} 条填写年份/日期并选择图片`);
    return;
  }

  saving.value = true;
  try {
    await saveContentPage(page.value.id, {
      category: page.value.category || null,
      contentData: {
        ...originalData.value,
        historyItems: form.historyItems.map((item) => ({
          date: item.date.trim(),
          imageMediaId: item.imageMediaId || null,
          imageUrl: item.imageMediaId ? null : item.imageUrl || null,
          text: item.text.trim() || null,
          title: item.title.trim() || null,
        })),
      },
      contentHtml: page.value.contentHtml || null,
      coverMediaId: page.value.coverMediaId || null,
      featured: Boolean(page.value.featured),
      pageKey: page.value.pageKey,
      seoDescription: page.value.seoDescription || null,
      seoKeywords: page.value.seoKeywords || null,
      seoTitle: page.value.seoTitle || null,
      sortOrder: Number(page.value.sortOrder || 0),
      summary: page.value.summary || null,
      title: page.value.title,
      version: page.value.version,
    });
    ElMessage.success('关于我们－发展历程已保存');
    await load();
  } finally {
    saving.value = false;
  }
}

void load();
</script>

<template>
  <div class="about-page-admin">
    <section class="about-page-hero">
      <div>
        <p>ABOUT CONTENT</p>
        <h1>关于我们</h1>
        <span>维护官网“发展历程”的日期、文字、图片与展示顺序。保存后官网会自动使用最新内容。</span>
      </div>
      <ElTag type="success">{{ form.historyItems.length }} 条历程</ElTag>
    </section>

    <ElCard v-loading="loading" class="history-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h2>发展历程</h2>
            <p>图片可从素材库选择或直接上传；拖动前请使用上下按钮调整官网展示顺序。</p>
          </div>
          <ElButton :disabled="!hasPage" plain type="primary" @click="addHistoryItem">
            + 新增历程
          </ElButton>
        </div>
      </template>

      <ElEmpty v-if="!hasPage" description="未找到 about 单页记录，请先完成数据库初始化" />
      <div v-else class="history-list">
        <article v-for="(item, index) in form.historyItems" :key="index" class="history-item-editor">
          <div class="history-item-index">{{ String(index + 1).padStart(2, '0') }}</div>
          <div class="history-item-fields">
            <div class="form-grid form-grid--top">
              <ElInput v-model="item.date" maxlength="30" placeholder="年份或日期，例如 2026年" />
              <ElInput v-model="item.title" maxlength="80" placeholder="标题（可留空）" />
            </div>
            <ElInput v-model="item.text" :rows="2" maxlength="300" placeholder="说明（可留空）" type="textarea" />
            <div class="image-control">
              <ElSelect
                :model-value="item.imageMediaId"
                clearable
                filterable
                placeholder="从素材库选择图片"
                @update:model-value="(value) => selectImage(item, Number(value) || undefined)"
              >
                <ElOption v-for="option in mediaOptions" :key="option.id" :label="option.name" :value="option.id" />
              </ElSelect>
              <ElUpload
                :auto-upload="false"
                :disabled="uploadingIndex === index"
                :show-file-list="false"
                accept="image/jpeg,image/png,image/webp,image/gif"
                @change="(file) => uploadHistoryImage(item, index, file)"
              >
                <ElButton :loading="uploadingIndex === index" plain>上传新图片</ElButton>
              </ElUpload>
              <small v-if="item.imageMediaId">当前素材：{{ selectedImageName(item.imageMediaId) || `#${item.imageMediaId}` }}</small>
            </div>
          </div>
          <ElImage v-if="imagePreview(item)" :alt="imageAlt(item, index)" :preview-src-list="[imagePreview(item)]" :src="imagePreview(item)" class="history-image-preview" fit="cover" />
          <div class="history-item-actions">
            <ElButton :disabled="index === 0" circle plain @click="moveHistoryItem(index, -1)">↑</ElButton>
            <ElButton :disabled="index === form.historyItems.length - 1" circle plain @click="moveHistoryItem(index, 1)">↓</ElButton>
            <ElButton circle plain type="danger" @click="removeHistoryItem(index)">×</ElButton>
          </div>
        </article>
      </div>

      <div v-if="hasPage" class="save-bar">
        <span>官网当前状态：{{ page?.status === 'PUBLISHED' ? '已发布' : '未发布' }}</span>
        <ElButton :loading="saving" type="primary" @click="save">保存发展历程</ElButton>
      </div>
    </ElCard>

    <section class="about-content-section">
      <div class="card-header">
        <div>
          <h2>核心研发团队</h2>
          <p>维护官网团队卡片的职务、姓名、简介、头像、排序及展示状态。</p>
        </div>
        <ElButton plain type="primary" @click="openTeamEditor()">+ 新增成员</ElButton>
      </div>
      <div class="about-card-grid" v-loading="loading">
        <ElCard v-for="member in teamMembers" :key="member.id" class="about-content-card" shadow="never">
          <ElImage v-if="member.photoUrl" :alt="member.name" :src="member.photoUrl" class="about-content-image" fit="cover" />
          <div class="about-content-copy">
            <div class="card-title-row">
              <span>{{ member.role }}</span>
              <ElTag :type="member.enabled ? 'success' : 'info'">{{ member.enabled ? '展示中' : '已隐藏' }}</ElTag>
            </div>
            <h3>{{ member.name }}</h3>
            <p>{{ member.bio || '暂未填写成员简介' }}</p>
            <small>排序：{{ member.sortOrder }}</small>
          </div>
          <div class="card-actions">
            <ElButton plain type="primary" @click="openTeamEditor(member)">编辑</ElButton>
            <ElPopconfirm title="确认删除该团队成员？" @confirm="removeTeam(member.id)">
              <template #reference><ElButton plain type="danger">删除</ElButton></template>
            </ElPopconfirm>
          </div>
        </ElCard>
      </div>
      <ElEmpty v-if="!loading && teamMembers.length === 0" description="暂无团队成员，可新增后在官网展示" />
    </section>

    <section class="about-content-section">
      <div class="card-header">
        <div>
          <h2>基地设施</h2>
          <p>维护“发展引擎”中的基地名称、地址、图片、排序及展示状态。</p>
        </div>
        <ElButton plain type="primary" @click="openFacilityEditor()">+ 新增基地</ElButton>
      </div>
      <div class="about-card-grid" v-loading="loading">
        <ElCard v-for="facility in baseFacilities" :key="facility.id" class="about-content-card" shadow="never">
          <ElImage v-if="facility.imageUrl" :alt="facility.name" :src="facility.imageUrl" class="about-content-image" fit="cover" />
          <div class="about-content-copy">
            <div class="card-title-row">
              <span>基地设施</span>
              <ElTag :type="facility.enabled ? 'success' : 'info'">{{ facility.enabled ? '展示中' : '已隐藏' }}</ElTag>
            </div>
            <h3>{{ facility.name }}</h3>
            <p>{{ facility.address || '暂未填写地址' }}</p>
            <small>排序：{{ facility.sortOrder }}</small>
          </div>
          <div class="card-actions">
            <ElButton plain type="primary" @click="openFacilityEditor(facility)">编辑</ElButton>
            <ElPopconfirm title="确认删除该基地设施？" @confirm="removeFacility(facility.id)">
              <template #reference><ElButton plain type="danger">删除</ElButton></template>
            </ElPopconfirm>
          </div>
        </ElCard>
      </div>
      <ElEmpty v-if="!loading && baseFacilities.length === 0" description="暂无基地设施，可新增后在官网展示" />
    </section>

    <ElDialog v-model="teamDialogVisible" :close-on-click-modal="false" :title="teamForm.id ? '编辑团队成员' : '新增团队成员'" width="640px">
      <ElForm label-position="top">
        <div class="form-grid">
          <ElFormItem label="职务" required><ElInput v-model="teamForm.role" maxlength="100" placeholder="例如：技术带头人" /></ElFormItem>
          <ElFormItem label="姓名" required><ElInput v-model="teamForm.name" maxlength="100" placeholder="例如：周科朝" /></ElFormItem>
        </div>
        <ElFormItem label="成员简介"><ElInput v-model="teamForm.bio" :rows="4" maxlength="1000" show-word-limit type="textarea" /></ElFormItem>
        <div class="form-grid">
          <ElFormItem label="展示顺序"><ElInputNumber v-model="teamForm.sortOrder" :min="0" style="width: 100%" /></ElFormItem>
          <ElFormItem label="官网展示"><ElSwitch v-model="teamForm.enabled" active-text="展示" inactive-text="隐藏" /></ElFormItem>
        </div>
        <ElFormItem label="成员头像">
          <div class="dialog-media-control">
            <ElSelect :model-value="teamForm.photoMediaId" clearable filterable placeholder="从素材库选择头像" @update:model-value="(value) => { teamForm.photoMediaId = Number(value) || undefined; teamForm.photoUrl = publicMediaUrl(teamForm.photoMediaId); }">
              <ElOption v-for="option in mediaOptions" :key="option.id" :label="option.name" :value="option.id" />
            </ElSelect>
            <ElUpload :auto-upload="false" :disabled="entityUploading === 'team'" :show-file-list="false" accept="image/jpeg,image/png,image/webp,image/gif" @change="uploadTeamPhoto">
              <ElButton :loading="entityUploading === 'team'" plain>上传头像</ElButton>
            </ElUpload>
          </div>
          <ElImage v-if="teamImagePreview()" :src="teamImagePreview()" class="dialog-image-preview" fit="cover" />
        </ElFormItem>
      </ElForm>
      <template #footer><ElButton @click="teamDialogVisible = false">取消</ElButton><ElButton :loading="savingTeam" type="primary" @click="saveTeam">保存成员</ElButton></template>
    </ElDialog>

    <ElDialog v-model="facilityDialogVisible" :close-on-click-modal="false" :title="facilityForm.id ? '编辑基地设施' : '新增基地设施'" width="640px">
      <ElForm label-position="top">
        <ElFormItem label="基地名称" required><ElInput v-model="facilityForm.name" maxlength="255" placeholder="例如：湖南省浏阳市研发基地" /></ElFormItem>
        <ElFormItem label="详细地址"><ElInput v-model="facilityForm.address" :rows="2" maxlength="500" show-word-limit type="textarea" /></ElFormItem>
        <div class="form-grid">
          <ElFormItem label="展示顺序"><ElInputNumber v-model="facilityForm.sortOrder" :min="0" style="width: 100%" /></ElFormItem>
          <ElFormItem label="官网展示"><ElSwitch v-model="facilityForm.enabled" active-text="展示" inactive-text="隐藏" /></ElFormItem>
        </div>
        <ElFormItem label="基地图片">
          <div class="dialog-media-control">
            <ElSelect :model-value="facilityForm.imageMediaId" clearable filterable placeholder="从素材库选择图片" @update:model-value="(value) => { facilityForm.imageMediaId = Number(value) || undefined; facilityForm.imageUrl = publicMediaUrl(facilityForm.imageMediaId); }">
              <ElOption v-for="option in mediaOptions" :key="option.id" :label="option.name" :value="option.id" />
            </ElSelect>
            <ElUpload :auto-upload="false" :disabled="entityUploading === 'facility'" :show-file-list="false" accept="image/jpeg,image/png,image/webp,image/gif" @change="uploadFacilityImage">
              <ElButton :loading="entityUploading === 'facility'" plain>上传图片</ElButton>
            </ElUpload>
          </div>
          <ElImage v-if="facilityImagePreview()" :src="facilityImagePreview()" class="dialog-image-preview" fit="cover" />
        </ElFormItem>
      </ElForm>
      <template #footer><ElButton @click="facilityDialogVisible = false">取消</ElButton><ElButton :loading="savingFacility" type="primary" @click="saveFacility">保存基地</ElButton></template>
    </ElDialog>
  </div>
</template>

<style scoped>
.about-page-admin { min-height: 100%; padding: 24px; background: #f5f7f8; }
.about-page-hero { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 30px; color: #fff; background: radial-gradient(circle at 82% 20%, rgb(109 182 137 / 32%), transparent 26%), linear-gradient(120deg, #123d36, #1e715f); border-radius: 18px; box-shadow: 0 18px 50px rgb(18 61 54 / 16%); }
.about-page-hero p { margin: 0; font-size: 11px; font-weight: 700; color: #a7ebc1; letter-spacing: .18em; }
.about-page-hero h1 { margin: 5px 0 8px; font-size: 28px; }
.about-page-hero span { display: block; max-width: 700px; line-height: 1.7; color: rgb(255 255 255 / 74%); }
.history-card { margin-top: 24px; border: 1px solid #e2e8ea; border-radius: 16px; }
.card-header, .save-bar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.card-header h2 { margin: 0; color: #17343b; }
.card-header p { margin: 6px 0 0; font-size: 13px; color: #7e8a8f; }
.history-list { display: grid; gap: 14px; }
.history-item-editor { display: grid; grid-template-columns: 42px minmax(0, 1fr) 190px auto; gap: 14px; align-items: start; padding: 16px; background: #f6f8f8; border: 1px solid #e6eeee; border-radius: 12px; }
.history-item-index { display: grid; place-items: center; width: 38px; height: 38px; color: #fff; font-size: 12px; font-weight: 800; background: #1e715f; border-radius: 10px; }
.history-item-fields { display: grid; gap: 10px; }
.form-grid { display: grid; grid-template-columns: minmax(150px, .5fr) minmax(220px, 1fr); gap: 10px; }
.image-control { display: grid; grid-template-columns: minmax(180px, 1fr) auto; gap: 10px; align-items: center; }
.image-control small { grid-column: 1 / -1; color: #849195; }
.history-image-preview { width: 190px; height: 128px; border-radius: 8px; background: #e8eeee; }
.history-item-actions { display: grid; gap: 8px; }
.save-bar { padding-top: 20px; margin-top: 20px; border-top: 1px solid #e7eeee; color: #718187; }
.about-content-section { margin-top: 30px; }
.about-content-section > .card-header { margin-bottom: 14px; }
.about-card-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.about-content-card { overflow: hidden; border: 1px solid #e2e8ea; border-radius: 14px; }
.about-content-card :deep(.el-card__body) { display: grid; grid-template-columns: 128px minmax(0, 1fr); gap: 16px; padding: 16px; }
.about-content-image { width: 128px; height: 150px; border-radius: 9px; background: #eef3f3; }
.about-content-copy { min-width: 0; }
.about-content-copy .card-title-row span { color: #1e715f; font-size: 13px; font-weight: 700; }
.about-content-copy h3 { margin: 8px 0; color: #17343b; font-size: 18px; }
.about-content-copy p { min-height: 40px; margin: 0; color: #718187; font-size: 13px; line-height: 1.65; }
.about-content-copy small { display: block; margin-top: 8px; color: #9aa6aa; }
.about-content-card .card-actions { grid-column: 1 / -1; padding-top: 0; }
.dialog-media-control { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 10px; width: 100%; }
.dialog-image-preview { width: 180px; height: 120px; margin-top: 12px; border-radius: 8px; background: #eef3f3; }
@media (max-width: 900px) { .history-item-editor { grid-template-columns: 42px minmax(0, 1fr) auto; } .history-image-preview { grid-column: 2; } .about-card-grid { grid-template-columns: 1fr; } }
@media (max-width: 640px) { .about-page-admin { padding: 14px; } .about-page-hero, .card-header, .save-bar { align-items: flex-start; flex-direction: column; } .history-item-editor, .form-grid, .image-control, .dialog-media-control { grid-template-columns: 1fr; } .history-item-index, .history-image-preview { grid-column: auto; } .history-image-preview { width: 100%; height: 180px; } .history-item-actions { display: flex; } .about-content-card :deep(.el-card__body) { grid-template-columns: 1fr; } .about-content-image { width: 100%; height: 180px; } }
</style>
