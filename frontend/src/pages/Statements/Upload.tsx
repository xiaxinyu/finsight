import { useState } from 'react'
import { Button, Card, message, Result, Steps, Table, Upload } from 'antd'
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

  const onUpload = async (file: File) => {
    setLoading(true)
    try {
      const result = await uploadStatement(file) as { statementId?: string; id?: string }
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
          <Upload beforeUpload={onUpload} showUploadList={false} accept=".csv,.xlsx,.xls,.pdf">
            <Button type="primary" icon={<UploadOutlined />} loading={loading}>Select statement file</Button>
          </Upload>
        </Card>
      )}
      {step === 1 && (
        <Card
          className="fs-content-card"
          title="Preview"
          extra={<Button type="primary" loading={loading} onClick={onCommit}>Commit to ledger</Button>}
        >
          <div className="fs-table-panel" style={{ border: 'none' }}>
            <Table size="small" className="fs-data-table" rowKey="id" dataSource={preview} pagination={{ pageSize: 20 }} columns={[
              { title: 'Date', dataIndex: 'transactionDate', width: 100, render: (v) => formatTableDate(v) },
              { title: 'Description', dataIndex: 'transactionDesc', ellipsis: true, render: (v) => cellText(v) },
              { title: 'Amount', dataIndex: 'balanceMoney', align: 'right', render: (_, r) => <MoneyText value={Number(r.balanceMoney)} type={moneyTypeFromRow(r.txnType as string, r.balanceMoney as number)} unit /> },
              { title: 'Category', dataIndex: 'consumeName', render: (v) => cellText(v) },
            ]} />
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
