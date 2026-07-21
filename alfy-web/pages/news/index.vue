<script setup lang="ts">
import { articles } from '~/data/articles'

useSeoMeta({ title: '新闻资讯', description: '奥飞新材公司新闻、行业新闻与技术动态。' })
const activeCategory = ref('all')
const filtered = computed(() => activeCategory.value === 'all' ? articles : articles.filter(item => item.category === activeCategory.value))
const featured = computed(() => filtered.value[0])
const remaining = computed(() => filtered.value.slice(1))
</script>

<template>
  <div><PageHero eyebrow="Newsroom" title="关注气凝胶产业" highlight="最新进展" description="记录科研、产品、项目与产业合作的重要节点。" image="/images/launch-2.jpg" /><section class="section news-archive-section"><div class="container"><div class="section-heading news-archive-heading"><div><p class="eyebrow">新闻与观点</p><h2>全部资讯</h2></div><div class="filters"><button :class="{ active: activeCategory === 'all' }" @click="activeCategory = 'all'">全部</button><button :class="{ active: activeCategory === 'company' }" @click="activeCategory = 'company'">公司新闻</button><button :class="{ active: activeCategory === 'industry' }" @click="activeCategory = 'industry'">行业新闻</button><button :class="{ active: activeCategory === 'technology' }" @click="activeCategory = 'technology'">技术文章</button></div></div><div v-if="featured" class="news-archive"><NuxtLink class="news-archive-feature" :to="`/news/${featured.slug}`"><img :src="featured.image" :alt="featured.title"><div><span>{{ featured.categoryName }} · {{ featured.date }}</span><h2>{{ featured.title }}</h2><p>{{ featured.summary }}</p><b>阅读全文 ↗</b></div></NuxtLink><div class="news-archive-list"><NuxtLink v-for="item in remaining" :key="item.id" :to="`/news/${item.slug}`"><div><span>{{ item.categoryName }}</span><time>{{ item.date }}</time></div><h3>{{ item.title }}</h3><p>{{ item.summary }}</p><b>查看详情 ↗</b></NuxtLink></div></div><p v-else class="empty-state">该栏目暂无内容。</p></div></section></div>
</template>
