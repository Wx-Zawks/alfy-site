import type { ApiRelatedProduct, ApiSeoMeta } from './api'

export interface Product {
  id: number | string
  name: string
  slug: string
  category: string
  categoryName: string
  summary: string
  image: string
  features: string[]
  contentHtml?: string
  seo?: ApiSeoMeta | null
  specifications?: unknown
}

export interface ApplicationScene {
  id?: number | string
  key: string
  name: string
  slogan: string
  summary: string
  image: string
}

export interface CaseProject {
  id: number | string
  slug: string
  title: string
  category: string
  summary: string
  image: string
  location?: string
  background?: string
  customerNeed?: string
  solution?: string
  implementation?: string
  resultSummary?: string
  contentHtml?: string
  relatedProducts?: ApiRelatedProduct[]
  seo?: ApiSeoMeta | null
}

export interface Article {
  id: number | string
  slug: string
  title: string
  category: string
  categoryName: string
  date: string
  summary: string
  image: string
  videoUrl?: string
  contentHtml?: string
  sourceUrl?: string
}
