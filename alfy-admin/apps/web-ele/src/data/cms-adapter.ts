import type { BackendContentRecord } from '#/api/cms';
import type { ContentItem, ContentResource } from '#/data/cms';

function status(value?: string): ContentItem['status'] {
  const normalized = value?.toLowerCase();
  if (normalized === 'published') return 'published';
  if (normalized === 'offline') return 'offline';
  return 'draft';
}
export function mediaUrl(id?: null | number) {
  if (!id) return '';
  const base = String(import.meta.env.VITE_GLOB_API_URL || '/api/v1').replace(
    /\/$/,
    '',
  );
  return `${base}/public/media/${id}`;
}

export function mediaIdFromUrl(value?: string) {
  const match = value?.match(/\/media\/(\d+)(?:\/file)?(?:\?.*)?$/);
  return match?.[1] ? Number(match[1]) : undefined;
}

export function contentFromBackend(
  resource: ContentResource,
  value: BackendContentRecord,
  categoryNames: Map<number, string> = new Map(),
): ContentItem {
  const categories = Array.isArray(value.categories) ? value.categories : [];
  const categoryId = value.categoryId ?? value.sceneId ?? categories[0]?.id;
  const coverMediaId =
    value.coverMediaId ?? value.logoMediaId ?? value.desktopMediaId;
  const title = value.name ?? value.title ?? '未命名内容';
  const homeSlot = String(value.homeSlot || '');
  let homePinned = false;
  let showOnHome = false;
  if (resource === 'articles') {
    homePinned = homeSlot === 'NEWS_PRIMARY';
    // `homeSlot` is the source of truth for homepage news placement. Falling
    // back to `featured` here makes a disabled switch turn itself back on
    // after reload when an older record still has is_featured = 1.
    showOnHome = Boolean(homeSlot);
  } else if (resource === 'cases') {
    homePinned = Boolean(value.featured);
    showOnHome = Boolean(value.featured);
  }

  return {
    category:
      value.category ??
      categoryNames.get(Number(categoryId)) ??
      categories[0]?.name ??
      '未分类',
    categoryId: categoryId ? Number(categoryId) : undefined,
    categoryIds: categories.map((item: { id: number }) => Number(item.id)),
    contentHtml: value.contentHtml ?? '',
    cover:
      value.desktopImageUrl ??
      value.logoUrl ??
      value.coverImageUrl ??
      mediaUrl(coverMediaId),
    coverMediaId: coverMediaId ? Number(coverMediaId) : undefined,
    createdAt: value.publishedAt ?? value.updatedAt ?? '',
    eyebrow: value.eyebrow ?? '',
    featured: resource === 'articles' ? showOnHome : Boolean(value.featured),
    highlightTitle: value.highlightText ?? '',
    homePinned,
    homeSortOrder: Number(value.homeSortOrder ?? value.sortOrder ?? 0),
    id: Number(value.id),
    mobileCover: value.mobileImageUrl ?? '',
    mobileMediaId: value.mobileMediaId
      ? Number(value.mobileMediaId)
      : mediaIdFromUrl(value.mobileImageUrl),
    primaryActionLabel: value.primaryAction?.label ?? value.action?.label ?? '',
    primaryActionLink:
      value.primaryAction?.target ?? value.action?.target ?? '',
    raw: value,
    resource,
    secondaryActionLabel: value.secondaryAction?.label ?? '',
    secondaryActionLink: value.secondaryAction?.target ?? '',
    seoDescription: value.seoDescription ?? '',
    seoKeywords: value.seoKeywords ?? '',
    seoTitle: value.seoTitle ?? '',
    showOnHome,
    slug:
      value.slug ??
      value.pageKey ??
      (resource === 'technologies' ? 'technology' : `${resource}-${value.id}`),
    sortOrder: Number(value.sortOrder ?? 0),
    status: status(value.status),
    summary: value.summary ?? '',
    title,
    updatedAt: value.updatedAt ?? value.publishedAt ?? '',
    version: value.version === undefined ? undefined : Number(value.version),
  };
}

function commonPayload(form: ContentItem) {
  return {
    // `form.cover` is what the editor actually changes. Keeping the media ID
    // received when the form was opened here causes the previous image to be
    // submitted even after an editor selects a different one (or clears it).
    coverMediaId: mediaIdFromUrl(form.cover) ?? null,
    featured: form.featured,
    seoDescription: form.seoDescription || null,
    seoKeywords: form.seoKeywords || null,
    seoTitle: form.seoTitle || null,
    sortOrder: form.sortOrder,
    summary: form.summary || null,
    version: form.version,
  };
}

export function contentPayload(
  resource: ContentResource,
  form: ContentItem,
): Record<string, unknown> {
  const raw = form.raw || {};
  const common = commonPayload(form);

  switch (resource) {
    case 'articles': {
      let categoryIds = form.categoryIds || [];
      if (categoryIds.length === 0 && form.categoryId) {
        categoryIds = [form.categoryId];
      }
      let homeSlot: null | string = null;
      if (form.homePinned) {
        homeSlot = 'NEWS_PRIMARY';
      } else if (form.showOnHome) {
        homeSlot = 'NEWS_LIST';
      }

      return {
        authorName: raw.authorName ?? null,
        categoryIds,
        contentHtml: form.contentHtml || null,
        contentText: raw.contentText ?? null,
        coverMediaId: common.coverMediaId,
        // Keep the legacy featured flag in sync with the explicit homepage
        // placement instead of allowing its stale value to re-enable news.
        featured: form.homePinned || form.showOnHome,
        homeSlot,
        homeSortOrder: form.homeSortOrder,
        seoDescription: common.seoDescription,
        seoKeywords: common.seoKeywords,
        seoTitle: common.seoTitle,
        slug: form.slug,
        sortOrder: form.sortOrder,
        sourcePublishedAt: raw.sourcePublishedAt ?? null,
        sourceUrl: raw.sourceUrl ?? null,
        summary: common.summary,
        title: form.title,
        version: common.version,
      };
    }
    case 'banners': {
      return {
        desktopMediaId: mediaIdFromUrl(form.cover) ?? undefined,
        endsAt: raw.endsAt ?? null,
        eyebrow: form.eyebrow || null,
        highlightText: form.highlightTitle || null,
        mobileMediaId:
          form.mobileMediaId ?? mediaIdFromUrl(form.mobileCover) ?? undefined,
        primaryActionLabel: form.primaryActionLabel || null,
        primaryActionTarget: form.primaryActionLink || null,
        secondaryActionLabel: form.secondaryActionLabel || null,
        secondaryActionTarget: form.secondaryActionLink || null,
        sortOrder: form.sortOrder,
        startsAt: raw.startsAt ?? null,
        summary: common.summary,
        title: form.title,
        version: common.version,
      };
    }
    case 'cases': {
      return {
        background: raw.background ?? null,
        clientName: raw.clientName ?? null,
        contentHtml: form.contentHtml || null,
        coverMediaId: common.coverMediaId,
        customerNeed: raw.customerNeed ?? null,
        featured: form.homePinned || form.showOnHome || form.featured,
        implementation: raw.implementation ?? null,
        location: raw.location ?? null,
        productIds: raw.productIds ?? [],
        resultSummary: raw.resultSummary ?? null,
        categoryId: form.categoryId,
        // 保留首个场景给旧版 API/官网，真正的可选关联使用 sceneIds。
        sceneId: Array.isArray(raw.sceneIds) ? (raw.sceneIds[0] ?? null) : null,
        sceneIds: raw.sceneIds ?? [],
        seoDescription: common.seoDescription,
        seoKeywords: common.seoKeywords,
        seoTitle: common.seoTitle,
        slug: form.slug,
        solution: raw.solution ?? null,
        sortOrder: form.sortOrder,
        summary: common.summary,
        title: form.title,
        version: common.version,
      };
    }
    case 'pages': {
      return {
        ...common,
        category: form.category || null,
        contentData: raw.contentData ?? null,
        contentHtml: form.contentHtml || null,
        pageKey: form.slug,
        title: form.title,
      };
    }
    case 'partners': {
      return {
        category: form.category || null,
        featured: form.featured,
        logoMediaId: mediaIdFromUrl(form.cover) ?? null,
        name: form.title,
        seoDescription: common.seoDescription,
        seoKeywords: common.seoKeywords,
        seoTitle: common.seoTitle,
        slug: form.slug,
        sortOrder: form.sortOrder,
        summary: common.summary,
        version: common.version,
        websiteUrl: raw.websiteUrl ?? null,
      };
    }
    case 'products': {
      return {
        ...common,
        categoryId: form.categoryId,
        contentHtml: form.contentHtml || null,
        contentText: raw.contentText ?? null,
        features: raw.features ?? [],
        name: form.title,
        sceneIds: raw.sceneIds ?? [],
        slug: form.slug,
        specifications: raw.specifications ?? null,
      };
    }
    case 'scenes': {
      return {
        ...common,
        contentHtml: form.contentHtml || null,
        name: form.title,
        painPoint: raw.painPoint ?? null,
        slogan: raw.slogan ?? null,
        slug: form.slug,
        solution: raw.solution ?? null,
      };
    }
    case 'technologies': {
      return {
        capabilityRows: raw.capabilityRows ?? [],
        contentHtml: form.contentHtml || null,
        ctaLabel: form.primaryActionLabel || null,
        ctaTarget: form.primaryActionLink || null,
        eyebrow: form.eyebrow || null,
        highlightText: form.highlightTitle || null,
        pillars: raw.pillars ?? [],
        summary: common.summary,
        title: form.title,
        version: common.version,
      };
    }
  }
}
