import { useEffect, useState } from 'react'
import { schreibe, type Haltung } from './types'

export const HALTUNG_LABEL: Record<Haltung, string> = {
  SPAREN: 'Sparen',
  FLEXIBEL: 'Neutral',
  BEWUSST_MEHR: 'Mehr gönnen',
}

const OPTIONEN: Haltung[] = ['SPAREN', 'FLEXIBEL', 'BEWUSST_MEHR']

export function HaltungsSelector({
  kategorie,
  haltung,
  onHaltung,
}: {
  kategorie: string
  haltung: Haltung
  onHaltung?: (wert: Haltung) => void
}) {
  const [gewaehlt, setGewaehlt] = useState(haltung)

  useEffect(() => {
    setGewaehlt(haltung)
  }, [haltung])

  const waehlen = async (wert: Haltung) => {
    const alt = gewaehlt
    if (wert === alt) return
    setGewaehlt(wert)
    const pfad = `/api/kategorien/${encodeURIComponent(kategorie)}/haltung`
    try {
      await schreibe<{ haltung: Haltung }>(pfad, { haltung: wert }, 'PUT')
      onHaltung?.(wert)
    } catch {
      try {
        await schreibe<{ haltung: Haltung }>(pfad, { haltung: wert }, 'PUT')
        onHaltung?.(wert)
      } catch {
        setGewaehlt(alt)
      }
    }
  }

  return (
    <div role="radiogroup" aria-label="Haltung" className="flex h-8 w-full gap-0.5 rounded-lg bg-karte2 p-0.5">
      {OPTIONEN.map((wert) => (
        <button
          key={wert}
          type="button"
          role="radio"
          aria-checked={gewaehlt === wert}
          onClick={() => waehlen(wert)}
          className={`flex h-7 flex-1 items-center justify-center rounded-[6px] text-[12px] font-medium transition-colors ${
            gewaehlt === wert
              ? 'bg-karte text-akzent ring-1 ring-akzent'
              : 'text-textschwach'
          }`}
        >
          {HALTUNG_LABEL[wert]}
        </button>
      ))}
    </div>
  )
}
