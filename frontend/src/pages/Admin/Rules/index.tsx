import { useEffect, useMemo, useRef, useState, type MouseEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button, Form, Input, InputNumber, message, Modal, Popconfirm, Select, Space, Switch, Table, Tag, Tooltip, Tree, TreeSelect, Typography,
} from 'antd'
import type { DataNode } from 'antd/es/tree'
import {
  BulbOutlined, DeleteOutlined, EditOutlined, PlusOutlined, ThunderboltOutlined,
} from '@ant-design/icons'
import type { ConsumeCategoryRow } from '../../../api/admin'
import {
  createRule, deleteRule, fetchUnclassifiedRuleKeywords, listCategoriesAdmin, listRules, updateRule,
  type ConsumeRuleRow,
} from '../../../api/admin'
import { DataPageLayout } from '../../../components/DataPageLayout'
import { EmptyState } from '../../../components/EmptyState'
import { PageSkeleton } from '../../../components/PageSkeleton'
import { useConsumeTreeSelect } from '../../../hooks/useConsumeTree'
import { useFillTableHeight } from '../../../hooks/useFillTableHeight'
import { buildCategoryTree, ruleMatchesCategory, type CategoryTreeNode } from '../../../utils/categoryTree'
import { categoryTitleMap, resolveCategoryTitleExtended } from '../../../utils/categoryLookup'
import {
  classifyRule, filterByTreeKey, INVALID_KEY, LEGACY_KEY, NO_CAT_KEY, ORPHAN_KEY, type RuleIssue,
} from '../../../utils/ruleHealth'
import { cellText } from '../../../utils/cell'

const ALL_KEY = '__all__'

const HIDDEN_TREE_KEYS = new Set([LEGACY_KEY, INVALID_KEY])

function isMainListRule(
  rule: ConsumeRuleRow,
  activeCategories: ConsumeCategoryRow[],
  allCategories: ConsumeCategoryRow[],
): boolean {
  const issue = classifyRule(rule, activeCategories, allCategories)
  return issue !== 'invalid_pattern' && issue !== 'legacy_archived'
}

const PATTERN_TYPES = [
  { value: 'contains', label: 'Contains' },
  { value: 'regex', label: 'Regex' },
  { value: 'equals', label: 'Equals' },
  { value: 'startswith', label: 'Starts with' },
]

const MATCH_COLORS: Record<string, string> = {
  contains: 'blue',
  regex: 'purple',
  equals: 'cyan',
  startswith: 'geekblue',
}

const BANK_OPTIONS = [
  { value: '', label: 'Any bank' },
  { value: 'CMB', label: 'CMB' },
  { value: 'CCB', label: 'CCB' },
  { value: 'CGB', label: 'CGB' },
  { value: 'CRBANK', label: 'CRBANK' },
]

const CARD_OPTIONS = [
  { value: '', label: 'Any card' },
  { value: 'debit', label: 'Debit' },
  { value: 'credit', label: 'Credit' },
  { value: 'ewallet', label: 'E-wallet' },
]

function tagsToString(tags?: string[]) {
  return (tags || []).join(', ')
}

function tagsFromString(raw?: string) {
  if (!raw) return []
  return raw.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
}

function countRulesForCategory(rules: ConsumeRuleRow[], cat: ConsumeCategoryRow) {
  return rules.filter((r) => ruleMatchesCategory(r.categoryId, cat)).length
}

function countByIssue(
  rules: ConsumeRuleRow[],
  issue: RuleIssue,
  activeCategories: ConsumeCategoryRow[],
  allCategories: ConsumeCategoryRow[],
) {
  return rules.filter((r) => classifyRule(r, activeCategories, allCategories) === issue).length
}

function labelWithCount(label: string, count: number, always = false): string {
  if (always || count > 0) return `${label} (${count})`
  return label
}

function buildTreeNodes(
  categories: CategoryTreeNode[],
  flatActive: ConsumeCategoryRow[],
  rules: ConsumeRuleRow[],
  activeCategories: ConsumeCategoryRow[],
  allCategories: ConsumeCategoryRow[],
): DataNode[] {
  const orphanCount = countByIssue(rules, 'orphaned', activeCategories, allCategories)
  const noCatCount = countByIssue(rules, 'no_category', activeCategories, allCategories)
  const mainListCount = rules.filter((r) => {
    const issue = classifyRule(r, activeCategories, allCategories)
    return issue !== 'invalid_pattern' && issue !== 'legacy_archived'
  }).length

  const mapNode = (node: CategoryTreeNode): { node: DataNode; total: number } => {
    const cat = flatActive.find((c) => (c.id || c.code) === node.key)
    const direct = cat ? countRulesForCategory(rules, cat) : 0
    const mappedChildren = (node.children || []).map(mapNode)
    const childTotal = mappedChildren.reduce((sum, ch) => sum + ch.total, 0)
    const total = direct + childTotal
    const childNodes = mappedChildren.map((ch) => ch.node)
    return {
      total,
      node: {
        key: node.key,
        title: labelWithCount(node.title, total),
        children: childNodes.length ? childNodes : undefined,
      },
    }
  }

  const attention: DataNode[] = []
  if (orphanCount > 0) {
    attention.push({ key: ORPHAN_KEY, title: labelWithCount('Orphaned', orphanCount, true) })
  }
  if (noCatCount > 0) {
    attention.push({ key: NO_CAT_KEY, title: labelWithCount('No category', noCatCount, true) })
  }

  const categoryNodes = categories.map((c) => mapNode(c).node)
  if (!attention.length) {
    return [
      { key: ALL_KEY, title: labelWithCount('All rules', mainListCount, true) },
      ...categoryNodes,
    ]
  }

  return [
    { key: ALL_KEY, title: labelWithCount('All rules', mainListCount, true) },
    ...categoryNodes,
    { key: '__attention_divider__', title: 'Needs attention', selectable: false, disabled: true },
    ...attention,
  ]
}

const TREE_LABELS: Record<string, string> = {
  [ALL_KEY]: 'all rules',
  [ORPHAN_KEY]: 'orphaned',
  [LEGACY_KEY]: 'inactive legacy',
  [NO_CAT_KEY]: 'no category',
  [INVALID_KEY]: 'invalid legacy',
}

function filterTreeBySearch(nodes: DataNode[], query: string, nameByKey: Map<string, string>): DataNode[] {
  const q = query.trim().toLowerCase()
  if (!q) return nodes
  const walk = (node: DataNode): DataNode | null => {
    if (node.disabled) return null
    const key = String(node.key)
    const name = (nameByKey.get(key) || TREE_LABELS[key] || key).toLowerCase()
    const selfMatch = name.includes(q)
    const kids = (node.children || []).map(walk).filter(Boolean) as DataNode[]
    if (selfMatch || kids.length) return { ...node, children: kids.length ? kids : undefined }
    return null
  }
  return nodes.map(walk).filter(Boolean) as DataNode[]
}

function panelTitleForKey(key: string, activeMap: Map<string, string>): string {
  if (key === ALL_KEY) return 'All rules'
  if (key === ORPHAN_KEY) return 'Orphaned rules'
  if (key === LEGACY_KEY) return 'Inactive legacy orphan rules'
  if (key === NO_CAT_KEY) return 'Rules without category'
  if (key === INVALID_KEY) return 'Invalid / legacy rules'
  return activeMap.get(key) || key
}

function panelHintForKey(key: string): string | undefined {
  if (key === ORPHAN_KEY) return 'These rules reference categories that were removed (soft-deleted), not deleted rules themselves.'
  if (key === LEGACY_KEY) return 'Archived orphan rules are inactive and kept for audit. Remap or leave disabled.'
  if (key === NO_CAT_KEY) return 'Assign a category so imports can classify matching transactions.'
  if (key === INVALID_KEY) return 'Rows with no keyword cannot match anything — usually safe to delete.'
  return undefined
}

export function RulesAdminPage() {
  const qc = useQueryClient()
  const [searchParams] = useSearchParams()
  const listPanelRef = useRef<HTMLDivElement>(null)
  const tableHeight = useFillTableHeight(listPanelRef)
  const [form] = Form.useForm<ConsumeRuleRow>()
  const { treeData: categorySelectTree } = useConsumeTreeSelect()

  const [selectedKey, setSelectedKey] = useState(ALL_KEY)
  const [keyword, setKeyword] = useState('')
  const [treeSearch, setTreeSearch] = useState('')
  const [editorOpen, setEditorOpen] = useState(false)
  const [editing, setEditing] = useState<ConsumeRuleRow | null>(null)
  const [suggestOpen, setSuggestOpen] = useState(false)
  const [suggestLoading, setSuggestLoading] = useState(false)
  const [suggestedKeywords, setSuggestedKeywords] = useState<string[]>([])

  useEffect(() => {
    if (HIDDEN_TREE_KEYS.has(selectedKey)) {
      setSelectedKey(ALL_KEY)
    }
  }, [selectedKey])

  const categoryFromUrl = searchParams.get('category') || ''

  const { data: allCategories = [], isLoading: catsLoading } = useQuery({
    queryKey: ['admin-categories', 'withDeleted'],
    queryFn: () => listCategoriesAdmin(true),
    staleTime: 60_000,
  })

  const activeCategories = useMemo(
    () => allCategories.filter((c) => c.deleted !== 1),
    [allCategories],
  )

  useEffect(() => {
    if (!categoryFromUrl || !activeCategories.length) return
    const match = activeCategories.find(
      (c) => c.code === categoryFromUrl || c.id === categoryFromUrl,
    )
    if (match) {
      setSelectedKey(match.id || match.code || ALL_KEY)
    }
  }, [categoryFromUrl, activeCategories])

  const { data: rules = [], isLoading: rulesLoading, refetch } = useQuery({
    queryKey: ['admin-rules'],
    queryFn: async () => {
      const valid = (await listRules(true, false)) as ConsumeRuleRow[]
      const all = (await listRules(true, true)) as ConsumeRuleRow[]
      const validIds = new Set(valid.map((r) => r.id))
      const invalidOnly = all.filter((r) => !validIds.has(r.id))
      return [...valid, ...invalidOnly]
    },
    staleTime: 30_000,
  })

  const activeMap = useMemo(() => categoryTitleMap(activeCategories), [activeCategories])
  const categoryTree = useMemo(() => buildCategoryTree(allCategories), [allCategories])
  const treeNodes = useMemo(
    () => buildTreeNodes(categoryTree, activeCategories, rules, activeCategories, allCategories),
    [categoryTree, activeCategories, rules, allCategories],
  )
  const nameByKey = useMemo(() => {
    const m = new Map<string, string>()
    for (const c of activeCategories) {
      const name = c.name || c.code || c.id || ''
      if (c.id) m.set(c.id, name)
      if (c.code) m.set(c.code, name)
    }
    return m
  }, [activeCategories])
  const visibleTree = useMemo(
    () => filterTreeBySearch(treeNodes, treeSearch, nameByKey),
    [treeNodes, treeSearch, nameByKey],
  )

  const filtered = useMemo(() => {
    let list = filterByTreeKey(
      rules,
      selectedKey,
      activeCategories,
      allCategories,
      (r, cat) => ruleMatchesCategory(r.categoryId, cat),
    )
    if (selectedKey === ALL_KEY) {
      list = list.filter((r) => isMainListRule(r, activeCategories, allCategories))
    }
    const q = keyword.trim().toLowerCase()
    if (q) {
      list = list.filter(
        (r) => (r.pattern || '').toLowerCase().includes(q) || tagsToString(r.tags).toLowerCase().includes(q),
      )
    }
    return list.sort((a, b) => (a.priority ?? 0) - (b.priority ?? 0))
  }, [rules, selectedKey, activeCategories, allCategories, keyword])

  const stats = useMemo(() => {
    const visible = rules.filter((r) => isMainListRule(r, activeCategories, allCategories))
    const active = visible.filter((r) => r.active === 1 && r.pattern?.trim()).length
    const disabled = visible.filter((r) => r.active !== 1 && r.pattern?.trim()).length
    const orphaned = countByIssue(rules, 'orphaned', activeCategories, allCategories)
    return { active, disabled, orphaned }
  }, [rules, activeCategories, allCategories])

  const panelTitle = panelTitleForKey(selectedKey, activeMap)
  const panelHint = panelHintForKey(selectedKey)
  const showCategoryCol = selectedKey === ALL_KEY || selectedKey === ORPHAN_KEY || selectedKey === NO_CAT_KEY

  const openCreate = (presetPattern = '') => {
    const presetCategory = ![ALL_KEY, ORPHAN_KEY, LEGACY_KEY, NO_CAT_KEY, INVALID_KEY].includes(selectedKey) ? selectedKey : ''
    setEditing(null)
    form.setFieldsValue({
      pattern: presetPattern,
      patternType: 'contains',
      categoryId: presetCategory,
      priority: 50,
      active: 1,
      bankCode: '',
      cardTypeCode: '',
      tags: [],
    })
    setEditorOpen(true)
  }

  const openSuggestKeywords = async () => {
    setSuggestOpen(true)
    setSuggestLoading(true)
    try {
      const keywords = await fetchUnclassifiedRuleKeywords(25)
      setSuggestedKeywords(keywords)
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Failed to load suggestions')
      setSuggestedKeywords([])
    } finally {
      setSuggestLoading(false)
    }
  }

  const applySuggestedKeyword = (keyword: string) => {
    setSuggestOpen(false)
    openCreate(keyword)
  }

  const openEdit = (rule: ConsumeRuleRow) => {
    setEditing(rule)
    form.setFieldsValue({ ...rule, tags: rule.tags || [] })
    setEditorOpen(true)
  }

  const isInteractiveRowClick = (target: EventTarget | null) => {
    if (!(target instanceof HTMLElement)) return false
    return Boolean(target.closest('.fs-rule-row-actions, .ant-switch, .ant-popover, button, a, input, textarea, .ant-select'))
  }

  const onRowClick = (rule: ConsumeRuleRow, e: MouseEvent) => {
    if (isInteractiveRowClick(e.target)) return
    openEdit(rule)
  }

  const renderRuleActions = (rule: ConsumeRuleRow) => (
    <div
      className="fs-rule-row-actions"
      onClick={(e) => e.stopPropagation()}
      onKeyDown={(e) => e.stopPropagation()}
    >
      <Tooltip title="Edit rule" mouseEnterDelay={0.4}>
        <Button
          type="text"
          size="small"
          icon={<EditOutlined />}
          aria-label="Edit rule"
          onClick={() => openEdit(rule)}
        />
      </Tooltip>
      <Popconfirm title="Delete this rule?" okText="Delete" okButtonProps={{ danger: true }} onConfirm={() => onDelete(String(rule.id))}>
        <Tooltip title="Delete rule" mouseEnterDelay={0.4}>
          <Button
            type="text"
            size="small"
            danger
            icon={<DeleteOutlined />}
            aria-label="Delete rule"
          />
        </Tooltip>
      </Popconfirm>
    </div>
  )

  const onSave = async () => {
    const values = await form.validateFields()
    const payload: ConsumeRuleRow = {
      ...values,
      tags: Array.isArray(values.tags) ? values.tags : tagsFromString(values.tags as unknown as string),
      bankCode: values.bankCode || undefined,
      cardTypeCode: values.cardTypeCode || undefined,
    }
    try {
      if (editing?.id) await updateRule(String(editing.id), payload)
      else await createRule(payload)
      message.success(editing ? 'Rule updated' : 'Rule created')
      setEditorOpen(false)
      refetch()
      qc.invalidateQueries({ queryKey: ['admin-rules'] })
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Save failed')
    }
  }

  const onDelete = async (id: string) => {
    await deleteRule(id)
    message.success('Deleted')
    refetch()
  }

  const onToggleActive = async (rule: ConsumeRuleRow, active: boolean) => {
    if (!rule.id) return
    await updateRule(String(rule.id), { ...rule, active: active ? 1 : 0 })
    refetch()
  }

  const loading = catsLoading || rulesLoading

  return (
    <DataPageLayout
      title="Rule Engine"
      subtitle="Keyword rules that auto-classify transactions on import"
      icon={<ThunderboltOutlined />}
      className="fs-data-page--dense fs-data-page--fill"
    >
      <div className="fs-admin-split">
        <div className="fs-admin-split-tree fs-table-panel fs-rule-engine-tree-panel">
          <Input
            allowClear
            size="small"
            placeholder="Filter categories…"
            value={treeSearch}
            onChange={(e) => setTreeSearch(e.target.value)}
          />
          {loading ? (
            <PageSkeleton variant="table" />
          ) : (
            <Tree
              showLine
              selectedKeys={[selectedKey]}
              treeData={visibleTree}
              onSelect={(keys) => {
                const k = keys[0]
                if (k && !String(k).startsWith('__')) setSelectedKey(String(k))
              }}
            />
          )}
        </div>

        <section ref={listPanelRef} className="fs-admin-split-form fs-table-panel fs-rule-engine-list-panel">
          <header className="fs-rule-engine-list-head">
            <div className="fs-rule-main-title-block">
              <Typography.Title level={5} className="fs-rule-main-title">{panelTitle}</Typography.Title>
              {panelHint && (
                <Typography.Text type="secondary" className="fs-rule-main-hint">{panelHint}</Typography.Text>
              )}
              <div className="fs-rule-stats">
                <span className="fs-rule-stat fs-rule-stat--on">{stats.active} active</span>
                <span className="fs-rule-stat">{stats.disabled} off</span>
                {stats.orphaned > 0 && (
                  <span className="fs-rule-stat fs-rule-stat--warn">{stats.orphaned} orphaned</span>
                )}
              </div>
            </div>
            <Space size="small" wrap className="fs-rule-main-actions">
              <Input.Search
                allowClear
                placeholder="Search keyword or tag…"
                className="fs-rule-keyword-search"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
              <Button icon={<BulbOutlined />} onClick={openSuggestKeywords}>
                Suggest keywords
              </Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate()}>
                Add rule
              </Button>
            </Space>
          </header>

          <Table<ConsumeRuleRow>
            className="fs-rule-table"
            rowKey="id"
            size="small"
            loading={loading}
            dataSource={filtered}
            onRow={(record) => ({
              onClick: (e) => onRowClick(record, e),
            })}
            rowClassName={(r) => {
              const issue = classifyRule(r, activeCategories, allCategories)
              const classes = ['fs-rule-row--clickable']
              if (issue !== 'ok') classes.push('fs-rule-row--attention')
              if (r.active !== 1) classes.push('fs-rule-row--off')
              return classes.join(' ')
            }}
            pagination={{ pageSize: 25, size: 'small', showTotal: (t) => `${t} rules`, showSizeChanger: false }}
            scroll={{ x: 960, y: tableHeight }}
            tableLayout="fixed"
            locale={{
              emptyText: (
                <EmptyState
                  compact
                  title="No rules here"
                  description="Add a keyword rule or pick another category."
                />
              ),
            }}
            columns={[
              {
                title: 'Keyword',
                dataIndex: 'pattern',
                ellipsis: true,
                render: (v) => {
                  const text = cellText(v)
                  if (!text) {
                    return <span className="fs-rule-keyword fs-rule-keyword--empty">No keyword</span>
                  }
                  return (
                    <span className="fs-rule-keyword" title={text}>
                      {text}
                    </span>
                  )
                },
              },
              ...(showCategoryCol ? [{
                title: 'Category',
                dataIndex: 'categoryId',
                width: 140,
                ellipsis: true,
                render: (_: unknown, r: ConsumeRuleRow) => {
                  const issue = classifyRule(r, activeCategories, allCategories)
                  const title = resolveCategoryTitleExtended(activeMap, allCategories, r.categoryId)
                  if (issue === 'orphaned') {
                    return <span className="fs-rule-cat fs-rule-cat--orphan" title={title}>{title}</span>
                  }
                  if (issue === 'legacy_archived') {
                    return <span className="fs-rule-cat fs-rule-cat--legacy" title={title}>{title}</span>
                  }
                  if (issue === 'no_category') {
                    return <span className="fs-rule-cat fs-rule-cat--none">—</span>
                  }
                  return <span className="fs-rule-cat" title={title}>{title}</span>
                },
              }] : []),
              {
                title: 'Match',
                dataIndex: 'patternType',
                width: 96,
                render: (v) => {
                  const t = String(v || 'contains')
                  return <Tag bordered={false} color={MATCH_COLORS[t] || 'default'} className="fs-rule-match-tag">{t}</Tag>
                },
              },
              {
                title: 'Pri',
                dataIndex: 'priority',
                width: 56,
                align: 'right' as const,
                render: (v) => <span className="fs-rule-priority">{v ?? 0}</span>,
              },
              {
                title: 'Scope',
                width: 100,
                render: (_, r) => {
                  const bank = r.bankCode?.trim()
                  const card = r.cardTypeCode?.trim()
                  if (!bank && !card) return <span className="fs-rule-scope-any">Any</span>
                  return <span className="fs-rule-scope">{[bank, card].filter(Boolean).join(' · ')}</span>
                },
              },
              {
                title: 'Tags',
                dataIndex: 'tags',
                width: 100,
                ellipsis: true,
                render: (_, r) => {
                  const tags = r.tags || []
                  if (!tags.length) return <span className="fs-cell-muted">—</span>
                  return (
                    <span className="fs-rule-tags">
                      {tags.slice(0, 2).map((t) => <Tag key={t} bordered={false} className="fs-rule-tag">{t}</Tag>)}
                      {tags.length > 2 && <span className="fs-rule-tag-more">+{tags.length - 2}</span>}
                    </span>
                  )
                },
              },
              {
                title: 'On',
                dataIndex: 'active',
                width: 56,
                align: 'center' as const,
                render: (_, r) => (
                  <span
                    className="fs-rule-switch-wrap"
                    onClick={(e) => e.stopPropagation()}
                    onKeyDown={(e) => e.stopPropagation()}
                  >
                    <Switch size="small" checked={r.active === 1} onChange={(c) => onToggleActive(r, c)} />
                  </span>
                ),
              },
              {
                title: '',
                key: 'actions',
                width: 88,
                fixed: 'right' as const,
                className: 'fs-rule-actions-col',
                render: (_, r) => renderRuleActions(r),
              },
            ]}
          />
        </section>
      </div>

      <Modal
        title="Keywords from unclassified transactions"
        open={suggestOpen}
        onCancel={() => setSuggestOpen(false)}
        footer={null}
        width={480}
      >
        <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
          Frequent tokens from unclassified ledger rows. Pick one to start a new rule — review category before saving.
        </Typography.Paragraph>
        {suggestLoading ? (
          <PageSkeleton variant="table" />
        ) : suggestedKeywords.length ? (
          <Space size={[8, 8]} wrap>
            {suggestedKeywords.map((kw) => (
              <Button key={kw} size="small" onClick={() => applySuggestedKeyword(kw)}>{kw}</Button>
            ))}
          </Space>
        ) : (
          <EmptyState compact title="No suggestions" description="Unclassified transactions may not have useful tokens yet." />
        )}
      </Modal>

      <Modal
        title={editing ? 'Edit rule' : 'New rule'}
        open={editorOpen}
        onCancel={() => setEditorOpen(false)}
        onOk={onSave}
        okText="Save"
        destroyOnClose
        width={520}
        className="fs-rule-editor-modal"
        footer={(_, { OkBtn, CancelBtn }) => (
          <div className="fs-rule-editor-footer">
            {editing?.id ? (
              <Popconfirm
                title="Delete this rule?"
                okText="Delete"
                okButtonProps={{ danger: true }}
                onConfirm={() => {
                  void onDelete(String(editing.id)).then(() => setEditorOpen(false))
                }}
              >
                <Button danger icon={<DeleteOutlined />}>Delete</Button>
              </Popconfirm>
            ) : (
              <span />
            )}
            <Space>
              <CancelBtn />
              <OkBtn />
            </Space>
          </div>
        )}
      >
        <Form form={form} layout="vertical" size="middle" className="fs-rule-editor-form">
          <Form.Item name="pattern" label="Keyword" rules={[{ required: true, message: 'Keyword is required' }]}>
            <Input placeholder="e.g. 美团, 地铁, salary" />
          </Form.Item>
          <Space size="middle" style={{ display: 'flex' }} align="start">
            <Form.Item name="patternType" label="Match type" style={{ flex: 1 }}>
              <Select options={PATTERN_TYPES} />
            </Form.Item>
            <Form.Item name="priority" label="Priority" style={{ width: 120 }}>
              <InputNumber min={0} max={999} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item name="categoryId" label="Category" rules={[{ required: true, message: 'Category is required' }]}>
            <TreeSelect
              allowClear
              treeData={categorySelectTree}
              treeDefaultExpandAll={false}
              placeholder="Target category"
              showSearch
              treeNodeFilterProp="title"
            />
          </Form.Item>
          <Space size="middle" style={{ display: 'flex' }} align="start">
            <Form.Item name="bankCode" label="Bank (optional)" style={{ flex: 1 }}>
              <Select options={BANK_OPTIONS} allowClear />
            </Form.Item>
            <Form.Item name="cardTypeCode" label="Card type (optional)" style={{ flex: 1 }}>
              <Select options={CARD_OPTIONS} allowClear />
            </Form.Item>
          </Space>
          <Form.Item name="tags" label="Tags (optional)">
            <Select mode="tags" tokenSeparators={[',']} placeholder="Comma-separated tags" />
          </Form.Item>
          <Form.Item
            name="active"
            label="Enabled"
            valuePropName="checked"
            getValueFromEvent={(c) => (c ? 1 : 0)}
            getValueProps={(v) => ({ checked: v === 1 })}
          >
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </DataPageLayout>
  )
}
