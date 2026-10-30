<script lang="ts" setup>
import type { SeoCrawlerPolicy } from '#/api';

import { computed, onMounted, reactive, ref } from 'vue';

import { useUserStore } from '@vben/stores';

import {
  ElAlert,
  ElButton,
  ElCol,
  ElDatePicker,
  ElDivider,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElRow,
  ElSelect,
  ElSwitch,
} from 'element-plus';

import { getSeoGlobalSettings, saveSeoGlobalSettings } from '#/api';

const userStore = useUserStore();
const loading = ref(false);
const saving = ref(false);
const sameAsText = ref('');
const canEdit = computed(() =>
  (userStore.userInfo?.roles || []).includes('super_admin'),
);

const form = reactive({
  baiduAnalyticsEnabled: false,
  baiduAnalyticsId: '',
  baiduSiteVerification: '',
  bingSiteVerification: '',
  canonicalOrigin: 'https://alfy.com.cn',
  clarityEnabled: false,
  clarityProjectId: '',
  defaultDescription: '',
  defaultOgMediaId: undefined as number | undefined,
  defaultTitle: '',
  foundingDate: '',
  ga4Enabled: false,
  ga4MeasurementId: '',
  googleExtendedPolicy: 'allow' as SeoCrawlerPolicy,
  googleSiteVerification: '',
  gptbotPolicy: 'allow' as SeoCrawlerPolicy,
  oaiSearchbotPolicy: 'allow' as SeoCrawlerPolicy,
  organizationAlternateName: '',
  organizationDescription: '',
  organizationLegalName: '',
  organizationName: '',
  publicIndexEnabled: true,
  titleTemplate: '%s｜奥飞新材',
  version: undefined as number | undefined,
});

async function load() {
  loading.value = true;
  try {
    const data = await getSeoGlobalSettings();
    Object.assign(form, {
      ...data,
      baiduAnalyticsId: data.baiduAnalyticsId || '',
      baiduSiteVerification: data.baiduSiteVerification || '',
      bingSiteVerification: data.bingSiteVerification || '',
      clarityProjectId: data.clarityProjectId || '',
      defaultOgMediaId: data.defaultOgMediaId || undefined,
      foundingDate: data.foundingDate || '',
      ga4MeasurementId: data.ga4MeasurementId || '',
      googleSiteVerification: data.googleSiteVerification || '',
      organizationAlternateName: data.organizationAlternateName || '',
      organizationDescription: data.organizationDescription || '',
      organizationLegalName: data.organizationLegalName || '',
    });
    sameAsText.value = (data.sameAs || []).join('\n');
  } finally {
    loading.value = false;
  }
}

function validate() {
  if (!/^https:\/\/[^/?#]+$/.test(form.canonicalOrigin)) {
    ElMessage.warning(
      '正式域名必须是无路径的 HTTPS 地址，例如 https://alfy.com.cn',
    );
    return false;
  }
  if ((form.titleTemplate.match(/%s/g) || []).length !== 1) {
    ElMessage.warning('标题模板必须且只能包含一个 %s');
    return false;
  }
  if (!form.defaultTitle.trim() || !form.defaultDescription.trim()) {
    ElMessage.warning('请填写默认标题和默认描述');
    return false;
  }
  if (!form.organizationName.trim()) {
    ElMessage.warning('请填写公司标准名称');
    return false;
  }
  if (form.ga4Enabled && !/^G-[A-Z0-9]+$/.test(form.ga4MeasurementId)) {
    ElMessage.warning('GA4 Measurement ID 格式应为 G-XXXXXXXXXX');
    return false;
  }
  return true;
}

async function save() {
  if (!canEdit.value || !validate()) return;
  saving.value = true;
  try {
    const sameAs = sameAsText.value
      .split(/\r?\n/)
      .map((item) => item.trim())
      .filter(Boolean);
    const saved = await saveSeoGlobalSettings({ ...form, sameAs });
    Object.assign(form, saved);
    sameAsText.value = saved.sameAs.join('\n');
    ElMessage.success('全局 SEO 配置已保存');
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="settings-pane" v-loading="loading">
    <ElAlert
      v-if="!canEdit"
      :closable="false"
      show-icon
      title="当前账号可以查看全局配置，但只有超级管理员可以保存修改。"
      type="info"
    />
    <ElAlert
      :closable="false"
      show-icon
      title="不要粘贴完整统计脚本，只填写平台提供的公开 ID 或验证 token。密钥和 API Token 必须保存在服务器环境变量中。"
      type="warning"
    />

    <ElForm class="seo-form" label-position="top">
      <ElDivider content-position="left">站点默认规则</ElDivider>
      <ElRow :gutter="18">
        <ElCol :md="12" :xs="24">
          <ElFormItem label="正式域名">
            <ElInput
              v-model="form.canonicalOrigin"
              placeholder="https://alfy.com.cn"
            />
          </ElFormItem>
        </ElCol>
        <ElCol :md="12" :xs="24">
          <ElFormItem label="标题模板">
            <ElInput v-model="form.titleTemplate" placeholder="%s｜奥飞新材" />
          </ElFormItem>
        </ElCol>
        <ElCol :md="12" :xs="24">
          <ElFormItem label="默认标题">
            <ElInput
              v-model="form.defaultTitle"
              maxlength="255"
              show-word-limit
            />
          </ElFormItem>
        </ElCol>
        <ElCol :md="12" :xs="24">
          <ElFormItem label="默认 Open Graph 图片媒体 ID">
            <ElInputNumber
              v-model="form.defaultOgMediaId"
              :min="1"
              controls-position="right"
            />
          </ElFormItem>
        </ElCol>
        <ElCol :span="24">
          <ElFormItem label="默认描述">
            <ElInput
              v-model="form.defaultDescription"
              :rows="3"
              maxlength="500"
              show-word-limit
              type="textarea"
            />
          </ElFormItem>
        </ElCol>
        <ElCol :span="24">
          <ElFormItem label="允许公开站点被索引">
            <ElSwitch v-model="form.publicIndexEnabled" />
            <span class="field-help">关闭后应由官网 robots 和页面 meta 统一输出
              noindex；正式站不要误关。</span>
          </ElFormItem>
        </ElCol>
      </ElRow>

      <ElDivider content-position="left">
        公司实体信息（用于 GEO 和 Organization JSON-LD）
      </ElDivider>
      <ElRow :gutter="18">
        <ElCol :md="12" :xs="24">
          <ElFormItem label="公司标准名称">
            <ElInput v-model="form.organizationName" maxlength="255" />
          </ElFormItem>
        </ElCol>
        <ElCol :md="12" :xs="24">
          <ElFormItem label="公司简称">
            <ElInput v-model="form.organizationAlternateName" maxlength="100" />
          </ElFormItem>
        </ElCol>
        <ElCol :md="12" :xs="24">
          <ElFormItem label="法定名称">
            <ElInput v-model="form.organizationLegalName" maxlength="255" />
          </ElFormItem>
        </ElCol>
        <ElCol :md="12" :xs="24">
          <ElFormItem label="成立日期">
            <ElDatePicker
              v-model="form.foundingDate"
              format="YYYY-MM-DD"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </ElFormItem>
        </ElCol>
        <ElCol :span="24">
          <ElFormItem label="公司实体说明">
            <ElInput
              v-model="form.organizationDescription"
              :rows="3"
              maxlength="1000"
              show-word-limit
              type="textarea"
            />
          </ElFormItem>
        </ElCol>
        <ElCol :span="24">
          <ElFormItem
            label="官方或权威资料链接（每行一个 HTTPS URL，最多 20 条）"
          >
            <ElInput
              v-model="sameAsText"
              :rows="4"
              placeholder="https://..."
              type="textarea"
            />
          </ElFormItem>
        </ElCol>
      </ElRow>

      <ElDivider content-position="left">站长平台验证</ElDivider>
      <ElRow :gutter="18">
        <ElCol :md="8" :xs="24">
          <ElFormItem label="百度验证 content 值">
            <ElInput
              v-model="form.baiduSiteVerification"
              placeholder="只填写 content 值"
            />
          </ElFormItem>
        </ElCol>
        <ElCol :md="8" :xs="24">
          <ElFormItem label="Google 验证 content 值">
            <ElInput
              v-model="form.googleSiteVerification"
              placeholder="只填写 content 值"
            />
          </ElFormItem>
        </ElCol>
        <ElCol :md="8" :xs="24">
          <ElFormItem label="Bing 验证 content 值">
            <ElInput
              v-model="form.bingSiteVerification"
              placeholder="只填写 content 值"
            />
          </ElFormItem>
        </ElCol>
      </ElRow>

      <ElDivider content-position="left">统计工具</ElDivider>
      <ElRow :gutter="18">
        <ElCol :md="8" :xs="24">
          <ElFormItem label="Google Analytics 4">
            <ElSwitch v-model="form.ga4Enabled" />
            <ElInput
              v-model="form.ga4MeasurementId"
              class="id-input"
              placeholder="G-XXXXXXXXXX"
            />
          </ElFormItem>
        </ElCol>
        <ElCol :md="8" :xs="24">
          <ElFormItem label="百度统计">
            <ElSwitch v-model="form.baiduAnalyticsEnabled" />
            <ElInput
              v-model="form.baiduAnalyticsId"
              class="id-input"
              placeholder="公开站点 ID"
            />
          </ElFormItem>
        </ElCol>
        <ElCol :md="8" :xs="24">
          <ElFormItem label="Microsoft Clarity">
            <ElSwitch v-model="form.clarityEnabled" />
            <ElInput
              v-model="form.clarityProjectId"
              class="id-input"
              placeholder="Project ID"
            />
          </ElFormItem>
        </ElCol>
      </ElRow>

      <ElDivider content-position="left">AI 爬虫策略</ElDivider>
      <ElRow :gutter="18">
        <ElCol :md="8" :xs="24">
          <ElFormItem label="OAI-SearchBot">
            <ElSelect v-model="form.oaiSearchbotPolicy">
              <ElOption label="允许" value="allow" />
              <ElOption label="禁止" value="disallow" />
            </ElSelect>
          </ElFormItem>
        </ElCol>
        <ElCol :md="8" :xs="24">
          <ElFormItem label="GPTBot">
            <ElSelect v-model="form.gptbotPolicy">
              <ElOption label="允许" value="allow" />
              <ElOption label="禁止" value="disallow" />
            </ElSelect>
          </ElFormItem>
        </ElCol>
        <ElCol :md="8" :xs="24">
          <ElFormItem label="Google-Extended">
            <ElSelect v-model="form.googleExtendedPolicy">
              <ElOption label="允许" value="allow" />
              <ElOption label="禁止" value="disallow" />
            </ElSelect>
          </ElFormItem>
        </ElCol>
      </ElRow>

      <div class="actions">
        <ElButton @click="load">重新读取</ElButton>
        <ElButton
          :disabled="!canEdit"
          :loading="saving"
          type="primary"
          @click="save"
        >
          保存全局配置
        </ElButton>
      </div>
    </ElForm>
  </div>
</template>

<style scoped>
.settings-pane {
  padding: 8px 2px 20px;
}

.settings-pane :deep(.el-alert) {
  margin-bottom: 14px;
}

.seo-form {
  margin-top: 10px;
}

.field-help {
  margin-left: 12px;
  color: #819095;
}

.id-input {
  width: calc(100% - 54px);
  margin-left: 10px;
}

.actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding-top: 12px;
}

@media (max-width: 760px) {
  .id-input {
    width: 100%;
    margin: 10px 0 0;
  }

  .field-help {
    display: block;
    width: 100%;
    margin: 8px 0 0;
  }
}
</style>
