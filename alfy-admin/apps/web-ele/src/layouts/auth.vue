<script lang="ts" setup>
import { computed } from 'vue';

import { AuthPageLayout } from '@vben/layouts';
import { preferences } from '@vben/preferences';

const appName = computed(() => preferences.app.name);
const logo = computed(() => preferences.logo.source);
</script>

<template>
  <AuthPageLayout
    :app-name="appName"
    :logo="logo"
    page-description="安全维护产品、新闻、案例与客户询盘"
    page-title="奥飞新材官网内容管理中心"
  >
    <!-- 自定义工具栏 -->
    <!-- <template #toolbar></template> -->
  </AuthPageLayout>
</template>
