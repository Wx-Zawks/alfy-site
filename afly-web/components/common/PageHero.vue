<script setup lang="ts">
import type { ApiPageHero } from '~/types/api'
import { useApi, useApiClient } from '~/composables/useApi'

const props = withDefaults(defineProps<{
  description?: string
  eyebrow?: string
  highlight?: string
  image?: string
  pageKey?: string
  title?: string
}>(), {
  description: '',
  eyebrow: '',
  highlight: '',
  image: '',
  pageKey: '',
  title: ''
})

const { resolveMediaUrl } = useApiClient()
const heroRequest = props.pageKey
  ? await useApi<ApiPageHero>(`page-hero-${props.pageKey}`, `/public/pages/${props.pageKey}/hero`, { optional: true })
  : null

const hero = computed(() => heroRequest?.data.value ?? null)
// Props are offline/API-error fallbacks only. Once CMS data exists, nullable fields
// must stay empty so content deleted in the admin does not reappear from defaults.
const eyebrow = computed(() => hero.value ? (hero.value.eyebrow ?? '') : props.eyebrow)
const title = computed(() => hero.value ? hero.value.title : props.title)
const highlight = computed(() =>
  hero.value ? (hero.value.highlightText ?? '') : props.highlight)
const description = computed(() =>
  hero.value ? (hero.value.summary ?? '') : props.description)
const image = computed(() => resolveMediaUrl(hero.value?.backgroundImageUrl, props.image))
const mobileImage = computed(() => resolveMediaUrl(hero.value?.mobileBackgroundImageUrl, image.value))
const actions = computed(() => [hero.value?.primaryAction, hero.value?.secondaryAction]
  .filter(action => action?.label && action?.target))
</script>

<template>
  <section
    class="page-hero"
    :style="{
      '--page-image': image ? `url(${image})` : 'none',
      '--page-image-mobile': mobileImage ? `url(${mobileImage})` : 'none'
    }"
  >
    <div class="container page-hero-content">
      <p v-if="eyebrow" class="eyebrow eyebrow-light">{{ eyebrow }}</p>
      <h1>{{ title }}<br><em v-if="highlight">{{ highlight }}</em></h1>
      <p v-if="description">{{ description }}</p>
      <div v-if="actions.length" class="button-row">
        <NuxtLink
          v-for="(action, index) in actions"
          :key="`${action?.target}-${index}`"
          :class="['button', index === 0 ? 'button-primary' : 'button-light']"
          :to="action?.target || '/'"
        >
          {{ action?.label }} <span>→</span>
        </NuxtLink>
      </div>
      <slot />
    </div>
  </section>
</template>
