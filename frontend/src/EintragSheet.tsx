import { useEffect, useState, type FormEvent } from 'react'
import { ApiFehler, lese, schreibe, type Eintrag, type EintragNeu, type KategorieInfo } from './types'
import { betragAusEingabe, heuteIso } from './format'
import { useApp } from './AppKontext'

export function EintragSheet({
  offen,
  zu,
  onErfasst,
}: {
  offen: boolean
  zu: () => void
  onErfasst: () => void
}) {
  const { personen, aktivePerson } = useApp()
  const [betrag, setBetrag] = useState('')
  const [kategorie, setKategorie] = useState('')
  const [kategorien, setKategorien] = useState<string[]>([])
  const [datum, setDatum] = useState(heuteIso())
  const [gemeinsam, setGemeinsam] = useState(false)
  const [person, setPerson] = useState<string | null>(null)
  const [zahler, setZahler] = useState<string | null>(null)
  const [fehler, setFehler] = useState<string | null>(null)
  const [laeuft, setLaeuft] = useState(false)

  useEffect(() => {
    if (!offen) return
    lese<KategorieInfo[]>('/api/kategorien')
      .then((liste) => setKategorien(liste.map((k) => k.name)))
      .catch(() => setKategorien([]))
    setBetrag('')
    setKategorie('')
    setDatum(heuteIso())
    setGemeinsam(false)
    setPerson(aktivePerson)
    setZahler(aktivePerson)
    setFehler(null)
  }, [offen, aktivePerson])

  if (!offen) return null

  const absenden = async (e: FormEvent) => {
    e.preventDefault()
    const wert = betragAusEingabe(betrag)
    if (wert === null || wert === 0) {
      setFehler('Bitte einen Betrag ungleich 0 eintragen.')
      return
    }
    if (!kategorie.trim()) {
      setFehler('Bitte eine Kategorie wählen oder eingeben.')
      return
    }
    const body: EintragNeu = {
      datum,
      betrag: wert,
      kategorie: kategorie.trim(),
    }
    if (gemeinsam) {
      body.gemeinsam = true
      body.zahler = zahler ?? undefined
    } else {
      body.person = person ?? undefined
    }
    setLaeuft(true)
    try {
      await schreibe<Eintrag>('/api/eintraege', body)
      zu()
      onErfasst()
    } catch (err) {
      setFehler(err instanceof ApiFehler ? err.message : 'Das hat nicht geklappt.')
    } finally {
      setLaeuft(false)
    }
  }

  return (
    <div className="fixed inset-0 z-30 flex flex-col justify-end">
      <button aria-label="Schließen" className="absolute inset-0 bg-black/60" onClick={zu} />
      <form
        onSubmit={absenden}
        className="relative mx-auto w-full max-w-[560px] rounded-t-3xl border-t border-strich bg-karte px-5 pb-8 pt-5"
      >
        <div className="mx-auto mb-5 h-1 w-10 rounded-full bg-strich" />
        <label className="mb-1 block text-xs text-textschwach">Betrag (minus = Ausgabe)</label>
        <input
          value={betrag}
          onChange={(e) => setBetrag(e.target.value)}
          inputMode="decimal"
          placeholder="-12,50"
          autoFocus
          className="mb-4 w-full rounded-xl bg-karte2 px-4 py-3 text-2xl font-semibold outline-none ring-strich focus:ring-2"
        />

        <label className="mb-1 block text-xs text-textschwach">Kategorie</label>
        <div className="mb-2 flex flex-wrap gap-2">
          {kategorien.map((k) => (
            <button
              type="button"
              key={k}
              onClick={() => setKategorie(k)}
              className={`rounded-full px-3 py-1.5 text-sm transition-colors ${
                kategorie === k ? 'bg-akzentstark text-white' : 'bg-karte2 text-textschwach'
              }`}
            >
              {k}
            </button>
          ))}
        </div>
        <input
          value={kategorie}
          onChange={(e) => setKategorie(e.target.value)}
          placeholder="oder neu eingeben"
          className="mb-4 w-full rounded-xl bg-karte2 px-4 py-3 outline-none ring-strich focus:ring-2"
        />

        <label className="mb-1 block text-xs text-textschwach">Datum</label>
        <input
          type="date"
          value={datum}
          onChange={(e) => setDatum(e.target.value)}
          className="mb-4 w-full rounded-xl bg-karte2 px-4 py-3 outline-none ring-strich focus:ring-2"
        />

        <div className="mb-4 flex items-center justify-between rounded-xl bg-karte2 px-4 py-3">
          <span className="font-medium">Gemeinsame Ausgabe</span>
          <button
            type="button"
            role="switch"
            aria-label="Gemeinsame Ausgabe"
            aria-checked={gemeinsam}
            onClick={() => setGemeinsam(!gemeinsam)}
            className={`h-7 w-12 rounded-full transition-colors ${gemeinsam ? 'bg-akzentstark' : 'bg-strich'}`}
          >
            <span
              className={`block h-7 w-7 rounded-full bg-white transition-transform ${
                gemeinsam ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>

        <label className="mb-1 block text-xs text-textschwach">{gemeinsam ? 'Bezahlt von' : 'Person'}</label>
        <div className="mb-4 flex gap-2">
          {personen.map((p) => {
            const gewaehlt = gemeinsam ? zahler === p.name : person === p.name
            return (
              <button
                type="button"
                key={p.id}
                onClick={() => (gemeinsam ? setZahler(p.name) : setPerson(p.name))}
                className={`flex-1 rounded-xl px-3 py-2.5 transition-colors ${
                  gewaehlt ? 'bg-akzentstark text-white' : 'bg-karte2 text-textschwach'
                }`}
              >
                {p.name}
              </button>
            )
          })}
        </div>

        {fehler && <p className="mb-3 text-sm text-schlecht">{fehler}</p>}

        <button
          type="submit"
          disabled={laeuft}
          className="w-full rounded-xl bg-akzentstark py-3 font-semibold text-white transition-transform active:scale-[0.98] disabled:opacity-50"
        >
          Speichern
        </button>
    </form>
  </div>
  )
}

