<script setup lang="ts">
import { MapPin } from 'lucide-vue-next'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { Day, Point } from './types'

const props = defineProps<{ day: Day; active: string | null }>()
const canvas = ref<HTMLCanvasElement>()
const legs = computed(() => props.day.stops.flatMap(stop => stop.inbound ? [stop.inbound] : []))
const totalDistance = computed(() => legs.value.reduce((sum, leg) => sum + leg.meters, 0))
const totalMinutes = computed(() => legs.value.reduce((sum, leg) => sum + leg.minutes, 0))
let observer: ResizeObserver | undefined

function draw() {
  const element = canvas.value
  if (!element) return
  const width = element.clientWidth
  const height = element.clientHeight
  if (!width || !height) return
  const pixelRatio = Math.min(2, window.devicePixelRatio || 1)
  element.width = width * pixelRatio
  element.height = height * pixelRatio
  const ctx = element.getContext('2d')
  if (!ctx) return
  ctx.scale(pixelRatio, pixelRatio)
  const points = [...props.day.stops.map(stop => stop.place.point), ...legs.value.flatMap(route => route.line)]
  if (!points.length) return

  const longitude = points.map(point => point.lng)
  const latitude = points.map(point => point.lat)
  const centralLng = (Math.min(...longitude) + Math.max(...longitude)) / 2
  const centralLat = (Math.min(...latitude) + Math.max(...latitude)) / 2
  const horizontal = Math.cos(centralLat * Math.PI / 180)
  const scale = Math.min((width - 56) / Math.max(0.0004, (Math.max(...longitude) - Math.min(...longitude)) * horizontal),
    (height - 56) / Math.max(0.0004, Math.max(...latitude) - Math.min(...latitude)))
  const project = (point: Point): [number, number] => [
    width / 2 + (point.lng - centralLng) * horizontal * scale,
    height / 2 - (point.lat - centralLat) * scale,
  ]

  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  for (const route of legs.value) {
    if (route.line.length < 2) continue
    ctx.beginPath()
    route.line.forEach((point, index) => {
      const [x, y] = project(point)
      if (index === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    })
    ctx.lineWidth = 5
    ctx.strokeStyle = '#faf9f2'
    ctx.stroke()
    ctx.lineWidth = 2.7
    ctx.strokeStyle = '#247a66'
    ctx.setLineDash(route.mode === 'WALK' ? [4, 5] : [])
    ctx.stroke()
    ctx.setLineDash([])
  }

  props.day.stops.forEach((stop, index) => {
    const [x, y] = project(stop.place.point)
    const active = props.active === stop.place.id
    ctx.beginPath()
    ctx.arc(x, y, active ? 14 : 11, 0, Math.PI * 2)
    ctx.fillStyle = active ? '#964827' : '#174d40'
    ctx.fill()
    ctx.lineWidth = 2
    ctx.strokeStyle = '#fcfbf7'
    ctx.stroke()
    ctx.fillStyle = '#ffffff'
    ctx.font = '600 11px sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(String(index + 1), x, y + 0.5)
  })
}

onMounted(() => {
  observer = new ResizeObserver(draw)
  if (canvas.value) observer.observe(canvas.value)
  draw()
})
onBeforeUnmount(() => observer?.disconnect())
watch(() => [props.day, props.active], draw, { flush: 'post' })
</script>

<template>
  <section class="route-sheet" aria-labelledby="route-title">
    <h3 id="route-title" tabindex="-1"><MapPin :size="17" /> 当日路线</h3>
    <canvas ref="canvas" role="img" :aria-label="day.stops.map((stop, i) => (i + 1) + ' ' + stop.place.name).join('，')" />
    <dl v-if="legs.length" class="route-metrics">
      <div><dt>预计路程</dt><dd>{{ (totalDistance / 1000).toFixed(1) }} 公里</dd></div>
      <div><dt>预计路上耗时</dt><dd>{{ totalMinutes }} 分钟</dd></div>
    </dl>
    <p class="muted small">地点与路段来自高德地图。路线图按坐标绘制，步行以虚线表示。</p>
    <p v-if="legs.some(leg => !leg.line.length)" class="muted small">部分路段未返回轨迹，可查看行程中的耗时。</p>
  </section>
</template>
