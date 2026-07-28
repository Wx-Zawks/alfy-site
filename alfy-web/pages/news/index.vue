<script setup lang="ts">
import type { ApiArticleCategory, ApiArticleListItem, PageResult } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { useContentMapper } from '~/composables/useContentMapper'

useSeoMeta({ title: '新闻资讯', description: '奥飞新材公司新闻、行业新闻与技术动态。' })
const { resolveMediaUrl } = useApiClient()
const { mapArticle } = useContentMapper()
const [{ data: categoryData }, { data: articleData }] = await Promise.all([
  useApi<ApiArticleCategory[]>('public-article-categories', '/public/article-categories'),
  useApi<PageResult<ApiArticleListItem>>('public-articles', '/public/articles?page=1&size=100')
])
const categories = computed(() => [
  { code: 'all', name: '全部' },
  ...(categoryData.value ?? []).flatMap(item => [item, ...(item.children ?? [])]).map(item => ({ code: item.code, name: item.name }))
])
const articles = computed(() => (articleData.value?.records ?? []).map(item => mapArticle(item, resolveMediaUrl)))
const activeCategory = ref('all')
const filtered = computed(() => activeCategory.value === 'all' ? articles.value : articles.value.filter(item => item.category === activeCategory.value))
const featured = computed(() => filtered.value[0])
const remaining = computed(() => filtered.value.slice(1))
</script>

<template>
  <div><PageHero page-key="news" eyebrow="Newsroom" title="关注气凝胶产业" highlight="最新进展" description="记录科研、产品、项目与产业合作的重要节点。" image="/images/launch-2.jpg" /><section class="section news-archive-section"><div class="container"><div class="section-heading news-archive-heading"><div><p class="eyebrow">新闻与观点</p><h2>全部资讯</h2></div><div class="filters"><button v-for="category in categories" :key="category.code" :class="{ active: activeCategory === category.code }" @click="activeCategory = category.code">{{ category.name }}</button></div></div><div v-if="featured" class="news-archive"><NuxtLink class="news-archive-feature" :to="`/news/${featured.slug}`"><img :src="featured.image" :alt="featured.title"><div><span>{{ featured.categoryName }} · {{ featured.date }}</span><h2>{{ featured.title }}</h2><p>{{ featured.summary }}</p><b>阅读全文 →</b></div></NuxtLink><div class="news-archive-list"><NuxtLink v-for="item in remaining" :key="item.id" :to="`/news/${item.slug}`"><div><span>{{ item.categoryName }}</span><time>{{ item.date }}</time></div><h3>{{ item.title }}</h3><p>{{ item.summary }}</p><b>查看详情 →</b></NuxtLink></div></div><p v-else class="empty-state">该栏目暂无已发布内容。</p></div></section></div>
</template>
