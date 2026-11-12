import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { CaretDown, Moon, Question, Sun } from '@phosphor-icons/react'
import { ApiError, read, write, type CategoryInfo, type Account } from './types'
import { CategoryIconTile } from './categoryIcon'
import { ATTITUDE_LABEL, AttitudeSelector } from './AttitudeSelector'
import { HelpModal } from './HelpModal'
import { amountFromInput, euro, monthAsString } from './format'
import { useApp } from './AppContext'

export function SettingsPage() {
  const { persons, activePerson } = useApp()
  const reduced = useReducedMotion()
  const [categories, setCategories] = useState<CategoryInfo[]>([])
  const [accounts, setAccounts] = useState<Account[]>([])
  const [accountName, setAccountName] = useState('')
  const [accountBalance, setAccountBalance] = useState('')
  const [expandedCategory, setExpandedCategory] = useState<string | null>(null)
  const [theme, setTheme] = useState<'mond' | 'sonne'>(
    () => (document.documentElement.dataset.theme === 'sonne' ? 'sonne' : 'mond'),
  )

  const applyTheme = (next: 'mond' | 'sonne') => {
    setTheme(next)
    document.documentElement.dataset.theme = next
    localStorage.setItem('lunefi:theme', next)
  }
  const [budgets, setBudgets] = useState<Record<string, string>>({})
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [helpOpen, setHelpOpen] = useState(false)

  const load = useCallback(() => {
    read<CategoryInfo[]>('/api/kategorien')
      .then((list) => {
        setCategories(list)
        const initial: Record<string, string> = {}
        list.forEach((k) => (initial[k.name] = ''))
        setBudgets(initial)
      })
      .catch(() => setCategories([]))
    read<Account[]>(
      activePerson ? `/api/konten?person=${encodeURIComponent(activePerson)}` : '/api/konten',
    )
      .then(setAccounts)
      .catch(() => setAccounts([]))
  }, [activePerson])

  useEffect(load, [load])

  const setSplit = async (kategorie: string, art: 'HALBHALB' | 'ANTEILE') => {
    try {
      if (art === 'HALBHALB') {
        await write(`/api/kategorien/${encodeURIComponent(kategorie)}/split`, { art }, 'PUT')
      } else {
        const names = persons.map((p) => p.name)
        const shares: Record<string, number> = {}
        names.forEach((n) => (shares[n] = Math.floor(100 / names.length)))
        const rest = 100 - Math.floor(100 / names.length) * names.length
        if (names[0]) shares[names[0]] += rest
        await write(`/api/kategorien/${encodeURIComponent(kategorie)}/split`, { art, anteile: shares }, 'PUT')
      }
      setMessage(`Split für „${kategorie}" gespeichert.`)
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Speichern fehlgeschlagen.')
    }
  }

  const setBudget = async (kategorie: string, e: FormEvent) => {
    e.preventDefault()
    const value = amountFromInput(budgets[kategorie] ?? '')
    if (value === null || value <= 0) {
      setError('Bitte ein Limit über 0 eintragen.')
      return
    }
    try {
      await write('/api/budgets', { monat: monthAsString(new Date()), kategorie, limit: value }, 'PUT')
      setMessage(`Budget für „${kategorie}" gespeichert.`)
      setBudgets((prev) => ({ ...prev, [kategorie]: '' }))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Speichern fehlgeschlagen.')
    }
  }

  const createAccount = async (e: FormEvent) => {
    e.preventDefault()
    const saldo = amountFromInput(accountBalance)
    if (!accountName.trim() || saldo === null || !activePerson) {
      setError('Name und Startsaldo eintragen.')
      return
    }
    try {
      await write('/api/konten', { name: accountName.trim(), startSaldo: saldo, person: activePerson })
      setMessage(`Konto „${accountName.trim()}" angelegt.`)
      setAccountName('')
      setAccountBalance('')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Konto anlegen fehlgeschlagen.')
    }
  }

  return (
    <div>
      <h2 className="mb-4 text-[15px] font-semibold">Einstellungen</h2>

      <section className="mb-6">
        <h3 className="mb-2 text-[13px] font-semibold">Erscheinungsbild</h3>
        <div className="flex gap-2">
          <button
            onClick={() => applyTheme('mond')}
            className={`flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl px-3 text-sm font-medium transition-colors ${
              theme === 'mond' ? 'bg-akzentstark text-akzenttext' : 'bg-karte text-textschwach'
            }`}
          >
            <Moon size={18} weight={theme === 'mond' ? 'fill' : 'regular'} />
            Dunkel
          </button>
          <button
            onClick={() => applyTheme('sonne')}
            className={`flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl px-3 text-sm font-medium transition-colors ${
              theme === 'sonne' ? 'bg-akzentstark text-akzenttext' : 'bg-karte text-textschwach'
            }`}
          >
            <Sun size={18} weight={theme === 'sonne' ? 'fill' : 'regular'} />
            Hell
          </button>
        </div>
      </section>

      <section className="mb-6">
        <h3 className="mb-2 text-[13px] font-semibold">Hilfe</h3>
        <button
          onClick={() => setHelpOpen(true)}
          className="flex min-h-11 w-full items-center gap-3 rounded-xl bg-karte px-4 text-[15px] font-medium text-text"
        >
          <Question size={18} className="shrink-0" />
          So funktioniert LuneFi
        </button>
      </section>

      <section className="mb-6">
        <h3 className="mb-2 text-[13px] font-semibold">Konten</h3>
        {accounts.length > 0 && (
          <ul className="mb-3 space-y-2">
            {accounts.map((k) => (
              <li key={k.id} className="flex items-center justify-between rounded-xl bg-karte px-4 py-3">
                <span className="truncate text-[13px] font-medium">
                  {k.name} <span className="text-textschwach">· {k.person}</span>
                </span>
                <span className="betragszahl text-[13px] font-semibold">{euro(k.saldo)}</span>
              </li>
            ))}
          </ul>
        )}
        <form onSubmit={createAccount} className="flex items-center gap-2">
          <input
            value={accountName}
            onChange={(e) => setAccountName(e.target.value)}
            placeholder="z. B. Girokonto"
            className="min-w-0 flex-1 rounded-xl bg-karte px-3 py-2.5 outline-none ring-strich focus:ring-2"
          />
          <input
            value={accountBalance}
            onChange={(e) => setAccountBalance(e.target.value)}
            inputMode="decimal"
            placeholder="Startsaldo"
            className="w-28 shrink-0 rounded-xl bg-karte px-3 py-2.5 outline-none ring-strich focus:ring-2"
          />
          <button type="submit" className="flex min-h-11 shrink-0 items-center rounded-xl bg-akzentstark px-3 text-sm font-semibold text-akzenttext">
            Anlegen
          </button>
        </form>
      </section>

      <section className="mb-6">
        <h3 className="mb-2 text-[13px] font-semibold">Kategorien</h3>
        <p className="mb-2 text-[12px] text-textschwach">Sichtbar für euch beide.</p>
        {categories.length === 0 && (
          <p className="text-sm text-textschwach">Noch keine Kategorien.</p>
        )}
        <ul className="space-y-1">
          {categories.map((k) => {
            const expanded = expandedCategory === k.name
            return (
              <li key={k.name} className="overflow-hidden rounded-xl bg-karte px-3.5">
                <button
                  type="button"
                  aria-expanded={expanded}
                  onClick={() => setExpandedCategory(expanded ? null : k.name)}
                  className="flex h-8 w-full items-center gap-3 text-left"
                >
                  <CategoryIconTile name={k.name} groesse={22} iconGroesse={13} />
                  <span className="min-w-0 flex-1 truncate text-[15px] font-semibold text-text">
                    {k.name}
                  </span>
                  <span className="flex shrink-0 items-center gap-1">
                    <span className="rounded-full bg-karte2 px-2 py-0.5 text-[11px] text-textschwach">
                      {k.split.art === 'ANTEILE' ? 'Anteile' : '50/50'}
                    </span>
                    {k.haltung !== 'FLEXIBEL' && (
                      <span className="rounded-full bg-akzent/15 px-2 py-0.5 text-[11px] text-akzent">
                        {ATTITUDE_LABEL[k.haltung]}
                      </span>
                    )}
                    <CaretDown
                      size={12}
                      aria-hidden="true"
                      className={`shrink-0 text-textschwach transition-transform ${expanded ? '' : 'rotate-180'}`}
                    />
                  </span>
                </button>
                <AnimatePresence initial={false}>
                  {expanded && (
                    <motion.div
                      className="overflow-hidden"
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={reduced ? { duration: 0 } : { duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
                    >
                      <div className="space-y-2.5 px-0.5 pb-3 pt-1">
                        <div>
                          <p className="mb-1 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                            Aufteilung
                          </p>
                          <div className="flex gap-1.5">
                            <button
                              onClick={() => setSplit(k.name, 'HALBHALB')}
                              aria-pressed={k.split.art !== 'ANTEILE'}
                              className={`flex min-h-8 flex-1 items-center justify-center rounded-lg text-xs ${
                                k.split.art !== 'ANTEILE'
                                  ? 'bg-akzentstark text-akzenttext'
                                  : 'bg-karte2 text-textschwach'
                              }`}
                            >
                              50/50
                            </button>
                            <button
                              onClick={() => setSplit(k.name, 'ANTEILE')}
                              aria-pressed={k.split.art === 'ANTEILE'}
                              className={`flex min-h-8 flex-1 items-center justify-center rounded-lg text-xs ${
                                k.split.art === 'ANTEILE'
                                  ? 'bg-akzentstark text-akzenttext'
                                  : 'bg-karte2 text-textschwach'
                              }`}
                            >
                              Anteile
                            </button>
                          </div>
                        </div>
                        <div>
                          <p className="mb-1 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                            Haltung
                          </p>
                          <AttitudeSelector
                            kategorie={k.name}
                            haltung={k.haltung}
                            onAttitude={(value) =>
                              setCategories((prev) =>
                                prev.map((x) => (x.name === k.name ? { ...x, haltung: value } : x)),
                              )
                            }
                          />
                        </div>
                        <div>
                          <p className="mb-1 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                            Budget-Limit
                          </p>
                          <form onSubmit={(e) => setBudget(k.name, e)} className="flex items-center gap-2">
                            <input
                              value={budgets[k.name] ?? ''}
                              onChange={(e) => setBudgets((prev) => ({ ...prev, [k.name]: e.target.value }))}
                              inputMode="decimal"
                              placeholder="Limit in Euro"
                              className="min-w-0 flex-1 rounded-xl bg-karte2 px-3 py-2.5 outline-none ring-strich focus:ring-2"
                            />
                            <button
                              type="submit"
                              className="flex min-h-11 shrink-0 items-center rounded-xl bg-akzentstark px-3 text-sm font-semibold text-akzenttext"
                            >
                              Speichern
                            </button>
                          </form>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </li>
            )
          })}
        </ul>
      </section>

      {message && <p className="mt-4 text-sm text-gut">{message}</p>}
      {error && <p className="mt-4 text-sm text-schlecht">{error}</p>}

      <p className="mt-8 text-center text-xs text-textschwach">
        LuneFi · {persons.map((p) => p.name).join(' & ')}
      </p>

      <HelpModal open={helpOpen} close={() => setHelpOpen(false)} />
    </div>
  )
}
