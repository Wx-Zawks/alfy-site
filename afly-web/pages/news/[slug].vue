<script setup lang="ts">
import type { ApiArticleDetail } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { useContentMapper } from '~/composables/useContentMapper'

const route = useRoute()
const { resolveMediaUrl } = useApiClient()
const { mapArticle } = useContentMapper()
const { data } = await useApi<ApiArticleDetail>(
  `public-article-${String(route.params.slug)}`,
  `/public/articles/${encodeURIComponent(String(route.params.slug))}`,
  { optional: true }
)
if (!data.value) throw createError({ statusCode: 404, statusMessage: '新闻不存在或尚未发布' })
const article = computed(() => data.value ? mapArticle(data.value, resolveMediaUrl) : null)
const requestUrl = useRequestURL()
const canonicalUrl = computed(() => new URL(route.path, requestUrl.origin).toString())
useSeoMeta({
  title: () => data.value?.seo?.title || article.value?.title || '新闻详情',
  description: () => data.value?.seo?.description || article.value?.summary || '',
  keywords: () => data.value?.seo?.keywords || ''
})
useHead({
  link: [{ rel: 'canonical', href: () => canonicalUrl.value }]
})
</script>

<template>
  <div v-if="article" class="content-detail-page">
    <PageHero :eyebrow="`${article.categoryName}${article.date ? ` · ${article.date}` : ''}`" :title="article.title" :description="article.summary" :image="article.image" />
    <section class="section"><article class="container article"><div v-if="article.contentHtml" class="cms-rich-text" v-html="article.contentHtml" /><p v-else class="lead">{{ article.summary }}</p><p v-if="article.sourceUrl" class="article-source"><a :href="article.sourceUrl" target="_blank" rel="noopener noreferrer">查看原文来源 →</a></p></article></section>
  </div>
</template>
