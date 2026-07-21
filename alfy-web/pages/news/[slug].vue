<script setup lang="ts">
import { articles } from '~/data/articles'

const route = useRoute()
const article = computed(() => articles.find(item => item.slug === route.params.slug))
if (!article.value) throw createError({ statusCode: 404, statusMessage: '新闻不存在' })
useSeoMeta({ title: () => article.value?.title || '新闻详情', description: () => article.value?.summary || '' })
</script>

<template>
  <div v-if="article"><PageHero :eyebrow="`${article.categoryName} · ${article.date}`" title="产学研协同推动" highlight="气凝胶产业化应用" :description="article.summary" :image="article.image" /><section class="section"><article class="container article"><p class="lead">围绕气凝胶技术产业化与多场景应用，奥飞新材持续连接高校科研平台、材料企业和工程应用伙伴。</p><h2>从科研成果到产业应用</h2><p>气凝胶从实验室走向规模化应用，需要同时解决制备工艺、分散改性、复合产品设计和工程施工等问题。</p><img src="/images/launch-1.jpg" alt="气凝胶技术发布与交流活动"><h2>以真实项目推动持续迭代</h2><p>项目现场提供实验室无法完全模拟的条件。基材、环境、施工组织和客户目标都会影响最终结果。</p><h2>下一步</h2><p>奥飞新材将继续围绕建筑节能、工业节能等方向，与合作伙伴开展产品开发、场景验证和项目应用。</p></article></section></div>
</template>
