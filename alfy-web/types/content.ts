export interface Product {
  id: string
  name: string
  slug: string
  category: 'raw' | 'blanket' | 'coating' | 'support'
  categoryName: string
  summary: string
  image: string
  features: string[]
}

export interface ApplicationScene {
  key: string
  name: string
  slogan: string
  summary: string
  image: string
}

export interface CaseProject {
  id: string
  slug: string
  title: string
  category: string
  summary: string
  image: string
}

export interface Article {
  id: string
  slug: string
  title: string
  category: 'company' | 'industry' | 'technology'
  categoryName: string
  date: string
  summary: string
  image: string
}
