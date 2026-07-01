const AVATAR_PALETTE = ['#2563eb', '#7c3aed', '#db2777', '#ea580c', '#059669', '#0891b2', '#4f46e5']

export function userInitials(displayName?: string, username?: string): string {
  const source = (displayName || username || '?').trim()
  if (!source) return '?'
  if (/[\u4e00-\u9fff]/.test(source[0])) return source[0]
  const parts = source.split(/\s+/).filter(Boolean)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  return source.slice(0, 2).toUpperCase()
}

export function avatarColor(seed?: string): string {
  if (!seed) return AVATAR_PALETTE[0]
  let hash = 0
  for (let i = 0; i < seed.length; i += 1) {
    hash = seed.charCodeAt(i) + ((hash << 5) - hash)
  }
  return AVATAR_PALETTE[Math.abs(hash) % AVATAR_PALETTE.length]
}

export const ROLE_TAG_COLORS: Record<string, string> = {
  ADMIN: 'red',
  USER: 'blue',
  VIP1: 'gold',
  VIP2: 'orange',
  VIP3: 'purple',
}
