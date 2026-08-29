import type {
  ApiApplicationScene,
  ApiArticleDetail,
  ApiArticleListItem,
  ApiCaseDetail,
  ApiCaseListItem,
  ApiProductDetail,
  ApiProductListItem
} from '~/types/api'
import type { ApplicationScene, Article, CaseProject, Product } from '~/types/content'

type MediaResolver = (value?: null | string, fallback?: string) => string

const MANAGED_RICH_TEXT_MEDIA = /(\bsrc\s*=\s*["'])(\/api\/v1\/public\/media\/\d+|alfy-media:(\d+))(["'])/gi

/**
 * 富文本由 v-html 直接渲染，不能经过 Vue 的 :src 绑定。将正文中的媒体地址
 * 转为 API 服务的完整地址，避免开发环境错误地请求 Nuxt 的 localhost:3000。
 */
export function resolveRichTextHtml(value: null | string | undefined, resolveMedia: MediaResolver) {
  if (!value) return ''
  return value.replaceAll(MANAGED_RICH_TEXT_MEDIA, (_match, prefix: string, source: string, mediaId: string | undefined, suffix: string) => {
    const mediaUrl = mediaId ? `/api/v1/public/media/${mediaId}` : source
    return `${prefix}${resolveMedia(mediaUrl)}${suffix}`
  })
}

export function formatContentDate(value?: null | string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.slice(0, 10)
  return new Intl.DateTimeFormat('zh-CN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
    .format(date)
    .replaceAll('/', '-')
}

export function mapProduct(
  item: ApiProductDetail | ApiProductListItem,
  resolveMedia: MediaResolver
): Product {
  return {
    category: item.category,
    categoryName: item.categoryName,
    contentHtml: 'contentHtml' in item ? resolveRichTextHtml(item.contentHtml, resolveMedia) : '',
    features: item.features || [],
    id: item.id,
    image: resolveMedia(item.coverImageUrl, '/images/aerogel-powder.jpg'),
    name: item.name,
    seo: 'seo' in item ? item.seo : null,
    slug: item.slug,
    specifications: 'specifications' in item ? item.specifications : undefined,
    summary: item.summary || ''
  }
}

export function mapScene(item: ApiApplicationScene, resolveMedia: MediaResolver): ApplicationScene {
  return {
    id: item.id,
    image: resolveMedia(item.coverImageUrl, '/images/news-3.jpeg'),
    key: item.key,
    name: item.name,
    slogan: item.slogan || item.name,
    summary: item.summary || ''
  }
}

export function mapCase(
  item: ApiCaseDetail | ApiCaseListItem,
  resolveMedia: MediaResolver
): CaseProject {
  return {
    background: 'background' in item ? item.background || '' : '',
    category: item.category || '应用案例',
    contentHtml: 'contentHtml' in item ? resolveRichTextHtml(item.contentHtml, resolveMedia) : '',
    customerNeed: 'customerNeed' in item ? item.customerNeed || '' : '',
    id: item.id,
    image: resolveMedia(item.coverImageUrl, '/images/news-3.jpeg'),
    implementation: 'implementation' in item ? item.implementation || '' : '',
    location: item.location || '',
    relatedProducts: 'relatedProducts' in item ? item.relatedProducts : [],
    resultSummary: 'resultSummary' in item ? item.resultSummary || '' : '',
    seo: 'seo' in item ? item.seo : null,
    slug: item.slug,
    solution: 'solution' in item ? item.solution || '' : '',
    summary: item.summary || '',
    title: item.title
  }
}

export function mapArticle(
  item: ApiArticleDetail | ApiArticleListItem,
  resolveMedia: MediaResolver
): Article {
  const category = item.categories[0]
  const media = 'media' in item ? item.media : []
  const coverMedia = media.find(entry => entry.usageType === 'COVER') || media.find(entry => entry.mediaType === 'IMAGE')
  const videoMedia = media.find(entry => entry.mediaType === 'VIDEO')
  const coverUrl = 'coverUrl' in item ? item.coverUrl : coverMedia?.url
  return {
    category: category?.code || 'all',
    categoryName: category?.name || '新闻资讯',
    contentHtml: 'contentHtml' in item ? resolveRichTextHtml(item.contentHtml, resolveMedia) : '',
    date: formatContentDate(item.sourcePublishedAt || item.publishedAt),
    id: item.id,
    image: resolveMedia(coverUrl, '/images/launch-2.jpg'),
    slug: item.slug,
    sourceUrl: 'sourceUrl' in item ? item.sourceUrl || '' : '',
    summary: item.summary || '',
    title: item.title,
    videoUrl: resolveMedia(item.videoUrl || videoMedia?.url, '') || undefined
  }
}

export function useContentMapper() {
  return { formatContentDate, mapArticle, mapCase, mapProduct, mapScene }
}
