import { useCallback, useEffect, useState } from 'react'
import { motion, useReducedMotion } from 'motion/react'
import { CheckCircle } from '@phosphor-icons/react'
import { lese, sende, type Ausgleich, type AusgleichMonat, type AusgleichPosition } from './types'
import { euro, monatAlsString, monatLabel, monatVerschieben } from './format'
import { KategorieIconTile } from './kategorieIcon'

function AusgeglichenPill() {
  return (
    <span className="flex items-center gap-1 rounded-full bg-gut/15 px-2 py-0.5 text-xs font-medium text-gut">
      <CheckCircle size={14} weight="fill" aria-hidden="true" />
      ausgeglichen
    </span>
  )
}

export function AusgleichSeite() {
  const reduziert = useReducedMotion()
  const [monat, setMonat] = useState(() => monatAlsString(new Date()))
  const [daten, setDaten] = useState<Ausgleich | null>(null)
  const [verlauf, setVerlauf] = useState<AusgleichMonat[]>([])
  const [laedt, setLaedt] = useState(true)
  const [fehler, setFehler] = useState(false)
  const [markiert, setMarkiert] = useState(false)
  const [markiertNeu, setMarkiertNeu] = useState(false)

  const laden = useCallback(() => {
    setLaedt(true)
    Promise.all([
      lese<Ausgleich>(`/api/ausgleich/${monat}`),
      lese<AusgleichMonat[]>(`/api/ausgleich/verlauf?monate=6`),
    ])
      .then(([aktuell, liste]) => {
        setDaten(aktuell)
        setVerlauf(liste)
        setFehler(false)
      })
      .catch(() => {
        setDaten(null)
        setFehler(true)
      })
      .finally(() => setLaedt(false))
  }, [monat])

  useEffect(laden, [laden])

  const markieren = async () => {
    setMarkiert(true)
    try {
      await sende(`/api/ausgleich/${monat}/ausgeglichen`)
      setMarkiertNeu(true)
      laden()
    } catch {
      setFehler(true)
    } finally {
      setMarkiert(false)
    }
  }

  const monatWechseln = (delta: number) => {
    setMarkiertNeu(false)
    setMonat(monatVerschieben(monat, delta))
  }

  const monatWaehlen = (neu: string) => {
    setMarkiertNeu(false)
    setMonat(neu)
  }

  const aktuellerMonat = monatAlsString(new Date())

  const personen = Object.keys(daten?.saldi ?? {}).sort((a, b) => (daten?.saldi[b] ?? 0) - (daten?.saldi[a] ?? 0))

  const positionenGruppen = (() => {
    const gruppen = new Map<string, { zahler: string; erhalten: boolean; positionen: AusgleichPosition[] }>()
    for (const position of daten?.positionen ?? []) {
      const schluessel = `${position.zahler}|${position.betrag >= 0}`
      const gruppe = gruppen.get(schluessel)
      if (gruppe) {
        gruppe.positionen.push(position)
      } else {
        gruppen.set(schluessel, {
          zahler: position.zahler,
          erhalten: position.betrag >= 0,
          positionen: [position],
        })
      }
    }
    return [...gruppen.values()]
  })()

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <button
          aria-label="Vormonat"
          onClick={() => monatWechseln(-1)}
          className="h-11 w-11 rounded-xl bg-karte text-lg"
        >
          ‹
        </button>
        <div className="flex items-center gap-2">
          <h2 className="text-lg font-semibold">{monatLabel(monat)}</h2>
          {daten?.ausgeglichen ? (
            <AusgeglichenPill />
          ) : (
            daten?.laeuftNoch && (
              <span className="rounded-full bg-akzent/20 px-2 py-0.5 text-xs text-akzent">läuft noch</span>
            )
          )}
        </div>
        <button
          aria-label="Folgemonat"
          onClick={() => monatWechseln(1)}
          disabled={monat >= aktuellerMonat}
          className="h-11 w-11 rounded-xl bg-karte text-lg disabled:opacity-40"
        >
          ›
        </button>
      </div>

      {laedt && <div className="h-32 animate-pulse rounded-2xl bg-karte" />}

      {fehler && !laedt && <p className="text-schlecht">Der Ausgleich konnte nicht geladen werden.</p>}

      {daten && !laedt && (
        <>
          <div className="mb-5 rounded-2xl bg-karte p-5 text-center">
            {daten.vorschlag ? (
              <p className="text-2xl font-semibold text-text">{daten.vorschlag}</p>
            ) : (
              <p className="text-lg text-textschwach">Alles ausgeglichen.</p>
            )}
          </div>

          {personen.length > 0 && Object.values(daten.saldi).some((saldo) => saldo !== 0) && (
            <div className="mb-5 flex flex-wrap items-center justify-center gap-x-2 gap-y-1 rounded-2xl bg-karte px-4 py-3">
              {personen.map((person, i) => {
                const saldo = daten.saldi[person] ?? 0
                const gezeigteBetraege = new Set(
                  personen.slice(0, i).map((p) => Math.abs(daten.saldi[p] ?? 0)),
                )
                return (
                  <span key={person} className="flex items-center gap-1 text-sm text-textschwach">
                    {i > 0 && <span aria-hidden="true">·</span>}
                    <span className="truncate">{person} {saldo > 0 ? 'bekommt' : saldo < 0 ? 'schuldet' : 'ist ausgeglichen'}</span>
                    {saldo !== 0 && !gezeigteBetraege.has(Math.abs(saldo)) && (
                      <span
                        className={`betragszahl text-sm font-semibold ${
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

          <h3 className="mb-2 text-sm font-medium text-textschwach">Gemeinsame Ausgaben</h3>
          {daten.positionen.length === 0 && (
            <p className="text-sm text-textschwach">Keine gemeinsamen Ausgaben in diesem Monat.</p>
          )}
          {positionenGruppen.length > 0 && (
            <ul className="mb-6 space-y-3">
              {positionenGruppen.map((gruppe) => (
                <li key={`${gruppe.zahler}-${gruppe.erhalten}`}>
                  <p className="mb-1 px-1 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                    {gruppe.zahler} {gruppe.erhalten ? 'hat erhalten' : 'hat bezahlt'}
                  </p>
                  <ul className="overflow-hidden rounded-xl bg-karte">
                    {gruppe.positionen.map((position) => (
                      <li
                        key={position.id}
                        className="flex items-center justify-between gap-3 border-b border-strich px-3 py-2.5 last:border-b-0"
                      >
                        <span className="flex min-w-0 items-center gap-3">
                          <KategorieIconTile name={position.kategorie} />
                          <span className="truncate text-[15px] font-medium">{position.kategorie}</span>
                        </span>
                        <span className="betragszahl shrink-0 text-sm font-semibold">
                          {euro(Math.abs(position.betrag))}
                        </span>
                      </li>
                    ))}
                  </ul>
                </li>
              ))}
            </ul>
          )}

          {!daten.ausgeglichen && daten.vorschlag && (
            <button
              onClick={markieren}
              disabled={markiert}
              className="mb-5 flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-akzent/10 py-3 text-sm font-semibold text-akzent ring-1 ring-akzent/40 transition-transform active:scale-[0.98] disabled:opacity-50"
            >
              <CheckCircle size={18} weight="bold" aria-hidden="true" />
              Ausgeglichen
            </button>
          )}
          {markiertNeu && (
            <motion.p
              initial={{ opacity: 0, scale: reduziert ? 1 : 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={reduziert ? { duration: 0 } : { duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
              className="mb-5 text-center text-sm font-medium text-gut"
            >
              Ausgeglichen — fair bleibt fair.
            </motion.p>
          )}

          {verlauf.length > 1 && (
            <>
              <h3 className="mb-2 text-sm font-medium text-textschwach">Verlauf</h3>
              <ul className="space-y-2">
                {verlauf.map((m) => (
                  <li key={m.monat}>
                    <button
                      onClick={() => monatWaehlen(m.monat)}
                      className={`flex w-full items-center justify-between rounded-xl bg-karte px-4 py-2.5 text-left transition-colors ${
                        m.monat === monat ? 'ring-2 ring-akzent' : ''
                      }`}
                    >
                      <span className="text-sm">{monatLabel(m.monat)}</span>
                      <span className="flex items-center gap-1.5">
                        <span className="text-sm text-textschwach">{m.vorschlag ?? 'ausgeglichen'}</span>
                        {m.ausgeglichen && (
                          <CheckCircle size={14} weight="fill" className="shrink-0 text-gut" aria-label="ausgeglichen" />
                        )}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </>
          )}
        </>
      )}
    </div>
  )
}
