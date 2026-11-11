import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { ArrowsLeftRight, CaretDown, CaretRight, Wallet } from '@phosphor-icons/react'
import { lese, type Ausgleich, type Konto, type Monatsuebersicht, type VerlaufMonat } from './types'
import {
  datumKurz,
  betragKompakt,
  euro,
  monatAlsString,
  monatKurz,
  monatLabel,
  monatVerschieben,
} from './format'
import { KategorieIconTile } from './kategorieIcon'
import { useApp } from './AppKontext'

const KATEGORIEN_SICHTBAR = 3
const EINTRAEGE_SICHTBAR = 3

const BALKEN_STAERKE = ['bg-schlecht', 'bg-schlecht/70', 'bg-schlecht/55', 'bg-schlecht/45', 'bg-schlecht/35']

type Sicht = 'meine' | 'zusammen'

function nurGemeinsam(daten: Monatsuebersicht): Monatsuebersicht {
  const eintraege = daten.eintraege.filter((eintrag) => eintrag.gemeinsam)
  const ausgabenJeKategorie: Record<string, number> = {}
  const einnahmenJeKategorie: Record<string, number> = {}
  for (const eintrag of eintraege) {
    const ziel = eintrag.betrag < 0 ? ausgabenJeKategorie : einnahmenJeKategorie
    ziel[eintrag.kategorie] = (ziel[eintrag.kategorie] ?? 0) + Math.abs(eintrag.betrag)
  }
  const budgets = Object.fromEntries(
    Object.entries(daten.budgets).map(([name, budget]) => {
      const ist = ausgabenJeKategorie[name] ?? 0
      return [name, { limit: budget.limit, ist, warnung: ist > budget.limit }]
    }),
  )
  return { ...daten, eintraege, ausgabenJeKategorie, einnahmenJeKategorie, budgets }
}

function sichtLaden(): Sicht {
  return localStorage.getItem('lunefi:sicht') === 'zusammen' ? 'zusammen' : 'meine'
}

function GeteiltPill() {
  return (
    <span
      title="geteilt"
      className="inline-flex h-5 shrink-0 items-center rounded-full px-1 text-textschwach"
      style={{ background: 'color-mix(in srgb, var(--color-text) 8%, transparent)' }}
    >
      <ArrowsLeftRight size={12} aria-hidden="true" />
    </span>
  )
}

function MiniBalken({ name, summe, skala, rang }: { name: string; summe: number; skala: number; rang: number }) {
  const breite = Math.min(100, (summe / Math.max(skala, 1)) * 100)
  return (
    <div>
      <div className="flex items-center justify-between gap-2">
        <span className="flex min-w-0 items-center gap-3">
          <KategorieIconTile name={name} />
          <span className="truncate text-[15px] font-semibold text-text">{name}</span>
        </span>
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

function TrendBalken({ eintrag, skala }: { eintrag: VerlaufMonat; skala: number }) {
  const positiv = eintrag.differenz >= 0
  const hoehe = Math.max(2, Math.round((Math.abs(eintrag.differenz) / Math.max(skala, 1)) * 12))
  return (
    <span className="flex shrink-0 items-center gap-1">
      <span className="relative block h-6 w-2">
        <span className="absolute inset-x-0 top-1/2 h-px bg-strich" />
        <span
          className={`absolute inset-x-0 rounded-[2px] ${positiv ? 'bottom-1/2 bg-gut' : 'top-1/2 bg-schlecht'}`}
          style={{ height: hoehe }}
        />
      </span>
      <span className="text-[10px] leading-none text-textschwach">{monatKurz(eintrag.monat)}</span>
    </span>
  )
}

export function Uebersicht() {
  const { aktivePerson } = useApp()
  const reduziert = useReducedMotion()
  const [monat, setMonat] = useState(() => monatAlsString(new Date()))
  const [sicht, setSicht] = useState<Sicht>(sichtLaden)
  const [daten, setDaten] = useState<Monatsuebersicht | null>(null)
  const [ausgleich, setAusgleich] = useState<Ausgleich | null>(null)
  const [konten, setKonten] = useState<Konto[]>([])
  const [verlauf, setVerlauf] = useState<VerlaufMonat[] | null>(null)
  const [laedt, setLaedt] = useState(true)
  const [fehler, setFehler] = useState(false)
  const [kategorienOffen, setKategorienOffen] = useState(false)
  const [eintraegeOffen, setEintraegeOffen] = useState(false)
  const [gemeinsamLokal, setGemeinsamLokal] = useState(false)

  const sichtSetzen = (neu: Sicht) => {
    setSicht(neu)
    localStorage.setItem('lunefi:sicht', neu)
  }

  const gefiltert = sicht === 'meine' && aktivePerson !== null
  const zusammen = sicht === 'zusammen'

  const laden = useCallback(() => {
    setLaedt(true)
    const personParam = gefiltert ? `?person=${encodeURIComponent(aktivePerson ?? '')}` : ''
    const uebersichtLaden = zusammen
      ? lese<Monatsuebersicht>(`/api/monatsuebersicht/${monat}?gemeinsam=true`)
          .catch(() => lese<Monatsuebersicht>(`/api/monatsuebersicht/${monat}`))
          .then((roh) => {
            if (roh.eintraege.some((eintrag) => !eintrag.gemeinsam)) {
              setGemeinsamLokal(true)
              return nurGemeinsam(roh)
            }
            setGemeinsamLokal(false)
            return roh
          })
      : lese<Monatsuebersicht>(`/api/monatsuebersicht/${monat}${personParam}`).then((roh) => {
          setGemeinsamLokal(false)
          return roh
        })
    const kontenLaden = zusammen
      ? Promise.resolve<Konto[]>([])
      : lese<Konto[]>(gefiltert ? `/api/konten${personParam}` : '/api/konten')
    Promise.all([uebersichtLaden, lese<Ausgleich>(`/api/ausgleich/${monat}`), kontenLaden])
      .then(([uebersicht, ausgleichsDaten, kontoListe]) => {
        setDaten(uebersicht)
        setAusgleich(ausgleichsDaten)
        setKonten(kontoListe)
        setFehler(false)
      })
      .catch(() => setFehler(true))
      .finally(() => setLaedt(false))
  }, [monat, gefiltert, zusammen, aktivePerson])

  useEffect(laden, [laden])

  useEffect(() => {
    const anfrage = zusammen
      ? lese<VerlaufMonat[]>('/api/verlauf?monate=3&gemeinsam=true').catch(() =>
          lese<VerlaufMonat[]>('/api/verlauf?monate=3'),
        )
      : lese<VerlaufMonat[]>(
          gefiltert ? `/api/verlauf?monate=3&person=${encodeURIComponent(aktivePerson ?? '')}` : '/api/verlauf?monate=3',
        )
    anfrage
      .then((liste) => setVerlauf(Array.isArray(liste) && liste.length > 0 ? liste : null))
      .catch(() => setVerlauf(null))
  }, [gefiltert, zusammen, aktivePerson])

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
  const verlaufSkala = verlauf
    ? verlauf.reduce((max, v) => Math.max(max, Math.abs(v.differenz)), 1)
    : 1
  const verlaufSchnitt =
    verlauf && verlauf.length > 0
      ? verlauf.reduce((summe, v) => summe + v.differenz, 0) / verlauf.length
      : null

  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <button
          aria-label="Vormonat"
          onClick={() => setMonat(monatVerschieben(monat, -1))}
          className="-ml-3 flex h-9 w-9 items-center justify-center text-xl text-textschwach"
        >
          ‹
        </button>
        <h2 className="text-[15px] font-semibold">{monatLabel(monat)}</h2>
        <button
          aria-label="Folgemonat"
          onClick={() => setMonat(monatVerschieben(monat, 1))}
          className="-mr-3 flex h-9 w-9 items-center justify-center text-xl text-textschwach"
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
          <section className="mb-2">
            {zusammen ? (
              <>
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                  Gemeinsam
                </p>
                {ausgleich && (
                  <p
                    className={`betragszahl mt-1 text-[34px] font-semibold leading-[1.15] tracking-tight ${
                      ausgleich.vorschlag ? 'text-text' : 'text-gut'
                    }`}
                  >
                    {ausgleich.vorschlag ?? 'Alles ausgeglichen'}
                  </p>
                )}
              </>
            ) : (
              <>
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                  {gefiltert ? 'Mein Saldo' : 'Gesamtsaldo'}
                </p>
                <p
                  className={`betragszahl mt-1 text-[44px] font-semibold leading-none tracking-tight ${
                    gesamt < 0 ? 'text-schlecht' : 'text-text'
                  }`}
                >
                  {betragKompakt(gesamt, 1000000)}
                </p>
              </>
            )}
            <div className="mt-1.5 flex h-8 w-fit items-center gap-0.5 rounded-full bg-karte2 p-0.5">
              <button
                onClick={() => sichtSetzen('meine')}
                aria-pressed={gefiltert}
                className={`flex h-7 items-center rounded-full px-3 text-xs font-medium transition-colors ${
                  gefiltert ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
                }`}
              >
                {aktivePerson ?? 'Meine'}
              </button>
              <button
                onClick={() => sichtSetzen('zusammen')}
                aria-pressed={!gefiltert}
                className={`flex h-7 items-center rounded-full px-3 text-xs font-medium transition-colors ${
                  !gefiltert ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
                }`}
              >
                Zusammen
              </button>
            </div>
            {ausgleich && (
              <Link to="/ausgleich" className="-my-1 flex min-h-7 items-center gap-2 py-1">
                <span className="shrink-0 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                  Ausgleich
                </span>
                <span className="betragszahl truncate text-[13px] font-medium text-text">
                  {ausgleich.vorschlag ?? 'Alles ausgeglichen'}
                </span>
                <CaretRight size={13} className="ml-auto shrink-0 text-textschwach" />
              </Link>
            )}
            {!zusammen && konten.length === 0 && (
              <Link to="/einstellungen" className="mt-2 flex items-center gap-1 text-sm text-akzent">
                Erstes Konto anlegen
                <CaretRight size={12} />
              </Link>
            )}
          </section>

          {!zusammen && konten.length > 0 && (
            <div className="mb-2 flex gap-2 overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
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

          <div className={`mb-2 grid gap-2.5 ${budgetListe.length > 0 ? 'grid-cols-2' : 'grid-cols-1'}`}>
            <div className="rounded-2xl bg-karte p-3">
              <div className="flex items-baseline justify-between gap-2">
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">Ausgaben</p>
                <p className="betragszahl text-[17px] font-semibold text-schlecht">{betragKompakt(ausgaben)}</p>
              </div>
              <div className="mt-2 flex items-baseline justify-between gap-2">
                <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">Einnahmen</p>
                <p className="betragszahl text-[17px] font-semibold text-gut">{betragKompakt(einnahmen)}</p>
              </div>
            </div>
            {budgetListe.length > 0 && (
              <div className="flex flex-col justify-center rounded-2xl bg-karte p-3">
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
                  {betragKompakt(budgetIst)} von {betragKompakt(budgetLimit)}
                </p>
                {budgetUeber && (
                  <p className="betragszahl mt-1 text-xs font-medium text-warn">
                    {euro(budgetIst - budgetLimit)} drüber
                  </p>
                )}
              </div>
            )}
          </div>

          {verlauf !== null && verlaufSchnitt !== null && (!zusammen || !gemeinsamLokal) && (
            <section className="mb-1.5 flex h-6 items-center gap-3">
              <p className="shrink-0 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                Verlauf
              </p>
              <div className="flex items-center gap-2.5">
                {verlauf.map((v) => (
                  <TrendBalken key={v.monat} eintrag={v} skala={verlaufSkala} />
                ))}
              </div>
              <p className="betragszahl ml-auto shrink-0 text-[13px] font-medium text-textschwach">
                Ø {verlaufSchnitt >= 0 ? '+' : ''}
                {betragKompakt(verlaufSchnitt)}
              </p>
            </section>
          )}

          {kategorien.length > 0 && (
          <section className="mb-2">
              <div className="mb-1 flex items-baseline justify-between">
                <h3 className="text-[13px] font-semibold">Kategorien</h3>
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
              <div className="space-y-1">
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
              <h3 className="text-[13px] font-semibold">Buchungen</h3>
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
                    className={`flex items-center justify-between gap-3 py-1.5 ${
                      i < liste.length - 1 ? 'border-b border-strich' : ''
                    }`}
                  >
                    <div className="flex min-w-0 items-center gap-3">
                      <KategorieIconTile name={eintrag.kategorie} />
                      <div className="min-w-0">
                        <p className="truncate text-[15px] font-semibold leading-tight text-text">
                          {eintrag.kategorie}
                        </p>
                        <p className="mt-0.5 flex flex-wrap items-center gap-1.5 text-xs leading-tight text-textschwach">
                          <span>{datumKurz(eintrag.datum)}</span>
                          <span aria-hidden="true">·</span>
                          {eintrag.gemeinsam ? (
                            <>
                              <GeteiltPill />
                              {eintrag.zahler && (
                                <>
                                  <span aria-hidden="true">·</span>
                                  <span>{eintrag.zahler}</span>
                                </>
                              )}
                            </>
                          ) : (
                            <span>{eintrag.person}</span>
                          )}
                        </p>
                      </div>
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
