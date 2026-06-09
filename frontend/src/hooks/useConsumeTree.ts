import { useQuery } from '@tanstack/react-query'
import { consumeTree, type TreeNode } from '../api/transaction'

export type TreeSelectNode = { title: string; value: string; children?: TreeSelectNode[] }
export type AntTreeNode = { key: string; title: string; children?: AntTreeNode[] }

function toTreeSelect(nodes: TreeNode[]): TreeSelectNode[] {
  return nodes.map((n) => ({
    title: n.text,
    value: n.id,
    children: n.children?.length ? toTreeSelect(n.children) : undefined,
  }))
}

function toAntTree(nodes: TreeNode[]): AntTreeNode[] {
  return nodes.map((n) => ({
    key: n.id,
    title: n.text,
    children: n.children?.length ? toAntTree(n.children) : undefined,
  }))
}

export function useConsumeTreeSelect(txnType?: string) {
  const { data, isLoading } = useQuery({
    queryKey: ['consume-tree', txnType],
    queryFn: () => consumeTree(txnType),
  })
  return { treeData: toTreeSelect(data || []), isLoading }
}

export function useConsumeAntTree(txnType?: string) {
  const { data, isLoading } = useQuery({
    queryKey: ['consume-tree', txnType],
    queryFn: () => consumeTree(txnType),
  })
  return { treeData: toAntTree(data || []), isLoading }
}
