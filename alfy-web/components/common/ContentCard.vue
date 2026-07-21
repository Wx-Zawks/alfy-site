<script setup lang="ts">
defineProps<{ to: string; image: string; tag: string; title: string; summary: string }>()
</script>

<template>
  <NuxtLink class="content-card" :to="to">
    <img :src="image" :alt="title">
    <div class="card-body"><span class="tag">{{ tag }}</span><h3>{{ title }}</h3><p>{{ summary }}</p><span class="text-link">查看详情 ↗</span></div>
  </NuxtLink>
</template>
