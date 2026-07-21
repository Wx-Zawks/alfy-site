<script setup lang="ts">
import { cases } from '~/data/applications'

const route = useRoute()
const item = computed(() => cases.find(caseItem => caseItem.slug === route.params.slug))
if (!item.value) throw createError({ statusCode: 404, statusMessage: '案例不存在' })
useSeoMeta({ title: () => item.value?.title || '案例详情', description: () => item.value?.summary || '' })
</script>

<template>
  <div v-if="item"><PageHero :eyebrow="item.category" title="真实项目推动" highlight="材料持续验证" :description="item.summary" :image="item.image" /><section class="section"><div class="container split"><div><p class="eyebrow">项目概况</p><h2>{{ item.title }}</h2><p class="lead">项目围绕真实场景中的隔热、构造与施工需求，通过材料选型、系统适配和应用反馈形成验证闭环。</p></div><div class="info-grid" style="grid-template-columns:1fr 1fr"><article class="info-card"><span>领域</span><h3>{{ item.category }}</h3></article><article class="info-card"><span>阶段</span><h3>示范应用</h3></article><article class="info-card"><span>方式</span><h3>项目验证</h3></article><article class="info-card"><span>数据</span><h3>待甲方确认</h3></article></div></div></section><section class="section section-muted"><div class="container"><div class="section-heading"><div><p class="eyebrow">需求与方案</p><h2>从项目问题反推材料组合</h2></div></div><div class="info-grid"><article class="info-card"><span>01</span><h3>客户需求</h3><p>明确能效、构造、施工和维护要求。</p></article><article class="info-card"><span>02</span><h3>奥飞方案</h3><p>匹配功能材料、配套产品和施工路径。</p></article><article class="info-card"><span>03</span><h3>应用反馈</h3><p>用现场数据支持产品持续迭代。</p></article></div></div></section></div>
</template>
