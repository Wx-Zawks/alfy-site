<script setup lang="ts">
const { open } = useInquiryDialog()
</script>

<template>
  <footer class="site-footer">
    <div class="container footer-main">
      <div class="footer-brand">
        <NuxtLink class="footer-logo-wrap" to="/" aria-label="奥飞新材首页"><img src="/images/alfy-logo.png" alt="奥飞新材 ALFY"></NuxtLink>
        <p>湖南奥飞新材料有限公司</p>
        <p>中南大学气凝胶成果转化核心平台</p>
        <button class="footer-contact-link" type="button" @click="open">立即咨询 <span>→</span></button>
      </div>
      <div class="footer-links"><h3>关于奥飞</h3><NuxtLink to="/about">企业介绍</NuxtLink><NuxtLink to="/about#history">发展历程</NuxtLink><NuxtLink to="/about#team">研发团队</NuxtLink></div>
      <div class="footer-links"><h3>应用与技术</h3><NuxtLink to="/applications">应用场景</NuxtLink><NuxtLink to="/applications">典型案例</NuxtLink><NuxtLink to="/technology">核心技术</NuxtLink></div>
      <div class="footer-links"><h3>合作与资讯</h3><NuxtLink to="/cooperation">典型案例</NuxtLink><NuxtLink to="/news">新闻动态</NuxtLink><NuxtLink to="/cooperation">商务合作</NuxtLink></div>
      <div class="footer-inquiry">
        <p class="eyebrow">项目咨询</p>
        <h3>获取产品资料和项目方案建议</h3>
        <p>提供应用场景、工况与目标，我们将为您匹配产品及技术路径。</p>
        <button class="button button-primary" type="button" @click="open">提交需求 <span>→</span></button>
      </div>
    </div>
    <div class="container footer-bottom"><span>© {{ new Date().getFullYear() }} 湖南奥飞新材料有限公司</span><span>节能 · 环保 · 安全</span></div>
  </footer>
</template>
