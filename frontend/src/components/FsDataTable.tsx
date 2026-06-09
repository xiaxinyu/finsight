import { useMemo, useState } from 'react'
import { Table, type TableProps } from 'antd'
import type { ColumnsType, ColumnType } from 'antd/es/table'
import { CaretDownOutlined, CaretUpOutlined } from '@ant-design/icons'
import { formatNumber } from '../utils/format'
import { ContentCard } from './ContentCard'

export type FsColumn<T> = ColumnType<T> & {
  unit?: string
  sortType?: 'text' | 'number' | 'date' | 'percent'
  isDelta?: boolean
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

  const antColumns: ColumnsType<T> = columns.map((col) => {
    const key = String(col.dataIndex ?? col.key ?? '')
    const sortable = col.sortType != null
    const active = sortKey === key
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
        if (col.render) return col.render(value, record, index)
        if (col.isDelta) {
          const n = Number(value) || 0
          const cls = n < 0 ? 'fs-delta-negative' : n > 0 ? 'fs-delta-positive' : 'fs-delta-neutral'
          return <span className={cls}>{n >= 0 ? '+' : ''}{Number(n).toFixed(1)}%</span>
        }
        if (col.unit === 'CNY' || col.unit === 'USD') {
          return <span className="fs-money">{formatNumber(Number(value))}</span>
        }
        if (col.mono) {
          return <code className="fs-mono" title={String(value ?? '')}>{String(value ?? '')}</code>
        }
        const text = String(value ?? '')
        if (col.ellipsis) {
          return <span title={text}>{text}</span>
        }
        return text
      },
    }
  })

  return (
    <ContentCard title={title} className="fs-table-card" size="small">
      <Table<T>
        className="fs-data-table"
        size={size}
        loading={loading}
        rowKey={rowKey}
        dataSource={sortedData}
        columns={antColumns}
        pagination={false}
        scroll={scroll ?? { y: 360 }}
        rowClassName={() => 'fs-table-row'}
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
