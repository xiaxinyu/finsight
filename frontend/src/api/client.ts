import { normalizeResult } from './normalize'

export class ApiError extends Error {
  status: number
  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

let onUnauthorized: (() => void) | null = null
let csrfEnabled: boolean | null = null

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

function isHtmlLoginPage(text: string, res: Response): boolean {
  const url = res.url || ''
  if (url.includes('/app/login') || url.includes('/login')) return true
  const lower = text.trim().slice(0, 200).toLowerCase()
  return lower.startsWith('<!doctype') || lower.startsWith('<html')
}

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|;\\s*)${name}=([^;]+)`))
  return match ? decodeURIComponent(match[1]) : null
}

async function ensureCsrfToken(): Promise<string | null> {
  const fromCookie = readCookie('XSRF-TOKEN')
  if (fromCookie) return fromCookie
  try {
    const res = await fetch('/api/v1/auth/csrf', { credentials: 'include' })
    if (!res.ok) return null
    const data = (await res.json()) as { token?: string }
    return data.token ?? readCookie('XSRF-TOKEN')
  } catch {
    return null
  }
}

async function csrfHeaders(method: string): Promise<Record<string, string>> {
  if (method === 'GET' || method === 'HEAD') return {}
  if (csrfEnabled === false) return {}
  const token = await ensureCsrfToken()
  if (!token) return {}
  csrfEnabled = true
  return { 'X-XSRF-TOKEN': token }
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.status === 401 || res.status === 403) {
    onUnauthorized?.()
    throw new ApiError('Session expired — please sign in again', res.status)
  }
  const contentType = res.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    return res.json() as Promise<T>
  }
  const text = await res.text()
  if (isHtmlLoginPage(text, res)) {
    onUnauthorized?.()
    throw new ApiError('Session expired — please sign in again', 401)
  }
  throw new ApiError(text || res.statusText || 'Request failed', res.status)
}

/** Probe whether the current browser session is authenticated for API calls. */
export async function verifySession(): Promise<boolean> {
  try {
    const res = await fetch('/api/v1/auth/me', { credentials: 'include' })
    if (res.status === 401 || res.status === 403) return false
    const ct = res.headers.get('content-type') || ''
    if (!ct.includes('application/json')) return false
    const data = (await res.json()) as { authenticated?: boolean }
    return res.ok && data.authenticated === true
  } catch {
    return false
  }
}

export async function getJson<T = unknown>(url: string): Promise<T> {
  const res = await fetch(url, { credentials: 'include' })
  return handleResponse<T>(res)
}

export async function postForm<T = unknown>(url: string, params: Record<string, unknown> = {}): Promise<T> {
  const body = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v != null && v !== '') body.append(k, String(v))
  })
  const headers: Record<string, string> = {
    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    ...(await csrfHeaders('POST')),
  }
  const res = await fetch(url, {
    method: 'POST',
    headers,
    credentials: 'include',
    body: body.toString(),
  })
  return handleResponse<T>(res)
}

export async function postJson<T = unknown>(url: string, data: unknown): Promise<T> {
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(await csrfHeaders('POST')),
    },
    credentials: 'include',
    body: JSON.stringify(data),
  })
  return handleResponse<T>(res)
}

export async function putJson<T = unknown>(url: string, data: unknown): Promise<T> {
  const res = await fetch(url, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...(await csrfHeaders('PUT')),
    },
    credentials: 'include',
    body: JSON.stringify(data),
  })
  return handleResponse<T>(res)
}

export async function deleteReq<T = unknown>(url: string): Promise<T> {
  const res = await fetch(url, {
    method: 'DELETE',
    credentials: 'include',
    headers: await csrfHeaders('DELETE'),
  })
  return handleResponse<T>(res)
}

export async function postCommon(url: string, params: Record<string, unknown> = {}) {
  const raw = await postForm(url, params)
  const n = normalizeResult(raw)
  if (!n.ok) throw new ApiError(n.message || 'Request failed', 500)
  return n.data
}

export async function uploadFile(url: string, formData: FormData) {
  const res = await fetch(url, {
    method: 'POST',
    credentials: 'include',
    headers: await csrfHeaders('POST'),
    body: formData,
  })
  const raw = await handleResponse(res)
  const n = normalizeResult(raw)
  if (!n.ok) throw new ApiError(n.message || 'Upload failed', 500)
  return n.data
}

/** Attach CSRF token to login POST when production CSRF is enabled. */
export async function postLoginForm(username: string, password: string): Promise<Response> {
  const body = new URLSearchParams()
  body.append('username', username)
  body.append('password', password)
  const csrf = await ensureCsrfToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/x-www-form-urlencoded',
  }
  if (csrf) {
    headers['X-XSRF-TOKEN'] = csrf
  }
  return fetch('/authentication/form', {
    method: 'POST',
    credentials: 'include',
    headers,
    body: body.toString(),
    redirect: 'follow',
  })
}
