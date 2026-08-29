<script lang="ts" setup>
import type { UploadFile, UploadFiles, UploadRawFile } from 'element-plus';

import type { ArticleCategoryRecord } from '#/api';
import type {
  ContentItem,
  ContentResource,
  ContentStatus,
  MediaAsset,
} from '#/data/cms';

import { computed, reactive, ref, toRaw, watch } from 'vue';

import {
  ElButton,
  ElCard,
  ElCol,
  ElDialog,
  ElDivider,
  ElEmpty,
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
  ElUpload,
} from 'element-plus';

import {
  changeContentStatus,
  deleteContent,
  getContent,
  getMediaPreviewUrl,
  listArticleCategories,
  listCaseCategories,
  listContent,
  listMedia,
  listProductCategories,
  saveContent,
  uploadMedia,
} from '#/api';
import RichTextEditor from '#/components/rich-text-editor.vue';
import { cmsState, resourceMeta } from '#/data/cms';
import {
  contentFromBackend,
  contentPayload,
  mediaIdFromUrl,
} from '#/data/cms-adapter';

const props = defineProps<{ resource: ContentResource }>();

const keyword = ref('');
const status = ref<'all' | ContentStatus>('all');
const category = ref('all');
const dialogVisible = ref(false);
const activeId = ref<null | number>(null);
const loading = ref(false);
const saving = ref(false);
const referenceOptions = ref<Array<{ id: number; name: string }>>([]);
const relatedOptions = ref<Array<{ id: number; name: string }>>([]);
const sceneOptions = ref<Array<{ id: number; name: string }>>([]);
const originalStatus = ref<ContentStatus>('draft');
const featuresText = ref('');
const capabilityRowsText = ref('');
const pillarsText = ref('');
const contentEditorRef = ref<{ insertHtml: (html: string) => void }>();
const inlineImageDialogVisible = ref(false);
const contentPreviewVisible = ref(false);
const selectedInlineMediaId = ref<number>();
const inlineImageAlt = ref('');
const inlineImageCaption = ref('');
const inlineImageUploading = ref(false);
const inlineImageUploadQueue = ref<UploadRawFile[]>([]);
const inlineVideoDialogVisible = ref(false);
const selectedInlineVideoId = ref<number>();
const inlineVideoCaption = ref('');
const inlineVideoUploading = ref(false);
const inlineVideoUploadQueue = ref<UploadRawFile[]>([]);
const coverImageUploading = ref(false);
const mediaOptionsLoading = ref(false);
const mediaOptionsLoaded = ref(false);
const coverUploadFileList = ref<UploadFile[]>([]);
let inlineImageUploadSchedule: ReturnType<typeof setTimeout> | undefined;
let inlineVideoUploadSchedule: ReturnType<typeof setTimeout> | undefined;

const MAX_IMAGE_SIZE = 30 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set([
  'image/gif',
  'image/jpeg',
  'image/png',
  'image/webp',
]);
const MAX_VIDEO_SIZE = 200 * 1024 * 1024;
const ALLOWED_VIDEO_TYPES = new Set([
  'video/mp4',
  'video/mpeg',
  'video/ogg',
  'video/quicktime',
  'video/webm',
  'video/x-m4v',
  'video/x-matroska',
  'video/x-msvideo',
]);

const meta = computed(() => resourceMeta[props.resource]);
const isHomePlacementResource = computed(() => props.resource === 'articles');
const supportsCoverImageUpload = computed(() =>
  ['articles', 'cases', 'products', 'scenes'].includes(props.resource),
);
const supportsDelete = computed(() => props.resource !== 'technologies');
const categoryOptions = computed(() =>
  referenceOptions.value.length > 0
    ? referenceOptions.value.map((item) => item.name)
    : meta.value.categories,
);
const resourceItems = computed(() =>
  cmsState.content.filter((item) => item.resource === props.resource),
);
const filteredItems = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return resourceItems.value
    .filter((item) => status.value === 'all' || item.status === status.value)
    .filter(
      (item) => category.value === 'all' || item.category === category.value,
    )
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
  featured: resourceItems.value.filter((item) =>
    isHomePlacementResource.value
      ? item.homePinned || item.showOnHome
      : item.featured,
  ).length,
  published: resourceItems.value.filter((item) => item.status === 'published')
    .length,
  total: resourceItems.value.length,
}));

const imageOptions = computed(() =>
  cmsState.media.filter((asset) => asset.type === 'image'),
);
const videoOptions = computed(() =>
  cmsState.media.filter((asset) => asset.type === 'video'),
);
const inlineMediaPreviewUrls = computed<Record<number, string>>(() => {
  const urls: Record<number, string> = {};
  for (const asset of imageOptions.value) {
    if (asset.url) urls[asset.id] = asset.url;
  }
  return urls;
});
const selectedInlineMedia = computed(() =>
  imageOptions.value.find((asset) => asset.id === selectedInlineMediaId.value),
);
const selectedInlineVideo = computed(() =>
  videoOptions.value.find((asset) => asset.id === selectedInlineVideoId.value),
);
const emptyForm = (): ContentItem => ({
  category: categoryOptions.value[0] || '未分类',
  categoryId: undefined,
  categoryIds: [],
  contentHtml: '',
  cover: '',
  coverMediaId: undefined,
  createdAt: '',
  eyebrow: '',
  featured: false,
  highlightTitle: '',
  homePinned: false,
  homeSortOrder: resourceItems.value.length + 1,
  id: 0,
  mobileCover: '',
  mobileMediaId: undefined,
  primaryActionLabel: '',
  primaryActionLink: '',
  raw: {},
  resource: props.resource,
  secondaryActionLabel: '',
  secondaryActionLink: '',
  seoDescription: '',
  seoKeywords: '奥飞新材,气凝胶',
  seoTitle: '',
  showOnHome: false,
  slug: '',
  sortOrder: resourceItems.value.length + 1,
  status: 'draft',
  summary: '',
  title: '',
  updatedAt: '',
  version: undefined,
});

const form = reactive<ContentItem>(emptyForm());
const rawForm = computed<Record<string, any>>(() => form.raw || {});
const contentPreviewDocument = computed(() => {
  const content = (form.contentHtml || '').replaceAll(
    /(["'])alfy-media:(\d+)\1/gi,
    (source, quote: string, id: string) => {
      const asset = imageOptions.value.find((item) => item.id === Number(id));
      return asset?.url ? `${quote}${asset.url}${quote}` : source;
    },
  );
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src blob: data: http: https:; media-src blob: data: http: https:; style-src 'unsafe-inline'">
  <style>
    * { box-sizing: border-box; }
    body { max-width: 820px; margin: 0 auto; padding: 28px; color: #3e4750; font: 16px/1.85 Inter, "Noto Sans SC", "Source Han Sans SC", "PingFang SC", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif; overflow-wrap: anywhere; }
    h1, h2, h3 { color: #152f36; line-height: 1.35; letter-spacing: -.035em; }
    h1 { margin: 44px 0 20px; font-size: 36px; }
    h2 { margin: 42px 0 18px; font-size: 28px; }
    h3 { margin: 34px 0 14px; font-size: 22px; }
    body > :first-child { margin-top: 0; }
    body > :last-child { margin-bottom: 0; }
    p { margin: 0 0 18px; }
    ul, ol { margin: 18px 0; padding-left: 2em; }
    li { margin: 6px 0; }
    figure { margin: 32px 0; }
    img, video { display: block; width: 100%; max-width: 100%; height: auto; }
    figcaption { margin-top: 10px; color: #78868b; font-size: 13px; line-height: 1.6; text-align: center; }
    blockquote { margin: 24px 0; padding: 4px 0 4px 18px; color: #52636b; border-left: 3px solid #0e7478; }
    hr { margin: 32px 0; border: 0; border-top: 1px solid #dfe3e6; }
    pre { margin: 24px 0; padding: 18px 20px; background: #e9edef; overflow-x: auto; }
    code { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }
    table { width: 100%; margin: 24px 0; border-collapse: collapse; }
    th, td { padding: 12px 14px; border: 1px solid #dfe3e6; text-align: left; }
    a { color: #e51b23; text-decoration: underline; text-underline-offset: 3px; }
    [data-font="sans"] { font-family: Inter, "Noto Sans SC", "Microsoft YaHei", sans-serif; }
    [data-font="serif"] { font-family: Georgia, "Noto Serif SC", "Songti SC", serif; }
    [data-font="mono"] { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }
    [data-size="small"] { font-size: .875em; }
    [data-size="large"] { font-size: 1.25em; }
    [data-size="xlarge"] { font-size: 1.5em; }
    [data-color="black"] { color: #202124; }
    [data-color="red"] { color: #c62828; }
    [data-color="orange"] { color: #e67e22; }
    [data-color="green"] { color: #1b7f5f; }
    [data-color="blue"] { color: #2563eb; }
    [data-color="gray"] { color: #6b7280; }
    [data-highlight="yellow"] { background: #fff0a6; }
    [data-highlight="blue"] { background: #cfe8ff; }
    [data-highlight="green"] { background: #d8f3dc; }
    [data-align="center"] { text-align: center; }
    [data-align="right"] { text-align: right; }
    [data-align="justify"] { text-align: justify; }
  </style>
</head>
<body>${content || '<p>暂无正文内容</p>'}</body>
</html>`;
});

function replaceResource(items: ContentItem[]) {
  const retained = cmsState.content.filter(
    (item) => item.resource !== props.resource,
  );
  cmsState.content.splice(0, cmsState.content.length, ...retained, ...items);
}

function readableSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function selectInlineImage(id: number) {
  const asset = imageOptions.value.find((item) => item.id === id);
  selectedInlineMediaId.value = id;
  inlineImageAlt.value =
    asset?.alt || asset?.name.replace(/\.[^.]+$/, '') || '新闻正文图片';
  inlineImageCaption.value = '';
}

function openInlineImagePicker() {
  selectedInlineMediaId.value = undefined;
  inlineImageAlt.value = '';
  inlineImageCaption.value = '';
  inlineImageDialogVisible.value = true;
  void loadMediaOptions('', true);
}

function validateImageFile(file: UploadRawFile) {
  if (file.size > MAX_IMAGE_SIZE) {
    ElMessage.error('单张图片不能超过 30MB');
    return false;
  }
  if (!ALLOWED_IMAGE_TYPES.has(file.type.toLowerCase())) {
    ElMessage.error('仅支持 JPG、PNG、WebP 和 GIF 图片');
    return false;
  }
  return true;
}

async function uploadImageToMediaLibrary(
  file: UploadRawFile,
): Promise<MediaAsset> {
  const saved = await uploadMedia(file, file.name.replace(/\.[^.]+$/, ''));
  const preview = await getMediaPreviewUrl(
    saved.thumbnailUrl || saved.adminUrl,
  ).catch((error) => {
    console.warn(`素材 ${saved.id} 的预览加载失败`, error);
    return '';
  });
  const asset: MediaAsset = {
    alt: saved.altText || file.name.replace(/\.[^.]+$/, ''),
    createdAt: saved.createdAt,
    id: saved.id,
    mimeType: saved.mimeType,
    name: saved.originalFilename,
    previewSourceUrl: saved.thumbnailUrl || saved.adminUrl,
    size: readableSize(saved.fileSize),
    sourceUrl: saved.adminUrl,
    type: 'image',
    url: preview,
  };
  const existingIndex = cmsState.media.findIndex(
    (item) => item.id === saved.id,
  );
  if (existingIndex !== -1) {
    cmsState.media.splice(existingIndex, 1);
  }
  cmsState.media.unshift(asset);
  return asset;
}

function insertInlineImage() {
  const asset = selectedInlineMedia.value;
  if (!asset) {
    ElMessage.warning('请先选择一张图片');
    return;
  }

  const caption = inlineImageCaption.value.trim();
  const figure = [
    '<figure>',
    `  <img src="alfy-media:${asset.id}" alt="${escapeHtml(inlineImageAlt.value.trim() || asset.name)}" loading="lazy">`,
    caption ? `  <figcaption>${escapeHtml(caption)}</figcaption>` : '',
    '</figure>',
  ]
    .filter(Boolean)
    .join('\n');
  const editor = contentEditorRef.value;
  if (!editor) {
    ElMessage.error('正文编辑器尚未就绪，请稍后重试');
    return;
  }

  editor.insertHtml(figure);

  inlineImageDialogVisible.value = false;
  ElMessage.success('图片已插入正文，保存后即可发布');
}

async function uploadInlineImage(
  uploadFile: UploadFile,
  _uploadFiles: UploadFiles,
) {
  const rawFile = uploadFile.raw;
  if (
    !rawFile ||
    inlineImageUploadQueue.value.some((file) => file.uid === rawFile.uid)
  ) {
    return;
  }
  inlineImageUploadQueue.value.push(rawFile);
  if (inlineImageUploadSchedule) clearTimeout(inlineImageUploadSchedule);
  // Element Plus 会为同一次多选中的每个文件分别触发 on-change。
  inlineImageUploadSchedule = setTimeout(() => {
    inlineImageUploadSchedule = undefined;
    void processInlineImageUploadQueue();
  }, 0);
}

async function processInlineImageUploadQueue() {
  const files = inlineImageUploadQueue.value
    .splice(0)
    .filter((file) => validateImageFile(file));
  if (files.length === 0) return;
  inlineImageUploading.value = true;
  try {
    let completed = 0;
    for (const file of files) {
      try {
        const asset = await uploadImageToMediaLibrary(file);
        selectInlineImage(asset.id);
        completed += 1;
      } catch (error) {
        console.warn(`图片 ${file.name} 上传失败`, error);
      }
    }
    if (completed > 0) {
      ElMessage.success(
        files.length === 1
          ? '图片已上传，请确认说明后插入正文'
          : `已上传 ${completed}/${files.length} 张图片，请选择后插入正文`,
      );
    }
  } finally {
    inlineImageUploading.value = false;
    if (inlineImageUploadQueue.value.length > 0) {
      void processInlineImageUploadQueue();
    }
  }
}

function selectInlineVideo(id: number) {
  selectedInlineVideoId.value = id;
  inlineVideoCaption.value = '';
}

function openInlineVideoPicker() {
  selectedInlineVideoId.value = undefined;
  inlineVideoCaption.value = '';
  inlineVideoDialogVisible.value = true;
  void loadMediaOptions('', false);
}

function validateVideoFile(file: UploadRawFile) {
  if (file.size > MAX_VIDEO_SIZE) {
    ElMessage.error('单个视频不能超过 200MB');
    return false;
  }
  const extension = file.name.split('.').pop()?.toLowerCase() || '';
  const type =
    file.type.toLowerCase() ||
    (
      {
        avi: 'video/x-msvideo',
        m4v: 'video/x-m4v',
        mkv: 'video/x-matroska',
        mov: 'video/quicktime',
        mp4: 'video/mp4',
        mpeg: 'video/mpeg',
        mpg: 'video/mpeg',
        ogg: 'video/ogg',
        ogv: 'video/ogg',
        webm: 'video/webm',
      } as Record<string, string>
    )[extension] ||
    '';
  if (!ALLOWED_VIDEO_TYPES.has(type)) {
    ElMessage.error('支持 MP4、M4V、MOV、WebM、OGV、MPEG 和 AVI 视频');
    return false;
  }
  return true;
}

async function uploadVideoToMediaLibrary(
  file: UploadRawFile,
): Promise<MediaAsset> {
  const saved = await uploadMedia(file, file.name.replace(/\.[^.]+$/, ''));
  const asset: MediaAsset = {
    alt: saved.altText || file.name.replace(/\.[^.]+$/, ''),
    createdAt: saved.createdAt,
    id: saved.id,
    mimeType: saved.mimeType,
    name: saved.originalFilename,
    previewSourceUrl: saved.adminUrl,
    size: readableSize(saved.fileSize),
    sourceUrl: saved.adminUrl,
    type: 'video',
    url: '',
  };
  const existingIndex = cmsState.media.findIndex(
    (item) => item.id === saved.id,
  );
  if (existingIndex !== -1) cmsState.media.splice(existingIndex, 1);
  cmsState.media.unshift(asset);
  return asset;
}

function insertInlineVideo() {
  const asset = selectedInlineVideo.value;
  if (!asset) {
    ElMessage.warning('请先选择一个视频');
    return;
  }
  const caption = inlineVideoCaption.value.trim();
  const figure = [
    '<figure>',
    '  <video controls preload="metadata">',
    `    <source src="alfy-media:${asset.id}" type="${escapeHtml(asset.mimeType || 'video/mp4')}">`,
    '  </video>',
    caption ? `  <figcaption>${escapeHtml(caption)}</figcaption>` : '',
    '</figure>',
  ]
    .filter(Boolean)
    .join('\n');
  if (!contentEditorRef.value) {
    ElMessage.error('正文编辑器尚未就绪，请稍后重试');
    return;
  }
  contentEditorRef.value.insertHtml(figure);
  inlineVideoDialogVisible.value = false;
  ElMessage.success('视频已插入正文，保存后自动关联素材');
}

async function uploadInlineVideo(
  uploadFile: UploadFile,
  _uploadFiles: UploadFiles,
) {
  const rawFile = uploadFile.raw;
  if (
    !rawFile ||
    inlineVideoUploadQueue.value.some((file) => file.uid === rawFile.uid)
  ) {
    return;
  }
  inlineVideoUploadQueue.value.push(rawFile);
  if (inlineVideoUploadSchedule) clearTimeout(inlineVideoUploadSchedule);
  inlineVideoUploadSchedule = setTimeout(() => {
    inlineVideoUploadSchedule = undefined;
    void processInlineVideoUploadQueue();
  }, 0);
}

async function processInlineVideoUploadQueue() {
  const files = inlineVideoUploadQueue.value
    .splice(0)
    .filter((file) => validateVideoFile(file));
  if (files.length === 0) return;
  inlineVideoUploading.value = true;
  try {
    let completed = 0;
    for (const file of files) {
      try {
        const asset = await uploadVideoToMediaLibrary(file);
        selectInlineVideo(asset.id);
        completed += 1;
      } catch (error) {
        console.warn(`视频 ${file.name} 上传失败`, error);
      }
    }
    if (completed > 0) {
      ElMessage.success(
        files.length === 1
          ? '视频已上传，请确认后插入正文'
          : `已上传 ${completed}/${files.length} 个视频，请选择后插入正文`,
      );
    }
  } finally {
    inlineVideoUploading.value = false;
    if (inlineVideoUploadQueue.value.length > 0) {
      void processInlineVideoUploadQueue();
    }
  }
}

async function uploadCoverImage(
  _uploadFile: UploadFile,
  uploadFiles: UploadFiles,
) {
  const latest = uploadFiles.at(-1)?.raw as undefined | UploadRawFile;
  if (!latest || !validateImageFile(latest)) {
    coverUploadFileList.value = [];
    return;
  }

  coverImageUploading.value = true;
  try {
    const asset = await uploadImageToMediaLibrary(latest);
    form.cover = asset.sourceUrl || asset.url;
    form.coverMediaId = asset.id;
    ElMessage.success('封面图片已上传并保存到素材库');
  } finally {
    coverImageUploading.value = false;
    coverUploadFileList.value = [];
  }
}

function openContentPreview() {
  if (!form.contentHtml?.trim()) {
    ElMessage.warning('请先填写正文内容');
    return;
  }
  contentPreviewVisible.value = true;
}

async function loadReferences() {
  referenceOptions.value = [];
  relatedOptions.value = [];
  sceneOptions.value = [];
  switch (props.resource) {
    case 'articles': {
      const flatten = (
        values: ArticleCategoryRecord[],
      ): ArticleCategoryRecord[] =>
        values.flatMap((item) => [item, ...flatten(item.children || [])]);
      const values = flatten(await listArticleCategories());
      referenceOptions.value = values.map((item) => ({
        id: item.id,
        name: item.name,
      }));

      break;
    }
    case 'cases': {
      const [values, products, scenes] = await Promise.all([
        listCaseCategories(),
        listContent('products'),
        listContent('scenes'),
      ]);
      referenceOptions.value = values.map((item) => ({
        id: Number(item.id),
        name: item.name,
      }));
      relatedOptions.value = products.map((item) => ({
        id: Number(item.id),
        name: String(item.name || item.title || item.slug),
      }));
      sceneOptions.value = scenes.map((item) => ({
        id: Number(item.id),
        name: String(item.name || item.title || item.slug),
      }));

      break;
    }
    case 'products': {
      const [values, scenes] = await Promise.all([
        listProductCategories(),
        listContent('scenes'),
      ]);
      referenceOptions.value = values.map((item) => ({
        id: item.id,
        name: item.name,
      }));
      relatedOptions.value = scenes.map((item) => ({
        id: Number(item.id),
        name: String(item.name || item.title || item.slug),
      }));

      break;
    }
    default: {
      referenceOptions.value = [];
      relatedOptions.value = [];
    }
  }
}

async function withConcurrency<T>(
  values: T[],
  worker: (value: T) => Promise<void>,
  limit = 4,
) {
  let nextIndex = 0;
  await Promise.all(
    Array.from({ length: Math.min(limit, values.length) }, async () => {
      while (nextIndex < values.length) {
        const current = values[nextIndex++];
        if (current) await worker(current);
      }
    }),
  );
}

async function loadMediaOptions(keyword = '', withPreviews = false) {
  mediaOptionsLoading.value = true;
  try {
    const media = await listMedia(keyword, { page: 1, size: 100 });
    const previous = new Map(cmsState.media.map((item) => [item.id, item]));
    const mappedMedia = media
      .filter((item) => ['IMAGE', 'VIDEO'].includes(item.mediaType))
      .map((item) => ({
        alt: item.altText || '',
        createdAt: item.createdAt,
        id: item.id,
        mimeType: item.mimeType,
        name: item.originalFilename,
        previewSourceUrl: item.thumbnailUrl || item.adminUrl,
        size: readableSize(item.fileSize),
        sourceUrl: item.adminUrl,
        type: item.mediaType.toLowerCase() as 'image' | 'video',
        url: previous.get(item.id)?.url || '',
      }));
    cmsState.media.splice(0, cmsState.media.length, ...mappedMedia);
    mediaOptionsLoaded.value = true;

    if (withPreviews) {
      await withConcurrency(mappedMedia, async (asset) => {
        if (asset.type !== 'image' || asset.url || !asset.previewSourceUrl)
          return;
        asset.url = await getMediaPreviewUrl(asset.previewSourceUrl).catch(
          () => '',
        );
      });
    }
  } finally {
    mediaOptionsLoading.value = false;
  }
}

function handleMediaSelector(visible: boolean) {
  if (visible && !mediaOptionsLoaded.value && !mediaOptionsLoading.value) {
    void loadMediaOptions();
  }
}

async function ensureSelectedMediaPreview(value: string) {
  const id = mediaIdFromUrl(value);
  const asset = cmsState.media.find((item) => item.id === id);
  if (!asset || asset.url || !asset.previewSourceUrl) return;
  asset.url = await getMediaPreviewUrl(asset.previewSourceUrl).catch(() => '');
}

async function load() {
  loading.value = true;
  try {
    const values = await listContent(props.resource);
    // Content is the primary data for this page. Render it before loading
    // media and category helpers so an auxiliary request cannot blank the list.
    replaceResource(
      values.map((item) => contentFromBackend(props.resource, item)),
    );

    try {
      await loadReferences();
      const names = new Map(
        referenceOptions.value.map((item) => [item.id, item.name]),
      );
      replaceResource(
        values.map((item) => contentFromBackend(props.resource, item, names)),
      );
    } catch (error) {
      console.warn('辅助素材或分类加载失败，内容列表仍保持可用', error);
    }
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.resource,
  async () => {
    keyword.value = '';
    status.value = 'all';
    category.value = 'all';
    await load();
  },
  { immediate: true },
);

function applyReference() {
  if (props.resource === 'articles' && form.categoryIds?.length) {
    form.categoryId = form.categoryIds[0];
    form.category =
      referenceOptions.value.find((item) => item.id === form.categoryId)
        ?.name || form.category;
    return;
  }
  const selected = referenceOptions.value.find(
    (item) => item.name === form.category,
  );
  form.categoryId = selected?.id;
  form.categoryIds = selected ? [selected.id] : [];
}

function resetAdvancedFields() {
  featuresText.value = '';
  capabilityRowsText.value = '';
  pillarsText.value = '';
}

function loadAdvancedFields(item: ContentItem) {
  const raw = item.raw || {};
  featuresText.value = Array.isArray(raw.features)
    ? raw.features.join('\n')
    : '';
  capabilityRowsText.value = raw.capabilityRows
    ? JSON.stringify(raw.capabilityRows, null, 2)
    : '';
  pillarsText.value = raw.pillars ? JSON.stringify(raw.pillars, null, 2) : '';
}

function applyAdvancedFields() {
  if (props.resource === 'products') {
    rawForm.value.features = featuresText.value
      .split(/[,，\n]/)
      .map((item) => item.trim())
      .filter(Boolean);
  }
  try {
    if (props.resource === 'technologies') {
      rawForm.value.capabilityRows = capabilityRowsText.value.trim()
        ? JSON.parse(capabilityRowsText.value)
        : [];
      rawForm.value.pillars = pillarsText.value.trim()
        ? JSON.parse(pillarsText.value)
        : [];
    }
  } catch {
    ElMessage.warning('JSON 字段格式不正确，请检查后重试');
    return false;
  }
  return true;
}

function previewUrl(value: string) {
  const id = mediaIdFromUrl(value);
  return cmsState.media.find((item) => item.id === id)?.url || value;
}

function openCreate() {
  const current = resourceItems.value[0];
  if (props.resource === 'technologies' && current) {
    void openEdit(current);
    return;
  }
  activeId.value = null;
  originalStatus.value = 'draft';
  Object.assign(form, emptyForm());
  resetAdvancedFields();
  dialogVisible.value = true;
  if (!mediaOptionsLoaded.value) void loadMediaOptions();
}

async function openEdit(value: unknown) {
  const item = value as ContentItem;
  activeId.value = item.id;
  loading.value = true;
  try {
    const detail = await getContent(props.resource, item.id);
    const names = new Map(
      referenceOptions.value.map((option) => [option.id, option.name]),
    );
    const mapped = contentFromBackend(props.resource, detail, names);
    originalStatus.value = mapped.status;
    Object.assign(form, structuredClone(toRaw(mapped)));
    loadAdvancedFields(mapped);
    dialogVisible.value = true;
    if (!mediaOptionsLoaded.value) void loadMediaOptions();
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (
    !form.title.trim() ||
    (!['banners', 'technologies'].includes(props.resource) && !form.slug.trim())
  ) {
    ElMessage.warning(
      props.resource === 'banners' || props.resource === 'technologies'
        ? '请填写标题'
        : '请填写标题和 URL slug',
    );
    return;
  }
  if (
    props.resource === 'products' &&
    (!Array.isArray(rawForm.value.sceneIds) ||
      rawForm.value.sceneIds.length === 0)
  ) {
    ElMessage.warning('请至少选择一个关联应用场景');
    return;
  }
  if (
    props.resource === 'cases' &&
    (!Array.isArray(rawForm.value.productIds) ||
      rawForm.value.productIds.length === 0)
  ) {
    ElMessage.warning('请至少选择一个关联产品');
    return;
  }
  const duplicate = resourceItems.value.some(
    (item) => item.slug === form.slug.trim() && item.id !== activeId.value,
  );
  if (!['banners', 'technologies'].includes(props.resource) && duplicate) {
    ElMessage.warning('该 slug 已存在，请更换');
    return;
  }

  applyReference();
  if (!applyAdvancedFields()) return;
  if (
    ['articles', 'cases', 'products'].includes(props.resource) &&
    !form.categoryId
  ) {
    ElMessage.warning('请先创建并选择有效分类');
    return;
  }

  saving.value = true;
  try {
    const saved = await saveContent(
      props.resource,
      activeId.value,
      contentPayload(props.resource, form),
    );
    const savedId = Number(saved.id || activeId.value || 0);
    if (
      savedId &&
      form.status !== originalStatus.value &&
      form.status !== 'draft'
    ) {
      await changeContentStatus(
        props.resource,
        savedId,
        form.status === 'published' ? 'published' : 'offline',
      );
    }
    dialogVisible.value = false;
    await load();
    ElMessage.success(activeId.value ? '内容已更新' : '内容已创建');
  } finally {
    saving.value = false;
  }
}

async function changeStatus(value: unknown, next: ContentStatus) {
  const item = value as ContentItem;
  await changeContentStatus(
    props.resource,
    item.id,
    next === 'published' ? 'published' : 'offline',
  );
  await load();
  ElMessage.success(next === 'published' ? '内容已发布' : '内容已下线');
}

async function remove(value: unknown) {
  const item = value as ContentItem;
  await deleteContent(props.resource, item.id);
  await load();
  ElMessage.success('内容已删除');
}

function statusType(value: ContentStatus) {
  if (value === 'published') return 'success';
  if (value === 'draft') return 'warning';
  return 'info';
}

function statusLabel(value: ContentStatus) {
  if (value === 'published') return '已发布';
  if (value === 'draft') return '草稿';
  return '已下线';
}

function homePlacementLabel(value: unknown) {
  const item = value as ContentItem;
  if (props.resource === 'articles') {
    if (item.homePinned) return '主置顶';
    if (item.showOnHome) return '首页新闻';
    return '';
  }
  return item.featured ? '推荐' : '';
}

function handleHomeVisibilityChange() {
  if (!form.showOnHome) form.homePinned = false;
}

function handleHomePinnedChange() {
  if (form.homePinned) form.showOnHome = true;
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
      <ElButton
        :loading="loading"
        class="brand-button"
        size="large"
        type="primary"
        @click="openCreate"
      >
        {{
          props.resource === 'technologies' ? '编辑技术页' : `新建${meta.label}`
        }}
      </ElButton>
    </section>

    <ElRow :gutter="16" class="stats-grid">
      <ElCol :lg="6" :sm="12" :xs="24">
        <ElCard shadow="never">
          <span>全部内容</span><strong>{{ stats.total }}</strong>
        </ElCard>
      </ElCol>
      <ElCol :lg="6" :sm="12" :xs="24">
        <ElCard shadow="never">
          <span>已发布</span><strong>{{ stats.published }}</strong>
        </ElCard>
      </ElCol>
      <ElCol :lg="6" :sm="12" :xs="24">
        <ElCard shadow="never">
          <span>草稿</span><strong>{{ stats.draft }}</strong>
        </ElCard>
      </ElCol>
      <ElCol :lg="6" :sm="12" :xs="24">
        <ElCard shadow="never">
          <span>{{ isHomePlacementResource ? '首页展示' : '首页推荐' }}</span>
          <strong>{{ stats.featured }}</strong>
        </ElCard>
      </ElCol>
    </ElRow>

    <ElCard class="content-card" shadow="never">
      <div class="filter-row">
        <ElInput
          v-model="keyword"
          clearable
          placeholder="搜索标题、slug 或摘要"
        />
        <ElSelect v-model="category" aria-label="按分类筛选">
          <ElOption label="全部分类" value="all" />
          <ElOption
            v-for="item in categoryOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </ElSelect>
        <ElSelect v-model="status" aria-label="按状态筛选">
          <ElOption label="全部状态" value="all" />
          <ElOption label="已发布" value="published" />
          <ElOption label="草稿" value="draft" />
          <ElOption label="已下线" value="offline" />
        </ElSelect>
      </div>

      <ElTable :data="filteredItems" row-key="id" v-loading="loading">
        <ElTableColumn label="内容" min-width="320">
          <template #default="{ row }">
            <div class="content-cell">
              <ElImage
                v-if="row.cover"
                :preview-src-list="[previewUrl(row.cover)]"
                :src="previewUrl(row.cover)"
                fit="cover"
                lazy
              />
              <div v-else class="cover-placeholder">
                {{ row.title.slice(0, 1) }}
              </div>
              <div>
                <strong>{{ row.title }}</strong>
                <small>/{{ row.slug }}</small>
              </div>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="分类" min-width="120" prop="category" />
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }">
            <ElTag :type="statusType(row.status)">
              {{ statusLabel(row.status) }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn
          :label="isHomePlacementResource ? '首页位置' : '推荐'"
          width="90"
        >
          <template #default="{ row }">
            <ElTag v-if="homePlacementLabel(row)" effect="plain" type="danger">
              {{ homePlacementLabel(row) }}
            </ElTag>
            <span v-else>-</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="排序" prop="sortOrder" width="80" />
        <ElTableColumn label="更新时间" min-width="150" prop="updatedAt" />
        <ElTableColumn fixed="right" label="操作" width="230">
          <template #default="{ row }">
            <ElButton link type="primary" @click="openEdit(row)">编辑</ElButton>
            <ElButton
              v-if="row.status !== 'published'"
              link
              type="success"
              @click="changeStatus(row, 'published')"
            >
              发布
            </ElButton>
            <ElButton v-else link @click="changeStatus(row, 'offline')">
              下线
            </ElButton>
            <ElPopconfirm
              v-if="supportsDelete"
              title="内容将移入回收站，确定继续？"
              @confirm="remove(row)"
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
      :title="activeId ? `编辑${meta.label}` : `新建${meta.label}`"
      destroy-on-close
      width="760px"
    >
      <ElForm :model="form" label-position="top">
        <ElRow :gutter="18">
          <ElCol :md="16" :xs="24">
            <ElFormItem label="标题" required>
              <ElInput v-model="form.title" maxlength="80" show-word-limit />
            </ElFormItem>
          </ElCol>
          <ElCol :md="8" :xs="24">
            <ElFormItem
              :label="props.resource === 'cases' ? '案例分类' : '分类'"
            >
              <ElSelect
                v-if="props.resource === 'articles'"
                v-model="form.categoryIds"
                multiple
                placeholder="至少选择一个文章分类"
                style="width: 100%"
              >
                <ElOption
                  v-for="item in referenceOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </ElSelect>
              <ElSelect v-else v-model="form.category" style="width: 100%">
                <ElOption
                  v-for="item in categoryOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </ElSelect>
            </ElFormItem>
          </ElCol>
          <ElCol :md="16" :xs="24">
            <ElFormItem label="URL slug" required>
              <ElInput v-model="form.slug" placeholder="例如 aerogel-powder" />
            </ElFormItem>
          </ElCol>
          <ElCol :md="8" :xs="24">
            <ElFormItem label="排序">
              <ElInputNumber
                v-model="form.sortOrder"
                :min="0"
                style="width: 100%"
              />
            </ElFormItem>
          </ElCol>
          <template v-if="props.resource === 'banners'">
            <ElCol :md="12" :xs="24">
              <ElFormItem label="栏目小标题">
                <ElInput
                  v-model="form.eyebrow"
                  maxlength="30"
                  placeholder="例如 技术发布 · 成果转化"
                  show-word-limit
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="12" :xs="24">
              <ElFormItem label="红色强调标题">
                <ElInput
                  v-model="form.highlightTitle"
                  maxlength="40"
                  show-word-limit
                />
              </ElFormItem>
            </ElCol>
          </template>
          <ElCol :md="props.resource === 'banners' ? 12 : 24" :xs="24">
            <ElFormItem
              :label="
                props.resource === 'banners' ? 'PC 端背景图片' : '封面图片'
              "
            >
              <div class="cover-image-field">
                <ElSelect
                  v-model="form.cover"
                  :loading="mediaOptionsLoading"
                  :remote-method="loadMediaOptions"
                  allow-create
                  class="cover-image-select"
                  clearable
                  filterable
                  placeholder="从素材库选择或粘贴图片地址"
                  remote
                  @change="ensureSelectedMediaPreview"
                  @visible-change="handleMediaSelector"
                >
                  <ElOption
                    v-for="asset in imageOptions"
                    :key="asset.id"
                    :label="asset.name"
                    :value="asset.sourceUrl || asset.url"
                  />
                </ElSelect>
                <ElUpload
                  v-if="supportsCoverImageUpload"
                  v-model:file-list="coverUploadFileList"
                  :auto-upload="false"
                  :on-change="uploadCoverImage"
                  :show-file-list="false"
                  accept=".jpg,.jpeg,.png,.webp,.gif"
                >
                  <ElButton :loading="coverImageUploading">
                    从本地上传
                  </ElButton>
                </ElUpload>
              </div>
              <p v-if="supportsCoverImageUpload" class="cover-upload-tip">
                上传成功后会自动设为当前封面，并保存到素材库。
              </p>
            </ElFormItem>
          </ElCol>
          <ElCol v-if="props.resource === 'banners'" :md="12" :xs="24">
            <ElFormItem label="手机端背景图片">
              <ElSelect
                v-model="form.mobileCover"
                :loading="mediaOptionsLoading"
                :remote-method="loadMediaOptions"
                allow-create
                clearable
                filterable
                placeholder="不设置时使用 PC 端图片"
                remote
                style="width: 100%"
                @change="ensureSelectedMediaPreview"
                @visible-change="handleMediaSelector"
              >
                <ElOption
                  v-for="asset in imageOptions"
                  :key="asset.id"
                  :label="asset.name"
                  :value="asset.sourceUrl || asset.url"
                />
              </ElSelect>
            </ElFormItem>
          </ElCol>
          <ElCol v-if="form.cover" :span="24">
            <ElFormItem label="图片预览">
              <ElImage
                :preview-src-list="[previewUrl(form.cover)]"
                :src="previewUrl(form.cover)"
                fit="cover"
                style="width: 220px; height: 120px; border-radius: 10px"
              />
            </ElFormItem>
          </ElCol>
          <ElCol :span="24">
            <ElFormItem label="摘要">
              <ElInput
                v-model="form.summary"
                :rows="3"
                maxlength="240"
                show-word-limit
                type="textarea"
              />
            </ElFormItem>
          </ElCol>
          <template v-if="props.resource === 'products'">
            <ElCol :span="24">
              <ElFormItem
                label="产品特点（官网独立展示模块，每行或逗号分隔一项）"
              >
                <ElInput v-model="featuresText" :rows="4" type="textarea" />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="关联应用场景" required>
                <ElSelect
                  v-model="rawForm.sceneIds"
                  clearable
                  multiple
                  placeholder="请至少选择一个关联应用场景"
                  style="width: 100%"
                >
                  <ElOption
                    v-for="item in relatedOptions"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </ElSelect>
              </ElFormItem>
            </ElCol>
          </template>
          <template v-if="props.resource === 'scenes'">
            <ElCol :span="24">
              <ElFormItem label="场景口号">
                <ElInput v-model="rawForm.slogan" />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="行业痛点">
                <ElInput
                  v-model="rawForm.painPoint"
                  :rows="4"
                  type="textarea"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="解决方案">
                <ElInput v-model="rawForm.solution" :rows="4" type="textarea" />
              </ElFormItem>
            </ElCol>
          </template>
          <template v-if="props.resource === 'cases'">
            <ElCol :span="24">
              <ElFormItem label="关联应用场景（可选）">
                <ElSelect
                  v-model="rawForm.sceneIds"
                  clearable
                  multiple
                  placeholder="案例可以不关联应用场景，也可以关联多个"
                  style="width: 100%"
                >
                  <ElOption
                    v-for="item in sceneOptions"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </ElSelect>
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="关联产品" required>
                <ElSelect
                  v-model="rawForm.productIds"
                  clearable
                  multiple
                  placeholder="请至少选择一个关联产品"
                  style="width: 100%"
                >
                  <ElOption
                    v-for="item in relatedOptions"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </ElSelect>
              </ElFormItem>
            </ElCol>
          </template>
          <template v-if="props.resource === 'articles'">
            <ElCol :md="12" :xs="24">
              <ElFormItem label="作者">
                <ElInput v-model="rawForm.authorName" />
              </ElFormItem>
            </ElCol>
            <ElCol :md="12" :xs="24">
              <ElFormItem label="来源链接">
                <ElInput v-model="rawForm.sourceUrl" />
              </ElFormItem>
            </ElCol>
          </template>
          <ElCol v-if="props.resource === 'partners'" :span="24">
            <ElFormItem label="合作伙伴网站">
              <ElInput v-model="rawForm.websiteUrl" />
            </ElFormItem>
          </ElCol>
          <template v-if="props.resource === 'technologies'">
            <ElCol :span="24">
              <ElFormItem label="能力表格 JSON">
                <ElInput
                  v-model="capabilityRowsText"
                  :rows="6"
                  type="textarea"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :span="24">
              <ElFormItem label="技术支柱 JSON">
                <ElInput v-model="pillarsText" :rows="6" type="textarea" />
              </ElFormItem>
            </ElCol>
            <ElCol :md="8" :xs="24">
              <ElFormItem label="按钮文字">
                <ElInput v-model="form.primaryActionLabel" />
              </ElFormItem>
            </ElCol>
            <ElCol :md="16" :xs="24">
              <ElFormItem label="按钮链接">
                <ElInput v-model="form.primaryActionLink" />
              </ElFormItem>
            </ElCol>
          </template>
          <ElCol
            v-if="!['banners', 'partners'].includes(props.resource)"
            :span="24"
          >
            <ElFormItem>
              <template #label>
                <div class="content-editor-label">
                  <span>正文内容（支持图文排版）</span>
                  <div>
                    <ElButton size="small" @click="openContentPreview">
                      预览正文
                    </ElButton>
                  </div>
                </div>
              </template>
              <RichTextEditor
                ref="contentEditorRef"
                v-model="form.contentHtml"
                :media-image-picker="true"
                :media-preview-urls="inlineMediaPreviewUrls"
                :media-video-picker="props.resource === 'articles'"
                :min-height="300"
                :show-video-button="props.resource === 'articles'"
                placeholder="请输入正文；可设置字体、字号、颜色、对齐方式，并从工具栏插入图片或视频"
                @request-image="openInlineImagePicker"
                @request-video="openInlineVideoPicker"
              />
              <p class="content-editor-tip">
                图片和视频会插入当前光标位置；可从素材库选择或直接上传，保存后自动关联素材。
              </p>
            </ElFormItem>
          </ElCol>
          <template v-if="props.resource === 'banners'">
            <ElCol :md="8" :xs="24">
              <ElFormItem label="主按钮文字">
                <ElInput
                  v-model="form.primaryActionLabel"
                  placeholder="查看应用案例"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="16" :xs="24">
              <ElFormItem label="主按钮链接">
                <ElInput
                  v-model="form.primaryActionLink"
                  placeholder="/applications"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="8" :xs="24">
              <ElFormItem label="次按钮文字">
                <ElInput
                  v-model="form.secondaryActionLabel"
                  placeholder="获取项目方案"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="16" :xs="24">
              <ElFormItem label="次按钮链接">
                <ElInput
                  v-model="form.secondaryActionLink"
                  placeholder="/cooperation"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="12" :xs="24">
              <ElFormItem label="开始展示时间">
                <ElInput
                  v-model="rawForm.startsAt"
                  placeholder="2026-07-22T08:00:00"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="12" :xs="24">
              <ElFormItem label="结束展示时间">
                <ElInput
                  v-model="rawForm.endsAt"
                  placeholder="2026-08-22T08:00:00"
                />
              </ElFormItem>
            </ElCol>
          </template>
          <ElCol :md="8" :xs="24">
            <ElFormItem label="状态">
              <ElSelect v-model="form.status" style="width: 100%">
                <ElOption label="草稿" value="draft" /><ElOption
                  label="已发布"
                  value="published"
                /><ElOption label="已下线" value="offline" />
              </ElSelect>
            </ElFormItem>
          </ElCol>
          <template v-if="isHomePlacementResource">
            <ElCol :md="8" :xs="24">
              <ElFormItem label="首页展示">
                <ElSwitch
                  v-model="form.showOnHome"
                  active-text="显示"
                  inactive-text="隐藏"
                  @change="handleHomeVisibilityChange"
                />
              </ElFormItem>
            </ElCol>
            <ElCol :md="8" :xs="24">
              <ElFormItem
                :label="
                  props.resource === 'articles' ? '首页主置顶' : '首页置顶'
                "
              >
                <ElSwitch
                  v-model="form.homePinned"
                  active-text="置顶"
                  inactive-text="普通"
                  @change="handleHomePinnedChange"
                />
              </ElFormItem>
            </ElCol>
          </template>
          <ElCol v-else :md="8" :xs="24">
            <ElFormItem label="首页推荐">
              <ElSwitch
                v-model="form.featured"
                active-text="推荐"
                inactive-text="普通"
              />
            </ElFormItem>
          </ElCol>
        </ElRow>
        <ElDivider content-position="left">SEO 设置</ElDivider>
        <ElFormItem label="SEO 标题">
          <ElInput v-model="form.seoTitle" maxlength="70" show-word-limit />
        </ElFormItem>
        <ElFormItem label="SEO 描述">
          <ElInput
            v-model="form.seoDescription"
            :rows="2"
            maxlength="160"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="SEO 关键词">
          <ElInput v-model="form.seoKeywords" placeholder="使用英文逗号分隔" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton :loading="saving" type="primary" @click="save">
          保存内容
        </ElButton>
      </template>
    </ElDialog>

    <ElDialog
      v-model="inlineImageDialogVisible"
      :close-on-click-modal="false"
      append-to-body
      title="插入正文图片"
      width="860px"
    >
      <div class="inline-image-toolbar" v-loading="mediaOptionsLoading">
        <p>选择素材后可修改图片说明，图片将插入正文当前光标位置。</p>
        <ElUpload
          :auto-upload="false"
          :disabled="inlineImageUploading"
          :on-change="uploadInlineImage"
          :show-file-list="false"
          accept=".jpg,.jpeg,.png,.webp,.gif"
          multiple
        >
          <ElButton :loading="inlineImageUploading" type="primary">
            上传新图片
          </ElButton>
        </ElUpload>
      </div>

      <div v-if="imageOptions.length > 0" class="inline-image-grid">
        <button
          v-for="asset in imageOptions"
          :key="asset.id"
          :class="{ selected: selectedInlineMediaId === asset.id }"
          type="button"
          @click="selectInlineImage(asset.id)"
        >
          <img
            v-if="asset.url"
            :alt="asset.alt || asset.name"
            :src="asset.url"
          />
          <span v-else class="inline-image-placeholder">图片预览不可用</span>
          <strong :title="asset.name">{{ asset.name }}</strong>
        </button>
      </div>
      <ElEmpty v-else description="素材库暂无图片，可先上传一张图片" />

      <div v-if="selectedInlineMedia" class="inline-image-fields">
        <ElFormItem label="图片替代文字">
          <ElInput
            v-model="inlineImageAlt"
            maxlength="160"
            placeholder="用于无障碍访问和图片加载失败时显示"
            show-word-limit
          />
        </ElFormItem>
        <ElFormItem label="图片说明（可选）">
          <ElInput
            v-model="inlineImageCaption"
            maxlength="200"
            placeholder="例如：奥飞新材与中南大学签约现场"
            show-word-limit
          />
        </ElFormItem>
      </div>

      <template #footer>
        <ElButton @click="inlineImageDialogVisible = false">取消</ElButton>
        <ElButton
          :disabled="!selectedInlineMedia"
          type="primary"
          @click="insertInlineImage"
        >
          插入当前位置
        </ElButton>
      </template>
    </ElDialog>

    <ElDialog
      v-model="inlineVideoDialogVisible"
      :close-on-click-modal="false"
      append-to-body
      title="插入正文视频"
      width="760px"
    >
      <div class="inline-image-toolbar" v-loading="mediaOptionsLoading">
        <p>选择素材库视频，或上传新视频后插入正文当前光标位置。</p>
        <ElUpload
          :auto-upload="false"
          :disabled="inlineVideoUploading"
          :on-change="uploadInlineVideo"
          :show-file-list="false"
          accept=".mp4,.m4v,.mov,.webm,.ogv,.ogg,.mpeg,.mpg,.avi,.mkv"
          multiple
        >
          <ElButton :loading="inlineVideoUploading" type="primary">
            上传新视频
          </ElButton>
        </ElUpload>
      </div>

      <div
        v-if="videoOptions.length > 0"
        class="inline-image-grid inline-video-grid"
      >
        <button
          v-for="asset in videoOptions"
          :key="asset.id"
          :class="{ selected: selectedInlineVideoId === asset.id }"
          type="button"
          @click="selectInlineVideo(asset.id)"
        >
          <span class="inline-image-placeholder">🎬 视频素材</span>
          <strong :title="asset.name">{{ asset.name }}</strong>
        </button>
      </div>
      <ElEmpty v-else description="素材库暂无视频，可先上传一个视频" />

      <div v-if="selectedInlineVideo" class="inline-image-fields">
        <ElFormItem label="视频说明（可选）">
          <ElInput
            v-model="inlineVideoCaption"
            maxlength="200"
            placeholder="例如：活动现场视频"
            show-word-limit
          />
        </ElFormItem>
        <ElFormItem label="文件信息">
          <ElInput
            :model-value="`${selectedInlineVideo.name} · ${selectedInlineVideo.size}`"
            disabled
          />
        </ElFormItem>
      </div>

      <template #footer>
        <ElButton @click="inlineVideoDialogVisible = false">取消</ElButton>
        <ElButton
          :disabled="!selectedInlineVideo"
          type="primary"
          @click="insertInlineVideo"
        >
          插入当前位置
        </ElButton>
      </template>
    </ElDialog>

    <ElDialog
      v-model="contentPreviewVisible"
      :close-on-click-modal="false"
      append-to-body
      title="官网正文预览"
      width="900px"
    >
      <iframe
        :srcdoc="contentPreviewDocument"
        class="content-preview-frame"
        sandbox="allow-same-origin"
        title="官网正文预览"
      ></iframe>
    </ElDialog>
  </div>
</template>

<style scoped>
.cms-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7f8;
}

.cms-hero {
  display: flex;
  gap: 24px;
  align-items: flex-end;
  justify-content: space-between;
  padding: 28px 30px;
  color: #fff;
  background: linear-gradient(120deg, #092c36, #0e5960 65%, #1b7780);
  border-radius: 18px;
  box-shadow: 0 18px 50px rgb(9 44 54 / 16%);
}

.cms-hero h1 {
  margin: 4px 0 8px;
  font-size: 28px;
  font-weight: 650;
  letter-spacing: -0.02em;
}

.cms-hero p:last-child {
  max-width: 720px;
  margin: 0;
  color: rgb(255 255 255 / 72%);
}

.cms-eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  color: #79d2c8 !important;
  letter-spacing: 0.18em;
}

.brand-button {
  --el-button-bg-color: #e65b43;
  --el-button-border-color: #e65b43;
  --el-button-hover-bg-color: #f16a52;
  --el-button-hover-border-color: #f16a52;
}

.stats-grid {
  margin-top: 16px;
}

.stats-grid :deep(.el-card) {
  margin-bottom: 16px;
  border: 1px solid #e6ebed;
  border-radius: 14px;
}

.stats-grid :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
}

.stats-grid span {
  font-size: 13px;
  color: #708087;
}

.stats-grid strong {
  font-size: 26px;
  color: #14343b;
}

.content-card {
  border: 1px solid #e6ebed;
  border-radius: 16px;
}

.filter-row {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 180px 160px;
  gap: 12px;
  margin-bottom: 18px;
}

.content-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.content-cell :deep(.el-image),
.cover-placeholder {
  flex: 0 0 58px;
  width: 58px;
  height: 44px;
  overflow: hidden;
  border-radius: 8px;
}

.cover-placeholder {
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #0e5960;
  background: #dcefed;
}

.content-cell strong,
.content-cell small {
  display: block;
}

.content-cell strong {
  color: #18343b;
}

.content-cell small {
  margin-top: 4px;
  color: #8a979b;
}

.cover-image-field {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
}

.cover-image-select {
  flex: 1;
  min-width: 0;
}

.cover-upload-tip {
  width: 100%;
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #7c8b90;
}

.content-editor-label {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.content-editor-tip {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #7c8b90;
}

.inline-image-toolbar {
  display: flex;
  gap: 20px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.inline-image-toolbar p {
  margin: 0;
  color: #65767c;
}

.inline-image-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  max-height: 420px;
  padding: 2px;
  overflow-y: auto;
}

.inline-image-grid button {
  padding: 0;
  overflow: hidden;
  color: #263e44;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 2px solid transparent;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e0e7e9;
}

.inline-image-grid button.selected {
  border-color: #0e7478;
  box-shadow: 0 0 0 3px rgb(14 116 120 / 14%);
}

.inline-image-grid img,
.inline-image-placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 120px;
  font-size: 12px;
  color: #879499;
  object-fit: cover;
  background: #edf2f3;
}

.inline-image-grid strong {
  display: block;
  padding: 10px;
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inline-image-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  padding: 16px;
  margin-top: 20px;
  background: #f5f8f8;
  border-radius: 10px;
}

.inline-image-fields :deep(.el-form-item) {
  margin-bottom: 0;
}

.content-preview-frame {
  display: block;
  width: 100%;
  height: min(680px, 70vh);
  background: #fff;
  border: 1px solid #e2e8ea;
  border-radius: 10px;
}

@media (max-width: 760px) {
  .cms-page {
    padding: 14px;
  }

  .cms-hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 22px;
  }

  .filter-row {
    grid-template-columns: 1fr;
  }

  .cover-image-field {
    flex-direction: column;
    align-items: stretch;
  }

  .cover-image-field :deep(.el-upload),
  .cover-image-field :deep(.el-button) {
    width: 100%;
  }

  .content-editor-label,
  .inline-image-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .inline-image-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .inline-image-fields {
    grid-template-columns: 1fr;
  }
}
</style>
