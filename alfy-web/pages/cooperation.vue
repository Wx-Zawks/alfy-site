<script setup lang="ts">
useSeoMeta({ title: '商务合作', description: '奥飞新材经销合作、复合产品开发与产业链合作。' })
const { open } = useInquiryDialog()
const modes = [
  { index: '01', title: '经销商合作', text: '面向具备区域客户、项目服务或建材渠道能力的伙伴。' },
  { index: '02', title: '复合产品开发', text: '基于粉体、分散体和改性能力共同定义产品目标。' },
  { index: '03', title: '产业链合作', text: '连接科研、材料、制造、设计与工程资源。' }
]
</script>

<template>
  <div><PageHero eyebrow="Open Collaboration" title="开放共赢，共建" highlight="气凝胶产业生态" description="以材料能力为基础，与渠道、产品和产业链伙伴建立清晰、可持续的合作关系。" image="/images/news-1.jpeg"><div class="button-row"><button class="button button-primary" type="button" @click="open">发起合作咨询 ↗</button></div></PageHero>
    <section class="section"><div class="container"><div class="center-heading"><p class="eyebrow">合作模式</p><h2>选择适合双方资源的合作方式</h2></div><div class="info-grid"><article v-for="mode in modes" :key="mode.index" class="info-card"><span>{{ mode.index }}</span><h3>{{ mode.title }}</h3><p>{{ mode.text }}</p><button class="button button-outline" type="button" @click="open">了解合作 ↗</button></article></div></div></section>
    <section class="section section-muted"><div class="container"><div class="center-heading"><p class="eyebrow">合作流程</p><h2>让合作预期清晰可见</h2></div><div class="process"><article><h3>需求沟通</h3><p>明确市场、场景与合作目标。</p></article><article><h3>能力评估</h3><p>确认双方资源和合作边界。</p></article><article><h3>方案验证</h3><p>通过样品、小试或项目验证。</p></article><article><h3>合作落地</h3><p>明确交付、支持和复盘机制。</p></article></div></div></section>
    <section class="section"><div class="container"><div class="section-heading"><div><p class="eyebrow">常见问题</p><h2>合作前，您可能关心</h2></div></div><div class="accordion"><details><summary>如何申请成为区域经销合作伙伴？</summary><p>提交目标区域、现有渠道、服务团队和重点客户类型，商务团队评估后安排沟通。</p></details><details><summary>联合开发需要提供哪些信息？</summary><p>建议提供目标产品体系、关键性能、基材、工艺条件和计划周期。</p></details><details><summary>是否可以先做样品或小试？</summary><p>可根据项目情况讨论样品、小试或联合测试方式。</p></details></div></div></section>
  </div>
</template>
