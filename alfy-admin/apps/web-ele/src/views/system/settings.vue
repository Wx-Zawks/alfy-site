<script lang="ts" setup>
import { reactive, ref } from 'vue';

import {
  ElAlert,
  ElButton,
  ElCard,
  ElCol,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElRow,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag,
} from 'element-plus';

import { cmsState, resetCmsState } from '#/data/cms';

const activeTab = ref('site');
const settings = reactive({ ...cmsState.settings });
const users = [
  { username: 'admin', realName: '系统管理员', role: '超级管理员', status: '启用', lastLogin: '2026-07-17 14:02' },
  { username: 'editor', realName: '内容运营', role: '内容管理员', status: '启用', lastLogin: '2026-07-16 17:35' },
  { username: 'inquiry', realName: '商务跟进', role: '询盘人员', status: '待启用', lastLogin: '-' },
];
const logs = [
  { operator: '系统管理员', action: '更新', object: '首页 Banner / 新一代气凝胶技术', ip: '127.0.0.1', time: '2026-07-17 15:30' },
  { operator: '内容运营', action: '发布', object: '新闻 / 产业化应用新范式', ip: '127.0.0.1', time: '2026-07-17 11:48' },
  { operator: '系统管理员', action: '修改', object: '301 重定向规则', ip: '127.0.0.1', time: '2026-07-16 16:20' },
];

function saveSettings() {
  Object.assign(cmsState.settings, settings);
  ElMessage.success('站点设置已保存');
}

function resetDemo() {
  resetCmsState();
  Object.assign(settings, cmsState.settings);
  ElMessage.success('演示数据已恢复');
}
</script>

<template>
  <div class="settings-page">
    <section class="page-header"><div><p>SYSTEM CONTROL</p><h1>系统管理</h1><span>维护站点资料、管理员权限与后台运行配置</span></div></section>
    <ElCard class="settings-card" shadow="never">
      <ElTabs v-model="activeTab" tab-position="left">
        <ElTabPane label="站点设置" name="site">
          <div class="section-title"><strong>官网基础信息</strong><span>这些信息将由公开 API 提供给官网页头、页脚和联系页面</span></div>
          <ElForm :model="settings" label-position="top" class="settings-form">
            <ElRow :gutter="18">
              <ElCol :md="12" :xs="24"><ElFormItem label="站点名称"><ElInput v-model="settings.siteName" /></ElFormItem></ElCol>
              <ElCol :md="12" :xs="24"><ElFormItem label="ICP备案号"><ElInput v-model="settings.icp" /></ElFormItem></ElCol>
              <ElCol :md="12" :xs="24"><ElFormItem label="联系电话"><ElInput v-model="settings.phone" /></ElFormItem></ElCol>
              <ElCol :md="12" :xs="24"><ElFormItem label="联系邮箱"><ElInput v-model="settings.email" /></ElFormItem></ElCol>
              <ElCol :span="24"><ElFormItem label="公司地址"><ElInput v-model="settings.address" /></ElFormItem></ElCol>
              <ElCol :span="24"><ElFormItem label="站点默认描述"><ElInput v-model="settings.siteDescription" :rows="3" type="textarea" /></ElFormItem></ElCol>
            </ElRow>
            <ElButton type="primary" @click="saveSettings">保存站点设置</ElButton>
          </ElForm>
        </ElTabPane>
        <ElTabPane label="管理员与角色" name="users">
          <div class="section-title"><strong>管理员账号</strong><span>一期角色：超级管理员、内容管理员、询盘人员</span></div>
          <ElTable :data="users"><ElTableColumn label="账号" prop="username" /><ElTableColumn label="姓名" prop="realName" /><ElTableColumn label="角色" prop="role" /><ElTableColumn label="状态"><template #default="{row}"><ElTag :type="row.status==='启用'?'success':'info'">{{row.status}}</ElTag></template></ElTableColumn><ElTableColumn label="最近登录" prop="lastLogin" /></ElTable>
        </ElTabPane>
        <ElTabPane label="操作日志" name="logs">
          <div class="section-title"><strong>操作审计</strong><span>真实后端接入后记录修改前后内容、操作者与 IP</span></div>
          <ElTable :data="logs"><ElTableColumn label="操作者" prop="operator" /><ElTableColumn label="动作" prop="action" /><ElTableColumn label="对象" min-width="280" prop="object" /><ElTableColumn label="IP" prop="ip" /><ElTableColumn label="时间" min-width="170" prop="time" /></ElTable>
        </ElTabPane>
        <ElTabPane label="接口与演示" name="integration">
          <div class="section-title"><strong>Spring Boot 联调</strong><span>当前使用本地 Mock 和浏览器存储，可随时切换真实 API</span></div>
          <ElAlert :closable="false" show-icon title="当前是前端演示模式：内容修改会保存在本机浏览器，不会影响正式官网。" type="warning" />
          <div class="integration-grid">
            <div><span>开发 API</span><code>/api → http://localhost:5320/api</code></div>
            <div><span>正式 API 规划</span><code>https://api.alfy.com.cn/api/v1/admin</code></div>
            <div><span>认证方式</span><code>Bearer accessToken + refresh token</code></div>
            <div><span>统一响应</span><code>{ code: 0, data, message }</code></div>
          </div>
          <ElButton type="danger" plain @click="resetDemo">恢复全部演示数据</ElButton>
        </ElTabPane>
      </ElTabs>
    </ElCard>
  </div>
</template>

<style scoped>
.settings-page{min-height:100%;padding:24px;background:#f5f7f8}.page-header{padding:28px 30px;border-radius:18px;color:#fff;background:linear-gradient(125deg,#102a35,#125b61)}.page-header p{margin:0;color:#78d2c8;font-size:11px;font-weight:700;letter-spacing:.18em}.page-header h1{margin:5px 0;font-size:28px}.page-header span{color:rgb(255 255 255 / 68%)}.settings-card{margin-top:16px;border-radius:16px}.settings-card :deep(.el-tabs__content){padding:4px 28px 24px}.section-title{margin:4px 0 24px}.section-title strong,.section-title span{display:block}.section-title strong{color:#17363d;font-size:19px}.section-title span{margin-top:5px;color:#819095}.settings-form{max-width:760px}.integration-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin:20px 0}.integration-grid div{padding:16px;border:1px solid #e3eaeb;border-radius:12px;background:#f7f9f9}.integration-grid span,.integration-grid code{display:block}.integration-grid span{margin-bottom:8px;color:#7c8c91;font-size:12px}.integration-grid code{color:#0d5e64;font-family:ui-monospace,SFMono-Regular,Consolas,monospace}@media(max-width:760px){.settings-page{padding:14px}.settings-card :deep(.el-tabs){display:block}.settings-card :deep(.el-tabs__header){float:none!important;width:auto}.settings-card :deep(.el-tabs__nav){display:flex;overflow:auto}.settings-card :deep(.el-tabs__item){justify-content:flex-start}.settings-card :deep(.el-tabs__content){padding:20px 4px}.integration-grid{grid-template-columns:1fr}}
</style>
