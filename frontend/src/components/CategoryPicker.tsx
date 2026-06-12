import { useMemo } from 'react'
import { Cascader } from 'antd'
import { useConsumeTreeSelect, type TreeSelectNode } from '../hooks/useConsumeTree'
import {
  cascaderSearchFilter,
  findCascaderPath,
  labeledTreeToCascader,
  type CascaderOption,
} from './filters/treeToCascader'

type Props = {
  value?: string
  onChange?: (categoryCode: string) => void
  placeholder?: string
  disabled?: boolean
  loading?: boolean
  size?: 'small' | 'middle' | 'large'
  className?: string
  treeData?: TreeSelectNode[]
  txnType?: string
  /** Wider popup for table / modal editing (default). Set false for compact toolbar filters. */
  largePopup?: boolean
}

export function CategoryPicker({
  value = '',
  onChange,
  placeholder = 'Select category',
  disabled,
  loading: loadingProp,
  size = 'middle',
  className,
  treeData: treeDataProp,
  txnType,
  largePopup = true,
}: Props) {
  const { treeData: hookTree, isLoading } = useConsumeTreeSelect(txnType)
  const treeData = treeDataProp ?? hookTree
  const loading = loadingProp ?? (!treeDataProp && isLoading)

  const options = useMemo(() => labeledTreeToCascader(treeData), [treeData])
  const cascaderValue = useMemo(
    () => (value ? findCascaderPath(options, value) : undefined),
    [options, value],
  )

  return (
    <Cascader<CascaderOption>
      className={['fs-category-picker', className].filter(Boolean).join(' ')}
      size={size}
      allowClear
      placeholder={placeholder}
      disabled={disabled || loading}
      loading={loading}
      options={options}
      value={cascaderValue ?? undefined}
      expandTrigger="hover"
      changeOnSelect
      popupClassName={largePopup ? 'fs-category-picker-popup' : 'fs-filter-cascader-popup'}
      showSearch={{ filter: cascaderSearchFilter, matchInputWidth: true }}
      displayRender={(labels) => labels.join(' / ')}
      placement="bottomLeft"
      getPopupContainer={() => document.body}
      onChange={(path) => onChange?.(path?.length ? String(path[path.length - 1]) : '')}
    />
  )
}
