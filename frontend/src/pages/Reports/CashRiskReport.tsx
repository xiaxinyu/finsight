import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Badge, Calendar, Col, Row, Select, Tag, Typography } from 'antd'
import { BarChartOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { fetchCashRiskCalendar } from '../../api/analytics'
import { useFeatureFlags } from '../../hooks/useFeatureFlags'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { FsChart } from '../../components/FsChart'
import { UnifiedDrillDrawer } from '../../components/ReportDrillDrawer'
import { buildReportDrillContext, drillParamsForYearMonth } from '../../components/drilldown/buildDrillContext'
import { useDrillDown } from '../../hooks/useDrillDown'
import { ReportKpiStrip } from '../../components/ReportKpiStrip'
import type { EChartsOption } from 'echarts'
import { formatMoney } from '../../utils/format'
import {
  eventTypeLabel,
  indexCashRiskDays,
  monthRiskLevel,
  riskLevelClass,
  type CashRiskDay,
} from '../../utils/cashRiskCalendar'

const SCENARIOS = [
  { value: 'base', label: 'Base' },
  { value: 'conservative', label: 'Conservative' },
  { value: 'optimistic', label: 'Optimistic' },
  { value: 'stress', label: 'Stress' },
]

type CashRiskReportProps = {
  title: string
  subtitle?: string
}

export function CashRiskReport({ title, subtitle }: CashRiskReportProps) {
  const { flags } = useFeatureFlags()
  const { open: drillOpen, context: drillContext, openDrill, closeDrill } = useDrillDown()
  const [year, setYear] = useState(dayjs().year())
  const [scenario, setScenario] = useState('stress')
  const [selectedDay, setSelectedDay] = useState<Dayjs>(dayjs())

  const { data, isLoading, isFetching, isError, error } = useQuery({
    queryKey: ['cash-risk-calendar', year, scenario],
    queryFn: () => fetchCashRiskCalendar(year, scenario),
    enabled: flags.forecast,
  })

  const dayIndex = useMemo(() => indexCashRiskDays(data?.days), [data?.days])
  const loading = isLoading || isFetching
  const calendarValue = useMemo(
    () => selectedDay.year(year),
    [selectedDay, year],
  )
  const selectedKey = selectedDay.format('YYYY-MM-DD')
  const selectedDetail: CashRiskDay | undefined = dayIndex.get(selectedKey)

  const chartOption = useMemo((): EChartsOption => {
    const months = data?.months || []
    return {
      grid: { left: 48, right: 16, top: 48, bottom: 28 },
      tooltip: { trigger: 'axis' },
      legend: { data: ['Net'], top: 4 },
      xAxis: { type: 'category', data: months.map((m) => m.yearMonth), axisLabel: { fontSize: 10 } },
      yAxis: { type: 'value' },
      series: [{
        name: 'Net',
        type: 'line',
        smooth: true,
        data: months.map((m) => m.net),
        itemStyle: { color: '#2563eb' },
        areaStyle: { opacity: 0.08 },
      }],
    }
  }, [data?.months])

  const openMonthDrill = (yearMonth: string) => {
    const month = data?.months?.find((m) => m.yearMonth === yearMonth)
    const isDeficit = (data?.deficitMonths || []).includes(yearMonth)
    openDrill(buildReportDrillContext({
      title: `Cash risk · ${yearMonth}`,
      metricLabel: `Projected net · ${scenario}`,
      params: drillParamsForYearMonth(yearMonth),
      explanation: [
        month
          ? `Forecast net ${formatMoney(month.net)} (${month.riskLevel} risk).`
          : `Forecast month ${yearMonth} under ${scenario} scenario.`,
        isDeficit
          ? 'This month is projected to run a deficit — review bills and discretionary spend.'
          : 'Liquidity looks manageable in this month under the selected scenario.',
      ],
      actions: [
        { label: 'Open planning', type: 'planning', path: '/planning' },
        { label: 'Cashflow report', type: 'report', path: '/reports/cashflow' },
      ],
      source: 'cash-risk',
    }))
  }

  const kpis = [
    { key: 'year', label: 'Year', value: String(year) },
    { key: 'scenario', label: 'Scenario', value: scenario },
    {
      key: 'def',
      label: 'Deficit months',
      value: String(data?.deficitMonths?.length || 0),
      tone: (data?.deficitMonths?.length || 0) > 0 ? 'warn' as const : 'neutral' as const,
    },
    {
      key: 'high',
      label: 'High-risk days',
      value: String((data?.days || []).filter((d) => d.riskLevel === 'high').length),
      tone: 'warn' as const,
    },
  ]

  return (
    <DataPageLayout
      title={title}
      subtitle={subtitle}
      icon={<BarChartOutlined />}
      className="fs-data-page--dense fs-data-page--reports"
      toolbar={(
        <div className="fs-cash-risk-toolbar">
          <Select
            size="small"
            value={year}
            style={{ width: 110 }}
            options={[year - 1, year, year + 1].map((y) => ({ value: y, label: String(y) }))}
            onChange={setYear}
          />
          <Select
            size="small"
            value={scenario}
            style={{ width: 150 }}
            options={SCENARIOS}
            onChange={setScenario}
          />
        </div>
      )}
    >
      {!flags.forecast && (
        <EmptyState title="Forecast module disabled" description="Enable finsight.forecast.enabled to use cash risk calendar." />
      )}

      {flags.forecast && isError && (
        <Alert
          type="error"
          showIcon
          message="Failed to load cash risk calendar"
          description={error instanceof Error ? error.message : 'Try another scenario.'}
        />
      )}

      {flags.forecast && (
        <>
      <ReportKpiStrip items={kpis} />

      <Row gutter={[12, 12]} className="fs-report-body">
        <Col xs={24} lg={14}>
          <ContentCard title="Cash pressure calendar" size="small">
            <Calendar
              fullscreen={false}
              value={calendarValue}
              onSelect={setSelectedDay}
              onPanelChange={(value) => setYear(value.year())}
              cellRender={(current, info) => {
                if (info.type !== 'date') return info.originNode
                const key = current.format('YYYY-MM-DD')
                const day = dayIndex.get(key)
                const ym = current.format('YYYY-MM')
                const monthRisk = monthRiskLevel(data?.months, ym)
                const cls = riskLevelClass(day?.riskLevel || monthRisk)
                return (
                  <div className={`fs-cash-risk-day ${cls}`}>
                    <div>{current.date()}</div>
                    {day && (
                      <div className="fs-cash-risk-day-dots">
                        {day.events.slice(0, 3).map((ev) => (
                          <span key={`${ev.type}-${ev.label}`} className={`fs-cash-risk-dot fs-cash-risk-dot--${ev.type}`} />
                        ))}
                      </div>
                    )}
                  </div>
                )
              }}
            />
            <div className="fs-cash-risk-legend">
              <Tag color="red">High risk</Tag>
              <Tag color="orange">Medium</Tag>
              <Tag color="green">Low</Tag>
              <span className="fs-cash-risk-legend-item"><span className="fs-cash-risk-dot fs-cash-risk-dot--bill" /> Bill</span>
              <span className="fs-cash-risk-legend-item"><span className="fs-cash-risk-dot fs-cash-risk-dot--income" /> Income</span>
              <span className="fs-cash-risk-legend-item"><span className="fs-cash-risk-dot fs-cash-risk-dot--goal" /> Goal</span>
            </div>
          </ContentCard>
        </Col>

        <Col xs={24} lg={10}>
          <ContentCard title={`Day detail · ${selectedKey}`} size="small">
            {selectedDetail ? (
              <>
                <div style={{ marginBottom: 8 }}>
                  <Badge
                    status={selectedDetail.riskLevel === 'high' ? 'error' : selectedDetail.riskLevel === 'medium' ? 'warning' : 'success'}
                    text={`Risk: ${selectedDetail.riskLevel}`}
                  />
                </div>
                <Typography.Paragraph type="secondary">
                  Inflow {formatMoney(selectedDetail.inflow)} · Outflow {formatMoney(selectedDetail.outflow)}
                </Typography.Paragraph>
                {selectedDetail.events.map((ev) => (
                  <div key={`${ev.type}-${ev.label}-${ev.amount}`} className="fs-cash-risk-event">
                    <Tag>{eventTypeLabel(ev.type)}</Tag>
                    <span>{ev.label}</span>
                    <strong>{formatMoney(ev.amount)}</strong>
                  </div>
                ))}
              </>
            ) : (
              <EmptyState compact title="No scheduled events" description="Select a highlighted day or add bills/goals in Planning." />
            )}
          </ContentCard>

          <ContentCard title="Monthly net forecast" size="small" styles={{ body: { padding: 8 } }}>
            <FsChart
              profile="timeSeries"
              height={260}
              loading={loading}
              option={chartOption}
              empty={<EmptyState compact title="No forecast data" />}
              onEvents={{
                click: (p) => {
                  const ym = (p as { name?: string }).name
                  if (ym) openMonthDrill(ym)
                },
              }}
            />
          </ContentCard>
        </Col>
      </Row>
        </>
      )}
      <UnifiedDrillDrawer open={drillOpen} context={drillContext} onClose={closeDrill} />
    </DataPageLayout>
  )
}
