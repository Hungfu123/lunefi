import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { ApiError, read, write, type SavingsGoal } from './types'
import { amountFromInput, amountCompact, todayIso } from './format'
import { useApp } from './AppContext'

function Dialog({
  title,
  close,
  children,
}: {
  title: string
  close: () => void
  children: React.ReactNode
}) {
  const reduced = useReducedMotion()
  return (
    <div className="fixed inset-0 z-30 flex items-center justify-center p-5">
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
        className="relative w-full max-w-xs rounded-2xl border border-strich bg-karte p-5"
        initial={{ opacity: 0, transform: 'scale(0.94)' }}
        animate={{ opacity: 1, transform: 'scale(1)' }}
        exit={{ opacity: 0, transform: 'scale(0.94)' }}
        transition={reduced ? { duration: 0 } : { type: 'spring', duration: 0.35, bounce: 0.2 }}
      >
        <h3 className="mb-4 font-semibold">{title}</h3>
        {children}
      </motion.div>
    </div>
  )
}

export function SavingsGoalsPage() {
  const { persons, activePerson } = useApp()
  const reduced = useReducedMotion()
  const [goals, setGoals] = useState<SavingsGoal[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [newOpen, setNewOpen] = useState(false)
  const [depositOpen, setDepositOpen] = useState<SavingsGoal | null>(null)
  const [celebrating, setCelebrating] = useState<Set<number>>(new Set())
  const seenReached = useRef<Map<number, boolean>>(new Map())

  const load = useCallback(() => {
    setLoading(true)
    read<SavingsGoal[]>('/api/sparziele')
      .then((list) => {
        setGoals(list)
        setError(false)
        setCelebrating((prev) => {
          let changed = false
          const next = new Set(prev)
          for (const goal of list) {
            const wasReached = seenReached.current.get(goal.id) ?? goal.erreicht
            if (goal.erreicht && !wasReached) {
              next.add(goal.id)
              changed = true
            }
            seenReached.current.set(goal.id, goal.erreicht)
          }
          return changed ? next : prev
        })
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [])

  useEffect(load, [load])

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold">Sparziele</h2>
        <button
          onClick={() => setNewOpen(true)}
          className="flex min-h-11 items-center rounded-xl bg-akzentstark px-4 text-sm font-semibold text-akzenttext active:scale-[0.98]"
        >
          Neues Ziel
        </button>
      </div>

      {loading && <div className="h-24 animate-pulse rounded-2xl bg-karte" />}

      {error && !loading && <p className="text-schlecht">Die Sparziele konnten nicht geladen werden.</p>}

      {!loading && !error && goals.length === 0 && (
        <p className="text-sm text-textschwach">Noch kein Sparziel — legt gemeinsam eines an.</p>
      )}

      <ul className="space-y-3">
        {goals.map((goal) => {
          const progress = Math.min(100, (goal.eingezahlt / goal.zielbetrag) * 100)
          const rest = Math.max(0, goal.zielbetrag - goal.eingezahlt)
          return (
            <li key={goal.id} className="rounded-2xl bg-karte p-4">
              <div className="flex items-center justify-between gap-2">
                <span className="min-w-0 truncate font-medium">{goal.name}</span>
                {goal.erreicht ? (
                  <motion.span
                    className="shrink-0 rounded-full bg-gut/15 px-2 py-0.5 text-xs text-gut"
                    initial={celebrating.has(goal.id) ? { scale: 1.3 } : false}
                    animate={{ scale: 1 }}
                    transition={reduced ? { duration: 0 } : { type: 'spring', duration: 0.5, bounce: 0.45 }}
                  >
                    {goal.name} ist finanziert!
                  </motion.span>
                ) : (
                  <button
                    onClick={() => setDepositOpen(goal)}
                    className="flex min-h-11 shrink-0 items-center text-sm font-semibold text-akzent active:scale-[0.98]"
                  >
                    Gespart
                  </button>
                )}
              </div>
              <div className="mt-3 h-2.5 overflow-hidden rounded-full bg-karte2">
                <div
                  className={`h-full rounded-full ${goal.erreicht ? 'bg-gut' : 'bg-akzent'}`}
                  style={{ width: `${progress}%` }}
                />
              </div>
              <p className="betragszahl mt-1.5 text-xs text-textschwach">
                {amountCompact(goal.eingezahlt)} · {Math.floor(progress)}%
                {goal.erreicht ? '' : ` · noch ${amountCompact(rest)}`}
              </p>
            </li>
          )
        })}
      </ul>

      {!loading && !error && goals.length > 0 && (
        <p className="mt-4 text-[12px] text-textschwach">
          Zählt euren Fortschritt. Kein echtes Konto — das Geld bleibt, wo es ist.
        </p>
      )}

      <AnimatePresence>
        {newOpen && (
        <NewGoalDialog
          close={() => setNewOpen(false)}
          onCreated={() => {
            setNewOpen(false)
            load()
          }}
        />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {depositOpen && (
        <DepositDialog
          goal={depositOpen}
          persons={persons.map((p) => p.name)}
          defaultPerson={activePerson}
          close={() => setDepositOpen(null)}
          onDeposited={() => {
            setDepositOpen(null)
            load()
          }}
        />
        )}
      </AnimatePresence>
    </div>
  )
}

function NewGoalDialog({ close, onCreated }: { close: () => void; onCreated: () => void }) {
  const [name, setName] = useState('')
  const [targetAmount, setTargetAmount] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const amount = amountFromInput(targetAmount)
    if (!name.trim() || amount === null || amount <= 0) {
      setError('Name und Zielbetrag (über 0) fehlen.')
      return
    }
    setBusy(true)
    try {
      await write('/api/sparziele', { name: name.trim(), zielbetrag: amount })
      onCreated()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Das hat nicht geklappt.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog title="Neues Sparziel" close={close}>
      <form onSubmit={submit} className="flex flex-col gap-3">
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="z. B. Urlaub"
          autoFocus
          className="rounded-xl bg-karte2 px-4 py-3 outline-none ring-strich focus:ring-2"
        />
        <input
          value={targetAmount}
          onChange={(e) => setTargetAmount(e.target.value)}
          inputMode="decimal"
          placeholder="2000,00"
          className="rounded-xl bg-karte2 px-4 py-3 outline-none ring-strich focus:ring-2"
        />
        {error && <p className="text-sm text-schlecht">{error}</p>}
        <button
          type="submit"
          disabled={busy}
          className="rounded-xl bg-akzentstark py-3 font-semibold text-akzenttext disabled:opacity-50"
        >
          Anlegen
        </button>
      </form>
    </Dialog>
  )
}

function DepositDialog({
  goal,
  persons,
  defaultPerson,
  close,
  onDeposited,
}: {
  goal: SavingsGoal
  persons: string[]
  defaultPerson: string | null
  close: () => void
  onDeposited: () => void
}) {
  const [amount, setAmount] = useState('')
  const [person, setPerson] = useState(defaultPerson ?? persons[0] ?? '')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const value = amountFromInput(amount)
    if (value === null || value <= 0) {
      setError('Bitte einen Betrag über 0 eintragen.')
      return
    }
    setBusy(true)
    try {
      await write(`/api/sparziele/${goal.id}/einzahlungen`, { betrag: value, person, datum: todayIso() })
      onDeposited()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Das hat nicht geklappt.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog title={`Gespart: ${goal.name}`} close={close}>
      <form onSubmit={submit} className="flex flex-col gap-3">
        {goal.eingezahlt === 0 && (
          <p className="text-sm text-textschwach">
            Trag hier ein, wie viel ihr beiseite gelegt habt.
          </p>
        )}
        <input
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          inputMode="decimal"
          placeholder="0,00"
          autoFocus
          className="rounded-xl bg-karte2 px-4 py-3 outline-none ring-strich focus:ring-2"
        />
        <div className="flex gap-2">
          {persons.map((p) => (
            <button
              type="button"
              key={p}
              onClick={() => setPerson(p)}
              className={`flex min-h-11 flex-1 items-center justify-center rounded-xl px-3 text-sm ${
                person === p ? 'bg-akzentstark text-akzenttext' : 'bg-karte2 text-textschwach'
              }`}
            >
              {p}
            </button>
          ))}
        </div>
        {error && <p className="text-sm text-schlecht">{error}</p>}
        <button
          type="submit"
          disabled={busy}
          className="rounded-xl bg-akzentstark py-3 font-semibold text-akzenttext disabled:opacity-50"
        >
          Als gespart vermerken
        </button>
      </form>
    </Dialog>
  )
}
