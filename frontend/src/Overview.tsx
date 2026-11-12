import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { AnimatePresence, motion, useReducedMotion, type Transition } from 'motion/react'
import { ArrowsLeftRight, CaretDown, CaretRight, LockSimple, Sparkle, Target, Wallet } from '@phosphor-icons/react'
import { read, type Settlement, type Coaching, type Entry, type Account, type MonthlyOverview, type SavingsGoal, type HistoryMonth } from './types'
import {
  shortDate,
  amountCompact,
  euro,
  isThisWeek,
  monthAsString,
  shortMonth,
  monthLabel,
  shiftMonth,
} from './format'
import { CategoryIcon, CategoryIconTile, categoryIcon, categoryIconName } from './categoryIcon'
import { CategorySheet } from './CategorySheet'
import { entriesForCategory } from './categoryEntries'
import { useApp } from './AppContext'

const VISIBLE_CATEGORIES = 3
const VISIBLE_ENTRIES = 3
const VISIBLE_REMAINING = 3
const VISIBLE_GOALS = 2

const BAR_WEIGHTS = ['bg-schlecht', 'bg-schlecht/70', 'bg-schlecht/55', 'bg-schlecht/45', 'bg-schlecht/35']

type View = 'meine' | 'zusammen'

function onlyShared(data: MonthlyOverview): MonthlyOverview {
  const entries = data.eintraege.filter((entry) => entry.gemeinsam)
  const ausgabenJeKategorie: Record<string, number> = {}
  const einnahmenJeKategorie: Record<string, number> = {}
  for (const entry of entries) {
    const target = entry.betrag < 0 ? ausgabenJeKategorie : einnahmenJeKategorie
    target[entry.kategorie] = (target[entry.kategorie] ?? 0) + Math.abs(entry.betrag)
  }
  const budgets = Object.fromEntries(
    Object.entries(data.budgets).map(([name, budget]) => {
      const ist = ausgabenJeKategorie[name] ?? 0
      return [name, { limit: budget.limit, ist, warnung: ist > budget.limit }]
    }),
  )
  return { ...data, eintraege: entries, ausgabenJeKategorie, einnahmenJeKategorie, budgets }
}

function loadView(): View {
  return localStorage.getItem('lunefi:sicht') === 'zusammen' ? 'zusammen' : 'meine'
}

function SharedPill() {
  return (
    <span
      title="geteilt"
      className="inline-flex h-5 shrink-0 items-center rounded-full px-1 text-textschwach"
      style={{ background: 'color-mix(in srgb, var(--color-text) 8%, transparent)' }}
    >
      <ArrowsLeftRight size={12} aria-hidden="true" />
    </span>
  )
}

function MiniBar({
  name,
  sum,
  scale,
  rank,
  onSelect,
}: {
  name: string
  sum: number
  scale: number
  rank: number
  onSelect: () => void
}) {
  const width = Math.min(100, (sum / Math.max(scale, 1)) * 100)
  return (
    <div>
      <button
        type="button"
        onClick={onSelect}
        aria-label={`${name} anzeigen`}
        className="block w-full text-left"
      >
        <div className="flex items-center justify-between gap-2">
          <span className="flex min-w-0 items-center gap-3">
            <CategoryIconTile name={name} />
            <span className="truncate text-[15px] font-semibold text-text">{name}</span>
          </span>
          <span className="betragszahl shrink-0 text-[13px] font-semibold">{euro(sum)}</span>
        </div>
        <div className="mt-1 h-1.5 rounded-full bg-karte2">
          <div
            className={`h-full rounded-full ${BAR_WEIGHTS[rank] ?? 'bg-schlecht/30'}`}
            style={{
              width: `${width}%`,
              transition: 'width 220ms cubic-bezier(0.22, 1, 0.36, 1)',
            }}
          />
        </div>
      </button>
    </div>
  )
}

function TrendBar({
  entry,
  scale,
  rank,
  reduced,
}: {
  entry: HistoryMonth
  scale: number
  rank: number
  reduced: boolean | null
}) {
  const incomeHeight = entry.einnahmen > 0 ? Math.max(2, Math.round((entry.einnahmen / Math.max(scale, 1)) * 10)) : 0
  const expenseHeight = entry.ausgaben > 0 ? Math.max(2, Math.round((entry.ausgaben / Math.max(scale, 1)) * 10)) : 0
  const grow: Transition = reduced
    ? { duration: 0 }
    : { duration: 0.3, ease: [0.22, 1, 0.36, 1] as const, delay: rank * 0.03 }
  return (
    <span className="flex shrink-0 items-center gap-1">
      <span className="relative block h-6 w-2">
        <span className="absolute inset-x-0 top-1/2 h-px bg-strich" />
        {incomeHeight > 0 && (
          <motion.span
            className="absolute inset-x-0 bottom-1/2 rounded-t-[2px] bg-gut/60"
            style={{ height: incomeHeight, transformOrigin: 'bottom' }}
            initial={reduced ? false : { scaleY: 0 }}
            animate={{ scaleY: 1 }}
            transition={grow}
          />
        )}
        {expenseHeight > 0 && (
          <motion.span
            className="absolute inset-x-0 top-1/2 rounded-b-[2px] bg-schlecht/60"
            style={{ height: expenseHeight, transformOrigin: 'top' }}
            initial={reduced ? false : { scaleY: 0 }}
            animate={{ scaleY: 1 }}
            transition={grow}
          />
        )}
      </span>
      <span className="text-[10px] leading-none text-textschwach">{shortMonth(entry.monat)}</span>
    </span>
  )
}

function RemainingRow({ name, rest }: { name: string; rest: number }) {
  return (
    <li className="flex h-7 items-center justify-between gap-2">
      <span className="flex min-w-0 items-center gap-2">
        <CategoryIcon name={name} size={15} />
        <span className="truncate text-[13px] font-medium text-text">{name}</span>
      </span>
      <span
        className={`betragszahl shrink-0 text-[13px] font-semibold ${
          rest >= 0 ? 'text-gut' : 'text-schlecht'
        }`}
      >
        {rest >= 0 ? `noch ${euro(rest)}` : `${euro(Math.abs(rest))} drüber`}
      </span>
    </li>
  )
}

function GoalRow({ goal }: { goal: SavingsGoal }) {
  const percent = Math.min(100, Math.floor((goal.eingezahlt / Math.max(goal.zielbetrag, 1)) * 100))
  const resolution = categoryIconName(goal.name)
  const Icon = resolution === 'Wallet' ? Target : categoryIcon(goal.name)
  return (
    <li className="flex h-7 items-center justify-between gap-2">
      <span className="flex min-w-0 items-center gap-2">
        <Icon size={15} aria-hidden="true" className="shrink-0 text-textschwach" />
        <span className="truncate text-[13px] font-medium text-text">{goal.name}</span>
      </span>
      <span className="betragszahl shrink-0 text-[13px] font-medium text-textschwach">
        {amountCompact(goal.eingezahlt)} von {amountCompact(goal.zielbetrag)} ({percent} %)
      </span>
    </li>
  )
}

export function Overview({
  onEditEntry,
}: {
  onEditEntry: (entry: Entry) => void
}) {
  const { activePerson } = useApp()
  const reduced = useReducedMotion()
  const [monat, setMonat] = useState(() => monthAsString(new Date()))
  const [view, setView] = useState<View>(loadView)
  const [data, setData] = useState<MonthlyOverview | null>(null)
  const [settlement, setSettlement] = useState<Settlement | null>(null)
  const [accounts, setAccounts] = useState<Account[]>([])
  const [history, setHistory] = useState<HistoryMonth[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [categoriesExpanded, setCategoriesExpanded] = useState(false)
  const [entriesExpanded, setEntriesExpanded] = useState(false)
  const [categoryDetail, setCategoryDetail] = useState<string | null>(null)
  const [sharedLocal, setSharedLocal] = useState(false)
  const [coaching, setCoaching] = useState<Coaching | null>(null)
  const [savingsGoals, setSavingsGoals] = useState<SavingsGoal[]>([])
  const [remainingExpanded, setRemainingExpanded] = useState(false)
  const [goalsExpanded, setGoalsExpanded] = useState(false)

  const setViewValue = (next: View) => {
    setView(next)
    localStorage.setItem('lunefi:sicht', next)
  }

  const filtered = view === 'meine' && activePerson !== null
  const together = view === 'zusammen'

  const load = useCallback(() => {
    setLoading(true)
    const personParam = filtered ? `?person=${encodeURIComponent(activePerson ?? '')}` : ''
    const overviewLoad = together
      ? read<MonthlyOverview>(`/api/monatsuebersicht/${monat}?gemeinsam=true`)
          .catch(() => read<MonthlyOverview>(`/api/monatsuebersicht/${monat}`))
          .then((raw) => {
            if (raw.eintraege.some((entry) => !entry.gemeinsam)) {
              setSharedLocal(true)
              return onlyShared(raw)
            }
            setSharedLocal(false)
            return raw
          })
      : read<MonthlyOverview>(`/api/monatsuebersicht/${monat}${personParam}`).then((raw) => {
          setSharedLocal(false)
          return raw
        })
    const accountsLoad = together
      ? Promise.resolve<Account[]>([])
      : read<Account[]>(filtered ? `/api/konten${personParam}` : '/api/konten')
    Promise.all([overviewLoad, read<Settlement>(`/api/ausgleich/${monat}`), accountsLoad])
      .then(([overview, settlementData, accountList]) => {
        setData(overview)
        setSettlement(settlementData)
        setAccounts(accountList)
        setError(false)
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [monat, filtered, together, activePerson])

  useEffect(load, [load])

  useEffect(() => {
    let active = true
    read<Coaching>(`/api/coaching?monat=${monat}`)
      .then((result) => {
        if (active) setCoaching(result)
      })
      .catch(() => {
        if (active) setCoaching(null)
      })
    return () => {
      active = false
    }
  }, [monat])

  useEffect(() => {
    const request = together
      ? read<HistoryMonth[]>('/api/verlauf?monate=3&gemeinsam=true').catch(() =>
          read<HistoryMonth[]>('/api/verlauf?monate=3'),
        )
      : read<HistoryMonth[]>(
          filtered ? `/api/verlauf?monate=3&person=${encodeURIComponent(activePerson ?? '')}` : '/api/verlauf?monate=3',
        )
    request
      .then((list) => setHistory(Array.isArray(list) && list.length > 0 ? list : null))
      .catch(() => setHistory(null))
  }, [filtered, together, activePerson])

  useEffect(() => {
    if (!together) return
    let active = true
    read<SavingsGoal[]>('/api/sparziele')
      .then((list) => {
        if (active) setSavingsGoals(Array.isArray(list) ? list : [])
      })
      .catch(() => {
        if (active) setSavingsGoals([])
      })
    return () => {
      active = false
    }
  }, [together])

  const total = accounts.reduce((sum, account) => sum + account.saldo, 0)
  const expenses = Object.values(data?.ausgabenJeKategorie ?? {}).reduce((a, b) => a + b, 0)
  const income = Object.values(data?.einnahmenJeKategorie ?? {}).reduce((a, b) => a + b, 0)
  const categories = Object.entries(data?.ausgabenJeKategorie ?? {}).sort((a, b) => b[1] - a[1])
  const categoryScale = categories.reduce((max, [, sum]) => Math.max(max, sum), 1)
  const budgetList = Object.values(data?.budgets ?? {})
  const budgetActual = budgetList.reduce((a, b) => a + b.ist, 0)
  const budgetLimit = budgetList.reduce((a, b) => a + b.limit, 0)
  const budgetOver = budgetActual > budgetLimit
  const budgetScale = Math.max(budgetActual, budgetLimit, 1)
  const remainingList = Object.entries(data?.budgets ?? {})
    .map(([name, budget]) => ({ name, rest: budget.limit - budget.ist }))
    .sort((a, b) => a.rest - b.rest)
  const entries = [...(data?.eintraege ?? [])].sort((a, b) =>
    a.datum === b.datum ? b.id - a.id : a.datum < b.datum ? 1 : -1,
  )
  const isCurrentMonth = monat === monthAsString(new Date())
  const week = isCurrentMonth
    ? entries.reduce((sum, entry) => sum + (entry.betrag < 0 && isThisWeek(entry.datum) ? entry.betrag : 0), 0)
    : null
  const categoryEntries = data ? entriesForCategory(data.eintraege, categoryDetail ?? '') : []
  const historyScale = history
    ? history.reduce((max, v) => Math.max(max, v.einnahmen, v.ausgaben), 1)
    : 1
  const historyAverage =
    history && history.length > 0
      ? history.reduce((sum, v) => sum + v.differenz, 0) / history.length
      : null

  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <button
          aria-label="Vormonat"
          onClick={() => setMonat(shiftMonth(monat, -1))}
          className="-ml-3 flex h-9 w-9 items-center justify-center text-xl text-textschwach"
        >
          ‹
        </button>
        <h2 className="text-[15px] font-semibold">{monthLabel(monat)}</h2>
        <button
          aria-label="Folgemonat"
          onClick={() => setMonat(shiftMonth(monat, 1))}
          disabled={monat >= monthAsString(new Date())}
          className="-mr-3 flex h-9 w-9 items-center justify-center text-xl text-textschwach disabled:opacity-40"
        >
          ›
        </button>
      </div>

      {loading && (
        <div className="space-y-3">
          <div className="h-24 animate-pulse rounded-2xl bg-karte" />
          <div className="h-32 animate-pulse rounded-2xl bg-karte" />
        </div>
      )}

      {error && <p className="text-schlecht">Die Übersicht konnte nicht geladen werden.</p>}

      {data && !loading && (
        <>
          <section className="mb-2">
            {together ? (
              <>
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                  Gemeinsam
                </p>
                {settlement && (
                  <p
                    className={`betragszahl mt-1 text-[34px] font-semibold leading-[1.15] tracking-tight ${
                      settlement.vorschlag ? 'text-text' : 'text-gut'
                    }`}
                  >
                    {settlement.vorschlag ?? 'Alles ausgeglichen'}
                  </p>
                )}
              </>
            ) : (
              <>
                <p className="flex items-center gap-1 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                  {filtered ? (
                    <>
                      Mein Saldo
                      <LockSimple size={12} aria-label="privat" className="normal-case" />
                    </>
                  ) : (
                    'Gesamtsaldo'
                  )}
                </p>
                <p
                  className={`betragszahl mt-1 text-[44px] font-semibold leading-none tracking-tight ${
                    total < 0 ? 'text-schlecht' : 'text-text'
                  }`}
                >
                  {amountCompact(total, 1000000)}
                </p>
              </>
            )}
            <div className="mt-1.5 flex h-8 w-fit items-center gap-0.5 rounded-full bg-karte2 p-0.5">
              <button
                onClick={() => setViewValue('meine')}
                aria-pressed={filtered}
                className={`flex h-7 items-center rounded-full px-3 text-xs font-medium transition-colors ${
                  filtered ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
                }`}
              >
                {activePerson ?? 'Meine'}
              </button>
              <button
                onClick={() => setViewValue('zusammen')}
                aria-pressed={!filtered}
                className={`flex h-7 items-center rounded-full px-3 text-xs font-medium transition-colors ${
                  !filtered ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
                }`}
              >
                Gemeinsam
              </button>
            </div>
            {settlement && (
              <Link to="/ausgleich" className="-my-1 flex min-h-7 items-center gap-2 py-1">
                <span className="shrink-0 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                  Ausgleich
                </span>
                <span className="betragszahl truncate text-[13px] font-medium text-text">
                  {settlement.vorschlag ?? 'Alles ausgeglichen'}
                </span>
                <CaretRight size={13} className="ml-auto shrink-0 text-textschwach" />
              </Link>
            )}
            {!together && accounts.length === 0 && (
              <Link to="/einstellungen" className="mt-2 flex items-center gap-1 text-sm text-akzent">
                Erstes Konto anlegen
                <CaretRight size={12} />
              </Link>
            )}
          </section>

          {!together && accounts.length > 0 && (
            <div className="mb-2 flex gap-2 overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
              {accounts.map((account) => (
                <Link
                  key={account.id}
                  to="/einstellungen"
                  className="flex h-11 shrink-0 items-center gap-2 rounded-full bg-karte pl-3 pr-3.5"
                >
                  <Wallet size={15} className="shrink-0 text-akzent" />
                  <span className="whitespace-nowrap text-[13px] font-medium">{account.name}</span>
                  <span
                    className={`betragszahl whitespace-nowrap text-[13px] font-semibold ${
                      account.saldo < 0 ? 'text-schlecht' : 'text-textschwach'
                    }`}
                  >
                    {euro(account.saldo)}
                  </span>
                </Link>
              ))}
            </div>
          )}

          <div className={`mb-2 grid gap-2.5 ${budgetList.length > 0 ? 'grid-cols-2' : 'grid-cols-1'}`}>
            <div className="rounded-2xl bg-karte p-3">
              <div className="flex items-baseline justify-between gap-2">
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">Ausgaben</p>
                <p className="betragszahl text-[17px] font-semibold text-schlecht">{amountCompact(expenses)}</p>
              </div>
              <div className="mt-2 flex items-baseline justify-between gap-2">
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">Einnahmen</p>
                <p className="betragszahl text-[17px] font-semibold text-gut">{amountCompact(income)}</p>
              </div>
              {week !== null && (
                <div className="mt-1.5 flex items-baseline justify-between gap-2">
                  <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                    Diese Woche
                  </p>
                  <p className="betragszahl text-[13px] font-medium text-textschwach">{euro(week)}</p>
                </div>
              )}
            </div>
            {budgetList.length > 0 && (
              <div className="flex flex-col justify-center rounded-2xl bg-karte p-3">
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">Budget</p>
                <div className="relative mt-2 h-1.5 rounded-full bg-karte2">
                  <div
                    className={`h-full rounded-full ${budgetOver ? 'bg-schlecht' : 'bg-akzent'}`}
                    style={{
                      width: `${Math.min(100, (budgetActual / budgetScale) * 100)}%`,
                      transition: 'width 220ms cubic-bezier(0.22, 1, 0.36, 1)',
                    }}
                  />
                  <div
                    className="absolute top-0 h-full w-0.5 bg-text/60"
                    style={{ left: `calc(${Math.min(100, (budgetLimit / budgetScale) * 100)}% - 1px)` }}
                  />
                </div>
                <p className="betragszahl mt-1.5 text-[13px] font-medium text-textschwach">
                  {amountCompact(budgetActual)} von {amountCompact(budgetLimit)}
                  {budgetOver && (
                    <span className="font-semibold text-schlecht">
                      {' '}
                      · {euro(budgetActual - budgetLimit)} drüber
                    </span>
                  )}
                </p>
              </div>
            )}
          </div>

          {together && (remainingList.length > 0 || savingsGoals.length > 0) && (
            <section className="mb-2">
              {remainingList.length > 0 && (
                <div>
                  <div className="mb-1 flex items-baseline justify-between">
                    <h3 className="text-[13px] font-semibold">Übrig</h3>
                    {remainingList.length > VISIBLE_REMAINING && (
                      <button
                        onClick={() => setRemainingExpanded((expanded) => !expanded)}
                        className="flex items-center gap-0.5 text-xs font-medium text-akzent"
                      >
                        {remainingExpanded ? 'Weniger' : 'Alle anzeigen'}
                        <CaretDown size={11} className={remainingExpanded ? '' : 'rotate-180'} />
                      </button>
                    )}
                  </div>
                  <ul className="space-y-0.5">
                    {(remainingExpanded ? remainingList : remainingList.slice(0, VISIBLE_REMAINING)).map((item) => (
                      <RemainingRow key={item.name} name={item.name} rest={item.rest} />
                    ))}
                  </ul>
                </div>
              )}
              {savingsGoals.length > 0 && (
                <div className={remainingList.length > 0 ? 'mt-1.5' : undefined}>
                  <div className="mb-1 flex items-baseline justify-between">
                    <h3 className="text-[13px] font-semibold">Ziele</h3>
                    {savingsGoals.length > VISIBLE_GOALS && (
                      <button
                        onClick={() => setGoalsExpanded((expanded) => !expanded)}
                        className="flex items-center gap-0.5 text-xs font-medium text-akzent"
                      >
                        {goalsExpanded ? 'Weniger' : 'Alle anzeigen'}
                        <CaretDown size={11} className={goalsExpanded ? '' : 'rotate-180'} />
                      </button>
                    )}
                  </div>
                  <ul className="space-y-0.5">
                    {(goalsExpanded ? savingsGoals : savingsGoals.slice(0, VISIBLE_GOALS)).map((goal) => (
                      <GoalRow key={goal.id} goal={goal} />
                    ))}
                  </ul>
                </div>
              )}
            </section>
          )}

          {coaching !== null && coaching.empfehlungen.length > 0 && (
            <div className="mb-2 flex h-7 items-center gap-1.5 overflow-hidden">
              <Sparkle size={14} weight="fill" className="shrink-0 text-akzent" />
              <p className="truncate text-[13px] text-akzent">{coaching.empfehlungen[0].tipp}</p>
              {coaching.empfehlungen.length > 1 && (
                <span className="ml-auto shrink-0 text-[11px] text-textschwach">
                  +{coaching.empfehlungen.length - 1} weitere
                </span>
              )}
            </div>
          )}

          {history !== null && historyAverage !== null && (!together || !sharedLocal) && (
            <section className="mb-1.5 flex h-6 items-center gap-3">
              <p className="shrink-0 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                Verlauf
              </p>
              <div key={monat} className="flex items-center gap-2.5">
                {history.map((v, rank) => (
                  <TrendBar key={v.monat} entry={v} scale={historyScale} rank={rank} reduced={reduced} />
                ))}
              </div>
              <p className="betragszahl ml-auto shrink-0 text-[13px] font-medium text-textschwach">
                Ø {historyAverage >= 0 ? '+' : ''}
                {amountCompact(historyAverage)}
              </p>
            </section>
          )}

          {!together && categories.length > 0 && (
          <section className="mb-2">
              <div className="mb-1 flex items-baseline justify-between">
                <h3 className="text-[13px] font-semibold">Kategorien</h3>
                {categories.length > VISIBLE_CATEGORIES && (
                  <button
                    onClick={() => setCategoriesExpanded((expanded) => !expanded)}
                    className="flex items-center gap-0.5 text-xs font-medium text-akzent"
                  >
                    {categoriesExpanded ? 'Weniger' : 'Alle anzeigen'}
                    <CaretDown size={11} className={categoriesExpanded ? '' : 'rotate-180'} />
                  </button>
                )}
              </div>
              <div className="space-y-1">
                {categories.slice(0, VISIBLE_CATEGORIES).map(([name, sum], rank) => (
                  <MiniBar
                    key={name}
                    name={name}
                    sum={sum}
                    scale={categoryScale}
                    rank={rank}
                    onSelect={() => setCategoryDetail(name)}
                  />
                ))}
                <AnimatePresence initial={false}>
                  {categoriesExpanded && categories.length > VISIBLE_CATEGORIES && (
                    <motion.div
                      key="rest"
                      className="overflow-hidden"
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={reduced ? { duration: 0 } : { duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
                    >
                      <div className="space-y-2 pt-2">
                        {categories.slice(VISIBLE_CATEGORIES).map(([name, sum], i) => (
                          <MiniBar
                            key={name}
                            name={name}
                            sum={sum}
                            scale={categoryScale}
                            rank={i + VISIBLE_CATEGORIES}
                            onSelect={() => setCategoryDetail(name)}
                          />
                        ))}
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </section>
          )}

          <section>
            <div className="flex items-baseline justify-between">
              <h3 className="text-[13px] font-semibold">Buchungen</h3>
              {entries.length > VISIBLE_ENTRIES && (
                <button
                  onClick={() => setEntriesExpanded((expanded) => !expanded)}
                  className="text-xs font-medium text-akzent"
                >
                  {entriesExpanded ? 'Weniger' : 'Alle anzeigen'}
                </button>
              )}
            </div>
            {entries.length === 0 ? (
              <p className="py-2 text-sm text-textschwach">Noch nichts erfasst — tippe unten rechts auf „+".</p>
            ) : (
              <ul>
                {(entriesExpanded ? entries : entries.slice(0, VISIBLE_ENTRIES)).map((entry, i, list) => (
                  <li
                    key={entry.id}
                    className={`border-b border-strich ${i < list.length - 1 ? '' : 'border-b-0'}`}
                  >
                    <button
                      type="button"
                      onClick={() => onEditEntry(entry)}
                      aria-label={`${entry.kategorie} bearbeiten`}
                      className="flex w-full items-center justify-between gap-3 py-1 text-left"
                    >
                    <div className="flex min-w-0 items-center gap-3">
                      <CategoryIconTile name={entry.kategorie} />
                      <div className="min-w-0">
                        <p className="truncate text-[15px] font-semibold leading-tight text-text">
                          {entry.kategorie}
                        </p>
                        <p className="mt-0.5 flex flex-wrap items-center gap-1.5 text-xs leading-tight text-textschwach">
                          <span>{shortDate(entry.datum)}</span>
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
                          {entry.teile && entry.teile.length > 1 && (
                            <>
                              <span aria-hidden="true">·</span>
                              <span>{entry.teile.length} Teile</span>
                            </>
                          )}
                        </p>
                      </div>
                    </div>
                    <span
                      className={`betragszahl shrink-0 text-[15px] font-semibold ${
                        entry.betrag < 0 ? 'text-schlecht' : 'text-gut'
                      }`}
                    >
                      {entry.betrag > 0 ? '+' : ''}
                      {euro(entry.betrag)}
                    </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}

      <CategorySheet
        name={categoryDetail}
        entries={categoryEntries}
        budget={categoryDetail ? (data?.budgets[categoryDetail] ?? null) : null}
        recommendation={
          coaching?.empfehlungen.find((e) => e.kategorie === categoryDetail) ?? null
        }
        close={() => setCategoryDetail(null)}
      />
    </div>
  )
}
