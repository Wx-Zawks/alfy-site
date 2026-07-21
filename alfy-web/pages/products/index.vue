<script setup lang="ts">
import { products } from '~/data/products'

useSeoMeta({ title: '产品中心', description: '奥飞新材气凝胶粉体、分散体、毡板、涂料及配套产品。' })

const route = useRoute()
const router = useRouter()
const categories = [
  { key: 'all', name: '全部产品' },
  { key: 'raw', name: '粉体/分散体' },
  { key: 'blanket', name: '气凝胶毡/板' },
  { key: 'coating', name: '气凝胶涂料' },
  { key: 'support', name: '涂料配套' }
]
const activeCategory = ref(typeof route.query.category === 'string' ? route.query.category : 'all')
const filteredProducts = computed(() => activeCategory.value === 'all' ? products : products.filter(item => item.category === activeCategory.value))
const changeCategory = (key: string) => { activeCategory.value = key; router.replace({ query: key === 'all' ? {} : { category: key } }) }
</script>

<template>
  <div><PageHero eyebrow="Products" title="从基础材料到" highlight="复合产品体系" description="从气凝胶粉体、颗粒和分散体向涂料、毡板及应用配套延伸。" image="/images/aerogel-fabric.jpg" />
    <section class="section catalog-section"><div class="container"><div class="section-heading catalog-heading"><div><p class="eyebrow">产品目录</p><h2>按产品形态快速选择</h2><p class="lead">围绕材料形态、应用工况与施工需求，快速定位适配的产品与配套方案。</p></div><div class="filters"><button v-for="category in categories" :key="category.key" :class="{ active: activeCategory === category.key }" @click="changeCategory(category.key)">{{ category.name }}</button></div></div><div class="listing-grid catalog-grid"><ProductCard v-for="product in filteredProducts" :key="product.id" :product="product" /></div></div></section>
  </div>
</template>
