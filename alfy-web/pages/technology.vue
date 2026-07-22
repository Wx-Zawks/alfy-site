<script setup lang="ts">
import { products } from '~/data/products'

useSeoMeta({ title: '核心技术', description: '奥飞新材气凝胶双纳米结构、常压干燥与复合材料制备技术。' })

const productTrack = ref<HTMLElement | null>(null)
const compositeProducts = computed(() => products.filter(item => ['coating', 'blanket', 'support'].includes(item.category)).slice(0, 7))
const scrollProducts = (direction: number) => productTrack.value?.scrollBy({ left: direction * 360, behavior: 'smooth' })

const materialTypes = [
  { formula: 'SiO₂', name: '气凝胶材料', text: '最常用的隔热材料，轻质、不燃，适用于建筑与工业保温。', image: '/images/aerogel-block.jpg' },
  { formula: 'Al₂O₃', name: '气凝胶材料', text: '耐超高温，可面向航天热防护与高温炉窑等极端环境。', image: '/images/alumina-aerogel.jpg' },
  { formula: 'C（碳）', name: '气凝胶材料', text: '兼具轻质多孔与导电特性，可用于储能、吸附和功能复合。', image: '/images/aerogel-block-2.jpg' }
]

const processSteps = [
  { number: '01', title: '粉碎', text: '气凝胶块体经破碎制成微米级粉体，双纳米结构完整保留。', image: '/images/aerogel-powder.jpg' },
  { number: '02', title: '分散', text: '粉体均匀分散于体系中，纳米多孔结构不坍塌并保持隔热活性。', image: '/images/dispersion.png' },
  { number: '03', title: '结构保护', text: '通过界面调控保护纳米骨架，使复合材料兼具强度与功能表现。', image: '/images/ambient-particles.png' }
]
</script>

<template>
  <div class="brief-page technology-page">
    <PageHero class="brief-hero" eyebrow="核心技术" title="中南大学气凝胶成果转化核心平台" image="/images/aerogel-powder.jpg" />

    <section class="brief-section">
      <div class="container">
        <header class="brief-section-heading wide-heading">
          <p class="eyebrow">气凝胶的特点</p>
          <h2>纳米骨架和纳米孔洞构成的<br>双纳米结构功能材料</h2>
          <p>气凝胶拥有高孔隙率（80%–99.8%）、低密度（≤1.5kg/m³）、低导热系数（≤0.012W/(m·K)@25℃），在隔热、隔音、吸附、催化等领域具有广泛应用前景。</p>
        </header>
        <div class="material-type-grid">
          <article v-for="item in materialTypes" :key="item.formula" class="material-type-card">
            <img :src="item.image" :alt="`${item.formula}${item.name}`">
            <div><h3>{{ item.formula }}<span>{{ item.name }}</span></h3><p>{{ item.text }}</p></div>
          </article>
        </div>
      </div>
    </section>

    <section class="brief-section technology-proof-section">
      <div class="container">
        <header class="brief-section-heading wide-heading">
          <p class="eyebrow">气凝胶核心技术</p>
          <h2>新一代气凝胶常压干燥技术</h2>
          <p>通过溶胶–凝胶结构均匀调控与环境友好型凝胶表面疏水改性，常压干燥制备的气凝胶在导热系数、孔隙率、比表面积等关键指标上达到超临界干燥水平。</p>
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
        <header class="brief-section-heading wide-heading">
          <p class="eyebrow">气凝胶复合产品核心技术</p>
          <h2>气凝胶复合材料制备技术</h2>
          <p>核心在于将气凝胶与纤维、树脂等基体复合时，通过工艺控制保留其双纳米结构与孔隙特征，使材料兼具应用强度与超低导热、轻质、防火等核心性能。</p>
        </header>
        <div class="technology-process-grid">
          <article v-for="step in processSteps" :key="step.number" class="technology-process-card">
            <div class="process-image"><img :src="step.image" :alt="step.title"><span>{{ step.number }}</span></div>
            <h3>第{{ step.number === '01' ? '一' : step.number === '02' ? '二' : '三' }}步：{{ step.title }}</h3>
            <p>{{ step.text }}</p>
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
        <div ref="productTrack" class="brief-carousel-track product-carousel-track">
          <ProductCard v-for="product in compositeProducts" :key="product.id" :product="product" />
        </div>
      </div>
    </section>
  </div>
</template>
