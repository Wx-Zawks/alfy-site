<script setup lang="ts">
import type { ApiNavigation, ApiNavigationItem, ApiSiteSetting } from '~/types/api'
import { useApi, useApiClient } from '~/composables/useApi'

interface FooterGroup {
  id: number
  label: string
  links: ApiNavigationItem[]
  target?: string
}

const { open } = useInquiryDialog()
const { resolveMediaUrl } = useApiClient()
const [{ data: site }, { data: navigation }] = await Promise.all([
  useApi<ApiSiteSetting>('public-site-setting', '/public/site'),
  useApi<ApiNavigation>('public-navigation', '/public/navigation')
])

const fallbackGroups: FooterGroup[] = [
  {
    id: -1,
    label: '关于奥飞',
    links: [
      { id: -11, label: '企业介绍', target: '/about', enabled: true, highlighted: false, sortOrder: 1, children: [] },
      { id: -12, label: '发展历程', target: '/about#history', enabled: true, highlighted: false, sortOrder: 2, children: [] },
      { id: -13, label: '研发团队', target: '/about#team', enabled: true, highlighted: false, sortOrder: 3, children: [] }
    ]
  },
  {
    id: -2,
    label: '应用与技术',
    links: [
      { id: -21, label: '产品中心', target: '/products', enabled: true, highlighted: false, sortOrder: 1, children: [] },
      { id: -22, label: '应用场景', target: '/applications', enabled: true, highlighted: false, sortOrder: 2, children: [] },
      { id: -23, label: '核心技术', target: '/technology', enabled: true, highlighted: false, sortOrder: 3, children: [] }
    ]
  },
  {
    id: -3,
    label: '合作与资讯',
    links: [
      { id: -31, label: '新闻动态', target: '/news', enabled: true, highlighted: false, sortOrder: 1, children: [] },
      { id: -32, label: '商务合作', target: '/cooperation', enabled: true, highlighted: false, sortOrder: 2, children: [] }
    ]
  }
]

const footerGroups = computed<FooterGroup[]>(() => {
  const items = navigation.value?.footer?.filter(item => item.enabled) ?? []
  if (!items.length) return fallbackGroups
  const nested = items.filter(item => item.children?.some(child => child.enabled))
  if (nested.length) {
    return nested.map(item => ({
      id: item.id,
      label: item.label,
      target: item.target,
      links: item.children.filter(child => child.enabled)
    }))
  }
  return [{ id: 0, label: '网站导航', links: items }]
})
const logoUrl = computed(() => resolveMediaUrl(site.value?.logoUrl, '/images/alfy-logo.png'))
const companyName = computed(() => site.value?.companyName || '湖南奥飞新材料有限公司')
const copyright = computed(() => site.value?.copyrightText || `© ${new Date().getFullYear()} ${companyName.value}`)
</script>

<template>
  <footer class="site-footer">
    <div class="container footer-main">
      <div class="footer-brand">
        <NuxtLink class="footer-logo-wrap" to="/" :aria-label="`${companyName}首页`"><img :src="logoUrl" :alt="companyName"></NuxtLink>
        <p>{{ companyName }}</p>
        <p v-if="site?.address">{{ site.address }}</p>
        <a v-if="site?.servicePhone" class="footer-contact-link" :href="`tel:${site.servicePhone}`">{{ site.servicePhone }}</a>
        <a v-if="site?.serviceEmail" class="footer-contact-link" :href="`mailto:${site.serviceEmail}`">{{ site.serviceEmail }}</a>
        <button class="footer-contact-link" type="button" @click="open">立即咨询 <span>→</span></button>
      </div>
      <div v-for="group in footerGroups" :key="group.id" class="footer-links">
        <h3><NuxtLink v-if="group.target" :to="group.target">{{ group.label }}</NuxtLink><template v-else>{{ group.label }}</template></h3>
        <NuxtLink v-for="item in group.links" :key="item.id" :to="item.target">{{ item.label }}</NuxtLink>
      </div>
      <div class="footer-inquiry">
        <p class="eyebrow">项目咨询</p>
        <h3>获取产品资料和项目方案建议</h3>
        <p>提供应用场景、工况与目标，我们将为您匹配产品及技术路径。</p>
        <button class="button button-primary" type="button" @click="open">提交需求 <span>→</span></button>
      </div>
    </div>
    <div class="container footer-bottom">
      <span>{{ copyright }}</span>
      <a v-if="site?.icpNumber" href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">{{ site.icpNumber }}</a>
      <span v-else>节能 · 环保 · 安全</span>
    </div>
  </footer>
</template>
