import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { ApiFehler, lese, schreibe, type Sparziel } from './types'
import { betragAusEingabe, euro, heuteIso } from './format'
import { useApp } from './AppKontext'

function Dialog({
  titel,
  zu,
  children,
}: {
  titel: string
  zu: () => void
  children: React.ReactNode
}) {
  const reduziert = useReducedMotion()
  return (
    <div className="fixed inset-0 z-30 flex items-center justify-center p-5">
      <motion.button
        aria-label="Schließen"
        className="absolute inset-0 bg-black/60"
        onClick={zu}
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 0.18 }}
      />
      <motion.div
        className="relative w-full max-w-xs rounded-2xl border border-strich bg-karte p-5"
        initial={{ opacity: 0, transform: 'scale(0.94)' }}
        animate={{ opacity: 1, transform: 'scale(1)' }}
        exit={{ opacity: 0, transform: 'scale(0.94)' }}
        transition={reduziert ? { duration: 0 } : { type: 'spring', duration: 0.35, bounce: 0.2 }}
      >
        <h3 className="mb-4 font-semibold">{titel}</h3>
        {children}
      </motion.div>
    </div>
  )
}

export function SparzieleSeite() {
  const { personen, aktivePerson } = useApp()
  const [ziele, setZiele] = useState<Sparziel[]>([])
  const [laedt, setLaedt] = useState(true)
  const [fehler, setFehler] = useState(false)
  const [neuOffen, setNeuOffen] = useState(false)
  const [einzahlenOffen, setEinzahlenOffen] = useState<Sparziel | null>(null)

  const laden = useCallback(() => {
    setLaedt(true)
    lese<Sparziel[]>('/api/sparziele')
      .then((liste) => {
        setZiele(liste)
        setFehler(false)
      })
      .catch(() => setFehler(true))
      .finally(() => setLaedt(false))
  }, [])

  useEffect(laden, [laden])

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold">Sparziele</h2>
        <button
          onClick={() => setNeuOffen(true)}
          className="rounded-xl bg-akzentstark px-4 py-2.5 text-sm font-semibold text-white active:scale-[0.98]"
        >
          Neues Ziel
        </button>
      </div>

      {laedt && <div className="h-24 animate-pulse rounded-2xl bg-karte" />}

      {fehler && !laedt && <p className="text-schlecht">Die Sparziele konnten nicht geladen werden.</p>}

      {!laedt && !fehler && ziele.length === 0 && (
        <p className="text-sm text-textschwach">Noch kein Sparziel — legt gemeinsam eines an.</p>
      )}

      <ul className="space-y-3">
        {ziele.map((ziel) => {
          const anteil = Math.min(100, (ziel.eingezahlt / ziel.zielbetrag) * 100)
          return (
            <li key={ziel.id} className="rounded-2xl bg-karte p-4">
              <div className="flex items-baseline justify-between">
                <span className="font-medium">{ziel.name}</span>
                {ziel.erreicht ? (
                  <span className="rounded-full bg-gut/15 px-2 py-0.5 text-xs text-gut">erreicht</span>
                ) : (
                  <button
                    onClick={() => setEinzahlenOffen(ziel)}
                    className="text-sm font-semibold text-akzent active:scale-[0.98]"
                  >
                    Einzahlen
                  </button>
                )}
              </div>
              <div className="mt-3 h-2.5 overflow-hidden rounded-full bg-karte2">
                <div className="h-full rounded-full bg-akzent" style={{ width: `${anteil}%` }} />
              </div>
              <p className="mt-1.5 text-xs text-textschwach">
                {euro(ziel.eingezahlt)} von {euro(ziel.zielbetrag)}
              </p>
            </li>
          )
        })}
      </ul>

      <AnimatePresence>
        {neuOffen && (
        <NeuesZielDialog
          zu={() => setNeuOffen(false)}
          erstellt={() => {
            setNeuOffen(false)
            laden()
          }}
        />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {einzahlenOffen && (
        <EinzahlDialog
          ziel={einzahlenOffen}
          personen={personen.map((p) => p.name)}
          standard={aktivePerson}
          zu={() => setEinzahlenOffen(null)}
          eingezahlt={() => {
            setEinzahlenOffen(null)
            laden()
          }}
        />
        )}
      </AnimatePresence>
    </div>
  )
}

function NeuesZielDialog({ zu, erstellt }: { zu: () => void; erstellt: () => void }) {
  const [name, setName] = useState('')
  const [zielbetrag, setZielbetrag] = useState('')
  const [fehler, setFehler] = useState<string | null>(null)
  const [laeuft, setLaeuft] = useState(false)

  const absenden = async (e: FormEvent) => {
    e.preventDefault()
    const betrag = betragAusEingabe(zielbetrag)
    if (!name.trim() || betrag === null || betrag <= 0) {
      setFehler('Name und Zielbetrag (über 0) fehlen.')
      return
    }
    setLaeuft(true)
    try {
      await schreibe('/api/sparziele', { name: name.trim(), zielbetrag: betrag })
      erstellt()
    } catch (err) {
      setFehler(err instanceof ApiFehler ? err.message : 'Das hat nicht geklappt.')
    } finally {
      setLaeuft(false)
    }
  }

  return (
    <Dialog titel="Neues Sparziel" zu={zu}>
      <form onSubmit={absenden} className="flex flex-col gap-3">
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="z. B. Urlaub"
          autoFocus
          className="rounded-xl bg-karte2 px-4 py-3 outline-none ring-strich focus:ring-2"
        />
        <input
          value={zielbetrag}
          onChange={(e) => setZielbetrag(e.target.value)}
          inputMode="decimal"
          placeholder="2000,00"
          className="rounded-xl bg-karte2 px-4 py-3 outline-none ring-strich focus:ring-2"
        />
        {fehler && <p className="text-sm text-schlecht">{fehler}</p>}
        <button
          type="submit"
          disabled={laeuft}
          className="rounded-xl bg-akzentstark py-3 font-semibold text-white disabled:opacity-50"
        >
          Anlegen
        </button>
      </form>
    </Dialog>
  )
}

function EinzahlDialog({
  ziel,
  personen,
  standard,
  zu,
  eingezahlt,
}: {
  ziel: Sparziel
  personen: string[]
  standard: string | null
  zu: () => void
  eingezahlt: () => void
}) {
  const [betrag, setBetrag] = useState('')
  const [person, setPerson] = useState(standard ?? personen[0] ?? '')
  const [fehler, setFehler] = useState<string | null>(null)
  const [laeuft, setLaeuft] = useState(false)

  const absenden = async (e: FormEvent) => {
    e.preventDefault()
    const wert = betragAusEingabe(betrag)
    if (wert === null || wert <= 0) {
      setFehler('Bitte einen Betrag über 0 eintragen.')
      return
    }
    setLaeuft(true)
    try {
      await schreibe(`/api/sparziele/${ziel.id}/einzahlungen`, { betrag: wert, person, datum: heuteIso() })
      eingezahlt()
    } catch (err) {
      setFehler(err instanceof ApiFehler ? err.message : 'Das hat nicht geklappt.')
    } finally {
      setLaeuft(false)
    }
  }

  return (
    <Dialog titel={`Einzahlen: ${ziel.name}`} zu={zu}>
      <form onSubmit={absenden} className="flex flex-col gap-3">
        <input
          value={betrag}
          onChange={(e) => setBetrag(e.target.value)}
          inputMode="decimal"
          placeholder="100,00"
          autoFocus
          className="rounded-xl bg-karte2 px-4 py-3 outline-none ring-strich focus:ring-2"
        />
        <div className="flex gap-2">
          {personen.map((p) => (
            <button
              type="button"
              key={p}
              onClick={() => setPerson(p)}
              className={`flex-1 rounded-xl px-3 py-2.5 text-sm ${
                person === p ? 'bg-akzentstark text-white' : 'bg-karte2 text-textschwach'
              }`}
            >
              {p}
            </button>
          ))}
        </div>
        {fehler && <p className="text-sm text-schlecht">{fehler}</p>}
        <button
          type="submit"
          disabled={laeuft}
          className="rounded-xl bg-akzentstark py-3 font-semibold text-white disabled:opacity-50"
        >
          Einzahlen
        </button>
      </form>
    </Dialog>
  )
}
