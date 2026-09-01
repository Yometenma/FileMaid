export async function api<T>(url: string, options?: RequestInit): Promise<T> {
  const headers = new Headers(options?.headers)
  const method = (options?.method || 'GET').toUpperCase()
  if (!['GET','HEAD','OPTIONS'].includes(method)) {
    const token = document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('=')
    if (token) headers.set('X-XSRF-TOKEN', decodeURIComponent(token))
  }
  const response = await fetch(url, { ...options, headers, credentials:'same-origin' })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.error || `请求失败 (${response.status})`)
  }
  return response.json()
}

export type Root = { id: string; path: string; writable: boolean }
export type MediaFile = { path: string; kind: string; size: number }
export type Preview = {
  source: string
  target: string
  warnings: string[]
  media: { type: string; title: string; year?: number; season?: number; episodes?: number[] }
}
