import { useEffect, useState } from 'react'
import { write, type Attitude } from './types'

export const ATTITUDE_LABEL: Record<Attitude, string> = {
  SPAREN: 'Sparen',
  FLEXIBEL: 'Neutral',
  BEWUSST_MEHR: 'Mehr gönnen',
}

const OPTIONS: Attitude[] = ['SPAREN', 'FLEXIBEL', 'BEWUSST_MEHR']

export function AttitudeSelector({
  kategorie,
  haltung,
  onAttitude,
}: {
  kategorie: string
  haltung: Attitude
  onAttitude?: (value: Attitude) => void
}) {
  const [selected, setSelected] = useState(haltung)

  useEffect(() => {
    setSelected(haltung)
  }, [haltung])

  const choose = async (value: Attitude) => {
    const prev = selected
    if (value === prev) return
    setSelected(value)
    const path = `/api/kategorien/${encodeURIComponent(kategorie)}/haltung`
    try {
      await write<{ haltung: Attitude }>(path, { haltung: value }, 'PUT')
      onAttitude?.(value)
    } catch {
      try {
        await write<{ haltung: Attitude }>(path, { haltung: value }, 'PUT')
        onAttitude?.(value)
      } catch {
        setSelected(prev)
      }
    }
  }

  return (
    <div role="radiogroup" aria-label="Haltung" className="flex h-8 w-full gap-0.5 rounded-lg bg-karte2 p-0.5">
      {OPTIONS.map((value) => (
        <button
          key={value}
          type="button"
          role="radio"
          aria-checked={selected === value}
          onClick={() => choose(value)}
          className={`flex h-7 flex-1 items-center justify-center rounded-[6px] text-[12px] font-medium transition-colors ${
            selected === value
              ? 'bg-karte text-akzent ring-1 ring-akzent'
              : 'text-textschwach'
          }`}
        >
          {ATTITUDE_LABEL[value]}
        </button>
      ))}
    </div>
  )
}
