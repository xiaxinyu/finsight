import { useState } from 'react'
import { Button, Card, message, Steps, Table, Upload } from 'antd'
import { PageContainer } from '@ant-design/pro-components'
import { UploadOutlined } from '@ant-design/icons'
import { commitStatement, previewStatement, uploadStatement } from '../../api/statement'
import { MoneyText } from '../../components/MoneyText'
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
    <PageContainer title="Import Statement">
      <Steps current={step} style={{ marginBottom: 24 }} items={[
        { title: 'Upload' },
        { title: 'Preview' },
        { title: 'Commit' },
      ]} />
      {step === 0 && (
        <Card>
          <Upload beforeUpload={onUpload} showUploadList={false} accept=".csv,.xlsx,.xls,.pdf">
            <Button icon={<UploadOutlined />} loading={loading}>Select statement file</Button>
          </Upload>
        </Card>
      )}
      {step >= 1 && (
        <Card title="Preview" extra={step === 1 && <Button type="primary" loading={loading} onClick={onCommit}>Commit to ledger</Button>}>
          <Table size="small" rowKey="id" dataSource={preview} pagination={{ pageSize: 20 }} columns={[
            { title: 'Date', dataIndex: 'transactionDate', width: 100, render: (v) => formatTableDate(v) },
            { title: 'Description', dataIndex: 'transactionDesc', ellipsis: true, render: (v) => cellText(v) },
            { title: 'Amount', dataIndex: 'balanceMoney', align: 'right', render: (_, r) => <MoneyText value={Number(r.balanceMoney)} unit /> },
            { title: 'Category', dataIndex: 'consumeName', render: (v) => cellText(v) },
          ]} />
        </Card>
      )}
    </PageContainer>
  )
}
