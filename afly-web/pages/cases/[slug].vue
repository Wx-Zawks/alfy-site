<script setup lang="ts">
import type { ApiCaseDetail } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { mapCase } from '~/composables/useContentMapper'
import { breadcrumbJsonLd } from '~/utils/json-ld'

const route = useRoute()
const { resolveMediaUrl } = useApiClient()
const { data } = await useApi<ApiCaseDetail>(
  `public-case-${String(route.params.slug)}`,
  `/public/cases/${encodeURIComponent(String(route.params.slug))}`,
  { optional: true }
)
if (!data.value) throw createError({ statusCode: 404, statusMessage: '案例不存在或尚未发布' })
const item = computed(() => data.value ? mapCase(data.value, resolveMediaUrl) : null)
const { settings: globalSeo } = await useGlobalSeo()
await usePageSeo({
  title: () => item.value?.seo?.title || item.value?.title || '案例详情',
  description: () => item.value?.seo?.description || item.value?.summary || '',
  keywords: () => item.value?.seo?.keywords || '',
  image: () => item.value?.image || '',
  type: 'article',
  jsonLd: () => item.value ? [
    {
      '@context': 'https://schema.org',
      '@type': 'Article',
      headline: item.value.title,
      description: item.value.summary,
      image: item.value.image ? [new URL(item.value.image, globalSeo.value.canonicalOrigin).toString()] : undefined,
      mainEntityOfPage: new URL(route.path, globalSeo.value.canonicalOrigin).toString(),
      publisher: { '@id': `${globalSeo.value.canonicalOrigin}/#organization` }
    },
    breadcrumbJsonLd(globalSeo.value.canonicalOrigin, [
      { name: '首页', path: '/' },
      { name: '应用与案例', path: '/applications' },
      { name: item.value.title, path: route.path }
    ])
  ] : null
})
</script>

<template>
  <div v-if="item" class="content-detail-page">
    <PageHero :eyebrow="item.category" :title="item.title" :description="item.summary" :image="item.image" />
    <section v-if="item.contentHtml" class="section"><div class="container"><div class="cms-rich-text article" v-html="item.contentHtml" /></div></section>
  </div>
</template>
