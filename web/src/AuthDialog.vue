<script setup lang="ts">
import { LogIn, X } from 'lucide-vue-next'
import { ref, watch } from 'vue'
import { api } from './api'
import type { Account } from './types'

const props = defineProps<{ open: boolean; registrationOpen: boolean }>()
const emit = defineEmits<{ close: []; authenticated: [account: Account] }>()
const dialog = ref<HTMLDialogElement>()
const registering = ref(false)
const name = ref('')
const email = ref('')
const password = ref('')
const processing = ref(false)
const error = ref('')

watch(() => props.open, (open) => {
  if (open && !dialog.value?.open) dialog.value?.showModal()
  if (!open && dialog.value?.open) dialog.value.close()
})

function close() {
  if (processing.value) return
  dialog.value?.close()
  password.value = ''
  error.value = ''
  emit('close')
}

async function submit() {
  if (processing.value) return
  processing.value = true
  error.value = ''
  try {
    const { account } = await api<{ account: Account }>(registering.value ? '/auth/register' : '/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: email.value, password: password.value, ...(registering.value ? { displayName: name.value.trim() } : {}) }),
    })
    processing.value = false
    dialog.value?.close()
    password.value = ''
    emit('authenticated', account)
  } catch (failure) {
    error.value = failure instanceof Error ? failure.message : '登录失败，请重试。'
  } finally {
    processing.value = false
  }
}
</script>

<template>
  <dialog ref="dialog" class="auth-dialog" aria-labelledby="auth-title" @cancel.prevent="close">
    <header class="panel-head">
      <h2 id="auth-title">{{ registering ? '创建账户' : '登录路书' }}</h2>
      <button type="button" class="icon-button" aria-label="关闭" :disabled="processing" @click="close"><X :size="20" /></button>
    </header>
    <form @submit.prevent="submit">
      <p class="muted">登录后可生成、保存和修改你的行程。</p>
      <label v-if="registering" class="field"><span>称呼</span><input v-model.trim="name" required maxlength="40" autocomplete="nickname" /></label>
      <label class="field"><span>邮箱</span><input v-model.trim="email" type="email" required maxlength="254" autocomplete="username" /></label>
      <label class="field"><span>密码</span><input v-model="password" type="password" required maxlength="72"
          :minlength="registering ? 10 : undefined" :autocomplete="registering ? 'new-password' : 'current-password'" /></label>
      <p v-if="error" class="form-issue" role="alert">{{ error }}</p>
      <button type="submit" class="button primary generate" :disabled="processing"><LogIn :size="17" />
        {{ processing ? '处理中…' : registering ? '创建账户' : '登录' }}
      </button>
      <button v-if="registrationOpen" type="button" class="text-button auth-switch" :disabled="processing"
        @click="registering = !registering; error = ''; password = ''">
        {{ registering ? '已有账户，返回登录' : '第一次使用？创建账户' }}
      </button>
    </form>
  </dialog>
</template>
