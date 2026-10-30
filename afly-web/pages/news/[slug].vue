<script setup lang="ts">
import type { ApiArticleDetail } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { mapArticle } from '~/composables/useContentMapper'
import { breadcrumbJsonLd } from '~/utils/json-ld'

const route = useRoute()
const { resolveMediaUrl } = useApiClient()
const { data } = await useApi<ApiArticleDetail>(
  `public-article-${String(route.params.slug)}`,
  `/public/articles/${encodeURIComponent(String(route.params.slug))}`,
  { optional: true }
)
if (!data.value) throw createError({ statusCode: 404, statusMessage: '新闻不存在或尚未发布' })
const article = computed(() => data.value ? mapArticle(data.value, resolveMediaUrl) : null)
const { settings: globalSeo } = await useGlobalSeo()
await usePageSeo({
  title: () => data.value?.seo?.title || article.value?.title || '新闻详情',
  description: () => data.value?.seo?.description || article.value?.summary || '',
  keywords: () => data.value?.seo?.keywords || '',
  image: () => article.value?.image || '',
  type: 'article',
  publishedTime: () => data.value?.publishedAt || data.value?.sourcePublishedAt || '',
  jsonLd: () => article.value ? [
    {
      '@context': 'https://schema.org',
      '@type': 'Article',
      headline: article.value.title,
      description: article.value.summary,
      image: article.value.image ? [new URL(article.value.image, globalSeo.value.canonicalOrigin).toString()] : undefined,
      datePublished: data.value?.publishedAt || data.value?.sourcePublishedAt || undefined,
      mainEntityOfPage: new URL(route.path, globalSeo.value.canonicalOrigin).toString(),
      publisher: { '@id': `${globalSeo.value.canonicalOrigin}/#organization` }
    },
    breadcrumbJsonLd(globalSeo.value.canonicalOrigin, [
      { name: '首页', path: '/' },
      { name: '新闻资讯', path: '/news' },
      { name: article.value.title, path: route.path }
    ])
  ] : null
})
</script>

<template>
  <div v-if="article" class="content-detail-page">
    <PageHero :eyebrow="`${article.categoryName}${article.date ? ` · ${article.date}` : ''}`" :title="article.title" :description="article.summary" :image="article.image" />
    <section class="section"><article class="container article"><div v-if="article.contentHtml" class="cms-rich-text" v-html="article.contentHtml" /><p v-else class="lead">{{ article.summary }}</p></article></section>
  </div>
</template>
