<script setup lang="ts">
import { products } from '~/data/products'

definePageMeta({ redirect: '/applications' })

useSeoMeta({ title: '产品中心', description: '奥飞新材气凝胶材料、气凝胶涂料、气凝胶毡板及配套复合产品。' })

const route = useRoute()
const router = useRouter()
const categories = [
  { key: 'all', name: '所有产品' },
  { key: 'coating', name: '气凝胶涂料' },
  { key: 'blanket', name: '气凝胶毡/板' },
  { key: 'support', name: '气凝胶配套' },
  { key: 'raw', name: '气凝胶粉体' }
]

const routeCategory = typeof route.query.category === 'string' ? route.query.category : 'all'
const activeCategory = ref(categories.some(item => item.key === routeCategory) ? routeCategory : 'all')
const filteredProducts = computed(() => activeCategory.value === 'all' ? products : products.filter(item => item.category === activeCategory.value))

const changeCategory = (key: string) => {
  activeCategory.value = key
  router.replace({ query: key === 'all' ? {} : { category: key } })
}

watch(() => route.query.category, (category) => {
  const next = typeof category === 'string' && categories.some(item => item.key === category) ? category : 'all'
  activeCategory.value = next
})
</script>

<template>
  <div class="brief-page products-page">
    <PageHero class="brief-hero" eyebrow="核心产品" title="中南大学气凝胶成果转化核心平台" image="/images/aerogel-powder.jpg" />

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

        <TransitionGroup name="catalog-list" tag="div" class="listing-grid client-product-grid">
          <ProductCard v-for="product in filteredProducts" :key="product.id" :product="product" />
        </TransitionGroup>

        <div class="catalog-note">
          <span>没有找到对应产品？</span>
          <NuxtLink to="/cooperation">告诉我们您的工况，获取定制建议 <b>→</b></NuxtLink>
        </div>
      </div>
    </section>
  </div>
</template>
