import { useCallback, useEffect, useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Button, Form, Input, InputNumber, message, Select, Space, Tree,
} from 'antd'
import { ClusterOutlined, PlusOutlined } from '@ant-design/icons'
import {
  createCategory,
  deleteCategory,
  fetchCategoryImpactPreview,
  listCategoriesAdmin,
  migrateCategory,
  updateCategory,
  type CategoryImpactPreview,
  type ConsumeCategoryRow,
} from '../../../api/admin'
import { CategoryImpactPreviewModal } from '../../../components/CategoryImpactPreviewModal'
import { DataPageLayout } from '../../../components/DataPageLayout'
import { EmptyState } from '../../../components/EmptyState'
import { PageSkeleton } from '../../../components/PageSkeleton'
import { buildCategoryTree, toAntTreeNodes } from '../../../utils/categoryTree'

const EMPTY: ConsumeCategoryRow = { name: '', code: '', parentId: '', sortNo: 1, txnTypes: 'expense' }

type PendingAction = 'delete' | 'rename' | 'merge' | null

export function CategoriesAdminPage() {
  const qc = useQueryClient()
  const [form] = Form.useForm<ConsumeCategoryRow>()
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [impactOpen, setImpactOpen] = useState(false)
  const [impactLoading, setImpactLoading] = useState(false)
  const [impactPreview, setImpactPreview] = useState<CategoryImpactPreview | null>(null)
  const [pendingAction, setPendingAction] = useState<PendingAction>(null)
  const [pendingValues, setPendingValues] = useState<ConsumeCategoryRow | null>(null)
  const [mergeTargetCode, setMergeTargetCode] = useState<string | null>(null)
  const [confirmLoading, setConfirmLoading] = useState(false)

  const { data: categories = [], isLoading, isError, error } = useQuery({
    queryKey: ['admin-categories'],
    queryFn: () => listCategoriesAdmin(),
  })

  const treeData = useMemo(() => toAntTreeNodes(buildCategoryTree(categories)), [categories])
  const selected = useMemo(
    () => categories.find((c) => c.id === selectedId) || null,
    [categories, selectedId],
  )
  const parentOptions = useMemo(
    () => categories
      .filter((c) => c.deleted !== 1 && (c.level === 1 || !c.parentId))
      .map((c) => ({ value: c.code, label: c.name || c.code })),
    [categories],
  )

  useEffect(() => {
    if (creating) {
      form.setFieldsValue({ ...EMPTY, parentId: selected?.code || '' })
      return
    }
    if (selected) form.setFieldsValue(selected)
    else form.resetFields()
  }, [selected, creating, form])

  const reload = () => {
    qc.invalidateQueries({ queryKey: ['admin-categories'] })
    qc.invalidateQueries({ queryKey: ['consume-tree'] })
  }

  const closeImpact = () => {
    setImpactOpen(false)
    setImpactPreview(null)
    setPendingAction(null)
    setPendingValues(null)
    setMergeTargetCode(null)
  }

  const openImpactPreview = useCallback(async (
    action: PendingAction,
    values?: ConsumeCategoryRow,
    targetCode?: string,
  ) => {
    if (!selected?.id || !action) return
    setPendingAction(action)
    setPendingValues(values ?? null)
    setMergeTargetCode(targetCode ?? null)
    setImpactOpen(true)
    setImpactLoading(true)
    setImpactPreview(null)
    try {
      const preview = await fetchCategoryImpactPreview(
        selected.id,
        action,
        targetCode,
      )
      setImpactPreview(preview)
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Failed to load impact preview')
      closeImpact()
    } finally {
      setImpactLoading(false)
    }
  }, [selected?.id])

  const onSave = async () => {
    const values = await form.validateFields()
    if (creating) {
      try {
        await createCategory(values)
        message.success('Category created')
        setCreating(false)
        reload()
      } catch (e) {
        message.error(e instanceof Error ? e.message : 'Save failed')
      }
      return
    }
    if (!selected?.id) return
    const nameChanged = (values.name ?? '').trim() !== (selected.name ?? '').trim()
    if (nameChanged) {
      await openImpactPreview('rename', values)
      return
    }
    try {
      await updateCategory(selected.id, values, true)
      message.success('Category updated')
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Save failed')
    }
  }

  const onDeleteClick = () => {
    if (!selected?.id) return
    openImpactPreview('delete')
  }

  const onMergeClick = () => {
    if (!selected?.id || !mergeTargetCode) {
      message.warning('Select a merge target category first')
      return
    }
    if (mergeTargetCode === selected.code) {
      message.warning('Source and target must differ')
      return
    }
    openImpactPreview('merge', undefined, mergeTargetCode)
  }

  const mergeTargetOptions = useMemo(
    () => categories
      .filter((c) => c.deleted !== 1 && c.code && c.code !== selected?.code)
      .map((c) => ({ value: c.code!, label: `${c.name || c.code} (${c.code})` })),
    [categories, selected?.code],
  )

  const confirmImpact = async () => {
    if (!selected?.id || !pendingAction) return
    setConfirmLoading(true)
    try {
      if (pendingAction === 'delete') {
        await deleteCategory(selected.id)
        message.success('Category deleted')
        setSelectedId(null)
      } else if (pendingAction === 'rename' && pendingValues) {
        await updateCategory(selected.id, pendingValues, true)
        message.success('Category updated')
      } else if (pendingAction === 'merge' && mergeTargetCode) {
        await migrateCategory(selected.id, mergeTargetCode, true, false)
        message.success('Category merged')
        setSelectedId(null)
      }
      closeImpact()
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Operation failed')
    } finally {
      setConfirmLoading(false)
    }
  }

  const deleteBlocked = (impactPreview?.childCategoryCount ?? 0) > 0

  return (
    <DataPageLayout
      title="Categories"
      subtitle="Expense and income category hierarchy"
      icon={<ClusterOutlined />}
      className="fs-data-page--dense fs-data-page--fill"
      actions={(
        <Space size="small">
          <Button
            size="small"
            icon={<PlusOutlined />}
            onClick={() => { setCreating(true); setSelectedId(null) }}
          >
            Add category
          </Button>
        </Space>
      )}
    >
      {isError && (
        <Alert type="error" showIcon style={{ marginBottom: 8 }}
          message="Failed to load categories" description={error instanceof Error ? error.message : 'Request failed'} />
      )}
      <div className="fs-admin-split">
        <div className="fs-admin-split-tree fs-table-panel">
          {isLoading ? (
            <PageSkeleton variant="table" />
          ) : !treeData.length ? (
            <EmptyState compact title="No categories" />
          ) : (
            <Tree
              showLine
              treeData={treeData}
              selectedKeys={selectedId && !creating ? [selectedId] : []}
              onSelect={(keys) => {
                setCreating(false)
                setSelectedId(keys[0] ? String(keys[0]) : null)
              }}
            />
          )}
        </div>
        <div className="fs-admin-split-form fs-table-panel">
          {creating || selected ? (
            <Form form={form} layout="vertical" size="small" className="fs-admin-category-form">
              <Form.Item name="name" label="Name" rules={[{ required: true, message: 'Name is required' }]}>
                <Input />
              </Form.Item>
              <Form.Item name="code" label="Code" tooltip="Leave blank to auto-generate">
                <Input placeholder="Auto-generated if empty" disabled={!creating && Boolean(selected?.id)} />
              </Form.Item>
              <Form.Item name="parentId" label="Parent category">
                <Select allowClear options={parentOptions} placeholder="Root category (no parent)" />
              </Form.Item>
              <Form.Item name="txnTypes" label="Transaction types">
                <Select options={[
                  { value: 'expense', label: 'Expense' },
                  { value: 'income', label: 'Income' },
                  { value: 'expense,income', label: 'Both' },
                ]} />
              </Form.Item>
              <Form.Item name="sortNo" label="Sort order">
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
              <Space wrap>
                <Button type="primary" onClick={onSave}>{creating ? 'Create' : 'Save'}</Button>
                {!creating && selected?.id && (
                  <>
                    <Button danger onClick={onDeleteClick}>Delete</Button>
                    <Select
                      allowClear
                      size="small"
                      placeholder="Merge into…"
                      style={{ minWidth: 180 }}
                      options={mergeTargetOptions}
                      value={mergeTargetCode ?? undefined}
                      onChange={(v) => setMergeTargetCode(v ?? null)}
                    />
                    <Button size="small" disabled={!mergeTargetCode} onClick={onMergeClick}>Merge</Button>
                  </>
                )}
                {creating && (
                  <Button onClick={() => setCreating(false)}>Cancel</Button>
                )}
              </Space>
            </Form>
          ) : (
            <EmptyState compact title="Select a category" description="Choose a node in the tree or add a new category." />
          )}
        </div>
      </div>

      <CategoryImpactPreviewModal
        open={impactOpen}
        loading={impactLoading || confirmLoading}
        preview={impactPreview}
        actionLabel={
          pendingAction === 'rename'
            ? 'Rename category'
            : pendingAction === 'merge'
              ? 'Merge category'
              : 'Delete category'
        }
        confirmLabel={
          pendingAction === 'rename'
            ? 'Save changes'
            : pendingAction === 'merge'
              ? 'Merge categories'
              : 'Delete category'
        }
        confirmDanger={pendingAction === 'delete' || pendingAction === 'merge'}
        confirmDisabled={pendingAction === 'delete' && deleteBlocked}
        onCancel={closeImpact}
        onConfirm={confirmImpact}
      />
    </DataPageLayout>
  )
}
