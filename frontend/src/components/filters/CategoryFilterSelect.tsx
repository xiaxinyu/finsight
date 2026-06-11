import { useMemo } from 'react'
import { Cascader } from 'antd'
import { useConsumeTreeSelect } from '../../hooks/useConsumeTree'
import {
  cascaderSearchFilter,
  findCascaderPath,
  type CascaderOption,
} from './treeToCascader'

type Props = {
  value: string
  onChange: (categoryId: string) => void
  disabled?: boolean
  txnType?: string
}

function treeSelectToCascader(
  nodes: { title: string; value: string; children?: typeof nodes }[],
): CascaderOption[] {
  return nodes.map((n) => ({
    value: n.value,
    label: n.title,
    children: n.children?.length ? treeSelectToCascader(n.children) : undefined,
  }))
}

export function CategoryFilterSelect({ value, onChange, disabled, txnType }: Props) {
  const { treeData, isLoading } = useConsumeTreeSelect(txnType)
  const options = useMemo(() => treeSelectToCascader(treeData), [treeData])
  const cascaderValue = useMemo(
    () => (value ? findCascaderPath(options, value) : undefined),
    [options, value],
  )

  return (
    <Cascader<CascaderOption>
      className="fs-filter-control fs-filter-control--category"
      size="small"
      allowClear
      placeholder="Category"
      disabled={disabled || isLoading}
      loading={isLoading}
      options={options}
      value={cascaderValue ?? undefined}
      expandTrigger="hover"
      changeOnSelect
      popupClassName="fs-filter-cascader-popup"
      showSearch={{ filter: cascaderSearchFilter }}
      displayRender={(labels) => labels.join(' / ')}
      onChange={(path) => onChange(path?.length ? String(path[path.length - 1]) : '')}
    />
  )
}
