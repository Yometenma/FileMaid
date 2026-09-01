import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from './api'

function response(body: unknown, init: ResponseInit = {}) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
}

afterEach(() => {
  vi.restoreAllMocks()
  document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/'
})

describe('api', () => {
  it('uses same-origin credentials and returns JSON', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(response({ status: 'UP' }))

    await expect(api('/api/v1/system/health')).resolves.toEqual({ status: 'UP' })
    expect(fetchMock).toHaveBeenCalledOnce()
    expect(fetchMock.mock.calls[0][1]?.credentials).toBe('same-origin')
  })

  it('adds the decoded CSRF cookie to mutating requests', async () => {
    document.cookie = 'XSRF-TOKEN=token%3Dvalue; Path=/'
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(response({ success: true }))

    await api('/api/v1/settings', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: '{}',
    })

    const headers = fetchMock.mock.calls[0][1]?.headers as Headers
    expect(headers.get('X-XSRF-TOKEN')).toBe('token=value')
    expect(headers.get('Content-Type')).toBe('application/json')
  })

  it('does not add a CSRF header to safe requests', async () => {
    document.cookie = 'XSRF-TOKEN=secret; Path=/'
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(response({ roots: [] }))

    await api('/api/v1/roots')

    const headers = fetchMock.mock.calls[0][1]?.headers as Headers
    expect(headers.has('X-XSRF-TOKEN')).toBe(false)
  })

  it('surfaces the server error message', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response(
      { error: '密码长度必须在 12-128 个字符之间' },
      { status: 400 },
    ))

    await expect(api('/api/v1/auth/setup', { method: 'POST' }))
      .rejects.toThrow('密码长度必须在 12-128 个字符之间')
  })

  it('falls back to the HTTP status for a non-JSON error', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('bad gateway', { status: 502 }))

    await expect(api('/api/v1/settings')).rejects.toThrow('请求失败 (502)')
  })
})
