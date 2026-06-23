import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Button, Descriptions, Drawer, List, Space, Tag, Typography } from 'antd'
import { RightOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import type { ProfileDimension } from '../../api/analytics'
import { fetchProfileHistory } from '../../api/analytics'
import { FsChart } from '../../components/FsChart'
import { EmptyState } from '../../components/EmptyState'
import { ANALYTICS_STALE_MS, QUERY_KEYS } from '../../constants/queryKeys'
import { PROFILE_DIM_LABELS } from './profileRadar'
import { buildProfileHistoryOption, historyDateRange } from './profileHistoryChart'
import { profileActionLinks } from './profileActions'

type Props = {
  open: boolean
  dimension: ProfileDimension | null
  asOf: string
  onClose: () => void
}

export function ProfileDimensionDrawer({ open, dimension, asOf, onClose }: Props) {
  const range = useMemo(() => historyDateRange(asOf), [asOf])
  const dimensionId = dimension?.id || ''

  const { data: history, isLoading } = useQuery({
    queryKey: QUERY_KEYS.profileHistory(dimensionId, range.from, range.to),
    enabled: open && !!dimensionId,
    staleTime: ANALYTICS_STALE_MS,
    queryFn: () => fetchProfileHistory(range.from, range.to, dimensionId),
  })

  const historyOption = useMemo(
    () => buildProfileHistoryOption(dimensionId, history),
    [dimensionId, history],
  )
  const actions = profileActionLinks(dimension ?? undefined)

  return (
    <Drawer
      title={dimension ? (PROFILE_DIM_LABELS[dimension.id] || dimension.id) : 'Dimension'}
      width={480}
      open={open}
      onClose={onClose}
      destroyOnClose
    >
      {!dimension ? null : (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Tag color={dimension.level === 'strong' ? 'green' : dimension.level === 'moderate' ? 'blue' : 'orange'}>
              {dimension.level.replace(/_/g, ' ')}
            </Tag>
            <Typography.Title level={3} style={{ margin: 0 }}>{dimension.score}</Typography.Title>
          </div>

          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            {dimension.reason || dimension.summary}
          </Typography.Paragraph>

          <div>
            <Typography.Text strong>Why this score</Typography.Text>
            <Typography.Paragraph style={{ marginTop: 4, marginBottom: 0 }}>
              {dimension.summary}
            </Typography.Paragraph>
          </div>

          <div>
            <Typography.Text strong>Score history</Typography.Text>
            <Typography.Paragraph type="secondary" style={{ margin: '4px 0 8px', fontSize: 12 }}>
              Daily snapshots · {range.from} → {range.to}
            </Typography.Paragraph>
            {isLoading ? (
              <FsChart option={historyOption} height={200} loading />
            ) : (
              <FsChart option={historyOption} height={200} />
            )}
          </div>

          <div>
            <Typography.Text strong>Evidence</Typography.Text>
            {dimension.evidence?.length ? (
              <List
                size="small"
                style={{ marginTop: 8 }}
                dataSource={dimension.evidence}
                renderItem={(item) => (
                  <List.Item style={{ display: 'block', paddingInline: 0 }}>
                    <Descriptions size="small" column={1} colon={false}>
                      <Descriptions.Item label={item.label || item.ref}>
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
              <EmptyState title="No evidence" description="Evidence will appear when metrics are available." />
            )}
          </div>

          {actions.length > 0 && (
            <div>
              <Typography.Text strong>Recommended actions</Typography.Text>
              <Space direction="vertical" style={{ width: '100%', marginTop: 8 }}>
                {actions.map((action) => (
                  <Link key={`${action.type}-${action.path}`} to={action.path} onClick={onClose}>
                    <Button block type="default" icon={<RightOutlined />}>
                      {action.label}
                    </Button>
                  </Link>
                ))}
              </Space>
            </div>
          )}
        </Space>
      )}
    </Drawer>
  )
}
