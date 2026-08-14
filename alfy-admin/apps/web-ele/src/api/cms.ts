import type { ContentResource } from '#/data/cms';

import {
  getApiErrorCode,
  rawRequestClient,
  requestClient,
  showApiErrorMessage,
} from '#/api/request';

export interface PageResult<T> {
  page: number;
  records: T[];
  size: number;
  total: number;
}

export interface BackendContentRecord {
  [key: string]: any;
  id: number;
  status?: string;
  version?: number;
}

export interface ProductCategoryRecord {
  coverMediaId?: null | number;
  enabled: boolean;
  id: number;
  name: string;
  slug: string;
  sortOrder: number;
  summary?: string;
  version: number;
}

export interface ArticleCategoryRecord {
  children: ArticleCategoryRecord[];
  code: string;
  id: number;
  name: string;
  sortOrder: number;
}

export interface MediaRecord {
  adminUrl: string;
  altText?: string;
  createdAt: string;
  fileSize: number;
  height?: null | number;
  id: number;
  mediaType: 'DOCUMENT' | 'IMAGE' | 'VIDEO';
  mimeType: string;
  originalFilename: string;
  width?: null | number;
}

export interface TechnologyBlockRecord {
  description?: string;
  imageMediaId?: null | number;
  imageUrl?: null | string;
  title: string;
}

export interface TechnologyPageRecord extends BackendContentRecord {
  action?: { label?: null | string; target?: null | string };
  capabilityRows?: TechnologyBlockRecord[];
  contentHtml?: null | string;
  eyebrow?: null | string;
  heroImageUrl?: null | string;
  heroMediaId?: null | number;
  highlightText?: null | string;
  pageKey: string;
  pillars?: TechnologyBlockRecord[];
  publishedAt?: null | string;
  seoDescription?: null | string;
  seoKeywords?: null | string;
  seoTitle?: null | string;
  sortOrder?: null | number;
  summary?: null | string;
  title: string;
  updatedAt?: null | string;
}

export interface ContentPageRecord extends BackendContentRecord {
  category?: null | string;
  contentData?: null | Record<string, unknown>;
  contentHtml?: null | string;
  coverImageUrl?: null | string;
  coverMediaId?: null | number;
  featured?: boolean;
  pageKey: string;
  publishedAt?: null | string;
  seoDescription?: null | string;
  seoKeywords?: null | string;
  seoTitle?: null | string;
  sortOrder?: null | number;
  summary?: null | string;
  title: string;
}

export interface InquiryRecord {
  adminNote?: string;
  company?: string;
  createdAt: string;
  email?: string;
  handledAt?: null | string;
  id: number;
  inquiryNo: string;
  inquiryType: string;
  message: string;
  name: string;
  phone: string;
  productId?: null | number;
  sourceUrl?: string;
  status: string;
  utm?: Record<string, unknown>;
}

export interface AdminUserRecord {
  createdAt: string;
  enabled: boolean;
  id: number;
  lastLoginAt?: null | string;
  role: 'content_admin' | 'super_admin';
  updatedAt: string;
  username: string;
}

export interface OperationLogRecord {
  action: string;
  createdAt: string;
  detail?: null | string;
  id: number;
  objectId?: null | number;
  objectType: string;
  operator: string;
  operatorId?: null | number;
}

export interface NotFoundLogRecord {
  count: number;
  firstSeen: string;
  id: number;
  lastSeen: string;
  path: string;
  referer?: null | string;
}

export interface HomeSectionRecord {
  buttonLabel?: string;
  buttonTarget?: string;
  description?: string;
  enabled: boolean;
  eyebrow?: string;
  highlightText?: string;
  id: number;
  imageMediaId?: null | number;
  imageUrl?: string;
  label: string;
  mobileImageUrl?: string;
  mobileMediaId?: null | number;
  sectionKey: string;
  sortOrder: number;
  title: string;
  updatedAt: string;
  version: number;
}

export interface RedirectRecord {
  enabled: boolean;
  id: number;
  sourcePath: string;
  targetUrl: string;
  updatedAt: string;
  version: number;
}

export interface SiteSettingRecord {
  address?: string;
  companyName?: string;
  copyrightText?: string;
  icpNumber?: string;
  id?: number;
  logoUrl?: string;
  privacyPolicyUrl?: string;
  serviceEmail?: string;
  servicePhone?: string;
  version?: number;
  wechatQrImageUrl?: string;
}

export interface NavigationRecord {
  children: NavigationRecord[];
  enabled: boolean;
  highlighted: boolean;
  id: number;
  label: string;
  sortOrder: number;
  target: string;
  version: number;
}

export interface PageHeroRecord extends BackendContentRecord {
  backgroundImageUrl?: string;
  eyebrow?: string;
  highlightText?: string;
  mobileBackgroundImageUrl?: string;
  pageKey: string;
  primaryAction?: { label?: string; target?: string };
  secondaryAction?: { label?: string; target?: string };
  summary?: string;
  title: string;
}

const endpointByResource: Record<
  Exclude<ContentResource, 'technologies'>,
  string
> = {
  articles: 'articles',
  banners: 'banners',
  cases: 'cases',
  pages: 'pages',
  partners: 'partners',
  products: 'products',
  scenes: 'application-scenes',
};

export async function listContent(resource: ContentResource) {
  if (resource === 'technologies') {
    try {
      const response = await rawRequestClient.get<{
        code: number;
        data: BackendContentRecord;
        message: string;
      }>('/admin/technologies');
      const value = (
        response as unknown as {
          data: { code: number; data: BackendContentRecord; message: string };
        }
      ).data.data;
      return value?.id ? [value] : [];
    } catch (error) {
      const apiCode = getApiErrorCode(error);
      if (apiCode === 40_400) {
        await showApiErrorMessage(error, '技术页面尚未创建');
        return [];
      }
      await showApiErrorMessage(error, '技术页面加载失败，请稍后重试');
      throw error;
    }
  }
  const endpoint = endpointByResource[resource];
  if (resource === 'banners') {
    return requestClient.get<BackendContentRecord[]>(`/admin/${endpoint}`);
  }
  const result = await requestClient.get<PageResult<BackendContentRecord>>(
    `/admin/${endpoint}`,
    { params: { page: 1, size: 100 } },
  );
  return result.records;
}

export async function getContent(resource: ContentResource, id: number) {
  if (resource === 'technologies') {
    return requestClient.get<BackendContentRecord>('/admin/technologies');
  }
  return requestClient.get<BackendContentRecord>(
    `/admin/${endpointByResource[resource]}/${id}`,
  );
}

export async function saveContent(
  resource: ContentResource,
  id: null | number,
  payload: Record<string, unknown>,
) {
  if (resource === 'technologies') {
    return requestClient.put<BackendContentRecord>(
      '/admin/technologies',
      payload,
    );
  }
  const base = `/admin/${endpointByResource[resource]}`;
  return id
    ? requestClient.put<BackendContentRecord>(`${base}/${id}`, payload)
    : requestClient.post<BackendContentRecord>(base, payload);
}

export async function changeContentStatus(
  resource: ContentResource,
  id: number,
  status: 'offline' | 'published',
) {
  const action = status === 'published' ? 'publish' : 'offline';
  const url =
    resource === 'technologies'
      ? `/admin/technologies/${action}`
      : `/admin/${endpointByResource[resource]}/${id}/${action}`;
  return requestClient.post<BackendContentRecord>(url);
}

export async function deleteContent(resource: ContentResource, id: number) {
  if (resource === 'technologies') throw new Error('技术页不支持删除');
  return requestClient.delete(`/admin/${endpointByResource[resource]}/${id}`);
}

export function listTechnologyPages() {
  return requestClient.get<TechnologyPageRecord[]>('/admin/technologies/pages');
}

export function getTechnologyPage(pageKey: string) {
  return requestClient.get<TechnologyPageRecord>(
    `/admin/technologies/pages/${pageKey}`,
  );
}

export function saveTechnologyPage(
  pageKey: string,
  payload: Record<string, unknown>,
) {
  return requestClient.put<TechnologyPageRecord>(
    `/admin/technologies/pages/${pageKey}`,
    payload,
  );
}

export function changeTechnologyPageStatus(
  pageKey: string,
  status: 'offline' | 'published',
) {
  const action = status === 'published' ? 'publish' : 'offline';
  return requestClient.post<TechnologyPageRecord>(
    `/admin/technologies/pages/${pageKey}/${action}`,
  );
}

export async function listContentPages() {
  const result = await requestClient.get<PageResult<ContentPageRecord>>(
    '/admin/pages',
    { params: { page: 1, size: 100 } },
  );
  return result.records;
}

export function getContentPage(id: number) {
  return requestClient.get<ContentPageRecord>(`/admin/pages/${id}`);
}

export function saveContentPage(
  id: null | number,
  payload: Record<string, unknown>,
) {
  return id
    ? requestClient.put<ContentPageRecord>(`/admin/pages/${id}`, payload)
    : requestClient.post<ContentPageRecord>('/admin/pages', payload);
}

export function changeContentPageStatus(
  id: number,
  status: 'offline' | 'published',
) {
  return requestClient.post<ContentPageRecord>(
    `/admin/pages/${id}/${status === 'published' ? 'publish' : 'offline'}`,
  );
}

export async function listProductCategories() {
  const result = await requestClient.get<PageResult<ProductCategoryRecord>>(
    '/admin/product-categories',
    { params: { page: 1, size: 100 } },
  );
  return result.records;
}

export function saveProductCategory(
  id: null | number,
  payload: Record<string, unknown>,
) {
  return id
    ? requestClient.put<ProductCategoryRecord>(
        `/admin/product-categories/${id}`,
        payload,
      )
    : requestClient.post<ProductCategoryRecord>(
        '/admin/product-categories',
        payload,
      );
}

export function deleteProductCategory(id: number) {
  return requestClient.delete(`/admin/product-categories/${id}`);
}

export async function listArticleCategories() {
  return requestClient.get<ArticleCategoryRecord[]>(
    '/public/article-categories',
  );
}

export async function listMedia(keyword = '') {
  const result = await requestClient.get<PageResult<MediaRecord>>(
    '/admin/media',
    {
      params: { keyword: keyword || undefined, page: 1, size: 100 },
    },
  );
  return result.records;
}

export function uploadMedia(file: File, altText = '') {
  const formData = new FormData();
  formData.append('file', file);
  if (altText) formData.append('altText', altText);
  return requestClient.post<MediaRecord>('/admin/media', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function updateMedia(
  id: number,
  payload: {
    altText: string;
    file?: File;
    originalFilename: string;
  },
) {
  const formData = new FormData();
  formData.append('originalFilename', payload.originalFilename);
  formData.append('altText', payload.altText);
  if (payload.file) formData.append('file', payload.file);
  return requestClient.put<MediaRecord>(`/admin/media/${id}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function deleteMedia(id: number) {
  return requestClient.delete(`/admin/media/${id}`);
}

export async function getMediaPreviewUrl(adminUrl: string) {
  const apiBase = String(
    import.meta.env.VITE_GLOB_API_URL || '/api/v1',
  ).replace(/\/$/, '');
  const baseUrl = new URL(apiBase, window.location.origin);
  const mediaUrl = new URL(adminUrl, baseUrl.origin);
  let requestUrl = adminUrl;

  // The backend used to return URLs prefixed with /api/v1. The request client
  // already owns that prefix, so passing the URL through unchanged produced
  // /api/v1/api/v1/admin/media/{id}/file.
  if (
    mediaUrl.origin === baseUrl.origin &&
    (mediaUrl.pathname === baseUrl.pathname ||
      mediaUrl.pathname.startsWith(`${baseUrl.pathname}/`))
  ) {
    requestUrl = `${
      mediaUrl.pathname.slice(baseUrl.pathname.length) || '/'
    }${mediaUrl.search}${mediaUrl.hash}`;
  }

  let response: unknown;
  try {
    response = await rawRequestClient.get<Blob>(requestUrl, {
      responseType: 'blob',
    });
  } catch (error) {
    await showApiErrorMessage(error, '素材预览加载失败，请稍后重试');
    throw error;
  }
  const blob = (response as unknown as { data: Blob }).data;
  return URL.createObjectURL(blob);
}

export function listHomeSections() {
  return requestClient.get<HomeSectionRecord[]>('/admin/home-sections');
}

export function saveHomeSection(
  id: null | number,
  payload: Record<string, unknown>,
) {
  return id
    ? requestClient.put<HomeSectionRecord>(
        `/admin/home-sections/${id}`,
        payload,
      )
    : requestClient.post<HomeSectionRecord>('/admin/home-sections', payload);
}

export function deleteHomeSection(id: number) {
  return requestClient.delete(`/admin/home-sections/${id}`);
}

export async function listInquiries(status = '', keyword = '') {
  const result = await requestClient.get<PageResult<InquiryRecord>>(
    '/admin/inquiries',
    {
      params: {
        keyword: keyword || undefined,
        page: 1,
        size: 100,
        status: status || undefined,
      },
    },
  );
  return result.records;
}

export function updateInquiry(
  id: number,
  payload: { adminNote?: string; status: string },
) {
  return requestClient.put<InquiryRecord>(`/admin/inquiries/${id}`, payload);
}

export function listAdminUsers() {
  return requestClient.get<AdminUserRecord[]>('/admin/users');
}

export function createAdminUser(payload: {
  password: string;
  role: AdminUserRecord['role'];
  username: string;
}) {
  return requestClient.post<AdminUserRecord>('/admin/users', payload);
}

export function updateAdminUser(
  id: number,
  payload: { enabled: boolean; role: AdminUserRecord['role'] },
) {
  return requestClient.put<AdminUserRecord>(`/admin/users/${id}`, payload);
}

export async function listOperationLogs(keyword = '') {
  const result = await requestClient.get<PageResult<OperationLogRecord>>(
    '/admin/operation-logs',
    { params: { keyword: keyword || undefined, page: 1, size: 100 } },
  );
  return result.records;
}

export function listNotFoundLogs() {
  return requestClient.get<NotFoundLogRecord[]>('/admin/not-found-logs');
}

export function listRedirects() {
  return requestClient.get<RedirectRecord[]>('/admin/redirects');
}

export function saveRedirect(
  id: null | number,
  payload: Record<string, unknown>,
) {
  return id
    ? requestClient.put<RedirectRecord>(`/admin/redirects/${id}`, payload)
    : requestClient.post<RedirectRecord>('/admin/redirects', payload);
}

export function deleteRedirect(id: number) {
  return requestClient.delete(`/admin/redirects/${id}`);
}

export function getSiteSettings() {
  return requestClient.get<SiteSettingRecord>('/admin/site-settings');
}

export function saveSiteSettings(payload: Record<string, unknown>) {
  return requestClient.put<SiteSettingRecord>('/admin/site-settings', payload);
}

export function listNavigations(area: 'FOOTER' | 'HEADER') {
  return requestClient.get<NavigationRecord[]>('/admin/navigations', {
    params: { area },
  });
}

export function saveNavigation(
  id: null | number,
  payload: Record<string, unknown>,
) {
  return id
    ? requestClient.put<NavigationRecord>(`/admin/navigations/${id}`, payload)
    : requestClient.post<NavigationRecord>('/admin/navigations', payload);
}

export function deleteNavigation(id: number) {
  return requestClient.delete(`/admin/navigations/${id}`);
}

export function listPageHeroes() {
  return requestClient.get<PageHeroRecord[]>('/admin/page-heroes');
}

export function savePageHero(
  id: null | number,
  payload: Record<string, unknown>,
) {
  return id
    ? requestClient.put<PageHeroRecord>(`/admin/page-heroes/${id}`, payload)
    : requestClient.post<PageHeroRecord>('/admin/page-heroes', payload);
}

export function changePageHeroStatus(
  id: number,
  status: 'offline' | 'published',
) {
  return requestClient.post<PageHeroRecord>(
    `/admin/page-heroes/${id}/${status === 'published' ? 'publish' : 'offline'}`,
  );
}

export function deletePageHero(id: number) {
  return requestClient.delete(`/admin/page-heroes/${id}`);
}
