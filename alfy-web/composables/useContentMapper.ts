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
    contentHtml: 'contentHtml' in item ? item.contentHtml || '' : '',
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
    category: item.scene || '应用案例',
    contentHtml: 'contentHtml' in item ? item.contentHtml || '' : '',
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
  const coverUrl = 'coverUrl' in item ? item.coverUrl : coverMedia?.url
  return {
    category: category?.code || 'all',
    categoryName: category?.name || '新闻资讯',
    contentHtml: 'contentHtml' in item ? item.contentHtml || '' : '',
    date: formatContentDate(item.sourcePublishedAt || item.publishedAt),
    id: item.id,
    image: resolveMedia(coverUrl, '/images/launch-2.jpg'),
    slug: item.slug,
    sourceUrl: 'sourceUrl' in item ? item.sourceUrl || '' : '',
    summary: item.summary || '',
    title: item.title
  }
}

export function useContentMapper() {
  return { formatContentDate, mapArticle, mapCase, mapProduct, mapScene }
}
