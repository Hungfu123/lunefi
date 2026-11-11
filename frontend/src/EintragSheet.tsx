import { useEffect, useState, type FormEvent } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { ApiFehler, lese, schreibe, type Eintrag, type EintragNeu, type KategorieInfo, type Konto } from './types'
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
  const reduziert = useReducedMotion()
  const [art, setArt] = useState<'ausgabe' | 'einnahme'>('ausgabe')
  const [betrag, setBetrag] = useState('')
  const [kategorie, setKategorie] = useState('')
  const [kategorien, setKategorien] = useState<string[]>([])
  const [konten, setKonten] = useState<Konto[]>([])
  const [konto, setKonto] = useState<string | null>(null)
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
    lese<Konto[]>('/api/konten')
      .then(setKonten)
      .catch(() => setKonten([]))
    setArt('ausgabe')
    setBetrag('')
    setKategorie('')
    setDatum(heuteIso())
    setGemeinsam(false)
    setPerson(aktivePerson)
    setZahler(aktivePerson)
    setKonto(null)
    setFehler(null)
  }, [offen, aktivePerson])

  const zustaendigePerson = gemeinsam ? zahler : person
  const eigeneKonten = konten.filter((k) => k.person === zustaendigePerson)

  const absenden = async (e: FormEvent) => {
    e.preventDefault()
    const wert = betragAusEingabe(betrag)
    if (wert === null || wert === 0) {
      setFehler('Bitte einen Betrag ungleich 0 eintragen.')
      return
    }
    const gerichtet = art === 'ausgabe' ? -wert : wert
    if (!kategorie.trim()) {
      setFehler('Bitte eine Kategorie wählen oder eingeben.')
      return
    }
    const body: EintragNeu = {
      datum,
      betrag: gerichtet,
      kategorie: kategorie.trim(),
    }
    if (gemeinsam) {
      body.gemeinsam = true
      body.zahler = zahler ?? undefined
    } else {
      body.person = person ?? undefined
    }
    if (konto) {
      body.konto = konto
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
    <AnimatePresence>
      {offen && (
        <div className="fixed inset-0 z-30 flex flex-col justify-end">
          <motion.button
            aria-label="Schließen"
            className="absolute inset-0 bg-black/60"
            onClick={zu}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.18 }}
          />
          <motion.form
            onSubmit={absenden}
            className="relative mx-auto w-full max-w-[560px] rounded-t-3xl border-t border-strich bg-karte px-5 pb-8 pt-5"
            initial={{ transform: 'translateY(100%)' }}
            animate={{ transform: 'translateY(0%)' }}
            exit={{ transform: 'translateY(100%)' }}
            transition={
              reduziert ? { duration: 0 } : { type: 'spring', duration: 0.5, bounce: 0.2 }
            }
            drag="y"
            dragConstraints={{ top: 0, bottom: 0 }}
            dragElastic={{ top: 0, bottom: 0.6 }}
            onDragEnd={(_, info) => {
              if (info.offset.y > 120 || info.velocity.y > 500) {
                zu()
              }
            }}
          >
            <div className="mx-auto mb-5 h-1 w-10 rounded-full bg-strich" />
            <div className="mb-4 flex gap-2 rounded-xl bg-karte2 p-1">
              <button
                type="button"
                onClick={() => setArt('ausgabe')}
                className={`flex min-h-11 flex-1 items-center justify-center rounded-lg text-sm font-semibold transition-colors ${
                  art === 'ausgabe' ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
                }`}
              >
                Ausgabe
              </button>
              <button
                type="button"
                onClick={() => setArt('einnahme')}
                className={`flex min-h-11 flex-1 items-center justify-center rounded-lg text-sm font-semibold transition-colors ${
                  art === 'einnahme' ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
                }`}
              >
                Einnahme
              </button>
            </div>

            <label className="mb-1 block text-xs text-textschwach">Betrag</label>
            <input
              value={betrag}
              onChange={(e) => setBetrag(e.target.value)}
              inputMode="decimal"
              placeholder="12,50"
              className="mb-4 w-full rounded-xl bg-karte2 px-4 py-3 text-2xl font-semibold outline-none ring-strich focus:ring-2"
            />

            <label className="mb-1 block text-xs text-textschwach">Kategorie</label>
            <div className="mb-2 flex flex-wrap gap-2">
              {kategorien.map((k) => (
                <button
                  type="button"
                  key={k}
                  onClick={() => setKategorie(k)}
                  className={`flex min-h-11 items-center rounded-full px-4 text-sm transition-colors ${
                    kategorie === k ? 'bg-akzentstark text-akzenttext' : 'bg-karte2 text-textschwach'
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
                className={`relative h-7 w-12 rounded-full transition-colors before:absolute before:-inset-3 before:content-[''] ${
                  gemeinsam ? 'bg-akzentstark' : 'bg-strich'
                }`}
              >
                <span
                  className={`block h-7 w-7 rounded-full bg-white transition-transform duration-200 ${
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
                    className={`flex min-h-11 flex-1 items-center justify-center rounded-xl px-3 transition-colors ${
                      gewaehlt ? 'bg-akzentstark text-akzenttext' : 'bg-karte2 text-textschwach'
                    }`}
                  >
                    {p.name}
                  </button>
                )
              })}
            </div>

            {eigeneKonten.length > 0 && (
              <div className="mb-4">
                <label className="mb-1 block text-xs text-textschwach">Konto (optional)</label>
                <div className="flex flex-wrap gap-2">
                  {eigeneKonten.map((k) => (
                    <button
                      type="button"
                      key={k.id}
                      onClick={() => setKonto(konto === k.name ? null : k.name)}
                      className={`flex min-h-11 items-center rounded-full px-4 text-sm transition-colors ${
                        konto === k.name ? 'bg-akzentstark text-akzenttext' : 'bg-karte2 text-textschwach'
                      }`}
                    >
                      {k.name}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {fehler && <p className="mb-3 text-sm text-schlecht">{fehler}</p>}

            <button
              type="submit"
              disabled={laeuft}
              className="w-full rounded-xl bg-akzentstark py-3 font-semibold text-akzenttext transition-transform active:scale-[0.98] disabled:opacity-50"
            >
              Speichern
            </button>
          </motion.form>
        </div>
      )}
    </AnimatePresence>
  )
}
