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
const scrollTimeline = (direction: number) => timelineTrack.value?.scrollBy({ left: direction * 360, behavior: 'smooth' })
const scrollTeam = (direction: number) => teamTrack.value?.scrollBy({ left: direction * 360, behavior: 'smooth' })

const milestones = [
  { date: '2026年1月', title: '新一代气凝胶常压干燥技术发布', text: '“新一代气凝胶及其复合材料技术发布暨产业发展签约大会”在湖南长沙隆重举行。', image: '/images/launch-1.jpg' },
  { date: '2024年6月', title: '新一代气凝胶常压干燥技术突破', text: '“新一代气凝胶及其复合材料技术发布暨产业发展签约大会”在湖南长沙隆重举行。', image: '/images/launch-1.jpg' },
  { date: '2019年4月', title: '气凝胶柔性复合材料中试', text: '“新一代气凝胶及其复合材料技术发布暨产业发展签约大会”在湖南长沙隆重举行。', image: '/images/launch-1.jpg' },
  { date: '2015年6月', title: '气凝胶分散体技术开发', text: '“新一代气凝胶及其复合材料技术发布暨产业发展签约大会”在湖南长沙隆重举行。', image: '/images/launch-1.jpg' },
  { date: '2022年7月', title: '奥飞公司成立及产业化验证', text: '“新一代气凝胶及其复合材料技术发布暨产业发展签约大会”在湖南长沙隆重举行。', image: '/images/launch-1.jpg' }
]

const team = [
  { role: '技术带头人', name: '周科朝', text: '教授、博士生导师，原中南大学副校长，粉末冶金全国重点实验室主任。', image: '/images/team-5.jpg' },
  { role: '首席科学家', name: '宋淼', text: '中南大学特聘教授、博士生导师、升华学者、国家级高层次青年人才。', image: '/images/team-4.jpg' },
  { role: '技术总监', name: '张丁日', text: '中南大学博士、奥飞新材董事长、湖南省“优秀创新创业导师”。', image: '/images/team-2.jpg' },
  { role: '技术顾问', name: '宋祁朋', text: '西安电子科技大学副教授，硕士生导师，西安电子科技大学“华山菁英学者”人才基金获得者。', image: '/images/team-3.jpg' },
  { role: '技术顾问', name: '蔡圳阳', text: '中南大学副教授、青年科协秘书长，中国有色金属产业联盟专家委员会委员等。', image: '/images/team-6.jpg' }
]

const facilities = [
  { name: '湖南省浏阳市研发基地', address: '湖南省浏阳市永安镇星辰·尚东产业小镇', image: '/images/about-facility.webp' },
  { name: '湖南省浏阳市生产基地', address: '湖南省浏阳市永安镇星辰·尚东产业小镇', image: '/images/about-facility.webp' },
  { name: '湖南省长沙市天心区销售中心', address: '长沙市天心区天心数谷创芯中心1-2栋', image: '/images/about-facility.webp' },
  { name: '中南大学科技园办公点', address: '中南大学科技园研发总部1栋四楼407房', image: '/images/about-facility.webp' }
]
</script>

<template>
  <div class="brief-page about-page">
    <PageHero class="brief-hero" page-key="about" eyebrow="关于我们" title="中南大学气凝胶成果转化核心平台" image="/images/about-hero.webp" />

    <section v-if="content?.contentHtml" class="brief-section">
      <article class="container article cms-rich-text" v-html="content.contentHtml" />
    </section>

    <template v-else>
    <section class="brief-section company-intro-section">
      <div class="container company-intro-grid">
        <div class="company-intro-copy">
          <p class="eyebrow">关于我们</p>
          <h2>湖南奥飞新材料有限公司</h2>
          <p>湖南奥飞新材料有限公司于2022年7月25日成立，是一家专注于世界十大新材料“气凝胶”技术开发及应用的有限责任公司。</p>
          <p>公司以节能、环保、安全的底层材料革新为目标，致力于为全领域节能减排、实现碳中和贡献力量，力争成为中国气凝胶行业头部企业。</p>
          <p>公司研发成员来源于中南大学粉末冶金全国重点实验室，与湘潭大学、西安电子科技大学等高水平科研单位深度合作，拥有国家级专家、博士等科研开发人才。团队在气凝胶涂料技术、低成本气凝胶制备技术等方面具有深厚的研发实力。产品已经推向市场并获得成功。</p>
          <div class="company-stats">
            <article><strong>15<sup>+年</sup></strong><span>气凝胶研发时间</span></article>
            <article><strong>30<sup>+项</sup></strong><span>授权发明专利</span></article>
            <article><strong>40%</strong><small>以上</small><span>气凝胶生产成本减低</span></article>
          </div>
        </div>
        <figure class="company-intro-media"><img src="/images/about-company.webp" alt="湖南奥飞新材料有限公司办公环境" decoding="async" loading="lazy"></figure>
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
            <button type="button" aria-label="向前查看" @click="scrollTimeline(-1)">←</button>
            <button type="button" aria-label="向后查看" @click="scrollTimeline(1)">→</button>
          </div>
        </div>
        <div ref="timelineTrack" class="brief-carousel-track milestone-track">
          <article v-for="item in milestones" :key="item.date" class="milestone-card">
            <p class="milestone-title">{{ item.title }}</p>
            <h3>{{ item.date }}</h3>
            <p>{{ item.text }}</p>
            <img :src="item.image" :alt="item.title" decoding="async" loading="lazy">
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
            <button type="button" aria-label="向前查看" @click="scrollTeam(-1)">←</button>
            <button type="button" aria-label="向后查看" @click="scrollTeam(1)">→</button>
          </div>
        </div>
        <div ref="teamTrack" class="brief-carousel-track team-track">
          <article v-for="member in team" :key="member.name" class="team-profile-card">
            <img :src="member.image" :alt="member.name" decoding="async" loading="lazy">
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
            <img :src="facility.image" :alt="facility.name" decoding="async" loading="lazy">
            <div><h3>{{ facility.name }}</h3><p>地址：{{ facility.address }}</p></div>
          </article>
        </div>
      </div>
    </section>
    </template>
  </div>
</template>
