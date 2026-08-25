<script setup lang="ts">
import type { Product } from '~/types/content'
defineProps<{ product: Product }>()
</script>

<template>
  <NuxtLink class="product-card" :to="`/products/${product.slug}`">
    <img :src="product.image" :alt="product.name" decoding="async" loading="lazy">
    <div class="card-body"><span class="tag">{{ product.categoryName }}</span><h3>{{ product.name }}</h3><p>{{ product.summary }}</p><span class="text-link">查看详情 <ArrowUpRightIcon /></span></div>
  </NuxtLink>
</template>
