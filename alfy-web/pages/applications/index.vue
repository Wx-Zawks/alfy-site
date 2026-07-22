<script setup lang="ts">
import { applicationScenes, cases } from '~/data/applications'

useSeoMeta({ title: '应用与案例', description: '奥飞新材建筑节能、工业节能、石油石化等应用场景与典型案例。' })
const route = useRoute()
const activeScene = ref(typeof route.query.category === 'string' ? route.query.category : applicationScenes[0]?.key || '')
const currentScene = computed(() => applicationScenes.find(item => item.key === activeScene.value) || applicationScenes[0])
</script>

<template>
  <div><PageHero eyebrow="Applications & Cases" title="从行业问题出发" highlight="让材料创造价值" description="围绕能效、空间、施工与安全等问题，匹配材料、产品和项目应用路径。" image="/images/news-3.jpeg" />
    <section class="section section-muted"><div class="container"><div class="section-heading"><div><p class="eyebrow">应用场景</p><h2>按场景理解解决方案</h2></div><div class="tabs"><button v-for="scene in applicationScenes" :key="scene.key" :class="{ active: activeScene === scene.key }" @click="activeScene = scene.key">{{ scene.name }}</button></div></div><div v-if="currentScene" class="split"><div class="media-frame"><img :src="currentScene.image" :alt="currentScene.name"></div><div><p class="eyebrow">{{ currentScene.name }}</p><h2>{{ currentScene.slogan }}</h2><p class="lead">{{ currentScene.summary }}</p><NuxtLink class="button button-primary" to="/technology">查看技术方案 ↗</NuxtLink></div></div></div></section>
    <section class="section"><div class="container"><div class="center-heading"><p class="eyebrow">方案路径</p><h2>从需求到验证的完整过程</h2></div><div class="process"><article><h3>需求识别</h3><p>明确温度、基材、空间和施工目标。</p></article><article><h3>材料选型</h3><p>筛选粉体、涂层、毡板及配套。</p></article><article><h3>样品测试</h3><p>通过样品或小试确认关键性能。</p></article><article><h3>项目交付</h3><p>形成施工、质量和跟踪闭环。</p></article></div></div></section>
    <section class="section section-muted"><div class="container"><div class="section-heading"><div><p class="eyebrow">典型案例</p><h2>用真实项目建立信任</h2></div></div><div class="content-grid"><ContentCard v-for="item in cases" :key="item.id" :to="`/cases/${item.slug}`" :image="item.image" :tag="item.category" :title="item.title" :summary="item.summary" /></div></div></section>
  </div>
</template>
