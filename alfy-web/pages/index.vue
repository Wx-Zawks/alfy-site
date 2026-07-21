<script setup lang="ts">
import { products } from '~/data/products'
import { applicationScenes, cases } from '~/data/applications'
import { articles } from '~/data/articles'

useSeoMeta({ title: '新一代气凝胶及其复合产品技术', description: '奥飞新材面向建筑节能、工业节能等场景提供气凝胶材料、复合产品与应用方案。' })

const { open } = useInquiryDialog()
const activeScene = ref(applicationScenes[0]?.key || '')
const currentScene = computed(() => applicationScenes.find(item => item.key === activeScene.value) || applicationScenes[0])
const nextScene = computed(() => {
  const currentIndex = applicationScenes.findIndex(item => item.key === activeScene.value)
  return applicationScenes[(currentIndex + 1) % applicationScenes.length]
})
const featuredProduct = products.find(item => item.slug === 'exterior-coating') || products[0]
const productTiles = products.filter(item => item.id !== featuredProduct?.id).slice(0, 4)
const featuredCase = cases[0]
const secondaryCases = cases.slice(1, 3)
const featuredArticle = articles[0]
const secondaryArticle = articles[1]
const newsList = articles.slice(2, 4)

const heroSlides = [
  {
    image: '/images/news-1.jpeg',
    eyebrow: '战略合作 · 产业协同',
    title: '以新材料之力',
    highlight: '服务“双碳”战略',
    description: '依托中南大学科研积淀，连接材料研发、复合开发与项目应用，让气凝胶从实验室走进真实产业场景。'
  },
  {
    image: '/images/news-3.jpeg',
    eyebrow: '技术发布 · 成果转化',
    title: '新一代气凝胶',
    highlight: '及其复合材料技术',
    description: '围绕粉体制备、分散改性与复合产品开发，持续推动气凝胶材料走向规模化应用。'
  },
  {
    image: '/images/news-5.jpeg',
    eyebrow: '产学研协同 · 产业发展',
    title: '让前沿材料',
    highlight: '连接产业与未来',
    description: '面向建筑节能、工业节能等真实工况，提供从材料到应用方案的一体化支持。'
  }
]

const activeHeroSlide = ref(0)
const heroPaused = ref(false)
const heroTouchStartX = ref<number | null>(null)
let heroTimer: ReturnType<typeof setInterval> | undefined

const currentHeroSlide = computed(() => heroSlides[activeHeroSlide.value] || heroSlides[0])
const goToHeroSlide = (index: number) => { activeHeroSlide.value = index }
const nextHeroSlide = () => { activeHeroSlide.value = (activeHeroSlide.value + 1) % heroSlides.length }
const previousHeroSlide = () => { activeHeroSlide.value = (activeHeroSlide.value - 1 + heroSlides.length) % heroSlides.length }
const startHeroTouch = (event: TouchEvent) => { heroTouchStartX.value = event.changedTouches[0]?.clientX ?? null }
const endHeroTouch = (event: TouchEvent) => {
  const startX = heroTouchStartX.value
  const endX = event.changedTouches[0]?.clientX
  heroTouchStartX.value = null
  if (startX === null || endX === undefined || Math.abs(startX - endX) < 48) return
  if (endX < startX) nextHeroSlide()
  else previousHeroSlide()
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  heroTimer = setInterval(() => {
    if (!heroPaused.value) nextHeroSlide()
  }, 6500)
})

onBeforeUnmount(() => {
  if (heroTimer) clearInterval(heroTimer)
})
</script>

<template>
  <div>
    <section class="home-hero" @mouseenter="heroPaused = true" @mouseleave="heroPaused = false" @focusin="heroPaused = true" @focusout="heroPaused = false" @touchstart.passive="startHeroTouch" @touchend.passive="endHeroTouch">
      <div class="hero-slides" aria-hidden="true">
        <div v-for="(slide, index) in heroSlides" :key="slide.image" class="hero-slide" :class="{ active: index === activeHeroSlide }" :style="{ backgroundImage: `url(${slide.image})` }" />
      </div>
      <div class="container home-hero-content">
        <p class="eyebrow eyebrow-light">{{ currentHeroSlide.eyebrow }}</p>
        <h1>{{ currentHeroSlide.title }}<br><em>{{ currentHeroSlide.highlight }}</em></h1>
        <p>{{ currentHeroSlide.description }}</p>
        <div class="button-row"><NuxtLink class="button button-primary" to="/applications">查看应用案例 <span>↗</span></NuxtLink><button class="button button-ghost" type="button" @click="open">获取项目方案 <span>↗</span></button></div>
      </div>
      <div class="container hero-footnote"><span>Founded 2022</span><span>材料研发 · 产品复合 · 场景应用</span></div>
      <div class="container hero-carousel-controls" aria-label="首页轮播图">
        <button class="hero-arrow" type="button" aria-label="上一张轮播图" @click="previousHeroSlide">←</button>
        <div class="hero-dots">
          <button v-for="(slide, index) in heroSlides" :key="slide.image" type="button" :class="{ active: index === activeHeroSlide }" :aria-label="`切换至第 ${index + 1} 张轮播图`" :aria-current="index === activeHeroSlide ? 'true' : undefined" @click="goToHeroSlide(index)"><span>{{ String(index + 1).padStart(2, '0') }}</span></button>
        </div>
        <button class="hero-arrow" type="button" aria-label="下一张轮播图" @click="nextHeroSlide">→</button>
      </div>
    </section>

    <section class="proof-strip"><div class="container proof-grid"><article><span>01</span><div><small>技术来源</small><b>中南大学粉末冶金<br>全国重点实验室</b></div></article><article><span>02</span><div><small>核心技术</small><b>新一代气凝胶<br>常压干燥技术</b></div></article><article><span>03</span><div><small>客户验证</small><b>以真实项目推动<br>材料性能验证</b></div></article><article><span>04</span><div><small>全链服务</small><b>从材料研发到<br>复合开发与交付</b></div></article></div></section>

    <section class="section about-section"><div class="container split about-split"><div><p class="eyebrow">关于奥飞</p><h2>中南大学气凝胶<br><em>成果转化核心平台</em></h2><p class="lead">奥飞新材聚焦气凝胶材料与复合产品技术，推动科研成果走向规模化产业应用。</p><div class="button-row"><NuxtLink class="button button-primary" to="/about">企业介绍 ↗</NuxtLink><NuxtLink class="button button-outline" to="/technology">研发与技术 ↗</NuxtLink></div><div class="stats"><article><b>材料</b><span>粉体 / 分散体</span></article><article><b>复合</b><span>涂料、毡板及配套</span></article><article><b>应用</b><span>多行业真实场景</span></article></div></div><figure class="media-frame about-media"><img src="/images/university.jpeg" alt="中南大学粉末冶金学院"><figcaption>科研源头与成果转化平台</figcaption></figure></div></section>

    <section class="section section-muted"><div class="container"><div class="section-heading section-heading-tabs"><div><p class="eyebrow">应用场景</p><h2>让材料进入真实场景</h2></div><div class="tabs"><button v-for="scene in applicationScenes" :key="scene.key" :class="{ active: activeScene === scene.key }" @click="activeScene = scene.key">{{ scene.name }}</button></div></div><div v-if="currentScene" class="scene-showcase"><NuxtLink class="scene-feature scene-primary" :to="`/applications?category=${currentScene.key}`"><img :src="currentScene.image" :alt="currentScene.name"><div class="scene-copy"><p class="eyebrow eyebrow-light">{{ currentScene.name }}</p><h2>{{ currentScene.slogan }}</h2><p>{{ currentScene.summary }}</p><span class="inline-arrow">查看解决方案 ↗</span></div></NuxtLink><NuxtLink v-if="nextScene" class="scene-feature scene-secondary" :to="`/applications?category=${nextScene.key}`"><img :src="nextScene.image" :alt="nextScene.name"><div class="scene-copy"><p class="eyebrow eyebrow-light">下一场景</p><h3>{{ nextScene.name }}</h3><p>{{ nextScene.summary }}</p><span class="inline-arrow">查看 ↗</span></div></NuxtLink></div></div></section>

    <section class="section"><div class="container"><div class="section-heading"><div><p class="eyebrow">产品中心</p><h2>全系列气凝胶复合产品</h2></div><NuxtLink class="text-link" to="/products">查看全部产品 ↗</NuxtLink></div><div class="product-showcase"><NuxtLink v-if="featuredProduct" class="product-feature" :to="`/products/${featuredProduct.slug}`" :style="{ '--product-image': `url(${featuredProduct.image})` }"><div><span>{{ featuredProduct.categoryName }}</span><h3>{{ featuredProduct.name }}</h3><p>{{ featuredProduct.summary }}</p><b>查看产品 ↗</b></div></NuxtLink><div class="product-tiles"><NuxtLink v-for="product in productTiles" :key="product.id" class="product-tile" :to="`/products/${product.slug}`"><img :src="product.image" :alt="product.name"><div><small>{{ product.categoryName }}</small><h3>{{ product.name }}</h3><span>产品详情 ↗</span></div></NuxtLink></div></div></div></section>

    <section class="section section-muted"><div class="container"><div class="section-heading"><div><p class="eyebrow">典型案例</p><h2>用数据说话的标杆项目</h2></div><NuxtLink class="text-link" to="/applications">查看全部案例 ↗</NuxtLink></div><div class="case-showcase"><NuxtLink v-if="featuredCase" class="case-feature" :to="`/cases/${featuredCase.slug}`"><img :src="featuredCase.image" :alt="featuredCase.title"><div><span>{{ featuredCase.category }}</span><h3>{{ featuredCase.title }}</h3><p>{{ featuredCase.summary }}</p><b>查看项目 ↗</b></div></NuxtLink><div class="case-side"><NuxtLink v-for="item in secondaryCases" :key="item.id" :to="`/cases/${item.slug}`"><img :src="item.image" :alt="item.title"><div><span>{{ item.category }}</span><h3>{{ item.title }}</h3><b>查看 ↗</b></div></NuxtLink></div></div><div class="partner-block"><p>奥飞新材与高校、行业伙伴和工程企业建立深度合作</p><div><span>中南大学</span><span>三一筑工</span><span>立邦中国</span><span>产学研联合创新</span></div></div></div></section>

    <section class="section section-dark technology-section"><div class="container"><div class="technology-head"><div><p class="eyebrow eyebrow-light">技术研发</p><h2>新一代常压<br><em>干燥技术</em></h2><p class="lead">围绕材料制备、分散改性和复合产品开发形成持续研发能力。</p><NuxtLink class="button button-light" to="/technology">进入技术中心 ↗</NuxtLink></div><div class="technology-table"><div><span>研发方向</span><span>技术阶段</span><span>开放方式</span></div><div><b>气凝胶粉体制备</b><span>产业化验证</span><em>技术咨询 ↗</em></div><div><b>水性分散体系</b><span>产品开发</span><em>联合开发 ↗</em></div><div><b>复合材料体系</b><span>场景验证</span><em>项目合作 ↗</em></div><div><b>应用解决方案</b><span>持续迭代</span><em>获取方案 ↗</em></div></div></div><div class="technology-pillars"><article><span>01 / 材料制备</span><h3>粉体与颗粒</h3><p>关注孔结构、工艺稳定性与规模化制备。</p><NuxtLink to="/technology">技术详情 ↗</NuxtLink></article><article><span>02 / 分散改性</span><h3>进入复合体系</h3><p>面向涂料、浆料和多元基材设计分散路径。</p><NuxtLink to="/technology">技术详情 ↗</NuxtLink></article><article><span>03 / 场景开发</span><h3>从工况反推产品</h3><p>通过样品、小试与项目数据持续验证。</p><NuxtLink to="/technology">技术详情 ↗</NuxtLink></article></div></div></section>

    <section class="section"><div class="container"><div class="section-heading"><div><p class="eyebrow">新闻资讯</p><h2>关注气凝胶产业最新进展</h2></div><NuxtLink class="text-link" to="/news">进入新闻中心 ↗</NuxtLink></div><div class="news-showcase"><NuxtLink v-if="featuredArticle" class="news-feature" :to="`/news/${featuredArticle.slug}`"><img :src="featuredArticle.image" :alt="featuredArticle.title"><div><span>{{ featuredArticle.categoryName }} · {{ featuredArticle.date }}</span><h3>{{ featuredArticle.title }}</h3><p>{{ featuredArticle.summary }}</p><b>阅读全文 ↗</b></div></NuxtLink><NuxtLink v-if="secondaryArticle" class="news-highlight" :to="`/news/${secondaryArticle.slug}`"><span>{{ secondaryArticle.categoryName }} · {{ secondaryArticle.date }}</span><h3>{{ secondaryArticle.title }}</h3><p>{{ secondaryArticle.summary }}</p><b>阅读全文 ↗</b></NuxtLink><div class="news-list"><NuxtLink v-for="item in newsList" :key="item.id" :to="`/news/${item.slug}`"><small>{{ item.date }}</small><h3>{{ item.title }}</h3><span>查看 ↗</span></NuxtLink></div></div></div></section>
  </div>
</template>
