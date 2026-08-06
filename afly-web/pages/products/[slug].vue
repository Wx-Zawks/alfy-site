<script setup lang="ts">
import type { ApiProductDetail } from '~/types/api'
import { useApiClient } from '~/composables/useApi'
import { useContentMapper } from '~/composables/useContentMapper'

const route = useRoute()
const { resolveMediaUrl } = useApiClient()
const { mapProduct } = useContentMapper()
const { data } = await useApi<ApiProductDetail>(
  `public-product-${String(route.params.slug)}`,
  `/public/products/${encodeURIComponent(String(route.params.slug))}`,
  { optional: true }
)
if (!data.value) throw createError({ statusCode: 404, statusMessage: '产品不存在或尚未发布' })

const product = computed(() => data.value ? mapProduct(data.value, resolveMediaUrl) : null)
const specificationRows = computed(() => {
  const value = product.value?.specifications
  if (!value) return []
  if (Array.isArray(value)) {
    return value.map((item, index) => {
      if (item && typeof item === 'object') {
        const record = item as Record<string, unknown>
        return { label: String(record.label ?? record.name ?? `参数 ${index + 1}`), value: String(record.value ?? record.content ?? '') }
      }
      return { label: `参数 ${index + 1}`, value: String(item) }
    })
  }
  if (typeof value === 'object') return Object.entries(value as Record<string, unknown>).map(([label, entry]) => ({ label, value: String(entry ?? '') }))
  return [{ label: '参数', value: String(value) }]
})

useSeoMeta({
  title: () => product.value?.seo?.title || product.value?.name || '产品详情',
  description: () => product.value?.seo?.description || product.value?.summary || '',
  keywords: () => product.value?.seo?.keywords || ''
})
const { open } = useInquiryDialog()
</script>

<template>
  <div v-if="product">
    <section class="section"><div class="container detail-layout"><div class="detail-image"><img :src="product.image" :alt="product.name"></div><div><p class="eyebrow">{{ product.categoryName }}</p><h1>{{ product.name }}</h1><p class="lead">{{ product.summary }}</p><div v-if="product.features.length" class="feature-list"><span v-for="feature in product.features" :key="feature">{{ feature }}</span></div><div class="button-row"><button class="button button-primary" type="button" @click="open">获取技术资料 →</button><NuxtLink class="button button-outline" to="/applications">查看应用案例 →</NuxtLink></div></div></div></section>
    <section v-if="product.contentHtml || specificationRows.length" class="section section-muted"><div class="container"><div v-if="product.contentHtml" class="cms-rich-text article" v-html="product.contentHtml" /><div v-if="specificationRows.length" class="specification-list"><div v-for="row in specificationRows" :key="row.label"><b>{{ row.label }}</b><span>{{ row.value }}</span></div></div></div></section>
  </div>
</template>
