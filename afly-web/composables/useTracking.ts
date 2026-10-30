interface TrackingParams {
  [key: string]: boolean | number | string | undefined
}

const UTM_KEYS = ['utm_source', 'utm_medium', 'utm_campaign', 'utm_content', 'utm_term'] as const
const UTM_STORAGE_KEY = 'alfy-utm-attribution'

declare global {
  interface Window {
    _hmt?: unknown[]
    clarity?: (...args: unknown[]) => void
    dataLayer?: unknown[]
    gtag?: (...args: unknown[]) => void
  }
}

export function useTracking() {
  function track(eventName: string, params: TrackingParams = {}) {
    if (!import.meta.client) return
    const safeParams = Object.fromEntries(
      Object.entries(params).filter(([, value]) => value !== undefined)
    )
    window.gtag?.('event', eventName, safeParams)
    window._hmt?.push(['_trackEvent', 'website', eventName, String(safeParams.position || safeParams.page_path || '')])
    window.clarity?.('event', eventName)
  }

  function trackPageView(path: string) {
    if (!import.meta.client) return
    window.gtag?.('event', 'page_view', {
      page_location: window.location.href,
      page_path: path,
      page_title: document.title
    })
  }

  return {
    track,
    trackCtaClick: (params: TrackingParams) => track('cta_click', params),
    trackEmailClick: (params: TrackingParams) => track('email_click', params),
    trackFileDownload: (params: TrackingParams) => track('file_download', params),
    trackGenerateLead: (params: TrackingParams) => track('generate_lead', params),
    trackPageView,
    trackPhoneClick: (params: TrackingParams) => track('phone_click', params)
  }
}

export function captureUtmAttribution(query: Record<string, unknown>) {
  if (!import.meta.client) return
  const attribution = Object.fromEntries(UTM_KEYS
    .map(key => [key, typeof query[key] === 'string' ? query[key] : undefined])
    .filter(([, value]) => Boolean(value)))
  if (Object.keys(attribution).length > 0) {
    sessionStorage.setItem(UTM_STORAGE_KEY, JSON.stringify(attribution))
  }
}

export function readUtmAttribution(): Record<string, string> {
  if (!import.meta.client) return {}
  try {
    const value = JSON.parse(sessionStorage.getItem(UTM_STORAGE_KEY) || '{}')
    return Object.fromEntries(Object.entries(value)
      .filter(([key, item]) => UTM_KEYS.includes(key as typeof UTM_KEYS[number]) && typeof item === 'string')) as Record<string, string>
  } catch {
    return {}
  }
}
