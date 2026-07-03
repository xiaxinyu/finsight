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
import { adminCardsCopy as copy } from '../adminLabels'

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
      message.success(copy.deleted)
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : copy.deleteFailed)
    }
  }

  const menuItems = (card: BankCardRow): MenuProps['items'] => [
    { key: 'edit', icon: <EditOutlined />, label: copy.edit, onClick: () => openEdit(card) },
    { type: 'divider' },
    {
      key: 'delete',
      icon: <DeleteOutlined />,
      label: copy.delete,
      danger: true,
      onClick: () => {
        if (window.confirm(copy.deleteConfirm)) onDelete(card)
      },
    },
  ]

  return (
    <DataPageLayout
      title={copy.title}
      subtitle={copy.subtitle}
      icon={<CreditCardOutlined />}
      className="fs-data-page--bank-cards"
      actions={(
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          {copy.addCard}
        </Button>
      )}
    >
      {isError && (
        <Typography.Paragraph type="danger">
          {error instanceof Error ? error.message : copy.loadFailed}
        </Typography.Paragraph>
      )}

      {cards.length > 0 && (
        <div className="fs-bank-cards-hero">
          <ContentCard className="fs-bank-cards-hero-card fs-bank-cards-hero-card--primary">
            <div className="fs-bank-cards-hero-card__label">{copy.totalCards}</div>
            <div className="fs-bank-cards-hero-card__value">{stats.total}</div>
          </ContentCard>
          <ContentCard className="fs-bank-cards-hero-card">
            <div className="fs-bank-cards-hero-card__label">{copy.debitCards}</div>
            <div className="fs-bank-cards-hero-card__value">{stats.debit}</div>
          </ContentCard>
          <ContentCard className="fs-bank-cards-hero-card">
            <div className="fs-bank-cards-hero-card__label">{copy.creditCards}</div>
            <div className="fs-bank-cards-hero-card__value">{stats.credit}</div>
          </ContentCard>
        </div>
      )}

      {!isLoading && cards.length === 0 ? (
        <ContentCard>
          <EmptyState
            title={copy.noCards}
            description={copy.noCardsHint}
            action={(
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
                {copy.addFirstCard}
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
                        <Button type="text" size="small" icon={<MoreOutlined />} aria-label={copy.actions} />
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
