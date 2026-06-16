import { Link } from 'react-router-dom'
import { RightOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { Tag } from 'antd'
import type { AdvisorCard } from '../api/analytics'
import {
  cardPrimaryLabel,
  cardPrimaryPath,
  formatConfidence,
  formatImpact,
  urgencyColor,
  urgencyLabel,
} from '../utils/advisorCard'

type AdvisorStripProps = {
  cards: AdvisorCard[]
  onOpenEvidence?: (card: AdvisorCard) => void
  onAccept?: (id: string) => void
  onSnooze?: (id: string) => void
  onDismiss?: (id: string) => void
}

export function AdvisorStrip({
  cards,
  onOpenEvidence,
  onAccept,
  onSnooze,
  onDismiss,
}: AdvisorStripProps) {
  if (!cards.length) return null
  return (
    <div className="fs-advisor-strip">
      <div className="fs-advisor-strip-head">
        <ThunderboltOutlined />
        <span>Today&apos;s priorities</span>
      </div>
      {cards.slice(0, 3).map((card, i) => {
        const path = cardPrimaryPath(card)
        const cardId = card.id || `${card.title}-${i}`
        return (
          <div key={cardId} className="fs-advisor-card">
            <div className="fs-advisor-card-body">
              <div className="fs-advisor-card-top">
                <div className="fs-advisor-card-title">{card.title}</div>
                {card.urgency && (
                  <Tag color={urgencyColor(card.urgency)} className="fs-advisor-urgency">
                    {urgencyLabel(card.urgency)}
                  </Tag>
                )}
              </div>
              <div className="fs-advisor-card-reason">{card.reason || card.detail}</div>
              <div className="fs-advisor-card-meta">
                <span>Impact: <strong>{formatImpact(card)}</strong></span>
                <span>Confidence: <strong>{formatConfidence(card)}</strong></span>
                {path && <span>Next: <strong>{cardPrimaryLabel(card)}</strong></span>}
              </div>
            </div>
            <div className="fs-advisor-card-actions">
              {onOpenEvidence && (
                <button type="button" className="fs-advisor-link-btn" onClick={() => onOpenEvidence(card)}>
                  View evidence
                </button>
              )}
              {path && (
                <Link to={path} className="fs-advisor-card-link">
                  {cardPrimaryLabel(card)}
                  <RightOutlined />
                </Link>
              )}
              {card.id && onAccept && (
                <button type="button" className="fs-advisor-link-btn" onClick={() => onAccept(card.id!)}>
                  Accept
                </button>
              )}
              {card.id && onSnooze && (
                <button type="button" className="fs-advisor-link-btn" onClick={() => onSnooze(card.id!)}>
                  Snooze
                </button>
              )}
              {card.id && onDismiss && (
                <button type="button" className="fs-advisor-dismiss" onClick={() => onDismiss(card.id!)}>
                  Ignore 7 days
                </button>
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}
