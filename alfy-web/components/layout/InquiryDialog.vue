<script setup lang="ts">
import type { ApiInquiryRequest, ApiInquiryResult } from '~/types/api'
import { useApiClient } from '~/composables/useApi'

const { isOpen, close } = useInquiryDialog()
const { request } = useApiClient()
const route = useRoute()
const submitting = ref(false)
const result = ref<ApiInquiryResult | null>(null)
const errorMessage = ref('')
const form = reactive<ApiInquiryRequest>({
  company: '',
  email: '',
  inquiryType: '项目方案',
  message: '',
  name: '',
  phone: '',
  privacyAccepted: false
})

function resetForm() {
  Object.assign(form, {
    company: '',
    email: '',
    inquiryType: '项目方案',
    message: '',
    name: '',
    phone: '',
    privacyAccepted: false
  })
  result.value = null
  errorMessage.value = ''
}

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

function closeDialog() {
  close()
  if (import.meta.client) window.setTimeout(resetForm, 200)
  else resetForm()
}

watch(isOpen, (value) => {
  if (import.meta.client) document.body.style.overflow = value ? 'hidden' : ''
})

onBeforeUnmount(() => {
  if (import.meta.client) document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="isOpen" class="dialog-backdrop" role="presentation" @click.self="closeDialog">
        <section class="inquiry-dialog" role="dialog" aria-modal="true" aria-labelledby="inquiry-title">
          <button class="dialog-close" type="button" aria-label="关闭" @click="closeDialog">×</button>
          <p class="eyebrow">获取方案</p>
          <h2 id="inquiry-title">让我们了解您的项目</h2>
          <p v-if="!result">提交后，咨询信息会直接进入后台管理系统。</p>

          <div v-if="result" class="inquiry-success">
            <strong>咨询已提交</strong>
            <p>咨询编号：{{ result.inquiryNo }}</p>
            <button class="button button-primary" type="button" @click="closeDialog">完成</button>
          </div>

          <form v-else class="inquiry-form" @submit.prevent="submit">
            <label>姓名<input v-model.trim="form.name" required maxlength="64" placeholder="请输入姓名"></label>
            <label>手机<input v-model.trim="form.phone" inputmode="tel" maxlength="32" placeholder="请输入手机号"></label>
            <label>邮箱<input v-model.trim="form.email" type="email" maxlength="128" placeholder="请输入邮箱"></label>
            <label>公司<input v-model.trim="form.company" maxlength="128" placeholder="请输入公司名称"></label>
            <label>需求类型
              <select v-model="form.inquiryType">
                <option>项目方案</option>
                <option>产品资料</option>
                <option>经销合作</option>
                <option>联合研发</option>
              </select>
            </label>
            <label class="full">项目需求<textarea v-model.trim="form.message" required maxlength="5000" rows="4" placeholder="请描述应用场景、工况或当前痛点" /></label>
            <label class="privacy-field full"><input v-model="form.privacyAccepted" required type="checkbox"> 我同意提交并处理以上联系信息</label>
            <p v-if="errorMessage" class="form-error full" role="alert">{{ errorMessage }}</p>
            <button class="button button-primary full" type="submit" :disabled="submitting">{{ submitting ? '正在提交…' : '提交咨询 →' }}</button>
          </form>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
