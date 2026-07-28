<script setup lang="ts">
import type { ApiContentPage } from '~/types/api'
import { useApiClient } from '~/composables/useApi'

const pageKeys: Record<string, string> = {
  dealer: 'cooperation-dealer',
  'product-development': 'cooperation-product-development',
  'industry-chain': 'cooperation-industry-chain'
}
const fallbackImages: Record<string, string> = {
  dealer: '/images/launch-1.jpg',
  'product-development': '/images/aerogel-powder.jpg',
  'industry-chain': '/images/research-institute.jpg'
}

const route = useRoute()
const slug = String(route.params.slug)
const pageKey = pageKeys[slug]

if (!pageKey) {
  throw createError({ statusCode: 404, statusMessage: '合作页面不存在' })
}

const { data: content } = await useApi<ApiContentPage>(
  `public-page-${pageKey}`,
  `/public/pages/${pageKey}`
)
const { resolveMediaUrl } = useApiClient()
const { open } = useInquiryDialog()
const pageData = computed(() => content.value?.contentData ?? {})

useSeoMeta({
  title: () => content.value?.seoTitle || content.value?.title || '合作模式',
  description: () => content.value?.seoDescription || content.value?.summary || '',
  keywords: () => content.value?.seoKeywords || ''
})
</script>

<template>
  <div v-if="content" class="brief-page cooperation-detail-page">
    <PageHero
      class="brief-hero"
      :eyebrow="pageData.eyebrow || content.category || '合作模式'"
      :title="content.title"
      :highlight="pageData.highlightText || ''"
      :description="content.summary || ''"
      :image="resolveMediaUrl(content.coverImageUrl, fallbackImages[slug])"
    >
      <div class="button-row">
        <button class="button button-primary" type="button" @click="open">
          {{ pageData.actionLabel || '咨询合作' }} →
        </button>
      </div>
    </PageHero>

    <nav class="technology-subnav" aria-label="合作模式">
      <div class="container">
        <NuxtLink to="/cooperation">合作总览</NuxtLink>
        <NuxtLink to="/cooperation/dealer">经销商合作</NuxtLink>
        <NuxtLink to="/cooperation/product-development">复合产品开发</NuxtLink>
        <NuxtLink to="/cooperation/industry-chain">产业链合作</NuxtLink>
      </div>
    </nav>

    <section class="brief-section">
      <article
        v-if="content.contentHtml"
        class="container article cms-rich-text"
        v-html="content.contentHtml"
      />
      <article v-else class="container article">
        <p>{{ content.summary }}</p>
      </article>
    </section>

    <section class="technology-detail-cta">
      <div class="container">
        <div>
          <p class="eyebrow eyebrow-light">合作咨询</p>
          <h2>围绕双方资源，共同明确合作路径</h2>
        </div>
        <button class="button button-light" type="button" @click="open">
          {{ pageData.actionLabel || '发起合作咨询' }} →
        </button>
      </div>
    </section>
  </div>
</template>
