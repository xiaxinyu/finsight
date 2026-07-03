import { Collapse, Typography } from 'antd'
import { InfoCircleOutlined } from '@ant-design/icons'

type Props = {
  title?: string
  items: string[]
  defaultOpen?: boolean
}

/** Collapsible disclaimer for reports that use heuristics or estimated series. */
export function EstimationMethodNote({ title = 'How these numbers are calculated', items, defaultOpen = false }: Props) {
  if (!items.length) return null
  return (
    <Collapse
      ghost
      className="fs-estimation-note"
      defaultActiveKey={defaultOpen ? ['method'] : []}
      items={[{
        key: 'method',
        label: (
          <span className="fs-estimation-note-label">
            <InfoCircleOutlined /> {title}
          </span>
        ),
        children: (
          <ul className="fs-estimation-note-list">
            {items.map((item) => (
              <li key={item}><Typography.Text type="secondary">{item}</Typography.Text></li>
            ))}
          </ul>
        ),
      }]}
    />
  )
}
