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

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.status === 401 || res.status === 403) {
    onUnauthorized?.()
    throw new ApiError('Unauthorized', res.status)
  }
  const contentType = res.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    return res.json() as Promise<T>
  }
  const text = await res.text()
  throw new ApiError(text || res.statusText, res.status)
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
