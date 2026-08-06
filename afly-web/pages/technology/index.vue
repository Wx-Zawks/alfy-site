<script setup lang="ts">
import type { ApiProductListItem, ApiTechnologyPage, PageResult } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { useContentMapper } from '~/composables/useContentMapper'

const { resolveMediaUrl } = useApiClient()
const { mapProduct } = useContentMapper()
const [
  { data: technology },
  { data: productData }
] = await Promise.all([
  useApi<ApiTechnologyPage>('public-technology', '/public/pages/technology', { optional: true }),
  useApi<PageResult<ApiProductListItem>>('public-products', '/public/products?page=1&size=100')
])

function documentTitle(value: string | null | undefined, fallback: string) {
  return (value || fallback).replace(/\s*(?:｜|\|)\s*奥飞新材$/, '')
}

useSeoMeta({
  title: () => documentTitle(technology.value?.seoTitle || technology.value?.title, '核心技术'),
  description: () => technology.value?.seoDescription || '奥飞新材气凝胶双纳米结构、常压干燥与复合材料制备技术。',
  keywords: () => technology.value?.seoKeywords || '奥飞新材,气凝胶,核心技术'
})

const materialTypes = [
  {
    description: '最常用的隔热材料，轻质、不燃，适用于建筑与工业保温。',
    formula: 'SiO₂',
    image: '/images/technology-silica-aerogel.webp',
    name: '气凝胶材料'
  },
  {
    description: '耐超高温（可承受1400℃以上），用于航天热防护和高温炉窑。',
    formula: 'Al₂O₃',
    image: '/images/technology-alumina-aerogel.webp',
    name: '气凝胶材料'
  },
  {
    description: '导电、耐酸碱，用于超级电容器和吸附过滤。',
    formula: 'C(碳)',
    image: '/images/technology-carbon-aerogel.webp',
    name: '气凝胶材料'
  }
]

const coverageSamples = [
  { image: '/images/technology-coverage-none.webp', label: '无气凝胶颗粒覆盖' },
  { image: '/images/technology-coverage-supercritical.webp', label: '超临界干燥工艺气凝胶颗粒覆盖' },
  { image: '/images/technology-coverage-atmospheric.webp', label: '新一代常压干燥气凝胶颗粒覆盖' }
]

const processSteps = [
  {
    description: '气凝胶块体经破碎制成微米级粉体，双纳米结构完整保留。',
    image: '/images/technology-process-powder.webp',
    title: '第一步：粉碎'
  },
  {
    description: '粉体均匀分散于体系中，纳米多孔结构未坍塌，保持隔热活性。',
    image: '/images/technology-process-dispersion.webp',
    title: '第二步：分散'
  },
  {
    description: '分散体微观结构，纳米多孔结构保留率＞95%。',
    image: '/images/technology-process-structure.webp',
    title: '第三步：结构保护'
  }
]

const productTrack = ref<HTMLElement | null>(null)
const canScrollBack = ref(false)
const canScrollForward = ref(false)
const compositeProducts = computed(() => (productData.value?.records ?? [])
  .map((item) => {
    const product = mapProduct(item, resolveMediaUrl)
    return {
      ...product,
      image: item.coverImageUrl ? product.image : '/images/technology-product-coating.webp'
    }
  })
  .slice(0, 7))

function updateProductScrollState() {
  const track = productTrack.value
  if (!track) return

  const maxScrollLeft = Math.max(0, track.scrollWidth - track.clientWidth)
  canScrollBack.value = track.scrollLeft > 4
  canScrollForward.value = track.scrollLeft < maxScrollLeft - 4
}

function scrollProducts(direction: number) {
  const track = productTrack.value
  if (!track) return

  const firstCard = track.querySelector<HTMLElement>('.product-card')
  const distance = firstCard ? firstCard.offsetWidth + 16 : track.clientWidth * 0.8
  track.scrollBy({ left: direction * distance, behavior: 'smooth' })
}

function resetProductTrack() {
  const track = productTrack.value
  if (!track) return

  track.scrollLeft = 0
  updateProductScrollState()
}

onMounted(() => {
  nextTick(resetProductTrack)
  window.addEventListener('resize', updateProductScrollState)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateProductScrollState)
})

watch(compositeProducts, () => nextTick(resetProductTrack))
</script>

<template>
  <div class="brief-page technology-page">
    <PageHero
      class="brief-hero technology-hero"
      page-key="technology"
      eyebrow="核心技术"
      title="中南大学气凝胶成果转化核心平台"
      image="/images/technology-hero.webp"
    />

    <nav class="technology-subnav" aria-label="技术栏目">
      <div class="container">
        <NuxtLink to="/technology">技术总览</NuxtLink>
        <NuxtLink to="/technology/aerogel-material">气凝胶材料技术</NuxtLink>
        <NuxtLink to="/technology/aerogel-composite">气凝胶复合产品技术</NuxtLink>
        <NuxtLink to="/technology/other">其他技术</NuxtLink>
      </div>
    </nav>

    <section class="brief-section technology-material-section">
      <div class="container">
        <header class="brief-section-heading wide-heading">
          <p class="eyebrow">气凝胶的特点</p>
          <h2>纳米骨架和纳米孔洞构成的双纳米结构功能材料</h2>
          <p>气凝胶拥有高孔隙率（80-99.8%）、低密度（≤1.5kg/m³）、低导热系数（≤0.012W/(m·K)@25℃），分为无机气凝胶（如二氧化硅、氧化铝）和有机气凝胶（如间苯二酚-甲醛、聚酰亚胺），在隔热、隔音、吸附、催化等领域应用前景广阔。</p>
        </header>
        <div class="material-type-grid">
          <article v-for="material in materialTypes" :key="material.formula" class="material-type-card">
            <img :src="material.image" :alt="`${material.formula}${material.name}`">
            <div>
              <h3><strong>{{ material.formula }}</strong>{{ material.name }}</h3>
              <p>{{ material.description }}</p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section class="brief-section technology-proof-section">
      <div class="container">
        <header class="brief-section-heading wide-heading">
          <p class="eyebrow">气凝胶核心技术</p>
          <h2>新一代气凝胶常压干燥技术</h2>
          <p>新一代常压干燥技术已实现与超临界工艺同等级别的产品性能。通过溶胶-凝胶结构均匀调控与环境友好型凝胶表面疏水改性技术，常压干燥制备的气凝胶在导热系数、孔隙率、比表面积等关键指标上均达到超临界干燥水平。</p>
        </header>
        <div class="technology-coverage-grid">
          <figure v-for="sample in coverageSamples" :key="sample.label">
            <img :src="sample.image" :alt="sample.label">
            <figcaption>{{ sample.label }}</figcaption>
          </figure>
        </div>
        <div class="technology-section-action">
          <NuxtLink class="button button-outline" to="/technology/aerogel-material">了解更多 ↗</NuxtLink>
        </div>
      </div>
    </section>

    <section class="brief-section technology-composite-section">
      <div class="container">
        <header class="brief-section-heading wide-heading">
          <p class="eyebrow">气凝胶复合产品核心技术</p>
          <h2>气凝胶复合材料制备技术</h2>
          <p>核心在于将气凝胶与纤维、树脂等基体复合时，通过工艺控制保留其双纳米结构与孔隙特征。即使在破碎、分散等加工过程中，仍维持纳米骨架完整性，确保复合材料兼具应用强度与超低导热、轻质、防火等核心性能。</p>
        </header>
        <div class="technology-process-grid">
          <article v-for="step in processSteps" :key="step.title" class="technology-process-card">
            <div class="process-image">
              <img :src="step.image" :alt="step.title">
            </div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </article>
        </div>
        <div class="technology-section-action">
          <NuxtLink class="button button-outline" to="/technology/aerogel-composite">了解更多 ↗</NuxtLink>
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
            <button
              class="product-carousel-arrow product-carousel-arrow-prev"
              type="button"
              aria-label="向前查看"
              :disabled="!canScrollBack"
              @click="scrollProducts(-1)"
            />
            <button
              class="product-carousel-arrow product-carousel-arrow-next"
              type="button"
              aria-label="向后查看"
              :disabled="!canScrollForward"
              @click="scrollProducts(1)"
            />
          </div>
        </div>
        <div
          v-if="compositeProducts.length"
          ref="productTrack"
          class="brief-carousel-track product-carousel-track"
          aria-label="气凝胶复合材料产品"
          tabindex="0"
          @scroll.passive="updateProductScrollState"
        >
          <ProductCard v-for="product in compositeProducts" :key="product.id" :product="product" />
        </div>
        <div v-else class="empty-state">暂无已发布产品。</div>
      </div>
    </section>
  </div>
</template>
