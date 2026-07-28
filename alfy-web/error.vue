<script setup lang="ts">
const props = defineProps<{
  error: {
    message?: string
    statusCode?: number
    statusMessage?: string
  }
}>()

const route = useRoute()
const { request } = useApiClient()
const isNotFound = computed(() => props.error.statusCode === 404)

onMounted(() => {
  if (!isNotFound.value) return
  request('/public/not-found', {
    body: {
      path: route.fullPath.slice(0, 1024),
      referer: document.referrer.slice(0, 1024)
    },
    method: 'POST'
  }).catch(() => undefined)
})

function returnHome() {
  clearError({ redirect: '/' })
}
</script>

<template>
  <main class="error-page">
    <div class="error-card">
      <p>{{ isNotFound ? '404 · PAGE NOT FOUND' : `ERROR ${error.statusCode || ''}` }}</p>
      <h1>{{ isNotFound ? '页面没有找到' : '页面暂时无法访问' }}</h1>
      <span>
        {{
          isNotFound
            ? '该地址可能已经变更，系统已记录本次访问，便于后台及时补充跳转规则。'
            : error.statusMessage || error.message || '请稍后重试。'
        }}
      </span>
      <button type="button" @click="returnHome">返回首页</button>
    </div>
  </main>
</template>

<style scoped>
.error-page {
  display: grid;
  min-height: 100vh;
  padding: 32px;
  place-items: center;
  color: #17363d;
  background: #f3f7f6;
}

.error-card {
  width: min(620px, 100%);
  padding: 56px;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 24px 80px rgb(16 42 53 / 12%);
}

.error-card p {
  color: #16816f;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.error-card h1 {
  margin: 12px 0;
  font-size: clamp(34px, 7vw, 64px);
}

.error-card span {
  display: block;
  color: #66777b;
  line-height: 1.8;
}

.error-card button {
  margin-top: 30px;
  padding: 13px 24px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: #e65b43;
  cursor: pointer;
}
</style>
