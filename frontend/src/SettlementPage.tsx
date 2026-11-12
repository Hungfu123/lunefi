import { useCallback, useEffect, useState } from 'react'
import { motion, useReducedMotion } from 'motion/react'
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
              {positionGroups.map((group) => (
                <li key={`${group.zahler}-${group.received}`}>
                  <p className="mb-1 px-1 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                    {group.zahler} {group.received ? 'hat erhalten' : 'hat bezahlt'}
                  </p>
                  <ul className="overflow-hidden rounded-xl bg-karte">
                    {group.positions.map((position) => (
                      <li
                        key={position.id}
                        className="flex items-center justify-between gap-3 border-b border-strich px-3 py-2.5 last:border-b-0"
                      >
                        <span className="flex min-w-0 items-center gap-3">
                          <CategoryIconTile name={position.kategorie} />
                          <span className="truncate text-[15px] font-semibold">{position.kategorie}</span>
                        </span>
                        <span className="betragszahl shrink-0 text-[13px] font-semibold">
                          {euro(Math.abs(position.betrag))}
                        </span>
                      </li>
                    ))}
                  </ul>
                </li>
              ))}
            </ul>
          )}

          {!data.ausgeglichen && data.vorschlag && (
            <button
              onClick={mark}
              disabled={marking}
              className="mb-5 flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-akzent/10 py-3 text-sm font-semibold text-akzent ring-1 ring-akzent/40 transition-transform active:scale-[0.98] disabled:opacity-50"
            >
              <CheckCircle size={18} weight="bold" aria-hidden="true" />
              Ausgeglichen
            </button>
          )}
          {justMarked && (
            <motion.p
              initial={{ opacity: 0, scale: reduced ? 1 : 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={reduced ? { duration: 0 } : { duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
              className="mb-5 text-center text-sm font-medium text-gut"
            >
              Ausgeglichen.
            </motion.p>
          )}

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
