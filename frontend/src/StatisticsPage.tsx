import { useEffect, useState } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { Lightbulb } from '@phosphor-icons/react'
import { read, type MonthlyOverview, type YearStatistics } from './types'
import { amountCompact, euro, monthAsString, monthLabel, shortMonth } from './format'
import { CategoryIconTile } from './categoryIcon'
import { CategorySheet } from './CategorySheet'
import { entriesForCategory } from './categoryEntries'
import { useApp } from './AppContext'

type View = 'meine' | 'zusammen'

interface SavingsHint {
  kategorie: string
  ist: number
  ueblich: number
  text: string
}

interface SavingsPotential {
  hinweise: SavingsHint[]
  basis: 'OK' | 'WENIG_DATEN'
}

interface Segment {
  name: string
  betrag: number
}

const SIZE = 150
const RADIUS = 61
const STROKE_WIDTH = 16
const CIRCUMFERENCE = 2 * Math.PI * RADIUS
const SLICES = 5

const COLORS = [
  'var(--color-akzent)',
  'color-mix(in srgb, var(--color-akzent) 70%, var(--color-textschwach))',
  'color-mix(in srgb, var(--color-akzent) 50%, var(--color-textschwach))',
  'color-mix(in srgb, var(--color-akzent) 34%, var(--color-textschwach))',
  'color-mix(in srgb, var(--color-akzent) 22%, var(--color-textschwach))',
]

const OTHERS_COLOR = 'color-mix(in srgb, var(--color-textschwach) 40%, transparent)'

function loadView(): View {
  return localStorage.getItem('lunefi:sicht') === 'zusammen' ? 'zusammen' : 'meine'
}

function color(name: string, rang: number): string {
  if (name === 'Sonstige') return OTHERS_COLOR
  return COLORS[rang] ?? OTHERS_COLOR
}

function Donut({
  segments,
  total,
  selected,
  onSelect,
}: {
  segments: Segment[]
  total: number
  selected: string | null
  onSelect: (segment: Segment) => void
}) {
  const reduced = useReducedMotion()
  const startFractions = segments.reduce<number[]>((list, segment) => {
    list.push((list[list.length - 1] ?? 0) + segment.betrag / Math.max(total, 1))
    return list
  }, [])
  const withGaps = segments.length > 1
  const active = segments.find((s) => s.name === selected) ?? null

  return (
    <div className="relative mx-auto" style={{ width: SIZE, height: SIZE }}>
      <svg width={SIZE} height={SIZE} viewBox={`0 0 ${SIZE} ${SIZE}`} aria-label="Ausgaben je Kategorie">
        {segments.map((segment, i) => {
          const share = segment.betrag / Math.max(total, 1)
          const length = share * CIRCUMFERENCE
          const visible = withGaps ? Math.max(0, length - 2.5) : CIRCUMFERENCE
          const offset = -(startFractions[i - 1] ?? 0) * CIRCUMFERENCE
          const colorValue = color(segment.name, i)
          const dimmed = selected !== null && segment.name !== selected
          const common = { cx: 75, cy: 75, r: RADIUS, fill: 'none' as const, transform: 'rotate(-90 75 75)' }
          return (
            <g
              key={segment.name}
              style={{ opacity: dimmed ? 0.35 : 1, transition: 'opacity 150ms ease-out' }}
            >
              <circle
                {...common}
                stroke={colorValue}
                strokeWidth={STROKE_WIDTH}
                strokeDasharray={`${visible} ${CIRCUMFERENCE - visible}`}
                strokeDashoffset={offset}
              />
              <circle
                {...common}
                stroke="black"
                strokeOpacity={0}
                strokeWidth={STROKE_WIDTH + 14}
                strokeDasharray={`${visible} ${CIRCUMFERENCE - visible}`}
                strokeDashoffset={offset}
                pointerEvents="stroke"
                className="cursor-pointer"
                onClick={() => onSelect(segment)}
              />
            </g>
          )
        })}
      </svg>
      <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
        <AnimatePresence mode="wait" initial={false}>
          <motion.div
            key={active?.name ?? 'gesamt'}
            className="flex w-24 flex-col items-center text-center"
            initial={{ opacity: 0, y: reduced ? 0 : 2 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, transition: reduced ? { duration: 0 } : { duration: 0.12 } }}
            transition={reduced ? { duration: 0 } : { duration: 0.18, ease: [0.22, 1, 0.36, 1] }}
          >
            {active ? (
              <>
                <p className="w-full truncate text-[12px] font-medium text-textschwach">{active.name}</p>
                <p className="betragszahl text-[15px] font-semibold leading-tight">{euro(active.betrag)}</p>
                <p className="betragszahl text-[11px] text-textschwach">
                  {Math.round((active.betrag / Math.max(total, 1)) * 100)} %
                </p>
              </>
            ) : (
              <>
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">Ausgaben</p>
                <p className="betragszahl text-[17px] font-semibold leading-tight">{euro(total)}</p>
              </>
            )}
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  )
}

function YearView({ year }: { year: YearStatistics | null }) {
  const currentMonth = monthAsString(new Date())
  if (year === null) {
    return (
      <div className="space-y-3">
        <div className="h-20 animate-pulse rounded-2xl bg-karte" />
        <div className="h-44 animate-pulse rounded-2xl bg-karte" />
      </div>
    )
  }
  const income = year.monate.reduce((sum, m) => sum + m.einnahmen, 0)
  const expense = year.monate.reduce((sum, m) => sum + m.ausgaben, 0)
  const rest = income - expense
  const scale = Math.max(income, expense, 1)
  const categories = Object.entries(year.ausgabenJeKategorie).sort((a, b) => b[1] - a[1])
  const catMax = categories.reduce((max, [, value]) => Math.max(max, value), 1)
  const incomeCategories = Object.entries(year.einnahmenJeKategorie).sort((a, b) => b[1] - a[1])
  const activeMonths = year.monate.filter((m) => m.einnahmen > 0 || m.ausgaben > 0).length
  const avgExpense = activeMonths > 0 ? expense / activeMonths : 0
  const best = [...year.monate].sort((a, b) => b.differenz - a.differenz)[0]

  return (
    <>
      <div className="mb-2 rounded-2xl bg-karte p-3">
        <div className="grid grid-cols-3 gap-2 text-center">
          <div>
            <p className="text-[10px] font-medium uppercase tracking-[0.08em] text-textschwach">Einnahmen</p>
            <p className="betragszahl text-[16px] font-semibold text-gut">{amountCompact(income)}</p>
          </div>
          <div>
            <p className="text-[10px] font-medium uppercase tracking-[0.08em] text-textschwach">Ausgaben</p>
            <p className="betragszahl text-[16px] font-semibold text-schlecht">{amountCompact(expense)}</p>
          </div>
          <div>
            <p className="text-[10px] font-medium uppercase tracking-[0.08em] text-textschwach">Übrig</p>
            <p
              className={`betragszahl text-[16px] font-semibold ${
                rest >= 0 ? 'text-gut' : 'text-schlecht'
              }`}
            >
              {amountCompact(rest)}
            </p>
          </div>
        </div>
        {activeMonths > 0 && (
          <p className="mt-2 text-center text-[11px] text-textschwach">
            Ø {euro(avgExpense)} Ausgaben pro Monat · stärkster Monat: {monthLabel(best.monat)}
          </p>
        )}
      </div>

      <div className="mb-2 rounded-2xl bg-karte px-2.5 pb-1.5 pt-3">
        <p className="mb-2 px-1 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
          Alle Monate
        </p>
        <div className="flex items-stretch justify-between gap-0.5">
          {year.monate.map((m) => {
            const current = m.monat === currentMonth
            const incomeHeight = m.einnahmen > 0 ? Math.max(2, (m.einnahmen / scale) * 30) : 0
            const expenseHeight = m.ausgaben > 0 ? Math.max(2, (m.ausgaben / scale) * 30) : 0
            return (
              <div key={m.monat} className="flex flex-1 flex-col items-center" title={`${monthLabel(m.monat)}: +${euro(m.einnahmen)} / -${euro(m.ausgaben)}`}>
                <span className="flex h-8 w-full items-end justify-center">
                  {incomeHeight > 0 && (
                    <span
                      className={`w-2 rounded-t-[2px] ${current ? 'bg-gut' : 'bg-gut/60'}`}
                      style={{ height: incomeHeight }}
                    />
                  )}
                </span>
                <span className="my-px h-px w-full bg-strich" />
                <span className="flex h-8 w-full items-start justify-center">
                  {expenseHeight > 0 && (
                    <span
                      className={`w-2 rounded-b-[2px] ${current ? 'bg-schlecht' : 'bg-schlecht/60'}`}
                      style={{ height: expenseHeight }}
                    />
                  )}
                </span>
                <span
                  className={`mt-1 text-[9px] leading-none ${
                    current ? 'font-semibold text-text' : 'text-textschwach'
                  }`}
                >
                  {shortMonth(m.monat)}
                </span>
              </div>
            )
          })}
        </div>
      </div>

      {categories.length > 0 && (
        <section>
          <h3 className="mb-1 text-[13px] font-semibold">Ausgaben nach Kategorie</h3>
          <ul>
            {categories.map(([name, betrag]) => (
              <li key={name} className="flex items-center gap-2.5 border-b border-strich py-1.5 last:border-b-0">
                <CategoryIconTile name={name} />
                <span className="min-w-0 flex-1 truncate text-[14px] font-semibold">{name}</span>
                <span className="h-1.5 w-14 shrink-0 overflow-hidden rounded-full bg-karte2">
                  <span
                    className="block h-full rounded-full bg-akzent"
                    style={{ width: `${(betrag / catMax) * 100}%` }}
                  />
                </span>
                <span className="betragszahl w-20 shrink-0 text-right text-[13px] font-semibold">
                  {euro(betrag)}
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {incomeCategories.length > 0 && (
        <section className="mt-2">
          <h3 className="mb-1 text-[13px] font-semibold">Einnahmen</h3>
          <ul>
            {incomeCategories.map(([name, betrag]) => (
              <li key={name} className="flex items-center gap-2.5 border-b border-strich py-1.5 last:border-b-0">
                <CategoryIconTile name={name} />
                <span className="min-w-0 flex-1 truncate text-[14px] font-semibold">{name}</span>
                <span className="betragszahl shrink-0 text-[13px] font-semibold text-gut">{euro(betrag)}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  )
}

export function StatisticsPage() {
  const { activePerson } = useApp()
  const monat = monthAsString(new Date())
  const jahr = new Date().getFullYear()
  const [view, setView] = useState<View>(loadView)
  const [yearView, setYearView] = useState(false)
  const [data, setData] = useState<MonthlyOverview | null>(null)
  const [year, setYear] = useState<YearStatistics | null>(null)
  const [savings, setSavings] = useState<SavingsPotential | null>(null)
  const [selected, setSelected] = useState<string | null>(null)
  const [detail, setDetail] = useState<string | null>(null)

  const filtered = view === 'meine' && activePerson !== null
  const together = view === 'zusammen'
  const personParam = filtered ? `person=${encodeURIComponent(activePerson ?? '')}` : ''

  useEffect(() => {
    let active = true
    setSelected(null)
    read<MonthlyOverview>(`/api/monatsuebersicht/${monat}${together ? '?gemeinsam=true' : personParam ? `?${personParam}` : ''}`)
      .then((result) => {
        if (active) setData(result)
      })
      .catch(() => {
        if (active) setData(null)
      })
    const savingsPath = `/api/statistik/sparpotenzial?monat=${monat}${
      together ? '&gemeinsam=true' : personParam ? `&${personParam}` : ''
    }`
    read<SavingsPotential>(savingsPath)
      .then((result) => {
        if (active) setSavings(result)
      })
      .catch(() => {
        if (active) setSavings(null)
      })
    return () => {
      active = false
    }
  }, [monat, together, personParam])

  useEffect(() => {
    if (!yearView) return
    let active = true
    read<YearStatistics>(
      `/api/statistik/jahr?jahr=${jahr}${together ? '&gemeinsam=true' : personParam ? `&${personParam}` : ''}`,
    )
      .then((result) => {
        if (active) setYear(result)
      })
      .catch(() => {
        if (active) setYear(null)
      })
    return () => {
      active = false
    }
  }, [yearView, jahr, together, personParam])

  const expenses = Object.entries(data?.ausgabenJeKategorie ?? {}).sort((a, b) => b[1] - a[1])
  const total = expenses.reduce((sum, [, betrag]) => sum + betrag, 0)
  const segments: Segment[] = expenses.slice(0, SLICES).map(([name, betrag]) => ({ name, betrag }))
  const rest = expenses.slice(SLICES)
  if (rest.length > 0) {
    segments.push({ name: 'Sonstige', betrag: rest.reduce((sum, [, betrag]) => sum + betrag, 0) })
  }

  const select = (segment: Segment) => {
    setSelected(segment.name)
    if (segment.name !== 'Sonstige') setDetail(segment.name)
  }

  const showSavingsPotential = savings !== null && (savings.basis === 'WENIG_DATEN' || savings.hinweise.length > 0)

  return (
    <div>
      <div className="mb-1 flex items-center justify-between">
        <h2 className="text-[15px] font-semibold">Statistik</h2>
        <div className="flex h-7 items-center gap-0.5 rounded-full bg-karte2 p-0.5">
          <button
            onClick={() => setYearView(false)}
            aria-pressed={!yearView}
            className={`flex h-6 items-center rounded-full px-2.5 text-[11px] font-medium transition-colors ${
              !yearView ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
            }`}
          >
            Monat
          </button>
          <button
            onClick={() => setYearView(true)}
            aria-pressed={yearView}
            className={`flex h-6 items-center rounded-full px-2.5 text-[11px] font-medium transition-colors ${
              yearView ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
            }`}
          >
            Jahr
          </button>
        </div>
      </div>

      <div className="mb-2 flex h-8 w-fit items-center gap-0.5 rounded-full bg-karte2 p-0.5">
        <button
          onClick={() => {
            setView('meine')
            localStorage.setItem('lunefi:sicht', 'meine')
          }}
          aria-pressed={filtered}
          className={`flex h-7 items-center rounded-full px-3 text-xs font-medium transition-colors ${
            filtered ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
          }`}
        >
          {activePerson ?? 'Meine'}
        </button>
        <button
          onClick={() => {
            setView('zusammen')
            localStorage.setItem('lunefi:sicht', 'zusammen')
          }}
          aria-pressed={together}
          className={`flex h-7 items-center rounded-full px-3 text-xs font-medium transition-colors ${
            together ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
          }`}
        >
          Gemeinsam
        </button>
      </div>

      {yearView ? (
        <YearView year={year} />
      ) : (
        <>
          {data === null ? (
        <div className="space-y-3">
          <div className="h-36 animate-pulse rounded-2xl bg-karte" />
          <div className="h-16 animate-pulse rounded-2xl bg-karte" />
        </div>
      ) : segments.length === 0 ? (
        <p className="py-8 text-sm text-textschwach">Keine Ausgaben in diesem Monat.</p>
      ) : (
        <>
          <Donut segments={segments} total={total} selected={selected} onSelect={select} />

          <ul className="mt-2">
            {segments.map((segment) => (
              <li key={segment.name} className="border-b border-strich last:border-b-0">
                <button
                  type="button"
                  onClick={() => select(segment)}
                  aria-label={`${segment.name} anzeigen`}
                  className="flex w-full items-center gap-2.5 py-1.5 text-left"
                >
                  {segment.name === 'Sonstige' ? (
                    <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg">
                      <span
                        className="h-2.5 w-2.5 rounded-full"
                        style={{ background: OTHERS_COLOR, border: '1px solid var(--color-textschwach)' }}
                      />
                    </span>
                  ) : (
                    <CategoryIconTile name={segment.name} />
                  )}
                  <span className="min-w-0 flex-1 truncate text-[15px] font-semibold">{segment.name}</span>
                  <span className="betragszahl shrink-0 text-[13px] font-semibold">{euro(segment.betrag)}</span>
                </button>
              </li>
            ))}
          </ul>
        </>
      )}

          {showSavingsPotential && (
            <section className="mt-2">
              <h3 className="flex items-center gap-1.5 text-[13px] font-semibold">
                <Lightbulb size={15} weight="fill" className="text-akzent" aria-hidden="true" />
                Was auffällt
              </h3>
              {savings.basis === 'WENIG_DATEN' ? (
                <p className="mt-1 text-sm text-textschwach">Noch zu wenig Verlauf für Tipps.</p>
              ) : (
                <ul className="mt-1">
                  {savings.hinweise.map((hint) => (
                    <li key={hint.kategorie} className="flex items-start gap-2.5 py-1.5">
                      <CategoryIconTile name={hint.kategorie} groesse={28} />
                      <p className="pt-1 text-sm leading-snug text-text">{hint.text}</p>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          )}
        </>
      )}

      <CategorySheet
        name={detail}
        entries={data ? entriesForCategory(data.eintraege, detail ?? '') : []}
        budget={detail && data?.budgets ? (data.budgets[detail] ?? null) : null}
        recommendation={null}
        close={() => setDetail(null)}
      />
    </div>
  )
}
