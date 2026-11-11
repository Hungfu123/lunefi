import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { CaretDown, CaretRight, Wallet } from '@phosphor-icons/react'
import { lese, type Ausgleich, type Konto, type Monatsuebersicht } from './types'
import { datumKurz, euro, monatAlsString, monatLabel, monatVerschieben } from './format'
import { useApp } from './AppKontext'

const KATEGORIEN_SICHTBAR = 3
const EINTRAEGE_SICHTBAR = 3

const BALKEN_STAERKE = ['bg-schlecht', 'bg-schlecht/70', 'bg-schlecht/55', 'bg-schlecht/45', 'bg-schlecht/35']

function MiniBalken({ name, summe, skala, rang }: { name: string; summe: number; skala: number; rang: number }) {
  const breite = Math.min(100, (summe / Math.max(skala, 1)) * 100)
  return (
    <div>
      <div className="flex items-baseline justify-between gap-2">
        <span className="truncate text-[13px] font-medium">{name}</span>
        <span className="betragszahl shrink-0 text-[13px] font-semibold">{euro(summe)}</span>
      </div>
      <div className="mt-1 h-1.5 rounded-full bg-karte2">
        <div
          className={`h-full rounded-full ${BALKEN_STAERKE[rang] ?? 'bg-schlecht/30'}`}
          style={{ width: `${breite}%` }}
        />
      </div>
    </div>
  )
}

export function Uebersicht() {
  const { aktivePerson } = useApp()
  const reduziert = useReducedMotion()
  const [monat, setMonat] = useState(() => monatAlsString(new Date()))
  const [daten, setDaten] = useState<Monatsuebersicht | null>(null)
  const [ausgleich, setAusgleich] = useState<Ausgleich | null>(null)
  const [konten, setKonten] = useState<Konto[]>([])
  const [laedt, setLaedt] = useState(true)
  const [fehler, setFehler] = useState(false)
  const [kategorienOffen, setKategorienOffen] = useState(false)
  const [eintraegeOffen, setEintraegeOffen] = useState(false)

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

  const gesamt = konten.reduce((summe, konto) => summe + konto.saldo, 0)
  const ausgaben = Object.values(daten?.ausgabenJeKategorie ?? {}).reduce((a, b) => a + b, 0)
  const einnahmen = Object.values(daten?.einnahmenJeKategorie ?? {}).reduce((a, b) => a + b, 0)
  const kategorien = Object.entries(daten?.ausgabenJeKategorie ?? {}).sort((a, b) => b[1] - a[1])
  const kategorieSkala = kategorien.reduce((max, [, summe]) => Math.max(max, summe), 1)
  const budgetListe = Object.values(daten?.budgets ?? {})
  const budgetIst = budgetListe.reduce((a, b) => a + b.ist, 0)
  const budgetLimit = budgetListe.reduce((a, b) => a + b.limit, 0)
  const budgetUeber = budgetListe.some((b) => b.warnung)
  const budgetSkala = Math.max(budgetIst, budgetLimit, 1)
  const eintraege = [...(daten?.eintraege ?? [])].sort((a, b) =>
    a.datum === b.datum ? b.id - a.id : a.datum < b.datum ? 1 : -1,
  )

  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <button
          aria-label="Vormonat"
          onClick={() => setMonat(monatVerschieben(monat, -1))}
          className="-ml-3 flex h-11 w-11 items-center justify-center text-xl text-textschwach"
        >
          ‹
        </button>
        <h2 className="text-[15px] font-semibold">{monatLabel(monat)}</h2>
        <button
          aria-label="Folgemonat"
          onClick={() => setMonat(monatVerschieben(monat, 1))}
          className="-mr-3 flex h-11 w-11 items-center justify-center text-xl text-textschwach"
        >
          ›
        </button>
      </div>

      {laedt && (
        <div className="space-y-3">
          <div className="h-24 animate-pulse rounded-2xl bg-karte" />
          <div className="h-32 animate-pulse rounded-2xl bg-karte" />
        </div>
      )}

      {fehler && <p className="text-schlecht">Die Übersicht konnte nicht geladen werden.</p>}

      {daten && !laedt && (
        <>
          <section className="mb-3">
            <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">Gesamtsaldo</p>
            <p
              className={`betragszahl mt-1.5 text-[44px] font-semibold leading-none tracking-tight ${
                gesamt < 0 ? 'text-schlecht' : 'text-text'
              }`}
            >
              {euro(gesamt)}
            </p>
            {ausgleich && (
              <Link to="/ausgleich" className="-my-1 flex min-h-8 items-center gap-2 py-1">
                <span className="shrink-0 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                  Ausgleich
                </span>
                <span className="betragszahl truncate text-[13px] font-medium text-text">
                  {ausgleich.vorschlag ?? 'Alles ausgeglichen'}
                </span>
                <CaretRight size={13} className="ml-auto shrink-0 text-textschwach" />
              </Link>
            )}
            {konten.length === 0 && (
              <Link to="/einstellungen" className="mt-2 flex items-center gap-1 text-sm text-akzent">
                Erstes Konto anlegen
                <CaretRight size={12} />
              </Link>
            )}
          </section>

          {konten.length > 0 && (
            <div className="mb-3 flex gap-2 overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
              {konten.map((konto) => (
                <Link
                  key={konto.id}
                  to="/einstellungen"
                  className="flex h-11 shrink-0 items-center gap-2 rounded-full bg-karte pl-3 pr-3.5"
                >
                  <Wallet size={15} className="shrink-0 text-akzent" />
                  <span className="whitespace-nowrap text-[13px] font-medium">{konto.name}</span>
                  <span
                    className={`betragszahl whitespace-nowrap text-[13px] font-semibold ${
                      konto.saldo < 0 ? 'text-schlecht' : 'text-textschwach'
                    }`}
                  >
                    {euro(konto.saldo)}
                  </span>
                </Link>
              ))}
            </div>
          )}

          <div className={`mb-3 grid gap-3 ${budgetListe.length > 0 ? 'grid-cols-2' : 'grid-cols-1'}`}>
            <div className="rounded-2xl bg-karte p-3.5">
              <div className="flex items-baseline justify-between gap-2">
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">Ausgaben</p>
                <p className="betragszahl text-[17px] font-semibold text-schlecht">{euro(ausgaben)}</p>
              </div>
              <div className="mt-2 flex items-baseline justify-between gap-2">
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">Einnahmen</p>
                <p className="betragszahl text-[17px] font-semibold text-gut">{euro(einnahmen)}</p>
              </div>
            </div>
            {budgetListe.length > 0 && (
              <div className="flex flex-col justify-center rounded-2xl bg-karte p-3.5">
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">Budget</p>
                <div className="relative mt-2 h-2 rounded-full bg-karte2">
                  <div
                    className={`h-full rounded-full ${budgetUeber ? 'bg-schlecht' : 'bg-akzent'}`}
                    style={{ width: `${Math.min(100, (budgetIst / budgetSkala) * 100)}%` }}
                  />
                  <div
                    className="absolute top-0 h-full w-0.5 bg-text/60"
                    style={{ left: `calc(${Math.min(100, (budgetLimit / budgetSkala) * 100)}% - 1px)` }}
                  />
                </div>
                <p className="betragszahl mt-1.5 text-[13px] font-medium text-textschwach">
                  {euro(budgetIst)} von {euro(budgetLimit)}
                </p>
                {budgetUeber && (
                  <p className="betragszahl mt-1 text-xs font-medium text-warn">
                    {euro(budgetIst - budgetLimit)} über Budget
                  </p>
                )}
              </div>
            )}
          </div>

          {kategorien.length > 0 && (
            <section className="mb-3">
              <div className="mb-1 flex items-baseline justify-between">
                <h3 className="text-[13px] font-semibold">Top-Kategorien</h3>
                {kategorien.length > KATEGORIEN_SICHTBAR && (
                  <button
                    onClick={() => setKategorienOffen((offen) => !offen)}
                    className="flex items-center gap-0.5 text-xs font-medium text-akzent"
                  >
                    {kategorienOffen ? 'Weniger' : 'Alle anzeigen'}
                    <CaretDown size={11} className={kategorienOffen ? '' : 'rotate-180'} />
                  </button>
                )}
              </div>
              <div className="space-y-1.5">
                {kategorien.slice(0, KATEGORIEN_SICHTBAR).map(([name, summe], rang) => (
                  <MiniBalken key={name} name={name} summe={summe} skala={kategorieSkala} rang={rang} />
                ))}
                <AnimatePresence initial={false}>
                  {kategorienOffen && kategorien.length > KATEGORIEN_SICHTBAR && (
                    <motion.div
                      key="rest"
                      className="overflow-hidden"
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={reduziert ? { duration: 0 } : { duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
                    >
                      <div className="space-y-2 pt-2">
                        {kategorien.slice(KATEGORIEN_SICHTBAR).map(([name, summe], i) => (
                          <MiniBalken
                            key={name}
                            name={name}
                            summe={summe}
                            skala={kategorieSkala}
                            rang={i + KATEGORIEN_SICHTBAR}
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
              <h3 className="text-[13px] font-semibold">Letzte Buchungen</h3>
              {eintraege.length > EINTRAEGE_SICHTBAR && (
                <button
                  onClick={() => setEintraegeOffen((offen) => !offen)}
                  className="text-xs font-medium text-akzent"
                >
                  {eintraegeOffen ? 'Weniger' : 'Alle anzeigen'}
                </button>
              )}
            </div>
            {eintraege.length === 0 ? (
              <p className="py-2 text-sm text-textschwach">Noch nichts erfasst — tippe unten rechts auf „+".</p>
            ) : (
              <ul>
                {(eintraegeOffen ? eintraege : eintraege.slice(0, EINTRAEGE_SICHTBAR)).map((eintrag, i, liste) => (
                  <li
                    key={eintrag.id}
                    className={`flex items-center justify-between gap-3 py-2 ${
                      i < liste.length - 1 ? 'border-b border-strich' : ''
                    }`}
                  >
                    <div className="min-w-0">
                      <p className="truncate text-[15px] font-semibold leading-tight">{eintrag.kategorie}</p>
                      <p className="text-xs leading-tight text-textschwach">
                        {datumKurz(eintrag.datum)} ·{' '}
                        {eintrag.gemeinsam ? `gemeinsam · ${eintrag.zahler}` : eintrag.person}
                      </p>
                    </div>
                    <span
                      className={`betragszahl shrink-0 text-[15px] font-semibold ${
                        eintrag.betrag < 0 ? 'text-schlecht' : 'text-gut'
                      }`}
                    >
                      {eintrag.betrag > 0 ? '+' : ''}
                      {euro(eintrag.betrag)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </div>
  )
}
