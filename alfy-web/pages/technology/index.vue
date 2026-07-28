<script setup lang="ts">
import type { ApiProductListItem, ApiTechnologyPage, PageResult } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { useContentMapper } from '~/composables/useContentMapper'

interface TechnologyBlock {
  description: string
  title: string
}

const { resolveMediaUrl } = useApiClient()
const { mapProduct } = useContentMapper()
const [
  { data: technology },
  { data: technologyPages },
  { data: productData }
] = await Promise.all([
  useApi<ApiTechnologyPage>('public-technology', '/public/pages/technology', { optional: true }),
  useApi<ApiTechnologyPage[]>('public-technology-pages', '/public/technologies', { optional: true }),
  useApi<PageResult<ApiProductListItem>>('public-products', '/public/products?page=1&size=100')
])

function documentTitle(value: string | null | undefined, fallback: string) {
  return (value || fallback).replace(/\s*(?:｜|\|)\s*奥飞新材$/, '')
}

useSeoMeta({
  title: () => documentTitle(technology.value?.seoTitle || technology.value?.title, '核心技术'),
  description: () => technology.value?.seoDescription || technology.value?.summary || '奥飞新材气凝胶双纳米结构、常压干燥与复合材料制备技术。',
  keywords: () => technology.value?.seoKeywords || '奥飞新材,气凝胶,核心技术'
})

function pageByKey(pageKey: string) {
  return computed(() => (technologyPages.value ?? []).find(item => item.pageKey === pageKey))
}

function normalizeBlocks(value: unknown): TechnologyBlock[] {
  if (!Array.isArray(value)) return []
  return value
    .filter(entry => entry && typeof entry === 'object')
    .map((entry) => {
      const record = entry as Record<string, unknown>
      return {
        title: String(record.title || record.name || ''),
        description: String(record.description || record.text || record.value || '')
      }
    })
    .filter(item => item.title || item.description)
}

const materialTechnology = pageByKey('aerogel-material')
const compositeTechnology = pageByKey('aerogel-composite')
const otherTechnology = pageByKey('other')
const otherBlocks = computed(() => {
  const values = normalizeBlocks(otherTechnology.value?.capabilityRows)
  return values.length
    ? values
    : [
        { title: '固废资源化', description: '围绕工业固废高值利用开展材料设计、工艺验证与应用评价。' },
        { title: '功能材料开发', description: '结合多孔材料、界面改性和复合工艺开发特定场景功能材料。' },
        { title: '产业化装备', description: '面向连续化、低能耗和稳定制造开展工艺与装备协同开发。' }
      ]
})

const productTrack = ref<HTMLElement | null>(null)
const compositeProducts = computed(() => (productData.value?.records ?? []).map(item => mapProduct(item, resolveMediaUrl)).slice(0, 7))
const scrollProducts = (direction: number) => productTrack.value?.scrollBy({ left: direction * 360, behavior: 'smooth' })

const processSteps = [
  { number: '01', title: '粉碎', text: '气凝胶块体经破碎制成微米级粉体，双纳米结构完整保留。', image: '/images/aerogel-powder.jpg' },
  { number: '02', title: '分散', text: '粉体均匀分散于体系中，纳米多孔结构不坍塌并保持隔热活性。', image: '/images/dispersion.png' },
  { number: '03', title: '结构保护', text: '通过界面调控保护纳米骨架，使复合材料兼具强度与功能表现。', image: '/images/ambient-particles.png' }
]
</script>

<template>
  <div class="brief-page technology-page">
    <PageHero
      class="brief-hero"
      :eyebrow="technology?.eyebrow || '核心技术'"
      :title="technology?.title || '中南大学气凝胶成果转化核心平台'"
      :highlight="technology?.highlightText || ''"
      :description="technology?.summary || ''"
      :image="resolveMediaUrl(technology?.heroImageUrl, '/images/aerogel-powder.jpg')"
    />

    <section class="brief-section technology-proof-section">
      <div class="container">
        <header class="brief-section-heading wide-heading technology-linked-heading">
          <div>
            <p class="eyebrow">{{ materialTechnology?.eyebrow || '气凝胶核心技术' }}</p>
            <h2>{{ materialTechnology?.title || '新一代气凝胶常压干燥技术' }}</h2>
            <p>{{ materialTechnology?.summary || '通过溶胶–凝胶结构均匀调控与环境友好型凝胶表面疏水改性，常压干燥制备的气凝胶在导热系数、孔隙率、比表面积等关键指标上达到超临界干燥水平。' }}</p>
          </div>
          <NuxtLink class="button button-outline" to="/technology/aerogel-material">了解更多 ↗</NuxtLink>
        </header>
        <figure class="technology-proof">
          <img src="/images/ambient-particles.png" alt="气凝胶颗粒覆盖对比">
          <figcaption>
            <span>无气凝胶颗粒覆盖</span>
            <span>超临界干燥工艺气凝胶颗粒覆盖</span>
            <span>新一代常压干燥气凝胶颗粒覆盖</span>
          </figcaption>
        </figure>
        <div class="technology-metrics">
          <article><strong>≤ 0.012</strong><span>W/(m·K) 导热系数</span></article>
          <article><strong>&gt; 850</strong><span>m²/g 比表面积</span></article>
          <article><strong>≤ 60</strong><span>kg/m³ 堆积密度</span></article>
          <article><strong>50%+</strong><span>综合能耗降低</span></article>
        </div>
      </div>
    </section>

    <section class="brief-section">
      <div class="container">
        <header class="brief-section-heading wide-heading technology-linked-heading">
          <div>
            <p class="eyebrow">{{ compositeTechnology?.eyebrow || '气凝胶复合产品核心技术' }}</p>
            <h2>{{ compositeTechnology?.title || '气凝胶复合材料制备技术' }}</h2>
            <p>{{ compositeTechnology?.summary || '核心在于将气凝胶与纤维、树脂等基体复合时，通过工艺控制保留其双纳米结构与孔隙特征，使材料兼具应用强度与超低导热、轻质、防火等核心性能。' }}</p>
          </div>
          <NuxtLink class="button button-outline" to="/technology/aerogel-composite">了解更多 ↗</NuxtLink>
        </header>
        <div class="technology-process-grid">
          <article v-for="step in processSteps" :key="step.number" class="technology-process-card">
            <div class="process-image">
              <img :src="step.image" :alt="step.title">
              <span>{{ step.number }}</span>
            </div>
            <h3>第{{ step.number === '01' ? '一' : step.number === '02' ? '二' : '三' }}步：{{ step.title }}</h3>
            <p>{{ step.text }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="brief-section other-technology-section">
      <div class="container">
        <header class="brief-section-heading wide-heading technology-linked-heading">
          <div>
            <p class="eyebrow">{{ otherTechnology?.eyebrow || '其他技术' }}</p>
            <h2>{{ otherTechnology?.title || '面向产业应用的延伸技术能力' }}</h2>
            <p>{{ otherTechnology?.summary || '围绕固废资源化、功能材料与产业化装备，持续推进可验证、可放大、可交付的技术成果。' }}</p>
          </div>
          <NuxtLink class="button button-outline" to="/technology/other">了解更多 ↗</NuxtLink>
        </header>
        <div class="other-technology-grid">
          <article v-for="(block, index) in otherBlocks" :key="`${block.title}-${index}`">
            <span>0{{ index + 1 }}</span>
            <h3>{{ block.title }}</h3>
            <p>{{ block.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="brief-section composite-products-section">
      <div class="container">
        <div class="carousel-heading">
          <header class="brief-section-heading">
            <p class="eyebrow">复合材料</p>
            <h2>气凝胶复合材料产品</h2>
          </header>
          <div class="carousel-arrows" aria-label="复合材料产品翻页">
            <button type="button" aria-label="向前查看" @click="scrollProducts(-1)">←</button>
            <button type="button" aria-label="向后查看" @click="scrollProducts(1)">→</button>
          </div>
        </div>
        <div v-if="compositeProducts.length" ref="productTrack" class="brief-carousel-track product-carousel-track">
          <ProductCard v-for="product in compositeProducts" :key="product.id" :product="product" />
        </div>
        <div v-else class="empty-state">暂无已发布产品。</div>
      </div>
    </section>
  </div>
</template>
