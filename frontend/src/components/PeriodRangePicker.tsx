import { useMemo, useRef, useState } from 'react'
import { Button, DatePicker, Popover, Segmented, Space } from 'antd'
import { CalendarOutlined, DownOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import {
  PERIOD_SECTION_LABELS,
  detectPresetId,
  formatPeriodPreview,
  isAllTimePeriod,
  periodTriggerLabel,
  presetRange,
  presetsForSection,
  shiftPeriod,
  type PeriodPresetId,
  type PeriodRange,
  type PeriodSection,
} from '../utils/periodPresets'

type Props = {
  value: PeriodRange
  onChange: (range: PeriodRange, presetId: PeriodPresetId) => void
  disabled?: boolean
  size?: 'small' | 'middle'
  placeholder?: string
}

type PanelMode = 'presets' | 'custom'

const SECTIONS: PeriodSection[] = ['recommended', 'relative', 'calendar']

export function PeriodRangePicker({
  value,
  onChange,
  disabled,
  size = 'small',
  placeholder = 'Select period',
}: Props) {
  const panelRef = useRef<HTMLDivElement>(null)
  const [open, setOpen] = useState(false)
  const presetId = useMemo(() => detectPresetId(value), [value])
  const [mode, setMode] = useState<PanelMode>(() => (presetId === 'custom' ? 'custom' : 'presets'))
  const [draftStart, setDraftStart] = useState<Dayjs | null>(() => value[0])
  const [draftEnd, setDraftEnd] = useState<Dayjs | null>(() => value[1])
  const [startPickerOpen, setStartPickerOpen] = useState(false)
  const [endPickerOpen, setEndPickerOpen] = useState(false)
  const childPickerOpen = startPickerOpen || endPickerOpen

  const triggerLabel = useMemo(
    () => periodTriggerLabel(value, presetId) || placeholder,
    [value, presetId, placeholder],
  )
  const preview = useMemo(
    () => (isAllTimePeriod(value) ? 'No date filter' : formatPeriodPreview(value[0], value[1])),
    [value],
  )
  const draftPreview = useMemo(() => {
    if (!draftStart || !draftEnd) return 'Select start and end dates'
    return formatPeriodPreview(draftStart, draftEnd)
  }, [draftStart, draftEnd])

  const applyPreset = (id: PeriodPresetId, range: PeriodRange) => {
    onChange(range, id)
    setOpen(false)
  }

  const applyCustom = () => {
    if (!draftStart || !draftEnd || draftEnd.isBefore(draftStart, 'day')) return
    const range: PeriodRange = [draftStart.startOf('day'), draftEnd.endOf('day')]
    onChange(range, 'custom')
    setOpen(false)
  }

  const navigate = (dir: -1 | 1) => {
    const next = shiftPeriod(value, presetId, dir)
    const nextId = detectPresetId(next)
    onChange(next, nextId)
  }

  const onStartChange = (d: Dayjs | null) => {
    setDraftStart(d)
    if (d && draftEnd && draftEnd.isBefore(d, 'day')) {
      setDraftEnd(d.endOf('day'))
    }
  }

  const presetPanel = (
    <>
      <div className="fs-period-nav">
        <Button type="text" size="small" className="fs-period-nav-btn" onClick={() => navigate(-1)}>
          <LeftOutlined /> Previous
        </Button>
        <Button type="text" size="small" className="fs-period-nav-btn" onClick={() => navigate(1)}>
          Next <RightOutlined />
        </Button>
      </div>
      {SECTIONS.map((section) => {
        const items = presetsForSection(section)
        if (!items.length) return null
        return (
          <div key={section} className="fs-period-section">
            <div className="fs-period-section-title">{PERIOD_SECTION_LABELS[section]}</div>
            {items.map((preset) => {
              const [start, end] = presetRange(preset.id)
              const active = presetId === preset.id
                && start.isSame(value[0], 'day')
                && end.isSame(value[1], 'day')
              return (
                <button
                  key={`${section}-${preset.id}`}
                  type="button"
                  className={`fs-period-option${active ? ' active' : ''}`}
                  onClick={() => applyPreset(preset.id, presetRange(preset.id))}
                >
                  <span className="fs-period-option-label">{preset.label}</span>
                  <span className="fs-period-option-preview">{formatPeriodPreview(start, end)}</span>
                </button>
              )
            })}
          </div>
        )
      })}
    </>
  )

  const customPanel = (
    <div className="fs-period-custom-panel">
      <div className="fs-period-custom-summary">{draftPreview}</div>
      <div className="fs-period-custom-fields">
        <label className="fs-period-field">
          <span className="fs-period-field-label">Start date</span>
          <DatePicker
            size="small"
            value={draftStart}
            onChange={onStartChange}
            onOpenChange={setStartPickerOpen}
            format="MM/DD/YYYY"
            allowClear={false}
            getPopupContainer={() => document.body}
            popupClassName="fs-period-date-popup"
            className="fs-period-date-input"
            placeholder="From"
          />
        </label>
        <label className="fs-period-field">
          <span className="fs-period-field-label">End date</span>
          <DatePicker
            size="small"
            value={draftEnd}
            onChange={(d) => setDraftEnd(d)}
            onOpenChange={setEndPickerOpen}
            format="MM/DD/YYYY"
            allowClear={false}
            getPopupContainer={() => document.body}
            popupClassName="fs-period-date-popup"
            className="fs-period-date-input"
            placeholder="To"
            disabledDate={(d) => draftStart != null && d.isBefore(draftStart, 'day')}
          />
        </label>
      </div>
      <Space className="fs-period-custom-actions" size={8}>
        <Button size="small" onClick={() => setMode('presets')}>Back</Button>
        <Button
          type="primary"
          size="small"
          disabled={!draftStart || !draftEnd || draftEnd.isBefore(draftStart, 'day')}
          onClick={applyCustom}
        >
          Apply range
        </Button>
      </Space>
    </div>
  )

  const panel = (
    <div ref={panelRef} className="fs-period-panel" onMouseDown={(e) => e.stopPropagation()}>
      <div className="fs-period-header">
        <Segmented
          size="small"
          block
          value={mode}
          onChange={(v) => setMode(v as PanelMode)}
          options={[
            { label: 'Presets', value: 'presets' },
            { label: 'Custom', value: 'custom' },
          ]}
        />
      </div>
      {mode === 'presets' ? presetPanel : customPanel}
    </div>
  )

  return (
    <Popover
      open={open}
      onOpenChange={(next) => {
        if (!next && childPickerOpen) return
        if (next) {
          const detected = detectPresetId(value)
          setDraftStart(value[0])
          setDraftEnd(value[1])
          setMode(detected === 'custom' ? 'custom' : 'presets')
        }
        setOpen(next)
        if (!next) {
          setStartPickerOpen(false)
          setEndPickerOpen(false)
        }
      }}
      trigger="click"
      placement="bottomLeft"
      content={panel}
      overlayClassName="fs-period-popover"
      destroyTooltipOnHide
      arrow={false}
    >
      <Button size={size} disabled={disabled} className="fs-period-trigger">
        <span className="fs-period-trigger-icon"><CalendarOutlined /></span>
        <span className="fs-period-trigger-text">
          <span className="fs-period-trigger-label">{triggerLabel}</span>
          <span className="fs-period-trigger-preview">{preview}</span>
        </span>
        <DownOutlined className="fs-period-trigger-caret" />
      </Button>
    </Popover>
  )
}
