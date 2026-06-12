import { Link } from 'react-router-dom'
import { RightOutlined, ThunderboltOutlined } from '@ant-design/icons'
import type { AdvisorCard } from '../api/analytics'

function cardPath(card: AdvisorCard): string | undefined {
  const action = card.actions?.[0]
  if (action?.payload?.path) return action.payload.path
  return card.actionPath
}

function cardLabel(card: AdvisorCard): string {
  return card.actions?.[0]?.label || card.actionLabel || 'View'
}

export function AdvisorStrip({ cards, onDismiss }: { cards: AdvisorCard[]; onDismiss?: (id: string) => void }) {
  if (!cards.length) return null
  return (
    <div className="fs-advisor-strip">
      <div className="fs-advisor-strip-head">
        <ThunderboltOutlined />
        <span>Today&apos;s priorities</span>
      </div>
      {cards.slice(0, 3).map((card, i) => {
        const path = cardPath(card)
        return (
          <div key={card.id || `${card.title}-${i}`} className="fs-advisor-card">
            <div className="fs-advisor-card-body">
              <div className="fs-advisor-card-title">{card.title}</div>
              <div className="fs-advisor-card-reason">{card.reason || card.detail}</div>
            </div>
            <div className="fs-advisor-card-actions">
              {path && (
                <Link to={path} className="fs-advisor-card-link">
                  {cardLabel(card)}
                  <RightOutlined />
                </Link>
              )}
              {card.id && onDismiss && (
                <button type="button" className="fs-advisor-dismiss" onClick={() => onDismiss(card.id!)}>
                  Dismiss
                </button>
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}
