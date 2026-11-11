import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { CaretRight } from '@phosphor-icons/react'
import { lese, type Ausgleich, type BudgetInfo, type Konto, type Monatsuebersicht } from './types'
import { datumKurz, euro, monatAlsString, monatLabel, monatVerschieben } from './format'
import { useApp } from './AppKontext'

function Gesamtsaldo({ konten }: { konten: Konto[] }) {
  const gesamt = konten.reduce((summe, konto) => summe + konto.saldo, 0)
  return (
    <div className="mb-5 rounded-2xl bg-karte2 p-5">
      <p className="text-xs text-textschwach">Dein Gesamtsaldo</p>
      <p className={`mt-1 text-3xl font-semibold ${gesamt < 0 ? 'text-schlecht' : ''}`}>{euro(gesamt)}</p>
      {konten.length === 0 ? (
          <Link to="/einstellungen" className="mt-2 flex min-h-11 items-center text-sm text-akzent">
            Erstes Konto anlegen
          </Link>
      ) : (
        <ul className="mt-3 space-y-1">
          {konten.map((konto) => (
            <li key={konto.id} className="flex items-center justify-between text-sm">
              <span className="truncate text-textschwach">{konto.name}</span>
              <span className="font-medium">{euro(konto.saldo)}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function Karten({ ausgaben, einnahmen }: { ausgaben: number; einnahmen: number }) {
  return (
    <div className="mb-5 grid grid-cols-2 gap-3">
      <div className="rounded-2xl bg-karte p-4">
        <p className="text-xs text-textschwach">Ausgaben</p>
        <p className="mt-1 text-xl font-semibold text-schlecht">{euro(ausgaben)}</p>
      </div>
      <div className="rounded-2xl bg-karte p-4">
        <p className="text-xs text-textschwach">Einnahmen</p>
        <p className="mt-1 text-xl font-semibold text-gut">{euro(einnahmen)}</p>
      </div>
    </div>
  )
}

function KategorieBalken({
  name,
  summe,
  skala,
  budget,
}: {
  name: string
  summe: number
  skala: number
  budget?: BudgetInfo
}) {
  const breite = Math.min(100, (summe / Math.max(skala, 1)) * 100)
  const ueber = budget?.warnung === true
  const marke = budget ? Math.min(100, (budget.limit / Math.max(skala, 1)) * 100) : null
  return (
    <div className="mb-3">
      <div className="mb-1 flex items-baseline justify-between gap-2">
        <span className="truncate text-sm">{name}</span>
        <span className={`text-sm font-semibold ${ueber ? 'text-warn' : ''}`}>
          {euro(summe)}
          {budget && <span className="ml-1 text-xs text-textschwach">/ {euro(budget.limit)}</span>}
        </span>
      </div>
      <div className="relative h-2.5 overflow-hidden rounded-full bg-karte2">
        <div
          className={`h-full rounded-full ${ueber ? 'bg-warn' : 'bg-schlecht'}`}
          style={{ width: `${breite}%` }}
        />
        {marke !== null && (
          <div
            className="absolute top-0 h-full w-0.5 bg-text/60"
            style={{ left: `calc(${marke}% - 1px)` }}
          />
        )}
      </div>
      {ueber && <p className="mt-1 text-xs text-warn">{euro(summe - budget.limit)} über Budget</p>}
    </div>
  )
}

export function Uebersicht() {
  const { aktivePerson } = useApp()
  const [monat, setMonat] = useState(() => monatAlsString(new Date()))
  const [daten, setDaten] = useState<Monatsuebersicht | null>(null)
  const [ausgleich, setAusgleich] = useState<Ausgleich | null>(null)
  const [konten, setKonten] = useState<Konto[]>([])
  const [laedt, setLaedt] = useState(true)
  const [fehler, setFehler] = useState(false)

  const laden = useCallback(() => {
    setLaedt(true)
    const kontenPfad = aktivePerson ? `/api/konten?person=${encodeURIComponent(aktivePerson)}` : '/api/konten'
    Promise.all([
      lese<Monatsuebersicht>(`/api/monatsuebersicht/${monat}`),
      lese<Ausgleich>(`/api/ausgleich/${monat}`),
      lese<Konto[]>(kontenPfad),
    ])
      .then(([uebersicht, ausgleichsDaten, kontoListe]) => {
        setDaten(uebersicht)
        setAusgleich(ausgleichsDaten)
        setKonten(kontoListe)
        setFehler(false)
      })
      .catch(() => setFehler(true))
      .finally(() => setLaedt(false))
  }, [monat, aktivePerson])

  useEffect(laden, [laden])

  const ausgaben = Object.values(daten?.ausgabenJeKategorie ?? {}).reduce((a, b) => a + b, 0)
  const einnahmen = Object.values(daten?.einnahmenJeKategorie ?? {}).reduce((a, b) => a + b, 0)
  const kategorien = Object.entries(daten?.ausgabenJeKategorie ?? {}).sort((a, b) => b[1] - a[1])
  const skala = kategorien.reduce(
    (max, [name, summe]) => Math.max(max, summe, daten?.budgets[name]?.limit ?? 0),
    1,
  )

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
        <h2 className="text-lg font-semibold">{monatLabel(monat)}</h2>
        <button
          aria-label="Folgemonat"
          onClick={() => setMonat(monatVerschieben(monat, 1))}
          className="h-11 w-11 rounded-xl bg-karte text-lg"
        >
          ›
        </button>
      </div>

      {laedt && (
        <div className="space-y-3">
          <div className="h-20 animate-pulse rounded-2xl bg-karte" />
          <div className="h-40 animate-pulse rounded-2xl bg-karte" />
        </div>
      )}

      {fehler && <p className="text-schlecht">Die Übersicht konnte nicht geladen werden.</p>}

      {daten && !laedt && (
        <>
          <Gesamtsaldo konten={konten} />
          <Karten ausgaben={ausgaben} einnahmen={einnahmen} />

          {ausgleich && (
            <Link
              to="/ausgleich"
              className="mb-5 flex items-center justify-between gap-3 rounded-2xl border border-strich bg-karte2 p-4 transition-transform active:scale-[0.99]"
            >
              <div className="min-w-0">
                <p className="text-xs text-textschwach">Ausgleich</p>
                {ausgleich.vorschlag ? (
                  <p className="mt-0.5 truncate text-lg font-semibold text-text">{ausgleich.vorschlag}</p>
                ) : (
                  <p className="mt-0.5 truncate text-lg font-semibold text-text">Alles ausgeglichen</p>
                )}
                {ausgleich.laeuftNoch && (
                  <p className="mt-0.5 text-xs text-textschwach">Monat läuft noch</p>
                )}
              </div>
              <span className="shrink-0 text-textschwach">
                <CaretRight size={20} weight="bold" />
              </span>
            </Link>
          )}

          <h3 className="mb-2 text-sm font-medium text-textschwach">Ausgaben je Kategorie</h3>
          <div className="mb-6 rounded-2xl bg-karte p-4">
            {kategorien.length === 0 && <p className="text-sm text-textschwach">Noch keine Ausgaben in diesem Monat.</p>}
            {kategorien.map(([name, summe]) => (
              <KategorieBalken
                key={name}
                name={name}
                summe={summe}
                skala={skala}
                budget={daten.budgets[name]}
              />
            ))}
          </div>

          <h3 className="mb-2 text-sm font-medium text-textschwach">Einträge</h3>
          {daten.eintraege.length === 0 && (
            <p className="text-sm text-textschwach">Noch nichts erfasst — tippe auf „+" rechts unten.</p>
          )}
          <ul className="space-y-2">
            {daten.eintraege.map((eintrag) => (
              <li key={eintrag.id} className="flex items-center justify-between gap-3 rounded-xl bg-karte px-4 py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{eintrag.kategorie}</p>
                  <p className="text-xs text-textschwach">
                    {datumKurz(eintrag.datum)} ·{' '}
                    {eintrag.gemeinsam ? `gemeinsam (${eintrag.zahler} hat bezahlt)` : eintrag.person}
                  </p>
                </div>
                <span className={`font-semibold ${eintrag.betrag < 0 ? 'text-schlecht' : 'text-gut'}`}>
                  {euro(eintrag.betrag)}
                </span>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  )
}
