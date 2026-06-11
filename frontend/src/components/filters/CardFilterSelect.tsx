import { useMemo } from 'react'
import { Cascader } from 'antd'
import { useCardTree } from '../../hooks/useCardTree'
import {
  cascaderSearchFilter,
  findCascaderPath,
  treeToCascaderOptions,
  type CascaderOption,
} from './treeToCascader'

type Props = {
  value: string
  onChange: (cardId: string) => void
  disabled?: boolean
}

export function CardFilterSelect({ value, onChange, disabled }: Props) {
  const { tree, isLoading } = useCardTree()
  const options = useMemo(() => treeToCascaderOptions(tree), [tree])
  const cascaderValue = useMemo(
    () => (value ? findCascaderPath(options, value) : undefined),
    [options, value],
  )

  return (
    <Cascader<CascaderOption>
      className="fs-filter-control fs-filter-control--card"
      size="small"
      allowClear
      placeholder="Card"
      disabled={disabled || isLoading}
      loading={isLoading}
      options={options}
      value={cascaderValue ?? undefined}
      expandTrigger="hover"
      changeOnSelect={false}
      popupClassName="fs-filter-cascader-popup"
      showSearch={{ filter: cascaderSearchFilter }}
      displayRender={(labels) => labels[labels.length - 1] || ''}
      onChange={(path) => onChange(path?.length ? String(path[path.length - 1]) : '')}
    />
  )
}
