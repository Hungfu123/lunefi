import { useEffect, useState } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { Lightbulb } from '@phosphor-icons/react'
import { lese, type Monatsuebersicht } from './types'
import { euro, monatAlsString, monatLabel } from './format'
import { KategorieIconTile } from './kategorieIcon'
import { KategorieSheet } from './KategorieSheet'
import { useApp } from './AppKontext'

type Sicht = 'meine' | 'zusammen'

interface Sparhinweis {
  kategorie: string
  ist: number
  ueblich: number
  text: string
}

interface Sparpotenzial {
  hinweise: Sparhinweis[]
  basis: 'OK' | 'WENIG_DATEN'
}

interface Segment {
  name: string
  betrag: number
}

const GROESSE = 150
const RADIUS = 61
const BREITE = 16
const UMFANG = 2 * Math.PI * RADIUS
const SPALTEN = 5

const FARBEN = [
  'var(--color-akzent)',
  'color-mix(in srgb, var(--color-akzent) 70%, var(--color-textschwach))',
  'color-mix(in srgb, var(--color-akzent) 50%, var(--color-textschwach))',
  'color-mix(in srgb, var(--color-akzent) 34%, var(--color-textschwach))',
  'color-mix(in srgb, var(--color-akzent) 22%, var(--color-textschwach))',
]

const SONSTIGE_FARBE = 'color-mix(in srgb, var(--color-textschwach) 40%, transparent)'

function sichtLaden(): Sicht {
  return localStorage.getItem('lunefi:sicht') === 'zusammen' ? 'zusammen' : 'meine'
}

function farbe(name: string, rang: number): string {
  if (name === 'Sonstige') return SONSTIGE_FARBE
  return FARBEN[rang] ?? SONSTIGE_FARBE
}

function Donut({
  segmente,
  gesamt,
  gewaehlt,
  onWahl,
}: {
  segmente: Segment[]
  gesamt: number
  gewaehlt: string | null
  onWahl: (segment: Segment) => void
}) {
  const reduziert = useReducedMotion()
  const start = segmente.reduce<number[]>((liste, segment) => {
    liste.push((liste[liste.length - 1] ?? 0) + segment.betrag / Math.max(gesamt, 1))
    return liste
  }, [])
  const mitSpalt = segmente.length > 1
  const aktiv = segmente.find((s) => s.name === gewaehlt) ?? null

  return (
    <div className="relative mx-auto" style={{ width: GROESSE, height: GROESSE }}>
      <svg width={GROESSE} height={GROESSE} viewBox={`0 0 ${GROESSE} ${GROESSE}`} aria-label="Ausgaben je Kategorie">
        {segmente.map((segment, i) => {
          const anteil = segment.betrag / Math.max(gesamt, 1)
          const laenge = anteil * UMFANG
          const sichtbar = mitSpalt ? Math.max(0, laenge - 2.5) : UMFANG
          const versatz = -(start[i - 1] ?? 0) * UMFANG
          const farbwert = farbe(segment.name, i)
          const gemeinsam = { cx: 75, cy: 75, r: RADIUS, fill: 'none' as const, transform: 'rotate(-90 75 75)' }
          return (
            <g key={segment.name}>
              <circle
                {...gemeinsam}
                stroke={farbwert}
                strokeWidth={BREITE}
                strokeDasharray={`${sichtbar} ${UMFANG - sichtbar}`}
                strokeDashoffset={versatz}
              />
              <circle
                {...gemeinsam}
                stroke="black"
                strokeOpacity={0}
                strokeWidth={BREITE + 14}
                strokeDasharray={`${sichtbar} ${UMFANG - sichtbar}`}
                strokeDashoffset={versatz}
                pointerEvents="stroke"
                className="cursor-pointer"
                onClick={() => onWahl(segment)}
              />
            </g>
          )
        })}
      </svg>
      <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
        <AnimatePresence mode="wait" initial={false}>
          <motion.div
            key={aktiv?.name ?? 'gesamt'}
            className="flex w-24 flex-col items-center text-center"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={reduziert ? { duration: 0 } : { duration: 0.15 }}
          >
            {aktiv ? (
              <>
                <p className="w-full truncate text-[12px] font-medium text-textschwach">{aktiv.name}</p>
                <p className="betragszahl text-[15px] font-semibold leading-tight">{euro(aktiv.betrag)}</p>
                <p className="betragszahl text-[11px] text-textschwach">
                  {Math.round((aktiv.betrag / Math.max(gesamt, 1)) * 100)} %
                </p>
              </>
            ) : (
              <>
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">Ausgaben</p>
                <p className="betragszahl text-[17px] font-semibold leading-tight">{euro(gesamt)}</p>
              </>
            )}
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  )
}

export function StatistikSeite() {
  const { aktivePerson } = useApp()
  const monat = monatAlsString(new Date())
  const [sicht, setSicht] = useState<Sicht>(sichtLaden)
  const [daten, setDaten] = useState<Monatsuebersicht | null>(null)
  const [spar, setSpar] = useState<Sparpotenzial | null>(null)
  const [gewaehlt, setGewaehlt] = useState<string | null>(null)
  const [detail, setDetail] = useState<string | null>(null)

  const gefiltert = sicht === 'meine' && aktivePerson !== null
  const zusammen = sicht === 'zusammen'
  const personParam = gefiltert ? `person=${encodeURIComponent(aktivePerson ?? '')}` : ''

  useEffect(() => {
    let aktiv = true
    setGewaehlt(null)
    lese<Monatsuebersicht>(`/api/monatsuebersicht/${monat}${zusammen ? '?gemeinsam=true' : personParam ? `?${personParam}` : ''}`)
      .then((ergebnis) => {
        if (aktiv) setDaten(ergebnis)
      })
      .catch(() => {
        if (aktiv) setDaten(null)
      })
    const sparpfad = `/api/statistik/sparpotenzial?monat=${monat}${
      zusammen ? '&gemeinsam=true' : personParam ? `&${personParam}` : ''
    }`
    lese<Sparpotenzial>(sparpfad)
      .then((ergebnis) => {
        if (aktiv) setSpar(ergebnis)
      })
      .catch(() => {
        if (aktiv) setSpar(null)
      })
    return () => {
      aktiv = false
    }
  }, [monat, zusammen, personParam])

  const ausgaben = Object.entries(daten?.ausgabenJeKategorie ?? {}).sort((a, b) => b[1] - a[1])
  const gesamt = ausgaben.reduce((summe, [, betrag]) => summe + betrag, 0)
  const segmente: Segment[] = ausgaben.slice(0, SPALTEN).map(([name, betrag]) => ({ name, betrag }))
  const rest = ausgaben.slice(SPALTEN)
  if (rest.length > 0) {
    segmente.push({ name: 'Sonstige', betrag: rest.reduce((summe, [, betrag]) => summe + betrag, 0) })
  }

  const wahl = (segment: Segment) => {
    setGewaehlt(segment.name)
    if (segment.name !== 'Sonstige') setDetail(segment.name)
  }

  const zeigeSparpotenzial = spar !== null && (spar.basis === 'WENIG_DATEN' || spar.hinweise.length > 0)

  return (
    <div>
      <div className="mb-1 flex items-center justify-between">
        <h2 className="text-[15px] font-semibold">Statistik</h2>
        <p className="text-xs text-textschwach">{monatLabel(monat)}</p>
      </div>

      <div className="mb-2 flex h-8 w-fit items-center gap-0.5 rounded-full bg-karte2 p-0.5">
        <button
          onClick={() => {
            setSicht('meine')
            localStorage.setItem('lunefi:sicht', 'meine')
          }}
          aria-pressed={gefiltert}
          className={`flex h-7 items-center rounded-full px-3 text-xs font-medium transition-colors ${
            gefiltert ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
          }`}
        >
          {aktivePerson ?? 'Meine'}
        </button>
        <button
          onClick={() => {
            setSicht('zusammen')
            localStorage.setItem('lunefi:sicht', 'zusammen')
          }}
          aria-pressed={zusammen}
          className={`flex h-7 items-center rounded-full px-3 text-xs font-medium transition-colors ${
            zusammen ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
          }`}
        >
          Gemeinsam
        </button>
      </div>

      {daten === null ? (
        <div className="space-y-3">
          <div className="h-36 animate-pulse rounded-2xl bg-karte" />
          <div className="h-16 animate-pulse rounded-2xl bg-karte" />
        </div>
      ) : segmente.length === 0 ? (
        <p className="py-8 text-sm text-textschwach">Keine Ausgaben in diesem Monat.</p>
      ) : (
        <>
          <Donut segmente={segmente} gesamt={gesamt} gewaehlt={gewaehlt} onWahl={wahl} />

          <ul className="mt-2">
            {segmente.map((segment) => (
              <li key={segment.name} className="border-b border-strich last:border-b-0">
                <button
                  type="button"
                  onClick={() => wahl(segment)}
                  aria-label={`${segment.name} anzeigen`}
                  className="flex w-full items-center gap-2.5 py-1.5 text-left"
                >
                  {segment.name === 'Sonstige' ? (
                    <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg">
                      <span
                        className="h-2.5 w-2.5 rounded-full"
                        style={{ background: SONSTIGE_FARBE, border: '1px solid var(--color-textschwach)' }}
                      />
                    </span>
                  ) : (
                    <KategorieIconTile name={segment.name} />
                  )}
                  <span className="min-w-0 flex-1 truncate text-[15px] font-semibold">{segment.name}</span>
                  <span className="betragszahl shrink-0 text-[13px] font-semibold">{euro(segment.betrag)}</span>
                </button>
              </li>
            ))}
          </ul>
        </>
      )}

      {zeigeSparpotenzial && (
        <section className="mt-2">
          <h3 className="flex items-center gap-1.5 text-[13px] font-semibold">
            <Lightbulb size={15} weight="fill" className="text-akzent" aria-hidden="true" />
            Was auffällt
          </h3>
          {spar.basis === 'WENIG_DATEN' ? (
            <p className="mt-1 text-sm text-textschwach">Noch zu wenig Verlauf für Tipps.</p>
          ) : (
            <ul className="mt-1">
              {spar.hinweise.map((hinweis) => (
                <li key={hinweis.kategorie} className="flex items-start gap-2.5 py-1.5">
                  <KategorieIconTile name={hinweis.kategorie} groesse={28} />
                  <p className="pt-1 text-sm leading-snug text-text">{hinweis.text}</p>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <KategorieSheet
        name={detail}
        eintraege={daten?.eintraege.filter((eintrag) => eintrag.kategorie === detail) ?? []}
        budget={detail && daten?.budgets ? (daten.budgets[detail] ?? null) : null}
        empfehlung={null}
        zu={() => setDetail(null)}
      />
    </div>
  )
}
