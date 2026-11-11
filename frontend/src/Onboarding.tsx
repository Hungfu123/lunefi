import { useState, type FormEvent } from 'react'
import { schreibe, type Person } from './types'
import { useApp } from './AppKontext'
import { Mond } from './Layout'

export function Onboarding() {
  const { personenNeuLaden } = useApp()
  const [name1, setName1] = useState('')
  const [name2, setName2] = useState('')
  const [fehler, setFehler] = useState<string | null>(null)
  const [laeuft, setLaeuft] = useState(false)

  const absenden = async (e: FormEvent) => {
    e.preventDefault()
    const a = name1.trim()
    const b = name2.trim()
    if (!a || !b) {
      setFehler('Bitte beide Namen eintragen.')
      return
    }
    setLaeuft(true)
    setFehler(null)
    try {
      await schreibe<Person>('/api/personen', { name: a })
      await schreibe<Person>('/api/personen', { name: b })
      personenNeuLaden()
    } catch {
      setFehler('Das hat nicht geklappt. Nochmal versuchen.')
      personenNeuLaden()
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
        <h1 className="text-2xl font-semibold">Willkommen bei LuneFi</h1>
        <p className="max-w-xs text-textschwach">
          Erfasst Ausgaben gemeinsam — LuneFi zeigt sofort, wer wem wie viel schuldet.
        </p>
        <p className="max-w-xs text-sm text-textschwach">
          Eure Saldi und Einnahmen bleiben privat — geteilt wird nur, was ihr gemeinsam macht.
        </p>
      </div>
      <form onSubmit={absenden} className="flex w-full max-w-xs flex-col gap-4">
        <div>
          <label htmlFor="name1" className="mb-1 block text-xs text-textschwach">
            Dein Name
          </label>
          <input
            id="name1"
            value={name1}
            onChange={(e) => setName1(e.target.value)}
            autoFocus
            className="w-full rounded-xl bg-karte px-4 py-3 outline-none ring-strich focus:ring-2"
          />
        </div>
        <div>
          <label htmlFor="name2" className="mb-1 block text-xs text-textschwach">
            Name deiner Partnerin / deines Partners
          </label>
          <input
            id="name2"
            value={name2}
            onChange={(e) => setName2(e.target.value)}
            className="w-full rounded-xl bg-karte px-4 py-3 outline-none ring-strich focus:ring-2"
          />
        </div>
        {fehler && <p className="text-sm text-schlecht">{fehler}</p>}
        <button
          type="submit"
          disabled={laeuft}
          className="rounded-xl bg-akzentstark py-3 font-semibold text-akzenttext transition-transform active:scale-[0.98] disabled:opacity-50"
        >
          Los geht's
        </button>
      </form>
    </div>
  )
}
