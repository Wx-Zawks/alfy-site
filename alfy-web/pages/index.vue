<script setup lang="ts">
import type { ApiCaseListItem, ApiHome, ApiHomeSection, PageResult } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { useContentMapper } from '~/composables/useContentMapper'

useSeoMeta({ title: '新一代气凝胶及其复合产品技术', description: '奥飞新材面向建筑节能、工业节能等场景提供气凝胶材料、复合产品与应用方案。' })

const { open } = useInquiryDialog()
const { resolveMediaUrl } = useApiClient()
const { mapArticle, mapCase, mapScene } = useContentMapper()
const [{ data: home }, { data: sectionData }, { data: caseData }] = await Promise.all([
  useApi<ApiHome>('public-home', '/public/home'),
  useApi<ApiHomeSection[]>('public-home-sections', '/public/home-sections'),
  useApi<PageResult<ApiCaseListItem>>('public-home-cases', '/public/cases?page=1&size=100')
])

const sectionMap = computed(() => new Map((sectionData.value ?? []).map(section => [section.sectionKey, section])))
const homeSection = (key: string) => sectionMap.value.get(key)
const sectionEnabled = (key: string) => homeSection(key)?.enabled ?? true
const applicationScenes = computed(() => (home.value?.applicationScenes ?? []).map(item => mapScene(item, resolveMediaUrl)))
const caseEntries = computed(() => (caseData.value?.records ?? []).map(record => ({
  record,
  view: mapCase(record, resolveMediaUrl)
})))
const articles = computed(() => (home.value?.featuredArticles ?? []).map(item => ({
  ...mapArticle(item, resolveMediaUrl),
  homeSlot: item.homeSlot
})))
const technology = computed(() => home.value?.technologyOverview ?? null)
const activeCaseSceneKey = ref('')
const filteredCaseEntries = computed(() => activeCaseSceneKey.value
  ? caseEntries.value.filter(({ record }) => record.sceneSlug === activeCaseSceneKey.value)
  : caseEntries.value)
const featuredCaseEntry = computed(() =>
  filteredCaseEntries.value.find(({ record }) => record.featured) || filteredCaseEntries.value[0]
)
const featuredCase = computed(() => featuredCaseEntry.value?.view)
const caseCards = computed(() => filteredCaseEntries.value
  .filter(({ view }) => view.id !== featuredCase.value?.id)
  .slice(0, 3)
  .map(({ view }) => view))
const featuredArticle = computed(() =>
  articles.value.find(item => item.homeSlot === 'NEWS_PRIMARY') || articles.value[0]
)
const remainingArticles = computed(() =>
  articles.value.filter(item => item.id !== featuredArticle.value?.id)
)
const secondaryArticle = computed(() => remainingArticles.value[0])
const newsList = computed(() => remainingArticles.value.slice(1, 3))

const fallbackHero = {
  id: -1,
  image: '/images/news-1.jpeg',
  eyebrow: '气凝胶材料 · 复合产品 · 应用方案',
  title: '奥飞新材料',
  highlight: '连接材料与产业',
  description: '后台发布首页轮播后，将在这里自动展示最新内容。',
  primaryAction: { label: '查看应用案例', target: '/applications' },
  secondaryAction: null
}
const heroSlides = computed(() => {
  const published = home.value?.banners ?? []
  if (!published.length) return [fallbackHero]
  return published.map(slide => ({
    id: slide.id,
    image: resolveMediaUrl(slide.desktopImageUrl, '/images/news-1.jpeg'),
    eyebrow: slide.eyebrow || '',
    title: slide.title,
    highlight: slide.highlightText || '',
    description: slide.summary || '',
    primaryAction: slide.primaryAction,
    secondaryAction: slide.secondaryAction
  }))
})

const sceneRail = ref<HTMLElement | null>(null)
const activeSceneKey = ref('')
watch(applicationScenes, (items) => {
  if (!items.some(item => item.key === activeSceneKey.value)) activeSceneKey.value = items[0]?.key || ''
}, { immediate: true })

watch(caseEntries, (entries) => {
  if (entries.some(({ record }) => record.sceneSlug === activeCaseSceneKey.value)) return
  activeCaseSceneKey.value = entries.find(({ record }) => record.featured)?.record.sceneSlug
    || entries[0]?.record.sceneSlug
    || ''
}, { immediate: true })

function scrollScenes(direction: -1 | 1) {
  const currentIndex = applicationScenes.value.findIndex(scene => scene.key === activeSceneKey.value)
  const nextIndex = Math.min(applicationScenes.value.length - 1, Math.max(0, currentIndex + direction))
  const nextScene = applicationScenes.value[nextIndex]
  if (nextScene) selectScene(nextScene.key)
}

function selectScene(key: string) {
  const rail = sceneRail.value
  activeSceneKey.value = key
  if (!rail) return
  const card = Array.from(rail.querySelectorAll<HTMLElement>('[data-scene-key]')).find(item => item.dataset.sceneKey === key)
  if (!card) return
  const targetLeft = card.getBoundingClientRect().left - rail.getBoundingClientRect().left + rail.scrollLeft
  rail.scrollTo({ left: targetLeft, behavior: 'smooth' })
}

const activeHeroSlide = ref(0)
const heroPaused = ref(false)
const heroTouchStartX = ref<number | null>(null)
let heroTimer: ReturnType<typeof setInterval> | undefined

const currentHeroSlide = computed(() => heroSlides.value[activeHeroSlide.value] || heroSlides.value[0] || fallbackHero)
const goToHeroSlide = (index: number) => { activeHeroSlide.value = index }
const nextHeroSlide = () => { activeHeroSlide.value = (activeHeroSlide.value + 1) % heroSlides.value.length }
const previousHeroSlide = () => { activeHeroSlide.value = (activeHeroSlide.value - 1 + heroSlides.value.length) % heroSlides.value.length }
const startHeroTouch = (event: TouchEvent) => { heroTouchStartX.value = event.changedTouches[0]?.clientX ?? null }
const endHeroTouch = (event: TouchEvent) => {
  const startX = heroTouchStartX.value
  const endX = event.changedTouches[0]?.clientX
  heroTouchStartX.value = null
  if (startX === null || endX === undefined || Math.abs(startX - endX) < 48) return
  if (endX < startX) nextHeroSlide()
  else previousHeroSlide()
}

watch(heroSlides, (slides) => {
  if (activeHeroSlide.value >= slides.length) activeHeroSlide.value = 0
})

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  heroTimer = setInterval(() => {
    if (!heroPaused.value && heroSlides.value.length > 1) nextHeroSlide()
  }, 6500)
})

onBeforeUnmount(() => {
  if (heroTimer) clearInterval(heroTimer)
})
</script>

<template>
  <div class="home-page">
    <section class="home-hero" :class="{ 'has-proof': sectionEnabled('proof') }" @mouseenter="heroPaused = true" @mouseleave="heroPaused = false" @focusin="heroPaused = true" @focusout="heroPaused = false" @touchstart.passive="startHeroTouch" @touchend.passive="endHeroTouch">
      <div class="hero-slides" aria-hidden="true">
        <div v-for="(slide, index) in heroSlides" :key="slide.id" class="hero-slide" :class="{ active: index === activeHeroSlide }" :style="{ backgroundImage: `url(${slide.image})` }" />
      </div>
      <div class="container home-hero-content">
        <p class="eyebrow eyebrow-light">{{ currentHeroSlide.eyebrow }}</p>
        <h1>{{ currentHeroSlide.title }}<br><em>{{ currentHeroSlide.highlight }}</em></h1>
        <p>{{ currentHeroSlide.description }}</p>
        <div class="button-row"><NuxtLink class="button button-primary" :to="currentHeroSlide.primaryAction?.target || '/applications'">{{ currentHeroSlide.primaryAction?.label || '查看应用案例' }} <span>→</span></NuxtLink><NuxtLink v-if="currentHeroSlide.secondaryAction?.label && currentHeroSlide.secondaryAction?.target" class="button button-ghost" :to="currentHeroSlide.secondaryAction.target">{{ currentHeroSlide.secondaryAction.label }} <span>→</span></NuxtLink><button v-else class="button button-ghost" type="button" @click="open">获取项目方案 <span>→</span></button></div>
      </div>
      <div v-if="heroSlides.length > 1" class="container hero-carousel-controls" aria-label="首页轮播图">
        <button class="hero-arrow" type="button" aria-label="上一张轮播图" @click="previousHeroSlide">←</button>
        <div class="hero-dots">
          <button v-for="(slide, index) in heroSlides" :key="slide.id" type="button" :class="{ active: index === activeHeroSlide }" :aria-label="`切换至第 ${index + 1} 张轮播图`" :aria-current="index === activeHeroSlide ? 'true' : undefined" @click="goToHeroSlide(index)"><span>{{ String(index + 1).padStart(2, '0') }}</span></button>
        </div>
        <button class="hero-arrow" type="button" aria-label="下一张轮播图" @click="nextHeroSlide">→</button>
      </div>
      <div v-if="sectionEnabled('proof')" class="proof-strip home-hero-proof">
        <div class="container proof-grid">
          <article><span>01</span><div><small>技术来源</small><b>中南大学粉末冶金<br>全国重点实验室</b></div></article>
          <article><span>02</span><div><small>核心技术</small><b>新一代气凝胶<br>常压干燥技术</b></div></article>
          <article><span>03</span><div><small>全链服务</small><b>从研发到交付的<br>完整闭环</b></div></article>
          <article><span>04</span><div><small>客户验证</small><b>头部企业认可的<br>可靠供应商</b></div></article>
        </div>
      </div>
    </section>

    <section v-if="sectionEnabled('about')" class="section about-section">
      <div class="container split about-split">
        <div>
          <p class="eyebrow">{{ homeSection('about')?.eyebrow || '关于我们' }}</p>
          <h2>{{ homeSection('about')?.title || '中南大学气凝胶' }}<br><em>{{ homeSection('about')?.highlightText || '成果转化核心平台' }}</em></h2>
          <p v-if="homeSection('about')?.description" class="lead">{{ homeSection('about')?.description }}</p>
          <div class="about-actions">
            <NuxtLink class="button button-primary" to="/about">企业介绍 ↗</NuxtLink>
            <NuxtLink class="button button-outline" to="/about#team">研发团队 ↗</NuxtLink>
            <NuxtLink class="button button-outline" to="/technology">气凝胶技术 ↗</NuxtLink>
            <NuxtLink class="button button-outline" to="/technology">复合材料技术 ↗</NuxtLink>
          </div>
        </div>
        <figure class="media-frame about-media"><img :src="resolveMediaUrl(homeSection('about')?.imageUrl, '/images/university.jpeg')" :alt="homeSection('about')?.title || '中南大学粉末冶金学院'"></figure>
      </div>
    </section>

    <section v-if="sectionEnabled('applications')" class="section section-muted applications-section">
      <div class="container">
        <div class="section-heading application-heading">
          <div><p class="eyebrow">{{ homeSection('applications')?.eyebrow || '应用场景' }}</p><h2>{{ homeSection('applications')?.title || '让材料进入真实场景' }}</h2></div>
          <div v-if="applicationScenes.length" class="application-category-bar" aria-label="应用场景分类">
            <div class="application-category-scroll">
              <button v-for="scene in applicationScenes" :key="scene.key" type="button" :class="{ active: activeSceneKey === scene.key }" :aria-pressed="activeSceneKey === scene.key" @click="selectScene(scene.key)">{{ scene.name }}</button>
            </div>
            <NuxtLink to="/applications">查看更多 ↗</NuxtLink>
          </div>
        </div>
        <div v-if="applicationScenes.length" ref="sceneRail" class="application-rail" aria-label="应用场景横向列表">
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
        <div v-else class="empty-state">暂无已发布应用场景。</div>
        <div v-if="applicationScenes.length > 1" class="application-controls" aria-label="切换应用场景">
          <button type="button" aria-label="向前浏览应用场景" @click="scrollScenes(-1)">←</button>
          <button type="button" aria-label="向后浏览应用场景" @click="scrollScenes(1)">→</button>
        </div>
      </div>
    </section>

    <section v-if="sectionEnabled('cases')" class="section section-muted case-section">
      <div class="container">
        <div class="section-heading case-heading">
          <div><p class="eyebrow">{{ homeSection('cases')?.eyebrow || '典型案例' }}</p><h2>{{ homeSection('cases')?.title || '用真实项目建立信任' }}</h2></div>
          <div v-if="applicationScenes.length" class="application-category-bar case-category-bar" aria-label="典型案例场景筛选">
            <div class="application-category-scroll">
              <button v-for="scene in applicationScenes" :key="scene.key" type="button" :class="{ active: activeCaseSceneKey === scene.key }" :aria-pressed="activeCaseSceneKey === scene.key" @click="activeCaseSceneKey = scene.key">{{ scene.name }}</button>
            </div>
            <NuxtLink :to="`/applications?category=${activeCaseSceneKey}`">查看更多 ↗</NuxtLink>
          </div>
        </div>
        <div v-if="featuredCase" class="case-prototype-feature">
          <NuxtLink class="case-main-image" :to="`/cases/${featuredCase.slug}`"><img :src="featuredCase.image" :alt="featuredCase.title"></NuxtLink>
          <article class="case-detail">
            <p class="eyebrow">{{ featuredCase.category }}<template v-if="featuredCase.location"> / {{ featuredCase.location }}</template></p>
            <h3>{{ featuredCase.title }}</h3>
            <p>{{ featuredCase.summary }}</p>
            <NuxtLink class="button button-outline" :to="`/cases/${featuredCase.slug}`">查看案例 →</NuxtLink>
          </article>
        </div>
        <div v-else class="empty-state">暂无已发布推荐案例。</div>
        <div v-if="caseCards.length" class="case-card-grid">
          <NuxtLink v-for="item in caseCards" :key="item.id" class="case-card" :to="`/cases/${item.slug}`">
            <img :src="item.image" :alt="item.title">
            <div><h3>{{ item.title }}</h3><span>查看案例 →</span></div>
          </NuxtLink>
        </div>
        <div v-if="sectionEnabled('partners')" class="partner-matrix">
          <h3>奥飞新材与立邦、三一、中化学、中国建筑等头部企业已建立深度合作</h3>
          <div class="partner-grid-scroll"><img src="/images/partners-grid.png" alt="奥飞新材合作企业：立邦、三一、中国化学、中国船舶、中国电信、中国建科、中国建筑、中国重汽、中国铁建、比亚迪、深铁置业、吉利汽车、美的、TCL、中国移动"></div>
        </div>
      </div>
    </section>

    <section v-if="sectionEnabled('technology')" class="section section-dark technology-section">
      <div class="container">
        <div class="technology-head">
          <div>
            <p class="eyebrow eyebrow-light">{{ technology?.eyebrow || homeSection('technology')?.eyebrow || '技术研发' }}</p>
            <h2>{{ technology?.title || homeSection('technology')?.title || '新一代常压' }}<br><em>{{ technology?.highlightText || homeSection('technology')?.highlightText || '干燥技术' }}</em></h2>
            <NuxtLink class="button button-outline" :to="technology?.action?.target || '/technology'">{{ technology?.action?.label || '与技术团队交流' }} →</NuxtLink>
          </div>
          <div v-if="!technology?.contentHtml" class="technology-table">
            <div><span>技术栏目</span><span>研发内容</span><span>查看方向</span></div>
            <div><b>气凝胶材料技术</b><span>常压干燥、粉体、分散体</span><em>材料技术 →</em></div>
            <div><b>气凝胶复合产品技术</b><span>涂料、板材、水泥、布料</span><em>复合技术 →</em></div>
            <div><b>其他技术</b><span>固废处理与产业化设备</span><em>技术成果 →</em></div>
            <div><b>工艺对比</b><span>传统超临界干燥</span><em>常压设备 ↓60%+</em></div>
            <p class="technology-table-note">PCT 国际专利 1 件 · 发明专利 11 件（已授权 6 件）· 参与多项国家、行业及团体标准制定</p>
          </div>
          <div v-else class="cms-rich-text technology-html" v-html="technology.contentHtml" />
        </div>
        <h3 class="technology-cooperation-title">奥飞新材本着开放共赢的心态，愿与伙伴构建气凝胶产业合作生态</h3>
        <div class="technology-pillars">
          <article><h3>经销商合作</h3><p>区域渠道、项目资源与品牌共建，拓展建筑节能和工业节能市场。</p><NuxtLink to="/cooperation">成为合作伙伴 →</NuxtLink></article>
          <article><h3>复合产品开发模式</h3><p>提供改性浆料、复合母粒等气凝胶功能模块，协助快速开发新产品。</p><NuxtLink to="/cooperation">联合开发 →</NuxtLink></article>
          <article><h3>产业链合作模式</h3><p>为领军企业提供定制材料、专属配方与全流程研发支持，共同定义行业标准。</p><NuxtLink to="/cooperation">项目合作 →</NuxtLink></article>
        </div>
      </div>
    </section>

    <section v-if="sectionEnabled('news')" class="section news-section">
      <div class="container">
        <div class="section-heading"><div><p class="eyebrow">{{ homeSection('news')?.eyebrow || '新闻资讯' }}</p><h2>{{ homeSection('news')?.title || '关注气凝胶产业最新进展' }}</h2></div><NuxtLink class="text-link" to="/news">进入新闻中心 →</NuxtLink></div>
        <div v-if="featuredArticle" class="news-showcase">
          <NuxtLink v-if="featuredArticle" class="news-feature" :to="`/news/${featuredArticle.slug}`">
            <img :src="featuredArticle.image" :alt="featuredArticle.title">
            <div><span>{{ featuredArticle.categoryName }} · {{ featuredArticle.date }}</span><h3>{{ featuredArticle.title }}</h3><b>了解更多 →</b></div>
          </NuxtLink>
          <NuxtLink v-if="secondaryArticle" class="news-highlight" :to="`/news/${secondaryArticle.slug}`"><span>{{ secondaryArticle.categoryName }} · {{ secondaryArticle.date }}</span><h3>{{ secondaryArticle.title }}</h3><p>{{ secondaryArticle.summary }}</p><b>了解更多 →</b></NuxtLink>
          <div v-if="newsList.length" class="news-list">
            <NuxtLink v-for="item in newsList" :key="item.id" :to="`/news/${item.slug}`"><img :src="item.image" :alt="item.title"><div><small>{{ item.categoryName }} · {{ item.date }}</small><h3>{{ item.title }}</h3><span>查看详情 →</span></div></NuxtLink>
          </div>
        </div>
        <div v-else class="empty-state">暂无已发布推荐新闻。</div>
      </div>
    </section>
  </div>
</template>
