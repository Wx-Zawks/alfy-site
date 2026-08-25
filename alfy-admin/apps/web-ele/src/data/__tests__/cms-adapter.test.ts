import { describe, expect, it } from 'vitest';

import { contentFromBackend, contentPayload } from '../cms-adapter';

describe('article homepage placement adapter', () => {
  it('does not restore homepage visibility from a stale featured flag', () => {
    const article = contentFromBackend('articles', {
      featured: true,
      homeSlot: null,
      id: 1,
      slug: 'hidden-news',
      status: 'PUBLISHED',
      title: 'Hidden news',
    });

    expect(article.showOnHome).toBe(false);
    expect(article.featured).toBe(false);
    expect(contentPayload('articles', article)).toMatchObject({
      featured: false,
      homeSlot: null,
    });
  });

  it('keeps the featured flag aligned with an explicit homepage slot', () => {
    const article = contentFromBackend('articles', {
      featured: false,
      homeSlot: 'NEWS_LIST',
      id: 2,
      slug: 'visible-news',
      status: 'PUBLISHED',
      title: 'Visible news',
    });

    expect(article.showOnHome).toBe(true);
    expect(contentPayload('articles', article)).toMatchObject({
      featured: true,
      homeSlot: 'NEWS_LIST',
    });
  });
});
