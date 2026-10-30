import type { JsonLdValue } from '~/types/seo'

export function safeJsonLd(value: JsonLdValue) {
  return JSON.stringify(value).replace(/</g, '\\u003c')
}

export function breadcrumbJsonLd(
  origin: string,
  items: Array<{ name: string; path: string }>
) {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: items.map((item, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: item.name,
      item: new URL(item.path, origin).toString()
    }))
  }
}
