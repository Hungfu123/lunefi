import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { CaretDown, Moon, Question, Sun } from '@phosphor-icons/react'
import { ApiFehler, lese, schreibe, type KategorieInfo, type Konto } from './types'
import { KategorieIconTile } from './kategorieIcon'
import { HALTUNG_LABEL, HaltungsSelector } from './HaltungsSelector'
import { HilfeModal } from './HilfeModal'
import { betragAusEingabe, euro, monatAlsString } from './format'
import { useApp } from './AppKontext'

export function EinstellungenSeite() {
  const { personen, aktivePerson } = useApp()
  const reduziert = useReducedMotion()
  const [kategorien, setKategorien] = useState<KategorieInfo[]>([])
  const [konten, setKonten] = useState<Konto[]>([])
  const [kontoName, setKontoName] = useState('')
  const [kontoSaldo, setKontoSaldo] = useState('')
  const [offeneKategorie, setOffeneKategorie] = useState<string | null>(null)
  const [theme, setTheme] = useState<'mond' | 'sonne'>(
    () => (document.documentElement.dataset.theme === 'sonne' ? 'sonne' : 'mond'),
  )

  const themeSetzen = (neu: 'mond' | 'sonne') => {
    setTheme(neu)
    document.documentElement.dataset.theme = neu
    localStorage.setItem('lunefi:theme', neu)
  }
  const [budgets, setBudgets] = useState<Record<string, string>>({})
  const [meldung, setMeldung] = useState<string | null>(null)
  const [fehler, setFehler] = useState<string | null>(null)
  const [hilfeOffen, setHilfeOffen] = useState(false)

  const laden = useCallback(() => {
    lese<KategorieInfo[]>('/api/kategorien')
      .then((liste) => {
        setKategorien(liste)
        const verzeichnis: Record<string, string> = {}
        liste.forEach((k) => (verzeichnis[k.name] = ''))
        setBudgets(verzeichnis)
      })
      .catch(() => setKategorien([]))
    lese<Konto[]>(
      aktivePerson ? `/api/konten?person=${encodeURIComponent(aktivePerson)}` : '/api/konten',
    )
      .then(setKonten)
      .catch(() => setKonten([]))
  }, [aktivePerson])

  useEffect(laden, [laden])

  const splitSetzen = async (kategorie: string, art: 'HALBHALB' | 'ANTEILE') => {
    try {
      if (art === 'HALBHALB') {
        await schreibe(`/api/kategorien/${encodeURIComponent(kategorie)}/split`, { art }, 'PUT')
      } else {
        const namen = personen.map((p) => p.name)
        const anteile: Record<string, number> = {}
        namen.forEach((n) => (anteile[n] = Math.floor(100 / namen.length)))
        const rest = 100 - Math.floor(100 / namen.length) * namen.length
        if (namen[0]) anteile[namen[0]] += rest
        await schreibe(`/api/kategorien/${encodeURIComponent(kategorie)}/split`, { art, anteile }, 'PUT')
      }
      setMeldung(`Split für „${kategorie}" gespeichert.`)
      laden()
    } catch (err) {
      setFehler(err instanceof ApiFehler ? err.message : 'Speichern fehlgeschlagen.')
    }
  }

  const budgetSetzen = async (kategorie: string, e: FormEvent) => {
    e.preventDefault()
    const wert = betragAusEingabe(budgets[kategorie] ?? '')
    if (wert === null || wert <= 0) {
      setFehler('Bitte ein Limit über 0 eintragen.')
      return
    }
    try {
      await schreibe('/api/budgets', { monat: monatAlsString(new Date()), kategorie, limit: wert }, 'PUT')
      setMeldung(`Budget für „${kategorie}" gespeichert.`)
      setBudgets((alt) => ({ ...alt, [kategorie]: '' }))
    } catch (err) {
      setFehler(err instanceof ApiFehler ? err.message : 'Speichern fehlgeschlagen.')
    }
  }

  const kontoAnlegen = async (e: FormEvent) => {
    e.preventDefault()
    const saldo = betragAusEingabe(kontoSaldo)
    if (!kontoName.trim() || saldo === null || !aktivePerson) {
      setFehler('Name und Startsaldo eintragen.')
      return
    }
    try {
      await schreibe('/api/konten', { name: kontoName.trim(), startSaldo: saldo, person: aktivePerson })
      setMeldung(`Konto „${kontoName.trim()}" angelegt.`)
      setKontoName('')
      setKontoSaldo('')
      laden()
    } catch (err) {
      setFehler(err instanceof ApiFehler ? err.message : 'Konto anlegen fehlgeschlagen.')
    }
  }

  return (
    <div>
      <h2 className="mb-4 text-lg font-semibold">Einstellungen</h2>

      <section className="mb-6">
        <h3 className="mb-2 text-sm font-medium text-textschwach">Erscheinungsbild</h3>
        <div className="flex gap-2">
          <button
            onClick={() => themeSetzen('mond')}
            className={`flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl px-3 transition-colors ${
              theme === 'mond' ? 'bg-akzentstark text-akzenttext' : 'bg-karte text-textschwach'
            }`}
          >
            <Moon size={18} weight={theme === 'mond' ? 'fill' : 'regular'} />
            Dunkel
          </button>
          <button
            onClick={() => themeSetzen('sonne')}
            className={`flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl px-3 transition-colors ${
              theme === 'sonne' ? 'bg-akzentstark text-akzenttext' : 'bg-karte text-textschwach'
            }`}
          >
            <Sun size={18} weight={theme === 'sonne' ? 'fill' : 'regular'} />
            Hell
          </button>
        </div>
      </section>

      <section className="mb-6">
        <h3 className="mb-2 text-sm font-medium text-textschwach">Hilfe</h3>
        <button
          onClick={() => setHilfeOffen(true)}
          className="flex min-h-11 w-full items-center gap-3 rounded-xl bg-karte px-4 text-sm text-text"
        >
          <Question size={18} className="shrink-0" />
          So funktioniert LuneFi
        </button>
      </section>

      <section className="mb-6">
        <h3 className="mb-2 text-sm font-medium text-textschwach">Konten</h3>
        {konten.length > 0 && (
          <ul className="mb-3 space-y-2">
            {konten.map((k) => (
              <li key={k.id} className="flex items-center justify-between rounded-xl bg-karte px-4 py-3">
                <span className="truncate text-sm">
                  {k.name} <span className="text-textschwach">· {k.person}</span>
                </span>
                <span className="betragszahl text-sm font-semibold">{euro(k.saldo)}</span>
              </li>
            ))}
          </ul>
        )}
        <form onSubmit={kontoAnlegen} className="flex items-center gap-2">
          <input
            value={kontoName}
            onChange={(e) => setKontoName(e.target.value)}
            placeholder="z. B. Girokonto"
            className="min-w-0 flex-1 rounded-xl bg-karte px-3 py-2.5 outline-none ring-strich focus:ring-2"
          />
          <input
            value={kontoSaldo}
            onChange={(e) => setKontoSaldo(e.target.value)}
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
        <h3 className="mb-2 text-sm font-medium text-textschwach">Kategorien</h3>
        <p className="mb-2 text-[12px] text-textschwach">
          Haltung fürs Budget-Coaching — euer Partner sieht sie ebenfalls.
        </p>
        {kategorien.length === 0 && (
          <p className="text-sm text-textschwach">Noch keine Kategorien — leg den ersten Eintrag an.</p>
        )}
        <ul className="space-y-1">
          {kategorien.map((k) => {
            const offen = offeneKategorie === k.name
            return (
              <li key={k.name} className="overflow-hidden rounded-xl bg-karte px-3.5">
                <button
                  type="button"
                  aria-expanded={offen}
                  onClick={() => setOffeneKategorie(offen ? null : k.name)}
                  className="flex h-8 w-full items-center gap-3 text-left"
                >
                  <KategorieIconTile name={k.name} groesse={22} iconGroesse={13} />
                  <span className="min-w-0 flex-1 truncate text-[14px] font-semibold text-text">
                    {k.name}
                  </span>
                  <span className="flex shrink-0 items-center gap-1">
                    <span className="rounded-full bg-karte2 px-2 py-0.5 text-[11px] text-textschwach">
                      {k.split.art === 'ANTEILE' ? 'Anteile' : '50/50'}
                    </span>
                    {k.haltung !== 'FLEXIBEL' && (
                      <span className="rounded-full bg-akzent/15 px-2 py-0.5 text-[11px] text-akzent">
                        {HALTUNG_LABEL[k.haltung]}
                      </span>
                    )}
                    <CaretDown
                      size={12}
                      aria-hidden="true"
                      className={`shrink-0 text-textschwach transition-transform ${offen ? '' : 'rotate-180'}`}
                    />
                  </span>
                </button>
                <AnimatePresence initial={false}>
                  {offen && (
                    <motion.div
                      className="overflow-hidden"
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={reduziert ? { duration: 0 } : { duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
                    >
                      <div className="space-y-2.5 px-0.5 pb-3 pt-1">
                        <div>
                          <p className="mb-1 text-[10px] font-medium uppercase tracking-[0.08em] text-textschwach">
                            Aufteilung
                          </p>
                          <div className="flex gap-1.5">
                            <button
                              onClick={() => splitSetzen(k.name, 'HALBHALB')}
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
                              onClick={() => splitSetzen(k.name, 'ANTEILE')}
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
                          <p className="mb-1 text-[10px] font-medium uppercase tracking-[0.08em] text-textschwach">
                            Haltung
                          </p>
                          <HaltungsSelector
                            kategorie={k.name}
                            haltung={k.haltung}
                            onHaltung={(wert) =>
                              setKategorien((alt) =>
                                alt.map((x) => (x.name === k.name ? { ...x, haltung: wert } : x)),
                              )
                            }
                          />
                        </div>
                        <div>
                          <p className="mb-1 text-[10px] font-medium uppercase tracking-[0.08em] text-textschwach">
                            Budget-Limit
                          </p>
                          <form onSubmit={(e) => budgetSetzen(k.name, e)} className="flex items-center gap-2">
                            <input
                              value={budgets[k.name] ?? ''}
                              onChange={(e) => setBudgets((alt) => ({ ...alt, [k.name]: e.target.value }))}
                              inputMode="decimal"
                              placeholder="Limit in Euro"
                              className="min-w-0 flex-1 rounded-xl bg-karte2 px-3 py-2 outline-none ring-strich focus:ring-2"
                            />
                            <button
                              type="submit"
                              className="flex min-h-9 shrink-0 items-center rounded-xl bg-akzentstark px-3 text-xs font-semibold text-akzenttext"
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

      {meldung && <p className="mt-4 text-sm text-gut">{meldung}</p>}
      {fehler && <p className="mt-4 text-sm text-schlecht">{fehler}</p>}

      <p className="mt-8 text-center text-xs text-textschwach">
        LuneFi · {personen.map((p) => p.name).join(' & ')}
      </p>

      <HilfeModal offen={hilfeOffen} zu={() => setHilfeOffen(false)} />
    </div>
  )
}
