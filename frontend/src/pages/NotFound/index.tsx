import { Link } from 'react-router-dom'
import { Button, Result } from 'antd'
import { DataPageLayout } from '../../components/DataPageLayout'

export function NotFoundPage() {
  return (
    <DataPageLayout title="Page not found" subtitle="The URL may be outdated or mistyped">
      <Result
        status="404"
        title="404"
        subTitle="We couldn't find this page in FinSight."
        extra={[
          <Link key="dash" to="/dashboard"><Button type="primary">Go to Dashboard</Button></Link>,
          <Link key="reports" to="/reports"><Button>Browse reports</Button></Link>,
          <Link key="tx" to="/transactions"><Button>Transactions</Button></Link>,
        ]}
      />
    </DataPageLayout>
  )
}
