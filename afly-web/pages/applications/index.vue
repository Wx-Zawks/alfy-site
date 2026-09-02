<script setup lang="ts">
import type { ApiCaseCategory, ApiCaseListItem, PageResult } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { mapCase } from '~/composables/useContentMapper'

useSeoMeta({ title: '应用与案例', description: '奥飞新材建筑节能、工业节能、石油石化等领域的典型应用案例。' })

const route = useRoute()
const router = useRouter()
const { resolveMediaUrl } = useApiClient()
const initialScene = typeof route.query.scene === 'string' ? route.query.scene : ''
const caseEndpoint = `/public/cases?page=1&size=100${initialScene ? `&scene=${encodeURIComponent(initialScene)}` : ''}`
const [{ data: categoryData }, { data: caseData }] = await Promise.all([
  useApi<ApiCaseCategory[]>('public-case-categories', '/public/case-categories'),
  useApi<PageResult<ApiCaseListItem>>(`public-cases-${initialScene || 'all'}`, caseEndpoint)
])

const categories = computed(() => [
  { key: 'all', name: '全部案例' },
  ...(categoryData.value ?? []).map(category => ({ key: category.slug, name: category.name }))
])
const caseEntries = computed(() => (caseData.value?.records ?? []).map(record => ({
  record,
  view: mapCase(record, resolveMediaUrl)
})))
const initialCategory = typeof route.query.category === 'string' ? route.query.category : 'all'
const activeCategory = ref(initialCategory)
const pageSize = 9
const filteredCases = computed(() => caseEntries.value
  .filter(({ record }) => activeCategory.value === 'all' || record.categorySlug === activeCategory.value)
  .map(({ view }) => view))
const {
  changePage: setPage,
  currentPage,
  paginatedItems: paginatedCases,
  paginationItems,
  resetPage,
  totalPages
} = useClientPagination(filteredCases, pageSize)

function changeCategory(key: string) {
  activeCategory.value = key
  resetPage()
  router.replace({ query: key === 'all' ? {} : { category: key } })
}

function changePage(page: number) {
  if (!setPage(page)) return
  nextTick(() => document.querySelector('.case-filter-row')?.scrollIntoView({
    behavior: 'smooth',
    block: 'start'
  }))
}

watch([() => route.query.category, categories], ([category]) => {
  const next = typeof category === 'string' && categories.value.some(item => item.key === category) ? category : 'all'
  if (activeCategory.value !== next) resetPage()
  activeCategory.value = next
}, { immediate: true })
</script>

<template>
  <div class="brief-page applications-page">
    <PageHero
      class="brief-hero applications-hero"
      page-key="applications"
      eyebrow="典型案例"
      title="从行业问题出发"
      highlight="让材料创造价值"
      description="围绕能效、空间、施工与安全等问题，匹配材料、产品和项目应用路径。"
      image="/images/news-3.jpeg"
    />

    <nav class="technology-subnav product-category-nav" aria-label="典型案例分类筛选">
      <div class="container">
        <button
          v-for="category in categories"
          :key="category.key"
          type="button"
          :class="{ active: activeCategory === category.key }"
          :aria-pressed="activeCategory === category.key"
          @click="changeCategory(category.key)"
        >
          {{ category.name }}
        </button>
      </div>
    </nav>

    <section class="brief-section product-catalog-section case-catalog-section">
      <div class="container">
        <header class="brief-section-heading">
          <p class="eyebrow">典型案例</p>
          <h1>用真实项目建立信任</h1>
        </header>

        <div class="product-filter-row case-filter-row">
          <span class="product-result-count" aria-live="polite">
            共 {{ filteredCases.length }} 个案例
            <template v-if="totalPages > 1"> · 第 {{ currentPage }} / {{ totalPages }} 页</template>
          </span>
        </div>

        <TransitionGroup v-if="filteredCases.length" name="catalog-list" tag="div" class="listing-grid case-catalog-grid">
          <ContentCard
            v-for="item in paginatedCases"
            :key="item.id"
            class="case-catalog-card"
            :to="`/cases/${item.slug}`"
            :image="item.image"
            :tag="item.category"
            :title="item.title"
            :summary="item.summary"
          />
        </TransitionGroup>
        <div v-else class="empty-state">当前分类暂无已发布案例。</div>

        <nav v-if="filteredCases.length > pageSize" class="product-pagination" aria-label="案例分页">
          <button
            type="button"
            class="pagination-nav"
            :disabled="currentPage === 1"
            aria-label="上一页"
            @click="changePage(currentPage - 1)"
          >
            ← 上一页
          </button>
          <template v-for="item in paginationItems" :key="item">
            <span v-if="typeof item === 'string'" class="pagination-ellipsis" aria-hidden="true">…</span>
            <button
              v-else
              type="button"
              :class="{ active: currentPage === item }"
              :aria-current="currentPage === item ? 'page' : undefined"
              :aria-label="`第 ${item} 页`"
              @click="changePage(item)"
            >
              {{ item }}
            </button>
          </template>
          <button
            type="button"
            class="pagination-nav"
            :disabled="currentPage === totalPages"
            aria-label="下一页"
            @click="changePage(currentPage + 1)"
          >
            下一页 →
          </button>
        </nav>
      </div>
    </section>
  </div>
</template>
