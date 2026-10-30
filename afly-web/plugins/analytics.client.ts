import type { ApiSeoGlobalSetting } from '~/types/seo'

function addScript(id: string, src?: string, content?: string) {
  if (document.getElementById(id)) return
  const script = document.createElement('script')
  script.id = id
  script.async = true
  if (src) script.src = src
  if (content) script.textContent = content
  document.head.appendChild(script)
}

export default defineNuxtPlugin(async (nuxtApp) => {
  const config = useRuntimeConfig()
  const route = useRoute()
  const { request } = useApiClient()
  const { track, trackPageView } = useTracking()
  let settings: ApiSeoGlobalSetting | null = null
  try {
    settings = await request<ApiSeoGlobalSetting>('/public/seo/settings', { optional: true })
  } catch {
    settings = null
  }
  if (!settings) return

  const ga4Id = settings.ga4MeasurementId || ''
  if (settings.ga4Enabled && /^G-[A-Z0-9]+$/.test(ga4Id)) {
    const id = ga4Id
    addScript('alfy-ga4', `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(id)}`)
    window.dataLayer = window.dataLayer || []
    window.gtag = (...args: unknown[]) => window.dataLayer?.push(args)
    window.gtag('js', new Date())
    window.gtag('config', id, { send_page_view: false })
  }
  const baiduAnalyticsId = settings.baiduAnalyticsId || ''
  if (settings.baiduAnalyticsEnabled && /^[A-Za-z0-9_-]+$/.test(baiduAnalyticsId)) {
    window._hmt = window._hmt || []
    addScript('alfy-baidu-analytics', `https://hm.baidu.com/hm.js?${encodeURIComponent(baiduAnalyticsId)}`)
  }
  const clarityProjectId = settings.clarityProjectId || ''
  if (settings.clarityEnabled && /^[A-Za-z0-9]+$/.test(clarityProjectId)) {
    const projectId = JSON.stringify(clarityProjectId)
    addScript('alfy-clarity', undefined,
      `(function(c,l,a,r,i,t,y){c[a]=c[a]||function(){(c[a].q=c[a].q||[]).push(arguments)};t=l.createElement(r);t.async=1;t.src="https://www.clarity.ms/tag/"+i;y=l.getElementsByTagName(r)[0];y.parentNode.insertBefore(t,y)})(window,document,"clarity","script",${projectId});`)
  }

  captureUtmAttribution(route.query)
  let lastPath = ''
  nuxtApp.hook('page:finish', () => {
    const path = window.location.pathname + window.location.search
    captureUtmAttribution(useRoute().query)
    if (path === lastPath) return
    if (lastPath && window._hmt) window._hmt.push(['_trackPageview', path])
    lastPath = path
    trackPageView(path)
  })

  document.addEventListener('click', (event) => {
    const target = event.target as HTMLElement | null
    const link = target?.closest('a')
    const button = target?.closest('button, [data-cta]') as HTMLElement | null
    const pagePath = window.location.pathname
    if (link?.href.startsWith('tel:')) {
      track('phone_click', { page_path: pagePath, position: link.dataset.position || 'link' })
    } else if (link?.href.startsWith('mailto:')) {
      track('email_click', { page_path: pagePath, position: link.dataset.position || 'link' })
    } else if (link?.hasAttribute('download')) {
      track('file_download', { file_name: link.getAttribute('download') || 'download', page_path: pagePath })
    } else if (button?.dataset.cta) {
      track('cta_click', { cta_name: button.dataset.cta, page_path: pagePath, position: button.dataset.position || 'button' })
    }
  }, { passive: true })
})
