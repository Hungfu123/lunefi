import { useEffect, useState, type FormEvent } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { ApiError, read, remove, write, type Entry, type NewEntry, type CategoryInfo, type Account } from './types'
import { amountFromInput, euro, todayIso } from './format'
import {
  CategoryIconTile,
  PICKER_ICONS,
  iconFromName,
  iconNameFrom,
  categoryIconName,
  setCategoryIcon,
} from './categoryIcon'
import { PREDEFINED_CATEGORIES } from './categories'
import { useApp } from './AppContext'

export function EntrySheet({
  open,
  close,
  onCaptured,
  entry,
}: {
  open: boolean
  close: () => void
  onCaptured: () => void
  entry?: Entry
}) {
  const { persons, activePerson } = useApp()
  const reduced = useReducedMotion()
  const [art, setArt] = useState<'ausgabe' | 'einnahme'>('ausgabe')
  const [amount, setAmount] = useState('')
  const [category, setCategory] = useState('')
  const [iconChoice, setIconChoice] = useState<string | null>(null)
  const [categories, setCategories] = useState<CategoryInfo[]>([])
  const [accounts, setAccounts] = useState<Account[]>([])
  const [account, setAccount] = useState<string | null>(null)
  const [datum, setDatum] = useState(todayIso())
  const [shared, setShared] = useState(false)
  const [person, setPerson] = useState<string | null>(null)
  const [payer, setPayer] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)

  useEffect(() => {
    if (!open) return
    read<CategoryInfo[]>('/api/kategorien')
      .then(setCategories)
      .catch(() => setCategories([]))
    read<Account[]>('/api/konten')
      .then(setAccounts)
      .catch(() => setAccounts([]))
    if (entry) {
      setArt(entry.betrag < 0 ? 'ausgabe' : 'einnahme')
      setAmount(
        new Intl.NumberFormat('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(
          Math.abs(entry.betrag),
        ),
      )
      setCategory(entry.kategorie)
      setIconChoice(null)
      setDatum(entry.datum)
      setShared(entry.gemeinsam)
      setPerson(entry.person ?? activePerson)
      setPayer(entry.zahler ?? activePerson)
      setAccount(entry.konto)
    } else {
      setArt('ausgabe')
      setAmount('')
      setCategory('')
      setIconChoice(null)
      setDatum(todayIso())
      setShared(false)
      setPerson(activePerson)
      setPayer(activePerson)
      setAccount(null)
    }
    setConfirmDelete(false)
    setError(null)
  }, [open, activePerson, entry])

  const responsiblePerson = shared ? payer : person
  const ownAccounts = accounts.filter((k) => k.person === responsiblePerson)
  const existingCategories = new Set(categories.map((k) => k.name.toLowerCase()))
  const suggestions = PREDEFINED_CATEGORIES.filter(
    (v) => !existingCategories.has(v.name.toLowerCase()),
  )

  const amountValue = amountFromInput(amount)
  const activeIconName = category.trim() === '' ? null : (iconChoice ?? categoryIconName(category))
  const ActiveIcon = activeIconName === null ? null : iconFromName(activeIconName)
  const splitInfo = categories.find((k) => k.name === category)?.split
  const sharesText =
    splitInfo?.art === 'ANTEILE' && splitInfo.anteile
      ? Object.values(splitInfo.anteile).join('/')
      : null
  const debtor = persons.map((p) => p.name).find((n) => n !== payer) ?? null
  const debtorShare =
    splitInfo?.art === 'ANTEILE' && splitInfo.anteile && debtor && splitInfo.anteile[debtor] != null
      ? splitInfo.anteile[debtor] / 100
      : 0.5
  const debtAmount =
    shared && amountValue !== null && amountValue !== 0 && payer && debtor
      ? Math.floor(amountValue * debtorShare * 100) / 100
      : null

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const value = amountFromInput(amount)
    if (value === null || value === 0) {
      setError('Bitte einen Betrag ungleich 0 eintragen.')
      return
    }
    const directed = art === 'ausgabe' ? -value : value
    if (!category.trim()) {
      setError('Bitte eine Kategorie wählen oder eingeben.')
      return
    }
    if (iconChoice) {
      setCategoryIcon(category.trim(), iconChoice)
    }
    const body: NewEntry = {
      datum,
      betrag: directed,
      kategorie: category.trim(),
    }
    if (shared) {
      body.gemeinsam = true
      body.zahler = payer ?? undefined
    } else {
      body.person = person ?? undefined
    }
    if (account) {
      body.konto = account
    }
    setBusy(true)
    try {
      if (entry) {
        await write<Entry>(`/api/eintraege/${entry.id}`, body, 'PUT')
      } else {
        await write<Entry>('/api/eintraege', body)
      }
      close()
      onCaptured()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Das hat nicht geklappt.')
    } finally {
      setBusy(false)
    }
  }

  const deleteEntry = async () => {
    if (!entry) return
    setBusy(true)
    try {
      await remove(`/api/eintraege/${entry.id}`)
      close()
      onCaptured()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Das hat nicht geklappt.')
    } finally {
      setBusy(false)
    }
  }

  const soft = reduced ? { duration: 0 } : { duration: 0.22, ease: [0.22, 1, 0.36, 1] as const }

  return (
    <AnimatePresence>
      {open && (
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
          <motion.form
            onSubmit={submit}
            className="relative mx-auto max-h-[calc(100dvh-16px)] w-full max-w-[560px] overflow-y-auto overscroll-contain rounded-t-3xl border-t border-strich bg-karte px-5 pb-5 pt-4"
            initial={{ transform: 'translateY(100%)' }}
            animate={{ transform: 'translateY(0%)' }}
            exit={{ transform: 'translateY(100%)' }}
            transition={
              reduced ? { duration: 0 } : { type: 'spring', duration: 0.5, bounce: 0.2 }
            }
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

            {entry && <h3 className="mb-2.5 text-lg font-semibold">Eintrag bearbeiten</h3>}

            <div className="mb-2.5 flex gap-2">
              <div className="flex flex-1 gap-2 rounded-xl bg-karte2 p-1">
                <button
                  type="button"
                  onClick={() => setArt('ausgabe')}
                  className={`flex min-h-11 flex-1 items-center justify-center rounded-lg text-sm font-semibold transition-colors ${
                    art === 'ausgabe' ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
                  }`}
                >
                  Ausgabe
                </button>
                <button
                  type="button"
                  onClick={() => setArt('einnahme')}
                  className={`flex min-h-11 flex-1 items-center justify-center rounded-lg text-sm font-semibold transition-colors ${
                    art === 'einnahme' ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
                  }`}
                >
                  Einnahme
                </button>
              </div>
              <input
                aria-label="Betrag"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                inputMode="decimal"
                placeholder="0,00"
                className="betragszahl w-32 shrink-0 rounded-xl bg-karte2 px-3 text-right text-xl font-semibold outline-none ring-strich focus:ring-2"
              />
            </div>

            <label className="mb-1 block text-xs text-textschwach">Kategorie</label>
            {categories.length > 0 && (
              <div className="mb-1.5 flex flex-wrap gap-2">
                {categories.map((k) => (
                  <button
                    type="button"
                    key={k.name}
                    onClick={() => setCategory(k.name)}
                    className={`flex min-h-11 items-center gap-2 rounded-full py-1 pl-1 pr-3 text-sm transition-colors ${
                      category === k.name ? 'bg-akzentstark text-akzenttext' : 'bg-karte2 text-textschwach'
                    }`}
                  >
                    <CategoryIconTile
                      name={k.name}
                      groesse={24}
                      iconGroesse={14}
                      iconKlasse={category === k.name ? 'text-akzenttext' : undefined}
                    />
                    {k.name}
                  </button>
                ))}
              </div>
            )}
            <div className="relative mb-1">
              <input
                value={category}
                onChange={(e) => {
                  setCategory(e.target.value)
                  setIconChoice(null)
                }}
                placeholder="Neue Kategorie"
                className="w-full rounded-xl bg-karte2 px-4 py-2.5 pr-11 outline-none ring-strich focus:ring-2"
              />
              {ActiveIcon && (
                <span
                  aria-hidden="true"
                  className="pointer-events-none absolute right-3 top-1/2 flex -translate-y-1/2 items-center text-text"
                >
                  <ActiveIcon size={17} />
                </span>
              )}
            </div>
            {ActiveIcon && activeIconName !== null && (
              <p className="mb-2 flex items-center gap-1.5 text-xs text-textschwach">
                Gewähltes Icon:
                <ActiveIcon size={13} className="shrink-0 text-akzent" aria-hidden="true" />
                <span className="font-medium text-text">{activeIconName}</span>
              </p>
            )}

            <AnimatePresence initial={false}>
              {category.trim() !== '' ? (
                <motion.div
                  key="picker"
                  className="mb-2.5"
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={soft}
                >
                  <div className="flex gap-2 overflow-x-auto overscroll-x-contain pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
                    {PICKER_ICONS.map((Icon) => {
                      const name = iconNameFrom(Icon)
                      const selected = name === (iconChoice ?? categoryIconName(category))
                      return (
                        <button
                          type="button"
                          key={name}
                          aria-label={name}
                          onClick={() => setIconChoice(iconChoice === name ? null : name)}
                          className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full transition-colors ${
                            selected
                              ? 'bg-akzentstark/20 text-akzent ring-2 ring-akzent'
                              : 'bg-karte2 text-textschwach'
                          }`}
                        >
                          <Icon size={17} weight={selected ? 'fill' : 'regular'} />
                        </button>
                      )
                    })}
                  </div>
                </motion.div>
              ) : suggestions.length > 0 ? (
                <motion.div
                  key="vorschlaege"
                  className="mb-2.5"
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={soft}
                >
                  <p className="mb-1 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                    Vorschläge
                  </p>
                  <div className="flex flex-wrap gap-1.5">
                    {suggestions.map((v) => (
                      <button
                        type="button"
                        key={v.name}
                        onClick={() => {
                          setCategory(v.name)
                          setIconChoice(iconNameFrom(v.icon))
                        }}
                        className="flex min-h-8 items-center gap-1.5 rounded-full bg-karte2 px-3 text-xs text-textschwach transition-colors"
                      >
                        <v.icon size={14} className="shrink-0" />
                        {v.name}
                      </button>
                    ))}
                  </div>
                </motion.div>
              ) : null}
            </AnimatePresence>

            <input
              type="date"
              aria-label="Datum"
              value={datum}
              onChange={(e) => setDatum(e.target.value)}
              className="mb-2.5 w-full rounded-xl bg-karte2 px-4 py-2.5 outline-none ring-strich focus:ring-2"
            />

            <div className="mb-2.5 rounded-xl bg-karte2 px-4 py-2.5">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Gemeinsame Ausgabe</span>
                <button
                  type="button"
                  role="switch"
                  aria-label="Gemeinsame Ausgabe"
                  aria-checked={shared}
                  onClick={() => setShared(!shared)}
                  className={`relative h-7 w-12 shrink-0 rounded-full transition-colors before:absolute before:-inset-3 before:content-[''] ${
                    shared ? 'bg-akzentstark' : 'bg-strich'
                  }`}
                >
                  <span
                    className={`block h-7 w-7 rounded-full bg-white transition-transform duration-200 ${
                      shared ? 'translate-x-5' : 'translate-x-0'
                    }`}
                  />
                </button>
              </div>
              <p className="mt-0.5 text-[12px] text-textschwach">
                {sharesText ? `Wird ${sharesText} geteilt` : 'Wird zu gleichen Teilen geteilt'}
              </p>
            </div>

            <label className="mb-1 block text-xs text-textschwach">
              {shared ? 'Bezahlt von' : 'Person'}
            </label>
            <div className="mb-2.5 flex gap-2">
              {persons.map((p) => {
                const selected = shared ? payer === p.name : person === p.name
                return (
                  <button
                    type="button"
                    key={p.id}
                    onClick={() => (shared ? setPayer(p.name) : setPerson(p.name))}
                    className={`flex min-h-11 flex-1 items-center justify-center rounded-xl px-3 transition-colors ${
                      selected ? 'bg-akzentstark text-akzenttext' : 'bg-karte2 text-textschwach'
                    }`}
                  >
                    {p.name}
                  </button>
                )
              })}
            </div>

            <AnimatePresence initial={false}>
              {debtAmount !== null && debtor && payer && (
                <motion.p
                  key={`${debtor}-${payer}-${debtAmount}`}
                  className="betragszahl mb-2.5 text-[13px] text-akzent"
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={soft}
                >
                  → {debtor} schuldet {payer} {euro(debtAmount)}
                </motion.p>
              )}
            </AnimatePresence>

            <div className="mb-2.5">
              <label className="mb-1 block text-xs text-textschwach">Konto</label>
              {ownAccounts.length > 0 ? (
                <div className="flex flex-wrap gap-2">
                  {ownAccounts.map((k) => (
                    <button
                      type="button"
                      key={k.id}
                      onClick={() => setAccount(account === k.name ? null : k.name)}
                      className={`flex min-h-9 items-center rounded-full px-4 text-sm transition-colors ${
                        account === k.name ? 'bg-akzentstark text-akzenttext' : 'bg-karte2 text-textschwach'
                      }`}
                    >
                      {k.name}
                    </button>
                  ))}
                </div>
              ) : (
                responsiblePerson && (
                  <p className="text-xs text-textschwach">{responsiblePerson} hat noch kein Konto</p>
                )
              )}
            </div>

            {error && <p className="mb-2 text-sm text-schlecht">{error}</p>}

            <button
              type="submit"
              disabled={busy}
              className="w-full rounded-xl bg-akzentstark py-2.5 font-semibold text-akzenttext transition-transform active:scale-[0.98] disabled:opacity-50"
            >
              Speichern
            </button>

            {entry && (
              <div className="mt-2 flex gap-2">
                {confirmDelete ? (
                  <>
                    <button
                      type="button"
                      onClick={deleteEntry}
                      disabled={busy}
                      className="flex min-h-11 flex-1 items-center justify-center rounded-xl bg-schlecht/15 px-3 text-sm font-semibold text-schlecht transition-transform active:scale-[0.98] disabled:opacity-50"
                    >
                      Wirklich löschen? — Tippen zum Bestätigen
                    </button>
                    <button
                      type="button"
                      onClick={() => setConfirmDelete(false)}
                      className="flex min-h-11 shrink-0 items-center justify-center rounded-xl px-4 text-sm font-medium text-textschwach"
                    >
                      Abbrechen
                    </button>
                  </>
                ) : (
                  <button
                    type="button"
                    onClick={() => setConfirmDelete(true)}
                    className="w-full py-2.5 font-semibold text-schlecht"
                  >
                    Löschen
                  </button>
                )}
              </div>
            )}
          </motion.form>
        </div>
      )}
    </AnimatePresence>
  )
}
