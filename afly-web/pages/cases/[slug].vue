<script setup lang="ts">
import type { ApiCaseDetail } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { mapCase } from '~/composables/useContentMapper'

const route = useRoute()
const { resolveMediaUrl } = useApiClient()
const { data } = await useApi<ApiCaseDetail>(
  `public-case-${String(route.params.slug)}`,
  `/public/cases/${encodeURIComponent(String(route.params.slug))}`,
  { optional: true }
)
if (!data.value) throw createError({ statusCode: 404, statusMessage: '案例不存在或尚未发布' })
const item = computed(() => data.value ? mapCase(data.value, resolveMediaUrl) : null)
useSeoMeta({
  title: () => item.value?.seo?.title || item.value?.title || '案例详情',
  description: () => item.value?.seo?.description || item.value?.summary || '',
  keywords: () => item.value?.seo?.keywords || ''
})
</script>

<template>
  <div v-if="item" class="content-detail-page">
    <PageHero :eyebrow="item.category" :title="item.title" :description="item.summary" :image="item.image" />
    <section v-if="item.contentHtml" class="section"><div class="container"><div class="cms-rich-text article" v-html="item.contentHtml" /></div></section>
  </div>
</template>
