<script setup lang="ts">
const { open } = useInquiryDialog()
</script>

<template>
  <footer class="site-footer">
    <div class="container footer-cta">
      <div><p class="eyebrow eyebrow-light">项目咨询</p><h2>获取产品资料或项目方案</h2><p>告诉我们应用场景、工况和目标，技术与商务团队将尽快联系您。</p></div>
      <button class="button button-light" type="button" @click="open">提交项目需求 <span>↗</span></button>
    </div>
    <div class="container footer-grid">
      <div class="footer-brand"><NuxtLink class="footer-logo-wrap" to="/" aria-label="奥飞新材首页"><img src="/images/alfy-logo.png" alt="奥飞新材 ALFY"></NuxtLink><p>湖南奥飞新材料有限公司</p><p>中南大学气凝胶成果转化核心平台</p></div>
      <div><h3>产品</h3><NuxtLink to="/products?category=raw">粉体与分散体</NuxtLink><NuxtLink to="/products?category=blanket">气凝胶毡/板</NuxtLink><NuxtLink to="/products?category=coating">气凝胶涂料</NuxtLink></div>
      <div><h3>探索</h3><NuxtLink to="/applications">应用与案例</NuxtLink><NuxtLink to="/technology">技术研发</NuxtLink><NuxtLink to="/news">新闻资讯</NuxtLink></div>
      <div><h3>公司</h3><NuxtLink to="/cooperation">商务合作</NuxtLink><NuxtLink to="/about#team">研发团队</NuxtLink><NuxtLink to="/about#jobs">招聘信息</NuxtLink><NuxtLink to="/about#contact">联系我们</NuxtLink></div>
    </div>
    <div class="container footer-bottom"><span>© {{ new Date().getFullYear() }} 湖南奥飞新材料有限公司</span><span>ICP备案信息待甲方确认</span></div>
  </footer>
</template>
