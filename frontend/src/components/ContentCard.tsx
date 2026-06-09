import type { ReactNode } from 'react'
import { Card, type CardProps } from 'antd'

type Props = CardProps & { children: ReactNode }

export function ContentCard({ className, children, ...rest }: Props) {
  return (
    <Card className={`fs-content-card ${className ?? ''}`} bordered={false} {...rest}>
      {children}
    </Card>
  )
}
