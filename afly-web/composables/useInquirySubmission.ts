import type { ApiInquiryRequest, ApiInquiryResult } from '~/types/api'

export function useInquirySubmission(form: ApiInquiryRequest) {
  const route = useRoute()
  const { request } = useApiClient()
  const submitting = ref(false)
  const result = ref<ApiInquiryResult | null>(null)
  const errorMessage = ref('')

  async function submit() {
    submitting.value = true
    errorMessage.value = ''
    try {
      const queryEntries = Object.entries(route.query)
        .filter(([key, value]) => key.startsWith('utm_') && typeof value === 'string')
        .map(([key, value]) => [key, String(value)])
      result.value = await request<ApiInquiryResult>('/public/inquiries', {
        method: 'POST',
        body: {
          ...form,
          sourceUrl: import.meta.client ? window.location.href : route.fullPath,
          utm: Object.fromEntries(queryEntries)
        }
      })
    } catch (error) {
      const value = error as { data?: { message?: string }; message?: string }
      errorMessage.value = value.data?.message || value.message || '提交失败，请稍后重试或直接联系我们。'
    } finally {
      submitting.value = false
    }
  }

  return { errorMessage, result, submit, submitting }
}
