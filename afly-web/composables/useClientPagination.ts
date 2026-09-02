import type { MaybeRefOrGetter } from 'vue'

export function useClientPagination<T>(items: MaybeRefOrGetter<readonly T[]>, pageSize: number) {
  const currentPage = ref(1)
  const totalPages = computed(() => Math.max(1, Math.ceil(toValue(items).length / pageSize)))
  const paginatedItems = computed(() => {
    const start = (currentPage.value - 1) * pageSize
    return toValue(items).slice(start, start + pageSize)
  })
  const paginationItems = computed<(number | string)[]>(() => {
    if (totalPages.value <= 7) {
      return Array.from({ length: totalPages.value }, (_, index) => index + 1)
    }

    const pages = [1, currentPage.value - 1, currentPage.value, currentPage.value + 1, totalPages.value]
      .filter(page => page >= 1 && page <= totalPages.value)
    const uniquePages = [...new Set(pages)].sort((left, right) => left - right)

    return uniquePages.flatMap((page, index) => {
      const previous = uniquePages[index - 1]
      return previous && page - previous > 1 ? [`ellipsis-${previous}`, page] : [page]
    })
  })

  function changePage(page: number) {
    const nextPage = Math.min(Math.max(page, 1), totalPages.value)
    if (nextPage === currentPage.value) return false
    currentPage.value = nextPage
    return true
  }

  function resetPage() {
    currentPage.value = 1
  }

  watch(totalPages, total => {
    if (currentPage.value > total) currentPage.value = total
  })

  return { changePage, currentPage, paginatedItems, paginationItems, resetPage, totalPages }
}
