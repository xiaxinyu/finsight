import { getJson } from './client'

export type AuthSession = {
  authenticated: boolean
  username?: string
  roles?: string[]
  admin?: boolean
}

export async function fetchAuthSession(): Promise<AuthSession> {
  return getJson<AuthSession>('/api/v1/auth/me')
}

export async function fetchCsrfToken(): Promise<string | null> {
  const data = await getJson<{ token?: string }>('/api/v1/auth/csrf')
  return data?.token ?? null
}
