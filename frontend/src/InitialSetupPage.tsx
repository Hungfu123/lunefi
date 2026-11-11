import { useState, type FormEvent } from 'react'
import { ApiError, write, type Person } from './types'
import { Mond } from './Layout'

export function InitialSetupPage({
  persons,
  onDone,
}: {
  persons: Person[]
  onDone: (name?: string) => void
}) {
  const [passwords, setPasswords] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [alreadySetUp, setAlreadySetUp] = useState(false)
  const [busy, setBusy] = useState(false)

  const valid = persons.every((p) => (passwords[p.name] ?? '').length >= 6)

  const setValue = (name: string, value: string) => {
    setPasswords((prev) => ({ ...prev, [name]: value }))
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (!valid || busy) return
    setBusy(true)
    setError(null)
    try {
      await write('/api/ersteinrichtung', { passwoerter: passwords })
      onDone(persons[0]?.name)
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError('Jedes Passwort braucht mindestens 6 Zeichen.')
      } else if (err instanceof ApiError && err.status === 409) {
        setError('Es sind schon Passwörter festgelegt.')
        setAlreadySetUp(true)
      } else {
        setError('Das hat nicht geklappt. Nochmal versuchen.')
      }
    } finally {
      setBusy(false)
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
      <form onSubmit={submit} className="flex w-full max-w-xs flex-col gap-4">
        {persons.map((p) => {
          const value = passwords[p.name] ?? ''
          return (
            <div key={p.id} className="text-left">
              <label htmlFor={`passwort-${p.id}`} className="mb-1 block text-xs text-textschwach">
                Passwort für {p.name}
              </label>
              <input
                id={`passwort-${p.id}`}
                type="password"
                value={value}
                onChange={(e) => setValue(p.name, e.target.value)}
                className="w-full rounded-xl bg-karte px-4 py-3 outline-none ring-strich focus:ring-2"
              />
              {value.length > 0 && value.length < 6 && (
                <p className="mt-1 text-xs text-schlecht">Mindestens 6 Zeichen</p>
              )}
            </div>
          )
        })}
        {error && <p className="text-sm text-schlecht">{error}</p>}
        <button
          type="submit"
          disabled={!valid || busy}
          className="rounded-xl bg-akzentstark py-3 font-semibold text-akzenttext transition-transform active:scale-[0.98] disabled:opacity-50"
        >
          Passwörter festlegen
        </button>
        {alreadySetUp && (
          <button
            type="button"
            onClick={() => onDone(persons[0]?.name)}
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
