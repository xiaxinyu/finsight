export interface NormalizedResult<T = unknown> {
  ok: boolean
  data: T
  message: string
}

export interface CollectionResult<T> {
  total: number
  rows: T[]
}

export interface CommonResult {
  code?: number
  message?: string
  returnCode?: string
  returnMessage?: string
  data?: unknown
}

export function normalizeResult<T = unknown>(obj: unknown): NormalizedResult<T> {
  if (!obj || typeof obj !== 'object') {
    return { ok: false, data: null as T, message: 'Invalid response' }
  }
  const r = obj as CommonResult
  if (typeof r.code !== 'undefined') {
    return {
      ok: Number(r.code) === 20000 || Number(r.code) === 200,
      data: (r.data ?? null) as T,
      message: r.message || '',
    }
  }
  if (r.returnCode === 'success') {
    return { ok: true, data: (r.data ?? r.returnMessage ?? null) as T, message: r.returnMessage || '' }
  }
  if (Array.isArray(obj)) {
    return { ok: true, data: obj as T, message: '' }
  }
  return { ok: true, data: obj as T, message: '' }
}

export function parseJsonObject<T extends Record<string, unknown> = Record<string, unknown>>(raw: unknown): T | null {
  if (raw == null || raw === '') return null
  if (typeof raw === 'object' && !Array.isArray(raw)) return raw as T
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as T) : null
    } catch {
      return null
    }
  }
  return null
}

export function parseJsonArray(raw: unknown): unknown[] {
  if (raw == null || raw === '') return []
  if (Array.isArray(raw)) return raw
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }
  return []
}

export function isCollectionResult<T>(obj: unknown): obj is CollectionResult<T> {
  return !!obj && typeof obj === 'object' && 'rows' in obj && Array.isArray((obj as CollectionResult<T>).rows)
}
