<script setup lang="ts">
import type { ApiCaseDetail } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { useContentMapper } from '~/composables/useContentMapper'

const route = useRoute()
const { resolveMediaUrl } = useApiClient()
const { mapCase } = useContentMapper()
const { data } = await useApi<ApiCaseDetail>(
  `public-case-${String(route.params.slug)}`,
  `/public/cases/${encodeURIComponent(String(route.params.slug))}`,
  { optional: true }
)
if (!data.value) throw createError({ statusCode: 404, statusMessage: '案例不存在或尚未发布' })
const item = computed(() => data.value ? mapCase(data.value, resolveMediaUrl) : null)
const solutionBlocks = computed(() => item.value ? [
  { label: '01', title: '客户需求', content: item.value.customerNeed },
  { label: '02', title: '奥飞方案', content: item.value.solution },
  { label: '03', title: '实施过程', content: item.value.implementation },
  { label: '04', title: '项目结果', content: item.value.resultSummary }
].filter(block => block.content) : [])
useSeoMeta({
  title: () => item.value?.seo?.title || item.value?.title || '案例详情',
  description: () => item.value?.seo?.description || item.value?.summary || '',
  keywords: () => item.value?.seo?.keywords || ''
})
</script>

<template>
  <div v-if="item" class="content-detail-page">
    <PageHero :eyebrow="item.category" :title="item.title" :highlight="item.location" :description="item.summary" :image="item.image" />
    <section v-if="solutionBlocks.length" class="section section-muted"><div class="container"><div class="section-heading"><div><p class="eyebrow">需求与方案</p><h2>从项目问题反推材料组合</h2></div></div><div class="info-grid"><article v-for="block in solutionBlocks" :key="block.label" class="info-card"><span>{{ block.label }}</span><h3>{{ block.title }}</h3><p>{{ block.content }}</p></article></div></div></section>
    <section v-if="item.contentHtml" class="section"><div class="container"><div class="cms-rich-text article" v-html="item.contentHtml" /></div></section>
  </div>
</template>
