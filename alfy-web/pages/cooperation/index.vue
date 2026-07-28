<script setup lang="ts">
import type {
  ApiContentBlock,
  ApiContentPage
} from '~/types/api'
import { useApiClient } from '~/composables/useApi'

const modeDefinitions = [
  {
    fallbackSummary: '面向具备区域客户、项目服务或建材渠道能力的伙伴。',
    fallbackTitle: '经销商合作',
    key: 'cooperation-dealer',
    path: '/cooperation/dealer'
  },
  {
    fallbackSummary: '基于粉体、分散体和改性能力共同定义产品目标。',
    fallbackTitle: '复合产品开发模式',
    key: 'cooperation-product-development',
    path: '/cooperation/product-development'
  },
  {
    fallbackSummary: '连接科研、材料、制造、设计与工程资源。',
    fallbackTitle: '产业链合作模式',
    key: 'cooperation-industry-chain',
    path: '/cooperation/industry-chain'
  }
]

const [
  { data: content },
  ...modeRequests
] = await Promise.all([
  useApi<ApiContentPage>('public-page-cooperation', '/public/pages/cooperation'),
  ...modeDefinitions.map(mode =>
    useApi<ApiContentPage>(
      `public-page-${mode.key}`,
      `/public/pages/${mode.key}`,
      { optional: true }
    )
  )
])

const { open } = useInquiryDialog()
const { resolveMediaUrl } = useApiClient()
const pageData = computed(() => content.value?.contentData ?? {})
const modes = computed(() =>
  modeDefinitions
    .map((definition, index) => {
      const page = modeRequests[index]?.data.value
      return page
        ? {
            index: String(index + 1).padStart(2, '0'),
            path: definition.path,
            text: page.summary || definition.fallbackSummary,
            title: page.title || definition.fallbackTitle
          }
        : null
    })
    .filter(Boolean) as Array<{
      index: string
      path: string
      text: string
      title: string
    }>
)

function normalizeBlocks(
  value: ApiContentBlock[] | null | undefined,
  fallback: ApiContentBlock[]
) {
  return Array.isArray(value) && value.length ? value : fallback
}

const process = computed(() =>
  normalizeBlocks(pageData.value.process, [
    { title: '需求沟通', description: '明确市场、场景与合作目标。' },
    { title: '能力评估', description: '确认双方资源和合作边界。' },
    { title: '方案验证', description: '通过样品、小试或项目验证。' },
    { title: '合作落地', description: '明确交付、支持和复盘机制。' }
  ])
)
const faqs = computed(() =>
  normalizeBlocks(pageData.value.faqs, [
    {
      title: '如何申请成为区域经销合作伙伴？',
      description: '提交目标区域、现有渠道、服务团队和重点客户类型，商务团队评估后安排沟通。'
    },
    {
      title: '联合开发需要提供哪些信息？',
      description: '建议提供目标产品体系、关键性能、基材、工艺条件和计划周期。'
    },
    {
      title: '是否可以先做样品或小试？',
      description: '可根据项目情况讨论样品、小试或联合测试方式。'
    }
  ])
)

useSeoMeta({
  title: () => content.value?.seoTitle || content.value?.title || '商务合作',
  description: () => content.value?.seoDescription || content.value?.summary || '奥飞新材经销合作、复合产品开发与产业链合作。',
  keywords: () => content.value?.seoKeywords || ''
})
</script>

<template>
  <div v-if="content" class="cooperation-page">
    <PageHero
      :eyebrow="pageData.eyebrow || 'OPEN COLLABORATION'"
      :title="content.title || '开放共赢，共建'"
      :highlight="pageData.highlightText || '气凝胶产业生态'"
      :description="content.summary || ''"
      :image="resolveMediaUrl(content.coverImageUrl, '/images/news-1.jpeg')"
    >
      <div class="button-row">
        <button class="button button-primary" type="button" @click="open">
          {{ pageData.actionLabel || '发起合作咨询' }} →
        </button>
      </div>
    </PageHero>

    <section v-if="content.contentHtml" class="section">
      <article class="container article cms-rich-text" v-html="content.contentHtml" />
    </section>

    <section v-if="modes.length" class="section">
      <div class="container">
        <div class="center-heading">
          <p class="eyebrow">{{ pageData.sectionEyebrow || '合作模式' }}</p>
          <h2>{{ pageData.sectionTitle || '选择适合双方资源的合作方式' }}</h2>
        </div>
        <div class="info-grid">
          <article v-for="mode in modes" :key="mode.path" class="info-card">
            <span>{{ mode.index }}</span>
            <h3>{{ mode.title }}</h3>
            <p>{{ mode.text }}</p>
            <NuxtLink class="button button-outline" :to="mode.path">
              了解合作 ↗
            </NuxtLink>
          </article>
        </div>
      </div>
    </section>

    <section v-if="process.length" class="section section-muted">
      <div class="container">
        <div class="center-heading">
          <p class="eyebrow">{{ pageData.processEyebrow || '合作流程' }}</p>
          <h2>{{ pageData.processTitle || '让合作预期清晰可见' }}</h2>
        </div>
        <div class="process">
          <article v-for="step in process" :key="step.title">
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section v-if="faqs.length" class="section">
      <div class="container">
        <div class="section-heading">
          <div>
            <p class="eyebrow">{{ pageData.faqEyebrow || '常见问题' }}</p>
            <h2>{{ pageData.faqTitle || '合作前，您可能关心' }}</h2>
          </div>
        </div>
        <div class="accordion">
          <details v-for="faq in faqs" :key="faq.title">
            <summary>{{ faq.title }}</summary>
            <p>{{ faq.description }}</p>
          </details>
        </div>
      </div>
    </section>
  </div>
</template>
