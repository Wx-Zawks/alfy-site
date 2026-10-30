<script setup lang="ts">
import { absoluteSeoUrl } from '~/utils/seo-url'
import { safeJsonLd } from '~/utils/json-ld'

const { settings } = await useGlobalSeo()

useHead(() => {
  const origin = settings.value.canonicalOrigin
  const organizationId = `${origin}/#organization`
  const organization: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    '@id': organizationId,
    name: settings.value.organizationName,
    alternateName: settings.value.organizationAlternateName || undefined,
    legalName: settings.value.organizationLegalName || undefined,
    url: `${origin}/`,
    logo: absoluteSeoUrl(origin, '/images/alfy-logo.png'),
    description: settings.value.organizationDescription || settings.value.defaultDescription,
    foundingDate: settings.value.foundingDate || undefined,
    sameAs: settings.value.sameAs.length ? settings.value.sameAs : undefined
  }
  const website = {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    '@id': `${origin}/#website`,
    url: `${origin}/`,
    name: settings.value.defaultTitle,
    publisher: { '@id': organizationId },
    inLanguage: 'zh-CN'
  }
  const verificationMeta = [
    settings.value.baiduSiteVerification
      ? { name: 'baidu-site-verification', content: settings.value.baiduSiteVerification }
      : null,
    settings.value.googleSiteVerification
      ? { name: 'google-site-verification', content: settings.value.googleSiteVerification }
      : null,
    settings.value.bingSiteVerification
      ? { name: 'msvalidate.01', content: settings.value.bingSiteVerification }
      : null
  ].filter(Boolean) as Array<{ content: string; name: string }>
  return {
    titleTemplate: settings.value.titleTemplate,
    meta: verificationMeta,
    script: [
      { key: 'organization-json-ld', type: 'application/ld+json', innerHTML: safeJsonLd(organization) },
      { key: 'website-json-ld', type: 'application/ld+json', innerHTML: safeJsonLd(website) }
    ]
  }
})
</script>

<template>
  <NuxtLoadingIndicator color="#e51b23" :height="3" />
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>
