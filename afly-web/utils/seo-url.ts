export function normalizeSiteOrigin(value?: null | string) {
  const fallback = 'https://alfy.com.cn'
  if (!value) return fallback
  try {
    const url = new URL(value)
    if (url.protocol !== 'https:' || !url.hostname) return fallback
    return `${url.protocol}//${url.host}`
  } catch {
    return fallback
  }
}

export function buildCanonical(origin: string, rawPath: string) {
  const url = new URL(rawPath || '/', normalizeSiteOrigin(origin))
  url.protocol = 'https:'
  url.search = ''
  url.hash = ''
  url.pathname = url.pathname.replace(/\/{2,}/g, '/')
  if (url.pathname !== '/') url.pathname = url.pathname.replace(/\/$/, '')
  return url.toString()
}

export function absoluteSeoUrl(origin: string, value?: null | string) {
  if (!value) return ''
  try {
    return new URL(value, normalizeSiteOrigin(origin)).toString()
  } catch {
    return ''
  }
}
