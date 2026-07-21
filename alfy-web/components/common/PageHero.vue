<script setup lang="ts">
defineProps<{ eyebrow: string; title: string; highlight?: string; description: string; image: string }>()
</script>

<template>
  <section class="page-hero" :style="{ '--page-image': `url(${image})` }">
    <div class="container page-hero-content">
      <p class="eyebrow eyebrow-light">{{ eyebrow }}</p>
      <h1>{{ title }}<br><em v-if="highlight">{{ highlight }}</em></h1>
      <p>{{ description }}</p>
      <slot />
    </div>
  </section>
</template>
