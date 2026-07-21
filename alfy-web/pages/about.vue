<script setup lang="ts">
useSeoMeta({ title: '关于我们', description: '了解湖南奥飞新材料有限公司、研发团队、招聘与联系方式。' })
const { open } = useInquiryDialog()
const team = [
  { name: '材料制备方向', text: '常压干燥、孔结构与粉体制备研究。', image: '/images/team-1.jpg' },
  { name: '分散改性方向', text: '分散体系、界面设计与复合配方。', image: '/images/team-2.jpg' },
  { name: '复合产品方向', text: '涂料、毡板及多元复合产品开发。', image: '/images/team-3.jpg' }
]
</script>

<template>
  <div><PageHero eyebrow="About ALFY" title="让气凝胶技术" highlight="服务真实产业" description="以科研积淀为起点，以产品和项目为桥梁，推动新材料成果走向规模化应用。" image="/images/university.jpeg" />
    <section class="section"><div class="container split"><div><p class="eyebrow">企业介绍</p><h2>湖南奥飞<br><em>新材料有限公司</em></h2></div><div><p class="lead">奥飞新材聚焦新一代气凝胶及其复合产品技术，依托中南大学科研积淀，推动气凝胶材料从基础研究走向产品开发和产业应用。</p><p>公司面向建筑节能、工业节能等场景，形成从粉体、分散体到涂料、毡板和配套体系的产品方向。</p></div></div></section>
    <section id="team" class="section section-muted"><div class="container"><div class="section-heading"><div><p class="eyebrow">研发团队</p><h2>连接材料研究与产业需求</h2><p class="lead">团队姓名、职务和履历是否公开需经本人及甲方确认。</p></div></div><div class="content-grid"><article v-for="member in team" :key="member.name" class="content-card"><img :src="member.image" :alt="member.name" style="height:390px;object-position:top"><div class="card-body"><h3>{{ member.name }}</h3><p>{{ member.text }}</p></div></article></div></div></section>
    <section id="jobs" class="section"><div class="container"><div class="section-heading"><div><p class="eyebrow">招聘信息</p><h2>与我们一起推动材料创新</h2></div></div><div class="accordion"><details><summary>材料研发工程师 · 长沙 · 全职</summary><p>参与气凝胶材料制备、分散与性能测试；岗位要求和投递邮箱待甲方确认。</p></details><details><summary>应用技术工程师 · 长沙/项目地 · 全职</summary><p>参与客户需求分析、产品选型、样品测试和项目现场技术支持。</p></details></div></div></section>
    <section id="contact" class="section section-dark"><div class="container split"><div><p class="eyebrow eyebrow-light">联系我们</p><h2>期待与您<br><em>建立连接</em></h2><p>公司地址、电话、备案号等正式信息待甲方确认。</p><button class="button button-light" type="button" @click="open">提交项目需求 ↗</button></div><div class="info-grid" style="grid-template-columns:1fr 1fr"><article class="info-card"><span>公司</span><h3>湖南奥飞新材料有限公司</h3></article><article class="info-card"><span>地址</span><h3>湖南省长沙市 · 待确认</h3></article><article class="info-card"><span>电话</span><h3>待甲方确认</h3></article><article class="info-card"><span>邮箱</span><h3>contact@alfy.com.cn</h3></article></div></div></section>
  </div>
</template>
