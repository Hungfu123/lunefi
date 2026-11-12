import { useEffect, useState } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { createPortal } from 'react-dom'
import { read, type BudgetInfo, type CoachingRecommendation, type Entry, type Attitude, type CategoryInfo } from './types'
import { shortDate, euro, isThisWeek, shortMonth, monthAsString } from './format'
import { CategoryIconTile } from './categoryIcon'
import { ATTITUDE_LABEL, AttitudeSelector } from './AttitudeSelector'
import { useApp } from './AppContext'

interface TrendMonth {
  monat: string
  betrag: number
}

interface CategoryStatistics {
  kategorie: string
  monate: TrendMonth[]
  median: number
  durchschnitt: number
}

function TrendRow({ statistics }: { statistics: CategoryStatistics }) {
  const [activeBar, setActiveBar] = useState<number | null>(null)
  const max = statistics.monate.reduce((highest, m) => Math.max(highest, m.betrag), 0)
  const currentMonth = monthAsString(new Date())
  return (
    <div className="mt-3">
      <div className="flex items-baseline justify-between gap-2">
        <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">Verlauf</p>
        <p className="betragszahl text-[13px] font-medium text-textschwach">Ø {euro(statistics.durchschnitt)}</p>
      </div>
      <div className="mt-1.5 flex items-end justify-between gap-2">
        {statistics.monate.map((m, i) => (
          <button
            type="button"
            key={m.monat}
            onClick={() => setActiveBar(i)}
            aria-label={`${shortMonth(m.monat)}: ${euro(m.betrag)}`}
            className="flex flex-1 flex-col items-center gap-1"
          >
            <span className="betragszahl h-4 text-[10px] font-medium leading-none text-text">
              {activeBar === i ? euro(m.betrag) : ''}
            </span>
            <span className="relative block h-11 w-full rounded-[3px] bg-karte2">
              <span
                className={`absolute inset-x-0 bottom-0 rounded-t-[3px] ${
                  m.monat === currentMonth ? 'bg-akzent' : 'bg-akzent/35'
                }`}
                style={{ height: Math.max(3, (m.betrag / Math.max(max, 1)) * 44) }}
              />
            </span>
            <span className="text-[10px] leading-none text-textschwach">{shortMonth(m.monat)}</span>
          </button>
        ))}
      </div>
    </div>
  )
}

function SharedPill() {
  return <span className="text-textschwach">geteilt</span>
}

export function CategorySheet({
  name,
  entries,
  budget,
  recommendation,
  close,
}: {
  name: string | null
  entries: Entry[]
  budget: BudgetInfo | null
  recommendation: CoachingRecommendation | null
  close: () => void
}) {
  const reduced = useReducedMotion()
  const { activePerson } = useApp()
  const open = name !== null
  const [attitude, setAttitude] = useState<Attitude | null>(null)
  const [selectorOpen, setSelectorOpen] = useState(false)
  const [trend, setTrend] = useState<CategoryStatistics | null>(null)
  const hasBudget = budget !== null

  useEffect(() => {
    setSelectorOpen(false)
    if (name === null || !hasBudget) {
      setAttitude(null)
      return
    }
    read<CategoryInfo[]>('/api/kategorien')
      .then((list) => setAttitude(list.find((k) => k.name === name)?.haltung ?? 'FLEXIBEL'))
      .catch(() => setAttitude('FLEXIBEL'))
  }, [name, hasBudget])

  useEffect(() => {
    setTrend(null)
    if (!open || name === null) return
    let active = true
    const together = localStorage.getItem('lunefi:sicht') === 'zusammen'
    const viewParam = together ? 'gemeinsam=true' : activePerson ? `person=${encodeURIComponent(activePerson)}` : ''
    read<CategoryStatistics>(
      `/api/statistik/kategorie/${encodeURIComponent(name)}?monate=6${viewParam ? `&${viewParam}` : ''}`,
    )
      .then((result) => {
        if (active) setTrend(result)
      })
      .catch(() => {})
    return () => {
      active = false
    }
  }, [open, name, activePerson])

  const list = [...entries].sort((a, b) =>
    a.datum === b.datum ? b.id - a.id : a.datum < b.datum ? 1 : -1,
  )
  const sum = list.reduce((s, entry) => s + entry.betrag, 0)
  const week = list.reduce((s, entry) => s + (isThisWeek(entry.datum) ? entry.betrag : 0), 0)
  const tip = recommendation && attitude === recommendation.haltung ? recommendation.tipp : null

  return createPortal(
    <AnimatePresence>
      {open && name !== null && (
        <div className="fixed inset-0 z-30 flex flex-col justify-end">
          <motion.button
            aria-label="Schließen"
            className="absolute inset-0 bg-black/60"
            onClick={close}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.18 }}
          />
          <motion.div
            className="relative mx-auto max-h-[calc(100dvh-16px)] w-full max-w-[560px] overflow-y-auto overscroll-contain rounded-t-3xl border-t border-strich bg-karte px-5 pb-5 pt-4"
            initial={{ transform: 'translateY(100%)' }}
            animate={{ transform: 'translateY(0%)' }}
            exit={{ transform: 'translateY(100%)' }}
            transition={reduced ? { duration: 0 } : { type: 'spring', duration: 0.5, bounce: 0.2 }}
            drag="y"
            dragConstraints={{ top: 0, bottom: 0 }}
            dragElastic={{ top: 0, bottom: 0.6 }}
            onDragEnd={(_, info) => {
              if (info.offset.y > 120 || info.velocity.y > 500) {
                close()
              }
            }}
          >
            <div className="mx-auto mb-2.5 h-1 w-10 rounded-full bg-strich" />
            <div className="mb-3 flex items-center gap-3">
              <CategoryIconTile name={name} groesse={32} iconGroesse={18} />
              <h3 className="text-lg font-semibold">{name}</h3>
            </div>
            <p
              className={`betragszahl text-[34px] font-semibold leading-none tracking-tight ${
                sum < 0 ? 'text-schlecht' : sum > 0 ? 'text-gut' : 'text-text'
              }`}
            >
              {euro(sum)}
            </p>
            <p className="mt-1 text-sm text-textschwach">
              Diese Woche:{' '}
              <span className="betragszahl font-medium text-text">{euro(week)}</span>
            </p>
            {budget && attitude && (
              <div className="mt-3 flex min-h-8 flex-wrap items-center gap-2">
                <p className="text-sm text-textschwach">
                  Budget{' '}
                  <span className="betragszahl font-medium text-text">{euro(budget.ist)}</span> von{' '}
                  <span className="betragszahl font-medium text-text">{euro(budget.limit)}</span>
                </p>
                <button
                  type="button"
                  onClick={() => setSelectorOpen((prev) => !prev)}
                  aria-expanded={selectorOpen}
                  className="flex h-7 shrink-0 items-center rounded-full bg-karte2 px-3 text-[12px] font-medium text-textschwach transition-colors"
                >
                  {ATTITUDE_LABEL[attitude]}
                </button>
              </div>
            )}
            {budget && attitude && selectorOpen && name !== null && (
              <div className="mt-1.5">
                <AttitudeSelector kategorie={name} haltung={attitude} onAttitude={setAttitude} />
              </div>
            )}
            {tip && <p className="mt-2 text-sm text-gut">{tip}</p>}
            {trend !== null && trend.monate.length > 0 && <TrendRow statistics={trend} />}
            {list.length === 0 ? (
              <p className="mt-4 text-sm text-textschwach">Keine Einträge in diesem Monat.</p>
            ) : (
              <ul className="mt-4">
                {list.map((entry, i) => (
                  <li
                    key={entry.id}
                    className={`flex items-center justify-between gap-3 py-2 ${
                      i < list.length - 1 ? 'border-b border-strich' : ''
                    }`}
                  >
                    <p className="flex min-w-0 flex-wrap items-center gap-1.5 text-sm text-textschwach">
                      <span className="font-medium text-text">{shortDate(entry.datum)}</span>
                      <span aria-hidden="true">·</span>
                      {entry.gemeinsam ? (
                        <>
                          <SharedPill />
                          {entry.zahler && (
                            <>
                              <span aria-hidden="true">·</span>
                              <span>{entry.zahler}</span>
                            </>
                          )}
                        </>
                      ) : (
                        <span>{entry.person}</span>
                      )}
                    </p>
                    <span
                      className={`betragszahl shrink-0 text-[15px] font-semibold ${
                        entry.betrag < 0 ? 'text-schlecht' : 'text-gut'
                      }`}
                    >
                      {entry.betrag > 0 ? '+' : ''}
                      {euro(entry.betrag)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </motion.div>
        </div>
      )}
    </AnimatePresence>,
    document.body,
  )
}
