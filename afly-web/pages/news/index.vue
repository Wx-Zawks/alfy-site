<script setup lang="ts">
import type { ApiArticleCategory, ApiArticleDetail, ApiArticleListItem, PageResult } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { useContentMapper } from '~/composables/useContentMapper'

useSeoMeta({ title: '新闻资讯', description: '奥飞新材公司新闻、行业新闻与技术动态。' })
const { request, resolveMediaUrl } = useApiClient()
const { mapArticle } = useContentMapper()
const { data: categoryData } = await useApi<ApiArticleCategory[]>('public-article-categories', '/public/article-categories')
const activeCategory = ref('all')
const NEWS_PAGE_SIZE = 5
const newsPage = ref(1)
const { data: articleData } = await useAsyncData<PageResult<ApiArticleListItem>>(
  'public-news-articles',
  () => request<PageResult<ApiArticleListItem>>('/public/articles', {
    query: {
      category: activeCategory.value === 'all' ? undefined : activeCategory.value,
      page: newsPage.value,
      size: NEWS_PAGE_SIZE
    }
  }),
  { default: () => null, watch: [activeCategory, newsPage] }
)
const categories = computed(() => [
  { code: 'all', name: '全部' },
  ...(categoryData.value ?? []).flatMap(item => [item, ...(item.children ?? [])]).map(item => ({ code: item.code, name: item.name }))
])
const articles = computed(() => (articleData.value?.records ?? []).map(item => mapArticle(item, resolveMediaUrl)))
const newsPageCount = computed(() => Math.ceil((articleData.value?.total ?? 0) / NEWS_PAGE_SIZE))

function articleExcerpt(contentHtml: null | string | undefined, title: string) {
  if (!contentHtml) return ''
  const plainText = contentHtml
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&(?:nbsp|#160);/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/gi, "'")
    .replace(/\s+/g, ' ')
    .trim()
  const withoutTitle = plainText.startsWith(title) ? plainText.slice(title.length).trim() : plainText
  const excerpt = withoutTitle || plainText
  return excerpt.length > 110 ? `${excerpt.slice(0, 110).trimEnd()}…` : excerpt
}

const { data: articleExcerpts } = await useAsyncData<Record<string, string>>(
  'public-news-article-excerpts',
  async () => {
    const excerpts = await Promise.all(articles.value.map(async (article) => {
      if (article.summary.trim()) return [article.slug, article.summary.trim()] as const
      const detail = await request<ApiArticleDetail>(
        `/public/articles/${encodeURIComponent(article.slug)}`,
        { optional: true }
      )
      return [article.slug, detail?.summary?.trim() || articleExcerpt(detail?.contentHtml, article.title)] as const
    }))
    return Object.fromEntries(excerpts)
  },
  { default: () => ({}), watch: [articleData] }
)

const visibleArticles = computed(() => {
  return articles.value.map((article) => {
    const [year = '', month = '', day = ''] = article.date.split('-')
    return {
      ...article,
      summary: article.summary.trim() || articleExcerpts.value[article.slug] || '',
      dateDay: day || '--',
      dateYearMonth: year && month ? `${year}-${month}` : article.date
    }
  })
})

function selectCategory(category: string) {
  if (activeCategory.value === category) return
  newsPage.value = 1
  activeCategory.value = category
}

function changeNewsPage(nextPage: number) {
  newsPage.value = Math.min(Math.max(nextPage, 1), newsPageCount.value)
}

watch(newsPageCount, (count) => {
  newsPage.value = Math.min(newsPage.value, Math.max(count, 1))
})
</script>

<template>
  <div>
    <PageHero page-key="news" eyebrow="Newsroom" title="关注气凝胶产业" highlight="最新进展" description="记录科研、产品、项目与产业合作的重要节点。" image="/images/launch-2.jpg" />

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
            <img :src="item.image" :alt="item.title">
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
