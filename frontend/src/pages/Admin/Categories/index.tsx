import { useEffect, useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Button, Form, Input, InputNumber, message, Popconfirm, Select, Space, Tree,
} from 'antd'
import { ClusterOutlined, PlusOutlined } from '@ant-design/icons'
import {
  createCategory, deleteCategory, listCategoriesAdmin, updateCategory, type ConsumeCategoryRow,
} from '../../../api/admin'
import { DataPageLayout } from '../../../components/DataPageLayout'
import { EmptyState } from '../../../components/EmptyState'
import { PageSkeleton } from '../../../components/PageSkeleton'
import { buildCategoryTree, toAntTreeNodes } from '../../../utils/categoryTree'

const EMPTY: ConsumeCategoryRow = { name: '', code: '', parentId: '', sortNo: 1, txnTypes: 'expense' }

export function CategoriesAdminPage() {
  const qc = useQueryClient()
  const [form] = Form.useForm<ConsumeCategoryRow>()
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)

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

  const onSave = async () => {
    const values = await form.validateFields()
    try {
      if (creating) {
        await createCategory(values)
        message.success('Category created')
        setCreating(false)
      } else if (selected?.id) {
        await updateCategory(selected.id, values, true)
        message.success('Category updated')
      }
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Save failed')
    }
  }

  const onDelete = async () => {
    if (!selected?.id) return
    try {
      await deleteCategory(selected.id)
      message.success('Category deleted')
      setSelectedId(null)
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Delete failed')
    }
  }

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
              <Space>
                <Button type="primary" onClick={onSave}>{creating ? 'Create' : 'Save'}</Button>
                {!creating && selected?.id && (
                  <Popconfirm title="Soft-delete this category?" onConfirm={onDelete}>
                    <Button danger>Delete</Button>
                  </Popconfirm>
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
    </DataPageLayout>
  )
}
