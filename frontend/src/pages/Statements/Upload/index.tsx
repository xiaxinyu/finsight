import { useState } from 'react'
import { Button, Card, message, Result, Select, Space, Steps, Table, Tag, Upload } from 'antd'
import { CheckCircleOutlined, EyeOutlined, UploadOutlined } from '@ant-design/icons'
import { commitStatement, previewStatement, uploadStatement } from '../../api/statement'
import { DataPageLayout } from '../../components/DataPageLayout'
import { MoneyText, moneyTypeFromRow } from '../../components/MoneyText'
import { cellText, formatTableDate } from '../../utils/cell'

export function StatementUploadPage() {
  const [step, setStep] = useState(0)
  const [statementId, setStatementId] = useState('')
  const [preview, setPreview] = useState<Record<string, unknown>[]>([])
  const [loading, setLoading] = useState(false)
  const [bankCode, setBankCode] = useState('CCB')
  const [cardTypeCode, setCardTypeCode] = useState('debit')
  const duplicateCount = preview.filter((r) => Boolean(r.possibleDuplicate)).length

  const onUpload = async (file: File) => {
    setLoading(true)
    try {
      const result = await uploadStatement(file, bankCode, cardTypeCode) as { statementId?: string; id?: string }
      const sid = result?.statementId || result?.id || String(result)
      setStatementId(sid)
      const rows = await previewStatement(sid)
      setPreview(rows as Record<string, unknown>[])
      setStep(1)
      message.success('File parsed — review preview')
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Upload failed')
    } finally {
      setLoading(false)
    }
    return false
  }

  const onCommit = async () => {
    setLoading(true)
    try {
      await commitStatement(statementId)
      message.success('Committed to ledger')
      setStep(2)
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Commit failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <DataPageLayout
      title="Import Statement"
      subtitle="Upload, preview, and commit bank statement data"
      icon={<UploadOutlined />}
    >
      <Steps
        current={step}
        style={{ marginBottom: 16 }}
        items={[
          { title: 'Upload', icon: <UploadOutlined /> },
          { title: 'Preview', icon: <EyeOutlined /> },
          { title: 'Commit', icon: <CheckCircleOutlined /> },
        ]}
      />
      {step === 0 && (
        <Card className="fs-content-card">
          <Space wrap style={{ marginBottom: 12 }}>
            <Select size="small" value={bankCode} onChange={setBankCode} style={{ width: 140 }} options={[
              { value: 'CCB', label: 'CCB' },
              { value: 'CMB', label: 'CMB' },
              { value: 'CGB', label: 'CGB' },
              { value: 'ALIPAY', label: 'Alipay' },
              { value: 'WECHAT', label: 'WeChat Pay' },
            ]} />
            <Select size="small" value={cardTypeCode} onChange={setCardTypeCode} style={{ width: 120 }} options={[
              { value: 'debit', label: 'Debit' },
              { value: 'credit', label: 'Credit' },
              { value: 'ewallet', label: 'E-wallet' },
            ]} />
          </Space>
          <Upload beforeUpload={onUpload} showUploadList={false} accept=".csv,.xlsx,.xls,.pdf">
            <Button type="primary" icon={<UploadOutlined />} loading={loading}>Select statement file</Button>
          </Upload>
        </Card>
      )}
      {step === 1 && (
        <Card
          className="fs-content-card"
          title="Preview"
          extra={(
            <Space>
              <Tag color="blue">{preview.length} rows</Tag>
              {duplicateCount > 0 && (
                <Tag color="orange">{duplicateCount} possible duplicate{duplicateCount === 1 ? '' : 's'}</Tag>
              )}
              <Button type="primary" loading={loading} onClick={onCommit}>Commit to ledger</Button>
            </Space>
          )}
        >
          <div className="fs-table-panel" style={{ border: 'none' }}>
            <Table
              size="small"
              className="fs-data-table"
              rowKey="id"
              dataSource={preview}
              pagination={{ pageSize: 20 }}
              rowClassName={(r) => (r.possibleDuplicate ? 'fs-row-warning' : '')}
              columns={[
              { title: 'Date', dataIndex: 'transactionDate', width: 100, render: (v) => formatTableDate(v) },
              { title: 'Description', dataIndex: 'transactionDesc', ellipsis: true, render: (v, r) => (
                <Space size={4}>
                  {cellText(v)}
                  {r.possibleDuplicate ? <Tag color="orange">Duplicate</Tag> : null}
                </Space>
              ) },
              { title: 'Amount', dataIndex: 'balanceMoney', align: 'right', render: (_, r) => <MoneyText value={Number(r.balanceMoney)} type={moneyTypeFromRow(r.txnType as string, r.balanceMoney as number)} unit /> },
              { title: 'Category', dataIndex: 'consumeName', render: (v) => cellText(v) },
            ]}
            />
          </div>
        </Card>
      )}
      {step === 2 && (
        <Result
          status="success"
          title="Statement committed"
          subTitle={`${preview.length} transactions imported to the ledger.`}
          extra={<Button type="primary" onClick={() => { setStep(0); setPreview([]); setStatementId('') }}>Import another</Button>}
        />
      )}
    </DataPageLayout>
  )
}
