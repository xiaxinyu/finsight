import type { ThemeConfig } from 'antd'

export const finsightTheme: ThemeConfig = {
  token: {
    colorPrimary: '#2563eb',
    colorSuccess: '#10b981',
    colorWarning: '#f59e0b',
    colorError: '#dc2626',
    borderRadius: 8,
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
  },
  components: {
    Layout: {
      siderBg: '#0b1f3a',
      triggerBg: '#0b1f3a',
    },
    Table: {
      cellFontSizeSM: 12,
    },
  },
}
