import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Moon, Sun } from '@phosphor-icons/react'
import { ApiFehler, lese, schreibe, type KategorieInfo, type Konto } from './types'
import { betragAusEingabe, euro, monatAlsString } from './format'
import { useApp } from './AppKontext'

export function EinstellungenSeite() {
  const { personen, aktivePerson, setAktivePerson } = useApp()
  const [kategorien, setKategorien] = useState<KategorieInfo[]>([])
  const [konten, setKonten] = useState<Konto[]>([])
  const [kontoName, setKontoName] = useState('')
  const [kontoSaldo, setKontoSaldo] = useState('')
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

  const laden = useCallback(() => {
    lese<KategorieInfo[]>('/api/kategorien')
      .then((liste) => {
        setKategorien(liste)
        const verzeichnis: Record<string, string> = {}
        liste.forEach((k) => (verzeichnis[k.name] = ''))
        setBudgets(verzeichnis)
      })
      .catch(() => setKategorien([]))
    lese<Konto[]>('/api/konten')
      .then(setKonten)
      .catch(() => setKonten([]))
  }, [])

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
              theme === 'mond' ? 'bg-akzentstark text-white' : 'bg-karte text-textschwach'
            }`}
          >
            <Moon size={18} weight={theme === 'mond' ? 'fill' : 'regular'} />
            Mond (dunkel)
          </button>
          <button
            onClick={() => themeSetzen('sonne')}
            className={`flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl px-3 transition-colors ${
              theme === 'sonne' ? 'bg-akzentstark text-white' : 'bg-karte text-textschwach'
            }`}
          >
            <Sun size={18} weight={theme === 'sonne' ? 'fill' : 'regular'} />
            Sonne (hell)
          </button>
        </div>
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
                <span className="text-sm font-semibold">{euro(k.saldo)}</span>
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
          <button type="submit" className="flex min-h-11 shrink-0 items-center rounded-xl bg-akzentstark px-3 text-sm font-semibold text-white">
            Anlegen
          </button>
        </form>
        <p className="mt-2 text-xs text-textschwach">
          Später ersetzt der FinTS-Import die manuellen Konten — der Startsaldo wird dann durch echte Kontostände abgelöst.
        </p>
      </section>

      <section className="mb-6">
        <h3 className="mb-2 text-sm font-medium text-textschwach">Wer bin ich?</h3>
        <div className="flex gap-2">
          {personen.map((p) => (
            <button
              key={p.id}
              onClick={() => setAktivePerson(p.name)}
              className={`flex min-h-11 flex-1 items-center justify-center rounded-xl px-3 ${
                aktivePerson === p.name ? 'bg-akzentstark text-white' : 'bg-karte'
              }`}
            >
              {p.name}
            </button>
          ))}
        </div>
      </section>

      <section className="mb-6">
        <h3 className="mb-2 text-sm font-medium text-textschwach">Aufteilung je Kategorie</h3>
        {kategorien.length === 0 && (
          <p className="text-sm text-textschwach">Kategorien entstehen, sobald ihr Einträge erfasst.</p>
        )}
        <ul className="space-y-2">
          {kategorien.map((k) => (
            <li key={k.name} className="rounded-xl bg-karte px-4 py-3">
              <div className="flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{k.name}</p>
                  <p className="text-xs text-textschwach">
                    {k.split.art === 'ANTEILE' && k.split.anteile
                      ? Object.entries(k.split.anteile)
                          .map(([n, a]) => `${n} ${a}%`)
                          .join(' · ')
                      : '50/50'}
                  </p>
                </div>
                <div className="flex shrink-0 gap-2">
                  <button
                    onClick={() => splitSetzen(k.name, 'HALBHALB')}
                    className="flex min-h-11 items-center rounded-lg bg-karte2 px-3 text-xs"
                  >
                    50/50
                  </button>
                  <button
                    onClick={() => splitSetzen(k.name, 'ANTEILE')}
                    className="flex min-h-11 items-center rounded-lg bg-karte2 px-3 text-xs"
                  >
                    Anteile
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
        <p className="mt-2 text-xs text-textschwach">
          „Anteile" verteilt zunächst gleichmäßig — ihr könnt die Prozente später jederzeit anpassen.
        </p>
      </section>

      <section>
        <h3 className="mb-2 text-sm font-medium text-textschwach">Budget-Limit setzen</h3>
        <ul className="space-y-2">
          {kategorien.map((k) => (
            <li key={k.name}>
              <form onSubmit={(e) => budgetSetzen(k.name, e)} className="flex items-center gap-2">
                <span className="w-28 shrink-0 truncate text-sm">{k.name}</span>
                <input
                  value={budgets[k.name] ?? ''}
                  onChange={(e) => setBudgets((alt) => ({ ...alt, [k.name]: e.target.value }))}
                  inputMode="decimal"
                  placeholder="Limit in Euro"
                  className="min-w-0 flex-1 rounded-xl bg-karte px-3 py-2.5 outline-none ring-strich focus:ring-2"
                />
                <button type="submit" className="flex min-h-11 shrink-0 items-center rounded-xl bg-akzentstark px-3 text-sm font-semibold text-white">
                  Speichern
                </button>
              </form>
            </li>
          ))}
        </ul>
      </section>

      {meldung && <p className="mt-4 text-sm text-gut">{meldung}</p>}
      {fehler && <p className="mt-4 text-sm text-schlecht">{fehler}</p>}

      <p className="mt-8 text-center text-xs text-textschwach">
        LuneFi — mit {personen.map((p) => p.name).join(' und ')} · alles in Euro
      </p>
    </div>
  )
}
