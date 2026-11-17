<script setup lang="ts">
import { ArrowRight, BookOpen, CalendarDays, CircleAlert, Compass, ListChecks, LogIn, LogOut, MapPin, Plus, RotateCcw, Route } from 'lucide-vue-next'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { api, ApiError } from './api'
import AuthDialog from './AuthDialog.vue'
import Itinerary from './Itinerary.vue'
import PlanForm from './PlanForm.vue'
import type { Account, Job, Request as TripRequest, Status, Stop, Trip, TripSummary } from './types'

const page = ref<'planner' | 'library'>('planner')
const account = ref<Account | null>(null)
const status = ref<Status | null>(null)
const authOpen = ref(false)
const trip = ref<Trip | null>(null)
const list = ref<TripSummary[]>([])
const historyBusy = ref(false)
const more = ref(false)
const pendingRequest = ref<TripRequest | null>(null)
const busy = ref(false)
const job = ref<Job | null>(null)
const notice = ref('')
const issue = ref('')
const failureCode = ref('')
const loadingStatus = ref(false)
const workspace = ref<HTMLElement>()
let pollController: AbortController | null = null
let pollTimer: number | undefined
let activeJob: string | null = null
const retrySubmit = ref<(() => Promise<void>) | null>(null)
let lastSubmission: { path: string; body: unknown; id: string } | null = null
const steps: Record<string, string> = {
  queued: '等待规划服务', city: '查询目的地', places: '核实必去地点', planning: '查询地点并拟定行程',
  routing: '查询路段并校验时间', repairing: '修正未满足的行程约束', saving: '保存行程', saved: '行程已保存',
}
const step = computed(() => job.value ? steps[job.value.step] ?? '正在处理' : '正在连接规划服务')
const resumeAvailable = computed(() => !!job.value && ['QUEUED', 'RUNNING'].includes(job.value.state) && !busy.value)
const ready = computed(() => status.value?.plannerConfigured === true)
const storedKey = (id: string) => 'travel-pending:' + id

function storeJob(id: string | null) {
  if (!account.value) return
  try {
    if (id) sessionStorage.setItem(storedKey(account.value.id), id)
    else sessionStorage.removeItem(storedKey(account.value.id))
  } catch { /* The application also works with browser storage disabled. */ }
}

function storedJob() {
  if (!account.value) return null
  try { return sessionStorage.getItem(storedKey(account.value.id)) } catch { return null }
}

function setIssue(error: unknown) {
  issue.value = error instanceof Error ? error.message : '操作未完成，请重试。'
  failureCode.value = error instanceof ApiError ? error.code : ''
  if (failureCode.value === 'SIGN_IN_REQUIRED') {
    storeJob(null)
    account.value = null
    trip.value = null
    list.value = []
    authOpen.value = true
  }
}

function clearMessages() {
  issue.value = ''
  failureCode.value = ''
  notice.value = ''
}

async function refreshStatus() {
  loadingStatus.value = true
  try { status.value = await api<Status>('/status') }
  catch (error) { setIssue(error) }
  finally { loadingStatus.value = false }
}

async function initialize() {
  await Promise.all([
    refreshStatus(),
    api<{ account: Account | null }>('/auth/me').then(result => {
      account.value = result.account
      const id = storedJob()
      if (id) resume(id)
    }).catch(setIssue),
  ])
}

function plannerView() {
  page.value = 'planner'
  clearMessages()
}

async function libraryView() {
  page.value = 'library'
  clearMessages()
  if (account.value) await loadHistory()
}

async function loadHistory(append = false) {
  if (historyBusy.value) return
  historyBusy.value = true
  try {
    const entries = await api<TripSummary[]>('/trips?offset=' + (append ? list.value.length : 0))
    list.value = append ? [...list.value, ...entries] : entries
    more.value = entries.length === 25
  } catch (error) { setIssue(error) }
  finally { historyBusy.value = false }
}

async function readTrip(id: string) {
  clearMessages()
  try {
    const opened = await api<Trip>('/trips/' + id)
    trip.value = opened
    page.value = 'planner'
    await revealTrip()
  } catch (error) { setIssue(error) }
}

async function revealTrip() {
  await nextTick()
  const heading = document.getElementById('itinerary-title')
  heading?.focus({ preventScroll: true })
  const target = window.matchMedia('(max-width: 730px)').matches ? heading : workspace.value
  target?.scrollIntoView({ behavior: 'instant', block: 'start' })
}

function authClose() {
  authOpen.value = false
  pendingRequest.value = null
}

function authenticated(user: Account) {
  authOpen.value = false
  account.value = user
  const request = pendingRequest.value
  pendingRequest.value = null
  if (page.value === 'library') void loadHistory()
  else if (request) void createTrip(request)
  else {
    const id = storedJob()
    if (id) resume(id)
  }
}

async function logout() {
  if (busy.value) return
  try {
    await api('/auth/logout', { method: 'POST' })
    storeJob(null)
    account.value = null
    trip.value = null
    list.value = []
    job.value = null
    activeJob = null
    retrySubmit.value = null
    lastSubmission = null
    plannerView()
  } catch (error) { setIssue(error) }
}

async function createTrip(request: TripRequest) {
  if (busy.value || !ready.value) return
  if (!account.value) {
    pendingRequest.value = request
    authOpen.value = true
    return
  }
  await start('/trips', request)
}

async function reviseTrip(days: number[], instruction: string) {
  if (!trip.value || !ready.value || busy.value) return
  await start('/trips/' + trip.value.id + '/revisions', { revision: trip.value.revision, days, instruction })
}

async function start(path: string, body: unknown, id = crypto.randomUUID()) {
  busy.value = true
  clearMessages()
  job.value = null
  retrySubmit.value = null
  lastSubmission = { path, body, id }
  try {
    const result = await api<Job>(path, {
      method: 'POST', headers: { 'Idempotency-Key': id }, body: JSON.stringify(body),
    })
    activeJob = result.id
    storeJob(result.id)
    await handleJob(result)
  } catch (error) {
    busy.value = false
    setIssue(error)
    if (error instanceof ApiError && (error.retryable || error.code === 'NETWORK')) {
      retrySubmit.value = () => start(path, body, id)
    }
  }
}

function schedulePoll(id: string) {
  window.clearTimeout(pollTimer)
  pollTimer = window.setTimeout(() => void poll(id, 0), 1_200)
}

function resume(id: string) {
  clearMessages()
  activeJob = id
  busy.value = true
  void poll(id, 0)
}

async function poll(id: string, failures: number) {
  pollController?.abort()
  const controller = new AbortController()
  pollController = controller
  try {
    const current = await api<Job>('/jobs/' + id, { signal: controller.signal })
    if (activeJob === id) await handleJob(current)
  } catch (error) {
    if (controller.signal.aborted || activeJob !== id) return
    if (error instanceof ApiError && error.retryable && failures < 2) {
      pollTimer = window.setTimeout(() => void poll(id, failures + 1), 2_000 * (failures + 1))
      return
    }
    busy.value = false
    if (error instanceof ApiError && error.code === 'NOT_FOUND') {
      job.value = null
      activeJob = null
      storeJob(null)
      issue.value = '找不到规划任务。请先检查「我的行程」，如未保存，可重新生成。'
      return
    }
    if (error instanceof ApiError && error.retryable && !job.value) {
      job.value = { id, state: 'RUNNING', step: 'planning', trip: null, error: null }
    }
    setIssue(error)
  }
}

async function handleJob(result: Job) {
  job.value = result
  if (result.state === 'QUEUED' || result.state === 'RUNNING') {
    schedulePoll(result.id)
    return
  }
  busy.value = false
  activeJob = null
  storeJob(null)
  if (result.state === 'FAILED') {
    if (result.error) {
      setIssue(new ApiError(result.error))
      const previous = lastSubmission
      if (result.error.retryable && previous && result.id === previous.id) {
        retrySubmit.value = () => start(previous.path, previous.body)
      }
    }
    return
  }
  if (result.trip) {
    trip.value = result.trip
    page.value = 'planner'
    notice.value = '行程已保存到「我的行程」。'
    await revealTrip()
  }
}

async function lockStop(stop: Stop) {
  if (!trip.value || busy.value) return
  busy.value = true
  clearMessages()
  try {
    trip.value = await api<Trip>('/trips/' + trip.value.id + '/stops/' + encodeURIComponent(stop.place.id), {
      method: 'PATCH', body: JSON.stringify({ revision: trip.value.revision, locked: !stop.locked }),
    })
    notice.value = stop.locked ? '已解锁地点。' : '已锁定地点和时间。'
  } catch (error) { setIssue(error) }
  finally { busy.value = false }
}

async function removeTrip() {
  if (!trip.value || busy.value) return
  busy.value = true
  clearMessages()
  try {
    await api('/trips/' + trip.value.id + '?revision=' + trip.value.revision, { method: 'DELETE' })
    trip.value = null
    job.value = null
    await libraryView()
    notice.value = '行程已删除。'
  } catch (error) { setIssue(error) }
  finally { busy.value = false }
}

onMounted(() => void initialize())
onBeforeUnmount(() => {
  pollController?.abort()
  window.clearTimeout(pollTimer)
})
</script>

<template>
  <a class="skip-link" href="#main">跳转到主要内容</a>
  <header class="topbar">
    <div class="topbar-inner">
      <button class="brand" type="button" aria-label="路书首页" @click="plannerView">
        <span class="brand-mark"><Compass :size="22" :stroke-width="1.65" /></span><span>路书</span>
      </button>
      <nav class="navigation" aria-label="主导航">
        <button type="button" :aria-current="page === 'planner' ? 'page' : undefined" @click="plannerView"><Route :size="17" /> <span>行程规划</span></button>
        <button type="button" :aria-current="page === 'library' ? 'page' : undefined" @click="libraryView"><BookOpen :size="17" /> <span>我的行程</span></button>
      </nav>
      <div class="account">
        <span v-if="account" class="account-name">{{ account.displayName }}</span>
        <button v-if="account" type="button" class="icon-button" aria-label="退出登录" title="退出登录" :disabled="busy" @click="logout"><LogOut :size="18" /></button>
        <button v-else class="button subtle login-button" type="button" @click="authOpen = true"><LogIn :size="16" /> 登录</button>
      </div>
    </div>
  </header>

  <main id="main" class="container">
    <section v-if="page === 'planner'" class="hero" aria-labelledby="page-title">
      <img src="/west-lake.webp" alt="" width="1350" height="900" fetchpriority="high" />
      <div class="hero-text">
        <h1 id="page-title">规划一趟自由行</h1>
        <p>查询真实地点和路线，排出可以逐日调整的行程。</p>
      </div>
      <a class="photo-label" title="查看西湖照片来源" href="https://commons.wikimedia.org/wiki/File:West_Lake_-_Hangzhou,_China.jpg"
        target="_blank" rel="noopener noreferrer"><MapPin :size="14" /> 杭州 · 西湖</a>
    </section>

    <div ref="workspace" class="workspace-anchor">
      <div v-if="status && !ready" class="service-note" role="status">
        <CircleAlert :size="17" /><span>实时规划暂不可用。部署者配置模型与高德服务后即可生成。</span>
        <button type="button" class="text-button" :disabled="loadingStatus" @click="refreshStatus"><RotateCcw :size="14" /> 重新检测</button>
      </div>
      <div v-if="issue" class="notice error-notice" role="alert">
        <CircleAlert :size="19" /><div><strong v-if="job?.state === 'FAILED'">{{ step }}未完成</strong><p>{{ issue }}</p></div>
        <button v-if="resumeAvailable && job && activeJob" class="text-button" type="button" @click="resume(job.id)">继续查询任务</button>
        <button v-else-if="retrySubmit" class="text-button" type="button" @click="retrySubmit()">重试请求</button>
        <button v-else-if="failureCode === 'REVISION_CONFLICT' && trip" class="text-button" type="button" @click="readTrip(trip.id)">打开最新版本</button>
      </div>
      <p v-if="notice" class="notice success-notice" role="status">{{ notice }}</p>
      <div v-if="busy && (job || activeJob)" class="notice running-notice" role="status" aria-live="polite">
        <span class="working-dot"></span><div><strong>{{ step }}</strong><p>任务可能需要几分钟，可以随时查看已保存行程。</p></div>
      </div>
    </div>

    <div v-show="page === 'planner'" class="workbench">
      <PlanForm :busy="busy" :ready="ready" :signed-in="!!account" @submit="createTrip" />
      <Itinerary v-if="trip" :trip="trip" :busy="busy" :ready="ready" @revise="reviseTrip" @lock="lockStop" @remove="removeTrip" />
      <section v-else class="panel reading" aria-labelledby="reading-title">
        <header class="panel-head">
          <h2 id="reading-title">每日行程</h2>
          <span class="section-indicator"><span></span>{{ busy ? '正在规划' : '等待生成' }}</span>
        </header>
        <div v-if="busy" class="reading-empty" :aria-busy="true">
          <span class="empty-icon"><Route :size="33" :stroke-width="1.4" /></span>
          <h3>{{ step }}</h3>
          <div class="skeleton" aria-hidden="true"><span></span><span></span><span></span></div>
        </div>
        <div v-else class="reading-empty">
          <span class="empty-icon"><Route :size="34" :stroke-width="1.45" /></span>
          <h3>在这里查看你的路线</h3>
          <p>填写行程设置并生成，就能查看每天去哪里、<br class="desktop-break" />路上需要多久，以及费用估算。</p>
        </div>
        <ol class="process" aria-label="行程生成步骤">
          <li><MapPin :size="19" /><span>查找地点</span></li>
          <li><Route :size="19" /><span>计算路段</span></li>
          <li><ListChecks :size="19" /><span>检查时间与预算</span></li>
        </ol>
      </section>
    </div>

    <section v-if="page === 'library'" class="panel library" aria-labelledby="library-title">
      <header class="panel-head">
        <h1 id="library-title">我的行程</h1>
        <button class="button primary" type="button" @click="plannerView"><Plus :size="17" /> 新规划</button>
      </header>
      <div v-if="!account" class="reading-empty">
        <span class="empty-icon"><BookOpen :size="30" /></span>
        <h2>登录后查看行程</h2><button class="button primary" type="button" @click="authOpen = true"><LogIn :size="16" /> 登录</button>
      </div>
      <p v-else-if="historyBusy && !list.length" class="reading-empty" role="status">正在读取你的行程…</p>
      <div v-else-if="!list.length" class="reading-empty">
        <span class="empty-icon"><CalendarDays :size="30" /></span>
        <h2>这里还没有行程</h2><button class="button subtle" type="button" @click="plannerView">开始规划 <ArrowRight :size="16" /></button>
      </div>
      <ul v-else class="saved-trips">
        <li v-for="item in list" :key="item.id">
          <button type="button" @click="readTrip(item.id)" :disabled="busy">
            <span><strong>{{ item.title }}</strong><small>{{ item.city }} · {{ item.startDate }}出发 · {{ item.days }} 天</small></span>
            <ArrowRight :size="18" />
          </button>
        </li>
      </ul>
      <button v-if="more" type="button" class="button subtle more" :disabled="historyBusy" @click="loadHistory(true)">加载更多</button>
    </section>
    <footer class="footer">
      <span>路书 · 自由行规划</span><span>地点和路线数据由高德地图提供</span>
    </footer>
  </main>
  <AuthDialog :open="authOpen" :registration-open="status?.registrationOpen ?? false" @close="authClose" @authenticated="authenticated" />
</template>
