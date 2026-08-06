import type { ApiContentPage } from '~/types/api'

export interface CooperationPageDefinition {
  fallbackImage: string
  fallbackNavLabel: string
  key: string
  path: string
  slug: string
}

export const cooperationPageDefinitions: CooperationPageDefinition[] = [
  {
    fallbackImage: '/images/news-1.jpeg',
    fallbackNavLabel: '合作总览',
    key: 'cooperation',
    path: '/cooperation',
    slug: ''
  },
  {
    fallbackImage: '/images/launch-1.jpg',
    fallbackNavLabel: '经销商合作',
    key: 'cooperation-dealer',
    path: '/cooperation/dealer',
    slug: 'dealer'
  },
  {
    fallbackImage: '/images/aerogel-powder.jpg',
    fallbackNavLabel: '复合产品开发',
    key: 'cooperation-product-development',
    path: '/cooperation/product-development',
    slug: 'product-development'
  },
  {
    fallbackImage: '/images/research-institute.jpg',
    fallbackNavLabel: '产业链合作',
    key: 'cooperation-industry-chain',
    path: '/cooperation/industry-chain',
    slug: 'industry-chain'
  }
]

export async function useCooperationPages() {
  const requests = await Promise.all(
    cooperationPageDefinitions.map(definition =>
      useApi<ApiContentPage>(
        `public-page-${definition.key}`,
        `/public/pages/${definition.key}`,
        { optional: true }
      )
    )
  )

  const entries = computed(() => cooperationPageDefinitions.map((definition, index) => ({
    content: requests[index]?.data.value ?? null,
    definition
  })))

  const navItems = computed(() => entries.value.map(({ content, definition }) => ({
    label: content?.contentData?.navLabel?.trim()
      || definition.fallbackNavLabel,
    to: definition.path
  })))

  return { entries, navItems }
}
