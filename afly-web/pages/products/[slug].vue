<script setup lang="ts">
import type { ApiProductDetail } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { mapProduct } from '~/composables/useContentMapper'

const route = useRoute()
const { resolveMediaUrl } = useApiClient()
const { data } = await useApi<ApiProductDetail>(
  `public-product-${String(route.params.slug)}`,
  `/public/products/${encodeURIComponent(String(route.params.slug))}`,
  { optional: true }
)
if (!data.value) throw createError({ statusCode: 404, statusMessage: '产品不存在或尚未发布' })

const product = computed(() => data.value ? mapProduct(data.value, resolveMediaUrl) : null)

useSeoMeta({
  title: () => product.value?.seo?.title || product.value?.name || '产品详情',
  description: () => product.value?.seo?.description || product.value?.summary || '',
  keywords: () => product.value?.seo?.keywords || ''
})
const { open } = useInquiryDialog()
</script>

<template>
  <div v-if="product" class="product-detail-page">
    <section class="section"><div class="container detail-layout"><div class="detail-image"><img :src="product.image" :alt="product.name"></div><div><p class="eyebrow">{{ product.categoryName }}</p><h1>{{ product.name }}</h1><p class="lead">{{ product.summary }}</p><div v-if="product.features.length" class="feature-list"><span v-for="feature in product.features" :key="feature">{{ feature }}</span></div><div class="button-row"><button class="button button-primary" type="button" @click="open">获取技术资料 →</button><NuxtLink class="button button-outline" to="/applications">查看应用案例 →</NuxtLink></div></div></div></section>
    <section v-if="product.contentHtml" class="section section-muted"><div class="container"><div class="cms-rich-text article" v-html="product.contentHtml" /></div></section>
  </div>
</template>
