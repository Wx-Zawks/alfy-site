import { requestClient } from '#/api/request';

export type SeoCrawlerPolicy = 'allow' | 'disallow';

export interface SeoGlobalSettingRecord {
  baiduAnalyticsEnabled: boolean;
  baiduAnalyticsId?: null | string;
  baiduSiteVerification?: null | string;
  bingSiteVerification?: null | string;
  canonicalOrigin: string;
  clarityEnabled: boolean;
  clarityProjectId?: null | string;
  defaultDescription: string;
  defaultOgImageUrl?: null | string;
  defaultOgMediaId?: null | number;
  defaultTitle: string;
  foundingDate?: null | string;
  ga4Enabled: boolean;
  ga4MeasurementId?: null | string;
  googleExtendedPolicy: SeoCrawlerPolicy;
  googleSiteVerification?: null | string;
  gptbotPolicy: SeoCrawlerPolicy;
  id?: null | number;
  oaiSearchbotPolicy: SeoCrawlerPolicy;
  organizationAlternateName?: null | string;
  organizationDescription?: null | string;
  organizationLegalName?: null | string;
  organizationName: string;
  publicIndexEnabled: boolean;
  sameAs: string[];
  titleTemplate: string;
  updatedAt?: null | string;
  version?: null | number;
}

export interface SeoRouteSettingRecord {
  displayName: string;
  id: number;
  indexEnabled: boolean;
  ogImageUrl?: null | string;
  ogMediaId?: null | number;
  routeKey: string;
  routePath: string;
  seoDescription?: null | string;
  seoKeywords?: null | string;
  seoTitle?: null | string;
  updatedAt?: null | string;
  version: number;
}

export function getSeoGlobalSettings() {
  return requestClient.get<SeoGlobalSettingRecord>('/admin/seo/settings');
}

export function saveSeoGlobalSettings(
  payload: Partial<SeoGlobalSettingRecord>,
) {
  return requestClient.put<SeoGlobalSettingRecord>(
    '/admin/seo/settings',
    payload,
  );
}

export function listSeoRouteSettings() {
  return requestClient.get<SeoRouteSettingRecord[]>('/admin/seo/routes');
}

export function saveSeoRouteSetting(
  routeKey: string,
  payload: Partial<SeoRouteSettingRecord>,
) {
  return requestClient.put<SeoRouteSettingRecord>(
    `/admin/seo/routes/${routeKey}`,
    payload,
  );
}
