import { useState, type FormEvent } from 'react'
import { xsrfTokenFrisch } from './types'
import { Mond } from './Layout'

export function LoginSeite({ startName, onErfolg }: { startName?: string; onErfolg: () => void }) {
  const [name, setName] = useState(startName ?? '')
  const [passwort, setPasswort] = useState('')
  const [fehler, setFehler] = useState<string | null>(null)
  const [laeuft, setLaeuft] = useState(false)

  const absenden = async (e: FormEvent) => {
    e.preventDefault()
    const n = name.trim()
    if (!n || !passwort || laeuft) return
    setLaeuft(true)
    setFehler(null)
    try {
      const token = await xsrfTokenFrisch()
      const kopf: Record<string, string> = { 'Content-Type': 'application/x-www-form-urlencoded' }
      if (token) kopf['X-XSRF-TOKEN'] = token
      const antwort = await fetch('/api/login', {
        method: 'POST',
        credentials: 'same-origin',
        headers: kopf,
        body: new URLSearchParams({ username: n, password: passwort }).toString(),
      })
      if (antwort.status === 401) {
        setFehler('Name oder Passwort stimmen nicht.')
        return
      }
      if (!antwort.ok) throw new Error()
      onErfolg()
    } catch {
      setFehler('Die App antwortet gerade nicht.')
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
        <h1 className="text-2xl font-semibold">LuneFi</h1>
      </div>
      <form onSubmit={absenden} className="flex w-full max-w-xs flex-col gap-4">
        <div>
          <label htmlFor="login-name" className="mb-1 block text-xs text-textschwach">
            Name
          </label>
          <input
            id="login-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            autoFocus={!startName}
            autoComplete="username"
            className="w-full rounded-xl bg-karte px-4 py-3 outline-none ring-strich focus:ring-2"
          />
        </div>
        <div>
          <label htmlFor="login-passwort" className="mb-1 block text-xs text-textschwach">
            Passwort
          </label>
          <input
            id="login-passwort"
            type="password"
            value={passwort}
            onChange={(e) => setPasswort(e.target.value)}
            autoFocus={!!startName}
            autoComplete="current-password"
            className="w-full rounded-xl bg-karte px-4 py-3 outline-none ring-strich focus:ring-2"
          />
        </div>
        {fehler && <p className="text-sm text-schlecht">{fehler}</p>}
        <button
          type="submit"
          disabled={laeuft}
          className="rounded-xl bg-akzentstark py-3 font-semibold text-akzenttext transition-transform active:scale-[0.98] disabled:opacity-50"
        >
          Anmelden
        </button>
      </form>
    </div>
  )
}
