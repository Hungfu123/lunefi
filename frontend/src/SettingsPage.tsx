import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { createPortal } from 'react-dom'
import { Bank, CaretDown, Moon, PencilSimple, Question, Sun } from '@phosphor-icons/react'
import { ApiError, read, write, type CategoryInfo, type Account } from './types'
import { CategoryIconTile } from './categoryIcon'
import { ATTITUDE_HINT, ATTITUDE_LABEL, AttitudeSelector } from './AttitudeSelector'
import { HelpModal } from './HelpModal'
import { amountFromInput, euro, monthAsString } from './format'
import { useApp } from './AppContext'

const BANKEN = [
  'Commerzbank',
  'Sparkasse',
  'Volks- und Raiffeisenbank',
  'Deutsche Bank',
  'Postbank',
  'HypoVereinsbank',
  'ING',
  'DKB',
  'N26',
  'Consorsbank',
  'TARGOBANK',
  'Santander',
  'Sparda-Bank',
  'PSD Bank',
]

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
  const [kontoHinweis, setKontoHinweis] = useState<{ text: string; gut: boolean } | null>(null)
  const [kategorieHinweis, setKategorieHinweis] = useState<{ text: string; gut: boolean } | null>(null)
  const [kontoOpen, setKontoOpen] = useState(false)
  const [pickedBank, setPickedBank] = useState<string | null>(null)
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
      setKategorieHinweis({ text: `Aufteilung für „${kategorie}" gespeichert.`, gut: true })
      load()
    } catch (err) {
      setKategorieHinweis({
        text: err instanceof ApiError ? err.message : 'Speichern fehlgeschlagen.',
        gut: false,
      })
    }
  }

  const setBudget = async (kategorie: string, e: FormEvent) => {
    e.preventDefault()
    const value = amountFromInput(budgets[kategorie] ?? '')
    if (value === null || value <= 0) {
      setKategorieHinweis({ text: 'Bitte ein Limit über 0 eintragen.', gut: false })
      return
    }
    try {
      await write('/api/budgets', { monat: monthAsString(new Date()), kategorie, limit: value }, 'PUT')
      setKategorieHinweis({ text: `Budget für „${kategorie}" gespeichert.`, gut: true })
      setBudgets((prev) => ({ ...prev, [kategorie]: '' }))
    } catch (err) {
      setKategorieHinweis({
        text: err instanceof ApiError ? err.message : 'Speichern fehlgeschlagen.',
        gut: false,
      })
    }
  }

  const createAccount = async (e: FormEvent) => {
    e.preventDefault()
    const name = accountName.trim()
    const saldo = amountFromInput(accountBalance)
    if (!name || saldo === null || !activePerson) {
      setKontoHinweis({ text: 'Name und Startsaldo eintragen.', gut: false })
      return
    }
    if (accounts.some((konto) => konto.name.toLowerCase() === name.toLowerCase())) {
      setKontoHinweis({ text: `Ein Konto namens „${name}" gibt es schon.`, gut: false })
      return
    }
    try {
      await write('/api/konten', { name, startSaldo: saldo, person: activePerson })
      setKontoHinweis({ text: `Konto „${name}" angelegt.`, gut: true })
      setKontoOpen(false)
      setAccountName('')
      setAccountBalance('')
      load()
    } catch (err) {
      setKontoHinweis({
        text: err instanceof ApiError ? err.message : 'Konto anlegen fehlgeschlagen.',
        gut: false,
      })
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
        <div className="mb-2 flex items-center justify-between">
          <h3 className="text-[13px] font-semibold">Konten</h3>
          <button
            onClick={() => {
              setKontoOpen(true)
              setPickedBank(null)
              setKontoHinweis(null)
            }}
            className="flex min-h-11 items-center rounded-xl bg-akzentstark px-4 text-sm font-semibold text-akzenttext active:scale-[0.98]"
          >
            Konto anlegen
          </button>
        </div>
        {accounts.length > 0 && (
          <ul className="space-y-2">
            {accounts.map((k) => (
              <li key={k.id} className="flex items-center justify-between rounded-xl bg-karte px-4 py-3">
                <span className="flex min-w-0 items-center gap-2.5">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-karte2 text-akzent">
                    <Bank size={15} weight="fill" aria-hidden="true" />
                  </span>
                  <span className="truncate text-[13px] font-medium">
                    {k.name} <span className="text-textschwach">· {k.person}</span>
                  </span>
                </span>
                <span className="betragszahl text-[13px] font-semibold">{euro(k.saldo)}</span>
              </li>
            ))}
          </ul>
        )}
        {accounts.length === 0 && (
          <p className="text-sm text-textschwach">Noch kein Konto.</p>
        )}
        {kontoHinweis && !kontoOpen && (
          <p className={`mt-1.5 text-[13px] ${kontoHinweis.gut ? 'text-gut' : 'text-schlecht'}`}>
            {kontoHinweis.text}
          </p>
        )}
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
                    {k.split.art === 'ANTEILE' && k.split.anteile && (
                      <span className="rounded-full bg-karte2 px-2 py-0.5 text-[11px] text-textschwach">
                        {Object.values(k.split.anteile).join('/')}
                      </span>
                    )}
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
                              Eigene Aufteilung
                            </button>
                          </div>
                        </div>
                        <div>
                          <p className="mb-1 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                            Einstellung
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
                          <p className="mt-1 text-[11px] text-textschwach">{ATTITUDE_HINT[k.haltung]}</p>
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
        {kategorieHinweis && (
          <p className={`mt-1.5 text-[13px] ${kategorieHinweis.gut ? 'text-gut' : 'text-schlecht'}`}>
            {kategorieHinweis.text}
          </p>
        )}
      </section>

      <p className="mt-8 text-center text-xs text-textschwach">
        LuneFi · {persons.map((p) => p.name).join(' & ')}
      </p>

      {createPortal(
        <AnimatePresence>
          {kontoOpen && (
            <div className="fixed inset-0 z-30 flex flex-col justify-end">
              <motion.button
                aria-label="Schließen"
                className="absolute inset-0 bg-black/60"
                onClick={() => setKontoOpen(false)}
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
                    setKontoOpen(false)
                  }
                }}
              >
                <div className="mx-auto mb-2.5 h-1 w-10 rounded-full bg-strich" />
                {pickedBank === null ? (
                  <>
                    <h3 className="mb-2.5 text-lg font-semibold">Bei welcher Bank?</h3>
                    <div className="grid grid-cols-2 gap-1.5">
                      {BANKEN.map((bank) => (
                        <button
                          type="button"
                          key={bank}
                          onClick={() => {
                            setPickedBank(bank)
                            setAccountName(bank)
                            setAccountBalance('')
                            setKontoHinweis(null)
                          }}
                          className={`flex min-h-11 items-center gap-2 rounded-xl bg-karte2 px-3 text-left transition-colors active:bg-karte2/60 ${
                            bank.length > 16 ? 'col-span-2' : ''
                          }`}
                        >
                          <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-karte text-akzent">
                            <Bank size={13} weight="fill" aria-hidden="true" />
                          </span>
                          <span className="truncate text-[13px] font-medium">{bank}</span>
                        </button>
                      ))}
                      <button
                        type="button"
                        onClick={() => {
                          setPickedBank('Andere')
                          setAccountName('')
                          setAccountBalance('')
                          setKontoHinweis(null)
                        }}
                        className="flex min-h-11 items-center gap-2 rounded-xl bg-karte2 px-3 text-left transition-colors active:bg-karte2/60"
                      >
                        <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-karte text-akzent">
                          <PencilSimple size={13} aria-hidden="true" />
                        </span>
                        <span className="truncate text-[13px] font-medium">Andere Bank</span>
                      </button>
                    </div>
                    <p className="mt-2.5 text-[11px] leading-snug text-textschwach">
                      Die Bankverbindung kommt später dazu — mit dem Bank-Import.
                    </p>
                  </>
                ) : (
                  <>
                    <button
                      type="button"
                      onClick={() => setPickedBank(null)}
                      className="mb-2 flex items-center gap-1 text-[12px] font-medium text-textschwach"
                    >
                      ‹ {pickedBank === 'Andere' ? 'Andere Bank' : pickedBank}
                    </button>
                    <h3 className="mb-2.5 text-lg font-semibold">Konto anlegen</h3>
                    <form onSubmit={createAccount} className="space-y-2">
                      <input
                        value={accountName}
                        onChange={(e) => {
                          setAccountName(e.target.value)
                          setKontoHinweis(null)
                        }}
                        placeholder="z. B. Girokonto"
                        aria-label="Kontoname"
                        className="w-full rounded-xl bg-karte2 px-4 py-2.5 outline-none ring-strich focus:ring-2"
                      />
                      <input
                        value={accountBalance}
                        onChange={(e) => {
                          setAccountBalance(e.target.value)
                          setKontoHinweis(null)
                        }}
                        inputMode="decimal"
                        placeholder="Startsaldo"
                        aria-label="Startsaldo"
                        className="w-full rounded-xl bg-karte2 px-4 py-2.5 outline-none ring-strich focus:ring-2"
                      />
                      <button
                        type="submit"
                        className="flex min-h-11 w-full items-center justify-center rounded-xl bg-akzentstark text-sm font-semibold text-akzenttext active:scale-[0.98]"
                      >
                        Anlegen
                      </button>
                    </form>
                    {kontoHinweis && (
                      <p className={`mt-1.5 text-[13px] ${kontoHinweis.gut ? 'text-gut' : 'text-schlecht'}`}>
                        {kontoHinweis.text}
                      </p>
                    )}
                  </>
                )}
              </motion.div>
            </div>
          )}
        </AnimatePresence>,
        document.body,
      )}

      <HelpModal open={helpOpen} close={() => setHelpOpen(false)} />
    </div>
  )
}
