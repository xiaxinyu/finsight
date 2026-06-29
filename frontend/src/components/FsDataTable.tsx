import { useMemo, useState, type ReactNode } from 'react'
import { Table, type TableProps } from 'antd'
import type { ColumnsType, ColumnType } from 'antd/es/table'
import { CaretDownOutlined, CaretUpOutlined } from '@ant-design/icons'
import { formatNumber } from '../utils/format'
import { formatTableDate } from '../utils/cell'
import { resolveForecastKind } from '../utils/fsTableCells'
import {
  ContributionBar,
  DeltaMoneyCell,
  DeltaPercentCell,
  ForecastTag,
  MoneyCell,
  RiskTag,
  RowExplanationHint,
} from './FsTableCellViews'
import { ContentCard } from './ContentCard'
import { EmptyState } from './EmptyState'

export type FsCellType =
  | 'money'
  | 'moneySigned'
  | 'deltaPercent'
  | 'deltaMoney'
  | 'contribution'
  | 'risk'
  | 'forecast'
  | 'percent'

export type FsColumn<T> = ColumnType<T> & {
  unit?: string
  sortType?: 'text' | 'number' | 'date' | 'percent'
  cellType?: FsCellType
  /** @deprecated use cellType: 'deltaPercent' */
  isDelta?: boolean
  deltaAmountKey?: string
  contributionMax?: number
  expenseContext?: boolean
  mono?: boolean
  ellipsis?: boolean
}

type Props<T> = {
  title?: string
  columns: FsColumn<T>[]
  dataSource: T[]
  rowKey: string | ((r: T) => string)
  loading?: boolean
  summary?: Record<string, number | string>
  summaryLabel?: string
  size?: TableProps<T>['size']
  scroll?: TableProps<T>['scroll']
  onRow?: TableProps<T>['onRow']
  locale?: TableProps<T>['locale']
  rowExplanation?: (record: T) => string | undefined
  /** Fixed column widths — prevents Change % / Contribution overlap in dense report tables. */
  fixedLayout?: boolean
}

function TwoLineTitle({ name, unit }: { name: string; unit?: string }) {
  return (
    <div className="fs-col-header">
      <div className="fs-col-header-name">{name}</div>
      {unit && <div className="fs-col-header-unit">{unit}</div>}
    </div>
  )
}

function compareValues(a: unknown, b: unknown, type: FsColumn<unknown>['sortType']): number {
  if (type === 'number' || type === 'percent') {
    return (Number(a) || 0) - (Number(b) || 0)
  }
  if (type === 'date') {
    return String(a ?? '').localeCompare(String(b ?? ''))
  }
  return String(a ?? '').localeCompare(String(b ?? ''), undefined, { sensitivity: 'base' })
}

function readField<T extends Record<string, unknown>>(record: T, key?: string): unknown {
  if (!key) return undefined
  return record[key]
}

function defaultCellRender<T extends Record<string, unknown>>(
  col: FsColumn<T>,
  value: unknown,
  record: T,
): ReactNode {
  const expenseContext = col.expenseContext !== false
  const cellType = col.cellType ?? (col.isDelta ? 'deltaPercent' : undefined)

  switch (cellType) {
    case 'money':
      return <MoneyCell value={Number(value)} unit={col.unit} />
    case 'moneySigned':
      return <MoneyCell value={Number(value)} signed unit={col.unit} />
    case 'deltaPercent':
      return (
        <DeltaPercentCell
          value={Number(value)}
          amount={Number(readField(record, col.deltaAmountKey))}
          expenseContext={expenseContext}
        />
      )
    case 'deltaMoney':
      return <DeltaMoneyCell value={Number(value)} expenseContext={expenseContext} />
    case 'contribution':
      return <ContributionBar value={Number(value)} max={col.contributionMax ?? 100} />
    case 'risk':
      return <RiskTag level={String(value ?? readField(record, col.dataIndex as string) ?? 'low')} />
    case 'forecast': {
      const kind = resolveForecastKind(record as Record<string, unknown>)
      return kind ? <ForecastTag kind={kind} /> : '—'
    }
    case 'percent':
      return <span className="fs-table-cell-percent">{`${Number(value || 0).toFixed(1)}%`}</span>
    default:
      break
  }

  if (col.sortType === 'date') {
    return <span className="fs-mono">{formatTableDate(value)}</span>
  }
  if (col.unit === 'CNY' || col.unit === 'USD') {
    return <MoneyCell value={Number(value)} unit={col.unit} />
  }
  if (col.mono) {
    return <code className="fs-mono" title={String(value ?? '')}>{String(value ?? '')}</code>
  }
  const text = String(value ?? '')
  if (col.ellipsis) {
    return <span title={text}>{text}</span>
  }
  return text
}

export function FsDataTable<T extends Record<string, unknown>>({
  title,
  columns,
  dataSource,
  rowKey,
  loading,
  summary,
  summaryLabel = 'Total',
  size = 'small',
  scroll,
  onRow,
  locale,
  rowExplanation,
  fixedLayout = true,
}: Props<T>) {
  const [sortKey, setSortKey] = useState<string | null>(null)
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc')

  const sortedData = useMemo(() => {
    if (!sortKey) return dataSource
    const col = columns.find((c) => c.dataIndex === sortKey || c.key === sortKey)
    const type = col?.sortType ?? 'text'
    const copy = [...dataSource]
    copy.sort((a, b) => {
      const av = a[sortKey as keyof T]
      const bv = b[sortKey as keyof T]
      const cmp = compareValues(av, bv, type)
      return sortDir === 'asc' ? cmp : -cmp
    })
    return copy
  }, [columns, dataSource, sortDir, sortKey])

  const antColumns: ColumnsType<T> = columns.map((col, colIdx) => {
    const key = String(col.dataIndex ?? col.key ?? '')
    const sortable = col.sortType != null
    const active = sortKey === key
    const showHint = Boolean(rowExplanation) && colIdx === 0
    return {
      ...col,
      title: (
        <div
          className={sortable ? 'fs-sortable-th' : undefined}
          onClick={sortable ? () => {
            if (active) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
            else { setSortKey(key); setSortDir('desc') }
          } : undefined}
          role={sortable ? 'button' : undefined}
          tabIndex={sortable ? 0 : undefined}
        >
          <TwoLineTitle name={typeof col.title === 'string' ? col.title : key} unit={col.unit} />
          {sortable && (
            <span className={`fs-sort-icon${active ? ' active' : ''}`}>
              {active && sortDir === 'asc' ? <CaretUpOutlined /> : <CaretDownOutlined />}
            </span>
          )}
        </div>
      ),
      className: active ? 'fs-col-sorted' : undefined,
      render: (value, record, index) => {
        const content: ReactNode = col.render
          ? (col.render(value, record, index) as ReactNode)
          : defaultCellRender(col, value, record)
        if (!showHint) return content
        const hint = rowExplanation?.(record)
        if (!hint) return content
        return (
          <span className="fs-table-cell-with-hint">
            {content}
            <RowExplanationHint text={hint} />
          </span>
        )
      },
    }
  })

  const mergedOnRow: TableProps<T>['onRow'] = (record, index) => {
    const base = onRow?.(record, index) ?? {}
    const hint = rowExplanation?.(record)
    return hint ? { ...base, title: hint } : base
  }

  return (
    <ContentCard title={title} className="fs-table-card" size="small">
      <Table<T>
        className={`fs-data-table fs-data-table--encoded${fixedLayout ? ' fs-data-table--fixed' : ''}`}
        size={size}
        loading={loading}
        rowKey={rowKey}
        dataSource={sortedData}
        columns={antColumns}
        pagination={false}
        tableLayout={fixedLayout ? 'fixed' : undefined}
        scroll={scroll}
        rowClassName={() => 'fs-table-row'}
        onRow={mergedOnRow}
        locale={locale ?? {
          emptyText: <EmptyState compact title="No rows" description="Adjust filters or date range." />,
        }}
        summary={() => summary ? (
          <Table.Summary fixed>
            <Table.Summary.Row className="fs-summary-row">
              {columns.map((col, idx) => {
                const key = String(col.dataIndex ?? col.key ?? idx)
                const val = summary[key]
                return (
                  <Table.Summary.Cell key={key} index={idx} align={col.align}>
                    {idx === 0 ? <strong>{summaryLabel}</strong> : val != null ? (
                      <strong className="fs-money">
                        {typeof val === 'number' ? formatNumber(val) : val}
                      </strong>
                    ) : null}
                  </Table.Summary.Cell>
                )
              })}
            </Table.Summary.Row>
          </Table.Summary>
        ) : undefined}
      />
    </ContentCard>
  )
}
