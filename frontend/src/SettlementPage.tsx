import { useCallback, useEffect, useState } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { CheckCircle } from '@phosphor-icons/react'
import { read, send, type Settlement, type SettlementMonth, type SettlementPosition } from './types'
import { euro, monthAsString, shortMonth, monthLabel, shiftMonth } from './format'
import { CategoryIconTile } from './categoryIcon'

function SettledPill() {
  return (
    <span className="flex items-center gap-1 rounded-full bg-gut/15 px-2 py-0.5 text-xs font-medium text-gut">
      <CheckCircle size={14} weight="fill" aria-hidden="true" />
      ausgeglichen
    </span>
  )
}

export function SettlementPage() {
  const reduced = useReducedMotion()
  const [monat, setMonat] = useState(() => monthAsString(new Date()))
  const [data, setData] = useState<Settlement | null>(null)
  const [history, setHistory] = useState<SettlementMonth[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [marking, setMarking] = useState(false)
  const [justMarked, setJustMarked] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [openGroup, setOpenGroup] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    Promise.all([
      read<Settlement>(`/api/ausgleich/${monat}`),
      read<SettlementMonth[]>(`/api/ausgleich/verlauf?monate=6`),
    ])
      .then(([current, list]) => {
        setData(current)
        setHistory(list)
        setError(false)
      })
      .catch(() => {
        setData(null)
        setError(true)
      })
      .finally(() => setLoading(false))
  }, [monat])

  useEffect(load, [load])

  const mark = async () => {
    setMarking(true)
    try {
      await send(`/api/ausgleich/${monat}/ausgeglichen`)
      setJustMarked(true)
      load()
    } catch {
      setError(true)
    } finally {
      setMarking(false)
    }
  }

  const changeMonth = (delta: number) => {
    setJustMarked(false)
    setMonat(shiftMonth(monat, delta))
  }

  const selectMonth = (next: string) => {
    setJustMarked(false)
    setMonat(next)
  }

  const currentMonth = monthAsString(new Date())

  const persons = Object.keys(data?.saldi ?? {}).sort((a, b) => (data?.saldi[b] ?? 0) - (data?.saldi[a] ?? 0))

  const positionGroups = (() => {
    const groups = new Map<string, { zahler: string; received: boolean; positions: SettlementPosition[] }>()
    for (const position of data?.positionen ?? []) {
      const key = `${position.zahler}|${position.betrag >= 0}`
      const group = groups.get(key)
      if (group) {
        group.positions.push(position)
      } else {
        groups.set(key, {
          zahler: position.zahler,
          received: position.betrag >= 0,
          positions: [position],
        })
      }
    }
    return [...groups.values()]
  })()

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <button
          aria-label="Vormonat"
          onClick={() => changeMonth(-1)}
          className="h-11 w-11 rounded-xl bg-karte text-lg"
        >
          ‹
        </button>
        <div className="flex items-center gap-2">
          <h2 className="text-[15px] font-semibold">{monthLabel(monat)}</h2>
          {data?.ausgeglichen ? (
            <SettledPill />
          ) : (
            data?.laeuftNoch && (
              <span className="rounded-full bg-akzent/20 px-2 py-0.5 text-xs text-akzent">läuft noch</span>
            )
          )}
        </div>
        <button
          aria-label="Folgemonat"
          onClick={() => changeMonth(1)}
          disabled={monat >= currentMonth}
          className="h-11 w-11 rounded-xl bg-karte text-lg disabled:opacity-40"
        >
          ›
        </button>
      </div>

      {loading && <div className="h-32 animate-pulse rounded-2xl bg-karte" />}

      {error && !loading && <p className="text-sm text-schlecht">Ausgleich konnte nicht geladen werden.</p>}

      {data && !loading && (
        <>
          <div className="mb-5 rounded-2xl bg-karte p-5 text-center">
            {data.vorschlag ? (
              <p className="text-2xl font-semibold text-text">{data.vorschlag}</p>
            ) : (
              <p className="text-[15px] text-textschwach">Alles ausgeglichen.</p>
            )}
          </div>

          {persons.length > 0 && Object.values(data.saldi).some((saldo) => saldo !== 0) && (
            <div className="mb-5 flex flex-wrap items-center justify-center gap-x-2 gap-y-1 rounded-2xl bg-karte px-4 py-3">
              {persons.map((person, i) => {
                const saldo = data.saldi[person] ?? 0
                const shownAmounts = new Set(
                  persons.slice(0, i).map((p) => Math.abs(data.saldi[p] ?? 0)),
                )
                return (
                  <span key={person} className="flex items-center gap-1 text-[13px] text-textschwach">
                    {i > 0 && <span aria-hidden="true">·</span>}
                    <span className="truncate">{person} {saldo > 0 ? 'bekommt' : saldo < 0 ? 'schuldet' : 'ist ausgeglichen'}</span>
                    {saldo !== 0 && !shownAmounts.has(Math.abs(saldo)) && (
                      <span
                        className={`betragszahl text-[13px] font-semibold ${
                          saldo > 0 ? 'text-gut' : 'text-schlecht'
                        }`}
                      >
                        {euro(Math.abs(saldo))}
                      </span>
                    )}
                  </span>
                )
              })}
            </div>
          )}

          <h3 className="mb-2 text-[13px] font-semibold">Gemeinsame Ausgaben</h3>
          {data.positionen.length === 0 && (
            <p className="text-sm text-textschwach">Keine gemeinsamen Ausgaben.</p>
          )}
          {positionGroups.length > 0 && (
            <ul className="mb-6 space-y-3">
              {positionGroups.map((group) => {
                const byCategory = new Map<string, { summe: number; einzel: number[] }>()
                for (const position of group.positions) {
                  const entry = byCategory.get(position.kategorie) ?? { summe: 0, einzel: [] }
                  entry.summe += Math.abs(position.betrag)
                  entry.einzel.push(Math.abs(position.betrag))
                  byCategory.set(position.kategorie, entry)
                }
                return (
                  <li key={`${group.zahler}-${group.received}`}>
                    <p className="mb-1 px-1 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                      {group.zahler} {group.received ? 'hat erhalten' : 'hat bezahlt'}
                    </p>
                    <ul className="overflow-hidden rounded-xl bg-karte">
                      {[...byCategory.entries()].map(([kategorie, info]) => {
                        const key = `${group.zahler}-${group.received}-${kategorie}`
                        const expanded = openGroup === key
                        return (
                          <li key={kategorie} className="border-b border-strich last:border-b-0">
                            <button
                              type="button"
                              onClick={() => setOpenGroup(expanded ? null : key)}
                              aria-expanded={info.einzel.length > 1 ? expanded : undefined}
                              disabled={info.einzel.length === 1}
                              className="flex w-full items-center justify-between gap-3 px-3 py-2.5 text-left"
                            >
                              <span className="flex min-w-0 items-center gap-3">
                                <CategoryIconTile name={kategorie} />
                                <span className="truncate text-[15px] font-semibold">{kategorie}</span>
                                {info.einzel.length > 1 && (
                                  <span className="shrink-0 text-[11px] text-textschwach">
                                    · {info.einzel.length} Zahlungen
                                  </span>
                                )}
                              </span>
                              <span className="betragszahl shrink-0 text-[13px] font-semibold">
                                {euro(info.summe)}
                              </span>
                            </button>
                            {expanded && info.einzel.length > 1 && (
                              <ul className="border-t border-strich bg-karte2/40 px-3 py-1.5">
                                {info.einzel.map((betrag, i) => (
                                  <li
                                    key={i}
                                    className="flex justify-between py-0.5 text-[12px] text-textschwach"
                                  >
                                    <span>Zahlung {i + 1}</span>
                                    <span className="betragszahl">{euro(betrag)}</span>
                                  </li>
                                ))}
                              </ul>
                            )}
                          </li>
                        )
                      })}
                    </ul>
                  </li>
                )
              })}
            </ul>
          )}

          {!data.ausgeglichen && data.vorschlag && (
            <button
              onClick={() => setConfirmOpen(true)}
              disabled={marking}
              className="mb-5 flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-akzent/10 py-3 text-sm font-semibold text-akzent ring-1 ring-akzent/40 transition-transform active:scale-[0.98] disabled:opacity-50"
            >
              <CheckCircle size={18} weight="bold" aria-hidden="true" />
              Alles beglichen
            </button>
          )}
          {justMarked && (
            <motion.p
              initial={{ opacity: 0, scale: reduced ? 1 : 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={reduced ? { duration: 0 } : { duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
              className="mb-5 text-center text-sm font-medium text-gut"
            >
              Alles beglichen.
            </motion.p>
          )}

          <AnimatePresence>
            {confirmOpen && (
              <div className="fixed inset-0 z-40 flex items-end justify-center">
                <motion.button
                  aria-label="Schließen"
                  className="absolute inset-0 bg-black/60"
                  onClick={() => setConfirmOpen(false)}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.18 }}
                />
                <motion.div
                  className="relative mx-auto w-full max-w-[560px] rounded-t-3xl border-t border-strich bg-karte px-5 pb-6 pt-4"
                  initial={{ transform: 'translateY(100%)' }}
                  animate={{ transform: 'translateY(0%)' }}
                  exit={{ transform: 'translateY(100%)' }}
                  transition={reduced ? { duration: 0 } : { type: 'spring', duration: 0.4, bounce: 0.15 }}
                >
                  <div className="mx-auto mb-3 h-1 w-10 rounded-full bg-strich" />
                  <p className="mb-1 text-lg font-semibold">Alles beglichen?</p>
                  <p className="mb-4 text-sm text-textschwach">
                    {data.vorschlag} — danach ist {monthLabel(monat)} erledigt.
                  </p>
                  <div className="flex gap-2">
                    <button
                      onClick={() => setConfirmOpen(false)}
                      className="min-h-11 flex-1 rounded-xl bg-karte2 py-3 text-sm font-medium text-textschwach"
                    >
                      Abbrechen
                    </button>
                    <button
                      onClick={() => {
                        setConfirmOpen(false)
                        mark()
                      }}
                      disabled={marking}
                      className="min-h-11 flex-1 rounded-xl bg-akzentstark py-3 text-sm font-semibold text-akzenttext disabled:opacity-50"
                    >
                      Ja, beglichen
                    </button>
                  </div>
                  <p className="mt-2 text-center text-[11px] text-textschwach">
                    Vermerkt nur — es wird kein Geld bewegt.
                  </p>
                </motion.div>
              </div>
            )}
          </AnimatePresence>

          {history.length > 1 && (
            <>
              <h3 className="mb-2 text-[13px] font-semibold">Verlauf</h3>
              <div className="flex gap-2 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
                {history.map((m) => {
                  const amount = Math.max(0, ...Object.values(m.saldi).map(Math.abs))
                  const status = m.ausgeglichen
                    ? 'ausgeglichen'
                    : m.laeuftNoch
                      ? 'läuft noch'
                      : m.vorschlag
                        ? 'offen'
                        : 'nichts offen'
                  return (
                    <button
                      key={m.monat}
                      onClick={() => selectMonth(m.monat)}
                      aria-label={`${monthLabel(m.monat)} — ${status}`}
                      className={`flex h-9 shrink-0 items-center gap-1.5 rounded-full bg-karte px-3 transition-[background-color,box-shadow] duration-150 ease-out ${
                        m.monat === monat ? 'ring-2 ring-akzent' : ''
                      }`}
                    >
                      <span className="text-[13px] font-medium">{shortMonth(m.monat)}</span>
                      {m.ausgeglichen ? (
                        <CheckCircle size={14} weight="fill" className="shrink-0 text-gut" />
                      ) : m.laeuftNoch ? (
                        <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-akzent" />
                      ) : m.vorschlag ? (
                        <span className="betragszahl text-[11px] font-semibold text-schlecht">
                          {euro(amount)}
                        </span>
                      ) : (
                        <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-textschwach/40" />
                      )}
                    </button>
                  )
                })}
              </div>
            </>
          )}
        </>
      )}
    </div>
  )
}
