<script setup lang="ts">
import type {
  ApiInquiryRequest,
  ApiInquiryResult,
  ApiNavigation,
  ApiNavigationItem,
  ApiSiteSetting
} from '~/types/api'
import { useApi, useApiClient } from '~/composables/useApi'

interface FooterGroup {
  id: number
  label: string
  links: ApiNavigationItem[]
  target?: string
}

const { open } = useInquiryDialog()
const { request, resolveMediaUrl } = useApiClient()
const route = useRoute()
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
      { id: -13, label: '研发团队', target: '/about#team', enabled: true, highlighted: false, sortOrder: 3, children: [] },
      { id: -14, label: '联系我们', target: '/about', enabled: true, highlighted: false, sortOrder: 4, children: [] }
    ]
  },
  {
    id: -2,
    label: '产品与方案',
    links: [
      { id: -21, label: '产品中心', target: '/products', enabled: true, highlighted: false, sortOrder: 1, children: [] },
      { id: -22, label: '应用场景', target: '/applications', enabled: true, highlighted: false, sortOrder: 2, children: [] },
      { id: -23, label: '核心技术', target: '/technology', enabled: true, highlighted: false, sortOrder: 3, children: [] }
    ]
  },
  {
    id: -3,
    label: '合作模式',
    links: [
      { id: -31, label: '经销商合作', target: '/cooperation/dealer', enabled: true, highlighted: false, sortOrder: 1, children: [] },
      { id: -32, label: '复合产品开发', target: '/cooperation/product-development', enabled: true, highlighted: false, sortOrder: 2, children: [] },
      { id: -33, label: '产业链合作', target: '/cooperation/industry-chain', enabled: true, highlighted: false, sortOrder: 3, children: [] }
    ]
  },
  {
    id: -4,
    label: '新闻资讯',
    links: [
      { id: -41, label: '公司新闻', target: '/news', enabled: true, highlighted: false, sortOrder: 1, children: [] },
      { id: -42, label: '行业动态', target: '/news', enabled: true, highlighted: false, sortOrder: 2, children: [] },
      { id: -43, label: '研发成果', target: '/news', enabled: true, highlighted: false, sortOrder: 3, children: [] }
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
const servicePhone = computed(() => site.value?.servicePhone || '0731-83976616')
const serviceEmail = computed(() => site.value?.serviceEmail || 'info@alfy.cn')
const wechatQrUrl = computed(() => resolveMediaUrl(site.value?.wechatQrImageUrl, '/images/alfy-site-qr.png'))
const copyright = computed(() => site.value?.copyrightText || `© ${new Date().getFullYear()} ${companyName.value}`)

const submitting = ref(false)
const inquiryResult = ref<ApiInquiryResult | null>(null)
const inquiryError = ref('')
const inquiryForm = reactive<ApiInquiryRequest>({
  inquiryType: '项目方案',
  message: '',
  name: '',
  phone: '',
  privacyAccepted: false
})

async function submitInquiry() {
  submitting.value = true
  inquiryError.value = ''
  try {
    const queryEntries = Object.entries(route.query)
      .filter(([key, value]) => key.startsWith('utm_') && typeof value === 'string')
      .map(([key, value]) => [key, String(value)])
    inquiryResult.value = await request<ApiInquiryResult>('/public/inquiries', {
      method: 'POST',
      body: {
        ...inquiryForm,
        sourceUrl: import.meta.client ? window.location.href : route.fullPath,
        utm: Object.fromEntries(queryEntries)
      }
    })
  } catch (error) {
    const value = error as { data?: { message?: string }; message?: string }
    inquiryError.value = value.data?.message || value.message || '提交失败，请稍后重试或直接联系我们。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <footer class="site-footer">
    <div class="container footer-header">
      <NuxtLink class="footer-logo-wrap" to="/" :aria-label="`${companyName}首页`"><img :src="logoUrl" :alt="companyName"></NuxtLink>
      <strong>{{ companyName }}</strong>
    </div>
    <div class="container footer-main">
      <div class="footer-brand">
        <h3>联系我们</h3>
        <img class="footer-qr" :src="wechatQrUrl" :alt="`${companyName}联系二维码`">
        <a class="footer-contact-detail" :href="`tel:${servicePhone}`"><b>企业电话：</b>{{ servicePhone }}</a>
        <a class="footer-contact-detail" :href="`mailto:${serviceEmail}`"><b>企业邮箱：</b>{{ serviceEmail }}</a>
        <button class="footer-contact-link" type="button" @click="open">立即咨询 <span>→</span></button>
      </div>
      <div v-for="group in footerGroups" :key="group.id" class="footer-links">
        <h3><NuxtLink v-if="group.target" :to="group.target">{{ group.label }}</NuxtLink><template v-else>{{ group.label }}</template></h3>
        <NuxtLink v-for="item in group.links" :key="item.id" :to="item.target">{{ item.label }}</NuxtLink>
      </div>
      <section class="footer-inquiry">
        <h3>获取产品资料或项目方案建议</h3>
        <div v-if="inquiryResult" class="footer-inquiry-success" role="status">
          <b>咨询已提交</b>
          <span>咨询编号：{{ inquiryResult.inquiryNo }}</span>
        </div>
        <form v-else class="footer-inquiry-form" @submit.prevent="submitInquiry">
          <label><span>姓名</span><input v-model.trim="inquiryForm.name" required maxlength="64" autocomplete="name" placeholder="请输入姓名"></label>
          <label><span>手机号</span><input v-model.trim="inquiryForm.phone" required inputmode="tel" maxlength="32" autocomplete="tel" placeholder="请输入手机号"></label>
          <label><span>需求描述</span><textarea v-model.trim="inquiryForm.message" required maxlength="5000" rows="3" placeholder="请简单描述应用场景或项目需求" /></label>
          <label class="footer-consent"><input v-model="inquiryForm.privacyAccepted" required type="checkbox"> 同意提交并处理以上联系信息</label>
          <p v-if="inquiryError" class="form-error" role="alert">{{ inquiryError }}</p>
          <button class="button button-primary" type="submit" :disabled="submitting">{{ submitting ? '正在提交…' : '提交咨询 →' }}</button>
        </form>
      </section>
    </div>
    <div class="container footer-bottom">
      <span>{{ copyright }}</span>
      <a v-if="site?.icpNumber" href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">{{ site.icpNumber }}</a>
      <span v-else>节能 · 环保 · 安全</span>
    </div>
  </footer>
</template>
