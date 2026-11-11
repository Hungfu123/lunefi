import { useCallback, useEffect, useState } from 'react'
import { lese, type Ausgleich, type AusgleichMonat } from './types'
import { euro, monatAlsString, monatLabel, monatVerschieben } from './format'

export function AusgleichSeite() {
  const [monat, setMonat] = useState(() => monatAlsString(new Date()))
  const [daten, setDaten] = useState<Ausgleich | null>(null)
  const [verlauf, setVerlauf] = useState<AusgleichMonat[]>([])
  const [laedt, setLaedt] = useState(true)
  const [fehler, setFehler] = useState(false)

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

  const durchschnitt = (person: string) => {
    const werte = verlauf.map((m) => m.saldi[person] ?? 0)
    if (werte.length === 0) return 0
    return werte.reduce((a, b) => a + b, 0) / werte.length
  }

  const personen = Object.keys(daten?.saldi ?? {})

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <button
          aria-label="Vormonat"
          onClick={() => setMonat(monatVerschieben(monat, -1))}
          className="h-11 w-11 rounded-xl bg-karte text-lg"
        >
          ‹
        </button>
        <div className="flex items-center gap-2">
          <h2 className="text-lg font-semibold">{monatLabel(monat)}</h2>
          {daten?.laeuftNoch && (
            <span className="rounded-full bg-akzent/20 px-2 py-0.5 text-xs text-akzent">läuft noch</span>
          )}
        </div>
        <button
          aria-label="Folgemonat"
          onClick={() => setMonat(monatVerschieben(monat, 1))}
          className="h-11 w-11 rounded-xl bg-karte text-lg"
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

          <div className="mb-5 grid grid-cols-2 gap-3">
            {personen.map((person) => {
              const saldo = daten.saldi[person] ?? 0
              return (
                <div key={person} className="rounded-2xl bg-karte p-4">
                  <p className="truncate text-xs text-textschwach">{person}</p>
                  <p className={`mt-1 text-lg font-semibold ${saldo > 0 ? 'text-gut' : saldo < 0 ? 'text-schlecht' : ''}`}>
                    {euro(saldo)}
                  </p>
                  <p className="mt-0.5 text-xs text-textschwach">
                    {saldo > 0 ? 'bekommt' : saldo < 0 ? 'schuldet' : 'ausgeglichen'} · Ø {euro(durchschnitt(person))} pro Monat
                  </p>
                </div>
              )
            })}
          </div>

          <h3 className="mb-2 text-sm font-medium text-textschwach">Gemeinsame Ausgaben</h3>
          {daten.positionen.length === 0 && (
            <p className="text-sm text-textschwach">Keine gemeinsamen Ausgaben in diesem Monat.</p>
          )}
          <ul className="mb-6 space-y-2">
            {daten.positionen.map((position) => (
              <li key={position.id} className="rounded-xl bg-karte px-4 py-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">{position.kategorie}</span>
                  <span className="text-sm font-semibold">{euro(Math.abs(position.betrag))}</span>
                </div>
                <p className="mt-1 text-xs text-textschwach">
                  {position.zahler} {position.betrag >= 0 ? 'hat erhalten' : 'hat bezahlt'}
                  {Object.entries(position.anteile).map(([person, anteil]) => (
                    <span key={person}>
                      {' '}
                      · {person} {euro(anteil)}
                    </span>
                  ))}
                </p>
              </li>
            ))}
          </ul>

          {verlauf.length > 1 && (
            <>
              <h3 className="mb-2 text-sm font-medium text-textschwach">Verlauf</h3>
              <ul className="space-y-2">
                {verlauf.map((m) => (
                  <li key={m.monat} className="flex items-center justify-between rounded-xl bg-karte px-4 py-2.5">
                    <span className="text-sm">{monatLabel(m.monat)}</span>
                    <span className="text-sm text-textschwach">{m.vorschlag ?? 'ausgeglichen'}</span>
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
