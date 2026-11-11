import { useState, type FormEvent } from 'react'
import { ApiFehler, schreibe, type Person } from './types'
import { Mond } from './Layout'

export function ErsteinrichtungSeite({
  personen,
  onFertig,
}: {
  personen: Person[]
  onFertig: (name?: string) => void
}) {
  const [passwoerter, setPasswoerter] = useState<Record<string, string>>({})
  const [fehler, setFehler] = useState<string | null>(null)
  const [schonVorhanden, setSchonVorhanden] = useState(false)
  const [laeuft, setLaeuft] = useState(false)

  const gueltig = personen.every((p) => (passwoerter[p.name] ?? '').length >= 6)

  const setzen = (name: string, wert: string) => {
    setPasswoerter((alt) => ({ ...alt, [name]: wert }))
  }

  const absenden = async (e: FormEvent) => {
    e.preventDefault()
    if (!gueltig || laeuft) return
    setLaeuft(true)
    setFehler(null)
    try {
      await schreibe('/api/ersteinrichtung', { passwoerter })
      onFertig(personen[0]?.name)
    } catch (err) {
      if (err instanceof ApiFehler && err.status === 400) {
        setFehler('Jedes Passwort braucht mindestens 6 Zeichen.')
      } else if (err instanceof ApiFehler && err.status === 409) {
        setFehler('Es sind schon Passwörter festgelegt.')
        setSchonVorhanden(true)
      } else {
        setFehler('Das hat nicht geklappt. Nochmal versuchen.')
      }
    } finally {
      setLaeuft(false)
    }
  }

  return (
    <div className="flex min-h-[70dvh] flex-col items-center justify-center gap-8 text-center">
      <div className="flex flex-col items-center gap-3">
        <span className="text-akzent">
          <Mond />
        </span>
        <h1 className="text-2xl font-semibold">Fast geschafft</h1>
        <p className="max-w-xs text-textschwach">
          Eure Personen stehen schon — legt jetzt je ein Passwort fest.
        </p>
      </div>
      <form onSubmit={absenden} className="flex w-full max-w-xs flex-col gap-4">
        {personen.map((p) => {
          const wert = passwoerter[p.name] ?? ''
          return (
            <div key={p.id} className="text-left">
              <label htmlFor={`passwort-${p.id}`} className="mb-1 block text-xs text-textschwach">
                Passwort für {p.name}
              </label>
              <input
                id={`passwort-${p.id}`}
                type="password"
                value={wert}
                onChange={(e) => setzen(p.name, e.target.value)}
                className="w-full rounded-xl bg-karte px-4 py-3 outline-none ring-strich focus:ring-2"
              />
              {wert.length > 0 && wert.length < 6 && (
                <p className="mt-1 text-xs text-schlecht">Mindestens 6 Zeichen</p>
              )}
            </div>
          )
        })}
        {fehler && <p className="text-sm text-schlecht">{fehler}</p>}
        <button
          type="submit"
          disabled={!gueltig || laeuft}
          className="rounded-xl bg-akzentstark py-3 font-semibold text-akzenttext transition-transform active:scale-[0.98] disabled:opacity-50"
        >
          Passwörter festlegen
        </button>
        {schonVorhanden && (
          <button
            type="button"
            onClick={() => onFertig(personen[0]?.name)}
            className="text-sm text-textschwach underline underline-offset-2"
          >
            Zum Login
          </button>
        )}
        <p className="text-xs text-textschwach">
          Damit ist die App abgeschlossen eingerichtet — nur ihr zwei kommt rein.
        </p>
      </form>
    </div>
  )
}
