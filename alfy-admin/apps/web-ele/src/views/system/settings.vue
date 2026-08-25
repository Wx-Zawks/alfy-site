<script lang="ts" setup>
import type { AdminUserRecord, OperationLogRecord } from '#/api';

import { onMounted, reactive, ref } from 'vue';

import {
  ElAlert,
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
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
} from 'element-plus';

import {
  createAdminUser,
  getSiteSettings,
  listAdminUsers,
  listMedia,
  listOperationLogs,
  saveSiteSettings,
  updateAdminUser,
} from '#/api';
import { mediaIdFromUrl } from '#/data/cms-adapter';

const activeTab = ref('site');
const loading = ref(false);
const saving = ref(false);
const userDialogVisible = ref(false);
const userSaving = ref(false);
const mediaOptions = ref<Array<{ label: string; value: string }>>([]);
const mediaLoading = ref(false);
const mediaLoaded = ref(false);
const usersLoaded = ref(false);
const logsLoaded = ref(false);
const settings = reactive({
  address: '',
  copyrightText: '',
  email: '',
  icp: '',
  logoUrl: '',
  phone: '',
  privacyPolicyUrl: '',
  siteName: '',
  version: undefined as number | undefined,
  wechatQrImageUrl: '',
});
const users = ref<AdminUserRecord[]>([]);
const logs = ref<OperationLogRecord[]>([]);
const userForm = reactive({
  password: '',
  role: 'content_admin' as AdminUserRecord['role'],
  username: '',
});

async function load() {
  loading.value = true;
  try {
    const value = await getSiteSettings();
    Object.assign(settings, {
      address: value.address || '',
      copyrightText: value.copyrightText || '',
      email: value.serviceEmail || '',
      icp: value.icpNumber || '',
      logoUrl: value.logoUrl || '',
      phone: value.servicePhone || '',
      privacyPolicyUrl: value.privacyPolicyUrl || '',
      siteName: value.companyName || '',
      version: value.version,
      wechatQrImageUrl: value.wechatQrImageUrl || '',
    });
  } finally {
    loading.value = false;
  }
}

async function loadTabData(name: number | string) {
  if (name === 'users' && !usersLoaded.value) {
    users.value = await listAdminUsers();
    usersLoaded.value = true;
  }
  if (name === 'logs' && !logsLoaded.value) {
    logs.value = await listOperationLogs();
    logsLoaded.value = true;
  }
}

async function refreshUsers() {
  users.value = await listAdminUsers();
  usersLoaded.value = true;
}

async function ensureMediaOptions(visible = true) {
  if (!visible || mediaLoaded.value || mediaLoading.value) return;
  mediaLoading.value = true;
  try {
    const media = await listMedia('', { page: 1, size: 100 });
    mediaOptions.value = media
      .filter((item) => item.mediaType === 'IMAGE')
      .map((item) => ({ label: item.originalFilename, value: item.adminUrl }));
    mediaLoaded.value = true;
  } finally {
    mediaLoading.value = false;
  }
}

async function saveSettings() {
  saving.value = true;
  try {
    await saveSiteSettings({
      address: settings.address || null,
      companyName: settings.siteName || null,
      copyrightText: settings.copyrightText || null,
      icpNumber: settings.icp || null,
      logoMediaId: mediaIdFromUrl(settings.logoUrl) ?? null,
      privacyPolicyUrl: settings.privacyPolicyUrl || null,
      serviceEmail: settings.email || null,
      servicePhone: settings.phone || null,
      version: settings.version,
      wechatQrMediaId: mediaIdFromUrl(settings.wechatQrImageUrl) ?? null,
    });
    await load();
    ElMessage.success('站点设置已保存到后端');
  } finally {
    saving.value = false;
  }
}

function openCreateUser() {
  Object.assign(userForm, {
    password: '',
    role: 'content_admin',
    username: '',
  });
  userDialogVisible.value = true;
}

async function saveUser() {
  if (!userForm.username.trim() || userForm.password.length < 8) {
    ElMessage.warning('请填写账号，并设置至少 8 位密码');
    return;
  }
  userSaving.value = true;
  try {
    await createAdminUser({
      password: userForm.password,
      role: userForm.role,
      username: userForm.username.trim(),
    });
    userDialogVisible.value = false;
    await refreshUsers();
    ElMessage.success('管理员已创建');
  } finally {
    userSaving.value = false;
  }
}

async function saveUserAccess(value: unknown) {
  const user = value as AdminUserRecord;
  try {
    await updateAdminUser(user.id, {
      enabled: user.enabled,
      role: user.role,
    });
    await refreshUsers();
    ElMessage.success('管理员权限已更新');
  } catch (error) {
    await refreshUsers();
    throw error;
  }
}

onMounted(load);
</script>

<template>
  <div class="settings-page">
    <section class="page-header">
      <div>
        <p>SYSTEM CONTROL</p>
        <h1>系统管理</h1>
        <span>维护站点资料、管理员权限与后台运行配置</span>
      </div>
    </section>
    <ElCard class="settings-card" shadow="never" v-loading="loading">
      <ElTabs
        v-model="activeTab"
        tab-position="left"
        @tab-change="loadTabData"
      >
        <ElTabPane label="站点设置" name="site">
          <div class="section-title">
            <strong>官网基础信息</strong><span>这些信息将由公开 API 提供给官网页头、页脚和联系页面</span>
          </div>
          <ElForm :model="settings" class="settings-form" label-position="top">
            <ElRow :gutter="18">
              <ElCol :md="12" :xs="24">
                <ElFormItem label="站点名称">
                  <ElInput v-model="settings.siteName" />
                </ElFormItem>
              </ElCol>
              <ElCol :md="12" :xs="24">
                <ElFormItem label="ICP备案号">
                  <ElInput v-model="settings.icp" />
                </ElFormItem>
              </ElCol>
              <ElCol :md="12" :xs="24">
                <ElFormItem label="联系电话">
                  <ElInput v-model="settings.phone" />
                </ElFormItem>
              </ElCol>
              <ElCol :md="12" :xs="24">
                <ElFormItem label="联系邮箱">
                  <ElInput v-model="settings.email" />
                </ElFormItem>
              </ElCol>
              <ElCol :md="12" :xs="24">
                <ElFormItem label="站点 Logo">
                  <ElSelect
                    v-model="settings.logoUrl"
                    clearable
                    filterable
                    :loading="mediaLoading"
                    style="width: 100%"
                    @visible-change="ensureMediaOptions"
                  >
                    <ElOption
                      v-for="item in mediaOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </ElSelect>
                </ElFormItem>
              </ElCol>
              <ElCol :md="12" :xs="24">
                <ElFormItem label="微信二维码">
                  <ElSelect
                    v-model="settings.wechatQrImageUrl"
                    clearable
                    filterable
                    :loading="mediaLoading"
                    style="width: 100%"
                    @visible-change="ensureMediaOptions"
                  >
                    <ElOption
                      v-for="item in mediaOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </ElSelect>
                </ElFormItem>
              </ElCol>
              <ElCol :span="24">
                <ElFormItem label="公司地址">
                  <ElInput v-model="settings.address" />
                </ElFormItem>
              </ElCol>
              <ElCol :span="24">
                <ElFormItem label="版权说明">
                  <ElInput
                    v-model="settings.copyrightText"
                    :rows="3"
                    type="textarea"
                  />
                </ElFormItem>
              </ElCol>
              <ElCol :span="24">
                <ElFormItem label="隐私政策地址">
                  <ElInput
                    v-model="settings.privacyPolicyUrl"
                    placeholder="/privacy 或完整网址"
                  />
                </ElFormItem>
              </ElCol>
            </ElRow>
            <ElButton :loading="saving" type="primary" @click="saveSettings">
              保存站点设置
            </ElButton>
          </ElForm>
        </ElTabPane>
        <ElTabPane label="管理员与角色" name="users">
          <div class="section-title">
            <strong>管理员账号</strong>
            <span>创建后台账号，并维护超级管理员与内容管理员权限</span>
          </div>
          <ElButton type="primary" @click="openCreateUser">
            新增管理员
          </ElButton>
          <ElTable :data="users" style="margin-top: 16px">
            <ElTableColumn label="账号" prop="username" />
            <ElTableColumn label="角色" min-width="180">
              <template #default="{ row }">
                <ElSelect
                  v-model="row.role"
                  style="width: 150px"
                  @change="saveUserAccess(row)"
                >
                  <ElOption label="超级管理员" value="super_admin" />
                  <ElOption label="内容管理员" value="content_admin" />
                </ElSelect>
              </template>
            </ElTableColumn>
            <ElTableColumn label="状态" width="140">
              <template #default="{ row }">
                <ElSwitch
                  v-model="row.enabled"
                  active-text="启用"
                  inactive-text="禁用"
                  @change="saveUserAccess(row)"
                />
              </template>
            </ElTableColumn>
            <ElTableColumn
              label="最近登录"
              min-width="180"
              prop="lastLoginAt"
            />
            <ElTableColumn label="创建时间" min-width="180" prop="createdAt" />
          </ElTable>
        </ElTabPane>
        <ElTabPane label="操作日志" name="logs">
          <div class="section-title">
            <strong>操作审计</strong>
            <span>展示登录、内容维护、媒体上传及权限变更记录</span>
          </div>
          <ElTable :data="logs">
            <ElTableColumn label="操作者" prop="operator" />
            <ElTableColumn label="动作" prop="action" />
            <ElTableColumn label="对象类型" min-width="140" prop="objectType" />
            <ElTableColumn label="对象 ID" prop="objectId" width="100" />
            <ElTableColumn label="说明" min-width="280" prop="detail" />
            <ElTableColumn label="时间" min-width="180" prop="createdAt" />
          </ElTable>
        </ElTabPane>
        <ElTabPane label="接口与演示" name="integration">
          <div class="section-title">
            <strong>Spring Boot 联调</strong><span>管理端请求已切换到 alfy-api，不再启动 Nitro Mock</span>
          </div>
          <ElAlert
            :closable="false"
            show-icon
            title="当前已连接真实 API：保存、发布和删除操作会直接影响后端数据库。"
            type="success"
          />
          <div class="integration-grid">
            <div>
              <span>开发 API</span><code>/api/v1 → http://localhost:8080/api/v1</code>
            </div>
            <div>
              <span>正式 API 规划</span><code>https://api.alfy.com.cn/api/v1/admin</code>
            </div>
            <div>
              <span>认证方式</span><code>Bearer accessToken + refresh token</code>
            </div>
            <div>
              <span>统一响应</span><code>{ code: 0, data, message }</code>
            </div>
          </div>
        </ElTabPane>
      </ElTabs>
    </ElCard>
    <ElDialog
      v-model="userDialogVisible"
      :close-on-click-modal="false"
      title="新增管理员"
      width="460px"
    >
      <ElForm :model="userForm" label-position="top">
        <ElFormItem label="账号">
          <ElInput v-model="userForm.username" maxlength="50" />
        </ElFormItem>
        <ElFormItem label="初始密码">
          <ElInput
            v-model="userForm.password"
            autocomplete="new-password"
            minlength="8"
            show-password
            type="password"
          />
        </ElFormItem>
        <ElFormItem label="角色">
          <ElSelect v-model="userForm.role" style="width: 100%">
            <ElOption label="内容管理员" value="content_admin" />
            <ElOption label="超级管理员" value="super_admin" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="userDialogVisible = false">取消</ElButton>
        <ElButton :loading="userSaving" type="primary" @click="saveUser">
          创建
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.settings-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7f8;
}

.page-header {
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

.page-header span {
  color: rgb(255 255 255 / 68%);
}

.settings-card {
  margin-top: 16px;
  border-radius: 16px;
}

.settings-card :deep(.el-tabs__content) {
  padding: 4px 28px 24px;
}

.section-title {
  margin: 4px 0 24px;
}

.section-title strong,
.section-title span {
  display: block;
}

.section-title strong {
  font-size: 19px;
  color: #17363d;
}

.section-title span {
  margin-top: 5px;
  color: #819095;
}

.settings-form {
  max-width: 760px;
}

.integration-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin: 20px 0;
}

.integration-grid div {
  padding: 16px;
  background: #f7f9f9;
  border: 1px solid #e3eaeb;
  border-radius: 12px;
}

.integration-grid span,
.integration-grid code {
  display: block;
}

.integration-grid span {
  margin-bottom: 8px;
  font-size: 12px;
  color: #7c8c91;
}

.integration-grid code {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  color: #0d5e64;
}

@media (max-width: 760px) {
  .settings-page {
    padding: 14px;
  }

  .settings-card :deep(.el-tabs) {
    display: block;
  }

  .settings-card :deep(.el-tabs__header) {
    float: none !important;
    width: auto;
  }

  .settings-card :deep(.el-tabs__nav) {
    display: flex;
    overflow: auto;
  }

  .settings-card :deep(.el-tabs__item) {
    justify-content: flex-start;
  }

  .settings-card :deep(.el-tabs__content) {
    padding: 20px 4px;
  }

  .integration-grid {
    grid-template-columns: 1fr;
  }
}
</style>
