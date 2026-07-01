import { useMemo, useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Collapse, Segmented, Select, Tag, Tooltip, Typography } from 'antd'
import { ArrowRightOutlined, CreditCardOutlined, DownloadOutlined, RiseOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { fetchDebtTrends } from '../../api/analytics'
import type { CategoryYearMatrixRow, DebtTrendMover, DebtYoYCard } from '../../utils/debtTrends'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { FsChart } from '../../components/FsChart'
import { FsDataTable } from '../../components/FsDataTable'
import { UnifiedDrillDrawer } from '../../components/ReportDrillDrawer'
import { buildReportDrillContext } from '../../components/drilldown/buildDrillContext'
import { useDrillDown } from '../../hooks/useDrillDown'
import { formatMoney } from '../../utils/format'
import { DeltaPercentCell } from '../../components/FsTableCellViews'
import {
  buildCategoryYearTrendChart,
  buildDebtBalanceChart,
  buildDebtInsights,
  buildDebtYearChart,
  buildDebtYoYCards,
  buildNetDebtLineChart,
  buildRepaymentMoverChart,
  debtDirectionLabel,
  debtDirectionTone,
  debtMatrixTotals,
  downloadCategoryYearMatrixCsv,
  moverKey,
  moverLabel,
  officialDebtTotals,
  orderedDebtCards,
  trendChartHeight,
  yearColumnLabel,
} from '../../utils/debtTrends'
import { REPORT_METRIC_HINTS } from '../../components/MetricExplanation'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'

type DebtTrendsReportProps = {
  title: string
  subtitle?: string
}

type MatrixView = 'repayment' | 'borrowing'
type YearsShown = 2 | 3 | 4 | 5

function formatDelta(card: DebtYoYCard): string {
  return formatMoney(card.deltaAmount)
}

function TrendSectionHead({
  title,
  description,
  actions,
}: {
  title: string
  description?: string
  actions?: ReactNode
}) {
  return (
    <header className="fs-trend-section-head">
      <div className="fs-trend-section-head__text">
        <Typography.Title level={5} className="fs-trend-section-head__title">{title}</Typography.Title>
        {description && (
          <Typography.Text type="secondary" className="fs-trend-section-head__desc">{description}</Typography.Text>
        )}
      </div>
      {actions && <div className="fs-trend-section-head__actions">{actions}</div>}
    </header>
  )
}

export function DebtTrendsReport({ title, subtitle }: DebtTrendsReportProps) {
  const { open: drillOpen, context: drillContext, openDrill, closeDrill } = useDrillDown()
  const viewportH = useViewportTableHeight(320)
  const [toYear, setToYear] = useState(dayjs().year())
  const [yearsShown, setYearsShown] = useState<YearsShown>(3)
  const [matrixView, setMatrixView] = useState<MatrixView>('repayment')

  const fromYear = toYear - 1
  const historyFromYear = toYear - yearsShown + 1

  const { data, isLoading, isFetching, isError, error } = useQuery({
    queryKey: ['debt-trends', fromYear, toYear, historyFromYear],
    queryFn: () => fetchDebtTrends(fromYear, toYear, historyFromYear),
  })

  const loading = isLoading || isFetching
  const yoyCards = useMemo(() => (data ? orderedDebtCards(buildDebtYoYCards(data)) : []), [data])
  const insights = useMemo(() => (data ? buildDebtInsights(data) : []), [data])
  const matrix = useMemo(() => {
    if (!data) return undefined
    return matrixView === 'repayment' ? data.repaymentTypeMatrix : data.borrowingTypeMatrix
  }, [data, matrixView])
  const yearSeries = data?.debtYearSeries ?? []
  const matrixTotals = useMemo(() => (matrix ? debtMatrixTotals(matrix) : {}), [matrix])
  const officialTotals = useMemo(
    () => officialDebtTotals(yearSeries, matrixView),
    [yearSeries, matrixView],
  )

  const yearChart = useMemo(() => (yearSeries.length ? buildDebtYearChart(yearSeries) : {}), [yearSeries])
  const netChart = useMemo(() => (yearSeries.length ? buildNetDebtLineChart(yearSeries) : {}), [yearSeries])
  const balanceChart = useMemo(() => (yearSeries.length ? buildDebtBalanceChart(yearSeries) : {}), [yearSeries])
  const debtBalance = data?.debtBalance
  const typeTrendChart = useMemo(() => (matrix ? buildCategoryYearTrendChart(matrix, 6) : {}), [matrix])
  const moverChart = useMemo(
    () => (data?.topRepaymentGrowth.length ? buildRepaymentMoverChart(data.topRepaymentGrowth) : {}),
    [data],
  )
  const movers = data?.topRepaymentGrowth ?? []
  const chartHeight = trendChartHeight(Math.min(movers.length, 8))

  const yearDrillBounds = (year: number) => {
    const pt = yearSeries.find((p) => p.year === year)
    const start = `01/01/${year}`
    const end = pt?.partial && pt.throughDate
      ? `${pt.throughDate.slice(5, 7)}/${pt.throughDate.slice(8, 10)}/${year}`
      : `12/31/${year}`
    return { start, end }
  }

  const openMatrixDrill = (row: CategoryYearMatrixRow, year: number) => {
    const bounds = yearDrillBounds(year)
    const label = yearColumnLabel(year, matrix?.partialYears)
    openDrill(buildReportDrillContext({
      title: `${row.label} · ${label}`,
      metricLabel: row.label,
      params: {
        transactionDateStartStr: bounds.start,
        transactionDateEndStr: bounds.end,
        txnTypes: 'finance',
        semanticFilter: row.tagId,
        consumeName: row.label,
      },
      explanation: [`${matrixView === 'repayment' ? 'Repayment' : 'Borrowing'} in ${label} for ${row.label}.`, REPORT_METRIC_HINTS.expense],
      source: 'report',
      provenance: { reportId: 'debt-trends', sourceView: `${matrixView} type matrix` },
    }))
  }

  const openMoverDrill = (row: DebtTrendMover) => {
    const name = moverLabel(row)
    openDrill(buildReportDrillContext({
      title: `${name} · ${fromYear}→${toYear}`,
      metricLabel: name,
      params: row.drillDown || {},
      explanation: [`Repayment for ${name} changed ${formatMoney(row.deltaAmount)} year over year.`],
      source: 'report',
      provenance: { reportId: 'debt-trends', sourceView: 'repayment movers' },
    }))
  }

  const exportMatrixCsv = () => {
    if (!matrix) return
    downloadCategoryYearMatrixCsv(
      matrix,
      officialTotals,
      `debt-trends-${historyFromYear}-${toYear}-${matrixView}.csv`,
    )
  }

  const matrixCols = useMemo(() => {
    if (!matrix) return []
    const partial = matrix.partialYears
    const latestYear = matrix.years[matrix.years.length - 1]
    const yearCols = matrix.years.map((y) => ({
      title: yearColumnLabel(y, partial),
      key: `y${y}`,
      align: 'right' as const,
      sortType: 'number' as const,
      width: y === latestYear ? 112 : 100,
      className: y === latestYear ? 'fs-col-year--current' : undefined,
      onHeaderCell: () => ({
        className: y === latestYear ? 'fs-col-year--current' : undefined,
      }),
      render: (_: unknown, row: CategoryYearMatrixRow) => {
        const amt = Number(row.amountsByYear[String(y)] ?? 0)
        const share = row.shareByYear?.[String(y)]
        return (
          <Tooltip title={share != null ? `${share.toFixed(1)}% of year total` : undefined}>
            <span className={`fs-trend-matrix-amount${y === latestYear ? ' fs-trend-matrix-amount--current' : ''}`}>
              {formatMoney(amt)}
            </span>
          </Tooltip>
        )
      },
    }))
    return [
      {
        title: 'Debt type',
        dataIndex: 'label',
        sortType: 'text' as const,
        ellipsis: true,
        fixed: 'left' as const,
        width: 148,
        render: (label: string) => (
          <Tooltip title={label}>
            <Typography.Text ellipsis className="fs-trend-matrix-label">{label}</Typography.Text>
          </Tooltip>
        ),
      },
      ...yearCols,
      {
        title: 'Δ%',
        dataIndex: 'yoyPercent',
        align: 'right' as const,
        sortType: 'number' as const,
        width: 68,
        render: (v: number | undefined) => (v == null ? '—' : <DeltaPercentCell value={Number(v)} />),
      },
      {
        title: 'Total Δ',
        dataIndex: 'deltaAmount',
        align: 'right' as const,
        sortType: 'number' as const,
        width: 96,
        render: (v: number) => (
          <span className={v > 0 ? 'fs-delta--up' : v < 0 ? 'fs-delta--down' : undefined}>
            {v >= 0 ? '+' : ''}{formatMoney(v)}
          </span>
        ),
      },
    ]
  }, [matrix])

  const moverCols = [
    {
      title: 'Debt type',
      dataIndex: 'label',
      sortType: 'text' as const,
      ellipsis: true,
      render: (_: unknown, row: DebtTrendMover) => {
        const name = moverLabel(row)
        return (
          <Tooltip title={name}>
            <Typography.Text ellipsis className="fs-trend-matrix-label">{name}</Typography.Text>
          </Tooltip>
        )
      },
    },
    {
      title: yearColumnLabel(fromYear, matrix?.partialYears),
      key: 'fromAmount',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 100,
      render: (_: unknown, row: DebtTrendMover) => (
        row.fromAmount == null ? '—' : formatMoney(row.fromAmount)
      ),
    },
    {
      title: yearColumnLabel(toYear, matrix?.partialYears),
      key: 'toAmount',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 100,
      render: (_: unknown, row: DebtTrendMover) => (
        row.toAmount == null ? '—' : formatMoney(row.toAmount)
      ),
    },
    {
      title: 'Change',
      dataIndex: 'deltaAmount',
      cellType: 'deltaMoney' as const,
      unit: 'CNY',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 104,
    },
    {
      title: 'Δ%',
      dataIndex: 'deltaPercent',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 72,
      render: (_: unknown, row: DebtTrendMover) => (
        <DeltaPercentCell value={Number(row.deltaPercent ?? row.pctChange ?? 0)} />
      ),
    },
  ]

  const yearOptions = [toYear - 2, toYear - 1, toYear, toYear + 1].map((y) => ({ value: y, label: String(y) }))

  return (
    <DataPageLayout
      title={title}
      subtitle={subtitle ? `${subtitle} · ${historyFromYear}–${toYear}` : `${historyFromYear}–${toYear}`}
      icon={<CreditCardOutlined />}
      className="fs-data-page--dense fs-data-page--fill fs-data-page--reports fs-trend-changes fs-debt-trends"
      toolbar={(
        <div className="fs-trend-controls">
          <div className="fs-trend-controls__group">
            <span className="fs-trend-controls__label">Compare year</span>
            <Select
              size="small"
              value={toYear}
              className="fs-trend-controls__select"
              options={yearOptions}
              onChange={setToYear}
            />
            <span className="fs-trend-controls__vs">vs {fromYear}</span>
          </div>
          <span className="fs-trend-controls__divider" aria-hidden />
          <div className="fs-trend-controls__group">
            <span className="fs-trend-controls__label">History</span>
            <Select
              size="small"
              value={yearsShown}
              className="fs-trend-controls__select fs-trend-controls__select--narrow"
              options={[
                { value: 2, label: '2 years' },
                { value: 3, label: '3 years' },
                { value: 4, label: '4 years' },
                { value: 5, label: '5 years' },
              ]}
              onChange={(v) => setYearsShown(v as YearsShown)}
            />
          </div>
          {(data?.compareMode === 'ytd_aligned' || data?.debtPressure.detected) && (
            <>
              <span className="fs-trend-controls__divider" aria-hidden />
              <div className="fs-trend-controls__tags">
                {data?.compareMode === 'ytd_aligned' && (
                  <Tag className="fs-trend-controls__tag">Same period YTD</Tag>
                )}
                {data?.debtPressure.detected && (
                  <Tag color="warning" className="fs-trend-controls__tag">Repayments rising</Tag>
                )}
              </div>
            </>
          )}
        </div>
      )}
    >
      {isError && (
        <Alert type="error" showIcon message="Failed to load debt trends" description={error instanceof Error ? error.message : 'Try another year.'} />
      )}

      {data && (
        <div className="fs-trend-layout">
          {data.compareMode === 'ytd_aligned' && (
            <Alert
              type="info"
              showIcon
              banner
              className="fs-trend-ytd-banner"
              message="Current year uses year-to-date totals for a fair comparison with the same period last year."
            />
          )}

          <section className="fs-trend-section" aria-label="Outstanding debt">
            {debtBalance && (
              <ContentCard className="fs-debt-balance-hero" styles={{ body: { padding: '18px 22px' } }}>
                <div className="fs-debt-balance-hero__grid">
                  <div className="fs-debt-balance-hero__primary">
                    <span className="fs-debt-balance-hero__label">Outstanding debt today</span>
                    <span className="fs-debt-balance-hero__value">{formatMoney(debtBalance.currentLiabilities)}</span>
                    <Typography.Text type="secondary" className="fs-debt-balance-hero__meta">
                      From credit cards &amp; liability accounts · as of {debtBalance.asOfDate ?? 'today'}
                    </Typography.Text>
                  </div>
                  {debtBalance.periodBalanceChange != null && (
                    <div className="fs-debt-balance-hero__change">
                      <span className="fs-debt-balance-hero__label">
                        Since {debtBalance.historyFromYear ?? historyFromYear}
                      </span>
                      <span className={`fs-debt-balance-hero__delta${(debtBalance.periodBalanceChange ?? 0) > 0 ? ' fs-delta--up' : (debtBalance.periodBalanceChange ?? 0) < 0 ? ' fs-delta--down' : ''}`}>
                        {(debtBalance.periodBalanceChange ?? 0) >= 0 ? '+' : ''}
                        {formatMoney(debtBalance.periodBalanceChange ?? 0)}
                      </span>
                      {debtBalance.periodStartBalance != null && (
                        <Typography.Text type="secondary" className="fs-debt-balance-hero__meta">
                          Was {formatMoney(debtBalance.periodStartBalance)} at start of period
                        </Typography.Text>
                      )}
                    </div>
                  )}
                </div>
                {debtBalance.note && (
                  <Typography.Text type="secondary" className="fs-debt-balance-hero__note">
                    {debtBalance.note}
                  </Typography.Text>
                )}
              </ContentCard>
            )}
          </section>

          <section className="fs-trend-section" aria-label="Overview">
            <ContentCard className="fs-trend-overview-card fs-debt-overview-card" styles={{ body: { padding: '20px 24px' } }}>
              <div className="fs-trend-overview">
                {yoyCards.map((card) => (
                  <div
                    key={card.key}
                    className={`fs-trend-stat fs-trend-stat--${card.tone}${card.key === 'repayment' ? ' fs-trend-stat--hero fs-debt-stat--hero' : ''}`}
                    title={card.hint}
                  >
                    <span className="fs-trend-stat__label">{card.label}</span>
                    <div className="fs-trend-stat__values">
                      <span className="fs-trend-stat__from">{formatMoney(card.from)}</span>
                      <ArrowRightOutlined className="fs-trend-stat__arrow" aria-hidden />
                      <span className="fs-trend-stat__to">{formatMoney(card.to)}</span>
                    </div>
                    <span className={`fs-trend-stat__delta${card.deltaAmount >= 0 ? ' fs-delta--up' : ' fs-delta--down'}`}>
                      {formatDelta(card)}
                      <span className="fs-trend-stat__pct">
                        ({card.deltaPercent >= 0 ? '+' : ''}{card.deltaPercent.toFixed(1)}%)
                      </span>
                    </span>
                    {card.hint && <span className="fs-debt-stat__hint">{card.hint}</span>}
                  </div>
                ))}
              </div>
              {insights.length > 0 && (
                <ul className="fs-trend-overview__insights">
                  {insights.slice(0, 3).map((item, i) => (
                    <li key={i} className={item.warn ? 'fs-trend-overview__insight--warn' : undefined}>
                      {item.text}
                    </li>
                  ))}
                </ul>
              )}
            </ContentCard>
          </section>

          {yearSeries.length > 0 && (
            <section className="fs-trend-section" aria-label="Yearly debt flows">
              <TrendSectionHead
                title="Debt trajectory"
                description="Estimated outstanding balance and yearly net flows · negative net = debt reduced"
              />
              <div className="fs-debt-charts-row fs-debt-charts-row--triple">
                <ContentCard className="fs-trend-chart-card fs-debt-chart-card--balance" styles={{ body: { padding: '12px 16px 8px' } }}>
                  <Typography.Text type="secondary" className="fs-debt-chart-caption">Estimated outstanding</Typography.Text>
                  <FsChart
                    profile="compareBars"
                    height={240}
                    loading={loading}
                    option={balanceChart}
                    empty={<EmptyState compact title="No balance estimate" description="Link credit cards with balances." />}
                  />
                </ContentCard>
                <ContentCard className="fs-trend-chart-card" styles={{ body: { padding: '12px 16px 8px' } }}>
                  <Typography.Text type="secondary" className="fs-debt-chart-caption">Borrowing vs repayments</Typography.Text>
                  <FsChart
                    profile="compareBars"
                    height={240}
                    loading={loading}
                    option={yearChart}
                    empty={<EmptyState compact title="No debt flows" />}
                  />
                </ContentCard>
                <ContentCard className="fs-trend-chart-card" styles={{ body: { padding: '12px 16px 8px' } }}>
                  <Typography.Text type="secondary" className="fs-debt-chart-caption">Net flow by year</Typography.Text>
                  <FsChart
                    profile="compareBars"
                    height={240}
                    loading={loading}
                    option={netChart}
                    empty={<EmptyState compact title="No net flow" />}
                  />
                </ContentCard>
              </div>
              <div className="fs-trend-year-strip fs-debt-year-strip">
                {yearSeries.map((pt) => (
                  <div
                    key={pt.year}
                    className={`fs-trend-year-strip__item fs-debt-year-strip__item fs-debt-year-strip__item--${pt.debtDirection ?? 'flat'}`}
                  >
                    <div className="fs-debt-year-strip__head">
                      <span className="fs-trend-year-strip__label">
                        {yearColumnLabel(pt.year, data.repaymentTypeMatrix?.partialYears)}
                      </span>
                      <Tag color={debtDirectionTone(pt.debtDirection)} className="fs-debt-direction-tag">
                        {debtDirectionLabel(pt.debtDirection)}
                      </Tag>
                    </div>
                    {pt.estimatedBalance != null && (
                      <span className="fs-debt-year-strip__balance">{formatMoney(pt.estimatedBalance)} est.</span>
                    )}
                    <span className={`fs-debt-year-strip__net${pt.net < 0 ? ' fs-delta--down' : pt.net > 0 ? ' fs-delta--up' : ''}`}>
                      {formatMoney(pt.net)} net
                    </span>
                    <span className="fs-debt-year-strip__detail">
                      {formatMoney(pt.borrowing)} in · {formatMoney(pt.repayment)} out
                    </span>
                  </div>
                ))}
              </div>
            </section>
          )}

          {matrix && matrix.rows.length > 0 && (
            <section className="fs-trend-section" aria-label="Breakdown by debt type">
              <TrendSectionHead
                title="Breakdown by type"
                description={`${matrixView === 'repayment' ? 'Repayments' : 'New borrowing'} · ${historyFromYear}–${toYear}`}
                actions={(
                  <div className="fs-trend-section-head__actions-inner">
                    <Segmented<MatrixView>
                      value={matrixView}
                      onChange={setMatrixView}
                      options={[
                        { label: 'Repayments', value: 'repayment' },
                        { label: 'Borrowing', value: 'borrowing' },
                      ]}
                    />
                    <Button size="small" type="text" icon={<DownloadOutlined />} onClick={exportMatrixCsv}>
                      Export
                    </Button>
                  </div>
                )}
              />
              <ContentCard className="fs-trend-chart-card fs-trend-chart-card--compact" styles={{ body: { padding: '8px 12px' } }}>
                <FsChart
                  profile="compareBars"
                  height={200}
                  loading={loading}
                  option={typeTrendChart}
                  empty={<EmptyState compact title="No breakdown chart" />}
                />
              </ContentCard>
              <div className="fs-trend-matrix-wrap">
                <FsDataTable
                  columns={matrixCols}
                  dataSource={matrix.rows}
                  rowKey="tagId"
                  loading={loading}
                  size="small"
                  scroll={{ x: 'max-content', y: Math.max(280, viewportH - 80) }}
                  summary={{
                    key: 'Total',
                    ...Object.fromEntries(
                      matrix.years.map((y) => [`y${y}`, officialTotals[String(y)] ?? matrixTotals[String(y)] ?? 0]),
                    ),
                  }}
                  onRow={(record) => ({
                    onClick: () => openMatrixDrill(record, toYear),
                    style: { cursor: 'pointer' },
                  })}
                  rowClassName={() => 'fs-trend-matrix-row'}
                  locale={{
                    emptyText: <EmptyState compact title="No debt history" description="Classify loan and repayment transactions." />,
                  }}
                />
              </div>
            </section>
          )}

          {movers.length > 0 && (
            <section className="fs-trend-section fs-trend-section--secondary">
              <Collapse
                bordered={false}
                className="fs-trend-movers-panel"
                items={[{
                  key: 'movers',
                  label: (
                    <span className="fs-trend-movers-panel__label">
                      <RiseOutlined aria-hidden />
                      Repayment changes · {fromYear} → {toYear}
                    </span>
                  ),
                  children: (
                    <div className="fs-trend-movers">
                      <div className="fs-trend-movers__body">
                        <ContentCard className="fs-trend-chart-card" styles={{ body: { padding: 8 } }}>
                          <FsChart
                            profile="horizontalBar"
                            height={chartHeight}
                            loading={loading}
                            option={moverChart}
                            empty={<EmptyState compact title="No movers" />}
                          />
                        </ContentCard>
                        <div className="fs-trend-movers__table">
                          <FsDataTable
                            columns={moverCols}
                            dataSource={movers}
                            rowKey={moverKey}
                            loading={loading}
                            size="small"
                            onRow={(record) => ({
                              onClick: () => openMoverDrill(record),
                              style: { cursor: 'pointer' },
                            })}
                            rowClassName={() => 'fs-trend-matrix-row'}
                            scroll={movers.length > 8 ? { y: 320 } : undefined}
                          />
                        </div>
                      </div>
                    </div>
                  ),
                }]}
              />
            </section>
          )}
        </div>
      )}

      <UnifiedDrillDrawer open={drillOpen} context={drillContext} onClose={closeDrill} />
    </DataPageLayout>
  )
}
