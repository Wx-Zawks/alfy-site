<script setup lang="ts">
import type { ApiArticleCategory, ApiArticleListItem, PageResult } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { mapArticle } from '~/composables/useContentMapper'

useSeoMeta({ title: '新闻资讯', description: '奥飞新材公司新闻、行业新闻与技术动态。' })
const { resolveMediaUrl } = useApiClient()
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
const NEWS_PAGE_SIZE = 5
const newsPage = ref(1)
const newsPageCount = computed(() => Math.ceil(filtered.value.length / NEWS_PAGE_SIZE))
const pageArticles = computed(() => {
  const start = (newsPage.value - 1) * NEWS_PAGE_SIZE
  return filtered.value.slice(start, start + NEWS_PAGE_SIZE)
})

const visibleArticles = computed(() => {
  return pageArticles.value.map((article) => {
    const [year = '', month = '', day = ''] = article.date.split('-')
    return {
      ...article,
      summary: article.summary.trim(),
      dateDay: day || '--',
      dateYearMonth: year && month ? `${year}-${month}` : article.date
    }
  })
})

function selectCategory(category: string) {
  activeCategory.value = category
  newsPage.value = 1
}

function changeNewsPage(nextPage: number) {
  newsPage.value = Math.min(Math.max(nextPage, 1), newsPageCount.value)
}

watch(filtered, () => {
  newsPage.value = Math.min(newsPage.value, Math.max(newsPageCount.value, 1))
})
</script>

<template>
  <div class="brief-page news-page">
    <PageHero
      class="brief-hero"
      page-key="news"
      eyebrow="Newsroom"
      title="关注气凝胶产业"
      highlight="最新进展"
      description="记录科研、产品、项目与产业合作的重要节点。"
      image="/images/launch-2.jpg"
    />

    <nav class="technology-subnav news-category-nav" aria-label="新闻分类">
      <div class="container">
        <button
          v-for="category in categories"
          :key="category.code"
          :class="{ active: activeCategory === category.code }"
          type="button"
          @click="selectCategory(category.code)"
        >
          {{ category.name }}
        </button>
      </div>
    </nav>

    <section class="section news-archive-section">
      <div class="container">
        <div v-if="visibleArticles.length" class="news-index-list">
          <NuxtLink
            v-for="(item, index) in visibleArticles"
            :key="item.id"
            class="news-index-row"
            :class="{ featured: newsPage === 1 && index === 0 }"
            :to="`/news/${item.slug}`"
          >
            <time class="news-index-date" :datetime="item.date">
              <strong>{{ item.dateDay }}</strong>
              <span>{{ item.dateYearMonth }}</span>
            </time>
            <div class="news-index-copy">
              <small>{{ item.categoryName }} · {{ item.date }}</small>
              <h2>{{ item.title }}</h2>
              <p v-if="item.summary">{{ item.summary }}</p>
              <b>查看详情 →</b>
            </div>
            <img :src="item.image" :alt="item.title" decoding="async" loading="lazy">
          </NuxtLink>
        </div>
        <p v-else class="empty-state">该栏目暂无已发布内容。</p>
        <nav v-if="newsPageCount > 1" class="news-index-pager" aria-label="新闻列表分页">
          <button :disabled="newsPage === 1" type="button" @click="changeNewsPage(newsPage - 1)">← 上一页</button>
          <span>第 {{ newsPage }} / {{ newsPageCount }} 页</span>
          <button :disabled="newsPage === newsPageCount" type="button" @click="changeNewsPage(newsPage + 1)">下一页 →</button>
        </nav>
      </div>
    </section>
  </div>
</template>
