<script setup lang="ts">
import { useApiClient } from '~/composables/useApi'
import { useCooperationPages } from '~/composables/useCooperationPages'

const { entries, navItems } = await useCooperationPages()
const content = computed(() =>
  entries.value.find(entry => entry.definition.key === 'cooperation')?.content ?? null
)

if (!content.value) {
  throw createError({ statusCode: 404, statusMessage: '合作总览尚未发布' })
}

const { open } = useInquiryDialog()
const { resolveMediaUrl } = useApiClient()
const pageData = computed(() => content.value?.contentData ?? {})

useSeoMeta({
  title: () => content.value?.seoTitle || content.value?.title || '商务合作',
  description: () => content.value?.seoDescription || content.value?.summary || '奥飞新材商务合作介绍。',
  keywords: () => content.value?.seoKeywords || ''
})
</script>

<template>
  <div v-if="content" class="brief-page cooperation-detail-page">
    <PageHero
      class="brief-hero"
      :eyebrow="pageData.eyebrow || 'OPEN COLLABORATION'"
      :title="content.title"
      :highlight="pageData.highlightText || ''"
      :description="content.summary || ''"
      :image="resolveMediaUrl(content.coverImageUrl, '/images/news-1.jpeg')"
    >
      <div class="button-row">
        <button class="button button-primary" type="button" @click="open">
          {{ pageData.actionLabel || '发起合作咨询' }} →
        </button>
      </div>
    </PageHero>

    <CooperationSubnav :items="navItems" />

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
