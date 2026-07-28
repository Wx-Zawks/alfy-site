<script setup lang="ts">
import type { ApiContentPage } from '~/types/api'

const { data: content } = await useApi<ApiContentPage>('public-page-about', '/public/pages/about', { optional: true })
useSeoMeta({
  title: () => content.value?.seoTitle || content.value?.title || '关于我们',
  description: () => content.value?.seoDescription || content.value?.summary || '了解湖南奥飞新材料有限公司、气凝胶研发历程、核心团队与产业布局。',
  keywords: () => content.value?.seoKeywords || ''
})

const timelineTrack = ref<HTMLElement | null>(null)
const teamTrack = ref<HTMLElement | null>(null)
const scrollRow = (target: HTMLElement | null, direction: number) => target?.scrollBy({ left: direction * 360, behavior: 'smooth' })

const milestones = [
  { date: '2015年6月', title: '气凝胶分散体技术开发', text: '核心团队开始围绕气凝胶分散、界面改性及稳定化调控开展系统研究。', image: '/images/launch-2.jpg' },
  { date: '2019年4月', title: '气凝胶柔性复合材料中试', text: '复合材料制备路线进入中试验证，持续提升气凝胶结构保留率与应用性能。', image: '/images/launch-3.jpg' },
  { date: '2022年7月', title: '奥飞公司成立及产业化验证', text: '湖南奥飞新材料有限公司成立，推动高校科研成果走向产品和规模化应用。', image: '/images/news-3.jpeg' },
  { date: '2024年8月', title: '常压干燥中试线落地', text: '气凝胶中试产业化落地浏阳尚东产业园，形成材料、涂料及固废处理闭环。', image: '/images/news-4.jpeg' },
  { date: '2026年1月', title: '新一代气凝胶技术发布', text: '新一代气凝胶及其复合材料技术发布暨产业发展签约大会在长沙举行。', image: '/images/launch-1.jpg' }
]

const team = [
  { role: '技术带头人', name: '周科朝', text: '教授、博士生导师，粉末冶金全国重点实验室主任。', image: '/images/team-5.jpg' },
  { role: '首席科学家', name: '宋淼', text: '中南大学特聘教授、博士生导师、国家级高层次青年人才。', image: '/images/team-4.jpg' },
  { role: '技术总监', name: '张丁日', text: '中南大学博士、奥飞新材董事长，湖南省优秀创新创业导师。', image: '/images/team-2.jpg' },
  { role: '技术顾问', name: '宋祁朋', text: '西安电子科技大学副教授、硕士生导师、华山菁英学者。', image: '/images/team-3.jpg' },
  { role: '技术顾问', name: '蔡圳阳', text: '中南大学副教授、青年科协秘书长，长期从事材料技术研究。', image: '/images/team-6.jpg' }
]

const facilities = [
  { name: '湖南省浏阳市研发基地', address: '湖南省浏阳市永安镇星辰·尚东产业小镇', image: '/images/university.jpeg' },
  { name: '湖南省浏阳市生产基地', address: '湖南省浏阳市永安镇星辰·尚东产业小镇', image: '/images/aerogel-granules-2.jpg' },
  { name: '湖南省长沙市天心区销售中心', address: '长沙市天心区天心数谷创芯中心1-2栋', image: '/images/news-1.jpeg' },
  { name: '中南大学科技园办公点', address: '中南大学科技园研发总部1栋四楼407房', image: '/images/research-institute.jpg' }
]
</script>

<template>
  <div class="brief-page about-page">
    <PageHero class="brief-hero" page-key="about" eyebrow="关于我们" title="中南大学气凝胶成果转化核心平台" image="/images/aerogel-powder.jpg" />

    <section v-if="content?.contentHtml" class="brief-section">
      <article class="container article cms-rich-text" v-html="content.contentHtml" />
    </section>

    <template v-else>
    <section class="brief-section company-intro-section">
      <div class="container company-intro-grid">
        <div class="company-intro-copy">
          <p class="eyebrow">关于我们</p>
          <h2>湖南奥飞新材料有限公司</h2>
          <p>湖南奥飞新材料有限公司成立于2022年7月25日，是一家专注于气凝胶技术开发及产业化的高新技术企业。</p>
          <p>公司以节能、环保、安全的底层材料革新为目标，致力于为全领域节能减排、实现碳中和贡献力量。研发成员来源于中南大学粉末冶金全国重点实验室，并与多所高校及科研单位深度合作。</p>
          <div class="company-stats">
            <article><strong>15<sup>+</sup></strong><span>气凝胶研发积淀</span></article>
            <article><strong>30<sup>+</sup></strong><span>专利及软件著作权</span></article>
            <article><strong>40<sup>%+</sup></strong><span>生产成本降低</span></article>
            <article><strong>200<sup>+</sup></strong><span>累计产品产出/吨</span></article>
          </div>
        </div>
        <figure class="company-intro-media"><img src="/images/university.jpeg" alt="中南大学气凝胶研发平台"><figcaption>中南大学 · 产学研协同创新</figcaption></figure>
      </div>
    </section>

    <section id="history" class="brief-section history-section">
      <div class="container">
        <div class="carousel-heading">
          <header class="brief-section-heading">
            <p class="eyebrow">发展历程</p>
            <h2>气凝胶及其复合材料研发历程</h2>
          </header>
          <div class="carousel-arrows" aria-label="发展历程翻页">
            <button type="button" aria-label="向前查看" @click="scrollRow(timelineTrack, -1)">←</button>
            <button type="button" aria-label="向后查看" @click="scrollRow(timelineTrack, 1)">→</button>
          </div>
        </div>
        <div ref="timelineTrack" class="brief-carousel-track milestone-track">
          <article v-for="(item, index) in milestones" :key="item.date" class="milestone-card">
            <span class="milestone-index">0{{ index + 1 }}</span>
            <p class="milestone-date">{{ item.date }}</p>
            <h3>{{ item.title }}</h3>
            <p>{{ item.text }}</p>
            <img :src="item.image" :alt="item.title">
          </article>
        </div>
      </div>
    </section>

    <section id="team" class="brief-section team-section">
      <div class="container">
        <div class="carousel-heading">
          <header class="brief-section-heading">
            <p class="eyebrow">核心研发团队</p>
            <h2>十余年气凝胶攻关积淀</h2>
          </header>
          <div class="carousel-arrows" aria-label="研发团队翻页">
            <button type="button" aria-label="向前查看" @click="scrollRow(teamTrack, -1)">←</button>
            <button type="button" aria-label="向后查看" @click="scrollRow(teamTrack, 1)">→</button>
          </div>
        </div>
        <div ref="teamTrack" class="brief-carousel-track team-track">
          <article v-for="member in team" :key="member.name" class="team-profile-card">
            <img :src="member.image" :alt="member.name">
            <div><span>{{ member.role }}</span><h3>{{ member.name }}</h3><p>{{ member.text }}</p></div>
          </article>
        </div>
      </div>
    </section>

    <section class="brief-section facilities-section">
      <div class="container">
        <header class="brief-section-heading">
          <p class="eyebrow">发展引擎</p>
          <h2>构建“研产销”一体化产业布局</h2>
        </header>
        <div class="facility-grid">
          <article v-for="facility in facilities" :key="facility.name" class="facility-card">
            <img :src="facility.image" :alt="facility.name">
            <div><h3>{{ facility.name }}</h3><p>地址：{{ facility.address }}</p></div>
          </article>
        </div>
      </div>
    </section>
    </template>
  </div>
</template>
