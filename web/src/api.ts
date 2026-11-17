import type { ErrorBody } from './types'

export class ApiError extends Error {
  readonly code: string
  readonly retryable: boolean

  constructor(error: ErrorBody) {
    super(error.message)
    this.code = error.code
    this.retryable = error.retryable
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.method && init.method !== 'GET') headers.set('X-Travel-Client', 'web')
  if (init.body) headers.set('Content-Type', 'application/json')
  let response: Response
  try {
    response = await fetch('/api' + path, { ...init, headers, credentials: 'same-origin' })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiError({ code: 'NETWORK', message: '连接中断，请检查网络后重试。', retryable: true })
  }
  if (response.status === 204) return undefined as T
  const body = await response.json().catch(() => null)
  if (!response.ok || body === null) {
    throw new ApiError({
      code: body?.code ?? (response.status === 401 ? 'SIGN_IN_REQUIRED' : 'UNEXPECTED_RESPONSE'),
      message: body?.message ?? '服务暂时没有响应，请稍后重试。',
      retryable: body?.retryable ?? response.status >= 500,
    })
  }
  return body as T
}

export const money = (amount: number) => '¥' + new Intl.NumberFormat('zh-CN').format(amount)

export function today(addDays = 0): string {
  const shanghai = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Shanghai' }).format(new Date())
  const date = new Date(shanghai + 'T12:00:00Z')
  date.setUTCDate(date.getUTCDate() + addDays)
  return date.toISOString().slice(0, 10)
}

export function mapLink(lng: number, lat: number, name: string): string {
  return 'https://uri.amap.com/marker?' + new URLSearchParams({
    position: lng + ',' + lat,
    name,
    src: 'travel-agent',
    coordinate: 'gaode',
    callnative: '0',
  })
}

export function downloadTrip(title: string, text: string) {
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = title.replace(/[\\/:*?"<>|]/g, '_') + '.txt'
  anchor.click()
  window.setTimeout(() => URL.revokeObjectURL(url), 1_000)
}
