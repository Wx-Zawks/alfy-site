<script setup lang="ts">
import type { ApiTechnologyPage } from '~/types/api'
import { useApiClient } from '~/composables/useApi'

interface TechnologyBlock {
  description: string
  imageUrl: string
  title: string
}

const route = useRoute()
const slug = String(route.params.slug)
const allowedSlugs = ['aerogel-material', 'aerogel-composite', 'other']

if (!allowedSlugs.includes(slug)) {
  throw createError({ statusCode: 404, statusMessage: '技术页面不存在' })
}

const { data: page } = await useApi<ApiTechnologyPage>(
  `public-technology-${slug}`,
  `/public/technologies/${slug}`
)

if (!page.value) {
  throw createError({ statusCode: 404, statusMessage: '技术页面不存在或尚未发布' })
}

const { resolveMediaUrl } = useApiClient()
const pageDefaults: Record<string, { image: string; label: string }> = {
  'aerogel-material': { image: '/images/aerogel-block.jpg', label: '气凝胶材料技术' },
  'aerogel-composite': { image: '/images/dispersion.png', label: '气凝胶复合产品技术' },
  other: { image: '/images/research-institute.jpg', label: '其他技术' }
}
function normalizeBlocks(value: unknown): TechnologyBlock[] {
  if (!Array.isArray(value)) return []
  return value
    .filter(entry => entry && typeof entry === 'object')
    .map((entry) => {
      const record = entry as Record<string, unknown>
      const mediaId = Number(record.imageMediaId)
      const configuredImage = typeof record.imageUrl === 'string' && record.imageUrl.trim()
        ? record.imageUrl
        : Number.isSafeInteger(mediaId) && mediaId > 0
          ? `/api/v1/public/media/${mediaId}`
          : ''
      return {
        title: String(record.title || record.name || ''),
        description: String(record.description || record.text || record.value || ''),
        imageUrl: configuredImage ? resolveMediaUrl(configuredImage) : ''
      }
    })
    .filter(item => item.title || item.description)
}

const capabilityRows = computed(() => normalizeBlocks(page.value?.capabilityRows))
const pillars = computed(() => normalizeBlocks(page.value?.pillars))
const fallback = pageDefaults[slug]!
const heroImage = computed(() => resolveMediaUrl(page.value?.heroImageUrl, fallback.image))

function documentTitle(value: string | null | undefined, fallbackTitle: string) {
  return (value || fallbackTitle).replace(/\s*(?:｜|\|)\s*奥飞新材$/, '')
}

useSeoMeta({
  title: () => documentTitle(page.value?.seoTitle || page.value?.title, fallback.label),
  description: () => page.value?.seoDescription || page.value?.summary || '',
  keywords: () => page.value?.seoKeywords || '奥飞新材,气凝胶'
})
</script>

<template>
  <div v-if="page" class="technology-detail-page">
    <PageHero
      :eyebrow="page.eyebrow || fallback.label"
      :title="page.title"
      :highlight="page.highlightText || ''"
      :description="page.summary || ''"
      :image="heroImage"
    />

    <nav class="technology-subnav" aria-label="技术栏目">
      <div class="container">
        <NuxtLink to="/technology">技术总览</NuxtLink>
        <NuxtLink to="/technology/aerogel-material">气凝胶材料技术</NuxtLink>
        <NuxtLink to="/technology/aerogel-composite">气凝胶复合产品技术</NuxtLink>
        <NuxtLink to="/technology/other">其他技术</NuxtLink>
      </div>
    </nav>

    <section v-if="capabilityRows.length" class="brief-section">
      <div class="container">
        <header class="brief-section-heading wide-heading">
          <p class="eyebrow">核心内容</p>
          <h2>从技术原理到产业化能力</h2>
          <p>{{ page.summary }}</p>
        </header>
        <div class="technology-detail-grid">
          <article v-for="(block, index) in capabilityRows" :key="`${block.title}-${index}`">
            <img v-if="block.imageUrl" :src="block.imageUrl" :alt="block.title">
            <div>
              <h3>{{ block.title }}</h3>
              <p>{{ block.description }}</p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section v-if="page.contentHtml" class="brief-section technology-rich-section">
      <article class="container article cms-rich-text" v-html="page.contentHtml" />
    </section>

    <section v-if="pillars.length" class="brief-section technology-pillar-section">
      <div class="container">
        <header class="brief-section-heading">
          <p class="eyebrow">技术亮点与应用方向</p>
          <h2>让技术价值进入真实场景</h2>
        </header>
        <div class="technology-pillar-grid">
          <article v-for="(block, index) in pillars" :key="`${block.title}-${index}`">
            <span>{{ String(index + 1).padStart(2, '0') }}</span>
            <h3>{{ block.title }}</h3>
            <p>{{ block.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="technology-detail-cta">
      <div class="container">
        <div>
          <p class="eyebrow eyebrow-light">技术合作</p>
          <h2>围绕您的场景，共同定义验证路径</h2>
        </div>
        <NuxtLink
          class="button button-light"
          :to="page.action?.target || '/cooperation'"
        >
          {{ page.action?.label || '联系技术团队' }} →
        </NuxtLink>
      </div>
    </section>
  </div>
</template>
