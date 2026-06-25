import { useCallback, useEffect, useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Button, Cascader, Form, Input, InputNumber, Modal, message, Select, Space, Tag, Tree, Typography,
} from 'antd'
import { ClusterOutlined, PlusOutlined } from '@ant-design/icons'
import {
  createCategory,
  deleteCategory,
  fetchCategoryAsset,
  fetchCategoryAssetSummary,
  fetchCategoryImpactPreview,
  listCategoriesAdmin,
  migrateCategory,
  updateCategory,
  type CategoryChildCandidate,
  type CategoryImpactPreview,
  type ConsumeCategoryRow,
} from '../../../api/admin'
import { CategoryAssetPanel, CategoryCandidateConfirmModal } from '../../../components/CategoryAssetPanel'
import { CategoryImpactPreviewModal } from '../../../components/CategoryImpactPreviewModal'
import { DataPageLayout } from '../../../components/DataPageLayout'
import { EmptyState } from '../../../components/EmptyState'
import { PageSkeleton } from '../../../components/PageSkeleton'
import {
  cascaderSearchFilter,
  findCascaderPath,
  type CascaderOption,
} from '../../../components/filters/treeToCascader'
import { useConsumeTreeSelect } from '../../../hooks/useConsumeTree'
import {
  applyMergeTargetConstraints,
  buildCategoryTree,
  collectSubtreeCodesFromTree,
  toAntTreeNodesWithCounts,
} from '../../../utils/categoryTree'
import type { CategoryTreeSelectNode } from '../../../utils/categoryTree'
import {
  economicNatureLabel,
  inclusionSummary,
  reportRoleLabel,
} from '../../../utils/categorySemantics'

function toMergeCascaderOptions(nodes: CategoryTreeSelectNode[]): CascaderOption[] {
  return nodes.map((n) => ({
    value: n.value,
    label: n.title,
    disabled: n.disabled,
    children: n.children?.length ? toMergeCascaderOptions(n.children) : undefined,
  }))
}

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
  const [reportImpactOpen, setReportImpactOpen] = useState(false)
  const [candidateDraft, setCandidateDraft] = useState<CategoryChildCandidate | null>(null)
  const [candidateSaving, setCandidateSaving] = useState(false)

  const { data: categories = [], isLoading, isError, error } = useQuery({
    queryKey: ['admin-categories'],
    queryFn: () => listCategoriesAdmin(),
  })
  const { data: assetSummary = {} } = useQuery({
    queryKey: ['admin-category-asset-summary'],
    queryFn: () => fetchCategoryAssetSummary(),
    staleTime: 60_000,
  })
  const { treeData: consumeTreeData } = useConsumeTreeSelect()

  const { data: categoryAsset, isLoading: assetLoading } = useQuery({
    queryKey: ['admin-category-asset', selectedId],
    queryFn: () => fetchCategoryAsset(selectedId!),
    enabled: Boolean(selectedId) && !creating,
    staleTime: 30_000,
  })

  const treeData = useMemo(
    () => toAntTreeNodesWithCounts(buildCategoryTree(categories), assetSummary, categories),
    [categories, assetSummary],
  )
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
    qc.invalidateQueries({ queryKey: ['admin-category-asset-summary'] })
    qc.invalidateQueries({ queryKey: ['admin-category-asset'] })
    qc.invalidateQueries({ queryKey: ['consume-tree'] })
  }

  const onCreateCandidate = async () => {
    if (!candidateDraft?.code || !selected) return
    setCandidateSaving(true)
    try {
      await createCategory({
        code: candidateDraft.code,
        name: candidateDraft.name,
        parentId: selected.code,
        sortNo: candidateDraft.sortNo ?? 99,
        txnTypes: candidateDraft.txnTypes ?? 'expense',
      })
      message.success(`Category ${candidateDraft.code} created`)
      setCandidateDraft(null)
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Create failed')
    } finally {
      setCandidateSaving(false)
    }
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

  const mergeTargetTree = useMemo(() => {
    if (!selected?.code || !consumeTreeData.length) return []
    const excludeCodes = collectSubtreeCodesFromTree(consumeTreeData, selected.code)
    const sourceIsL1 = selected.level === 1 || !selected.parentId
    return applyMergeTargetConstraints(consumeTreeData, {
      excludeCodes,
      l1TargetsOnly: sourceIsL1,
    })
  }, [consumeTreeData, selected])

  const mergeCascaderOptions = useMemo(
    () => toMergeCascaderOptions(mergeTargetTree),
    [mergeTargetTree],
  )

  const mergeCascaderValue = useMemo(
    () => (mergeTargetCode ? findCascaderPath(mergeCascaderOptions, mergeTargetCode) ?? undefined : undefined),
    [mergeCascaderOptions, mergeTargetCode],
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
        await migrateCategory(selected.id, mergeTargetCode, true, true)
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
            <div className="fs-admin-split-tree-scroll">
              <PageSkeleton variant="table" />
            </div>
          ) : !treeData.length ? (
            <EmptyState compact title="No categories" />
          ) : (
            <div className="fs-admin-split-tree-scroll">
              <Tree
                showLine
                treeData={treeData}
                selectedKeys={selectedId && !creating ? [selectedId] : []}
                onSelect={(keys) => {
                  setCreating(false)
                  setSelectedId(keys[0] ? String(keys[0]) : null)
                }}
              />
            </div>
          )}
        </div>
        <div className="fs-admin-split-form fs-table-panel fs-admin-category-detail">
          {creating || selected ? (
            <div className="fs-admin-category-detail-inner">
              <Form form={form} layout="vertical" size="small" className="fs-admin-category-form">
                <div className="fs-admin-category-form-fields">
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
                  {!creating && categoryAsset?.reportRole && (
                    <div className="fs-admin-category-semantics">
                      <Typography.Text type="secondary" className="fs-admin-category-semantics-label">
                        Finance semantics
                      </Typography.Text>
                      <Space wrap size={[4, 4]}>
                        <Tag color="blue">{reportRoleLabel(categoryAsset.reportRole)}</Tag>
                        <Tag>{economicNatureLabel(categoryAsset.economicNature)}</Tag>
                      </Space>
                      <Typography.Text type="secondary" className="fs-admin-category-semantics-hint">
                        {inclusionSummary(categoryAsset)}
                      </Typography.Text>
                    </div>
                  )}
                  <Form.Item name="sortNo" label="Sort order">
                    <InputNumber min={1} style={{ width: '100%' }} />
                  </Form.Item>
                </div>
                <div className="fs-admin-category-form-actions">
                  <Space wrap size="small">
                    <Button type="primary" onClick={onSave}>{creating ? 'Create' : 'Save'}</Button>
                    {!creating && selected?.id && (
                      <>
                        <Button danger onClick={onDeleteClick}>Delete</Button>
                        <Cascader<CascaderOption>
                          allowClear
                          size="small"
                          placeholder="Merge into…"
                          className="fs-category-merge-target"
                          style={{ minWidth: 240 }}
                          options={mergeCascaderOptions}
                          value={mergeCascaderValue}
                          onChange={(path) => setMergeTargetCode(path?.length ? String(path[path.length - 1]) : null)}
                          changeOnSelect
                          expandTrigger="hover"
                          popupClassName="fs-category-picker-popup"
                          showSearch={{ filter: cascaderSearchFilter, matchInputWidth: true }}
                          displayRender={(labels) => labels.join(' / ')}
                          getPopupContainer={() => document.body}
                        />
                        <Button size="small" disabled={!mergeTargetCode} onClick={onMergeClick}>Merge</Button>
                      </>
                    )}
                    {creating && (
                      <Button onClick={() => setCreating(false)}>Cancel</Button>
                    )}
                  </Space>
                </div>
              </Form>
              {!creating && selected?.id && (
                <CategoryAssetPanel
                  asset={categoryAsset ?? null}
                  loading={assetLoading}
                  onCreateCandidate={(c) => setCandidateDraft(c)}
                  onViewReportImpact={() => setReportImpactOpen(true)}
                />
              )}
            </div>
          ) : (
            <EmptyState compact title="Select a category" description="Choose a node in the tree or add a new category." />
          )}
        </div>
      </div>

      <CategoryCandidateConfirmModal
        open={Boolean(candidateDraft)}
        candidate={candidateDraft}
        parentName={selected?.name || selected?.code}
        loading={candidateSaving}
        onCancel={() => setCandidateDraft(null)}
        onConfirm={onCreateCandidate}
      />

      <Modal
        open={reportImpactOpen}
        title="Report impact"
        footer={<Button onClick={() => setReportImpactOpen(false)}>Close</Button>}
        onCancel={() => setReportImpactOpen(false)}
        width={640}
        destroyOnClose
      >
        {categoryAsset ? (
          <Space direction="vertical" style={{ width: '100%' }}>
            <Typography.Text type="secondary">
              Transactions tagged with {categoryAsset.categoryCode} roll into these report surfaces.
            </Typography.Text>
            <Space wrap>
              {categoryAsset.affectedReports?.map((r) => <Tag key={r}>{r}</Tag>)}
            </Space>
          </Space>
        ) : null}
      </Modal>

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
