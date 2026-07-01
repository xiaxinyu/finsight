import { useMemo, useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Collapse, Segmented, Select, Tag, Tooltip, Typography } from 'antd'
import { ArrowRightOutlined, DownloadOutlined, RiseOutlined, WalletOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { fetchIncomeTrends } from '../../api/analytics'
import type { CategoryYearMatrixRow, IncomeTrendMover, IncomeYoYCard } from '../../utils/incomeTrends'
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
  buildIncomeInsights,
  buildIncomeMoverChart,
  buildIncomeTotalYearChart,
  buildIncomeYoYCards,
  downloadCategoryYearMatrixCsv,
  incomeMatrixTotals,
  moverKey,
  moverLabel,
  officialIncomeTotals,
  orderedIncomeCards,
  trendChartHeight,
  yearColumnLabel,
} from '../../utils/incomeTrends'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'

type IncomeTrendsReportProps = {
  title: string
  subtitle?: string
}

type MatrixView = 'classification' | 'categoryL1'
type YearsShown = 2 | 3 | 4 | 5

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

export function IncomeTrendsReport({ title, subtitle }: IncomeTrendsReportProps) {
  const { open: drillOpen, context: drillContext, openDrill, closeDrill } = useDrillDown()
  const viewportH = useViewportTableHeight(320)
  const [toYear, setToYear] = useState(dayjs().year())
  const [yearsShown, setYearsShown] = useState<YearsShown>(3)
  const [matrixView, setMatrixView] = useState<MatrixView>('classification')

  const fromYear = toYear - 1
  const historyFromYear = toYear - yearsShown + 1

  const { data, isLoading, isFetching, isError, error } = useQuery({
    queryKey: ['income-trends', fromYear, toYear, historyFromYear],
    queryFn: () => fetchIncomeTrends(fromYear, toYear, historyFromYear),
  })

  const loading = isLoading || isFetching
  const yoyCards = useMemo(() => (data ? orderedIncomeCards(buildIncomeYoYCards(data)) : []), [data])
  const insights = useMemo(() => (data ? buildIncomeInsights(data) : []), [data])
  const matrix = useMemo(() => {
    if (!data) return undefined
    return matrixView === 'classification' ? data.incomeTypeMatrix : data.categoryL1YearMatrix
  }, [data, matrixView])
  const yearSeries = data?.incomeYearSeries ?? []
  const matrixTotals = useMemo(() => (matrix ? incomeMatrixTotals(matrix) : {}), [matrix])
  const officialTotals = useMemo(() => officialIncomeTotals(yearSeries), [yearSeries])
  const movers = data?.topIncomeGrowth ?? []

  const totalYearChart = useMemo(
    () => (yearSeries.length ? buildIncomeTotalYearChart(yearSeries) : {}),
    [yearSeries],
  )
  const typeTrendChart = useMemo(
    () => (matrix ? buildCategoryYearTrendChart(matrix, 6) : {}),
    [matrix],
  )
  const moverChart = useMemo(
    () => (movers.length ? buildIncomeMoverChart(movers) : {}),
    [movers],
  )
  const chartHeight = trendChartHeight(Math.min(movers.length, 8))

  const yearDrillBounds = (year: number) => {
    const pt = yearSeries.find((p) => p.year === year)
    const start = dayjs(`${year}-01-01`).format('YYYY-MM-DD')
    const end = pt?.partial && pt.throughDate
      ? pt.throughDate.slice(0, 10)
      : dayjs(`${year}-12-31`).format('YYYY-MM-DD')
    return { start, end }
  }

  const openMatrixDrill = (row: CategoryYearMatrixRow, year: number) => {
    const bounds = yearDrillBounds(year)
    const label = yearColumnLabel(year, matrix?.partialYears)
    const params: Record<string, string> = matrixView === 'categoryL1'
      ? {
          transactionDateStartStr: bounds.start,
          transactionDateEndStr: bounds.end,
          txnTypes: 'income',
          consumeID: row.tagId,
        }
      : {
          transactionDateStartStr: bounds.start,
          transactionDateEndStr: bounds.end,
          txnTypes: 'income',
          semanticFilter: row.tagId,
        }
    openDrill(buildReportDrillContext({
      title: `${row.label} · ${label}`,
      metricLabel: row.label,
      params,
      explanation: [`Income in ${label} for ${row.label}.`, 'Excludes transfers and non-P&L flows.'],
      source: 'report',
      provenance: { reportId: 'income-trends', sourceView: matrixView === 'categoryL1' ? 'category matrix' : 'income type matrix' },
    }))
  }

  const openMoverDrill = (row: IncomeTrendMover) => {
    openDrill(buildReportDrillContext({
      title: `${moverLabel(row)} · ${fromYear}→${toYear}`,
      metricLabel: moverLabel(row),
      params: row.drillDown || {},
      explanation: [`${moverLabel(row)} changed ${formatMoney(row.deltaAmount)} year over year.`],
      source: 'report',
      provenance: { reportId: 'income-trends', sourceView: 'income movers' },
    }))
  }

  const exportMatrixCsv = () => {
    if (!matrix) return
    const suffix = matrixView === 'classification' ? 'type' : 'category-l1'
    downloadCategoryYearMatrixCsv(
      matrix,
      officialTotals,
      `income-trends-${historyFromYear}-${toYear}-${suffix}.csv`,
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
      onHeaderCell: () => ({ className: y === latestYear ? 'fs-col-year--current' : undefined }),
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
        title: matrixView === 'classification' ? 'Income type' : 'Category',
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
          <span className={v > 0 ? 'fs-delta--down' : v < 0 ? 'fs-delta--up' : undefined}>
            {v >= 0 ? '+' : ''}{formatMoney(v)}
          </span>
        ),
      },
    ]
  }, [matrix, matrixView])

  const moverCols = [
    {
      title: 'Income type',
      dataIndex: 'label',
      sortType: 'text' as const,
      ellipsis: true,
      render: (_: unknown, row: IncomeTrendMover) => (
        <Typography.Text ellipsis>{moverLabel(row)}</Typography.Text>
      ),
    },
    {
      title: yearColumnLabel(fromYear, matrix?.partialYears),
      key: 'fromAmount',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 100,
      render: (_: unknown, row: IncomeTrendMover) => (
        row.fromAmount == null ? '—' : formatMoney(row.fromAmount)
      ),
    },
    {
      title: yearColumnLabel(toYear, matrix?.partialYears),
      key: 'toAmount',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 100,
      render: (_: unknown, row: IncomeTrendMover) => (
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
      render: (_: unknown, row: IncomeTrendMover) => (
        <DeltaPercentCell value={Number(row.deltaPercent ?? row.pctChange ?? 0)} />
      ),
    },
  ]

  const yearOptions = [toYear - 2, toYear - 1, toYear, toYear + 1].map((y) => ({ value: y, label: String(y) }))

  return (
    <DataPageLayout
      title={title}
      subtitle={subtitle ? `${subtitle} · ${historyFromYear}–${toYear}` : `${historyFromYear}–${toYear}`}
      icon={<WalletOutlined />}
      className="fs-data-page--dense fs-data-page--fill fs-data-page--reports fs-trend-changes fs-income-trends"
      toolbar={(
        <div className="fs-trend-controls">
          <div className="fs-trend-controls__group">
            <span className="fs-trend-controls__label">Compare year</span>
            <Select size="small" value={toYear} className="fs-trend-controls__select" options={yearOptions} onChange={setToYear} />
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
          {(data?.compareMode === 'ytd_aligned' || data?.incomeMomentum.detected) && (
            <>
              <span className="fs-trend-controls__divider" aria-hidden />
              <div className="fs-trend-controls__tags">
                {data?.compareMode === 'ytd_aligned' && <Tag className="fs-trend-controls__tag">Same period YTD</Tag>}
                {data?.incomeMomentum.detected && <Tag color="success" className="fs-trend-controls__tag">Income rising</Tag>}
              </div>
            </>
          )}
        </div>
      )}
    >
      {isError && (
        <Alert type="error" showIcon message="Failed to load income trends" description={error instanceof Error ? error.message : 'Try another year.'} />
      )}

      {data && (
        <div className="fs-trend-layout">
          {data.compareMode === 'ytd_aligned' && (
            <Alert type="info" showIcon banner className="fs-trend-ytd-banner" message="Current year uses year-to-date totals for a fair comparison with the same period last year." />
          )}

          <section className="fs-trend-section" aria-label="Overview">
            <ContentCard className="fs-trend-overview-card fs-income-overview-card" styles={{ body: { padding: '20px 24px' } }}>
              <div className="fs-trend-overview">
                {yoyCards.map((card: IncomeYoYCard) => (
                  <div
                    key={card.key}
                    className={`fs-trend-stat fs-trend-stat--${card.tone}${card.key === 'total' ? ' fs-trend-stat--hero fs-income-stat--hero' : ''}`}
                    title={card.hint}
                  >
                    <span className="fs-trend-stat__label">{card.label}</span>
                    <div className="fs-trend-stat__values">
                      <span className="fs-trend-stat__from">{formatMoney(card.from)}</span>
                      <ArrowRightOutlined className="fs-trend-stat__arrow" aria-hidden />
                      <span className="fs-trend-stat__to">{formatMoney(card.to)}</span>
                    </div>
                    <span className={`fs-trend-stat__delta${card.deltaAmount >= 0 ? ' fs-delta--down' : ' fs-delta--up'}`}>
                      {formatMoney(card.deltaAmount)}
                      <span className="fs-trend-stat__pct">({card.deltaPercent >= 0 ? '+' : ''}{card.deltaPercent.toFixed(1)}%)</span>
                    </span>
                    {card.hint && <span className="fs-income-stat__hint">{card.hint}</span>}
                  </div>
                ))}
              </div>
              {insights.length > 0 && (
                <ul className="fs-trend-overview__insights">
                  {insights.map((item, i) => (
                    <li key={i} className={item.warn ? 'fs-trend-overview__insight--warn' : undefined}>{item.text}</li>
                  ))}
                </ul>
              )}
            </ContentCard>
          </section>

          {yearSeries.length > 0 && (
            <section className="fs-trend-section" aria-label="Yearly income">
              <TrendSectionHead title="Yearly total" description="Real income trend · click a breakdown row for transactions" />
              <ContentCard className="fs-trend-chart-card" styles={{ body: { padding: '12px 16px 8px' } }}>
                <FsChart profile="compareBars" height={240} loading={loading} option={totalYearChart} empty={<EmptyState compact title="No income data" />} />
                <div className="fs-trend-year-strip">
                  {yearSeries.map((pt) => (
                    <div key={pt.year} className="fs-trend-year-strip__item">
                      <span className="fs-trend-year-strip__label">{yearColumnLabel(pt.year, data.incomeTypeMatrix?.partialYears)}</span>
                      <span className="fs-trend-year-strip__value">{formatMoney(pt.amount)}</span>
                    </div>
                  ))}
                </div>
              </ContentCard>
            </section>
          )}

          {matrix && matrix.rows.length > 0 && (
            <section className="fs-trend-section" aria-label="Breakdown">
              <TrendSectionHead
                title="Breakdown"
                description={`${matrixView === 'classification' ? 'By income type' : 'By category'} · ${historyFromYear}–${toYear}`}
                actions={(
                  <div className="fs-trend-section-head__actions-inner">
                    <Segmented<MatrixView>
                      value={matrixView}
                      onChange={setMatrixView}
                      options={[
                        { label: 'Type', value: 'classification' },
                        { label: 'Category', value: 'categoryL1' },
                      ]}
                    />
                    <Button size="small" type="text" icon={<DownloadOutlined />} onClick={exportMatrixCsv}>Export</Button>
                  </div>
                )}
              />
              <ContentCard className="fs-trend-chart-card fs-trend-chart-card--compact" styles={{ body: { padding: '8px 12px' } }}>
                <FsChart profile="compareBars" height={200} loading={loading} option={typeTrendChart} empty={<EmptyState compact title="No breakdown chart" />} />
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
                  locale={{ emptyText: <EmptyState compact title="No income history" description="Import and classify income transactions." /> }}
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
                      Biggest changes · {fromYear} → {toYear}
                    </span>
                  ),
                  children: (
                    <div className="fs-trend-movers">
                      <div className="fs-trend-movers__body">
                        <ContentCard className="fs-trend-chart-card" styles={{ body: { padding: 8 } }}>
                          <FsChart profile="horizontalBar" height={chartHeight} loading={loading} option={moverChart} empty={<EmptyState compact title="No movers" />} />
                        </ContentCard>
                        <div className="fs-trend-movers__table">
                          <FsDataTable
                            columns={moverCols}
                            dataSource={movers}
                            rowKey={moverKey}
                            loading={loading}
                            size="small"
                            onRow={(record) => ({ onClick: () => openMoverDrill(record), style: { cursor: 'pointer' } })}
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
