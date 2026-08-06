<script setup lang="ts">
import { useApiClient } from '~/composables/useApi'
import { cooperationPageDefinitions, useCooperationPages } from '~/composables/useCooperationPages'

const route = useRoute()
const slug = String(route.params.slug)
const definition = cooperationPageDefinitions.find(item => item.slug === slug)

if (!definition) {
  throw createError({ statusCode: 404, statusMessage: '合作页面不存在' })
}

const { entries, navItems } = await useCooperationPages()
const content = computed(() =>
  entries.value.find(entry => entry.definition.key === definition.key)?.content ?? null
)

if (!content.value) {
  throw createError({ statusCode: 404, statusMessage: '合作页面不存在或尚未发布' })
}

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
      :image="resolveMediaUrl(content.coverImageUrl, definition.fallbackImage)"
    >
      <div class="button-row">
        <button class="button button-primary" type="button" @click="open">
          {{ pageData.actionLabel || '咨询合作' }} →
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
