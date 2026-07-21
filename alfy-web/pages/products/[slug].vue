<script setup lang="ts">
import { products } from '~/data/products'

const route = useRoute()
const product = computed(() => products.find(item => item.slug === route.params.slug))
if (!product.value) throw createError({ statusCode: 404, statusMessage: '产品不存在' })

useSeoMeta({ title: () => product.value?.name || '产品详情', description: () => product.value?.summary || '' })
const { open } = useInquiryDialog()
</script>

<template>
  <div v-if="product"><section class="section"><div class="container detail-layout"><div class="detail-image"><img :src="product.image" :alt="product.name"></div><div><p class="eyebrow">{{ product.categoryName }}</p><h1>{{ product.name }}</h1><p class="lead">{{ product.summary }}</p><div class="feature-list"><span v-for="feature in product.features" :key="feature">{{ feature }}</span></div><div class="button-row"><button class="button button-primary" type="button" @click="open">获取技术资料 ↗</button><NuxtLink class="button button-outline" to="/applications">查看应用案例 ↗</NuxtLink></div></div></div></section><section class="section section-muted"><div class="container"><div class="section-heading"><div><p class="eyebrow">产品特点</p><h2>围绕真实施工与使用价值</h2></div></div><div class="info-grid"><article v-for="(feature, index) in product.features" :key="feature" class="info-card"><span>0{{ index + 1 }}</span><h3>{{ feature }}</h3><p>正式参数、检测口径和应用说明将在接入后台后结构化展示。</p></article></div></div></section></div>
</template>
