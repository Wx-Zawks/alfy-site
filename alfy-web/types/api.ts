export interface PageResult<T> {
  page: number
  records: T[]
  size: number
  total: number
}

export interface ApiAction {
  label?: null | string
  target?: null | string
}

export interface ApiHeroSlide {
  desktopImageUrl?: null | string
  eyebrow?: null | string
  highlightText?: null | string
  id: number
  mobileImageUrl?: null | string
  primaryAction?: ApiAction | null
  secondaryAction?: ApiAction | null
  sortOrder?: null | number
  summary?: null | string
  title: string
}

export interface ApiApplicationScene {
  coverImageUrl?: null | string
  id: number
  key: string
  name: string
  slogan?: null | string
  summary?: null | string
}

export interface ApiCaseListItem {
  coverImageUrl?: null | string
  featured: boolean
  id: number
  location?: null | string
  scene?: null | string
  sceneSlug?: null | string
  slug: string
  sortOrder?: null | number
  summary?: null | string
  title: string
}

export interface ApiRelatedProduct {
  id: number
  name: string
  slug: string
}

export interface ApiSeoMeta {
  description?: null | string
  keywords?: null | string
  title?: null | string
}

export interface ApiCaseDetail extends ApiCaseListItem {
  background?: null | string
  contentHtml?: null | string
  customerNeed?: null | string
  implementation?: null | string
  relatedProducts: ApiRelatedProduct[]
  resultSummary?: null | string
  seo?: ApiSeoMeta | null
  solution?: null | string
}

export interface ApiProductCategory {
  coverImageUrl?: null | string
  id: number
  name: string
  slug: string
  sortOrder?: null | number
  summary?: null | string
}

export interface ApiProductListItem {
  category: string
  categoryName: string
  coverImageUrl?: null | string
  features?: string[] | null
  id: number
  name: string
  slug: string
  summary?: null | string
}

export interface ApiProductDetail extends ApiProductListItem {
  contentHtml?: null | string
  seo?: ApiSeoMeta | null
  specifications?: unknown
}

export interface ApiArticleCategory {
  children: ApiArticleCategory[]
  code: string
  id: number
  name: string
  sortOrder: number
}

export interface ApiArticleListItem {
  categories: ApiArticleCategory[]
  coverUrl?: null | string
  homeSlot?: null | 'NEWS_LIST' | 'NEWS_PRIMARY' | 'NEWS_SECONDARY'
  id: number
  publishedAt?: null | string
  slug: string
  sourcePublishedAt?: null | string
  summary?: null | string
  title: string
}

export interface ApiArticleMedia {
  caption?: null | string
  id: number
  mediaType: string
  mimeType: string
  originalFilename: string
  sortOrder: number
  url: string
  usageType?: null | string
}

export interface ApiArticleDetail extends ApiArticleListItem {
  contentHtml?: null | string
  media: ApiArticleMedia[]
  seo?: ApiSeoMeta | null
  sourceUrl?: null | string
}

export interface ApiTechnologyPage {
  action?: ApiAction | null
  capabilityRows?: unknown
  contentHtml?: null | string
  eyebrow?: null | string
  heroImageUrl?: null | string
  heroMediaId?: null | number
  highlightText?: null | string
  id: number
  pageKey: string
  pillars?: unknown
  publishedAt?: null | string
  seoDescription?: null | string
  seoKeywords?: null | string
  seoTitle?: null | string
  sortOrder?: null | number
  status: string
  summary?: null | string
  title: string
  updatedAt?: null | string
  version?: number
}

export interface ApiHome {
  applicationScenes: ApiApplicationScene[]
  banners: ApiHeroSlide[]
  featuredArticles: ApiArticleListItem[]
  featuredCases: ApiCaseListItem[]
  technologyOverview?: ApiTechnologyPage | null
}

export interface ApiHomeSection {
  buttonLabel?: null | string
  buttonTarget?: null | string
  description?: null | string
  enabled: boolean
  eyebrow?: null | string
  highlightText?: null | string
  id: number
  imageUrl?: null | string
  label: string
  mobileImageUrl?: null | string
  sectionKey: string
  sortOrder?: null | number
  title: string
}

export interface ApiSiteSetting {
  address?: null | string
  companyName?: null | string
  copyrightText?: null | string
  icpNumber?: null | string
  logoUrl?: null | string
  privacyPolicyUrl?: null | string
  serviceEmail?: null | string
  servicePhone?: null | string
  wechatQrImageUrl?: null | string
}

export interface ApiNavigationItem {
  children: ApiNavigationItem[]
  enabled: boolean
  highlighted: boolean
  id: number
  label: string
  sortOrder: number
  target: string
}

export interface ApiNavigation {
  footer: ApiNavigationItem[]
  header: ApiNavigationItem[]
}

export interface ApiPageHero {
  backgroundImageUrl?: null | string
  eyebrow?: null | string
  highlightText?: null | string
  id: number
  mobileBackgroundImageUrl?: null | string
  pageKey: string
  primaryAction?: ApiAction | null
  secondaryAction?: ApiAction | null
  summary?: null | string
  title: string
}

export interface ApiPartner {
  category?: null | string
  featured: boolean
  id: number
  logoUrl?: null | string
  name: string
  slug: string
  sortOrder?: null | number
  summary?: null | string
  websiteUrl?: null | string
}

export interface ApiContentPage {
  category?: null | string
  contentData?: null | ApiCooperationContentData
  contentHtml?: null | string
  coverImageUrl?: null | string
  pageKey: string
  seoDescription?: null | string
  seoKeywords?: null | string
  seoTitle?: null | string
  summary?: null | string
  title: string
}

export interface ApiContentBlock {
  description: string
  title: string
}

export interface ApiCooperationContentData {
  actionLabel?: null | string
  eyebrow?: null | string
  faqEyebrow?: null | string
  faqTitle?: null | string
  faqs?: null | ApiContentBlock[]
  highlightText?: null | string
  process?: null | ApiContentBlock[]
  processEyebrow?: null | string
  processTitle?: null | string
  sectionEyebrow?: null | string
  sectionTitle?: null | string
}

export interface ApiInquiryRequest {
  company?: string
  email?: string
  inquiryType?: string
  message: string
  name: string
  phone?: string
  privacyAccepted: boolean
  productId?: number
  sourceUrl?: string
  utm?: Record<string, string>
}

export interface ApiInquiryResult {
  inquiryNo: string
}
