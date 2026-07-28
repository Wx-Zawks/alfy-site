<script setup lang="ts">
import type { ApiNavigation, ApiNavigationItem, ApiSiteSetting } from '~/types/api'
import { useApi, useApiClient } from '~/composables/useApi'

const route = useRoute()
const { open } = useInquiryDialog()
const { resolveMediaUrl } = useApiClient()
const menuOpen = ref(false)
const transparentHeader = computed(() => route.path === '/')
const overlayHeader = computed(() => transparentHeader.value || route.path.startsWith('/technology') || route.path === '/about')

const [{ data: site }, { data: navigation }] = await Promise.all([
  useApi<ApiSiteSetting>('public-site-setting', '/public/site'),
  useApi<ApiNavigation>('public-navigation', '/public/navigation')
])

const fallbackNav: ApiNavigationItem[] = [
  { id: -1, label: '首页', target: '/', enabled: true, highlighted: false, sortOrder: 1, children: [] },
  { id: -2, label: '产品', target: '/products', enabled: true, highlighted: false, sortOrder: 2, children: [] },
  { id: -3, label: '应用与案例', target: '/applications', enabled: true, highlighted: false, sortOrder: 3, children: [] },
  { id: -4, label: '技术', target: '/technology', enabled: true, highlighted: false, sortOrder: 4, children: [] },
  { id: -5, label: '新闻', target: '/news', enabled: true, highlighted: false, sortOrder: 5, children: [] },
  { id: -6, label: '合作', target: '/cooperation', enabled: true, highlighted: false, sortOrder: 6, children: [] },
  { id: -7, label: '我们', target: '/about', enabled: true, highlighted: false, sortOrder: 7, children: [] }
]

const navItems = computed(() => {
  const published = navigation.value?.header?.filter(item => item.enabled) ?? []
  return published.length ? published : fallbackNav
})
const logoUrl = computed(() => resolveMediaUrl(site.value?.logoUrl, '/images/alfy-logo.png'))
const companyName = computed(() => site.value?.companyName || '奥飞新材')
const isActive = (to: string) => to === '/' ? route.path === '/' : route.path.startsWith(to)

watch(() => route.fullPath, () => { menuOpen.value = false })
</script>

<template>
  <header class="site-header" :class="{ 'is-overlay': overlayHeader, 'is-open': menuOpen, 'is-transparent': transparentHeader }">
    <NuxtLink class="brand" to="/" :aria-label="`${companyName}首页`">
      <span class="brand-glyph" aria-hidden="true"><img :src="logoUrl" alt=""></span>
      <span class="brand-word">ALFY</span>
    </NuxtLink>

    <button class="menu-toggle" type="button" :aria-expanded="menuOpen" aria-label="打开导航" @click="menuOpen = !menuOpen">
      <span /><span /><span />
    </button>

    <nav class="site-nav" :class="{ open: menuOpen }" aria-label="主导航">
      <NuxtLink
        v-for="item in navItems"
        :key="item.id"
        :to="item.target"
        :class="{ active: isActive(item.target), highlighted: item.highlighted }"
      >{{ item.label }}</NuxtLink>
      <button class="button button-primary button-small nav-cta" type="button" @click="open">获取方案 <span>→</span></button>
    </nav>
  </header>
</template>
