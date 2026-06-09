export function TableHeader({ name, unit }: { name: string; unit?: string }) {
  return (
    <div className="fs-col-header">
      <div className="fs-col-header-name">{name}</div>
      {unit && <div className="fs-col-header-unit">{unit}</div>}
    </div>
  )
}
