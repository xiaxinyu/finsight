import { describe, expect, it } from 'vitest'
import { menuItems } from '../config/menuConfig'
import { filterMenuByRole } from './menuRoleFilter'

describe('filterMenuByRole', () => {
  it('hides admin menu for non-admin users', () => {
    const filtered = filterMenuByRole(menuItems, false)
    const flatPaths = JSON.stringify(filtered)
    expect(flatPaths).not.toContain('/admin/users')
    expect(flatPaths).toContain('/profile')
  })

  it('keeps admin menu for admin users', () => {
    const filtered = filterMenuByRole(menuItems, true)
    const flatPaths = JSON.stringify(filtered)
    expect(flatPaths).toContain('/admin/users')
  })
})
