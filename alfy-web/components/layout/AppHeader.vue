<script setup lang="ts">
const route = useRoute()
const { open } = useInquiryDialog()
const menuOpen = ref(false)

const navItems = [
  { label: '首页', to: '/' },
  { label: '产品', to: '/products' },
  { label: '应用与案例', to: '/applications' },
  { label: '技术', to: '/technology' },
  { label: '新闻', to: '/news' },
  { label: '合作', to: '/cooperation' },
  { label: '我们', to: '/about' }
]

const isActive = (to: string) => to === '/' ? route.path === '/' : route.path.startsWith(to)

watch(() => route.fullPath, () => { menuOpen.value = false })
</script>

<template>
  <header class="site-header">
    <NuxtLink class="brand" to="/" aria-label="奥飞新材首页">
      <img class="brand-logo" src="/images/alfy-logo.png" alt="奥飞新材 ALFY">
    </NuxtLink>

    <button class="menu-toggle" type="button" :aria-expanded="menuOpen" aria-label="打开导航" @click="menuOpen = !menuOpen">
      <span /><span /><span />
    </button>

    <nav class="site-nav" :class="{ open: menuOpen }" aria-label="主导航">
      <NuxtLink v-for="item in navItems" :key="item.to" :to="item.to" :class="{ active: isActive(item.to) }">{{ item.label }}</NuxtLink>
      <button class="button button-primary button-small nav-cta" type="button" @click="open">获取方案 <span>↗</span></button>
    </nav>
  </header>
</template>
