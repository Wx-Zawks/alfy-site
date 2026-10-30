export type SeoCrawlerPolicy = 'allow' | 'disallow'

export interface ApiSeoGlobalSetting {
  baiduAnalyticsEnabled: boolean
  baiduAnalyticsId?: null | string
  baiduSiteVerification?: null | string
  bingSiteVerification?: null | string
  canonicalOrigin: string
  clarityEnabled: boolean
  clarityProjectId?: null | string
  defaultDescription: string
  defaultOgImageUrl?: null | string
  defaultOgMediaId?: null | number
  defaultTitle: string
  foundingDate?: null | string
  ga4Enabled: boolean
  ga4MeasurementId?: null | string
  googleExtendedPolicy: SeoCrawlerPolicy
  googleSiteVerification?: null | string
  gptbotPolicy: SeoCrawlerPolicy
  oaiSearchbotPolicy: SeoCrawlerPolicy
  organizationAlternateName?: null | string
  organizationDescription?: null | string
  organizationLegalName?: null | string
  organizationName: string
  publicIndexEnabled: boolean
  sameAs: string[]
  titleTemplate: string
  updatedAt?: null | string
}

export interface ApiSeoRouteSetting {
  displayName: string
  indexEnabled: boolean
  ogImageUrl?: null | string
  ogMediaId?: null | number
  routeKey: string
  routePath: string
  seoDescription?: null | string
  seoKeywords?: null | string
  seoTitle?: null | string
  updatedAt?: null | string
}

export interface ApiSeoUrl {
  lastModified?: null | string
  loc: string
  type: string
}

export interface ApiRedirectResolve {
  statusCode: 301 | 308
  targetUrl: string
}

export type JsonLdValue = Record<string, unknown> | Array<Record<string, unknown>>
