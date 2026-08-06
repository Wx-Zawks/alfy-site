<script setup lang="ts">
defineProps<{
  items: Array<{
    label: string
    to: string
  }>
}>()
</script>

<template>
  <nav class="technology-subnav" aria-label="合作页面">
    <div class="container">
      <NuxtLink v-for="item in items" :key="item.to" :to="item.to">
        {{ item.label }}
      </NuxtLink>
    </div>
  </nav>
</template>
