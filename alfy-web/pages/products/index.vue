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
const filteredProducts = computed(() => activeCategory.value === 'all'
  ? products.value
  : products.value.filter(item => item.category === activeCategory.value))

function changeCategory(key: string) {
  activeCategory.value = key
  router.replace({ query: key === 'all' ? {} : { category: key } })
}

watch([() => route.query.category, categories], ([category]) => {
  const next = typeof category === 'string' && categories.value.some(item => item.key === category) ? category : 'all'
  activeCategory.value = next
}, { immediate: true })
</script>

<template>
  <div class="brief-page products-page">
    <PageHero class="brief-hero" page-key="products" eyebrow="核心产品" title="中南大学气凝胶成果转化核心平台" image="/images/aerogel-powder.jpg" />

    <section class="brief-section product-catalog-section">
      <div class="container">
        <header class="brief-section-heading">
          <p class="eyebrow">核心产品</p>
          <h2>满足不同场景的隔热需求</h2>
        </header>

        <div class="product-filter-row" aria-label="产品分类筛选">
          <div class="filters brief-filters">
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
          <span class="product-result-count">共 {{ filteredProducts.length }} 款产品</span>
        </div>

        <TransitionGroup v-if="filteredProducts.length" name="catalog-list" tag="div" class="listing-grid client-product-grid">
          <ProductCard v-for="product in filteredProducts" :key="product.id" :product="product" />
        </TransitionGroup>
        <div v-else class="empty-state">当前分类暂无已发布产品。</div>

        <div class="catalog-note">
          <span>没有找到对应产品？</span>
          <NuxtLink to="/cooperation">告诉我们您的工况，获取定制建议 <b>→</b></NuxtLink>
        </div>
      </div>
    </section>
  </div>
</template>
