import { useMemo, useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Collapse, Segmented, Select, Tag, Tooltip, Typography } from 'antd'
import { ArrowRightOutlined, BarChartOutlined, DownloadOutlined, RiseOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { fetchTrends } from '../../api/analytics'
import type { CategoryYearMatrix, CategoryYearMatrixRow, TrendItem, TrendMover, TrendYoYCard } from '../../utils/trendChanges'
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
  buildCategoryContributorChart,
  buildCategoryYearTrendChart,
  buildConsumptionTotalYearChart,
  buildMerchantContributorChart,
  buildTrendInsights,
  buildTrendYoYCards,
  categoryYearMatrixTotals,
  downloadCategoryYearMatrixCsv,
  findTrendItem,
  moverFromTo,
  moverKey,
  moverLabel,
  officialConsumptionTotals,
  trendChartHeight,
  yearColumnLabel,
} from '../../utils/trendChanges'
import { REPORT_COLUMN_LABELS } from '../../utils/reportTaxonomy'
import { REPORT_METRIC_HINTS } from '../../components/MetricExplanation'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'

type TrendChangesReportProps = {
  title: string
  subtitle?: string
}

type DriverView = 'category' | 'merchant'
type MatrixView = 'classification' | 'categoryL1'
type YearsShown = 2 | 3 | 4 | 5

const CARD_ORDER = ['expense', 'income', 'savings'] as const

function formatYoYDelta(card: TrendYoYCard): string {
  if (card.format === 'percent') {
    return `${card.deltaAmount >= 0 ? '+' : ''}${card.deltaAmount.toFixed(1)} pts`
  }
  return formatMoney(card.deltaAmount)
}

function formatYoYValue(card: TrendYoYCard, value: number): string {
  return card.format === 'percent' ? `${value.toFixed(1)}%` : formatMoney(value)
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

export function TrendChangesReport({ title, subtitle }: TrendChangesReportProps) {
  const { open: drillOpen, context: drillContext, openDrill, closeDrill } = useDrillDown()
  const viewportH = useViewportTableHeight(320)
  const [toYear, setToYear] = useState(dayjs().year())
  const [yearsShown, setYearsShown] = useState<YearsShown>(3)
  const [driverView, setDriverView] = useState<DriverView>('category')
  const [matrixView, setMatrixView] = useState<MatrixView>('classification')
  const [selectedKey, setSelectedKey] = useState<string | null>(null)

  const fromYear = toYear - 1
  const historyFromYear = toYear - yearsShown + 1

  const { data, isLoading, isFetching, isError, error } = useQuery({
    queryKey: ['trend-changes', fromYear, toYear, historyFromYear],
    queryFn: () => fetchTrends(fromYear, toYear, historyFromYear),
  })

  const loading = isLoading || isFetching
  const insights = useMemo(() => (data ? buildTrendInsights(data) : []), [data])
  const yoyCards = useMemo(() => {
    if (!data) return []
    const cards = buildTrendYoYCards(data)
    return CARD_ORDER
      .map((key) => cards.find((c) => c.key === key))
      .filter((c): c is TrendYoYCard => c != null)
  }, [data])
  const matrix = useMemo((): CategoryYearMatrix | undefined => {
    if (!data) return undefined
    return matrixView === 'classification' ? data.categoryYearMatrix : data.categoryL1YearMatrix
  }, [data, matrixView])
  const yearSeries = data?.consumptionYearSeries ?? []
  const matrixTotals = useMemo(() => (matrix ? categoryYearMatrixTotals(matrix) : {}), [matrix])
  const officialTotals = useMemo(() => officialConsumptionTotals(yearSeries), [yearSeries])

  const totalYearChart = useMemo(
    () => (yearSeries.length ? buildConsumptionTotalYearChart(yearSeries) : {}),
    [yearSeries],
  )

  const movers = useMemo(() => {
    if (!data) return []
    return driverView === 'category' ? data.topCategoryGrowth : data.topMerchantMovers
  }, [data, driverView])

  const chartOption = useMemo(() => {
    if (!data) return {}
    return driverView === 'category'
      ? buildCategoryContributorChart(data)
      : buildMerchantContributorChart(data)
  }, [data, driverView])

  const yearTrendChart = useMemo(
    () => (matrix ? buildCategoryYearTrendChart(matrix, 6) : {}),
    [matrix],
  )

  const chartHeight = trendChartHeight(Math.min(movers.length, 8))

  const openTrendDrill = (label: string, item: TrendItem | TrendMover, explanation: string[]) => {
    openDrill(buildReportDrillContext({
      title: `${label} · ${fromYear}→${toYear}`,
      metricLabel: label,
      params: item.drillDown || {},
      explanation,
      source: 'report',
      provenance: {
        reportId: 'trend-changes',
        sourceView: `${label} contributor`,
        aggregateTotal: 'deltaAmount' in item ? item.deltaAmount : undefined,
      },
    }))
  }

  const yearDrillBounds = (year: number) => {
    const pt = yearSeries.find((p) => p.year === year)
    const start = dayjs(`${year}-01-01`).format('YYYY-MM-DD')
    const end = pt?.partial && pt.throughDate
      ? pt.throughDate.slice(0, 10)
      : dayjs(`${year}-12-31`).format('YYYY-MM-DD')
    return { start, end }
  }

  const openMatrixDrill = (row: CategoryYearMatrixRow, year: number) => {
    if (!row.drillDown && matrixView === 'classification') return
    const bounds = yearDrillBounds(year)
    const label = yearColumnLabel(year, matrix?.partialYears)
    const params: Record<string, string> = matrixView === 'categoryL1'
      ? {
          transactionDateStartStr: bounds.start,
          transactionDateEndStr: bounds.end,
          txnTypes: 'expense',
          consumeID: row.tagId,
          consumeName: row.label,
        }
      : {
          transactionDateStartStr: bounds.start,
          transactionDateEndStr: bounds.end,
          txnTypes: 'expense',
          semanticFilter: row.tagId,
        }
    openDrill(buildReportDrillContext({
      title: `${row.label} · ${label}`,
      metricLabel: row.label,
      params,
      explanation: [`Consumption in ${label} for ${row.label}.`, REPORT_METRIC_HINTS.expense],
      source: 'report',
      provenance: { reportId: 'trend-changes', sourceView: matrixView === 'categoryL1' ? 'category L1 matrix' : 'classification matrix' },
    }))
  }

  const exportMatrixCsv = () => {
    if (!matrix) return
    const suffix = matrixView === 'classification' ? 'classification' : 'category-l1'
    downloadCategoryYearMatrixCsv(
      matrix,
      officialTotals,
      `consumption-trends-${historyFromYear}-${toYear}-${suffix}.csv`,
    )
  }

  const moverExplanation = (row: TrendMover) => {
    const { from, to } = moverFromTo(row)
    const pct = Number(row.deltaPercent ?? row.pctChange ?? 0)
    const name = moverLabel(row)
    const parts = [
      from != null && to != null ? `${name}: ${formatMoney(from)} → ${formatMoney(to)}` : name,
      `Δ ${formatMoney(row.deltaAmount)} (${pct >= 0 ? '+' : ''}${pct.toFixed(1)}%)`,
      `${Number(row.contributionPct).toFixed(1)}% of total change`,
    ]
    return parts.join(' · ')
  }

  const openMoverDrill = (row: TrendMover) => {
    const name = moverLabel(row)
    openTrendDrill(name, row, [
      `${driverView === 'category' ? 'Type' : 'Merchant'} ${name} moved ${formatMoney(row.deltaAmount)} year over year.`,
      moverExplanation(row),
    ])
  }

  const openYoYCardDrill = (card: TrendYoYCard) => {
    if (!data || !card.trendType) return
    const trend = findTrendItem(data, card.trendType)
    if (!trend?.drillDown) return
    openTrendDrill(card.label, trend, [
      `${card.label}: ${formatYoYValue(card, card.from)} → ${formatYoYValue(card, card.to)}`,
      `${formatYoYDelta(card)} (${card.deltaPercent >= 0 ? '+' : ''}${card.deltaPercent.toFixed(1)}% change).`,
    ])
  }

  const fromToCols = [
    {
      title: yearColumnLabel(fromYear, matrix?.partialYears),
      key: 'fromAmount',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 100,
      render: (_: unknown, row: TrendMover) => {
        const { from } = moverFromTo(row)
        return from == null ? '—' : formatMoney(from)
      },
    },
    {
      title: yearColumnLabel(toYear, matrix?.partialYears),
      key: 'toAmount',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 100,
      render: (_: unknown, row: TrendMover) => {
        const { to } = moverFromTo(row)
        return to == null ? '—' : formatMoney(to)
      },
    },
  ]

  const moverCols = [
    {
      title: driverView === 'category' ? REPORT_COLUMN_LABELS.classification : 'Merchant',
      dataIndex: 'label',
      sortType: 'text' as const,
      ellipsis: true,
      render: (_: unknown, row: TrendMover) => {
        const name = moverLabel(row)
        return (
          <Tooltip title={name}>
            <Typography.Text ellipsis className="fs-trend-matrix-label">{name}</Typography.Text>
          </Tooltip>
        )
      },
    },
    ...fromToCols,
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
      render: (_: unknown, row: TrendMover) => (
        <DeltaPercentCell value={Number(row.deltaPercent ?? row.pctChange ?? 0)} />
      ),
    },
    {
      title: 'Share',
      dataIndex: 'contributionPct',
      cellType: 'contribution' as const,
      align: 'right' as const,
      sortType: 'number' as const,
      width: 88,
    },
  ]

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
        title: matrixView === 'classification' ? 'Type' : 'Category',
        dataIndex: 'label',
        sortType: 'text' as const,
        ellipsis: true,
        fixed: 'left' as const,
        width: 140,
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
  }, [matrix, matrixView])

  const onChartClick = (params: unknown) => {
    if (!data) return
    const clickedName = (params as { name?: string }).name
    if (!clickedName) return
    const row = movers.find((m) => moverLabel(m) === clickedName)
    if (row) {
      setSelectedKey(moverKey(row, driverView))
    }
  }

  const yearOptions = [toYear - 2, toYear - 1, toYear, toYear + 1].map((y) => ({ value: y, label: String(y) }))

  return (
    <DataPageLayout
      title={title}
      subtitle={subtitle ? `${subtitle} · ${historyFromYear}–${toYear}` : `${historyFromYear}–${toYear}`}
      icon={<BarChartOutlined />}
      className="fs-data-page--dense fs-data-page--fill fs-data-page--reports fs-trend-changes"
      toolbar={(
        <div className="fs-trend-controls">
          <div className="fs-trend-controls__group">
            <span className="fs-trend-controls__label">Compare year</span>
            <Select
              size="small"
              value={toYear}
              className="fs-trend-controls__select"
              options={yearOptions}
              onChange={(y) => {
                setToYear(y)
                setSelectedKey(null)
              }}
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
          {(data?.compareMode === 'ytd_aligned' || data?.lifestyleInflation.detected) && (
            <>
              <span className="fs-trend-controls__divider" aria-hidden />
              <div className="fs-trend-controls__tags">
                {data?.compareMode === 'ytd_aligned' && (
                  <Tag className="fs-trend-controls__tag">Same period YTD</Tag>
                )}
                {data?.lifestyleInflation.detected && (
                  <Tag color="warning" className="fs-trend-controls__tag">Spending &gt; income growth</Tag>
                )}
              </div>
            </>
          )}
        </div>
      )}
    >
      {isError && (
        <Alert type="error" showIcon message="Failed to load trends" description={error instanceof Error ? error.message : 'Try another year.'} />
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

          <section className="fs-trend-section" aria-label="Overview">
            <ContentCard className="fs-trend-overview-card" styles={{ body: { padding: '20px 24px' } }}>
              <div className="fs-trend-overview">
                {yoyCards.map((card) => (
                  <button
                    key={card.key}
                    type="button"
                    className={`fs-trend-stat fs-trend-stat--${card.tone}${card.key === 'expense' ? ' fs-trend-stat--hero' : ''}`}
                    onClick={() => openYoYCardDrill(card)}
                    title={card.trendType ? 'Click to view transactions' : undefined}
                  >
                    <span className="fs-trend-stat__label">{card.label}</span>
                    <div className="fs-trend-stat__values">
                      <span className="fs-trend-stat__from">{formatYoYValue(card, card.from)}</span>
                      <ArrowRightOutlined className="fs-trend-stat__arrow" aria-hidden />
                      <span className="fs-trend-stat__to">{formatYoYValue(card, card.to)}</span>
                    </div>
                    <span className={`fs-trend-stat__delta${card.deltaAmount >= 0 ? ' fs-delta--up' : ' fs-delta--down'}`}>
                      {formatYoYDelta(card)}
                      <span className="fs-trend-stat__pct">
                        ({card.deltaPercent >= 0 ? '+' : ''}{card.deltaPercent.toFixed(1)}%)
                      </span>
                    </span>
                  </button>
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
            <section className="fs-trend-section" aria-label="Yearly trend">
              <TrendSectionHead
                title="Yearly total"
                description="Living spend per calendar year · click a breakdown row below for details"
              />
              <ContentCard className="fs-trend-chart-card" styles={{ body: { padding: '12px 16px 8px' } }}>
                <FsChart
                  profile="compareBars"
                  height={240}
                  loading={loading}
                  option={totalYearChart}
                  empty={<EmptyState compact title="No yearly totals" />}
                />
                <div className="fs-trend-year-strip">
                  {yearSeries.map((pt) => (
                    <div key={pt.year} className="fs-trend-year-strip__item">
                      <span className="fs-trend-year-strip__label">
                        {yearColumnLabel(pt.year, data.categoryYearMatrix?.partialYears)}
                      </span>
                      <span className="fs-trend-year-strip__value">{formatMoney(pt.amount)}</span>
                    </div>
                  ))}
                </div>
              </ContentCard>
            </section>
          )}

          {matrix && matrix.rows.length > 0 && (
            <section className="fs-trend-section" aria-label="Breakdown by year">
              <TrendSectionHead
                title="Breakdown"
                description={`${matrixView === 'classification' ? 'By spending type' : 'By category'} · ${historyFromYear}–${toYear}`}
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
                    <Button
                      size="small"
                      type="text"
                      icon={<DownloadOutlined />}
                      disabled={!matrix.rows.length}
                      onClick={exportMatrixCsv}
                    >
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
                  option={yearTrendChart}
                  empty={<EmptyState compact title="No breakdown chart" />}
                />
              </ContentCard>

              <div className="fs-trend-matrix-wrap">
                <FsDataTable
                  title={undefined}
                  columns={matrixCols}
                  dataSource={matrix.rows}
                  rowKey="tagId"
                  loading={loading}
                  size="small"
                  scroll={{ x: 'max-content', y: Math.max(320, viewportH - 80) }}
                  fixedLayout={false}
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
                    emptyText: <EmptyState compact title="No history" description="Import and classify transactions." />,
                  }}
                />
              </div>
            </section>
          )}

          <section className="fs-trend-section fs-trend-section--secondary">
            <Collapse
              bordered={false}
              className="fs-trend-movers-panel"
              items={[{
                key: 'drivers',
                label: (
                  <span className="fs-trend-movers-panel__label">
                    <RiseOutlined aria-hidden />
                    Biggest changes · {fromYear} → {toYear}
                  </span>
                ),
                children: (
                  <div className="fs-trend-movers">
                    <div className="fs-trend-movers__toolbar">
                      <Segmented<DriverView>
                        value={driverView}
                        onChange={(v) => {
                          setDriverView(v)
                          setSelectedKey(null)
                        }}
                        options={[
                          { label: `By type (${data.topCategoryGrowth.length})`, value: 'category' },
                          { label: `By merchant (${data.topMerchantMovers.length})`, value: 'merchant' },
                        ]}
                      />
                    </div>
                    <div className="fs-trend-movers__body">
                      <ContentCard className="fs-trend-chart-card" styles={{ body: { padding: 8 } }}>
                        <FsChart
                          profile="horizontalBar"
                          height={chartHeight}
                          loading={loading}
                          option={chartOption}
                          empty={<EmptyState compact title="No movers" description="Spending may be flat this year." />}
                          onEvents={{ click: onChartClick }}
                        />
                      </ContentCard>
                      <div className="fs-trend-movers__table">
                        <FsDataTable
                          title={undefined}
                          columns={moverCols}
                          dataSource={movers}
                          rowKey={(r) => moverKey(r, driverView)}
                          loading={loading}
                          size="small"
                          rowExplanation={moverExplanation}
                          onRow={(record) => ({
                            onClick: () => {
                              setSelectedKey(moverKey(record, driverView))
                              openMoverDrill(record)
                            },
                            style: { cursor: 'pointer' },
                          })}
                          rowClassName={(record) => (
                            selectedKey === moverKey(record, driverView) ? 'fs-trend-row--selected' : 'fs-trend-matrix-row'
                          )}
                          scroll={movers.length > 8 ? { y: 320 } : undefined}
                          fixedLayout={false}
                          locale={{
                            emptyText: (
                              <EmptyState
                                compact
                                title={driverView === 'category' ? 'No type movers' : 'No merchant movers'}
                                description="Spending may be flat or evenly distributed."
                              />
                            ),
                          }}
                        />
                      </div>
                    </div>
                  </div>
                ),
              }]}
            />
          </section>
        </div>
      )}

      <UnifiedDrillDrawer open={drillOpen} context={drillContext} onClose={closeDrill} />
    </DataPageLayout>
  )
}
