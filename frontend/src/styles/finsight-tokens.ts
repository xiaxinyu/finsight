import type { ThemeConfig } from 'antd'

export const finsightColors = {
  primary: '#1d4ed8',
  income: '#059669',
  expense: '#d97706',
  sider: '#0f172a',
  canvas: '#f1f5f9',
  border: '#e2e8f0',
  text: '#0f172a',
  textSecondary: '#64748b',
}

export const finsightTheme: ThemeConfig = {
  token: {
    colorPrimary: finsightColors.primary,
    colorSuccess: finsightColors.income,
    colorWarning: finsightColors.expense,
    colorError: '#dc2626',
    colorBgLayout: finsightColors.canvas,
    colorBgContainer: '#ffffff',
    colorBorder: finsightColors.border,
    colorText: finsightColors.text,
    colorTextSecondary: finsightColors.textSecondary,
    borderRadius: 6,
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    fontSize: 13,
    controlHeight: 30,
  },
  components: {
    Layout: {
      siderBg: finsightColors.sider,
      triggerBg: finsightColors.sider,
      headerHeight: 48,
    },
    Table: {
      cellFontSizeSM: 12,
      cellPaddingBlockSM: 6,
      cellPaddingInlineSM: 8,
    },
    Button: {
      paddingInlineSM: 10,
    },
  },
}
