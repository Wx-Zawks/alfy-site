const DEFAULT_REFRESH_INTERVAL_MS = 120_000
const MIN_REFRESH_INTERVAL_MS = 5_000
const FOCUS_REFRESH_THROTTLE_MS = 1_000

/**
 * 后台内容发布后，定时重新获取当前页面使用的所有公共数据。
 * 页面在后台标签页时暂停请求，用户切回页面后立即同步一次。
 */
export default defineNuxtPlugin((nuxtApp) => {
  const config = useRuntimeConfig()
  const configuredInterval = Number(config.public.contentRefreshIntervalMs)
  const refreshInterval = Number.isFinite(configuredInterval) && configuredInterval > 0
    ? Math.max(configuredInterval, MIN_REFRESH_INTERVAL_MS)
    : DEFAULT_REFRESH_INTERVAL_MS

  let intervalId: number | undefined
  let refreshPromise: Promise<void> | undefined
  let lastRefreshAt = 0

  const refreshContent = () => {
    if (document.visibilityState !== 'visible') return
    if (refreshPromise) return refreshPromise

    const now = Date.now()
    if (now - lastRefreshAt < FOCUS_REFRESH_THROTTLE_MS) return
    lastRefreshAt = now

    refreshPromise = refreshNuxtData()
      .catch((error: unknown) => {
        if (import.meta.dev) {
          console.warn('[content-auto-refresh] 内容同步失败，将在下次自动重试。', error)
        }
      })
      .finally(() => {
        refreshPromise = undefined
      })

    return refreshPromise
  }

  const handleFocus = () => {
    void refreshContent()
  }

  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      void refreshContent()
    }
  }

  const stop = () => {
    if (intervalId !== undefined) {
      window.clearInterval(intervalId)
      intervalId = undefined
    }
    window.removeEventListener('focus', handleFocus)
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  }

  nuxtApp.hook('app:mounted', () => {
    intervalId = window.setInterval(() => {
      void refreshContent()
    }, refreshInterval)
    window.addEventListener('focus', handleFocus)
    document.addEventListener('visibilitychange', handleVisibilityChange)
  })

  nuxtApp.vueApp.onUnmount(stop)
  import.meta.hot?.dispose(stop)
})
