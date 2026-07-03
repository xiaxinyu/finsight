import { useCallback, useMemo, useState, type CSSProperties } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Dropdown, Tag, Typography, message, type MenuProps } from 'antd'
import {
  CreditCardOutlined, DeleteOutlined, EditOutlined, MoreOutlined, PlusOutlined,
} from '@ant-design/icons'
import { deleteCard, listCardsAdmin } from '../../../api/admin'
import { BankCardFormDrawer, type BankCardRow } from '../../../components/BankCardFormDrawer'
import { ContentCard } from '../../../components/ContentCard'
import { DataPageLayout } from '../../../components/DataPageLayout'
import { EmptyState } from '../../../components/EmptyState'
import {
  bankAccent, bankInitial, cardTypeLabel, displayCardTitle, maskCardNo,
} from '../../../utils/bankCardDisplay'

export function CardsAdminPage() {
  const queryClient = useQueryClient()
  const [drawer, setDrawer] = useState<BankCardRow | null | undefined>(undefined)

  const { data: cards = [], isLoading, isError, error } = useQuery({
    queryKey: ['bank-cards-admin'],
    queryFn: () => listCardsAdmin() as Promise<BankCardRow[]>,
  })

  const reload = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['bank-cards-admin'] })
    queryClient.invalidateQueries({ queryKey: ['accounts'] })
    queryClient.invalidateQueries({ queryKey: ['bank-cards'] })
  }, [queryClient])

  const stats = useMemo(() => ({
    total: cards.length,
    debit: cards.filter((c) => c.cardTypeCode === 'debit').length,
    credit: cards.filter((c) => c.cardTypeCode === 'credit').length,
  }), [cards])

  const openCreate = () => setDrawer(null)
  const openEdit = (card: BankCardRow) => setDrawer(card)
  const closeDrawer = () => setDrawer(undefined)

  const onDelete = async (card: BankCardRow) => {
    if (!card.id) return
    try {
      await deleteCard(card.id)
      message.success('已删除')
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  const menuItems = (card: BankCardRow): MenuProps['items'] => [
    { key: 'edit', icon: <EditOutlined />, label: '编辑', onClick: () => openEdit(card) },
    { type: 'divider' },
    {
      key: 'delete',
      icon: <DeleteOutlined />,
      label: '删除',
      danger: true,
      onClick: () => {
        if (window.confirm('确定删除此银行卡？')) onDelete(card)
      },
    },
  ]

  return (
    <DataPageLayout
      title="银行卡"
      subtitle="管理账户卡号 · 用于账单导入与贷款流水关联"
      icon={<CreditCardOutlined />}
      className="fs-data-page--bank-cards"
      actions={(
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          添加银行卡
        </Button>
      )}
    >
      {isError && (
        <Typography.Paragraph type="danger">
          {error instanceof Error ? error.message : '加载失败'}
        </Typography.Paragraph>
      )}

      {cards.length > 0 && (
        <div className="fs-bank-cards-hero">
          <ContentCard className="fs-bank-cards-hero-card fs-bank-cards-hero-card--primary">
            <div className="fs-bank-cards-hero-card__label">银行卡总数</div>
            <div className="fs-bank-cards-hero-card__value">{stats.total}</div>
          </ContentCard>
          <ContentCard className="fs-bank-cards-hero-card">
            <div className="fs-bank-cards-hero-card__label">借记卡</div>
            <div className="fs-bank-cards-hero-card__value">{stats.debit}</div>
          </ContentCard>
          <ContentCard className="fs-bank-cards-hero-card">
            <div className="fs-bank-cards-hero-card__label">信用卡</div>
            <div className="fs-bank-cards-hero-card__value">{stats.credit}</div>
          </ContentCard>
        </div>
      )}

      {!isLoading && cards.length === 0 ? (
        <ContentCard>
          <EmptyState
            title="还没有银行卡"
            description="添加银行卡后，可导入账单并在贷款页关联放款/还款流水。"
            action={(
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
                添加第一张卡
              </Button>
            )}
          />
        </ContentCard>
      ) : (
        <div className="fs-bank-card-grid">
          {cards.map((card) => {
            const accent = bankAccent(card.bankCode)
            return (
              <ContentCard key={card.id} className="fs-bank-card-item" styles={{ body: { padding: 0 } }}>
                <div
                  className="fs-bank-card-item-inner"
                  style={{ '--bank-accent': accent } as CSSProperties}
                  onClick={() => openEdit(card)}
                  onKeyDown={(e) => e.key === 'Enter' && openEdit(card)}
                  role="button"
                  tabIndex={0}
                >
                  <div className="fs-bank-card-item-head">
                    <div className="fs-bank-card-item-avatar">{bankInitial(card.bankCode, card.cardName)}</div>
                    <div className="fs-bank-card-item-title">{displayCardTitle(card)}</div>
                    <div onClick={(e) => e.stopPropagation()}>
                      <Dropdown menu={{ items: menuItems(card) }} trigger={['click']}>
                        <Button type="text" size="small" icon={<MoreOutlined />} aria-label="操作" />
                      </Dropdown>
                    </div>
                  </div>
                  <div className="fs-bank-card-item-no">{maskCardNo(card.cardNo)}</div>
                  <div className="fs-bank-card-item-foot">
                    <Tag className="fs-bank-card-item-tag">{cardTypeLabel(card.cardTypeCode)}</Tag>
                    <span className="fs-bank-card-item-id">{card.id}</span>
                  </div>
                </div>
              </ContentCard>
            )
          })}
        </div>
      )}

      <BankCardFormDrawer
        open={drawer !== undefined}
        card={drawer}
        onClose={closeDrawer}
        onSaved={() => {
          reload()
          closeDrawer()
        }}
      />
    </DataPageLayout>
  )
}
