import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { ApiFehler, lese, schreibe, type KategorieInfo } from './types'
import { betragAusEingabe, monatAlsString } from './format'
import { useApp } from './AppKontext'

export function EinstellungenSeite() {
  const { personen, aktivePerson, setAktivePerson } = useApp()
  const [kategorien, setKategorien] = useState<KategorieInfo[]>([])
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

  return (
    <div>
      <h2 className="mb-4 text-lg font-semibold">Einstellungen</h2>

      <section className="mb-6">
        <h3 className="mb-2 text-sm font-medium text-textschwach">Wer bin ich?</h3>
        <div className="flex gap-2">
          {personen.map((p) => (
            <button
              key={p.id}
              onClick={() => setAktivePerson(p.name)}
              className={`flex-1 rounded-xl px-3 py-2.5 ${
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
                    className="rounded-lg bg-karte2 px-3 py-2 text-xs"
                  >
                    50/50
                  </button>
                  <button
                    onClick={() => splitSetzen(k.name, 'ANTEILE')}
                    className="rounded-lg bg-karte2 px-3 py-2 text-xs"
                  >
                    Anteile
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
        <p className="mt-2 text-xs text-textschwach">
          „Anteile" verteilt zunächst gleichmäßig — danach feinjustieren über die API möglich.
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
                  placeholder="Limit"
                  className="min-w-0 flex-1 rounded-xl bg-karte px-3 py-2.5 outline-none ring-strich focus:ring-2"
                />
                <button type="submit" className="shrink-0 rounded-xl bg-akzentstark px-3 py-2.5 text-sm font-semibold text-white">
                  OK
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
