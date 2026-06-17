import { Collapse, Form, InputNumber, Typography } from 'antd'
import { SlidersOutlined } from '@ant-design/icons'
import type { ScenarioInputsState } from '../utils/annualOutlookScenario'

type Props = {
  draft: ScenarioInputsState
  dirty: boolean
  disabled?: boolean
  onChange: (next: ScenarioInputsState) => void
  onApply: () => void
}

const fieldProps = { size: 'small' as const, style: { width: '100%' } }

export function AnnualOutlookScenarioInputs({
  draft,
  dirty,
  disabled,
  onChange,
  onApply,
}: Props) {
  return (
    <Collapse
      className="fs-annual-outlook-scenario"
      size="small"
      items={[{
        key: 'inputs',
        label: (
          <span>
            <SlidersOutlined style={{ marginRight: 8 }} />
            Scenario inputs
            {dirty && <Typography.Text type="warning" style={{ marginLeft: 8, fontSize: 12 }}>Unapplied changes</Typography.Text>}
          </span>
        ),
        extra: dirty ? (
          <Typography.Link
            onClick={(e) => {
              e.stopPropagation()
              onApply()
            }}
          >
            Apply
          </Typography.Link>
        ) : null,
        children: (
          <Form layout="vertical" size="small" className="fs-annual-outlook-scenario-form" disabled={disabled}>
            <div className="fs-annual-outlook-scenario-grid">
              <Form.Item label="Income change %" className="fs-annual-outlook-scenario-field">
                <InputNumber
                  {...fieldProps}
                  value={draft.incomeChangePct}
                  placeholder="e.g. -10"
                  addonAfter="%"
                  onChange={(v) => onChange({ ...draft, incomeChangePct: v })}
                />
              </Form.Item>
              <Form.Item label="Lump-sum expense (Jan)" className="fs-annual-outlook-scenario-field">
                <InputNumber
                  {...fieldProps}
                  min={0}
                  value={draft.lumpSumExpense}
                  placeholder="One-time hit"
                  onChange={(v) => onChange({ ...draft, lumpSumExpense: v })}
                />
              </Form.Item>
              <Form.Item label="New monthly bill" className="fs-annual-outlook-scenario-field">
                <InputNumber
                  {...fieldProps}
                  min={0}
                  value={draft.newMonthlyBill}
                  placeholder="Recurring add-on"
                  onChange={(v) => onChange({ ...draft, newMonthlyBill: v })}
                />
              </Form.Item>
              <Form.Item label="Target monthly payment" className="fs-annual-outlook-scenario-field">
                <InputNumber
                  {...fieldProps}
                  min={0}
                  value={draft.targetMonthlyPayment}
                  placeholder="Budget comparison cap"
                  onChange={(v) => onChange({ ...draft, targetMonthlyPayment: v })}
                />
              </Form.Item>
            </div>
            <Typography.Paragraph type="secondary" style={{ marginBottom: 0, fontSize: 12 }}>
              Adjust inputs then Apply — KPI, chart, and monthly table refresh with your assumptions.
            </Typography.Paragraph>
          </Form>
        ),
      }]}
    />
  )
}
