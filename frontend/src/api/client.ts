import { normalizeResult } from './normalize'

export class ApiError extends Error {
  status: number
  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

let onUnauthorized: (() => void) | null = null

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

function isHtmlLoginPage(text: string, res: Response): boolean {
  const url = res.url || ''
  if (url.includes('/app/login') || url.includes('/login')) return true
  const lower = text.trim().slice(0, 200).toLowerCase()
  return lower.startsWith('<!doctype') || lower.startsWith('<html')
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
    const res = await fetch('/api/v1/cards/list', { credentials: 'include' })
    if (res.status === 401 || res.status === 403) return false
    const ct = res.headers.get('content-type') || ''
    if (!ct.includes('application/json')) return false
    return res.ok
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
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
    credentials: 'include',
    body: body.toString(),
  })
  return handleResponse<T>(res)
}

export async function postJson<T = unknown>(url: string, data: unknown): Promise<T> {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(data),
  })
  return handleResponse<T>(res)
}

export async function putJson<T = unknown>(url: string, data: unknown): Promise<T> {
  const res = await fetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(data),
  })
  return handleResponse<T>(res)
}

export async function deleteReq<T = unknown>(url: string): Promise<T> {
  const res = await fetch(url, { method: 'DELETE', credentials: 'include' })
  return handleResponse<T>(res)
}

export async function postCommon(url: string, params: Record<string, unknown> = {}) {
  const raw = await postForm(url, params)
  const n = normalizeResult(raw)
  if (!n.ok) throw new ApiError(n.message || 'Request failed', 500)
  return n.data
}

export async function uploadFile(url: string, formData: FormData) {
  const res = await fetch(url, { method: 'POST', credentials: 'include', body: formData })
  const raw = await handleResponse(res)
  const n = normalizeResult(raw)
  if (!n.ok) throw new ApiError(n.message || 'Upload failed', 500)
  return n.data
}
