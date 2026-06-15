import type { ProfileDimension } from '../../api/analytics'

export type ProfileActionLink = {
  label: string
  path: string
  type: string
}

export function profileActionLinks(dim: ProfileDimension | undefined): ProfileActionLink[] {
  if (!dim?.actions?.length) return []
  return dim.actions
    .map((action) => ({
      label: action.label,
      path: action.payload?.path || '',
      type: action.type,
    }))
    .filter((a) => a.path.length > 0)
}
