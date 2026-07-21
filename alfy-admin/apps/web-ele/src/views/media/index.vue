<script lang="ts" setup>
import type { UploadFile, UploadFiles, UploadRawFile } from 'element-plus';

import { computed, ref } from 'vue';

import {
  ElButton,
  ElCard,
  ElEmpty,
  ElImage,
  ElInput,
  ElMessage,
  ElPopconfirm,
  ElTag,
  ElUpload,
} from 'element-plus';

import { cmsState, nextId } from '#/data/cms';

const keyword = ref('');
const filtered = computed(() =>
  cmsState.media.filter((item) =>
    item.name.toLowerCase().includes(keyword.value.trim().toLowerCase()),
  ),
);

function readableSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function addFile(_uploadFile: UploadFile, uploadFiles: UploadFiles) {
  const latest = uploadFiles.at(-1)?.raw as UploadRawFile | undefined;
  if (!latest) return;
  if (latest.size > 4 * 1024 * 1024) {
    ElMessage.warning('本地演示模式单个文件不超过 4 MB；接入后端后可调整限制');
    return;
  }
  const reader = new FileReader();
  reader.onload = () => {
    const mime = latest.type || '';
    const type = mime.startsWith('image/') ? 'image' : mime.startsWith('video/') ? 'video' : 'document';
    cmsState.media.unshift({
      alt: latest.name.replace(/\.[^.]+$/, ''),
      createdAt: new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date()),
      id: nextId(cmsState.media),
      name: latest.name,
      size: readableSize(latest.size),
      type,
      url: String(reader.result || '#'),
    });
    ElMessage.success('素材已加入本地素材库');
  };
  reader.readAsDataURL(latest);
}

function remove(id: number) {
  const index = cmsState.media.findIndex((item) => item.id === id);
  if (index >= 0) cmsState.media.splice(index, 1);
  ElMessage.success('素材已删除');
}
</script>

<template>
  <div class="media-page">
    <section class="page-header">
      <div><p>ASSET LIBRARY</p><h1>素材库</h1><span>统一管理官网图片、产品手册和视频资料</span></div>
      <ElUpload :auto-upload="false" :on-change="addFile" :show-file-list="false" accept="image/*,.pdf,.doc,.docx,video/*">
        <ElButton size="large" type="primary">上传素材</ElButton>
      </ElUpload>
    </section>
    <ElCard class="media-card" shadow="never">
      <div class="toolbar"><ElInput v-model="keyword" clearable placeholder="搜索素材名称" /><span>共 {{ filtered.length }} 个文件</span></div>
      <div v-if="filtered.length" class="media-grid">
        <article v-for="item in filtered" :key="item.id" class="media-item">
          <ElImage v-if="item.type === 'image'" :preview-src-list="[item.url]" :src="item.url" fit="cover" />
          <div v-else class="file-preview"><b>{{ item.type === 'video' ? 'VIDEO' : 'DOC' }}</b><span>{{ item.name.split('.').pop()?.toUpperCase() }}</span></div>
          <div class="media-info"><strong :title="item.name">{{ item.name }}</strong><span>{{ item.size }} · {{ item.createdAt }}</span><ElInput v-model="item.alt" placeholder="图片替代文本 / 素材说明" /></div>
          <div class="media-actions"><ElTag effect="plain">{{ item.type === 'image' ? '图片' : item.type === 'video' ? '视频' : '文档' }}</ElTag><ElPopconfirm title="确定删除该素材？" @confirm="remove(item.id)"><template #reference><ElButton link type="danger">删除</ElButton></template></ElPopconfirm></div>
        </article>
      </div>
      <ElEmpty v-else description="没有匹配的素材" />
    </ElCard>
  </div>
</template>

<style scoped>
.media-page{min-height:100%;padding:24px;background:#f5f7f8}.page-header{display:flex;align-items:flex-end;justify-content:space-between;padding:28px 30px;border-radius:18px;color:#fff;background:linear-gradient(125deg,#102a35,#125b61)}.page-header p{margin:0;color:#78d2c8;font-size:11px;font-weight:700;letter-spacing:.18em}.page-header h1{margin:5px 0;font-size:28px}.page-header span{color:rgb(255 255 255 / 68%)}.page-header :deep(.el-button--primary){--el-button-bg-color:#e85d45;--el-button-border-color:#e85d45}.media-card{margin-top:16px;border-radius:16px}.toolbar{display:flex;align-items:center;justify-content:space-between;gap:20px;margin-bottom:20px}.toolbar :deep(.el-input){max-width:420px}.toolbar span{color:#849195}.media-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:16px}.media-item{position:relative;overflow:hidden;border:1px solid #e5eaeb;border-radius:14px;background:#fff}.media-item :deep(.el-image),.file-preview{width:100%;height:165px}.file-preview{display:grid;place-content:center;text-align:center;color:#0f6267;background:linear-gradient(145deg,#e3f0ef,#f1f5f5)}.file-preview b{font-size:28px;letter-spacing:.08em}.file-preview span{margin-top:4px;color:#708287}.media-info{padding:14px}.media-info strong,.media-info span{display:block}.media-info strong{overflow:hidden;color:#203a40;text-overflow:ellipsis;white-space:nowrap}.media-info span{margin:5px 0 12px;color:#89969a;font-size:12px}.media-actions{display:flex;align-items:center;justify-content:space-between;padding:0 14px 14px}@media(max-width:1100px){.media-grid{grid-template-columns:repeat(3,1fr)}}@media(max-width:760px){.media-page{padding:14px}.page-header{align-items:flex-start;flex-direction:column;gap:18px}.media-grid{grid-template-columns:1fr 1fr}.toolbar{align-items:flex-start;flex-direction:column}.toolbar :deep(.el-input){max-width:none}}@media(max-width:480px){.media-grid{grid-template-columns:1fr}}
</style>
