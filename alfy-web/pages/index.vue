<script setup lang="ts">
import { applicationScenes, cases } from '~/data/applications'
import { articles } from '~/data/articles'

useSeoMeta({ title: '新一代气凝胶及其复合产品技术', description: '奥飞新材面向建筑节能、工业节能等场景提供气凝胶材料、复合产品与应用方案。' })

const { open } = useInquiryDialog()
const sceneRail = ref<HTMLElement | null>(null)
const activeSceneKey = ref(applicationScenes[0]?.key || '')
const featuredCase = cases[0]
const caseCards = cases.slice(0, 3)
const featuredArticle = articles[0]
const secondaryArticle = articles[1]
const newsList = articles.slice(2, 4)

const scrollScenes = (direction: -1 | 1) => {
  const currentIndex = applicationScenes.findIndex(scene => scene.key === activeSceneKey.value)
  const nextIndex = Math.min(applicationScenes.length - 1, Math.max(0, currentIndex + direction))
  const nextScene = applicationScenes[nextIndex]
  if (nextScene) selectScene(nextScene.key)
}

const selectScene = (key: string) => {
  const rail = sceneRail.value
  activeSceneKey.value = key
  if (!rail) return
  const card = Array.from(rail.querySelectorAll<HTMLElement>('[data-scene-key]')).find(item => item.dataset.sceneKey === key)
  if (!card) return
  const targetLeft = card.getBoundingClientRect().left - rail.getBoundingClientRect().left + rail.scrollLeft
  rail.scrollTo({ left: targetLeft, behavior: 'smooth' })
}

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
  <div class="home-page">
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

    <section class="proof-strip">
      <div class="container proof-grid">
        <article><span>01</span><div><small>技术来源</small><b>中南大学粉末冶金<br>全国重点实验室</b></div></article>
        <article><span>02</span><div><small>核心技术</small><b>新一代气凝胶<br>常压干燥技术</b></div></article>
        <article><span>03</span><div><small>全链服务</small><b>从研发到交付的<br>完整闭环</b></div></article>
        <article><span>04</span><div><small>客户验证</small><b>头部企业认可的<br>可靠供应商</b></div></article>
      </div>
    </section>

    <section class="section about-section">
      <div class="container split about-split">
        <div>
          <p class="eyebrow">关于我们</p>
          <h2>中南大学气凝胶<br><em>成果转化核心平台</em></h2>
          <p class="lead">源自中南大学粉末冶金全国重点实验室，十余年技术积淀，新一代常压干燥技术的产业化践行者。拥有国家级专家、博士等科研开发人才，团队在气凝胶常压干燥制备技术、气凝胶复合材料技术等方面具有深厚的研发实力，产品已经推向市场并获得成功。</p>
          <div class="about-actions">
            <NuxtLink class="button button-primary" to="/about">企业介绍 ↗</NuxtLink>
            <NuxtLink class="button button-outline" to="/about#team">研发团队 ↗</NuxtLink>
            <NuxtLink class="button button-outline" to="/technology">气凝胶技术 ↗</NuxtLink>
            <NuxtLink class="button button-outline" to="/technology">复合材料技术 ↗</NuxtLink>
          </div>
        </div>
        <figure class="media-frame about-media"><img src="/images/university.jpeg" alt="中南大学粉末冶金学院"></figure>
      </div>
    </section>

    <section class="section section-muted applications-section">
      <div class="container">
        <div class="section-heading application-heading">
          <div><p class="eyebrow">应用场景</p><h2>让材料进入真实场景</h2></div>
          <div class="application-category-bar" aria-label="应用场景分类">
            <div class="application-category-scroll">
              <button v-for="scene in applicationScenes" :key="scene.key" type="button" :class="{ active: activeSceneKey === scene.key }" :aria-pressed="activeSceneKey === scene.key" @click="selectScene(scene.key)">{{ scene.name }}</button>
            </div>
            <NuxtLink to="/applications">查看更多 ↗</NuxtLink>
          </div>
        </div>
        <div ref="sceneRail" class="application-rail" aria-label="应用场景横向列表">
          <NuxtLink v-for="scene in applicationScenes" :key="scene.key" class="application-card" :data-scene-key="scene.key" :to="`/applications?category=${scene.key}`">
            <img :src="scene.image" :alt="scene.name">
            <div class="application-card-copy">
              <span>{{ scene.name }}</span>
              <h3>{{ scene.slogan }}</h3>
              <p>{{ scene.summary }}</p>
              <b>查看场景 ↗</b>
            </div>
          </NuxtLink>
          <span class="application-rail-spacer" aria-hidden="true" />
        </div>
        <div class="application-controls" aria-label="切换应用场景">
          <button type="button" aria-label="向前浏览应用场景" @click="scrollScenes(-1)">←</button>
          <button type="button" aria-label="向后浏览应用场景" @click="scrollScenes(1)">→</button>
        </div>
      </div>
    </section>

    <section class="section section-muted case-section">
      <div class="container">
        <div class="section-heading"><div><p class="eyebrow">典型案例</p><h2>用数据说话的标杆项目</h2></div><NuxtLink class="text-link" to="/applications">查看更多 ↗</NuxtLink></div>
        <div v-if="featuredCase" class="case-prototype-feature">
          <NuxtLink class="case-main-image" :to="`/cases/${featuredCase.slug}`"><img :src="featuredCase.image" :alt="featuredCase.title"></NuxtLink>
          <article class="case-detail">
            <p class="eyebrow">合作伙伴 / 绿色建造</p>
            <h3>三一筑工</h3>
            <p>2026年1月，奥飞新材与三一筑工签署战略合作协议，共同推动气凝胶技术在绿色建筑、工程机械涂装等领域的工程应用。同年，双方合作打造三一云谷项目，将气凝胶应用于装配式建筑外墙外保温及夹层，实现保温隔热、防火轻质一体化，助力装配式建筑节能升级。</p>
            <div class="case-products"><b>核心产品</b><span>气凝胶外墙涂料</span><span>气凝胶水泥</span></div>
            <p class="case-summary">项目采用气凝胶外墙外保温系统及气凝胶水泥，应用于装配式建筑夹层，实现保温、隔热、防水一体化。</p>
            <NuxtLink class="button button-outline" :to="`/cases/${featuredCase.slug}`">查看案例 ↗</NuxtLink>
          </article>
        </div>
        <div class="case-card-grid">
          <NuxtLink v-for="item in caseCards" :key="item.id" class="case-card" :to="`/cases/${item.slug}`">
            <img :src="item.image" :alt="item.title">
            <div><h3>{{ item.title }}</h3><span>查看案例 ↗</span></div>
          </NuxtLink>
        </div>
        <div class="partner-matrix">
          <h3>奥飞新材与立邦、三一、中化学、中国建筑等头部企业已建立深度合作</h3>
          <div class="partner-grid-scroll"><img src="/images/partners-grid.png" alt="奥飞新材合作企业：立邦、三一、中国化学、中国船舶、中国电信、中国建科、中国建筑、中国重汽、中国铁建、比亚迪、深铁置业、吉利汽车、美的、TCL、中国移动"></div>
        </div>
      </div>
    </section>

    <section class="section section-dark technology-section">
      <div class="container">
        <div class="technology-head">
          <div>
            <p class="eyebrow eyebrow-light">技术研发</p>
            <h2>新一代常压<br><em>干燥技术</em></h2>
            <p class="lead">突破传统超临界工艺的高投入、高能耗、难连续生产瓶颈，让气凝胶实现绿色、低成本、规模化生产。</p>
            <NuxtLink class="button button-outline" to="/technology">与技术团队交流 ↗</NuxtLink>
          </div>
          <div class="technology-table">
            <div><span>技术栏目</span><span>研发内容</span><span>查看方向</span></div>
            <div><b>气凝胶材料技术</b><span>常压干燥、粉体、分散体</span><em>材料技术 →</em></div>
            <div><b>气凝胶复合产品技术</b><span>涂料、板材、水泥、布料</span><em>复合技术 →</em></div>
            <div><b>其他技术</b><span>固废处理与产业化设备</span><em>技术成果 →</em></div>
            <div><b>工艺对比</b><span>传统超临界干燥</span><em>常压设备 ↓60%+</em></div>
            <p class="technology-table-note">PCT 国际专利 1 件 · 发明专利 11 件（已授权 6 件）· 参与多项国家、行业及团体标准制定</p>
          </div>
        </div>
        <h3 class="technology-cooperation-title">奥飞新材本着开放共赢的心态，愿与伙伴构建气凝胶产业合作生态</h3>
        <div class="technology-pillars">
          <article><h3>经销商合作</h3><p>区域渠道、项目资源与品牌共建，拓展建筑节能和工业节能市场。</p><NuxtLink to="/cooperation">成为合作伙伴 →</NuxtLink></article>
          <article><h3>复合产品开发模式</h3><p>提供改性浆料、复合母粒等气凝胶功能模块，协助快速开发新产品。</p><NuxtLink to="/cooperation">联合开发 →</NuxtLink></article>
          <article><h3>产业链合作模式</h3><p>为领军企业提供定制材料、专属配方与全流程研发支持，共同定义行业标准。</p><NuxtLink to="/cooperation">项目合作 →</NuxtLink></article>
        </div>
      </div>
    </section>

    <section class="section news-section">
      <div class="container">
        <div class="section-heading"><div><p class="eyebrow">新闻资讯</p><h2>关注气凝胶产业最新进展</h2></div><NuxtLink class="text-link" to="/news">进入新闻中心 ↗</NuxtLink></div>
        <div class="news-showcase">
          <NuxtLink v-if="featuredArticle" class="news-feature" :to="`/news/${featuredArticle.slug}`">
            <img :src="featuredArticle.image" :alt="featuredArticle.title">
            <div><span>{{ featuredArticle.categoryName }} · {{ featuredArticle.date }}</span><h3>{{ featuredArticle.title }}</h3><b>了解更多 ↗</b></div>
          </NuxtLink>
          <NuxtLink v-if="secondaryArticle" class="news-highlight" :to="`/news/${secondaryArticle.slug}`"><span>{{ secondaryArticle.categoryName }} · {{ secondaryArticle.date }}</span><h3>{{ secondaryArticle.title }}</h3><p>{{ secondaryArticle.summary }}</p><b>了解更多 ↗</b></NuxtLink>
          <div class="news-list">
            <NuxtLink v-for="item in newsList" :key="item.id" :to="`/news/${item.slug}`"><img :src="item.image" :alt="item.title"><div><small>活动回顾 · {{ item.date }}</small><h3>{{ item.title }}</h3><span>查看案例 ↗</span></div></NuxtLink>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
