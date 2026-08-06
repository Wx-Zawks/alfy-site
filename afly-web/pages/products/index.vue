<script setup lang="ts">
import type { ApiProductCategory, ApiProductListItem, PageResult } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { useContentMapper } from '~/composables/useContentMapper'

useSeoMeta({ title: '产品中心', description: '奥飞新材气凝胶材料、气凝胶涂料、气凝胶毡板及配套复合产品。' })

const route = useRoute()
const router = useRouter()
const { resolveMediaUrl } = useApiClient()
const { mapProduct } = useContentMapper()
const [{ data: categoryData }, { data: productData }] = await Promise.all([
  useApi<ApiProductCategory[]>('public-product-categories', '/public/product-categories'),
  useApi<PageResult<ApiProductListItem>>('public-products', '/public/products?page=1&size=100')
])

const categories = computed(() => [
  { key: 'all', name: '所有产品' },
  ...(categoryData.value ?? []).map(item => ({ key: item.slug, name: item.name }))
])
const products = computed(() => (productData.value?.records ?? []).map(item => mapProduct(item, resolveMediaUrl)))
const initialCategory = typeof route.query.category === 'string' ? route.query.category : 'all'
const activeCategory = ref(initialCategory)
const pageSize = 8
const currentPage = ref(1)
const filteredProducts = computed(() => activeCategory.value === 'all'
  ? products.value
  : products.value.filter(item => item.category === activeCategory.value))
const totalPages = computed(() => Math.max(1, Math.ceil(filteredProducts.value.length / pageSize)))
const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredProducts.value.slice(start, start + pageSize)
})
const paginationItems = computed<(number | string)[]>(() => {
  if (totalPages.value <= 7) return Array.from({ length: totalPages.value }, (_, index) => index + 1)

  const pages = [1, currentPage.value - 1, currentPage.value, currentPage.value + 1, totalPages.value]
    .filter(page => page >= 1 && page <= totalPages.value)
  const uniquePages = [...new Set(pages)].sort((left, right) => left - right)

  return uniquePages.flatMap((page, index) => {
    const previous = uniquePages[index - 1]
    return previous && page - previous > 1 ? [`ellipsis-${previous}`, page] : [page]
  })
})

function changeCategory(key: string) {
  activeCategory.value = key
  currentPage.value = 1
  router.replace({ query: key === 'all' ? {} : { category: key } })
}

function changePage(page: number) {
  const nextPage = Math.min(Math.max(page, 1), totalPages.value)
  if (nextPage === currentPage.value) return

  currentPage.value = nextPage
  nextTick(() => document.querySelector('.product-filter-row')?.scrollIntoView({
    behavior: 'smooth',
    block: 'start'
  }))
}

watch([() => route.query.category, categories], ([category]) => {
  const next = typeof category === 'string' && categories.value.some(item => item.key === category) ? category : 'all'
  if (activeCategory.value !== next) currentPage.value = 1
  activeCategory.value = next
}, { immediate: true })

watch(totalPages, total => {
  if (currentPage.value > total) currentPage.value = total
})
</script>

<template>
  <div class="brief-page products-page">
    <PageHero class="brief-hero" page-key="products" eyebrow="核心产品" title="中南大学气凝胶成果转化核心平台" image="/images/aerogel-powder.jpg" />

    <nav class="technology-subnav product-category-nav" aria-label="产品分类筛选">
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

    <section class="brief-section product-catalog-section">
      <div class="container">
        <header class="brief-section-heading">
          <p class="eyebrow">核心产品</p>
          <h2>满足不同场景的隔热需求</h2>
        </header>

        <div class="product-filter-row">
          <span class="product-result-count" aria-live="polite">
            共 {{ filteredProducts.length }} 款产品
            <template v-if="totalPages > 1"> · 第 {{ currentPage }} / {{ totalPages }} 页</template>
          </span>
        </div>

        <TransitionGroup v-if="filteredProducts.length" name="catalog-list" tag="div" class="listing-grid client-product-grid">
          <ProductCard v-for="product in paginatedProducts" :key="product.id" :product="product" />
        </TransitionGroup>
        <div v-else class="empty-state">当前分类暂无已发布产品。</div>

        <nav v-if="filteredProducts.length > pageSize" class="product-pagination" aria-label="产品分页">
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

        <div class="catalog-note">
          <span>没有找到对应产品？</span>
          <NuxtLink to="/cooperation">告诉我们您的工况，获取定制建议 <b>→</b></NuxtLink>
        </div>
      </div>
    </section>
  </div>
</template>
