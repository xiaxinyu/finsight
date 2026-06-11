import { Link } from 'react-router-dom'
import {
  CheckCircleOutlined, InfoCircleOutlined, RightOutlined, WarningOutlined,
} from '@ant-design/icons'
import type { DecisionCard } from '../api/finance'

function tone(card: DecisionCard): 'warn' | 'info' | 'ok' {
  if (card.type === 'warning') return 'warn'
  if (card.title?.toLowerCase().includes('stable') || card.title?.toLowerCase().includes('strong')) return 'ok'
  return 'info'
}

export function DashboardInsightStrip({ cards }: { cards: DecisionCard[] }) {
  if (!cards.length) return null
  return (
    <div className="fs-dash-insight-strip">
      {cards.map((card, i) => {
        const t = tone(card)
        return (
          <div key={`${card.title}-${i}`} className={`fs-dash-insight fs-dash-insight--${t}`}>
            <span className="fs-dash-insight-icon" aria-hidden>
              {t === 'warn' ? <WarningOutlined /> : t === 'ok' ? <CheckCircleOutlined /> : <InfoCircleOutlined />}
            </span>
            <div className="fs-dash-insight-body">
              <div className="fs-dash-insight-title">{card.title}</div>
              <div className="fs-dash-insight-detail">{card.detail}</div>
            </div>
            {card.actionPath && (
              <Link to={card.actionPath} className="fs-dash-insight-action">
                {card.actionLabel || 'View'}
                <RightOutlined />
              </Link>
            )}
          </div>
        )
      })}
    </div>
  )
}
