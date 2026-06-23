import { Button, Descriptions, Drawer, List, Space, Tag, Typography } from 'antd'
import { RightOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import type { AdvisorCard } from '../api/analytics'
import { EmptyState } from './EmptyState'
import {
  cardPrimaryLabel,
  cardPrimaryPath,
  evidenceSourceLabel,
  formatConfidence,
  formatImpact,
  normalizeEvidence,
  urgencyColor,
  urgencyLabel,
} from '../utils/advisorCard'
import { combinedKindLabel, sectionSourceLabel } from '../utils/combinedInsight'

type Props = {
  open: boolean
  card: AdvisorCard | null
  onClose: () => void
}

export function AdvisorEvidenceDrawer({ open, card, onClose }: Props) {
  if (!card) {
    return (
      <Drawer title="Recommendation" width={480} open={open} onClose={onClose} destroyOnClose />
    )
  }

  const evidence = normalizeEvidence(card)
  const primaryPath = cardPrimaryPath(card)

  return (
    <Drawer
      title={card.title}
      width={480}
      open={open}
      onClose={onClose}
      destroyOnClose
      extra={(
        <>
          {card.combinedKind && (
            <Tag color="geekblue" style={{ marginRight: 8 }}>{combinedKindLabel(card.combinedKind)}</Tag>
          )}
          {card.urgency && <Tag color={urgencyColor(card.urgency)}>{urgencyLabel(card.urgency)}</Tag>}
        </>
      )}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Typography.Paragraph style={{ marginBottom: 0 }}>{card.reason || card.detail}</Typography.Paragraph>

        <Descriptions size="small" column={2} bordered>
          <Descriptions.Item label="Impact">{formatImpact(card)}</Descriptions.Item>
          <Descriptions.Item label="Confidence">{formatConfidence(card)}</Descriptions.Item>
          <Descriptions.Item label="Priority">{card.priority ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="Expires">{card.expiresAt ? card.expiresAt.slice(0, 10) : '—'}</Descriptions.Item>
        </Descriptions>

        {card.sections?.length ? (
          <div>
            <Typography.Text strong>Linked signals</Typography.Text>
            <List
              size="small"
              style={{ marginTop: 8 }}
              dataSource={card.sections}
              renderItem={(section) => (
                <List.Item style={{ display: 'block', paddingInline: 0 }}>
                  <Tag style={{ marginBottom: 4 }}>{sectionSourceLabel(section.key)}</Tag>
                  <Typography.Text strong style={{ display: 'block' }}>{section.title}</Typography.Text>
                  <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
                    {section.body}
                  </Typography.Paragraph>
                </List.Item>
              )}
            />
          </div>
        ) : null}

        <div>
          <Typography.Text strong>Evidence</Typography.Text>
          {evidence.length ? (
            <List
              size="small"
              style={{ marginTop: 8 }}
              dataSource={evidence}
              renderItem={(item) => (
                <List.Item style={{ display: 'block', paddingInline: 0 }}>
                  <Descriptions size="small" column={1} colon={false}>
                    <Descriptions.Item label={item.label || evidenceSourceLabel(item.source) || item.ref}>
                      <Typography.Text>{String(item.value ?? '—')}</Typography.Text>
                    </Descriptions.Item>
                    {item.detail && (
                      <Descriptions.Item label="Why it matters">
                        <Typography.Text type="secondary">{item.detail}</Typography.Text>
                      </Descriptions.Item>
                    )}
                  </Descriptions>
                </List.Item>
              )}
            />
          ) : (
            <EmptyState compact title="No evidence" description="Evidence will appear when metrics are available." />
          )}
        </div>

        <div>
          <Typography.Text strong>Recommended actions</Typography.Text>
          <Space direction="vertical" style={{ width: '100%', marginTop: 8 }}>
            {(card.actions || []).map((action) => {
              const path = action.payload?.path
              const btn = (
                <Button block type="default" icon={<RightOutlined />}>
                  {action.label}
                </Button>
              )
              return path ? (
                <Link key={`${action.type}-${path}`} to={path} onClick={onClose}>
                  {btn}
                </Link>
              ) : (
                <div key={action.type}>{btn}</div>
              )
            })}
            {primaryPath && (
              <Link to={primaryPath} onClick={onClose}>
                <Button block type="primary" icon={<RightOutlined />}>
                  {cardPrimaryLabel(card)}
                </Button>
              </Link>
            )}
          </Space>
        </div>
      </Space>
    </Drawer>
  )
}
