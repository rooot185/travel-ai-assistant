<script setup lang="ts">
import { ArrowRight, CalendarDays, ChevronDown, MapPin, Plus, Sparkles, X } from 'lucide-vue-next'
import { ref } from 'vue'
import { today } from './api'
import type { Request } from './types'

defineProps<{ busy: boolean; ready: boolean; signedIn: boolean }>()
const emit = defineEmits<{ submit: [request: Request] }>()
const interests = ['自然风光', '人文历史', '特色美食', '城市漫步', '博物馆', '亲子活动']
const form = ref<Request>({
  city: '', startDate: today(7), days: 3, travelers: 2, budget: 3000,
  pace: 'BALANCED', preferences: [], mustVisit: [], notes: '',
})
const mustText = ref('')
const issue = ref('')
const cityField = ref<HTMLInputElement>()
const minDate = today()
const maxDate = today(365)

function example() {
  form.value = {
    city: '杭州市', startDate: today(14), days: 3, travelers: 2, budget: 3200,
    pace: 'BALANCED', preferences: ['自然风光', '人文历史', '特色美食'],
    mustVisit: ['西湖风景名胜区', '灵隐寺'], notes: '希望每天留出午餐和自由散步的时间。',
  }
  mustText.value = ''
  issue.value = ''
  cityField.value?.focus()
}

function toggle(topic: string) {
  const current = form.value.preferences
  form.value.preferences = current.includes(topic) ? current.filter(item => item !== topic) : [...current, topic]
}

function addPlace() {
  const value = mustText.value.trim()
  if (!value) return true
  const places = form.value.mustVisit
  if (places.includes(value)) {
    mustText.value = ''
    return true
  }
  if (places.length >= 5) {
    issue.value = '最多可添加五个必去地点。'
    return false
  }
  places.push(value)
  mustText.value = ''
  issue.value = ''
  return true
}

function submit() {
  if (!addPlace()) return
  issue.value = ''
  emit('submit', { ...form.value, city: form.value.city.trim(), notes: form.value.notes.trim(),
    preferences: [...form.value.preferences], mustVisit: [...form.value.mustVisit] })
}
</script>

<template>
  <form class="panel setup" aria-labelledby="setup-title" @submit.prevent="submit">
    <header class="panel-head">
      <h2 id="setup-title">行程设置</h2>
      <button type="button" class="text-button" :disabled="busy" @click="example"><Sparkles :size="15" /> 杭州示例</button>
    </header>
    <fieldset :disabled="busy">
      <label class="field">
        <span>目的地</span>
        <span class="input-icon"><MapPin :size="17" /><input ref="cityField" v-model.trim="form.city" required maxlength="16" placeholder="输入中国大陆城市" autocomplete="off" /></span>
      </label>
      <div class="two-fields">
        <label class="field"><span>出发日期</span><input v-model="form.startDate" required type="date" :min="minDate" :max="maxDate" /></label>
        <label class="field">
          <span>游玩天数</span>
          <span class="input-icon"><CalendarDays :size="16" /><select v-model="form.days"><option v-for="n in 7" :key="n" :value="n">{{ n }} 天</option></select></span>
        </label>
      </div>
      <div class="two-fields">
        <label class="field"><span>同行人数</span><input v-model.number="form.travelers" required type="number" min="1" max="12" step="1" /></label>
        <label class="field">
          <span>当地总预算</span>
          <span class="input-icon"><span class="input-token">¥</span><input v-model.number="form.budget" required type="number" min="100" max="200000" step="1" /></span>
        </label>
      </div>
      <label class="field">
        <span>旅行节奏</span>
        <select v-model="form.pace"><option value="RELAXED">悠闲 · 每天最多 3 站</option><option value="BALANCED">适中 · 留出机动时间</option><option value="FULL">充实 · 每天最多 4 站</option></select>
      </label>
      <fieldset class="preferences">
        <legend>旅行偏好</legend>
        <div class="chips">
          <button v-for="topic in interests" :key="topic" type="button" class="chip"
            :aria-pressed="form.preferences.includes(topic)" @click="toggle(topic)">{{ topic }}</button>
        </div>
      </fieldset>
      <details class="form-details">
        <summary><span>必去地点与补充要求 <small v-if="form.mustVisit.length">· {{ form.mustVisit.length }} 个地点</small></span><ChevronDown :size="16" /></summary>
        <label class="field">
          <span>必去地点 <small>选填</small></span>
          <span class="input-with-button"><input v-model.trim="mustText" maxlength="40" placeholder="例如：灵隐寺" @keydown.enter.prevent="addPlace" />
            <button class="icon-button" type="button" aria-label="添加必去地点" @click="addPlace"><Plus :size="18" /></button></span>
        </label>
        <div v-if="form.mustVisit.length" class="chips added">
          <button v-for="place in form.mustVisit" :key="place" type="button" class="chip active"
            :aria-label="'移除' + place" @click="form.mustVisit = form.mustVisit.filter(item => item !== place); issue = ''">{{ place }} <X :size="13" /></button>
        </div>
        <label class="field"><span>补充要求 <small>选填</small></span><textarea v-model="form.notes" rows="2" maxlength="300" placeholder="如带老人同行，希望少步行" /></label>
      </details>
      <p v-if="issue" class="form-issue" role="alert">{{ issue }}</p>
      <button class="button primary generate" type="submit" :disabled="busy || !ready">
        <Sparkles :size="17" /> {{ busy ? '正在处理行程' : signedIn ? '生成行程' : '登录后生成行程' }} <ArrowRight :size="17" />
      </button>
      <p class="form-caption">预算包含当地产生的费用，不含往返城市的交通。</p>
    </fieldset>
  </form>
</template>
