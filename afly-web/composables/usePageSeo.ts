import type { MaybeRefOrGetter } from 'vue'
import * as VueRuntime from 'vue'
import type { JsonLdValue } from '~/types/seo'
import { absoluteSeoUrl, buildCanonical } from '~/utils/seo-url'
import { safeJsonLd } from '~/utils/json-ld'

// Vue 编译器会使用该运行时辅助函数，但当前公开类型声明未导出它。
const withAsyncContext = (VueRuntime as unknown as {
  withAsyncContext<T>(getAwaitable: () => T): [T, () => void]
}).withAsyncContext

export interface PageSeoInput {
  description?: MaybeRefOrGetter<null | string | undefined>
  image?: MaybeRefOrGetter<null | string | undefined>
  index?: MaybeRefOrGetter<boolean | undefined>
  jsonLd?: MaybeRefOrGetter<JsonLdValue | null | undefined>
  keywords?: MaybeRefOrGetter<null | string | undefined>
  modifiedTime?: MaybeRefOrGetter<null | string | undefined>
  path?: MaybeRefOrGetter<null | string | undefined>
  publishedTime?: MaybeRefOrGetter<null | string | undefined>
  title?: MaybeRefOrGetter<null | string | undefined>
  type?: 'article' | 'product' | 'website'
}

export async function usePageSeo(input: PageSeoInput = {}) {
  const route = useRoute()
  // 该 composable 内部需要等待接口数据；显式恢复组件上下文，确保后续 head 条目
  // 归属于当前页面并在路由切换时被正确替换/释放。
  const [globalSeoRequest, restoreContext] = withAsyncContext(() => useGlobalSeo())
  const { routeByPath, settings } = await globalSeoRequest
  restoreContext()
  const path = computed(() => toValue(input.path) || route.path)
  const fixed = computed(() => routeByPath(path.value))
  const title = computed(() => fixed.value?.seoTitle || toValue(input.title)?.trim() || settings.value.defaultTitle)
  const description = computed(() => fixed.value?.seoDescription || toValue(input.description)?.trim() || settings.value.defaultDescription)
  const keywords = computed(() => fixed.value?.seoKeywords || toValue(input.keywords)?.trim() || '')
  const canonical = computed(() => buildCanonical(settings.value.canonicalOrigin, path.value))
  const image = computed(() => absoluteSeoUrl(
    settings.value.canonicalOrigin,
    fixed.value?.ogImageUrl || toValue(input.image) || settings.value.defaultOgImageUrl
  ))
  const indexable = computed(() => settings.value.publicIndexEnabled
    && fixed.value?.indexEnabled !== false
    && toValue(input.index) !== false)
  const robots = computed(() => indexable.value
    ? 'index,follow,max-image-preview:large,max-snippet:-1,max-video-preview:-1'
    : 'noindex,nofollow')

  useSeoMeta({
    title,
    description,
    keywords,
    robots,
    ogTitle: title,
    ogDescription: description,
    ogUrl: canonical,
    ogImage: image,
    // Open Graph 标准没有 product 类型；产品页使用 website，并由 Product JSON-LD 表达实体类型。
    ogType: input.type === 'article' ? 'article' : 'website',
    articlePublishedTime: () => toValue(input.publishedTime) || undefined,
    articleModifiedTime: () => toValue(input.modifiedTime) || undefined,
    twitterCard: 'summary_large_image',
    twitterTitle: title,
    twitterDescription: description,
    twitterImage: image
  })

  useHead(() => {
    const jsonLd = toValue(input.jsonLd)
    const entries = jsonLd ? (Array.isArray(jsonLd) ? jsonLd : [jsonLd]) : []
    return {
      titleTemplate: settings.value.titleTemplate,
      link: [{ key: 'canonical', rel: 'canonical', href: canonical.value }],
      script: entries.map((entry, index) => ({
        key: `page-json-ld-${index}`,
        type: 'application/ld+json',
        innerHTML: safeJsonLd(entry)
      }))
    }
  })

  return { canonical, description, image, indexable, settings, title }
}
