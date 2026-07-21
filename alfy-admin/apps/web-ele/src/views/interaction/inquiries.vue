<script lang="ts" setup>
import type { Inquiry } from '#/data/cms';

import { computed, reactive, ref } from 'vue';

import {
  ElButton,
  ElCard,
  ElCol,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElRow,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import { cmsState } from '#/data/cms';

const keyword = ref('');
const status = ref('all');
const dialogVisible = ref(false);
const form = reactive<Inquiry>({} as Inquiry);

const statusMap: Record<string, { label: string; type: 'info' | 'success' | 'warning' }> = {
  completed: { label: '已完成', type: 'success' },
  following: { label: '跟进中', type: 'warning' },
  invalid: { label: '无效线索', type: 'info' },
  pending: { label: '待处理', type: 'warning' },
};

function getStatus(value: string) {
  return statusMap[value] || { label: '未知', type: 'info' as const };
}

const filtered = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return cmsState.inquiries.filter(
    (item) =>
      (status.value === 'all' || item.status === status.value) &&
      (!query ||
        item.name.toLowerCase().includes(query) ||
        item.company.toLowerCase().includes(query) ||
        item.phone.includes(query) ||
        item.message.toLowerCase().includes(query)),
  );
});

const stats = computed(() => ({
  completed: cmsState.inquiries.filter((item) => item.status === 'completed').length,
  following: cmsState.inquiries.filter((item) => item.status === 'following').length,
  pending: cmsState.inquiries.filter((item) => item.status === 'pending').length,
  total: cmsState.inquiries.length,
}));

function openInquiry(value: unknown) {
  const item = value as Inquiry;
  Object.assign(form, JSON.parse(JSON.stringify(item)));
  dialogVisible.value = true;
}

function save() {
  const current = cmsState.inquiries.find((item) => item.id === form.id);
  if (current) Object.assign(current, form);
  dialogVisible.value = false;
  ElMessage.success('跟进信息已保存');
}

function exportCsv() {
  const header = '编号,联系人,公司,电话,需求类型,状态,负责人,提交时间';
  const rows = filtered.value.map((item) =>
    [item.id, item.name, item.company, item.phone, item.type, getStatus(item.status).label, item.assignee, item.createdAt]
      .map((value) => `"${String(value).replaceAll('"', '""')}"`)
      .join(','),
  );
  const blob = new Blob([`\uFEFF${[header, ...rows].join('\n')}`], { type: 'text/csv;charset=utf-8' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = `奥飞新材询盘-${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(link.href);
}
</script>

<template>
  <div class="inquiry-page">
    <section class="page-header">
      <div><p>LEAD MANAGEMENT</p><h1>询盘管理</h1><span>集中处理官网表单、来源页面和客户跟进记录</span></div>
      <ElButton size="large" @click="exportCsv">导出当前结果</ElButton>
    </section>

    <ElRow :gutter="16" class="stats">
      <ElCol v-for="item in [{label:'全部询盘',value:stats.total},{label:'待处理',value:stats.pending},{label:'跟进中',value:stats.following},{label:'已完成',value:stats.completed}]" :key="item.label" :lg="6" :sm="12" :xs="24">
        <ElCard shadow="never"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></ElCard>
      </ElCol>
    </ElRow>

    <ElCard class="table-card" shadow="never">
      <div class="filters">
        <ElInput v-model="keyword" clearable placeholder="搜索联系人、公司、电话或需求" />
        <ElSelect v-model="status"><ElOption label="全部状态" value="all" /><ElOption v-for="(item, key) in statusMap" :key="key" :label="item.label" :value="key" /></ElSelect>
      </div>
      <ElTable :data="filtered">
        <ElTableColumn label="编号" width="90"><template #default="{ row }">#{{ row.id }}</template></ElTableColumn>
        <ElTableColumn label="联系人" min-width="170"><template #default="{ row }"><div class="contact"><strong>{{ row.name }}</strong><small>{{ row.phone }}</small></div></template></ElTableColumn>
        <ElTableColumn label="公司" min-width="190" prop="company" />
        <ElTableColumn label="需求类型" width="120" prop="type" />
        <ElTableColumn label="需求摘要" min-width="260" show-overflow-tooltip prop="message" />
        <ElTableColumn label="状态" width="110"><template #default="{ row }"><ElTag :type="getStatus(row.status).type">{{ getStatus(row.status).label }}</ElTag></template></ElTableColumn>
        <ElTableColumn label="负责人" width="100" prop="assignee" />
        <ElTableColumn label="提交时间" min-width="150" prop="createdAt" />
        <ElTableColumn fixed="right" label="操作" width="90"><template #default="{ row }"><ElButton link type="primary" @click="openInquiry(row)">跟进</ElButton></template></ElTableColumn>
      </ElTable>
    </ElCard>

    <ElDialog v-model="dialogVisible" title="询盘详情与跟进" width="680px">
      <div class="detail-grid">
        <div><span>联系人</span><strong>{{ form.name }} · {{ form.phone }}</strong></div>
        <div><span>公司</span><strong>{{ form.company }}</strong></div>
        <div><span>邮箱</span><strong>{{ form.email }}</strong></div>
        <div><span>来源页面</span><strong>{{ form.source }}</strong></div>
      </div>
      <div class="message-box"><span>客户需求</span><p>{{ form.message }}</p></div>
      <ElForm label-position="top">
        <ElRow :gutter="16">
          <ElCol :span="12"><ElFormItem label="跟进状态"><ElSelect v-model="form.status" style="width: 100%"><ElOption v-for="(item, key) in statusMap" :key="key" :label="item.label" :value="key" /></ElSelect></ElFormItem></ElCol>
          <ElCol :span="12"><ElFormItem label="负责人"><ElInput v-model="form.assignee" /></ElFormItem></ElCol>
        </ElRow>
        <ElFormItem label="跟进记录"><ElInput v-model="form.followUp" :rows="4" placeholder="填写联系结果、客户需求和下一步计划" type="textarea" /></ElFormItem>
      </ElForm>
      <template #footer><ElButton @click="dialogVisible = false">取消</ElButton><ElButton type="primary" @click="save">保存跟进</ElButton></template>
    </ElDialog>
  </div>
</template>

<style scoped>
.inquiry-page { min-height: 100%; padding: 24px; background: #f5f7f8; }.page-header { display: flex; align-items: flex-end; justify-content: space-between; padding: 28px 30px; border-radius: 18px; color: #fff; background: linear-gradient(120deg,#142e3b,#1d5860); }.page-header p { margin: 0; color: #77d2c8; font-size: 11px; font-weight: 700; letter-spacing: .18em; }.page-header h1 { margin: 5px 0; font-size: 28px; }.page-header span { color: rgb(255 255 255 / 68%); }.page-header :deep(.el-button) { color: #fff; background: rgb(255 255 255 / 8%); border-color: rgb(255 255 255 / 28%); }.stats { margin-top: 16px; }.stats :deep(.el-card) { margin-bottom: 16px; border-radius: 14px; }.stats :deep(.el-card__body) { display: flex; justify-content: space-between; align-items: center; padding: 18px 20px; }.stats span { color: #718187; }.stats strong { color: #14343b; font-size: 26px; }.table-card { border-radius: 16px; }.filters { display: grid; grid-template-columns: minmax(260px,1fr) 180px; gap: 12px; margin-bottom: 18px; }.contact strong,.contact small { display: block; }.contact small { margin-top: 3px; color: #8b989c; }.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 14px; }.detail-grid div,.message-box { padding: 13px 15px; border-radius: 10px; background: #f4f7f7; }.detail-grid span,.message-box span { display: block; margin-bottom: 5px; color: #879397; font-size: 12px; }.message-box { margin-bottom: 18px; }.message-box p { margin: 0; color: #263e44; }.detail-grid strong { color: #263e44; }.message-box p { line-height: 1.7; }@media(max-width:760px){.inquiry-page{padding:14px}.page-header{align-items:flex-start;flex-direction:column;gap:18px}.filters,.detail-grid{grid-template-columns:1fr}}
</style>
