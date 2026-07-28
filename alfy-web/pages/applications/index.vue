<script setup lang="ts">
import type { ApiApplicationScene, ApiCaseListItem, PageResult } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { useContentMapper } from '~/composables/useContentMapper'

useSeoMeta({ title: '应用与案例', description: '奥飞新材建筑节能、工业节能、石油石化等应用场景与典型案例。' })
const route = useRoute()
const router = useRouter()
const { resolveMediaUrl } = useApiClient()
const { mapCase, mapScene } = useContentMapper()
const [{ data: sceneData }, { data: caseData }] = await Promise.all([
  useApi<ApiApplicationScene[]>('public-application-scenes', '/public/application-scenes'),
  useApi<PageResult<ApiCaseListItem>>('public-cases', '/public/cases?page=1&size=100')
])
const applicationScenes = computed(() => (sceneData.value ?? []).map(item => mapScene(item, resolveMediaUrl)))
const caseEntries = computed(() => (caseData.value?.records ?? []).map(record => ({
  record,
  view: mapCase(record, resolveMediaUrl)
})))
const activeScene = ref(typeof route.query.category === 'string' ? route.query.category : '')
const currentScene = computed(() => applicationScenes.value.find(item => item.key === activeScene.value) || applicationScenes.value[0])
const activeCaseSceneKey = ref('')
const filteredCases = computed(() => caseEntries.value
  .filter(({ record }) => !activeCaseSceneKey.value || record.sceneSlug === activeCaseSceneKey.value)
  .map(({ view }) => view))

function changeScene(key: string) {
  activeScene.value = key
  router.replace({ query: { ...route.query, category: key } })
}

watch(applicationScenes, (items) => {
  if (!items.some(item => item.key === activeScene.value)) activeScene.value = items[0]?.key || ''
}, { immediate: true })

watch(caseEntries, (entries) => {
  if (entries.some(({ record }) => record.sceneSlug === activeCaseSceneKey.value)) return
  activeCaseSceneKey.value = entries.find(({ record }) => record.featured)?.record.sceneSlug
    || entries[0]?.record.sceneSlug
    || ''
}, { immediate: true })
</script>

<template>
  <div class="applications-page"><PageHero page-key="applications" eyebrow="Applications & Cases" title="从行业问题出发" highlight="让材料创造价值" description="围绕能效、空间、施工与安全等问题，匹配材料、产品和项目应用路径。" image="/images/news-3.jpeg" />
    <section class="section section-muted"><div class="container"><div class="section-heading applications-case-heading"><div><p class="eyebrow">应用场景</p><h2>按场景理解解决方案</h2></div><div v-if="applicationScenes.length" class="application-category-bar" aria-label="应用场景筛选"><div class="application-category-scroll"><button v-for="scene in applicationScenes" :key="scene.key" type="button" :class="{ active: activeScene === scene.key }" :aria-pressed="activeScene === scene.key" @click="changeScene(scene.key)">{{ scene.name }}</button></div></div></div><div v-if="currentScene" class="split"><div class="media-frame"><img :src="currentScene.image" :alt="currentScene.name"></div><div><p class="eyebrow">{{ currentScene.name }}</p><h2>{{ currentScene.slogan }}</h2><p class="lead">{{ currentScene.summary }}</p><NuxtLink class="button button-primary" to="/technology">查看技术方案 →</NuxtLink></div></div><div v-else class="empty-state">暂无已发布应用场景。</div></div></section>
    <section class="section"><div class="container"><div class="center-heading"><p class="eyebrow">方案路径</p><h2>从需求到验证的完整过程</h2></div><div class="process"><article><h3>需求识别</h3><p>明确温度、基材、空间和施工目标。</p></article><article><h3>材料选型</h3><p>筛选粉体、涂层、毡板及配套。</p></article><article><h3>样品测试</h3><p>通过样品或小试确认关键性能。</p></article><article><h3>项目交付</h3><p>形成施工、质量和跟踪闭环。</p></article></div></div></section>
    <section class="section section-muted"><div class="container"><div class="section-heading applications-case-heading"><div><p class="eyebrow">典型案例</p><h2>用真实项目建立信任</h2></div><div v-if="applicationScenes.length" class="application-category-bar" aria-label="典型案例场景筛选"><div class="application-category-scroll"><button v-for="scene in applicationScenes" :key="scene.key" type="button" :class="{ active: activeCaseSceneKey === scene.key }" :aria-pressed="activeCaseSceneKey === scene.key" @click="activeCaseSceneKey = scene.key">{{ scene.name }}</button></div></div></div><div v-if="filteredCases.length" class="content-grid"><ContentCard v-for="item in filteredCases" :key="item.id" :to="`/cases/${item.slug}`" :image="item.image" :tag="item.category" :title="item.title" :summary="item.summary" /></div><div v-else class="empty-state">暂无该场景下已发布案例。</div></div></section>
  </div>
</template>
