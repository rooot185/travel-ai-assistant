<script setup lang="ts">
import { ArrowUpRight, CarFront, Check, Download, Footprints, LockKeyhole, LockKeyholeOpen, MapPin, Pencil, Trash2, WalletCards } from 'lucide-vue-next'
import { computed, ref, watch } from 'vue'
import { downloadTrip, mapLink, money } from './api'
import RouteSheet from './RouteSheet.vue'
import type { Stop, Trip } from './types'

const props = defineProps<{ trip: Trip; busy: boolean; ready: boolean }>()
const emit = defineEmits<{ revise: [days: number[], instruction: string]; lock: [stop: Stop]; remove: [] }>()
const dayNumber = ref(1)
const pinnedMarker = ref<string | null>(null)
const previewMarker = ref<string | null>(null)
const active = computed(() => previewMarker.value ?? pinnedMarker.value)
const instruction = ref('')
const selected = ref<number[]>([1])
const confirmDelete = ref(false)
const day = computed(() => props.trip.plan.days.find(item => item.number === dayNumber.value) ?? props.trip.plan.days[0]!)
const categories = computed(() => [
  ['住宿', props.trip.plan.budget.lodging], ['餐饮', props.trip.plan.budget.food],
  ['当地交通', props.trip.plan.budget.localTransport], ['活动', props.trip.plan.budget.activities],
] as const)
const total = computed(() => categories.value.reduce((sum, [, value]) => sum + value, 0))
const dateFormat = new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Shanghai' })
const sourceNames: Record<string, string> = { place_search: '高德地点检索', route_lookup: '高德路线查询', model_call: '模型调用' }

watch(() => props.trip.id, () => {
  dayNumber.value = 1
  pinnedMarker.value = null
  previewMarker.value = null
  instruction.value = ''
  selected.value = [1]
  confirmDelete.value = false
})
watch(() => props.trip.plan.generatedAt, () => { instruction.value = '' })

function choose(number: number) {
  dayNumber.value = number
  pinnedMarker.value = null
  previewMarker.value = null
  selected.value = [number]
}

function locate(id: string) {
  pinnedMarker.value = pinnedMarker.value === id ? null : id
  if (pinnedMarker.value && window.matchMedia('(max-width: 730px)').matches) {
    const routeTitle = document.getElementById('route-title')
    routeTitle?.focus({ preventScroll: true })
    routeTitle?.scrollIntoView({ behavior: 'instant', block: 'start' })
  }
}

function submit() {
  if (!instruction.value.trim() || !selected.value.length) return
  emit('revise', [...selected.value].sort((a, b) => a - b), instruction.value.trim())
}

function exportPlan() {
  const { plan } = props.trip
  const body = [
    plan.title, plan.summary, plan.city.name + ' · ' + plan.request.travelers + ' 人',
    ...plan.days.flatMap(item => [
      '', item.date + ' · ' + item.title,
      ...item.stops.map(stop => [
        stop.start + '–' + stop.end + ' ' + stop.place.name,
        '  ' + stop.place.address, '  ' + stop.note,
        '  ' + mapLink(stop.place.point.lng, stop.place.point.lat, stop.place.name),
      ].join('\n')),
    ]),
    '', '当地费用估算：' + money(total.value),
    '地点与路段来自高德地图，请出发前核实营业、预订及票价。',
  ].join('\n')
  downloadTrip(plan.title, body)
}
</script>

<template>
  <section class="panel itinerary" aria-labelledby="itinerary-title">
    <header class="panel-head result-head">
      <div>
        <h2 id="itinerary-title" tabindex="-1">{{ trip.plan.title }}</h2>
        <p class="trip-city"><MapPin :size="15" /> {{ trip.plan.city.name }} <span>· {{ trip.plan.request.travelers }} 人 / {{ trip.plan.days.length }} 天</span></p>
      </div>
      <button class="button subtle small-button" type="button" @click="exportPlan"><Download :size="16" /> 导出</button>
    </header>
    <p class="trip-summary">{{ trip.plan.summary }}</p>
    <p v-if="trip.plan.changeSummary" class="change-note"><Check :size="16" /> {{ trip.plan.changeSummary }}</p>
    <nav class="days" aria-label="选择行程日期">
      <button v-for="item in trip.plan.days" :key="item.number" type="button" class="day-button"
        :aria-pressed="item.number === day.number" @click="choose(item.number)">
        <strong>第 {{ item.number }} 天</strong><span>{{ item.date.slice(5).replace('-', '/') }}</span>
      </button>
    </nav>
    <div class="day-spread">
      <section class="day-timeline" :aria-label="day.title">
        <h3>{{ day.title }}</h3>
        <ol class="stops">
          <li v-for="(stop, index) in day.stops" :key="stop.place.id" class="stop">
            <p v-if="stop.inbound" class="leg">
              <component :is="stop.inbound.mode === 'WALK' ? Footprints : CarFront" :size="15" />
              {{ stop.inbound.mode === 'WALK' ? '步行' : '驾车' }}约 {{ stop.inbound.minutes }} 分钟 · {{ (stop.inbound.meters / 1000).toFixed(1) }} 公里
            </p>
            <div class="stop-card" @mouseenter="previewMarker = stop.place.id" @mouseleave="previewMarker = null">
              <button class="stop-index" type="button" :aria-label="'在路线图上定位' + stop.place.name"
                :aria-pressed="pinnedMarker === stop.place.id" @click="locate(stop.place.id)">{{ index + 1 }}</button>
              <div class="stop-copy">
                <p class="stop-time">{{ stop.start }}–{{ stop.end }}<span v-if="trip.plan.mustVisitIds.includes(stop.place.id)"> · 必去</span></p>
                <h4><a :href="mapLink(stop.place.point.lng, stop.place.point.lat, stop.place.name)"
                    target="_blank" rel="noopener noreferrer">{{ stop.place.name }} <ArrowUpRight :size="14" /></a></h4>
                <p v-if="stop.place.address" class="muted small">{{ stop.place.address }}</p>
                <p>{{ stop.note }}</p>
                <p class="muted small">活动估算 {{ money(stop.estimatedCost) }} · 地点查询于 {{ dateFormat.format(new Date(stop.placeCheckedAt)) }}</p>
              </div>
              <button class="icon-button lock-button" type="button" :aria-pressed="stop.locked"
                :aria-label="(stop.locked ? '解锁' : '锁定') + stop.place.name"
                :title="stop.locked ? '解锁地点' : '锁定地点'" :disabled="busy" @click="emit('lock', stop)">
                <component :is="stop.locked ? LockKeyhole : LockKeyholeOpen" :size="17" />
              </button>
            </div>
          </li>
        </ol>
      </section>
      <aside class="day-aside">
        <RouteSheet :day="day" :active="active" />
        <section class="budget" aria-labelledby="budget-title">
          <h3 id="budget-title"><WalletCards :size="17" /> 当地预算</h3>
          <dl>
            <div v-for="[label, value] in categories" :key="label"><dt>{{ label }}</dt><dd>{{ money(value) }}</dd></div>
            <div class="budget-total"><dt>总计 / 上限</dt><dd>{{ money(total) }} / {{ money(trip.plan.request.budget) }}</dd></div>
          </dl>
          <p class="muted small">费用为全组估算。出发前请核实票价、开放时间与预约要求。</p>
        </section>
      </aside>
    </div>

    <form class="revision" aria-labelledby="revision-title" @submit.prevent="submit">
      <h3 id="revision-title"><Pencil :size="17" /> 调整安排</h3>
      <fieldset :disabled="busy || !ready">
        <p class="muted">选择要改的日期。跨日移动时选两天，已锁定的地点会保留。</p>
        <div class="revision-days">
          <label v-for="item in trip.plan.days" :key="item.number" class="check-chip">
            <input v-model="selected" type="checkbox" :value="item.number" /> 第 {{ item.number }} 天
          </label>
        </div>
        <label class="field"><span>调整要求</span><textarea v-model="instruction" maxlength="400" rows="2"
            placeholder="例如：第一天下午安排轻松一点，保留已锁定的景点" required /></label>
        <button class="button primary" type="submit" :disabled="!selected.length || !instruction.trim()">
          <Pencil :size="16" /> 应用调整
        </button>
      </fieldset>
      <p v-if="!ready" class="muted small">配置实时服务后即可调整。</p>
    </form>

    <details class="evidence">
      <summary>数据来源及本次调用</summary>
      <p class="muted">高德地图提供地点与路线，模型负责拟定安排。以下为最近一次生成或调整的查询次数。</p>
      <dl>
        <div v-for="item in trip.plan.evidence" :key="item.step"><dt>{{ sourceNames[item.step] ?? item.step }}</dt><dd>{{ item.calls }} 次</dd></div>
        <div><dt>生成时间</dt><dd>{{ dateFormat.format(new Date(trip.plan.generatedAt)) }}</dd></div>
      </dl>
    </details>
    <div class="trip-footer">
      <span class="muted small">已保存 · 版本 {{ trip.revision }}</span>
      <button v-if="!confirmDelete" class="text-button danger" type="button" :disabled="busy" @click="confirmDelete = true"><Trash2 :size="15" /> 删除行程</button>
      <template v-else>
        <button class="button subtle small-button" type="button" @click="confirmDelete = false">取消</button>
        <button class="button destructive small-button" type="button" :disabled="busy" @click="emit('remove')">确认删除</button>
      </template>
    </div>
  </section>
</template>
