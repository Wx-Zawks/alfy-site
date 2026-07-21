<script setup lang="ts">
const { isOpen, close } = useInquiryDialog()
const submitted = ref(false)

const submit = () => { submitted.value = true }

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
      <div v-if="isOpen" class="dialog-backdrop" role="presentation" @click.self="close">
        <section class="inquiry-dialog" role="dialog" aria-modal="true" aria-labelledby="inquiry-title">
          <button class="dialog-close" type="button" aria-label="关闭" @click="close">×</button>
          <p class="eyebrow">获取方案</p><h2 id="inquiry-title">让我们了解您的项目</h2><p>当前为前端演示表单，后续接入询盘 API。</p>
          <form class="inquiry-form" @submit.prevent="submit">
            <label>姓名<input required placeholder="请输入姓名"></label>
            <label>手机<input required inputmode="tel" placeholder="请输入手机号"></label>
            <label>公司<input placeholder="请输入公司名称"></label>
            <label>需求类型<select><option>项目方案</option><option>产品资料</option><option>经销合作</option><option>联合研发</option></select></label>
            <label class="full">项目需求<textarea required rows="4" placeholder="请描述应用场景、工况或当前痛点" /></label>
            <button class="button button-primary full" type="submit">{{ submitted ? '已提交 ✓' : '提交咨询 ↗' }}</button>
          </form>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
