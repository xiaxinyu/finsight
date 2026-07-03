import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { useViewportTableHeight } from '../../../hooks/useViewportTableHeight'
import { EmptyState } from '../../../components/EmptyState'
import { Alert, Button, message, Segmented, Select, Space, Steps, Table, Tag, Tooltip, Upload } from 'antd'
import { CheckCircleOutlined, CloudUploadOutlined, EyeOutlined, InboxOutlined, UploadOutlined } from '@ant-design/icons'
import {
  commitStatement, previewStatement, skippedStatementLines, uploadStatement,
  type SkippedImportRow, type StatementCommitResult, type StatementPreviewRow,
} from '../../../api/statement'
import { listBankCards, type BankCardRow } from '../../../api/transaction'
import { DataPageLayout } from '../../../components/DataPageLayout'
import { MoneyText } from '../../../components/MoneyText'
import { ImportQualityGate } from '../../../components/ImportQualityGate'
import { TransactionSummaryBar } from '../../../components/TransactionSummaryBar'
import { cellText, formatTableDate } from '../../../utils/cell'
import { formatNumber } from '../../../utils/format'
import { moneyTypeFromRow } from '../../../utils/moneyType'

const { Dragger } = Upload

const BANK_OPTIONS = [
  { value: 'CMB', label: 'CMB — China Merchants Bank' },
  { value: 'CCB', label: 'CCB — China Construction Bank' },
  { value: 'ABC', label: 'ABC — Agricultural Bank of China' },
  { value: 'CGB', label: 'CGB — China Guangfa Bank' },
  { value: 'CRBANK', label: 'CRBANK — China Resources Bank' },
  { value: 'ALIPAY', label: 'Alipay' },
  { value: 'WECHAT', label: 'WeChat Pay' },
]

const CARD_OPTIONS = [
  { value: 'debit', label: 'Debit card' },
  { value: 'credit', label: 'Credit card' },
  { value: 'ewallet', label: 'E-wallet' },
]

function previewAmount(row: StatementPreviewRow): number {
  const income = Math.abs(Number(row.incomeMoney || 0))
  const expense = Math.abs(Number(row.balanceMoney || 0))
  return income > 0 ? income : expense
}

function previewTxnType(row: StatementPreviewRow): 'income' | 'expense' {
  return Number(row.incomeMoney) > 0 ? 'income' : 'expense'
}

function guessBankFromFilename(filename: string): string | null {
  const name = filename.toLowerCase()
  if (name.includes('招商') || name.includes('cmb') || name.includes('merchants')) return 'CMB'
  if (name.includes('建设') || name.includes('ccb') || name.includes('construction')) return 'CCB'
  if (name.includes('农业') || name.includes('abc') || name.includes('agricultural')) return 'ABC'
  if (name.includes('广发') || name.includes('cgb') || name.includes('guangfa')) return 'CGB'
  if (name.includes('华润') || name.includes('crbank')) return 'CRBANK'
  if (name.includes('支付宝') || name.includes('alipay')) return 'ALIPAY'
  if (name.includes('微信') || name.includes('wechat')) return 'WECHAT'
  return null
}

function SkippedRowDetail({ row }: { row: SkippedImportRow }) {
  const original = row.originalLine?.trim() || row.rawText
  const columns = row.columns?.filter((c) => c.length > 0) ?? []
  return (
    <div className="fs-skipped-detail">
      <div className="fs-skipped-detail-grid">
        <div className="fs-skipped-detail-block">
          <div className="fs-skipped-detail-label">Original line (file L{row.fileLineNumber ?? row.lineNumber})</div>
          <pre className="fs-skipped-detail-pre">{original}</pre>
        </div>
        {row.hint && (
          <div className="fs-skipped-detail-block">
            <div className="fs-skipped-detail-label">Diagnostics</div>
            <pre className="fs-skipped-detail-pre fs-skipped-detail-pre--hint">{row.hint}</pre>
          </div>
        )}
      </div>
      {columns.length > 0 && (
        <div className="fs-skipped-detail-block">
          <div className="fs-skipped-detail-label">Split columns ({columns.length})</div>
          <div className="fs-skipped-cols">
            {columns.map((col, i) => (
              <div key={i} className="fs-skipped-col">
                <span className="fs-skipped-col-idx">{i}</span>
                <span className="fs-skipped-col-val">{col || '—'}</span>
              </div>
            ))}
          </div>
        </div>
      )}
      {(row.contextBefore || row.contextAfter) && (
        <div className="fs-skipped-detail-block">
          <div className="fs-skipped-detail-label">Nearby lines</div>
          {row.contextBefore && (
            <div className="fs-skipped-context-line">
              <span className="fs-skipped-context-tag">prev</span>
              <code>{row.contextBefore}</code>
            </div>
          )}
          {row.contextAfter && (
            <div className="fs-skipped-context-line">
              <span className="fs-skipped-context-tag">next</span>
              <code>{row.contextAfter}</code>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function summarizePreview(rows: StatementPreviewRow[]) {
  let income = 0
  let expense = 0
  for (const row of rows) {
    const inc = Math.abs(Number(row.incomeMoney || 0))
    const exp = Math.abs(Number(row.balanceMoney || 0))
    if (inc > 0) income += inc
    else expense += exp
  }
  return { income, expense, net: income - expense }
}

export function StatementUploadPage() {
  const qc = useQueryClient()
  const [searchParams] = useSearchParams()
  const resumeId = searchParams.get('resume') || searchParams.get('statementId') || ''
  const previewTableHeight = useViewportTableHeight(280)
  const [step, setStep] = useState(0)
  const [statementId, setStatementId] = useState('')
  const [preview, setPreview] = useState<StatementPreviewRow[]>([])
  const [uploadMeta, setUploadMeta] = useState<{
    rows: number
    parsed: number
    skipped: number
    ignored?: number
    linked?: number
  } | null>(null)
  const [loading, setLoading] = useState(false)
  const [bankCode, setBankCode] = useState('CMB')
  const [cardTypeCode, setCardTypeCode] = useState('debit')
  const [bankCardId, setBankCardId] = useState('')
  const [boundCardName, setBoundCardName] = useState('')
  const [commitResult, setCommitResult] = useState<StatementCommitResult | null>(null)
  const [commitQuality, setCommitQuality] = useState<{ unclassified: number; possibleDuplicate: number } | null>(null)
  const [committedCardId, setCommittedCardId] = useState('')

  const { data: bankCards = [] } = useQuery({
    queryKey: ['bank-cards', cardTypeCode],
    queryFn: () => listBankCards(cardTypeCode),
    staleTime: 60_000,
  })

  const matchingCards = useMemo(
    () => bankCards.filter((c) => (c.bankCode || '').toUpperCase() === bankCode.toUpperCase()),
    [bankCards, bankCode],
  )

  const normalizedBankCardId = useMemo(() => {
    if (!bankCardId) return ''
    return matchingCards.some((c) => c.id === bankCardId) ? bankCardId : ''
  }, [bankCardId, matchingCards])

  const effectiveBankCardId = useMemo(() => {
    if (normalizedBankCardId) return normalizedBankCardId
    return matchingCards.length === 1 ? matchingCards[0].id : ''
  }, [normalizedBankCardId, matchingCards])

  const [previewView, setPreviewView] = useState<'parsed' | 'skipped'>('parsed')

  const resumeHandled = useRef('')

  useEffect(() => {
    if (!resumeId || step !== 0 || resumeHandled.current === resumeId) return
    resumeHandled.current = resumeId
    let cancelled = false
    ;(async () => {
      setLoading(true)
      try {
        const rows = await previewStatement(resumeId)
        if (cancelled) return
        if (rows.length === 0) {
          message.warning('No preview rows for this import — it may already be committed or expired.')
          return
        }
        setStatementId(resumeId)
        setPreview(rows)
        setBoundCardName(rows.find((r) => r.bankCardName)?.bankCardName || '')
        setUploadMeta({
          rows: rows.length,
          parsed: rows.length,
          skipped: 0,
        })
        setPreviewView('parsed')
        setStep(1)
        message.info(`Resumed preview (${rows.length} rows) — commit when ready.`)
      } catch (e) {
        if (!cancelled) {
          message.error(e instanceof Error ? e.message : 'Failed to resume import')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => { cancelled = true }
  }, [resumeId, step])

  const { data: skippedRows = [], isFetching: skippedLoading } = useQuery({
    queryKey: ['statement-skipped', statementId, cardTypeCode],
    queryFn: () => skippedStatementLines(statementId, cardTypeCode),
    enabled: step === 1 && Boolean(statementId),
    staleTime: 60_000,
  })

  const alreadyInLedgerCount = preview.filter((r) => Boolean(r.possibleDuplicate)).length
  const stats = useMemo(() => summarizePreview(preview), [preview])

  const onUpload = async (file: File) => {
    setLoading(true)
    try {
      const guessed = guessBankFromFilename(file.name)
      const effectiveBank = guessed || bankCode
      if (guessed && guessed !== bankCode) {
        setBankCode(guessed)
        message.info(`Detected ${BANK_OPTIONS.find((b) => b.value === guessed)?.label || guessed} from filename`)
      }
      const result = await uploadStatement(file, effectiveBank, cardTypeCode, undefined, effectiveBankCardId || undefined)
      setStatementId(result.statementId)
      setBoundCardName(result.bankCardName || '')
      setUploadMeta({
        rows: result.rows,
        parsed: result.parsed,
        skipped: result.skipped,
        ignored: result.ignored,
        linked: result.linked,
      })
      const rows = await previewStatement(result.statementId)
      setPreview(rows)
      setPreviewView('parsed')
      setStep(1)
      if (rows.length === 0) {
        message.warning(
          result.parsed === 0
            ? `No transactions parsed (${result.rows} raw rows). Check bank/account type matches the file.`
            : 'Preview is empty — try uploading again.',
        )
      } else {
        message.success(`Parsed ${result.parsed} transactions from ${result.rows} rows`)
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Upload failed')
    } finally {
      setLoading(false)
    }
    return false
  }

  const onCommit = async () => {
    if (!statementId) {
      message.warning('No statement to commit')
      return
    }
    setLoading(true)
    try {
      setCommitQuality({
        unclassified: preview.filter((r) => !r.consumeName?.trim()).length,
        possibleDuplicate: preview.filter((r) => r.possibleDuplicate).length,
      })
      const result = await commitStatement(statementId)
      setCommitResult(result)
      const dupNote = result.skippedDuplicates ? ` · ${result.skippedDuplicates} already in ledger (skipped)` : ''
      message.success(`Imported ${result.imported} of ${result.total} transactions${dupNote}`)
      qc.invalidateQueries({ queryKey: ['financial-pulse'] })
      qc.invalidateQueries({ queryKey: ['wealth'] })
      qc.invalidateQueries({ queryKey: ['budget-vs-actual'] })
      qc.invalidateQueries({ queryKey: ['dash-semantic'] })
      qc.invalidateQueries({ queryKey: ['dash-breakdown'] })
      qc.invalidateQueries({ queryKey: ['tx-stats'] })
      setCommittedCardId(effectiveBankCardId)
      setStep(2)
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Commit failed')
    } finally {
      setLoading(false)
    }
  }

  const reset = () => {
    setStep(0)
    setPreview([])
    setStatementId('')
    setUploadMeta(null)
    setCommitResult(null)
    setCommitQuality(null)
    setPreviewView('parsed')
    setBoundCardName('')
    setCommittedCardId('')
  }

  function cardLabel(c: BankCardRow): string {
    if (c.cardName?.trim()) return c.cardName.trim()
    const tail = c.cardNo && c.cardNo.length > 4 ? `****${c.cardNo.slice(-4)}` : c.cardNo || ''
    return [c.bankCode, c.cardTypeCode, tail].filter(Boolean).join(' ')
  }

  return (
    <DataPageLayout
      title="Import Statement"
      subtitle="Upload, preview, and commit bank statement data"
      icon={<UploadOutlined />}
    >
      <div className="fs-import-steps">
        <Steps
          current={step}
          size="small"
          items={[
            { title: 'Upload', icon: <UploadOutlined /> },
            { title: 'Preview', icon: <EyeOutlined /> },
            { title: 'Commit', icon: <CheckCircleOutlined /> },
          ]}
        />
      </div>

      {step === 0 && (
        <div className="fs-import-panel">
          <div className="fs-import-toolbar">
            <div className="fs-import-field">
              <span className="fs-import-label">Bank</span>
              <Select size="small" value={bankCode} onChange={setBankCode} style={{ minWidth: 220 }} options={BANK_OPTIONS} />
            </div>
            <div className="fs-import-field">
              <span className="fs-import-label">Account type</span>
              <Select size="small" value={cardTypeCode} onChange={setCardTypeCode} style={{ minWidth: 140 }} options={CARD_OPTIONS} />
            </div>
            <div className="fs-import-field">
              <span className="fs-import-label">Card account</span>
              <Select
                size="small"
                allowClear
                placeholder={matchingCards.length ? 'Select card (recommended)' : 'No card for this bank'}
                value={effectiveBankCardId || undefined}
                onChange={(v) => setBankCardId(v || '')}
                style={{ minWidth: 240 }}
                options={matchingCards.map((c) => ({ value: c.id, label: cardLabel(c) }))}
              />
            </div>
            <span className="fs-import-hint">
              Pick the card account so imported rows appear under Transactions → Card filter.
              {matchingCards.length === 1 ? ' Auto-selected the only matching card.' : ''}
            </span>
          </div>
          <Dragger
            className="fs-upload-dragger"
            beforeUpload={onUpload}
            showUploadList={false}
            accept=".csv,.xlsx,.xls,.pdf"
            disabled={loading}
          >
            <p className="ant-upload-drag-icon"><InboxOutlined /></p>
            <p className="ant-upload-text">Drop your statement here, or click to browse</p>
            <p className="ant-upload-hint">CSV, Excel, or PDF exports (CMB, CCB, ABC, CGB, Alipay, WeChat)</p>
            <Button type="primary" icon={<CloudUploadOutlined />} loading={loading}>
              Select statement file
            </Button>
          </Dragger>
        </div>
      )}

      {step === 1 && (
        <div className="fs-import-panel">
          <Alert
            type="info"
            showIcon
            className="fs-import-commit-banner"
            message="Preview only — not in Transactions yet"
            description={
              boundCardName
                ? `These ${preview.length} rows are staged for「${boundCardName}」. Click「Commit to ledger」to save them to the ledger.`
                : `These ${preview.length} rows are staged. Select a card account before upload, then click「Commit to ledger」— otherwise they will not show under a Card filter in Transactions.`
            }
          />
          <div className="fs-import-preview-head">
            <TransactionSummaryBar
              total={preview.length}
              income={stats.income}
              expense={stats.expense}
              net={stats.net}
              unclassified={preview.filter((r) => !r.consumeName).length}
              loading={loading}
            />
            <Space size="small" wrap className="fs-import-preview-actions">
              {uploadMeta && (
                <Tooltip
                  title={
                    uploadMeta.skipped > 0
                      ? 'Lines = linked + skipped + ignored. Txns may be fewer than linked when rows merge. Click to review skipped lines.'
                      : 'Lines = linked + ignored (headers/metadata). Txns = parsed transactions.'
                  }
                >
                  <Tag
                    className="fs-tag"
                    color={uploadMeta.skipped > 0 && previewView === 'skipped' ? 'blue' : undefined}
                    style={uploadMeta.skipped > 0 ? { cursor: 'pointer' } : undefined}
                    onClick={() => uploadMeta.skipped > 0 && setPreviewView('skipped')}
                  >
                    Lines {uploadMeta.rows} · Txns {uploadMeta.parsed}
                    {uploadMeta.skipped > 0 ? ` · Skipped ${uploadMeta.skipped}` : ''}
                    {(uploadMeta.ignored ?? 0) > 0 ? ` · Ignored ${uploadMeta.ignored}` : ''}
                  </Tag>
                </Tooltip>
              )}
              {alreadyInLedgerCount > 0 && (
                <Tag className="fs-tag" color="orange">{alreadyInLedgerCount} already in ledger</Tag>
              )}
              <Button size="small" onClick={reset}>Upload another</Button>
              <Button type="primary" size="small" loading={loading} disabled={preview.length === 0} onClick={onCommit}>
                Commit to ledger
              </Button>
            </Space>
          </div>
          {(uploadMeta?.skipped ?? 0) > 0 && (
            <div className="fs-import-preview-tabs">
              <Segmented
                size="small"
                value={previewView}
                onChange={(v) => setPreviewView(v as 'parsed' | 'skipped')}
                options={[
                  { label: `Parsed (${preview.length})`, value: 'parsed' },
                  { label: `Skipped (${uploadMeta?.skipped ?? skippedRows.length ?? 0})`, value: 'skipped' },
                ]}
              />
            </div>
          )}
          <div className="fs-table-panel fs-table-panel--nested">
            {previewView === 'skipped' ? (
            <Table
              size="small"
              className="fs-data-table fs-skipped-table"
              rowKey={(r) => `${r.fileLineNumber ?? r.lineNumber}-${r.lineNumber}`}
              dataSource={skippedRows}
              loading={skippedLoading}
              pagination={{ pageSize: 20, size: 'small', showTotal: (t) => `${t} lines` }}
              locale={{ emptyText: <EmptyState compact title="No skipped lines" description="All raw rows were imported as transactions." /> }}
              scroll={{ x: 1100, y: previewTableHeight }}
              expandable={{
                expandedRowRender: (row) => <SkippedRowDetail row={row} />,
                rowExpandable: () => true,
              }}
              columns={[
                {
                  title: 'Line',
                  width: 88,
                  fixed: 'left',
                  render: (_, r) => (
                    <span className="fs-mono" title={`Data row #${r.lineNumber}`}>
                      {r.fileLineNumber ?? r.lineNumber}
                    </span>
                  ),
                },
                {
                  title: 'Reason',
                  dataIndex: 'reason',
                  width: 360,
                  render: (v) => <span className="fs-skipped-reason">{cellText(v)}</span>,
                },
                {
                  title: 'Preview',
                  dataIndex: 'rawText',
                  width: 280,
                  ellipsis: true,
                  render: (v) => <span className="fs-cell-muted" title={cellText(v)}>{cellText(v)}</span>,
                },
                {
                  title: 'Cols',
                  width: 52,
                  align: 'center',
                  render: (_, r) => <span className="fs-mono fs-cell-muted">{r.columns?.length ?? '—'}</span>,
                },
              ]}
            />
            ) : (
            <Table
              size="small"
              className="fs-data-table"
              rowKey="id"
              dataSource={preview}
              pagination={{ pageSize: 20, size: 'small', showTotal: (t) => `${t} rows` }}
              rowClassName={(r) => (r.possibleDuplicate ? 'fs-row-warning' : 'fs-table-row')}
              locale={{ emptyText: <EmptyState compact title="No preview rows" description="Verify bank/account type and re-upload." /> }}
              scroll={{ x: 1100, y: previewTableHeight }}
              columns={[
                { title: 'Date', dataIndex: 'transactionDate', width: 96, fixed: 'left', render: (v) => <span className="fs-mono">{formatTableDate(v)}</span> },
                {
                  title: 'Type',
                  width: 72,
                  render: (_, r) => {
                    const type = previewTxnType(r)
                    return <Tag className="fs-tag" color={type === 'income' ? 'green' : 'default'}>{type}</Tag>
                  },
                },
                { title: 'Description', dataIndex: 'transactionDesc', width: 180, ellipsis: true, render: (v, r) => (
                  <Space size={4}>
                    <span className="fs-cell-text" title={cellText(v)}>{cellText(v)}</span>
                    {r.possibleDuplicate ? <Tag className="fs-tag" color="orange">Exists</Tag> : null}
                  </Space>
                ) },
                {
                  title: 'Amount',
                  width: 108,
                  align: 'right',
                  render: (_, r) => (
                    <MoneyText
                      value={previewAmount(r)}
                      type={moneyTypeFromRow(previewTxnType(r), r.balanceMoney)}
                    />
                  ),
                },
                {
                  title: 'Balance',
                  dataIndex: 'accountBalance',
                  width: 108,
                  align: 'right',
                  render: (v) => (v != null && Number(v) !== 0
                    ? <span className="fs-mono fs-cell-muted">{formatNumber(v)}</span>
                    : <span className="fs-cell-muted">—</span>),
                },
                {
                  title: 'Counterparty',
                  width: 140,
                  ellipsis: true,
                  render: (_, r) => {
                    const name = cellText(r.opponentName)
                    const acct = cellText(r.opponentAccount)
                    const text = name || acct
                    return <span className="fs-cell-muted" title={text}>{text || '—'}</span>
                  },
                },
                { title: 'Category', dataIndex: 'consumeName', width: 130, ellipsis: true, render: (v) => <span className="fs-cell-text">{cellText(v) || '—'}</span> },
                {
                  title: 'Card',
                  width: 120,
                  ellipsis: true,
                  render: (_, r) => {
                    const name = cellText(r.bankCardName) || cellText(r.cardTypeName)
                    return <span className="fs-cell-muted" title={name}>{name || '—'}</span>
                  },
                },
                { title: 'Memo', dataIndex: 'demoArea', width: 120, ellipsis: true, render: (v) => <span className="fs-cell-muted" title={cellText(v)}>{cellText(v) || '—'}</span> },
              ]}
            />
            )}
          </div>
        </div>
      )}

      {step === 2 && (
        <div className="fs-import-panel">
          {commitResult && commitQuality && (
            <ImportQualityGate
              imported={commitResult.imported}
              skippedDuplicates={commitResult.skippedDuplicates}
              unclassifiedCount={commitQuality.unclassified}
              possibleDuplicateCount={commitQuality.possibleDuplicate}
              cardId={committedCardId}
              cardLabel={boundCardName || undefined}
            />
          )}
          <div className="fs-import-commit-actions">
            <Link
              to={committedCardId ? `/transactions?cardId=${encodeURIComponent(committedCardId)}` : '/transactions'}
            >
              <Button type="primary">View in Transactions</Button>
            </Link>
            <Button onClick={reset}>Import another</Button>
          </div>
        </div>
      )}
    </DataPageLayout>
  )
}
