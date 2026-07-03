import type { ProfileDimension } from '../../api/analytics'
import {
  profileDimensionLabel,
  profileDimensionVisual,
  profileDimensionsByPriority,
} from './profileDisplay'

type Props = {
  dimensions: ProfileDimension[]
  onSelect: (dim: ProfileDimension) => void
}

export function ProfileDimensionBarList({ dimensions, onSelect }: Props) {
  const ordered = profileDimensionsByPriority(dimensions)

  return (
    <div className="fs-profile-dim-bar-list" role="list">
      {ordered.map((dim) => {
        const visual = profileDimensionVisual(dim.id, dim.score, dim.level)
        return (
          <button
            key={dim.id}
            type="button"
            className={`fs-profile-dim-bar fs-profile-dim-bar--${visual.tier}`}
            onClick={() => onSelect(dim)}
            role="listitem"
          >
            <span className="fs-profile-dim-bar__label">{profileDimensionLabel(dim.id)}</span>
            <span className="fs-profile-dim-bar-track" aria-hidden>
              <span
                className="fs-profile-dim-bar-fill"
                style={{ width: `${Math.max(0, Math.min(100, dim.score))}%`, background: visual.color }}
              />
            </span>
            <span className="fs-profile-dim-bar__score">{dim.score}</span>
          </button>
        )
      })}
    </div>
  )
}
