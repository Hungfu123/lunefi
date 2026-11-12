import { useEffect, useRef, useState, type FormEvent } from 'react'
import { freshXsrfToken } from './types'
import { Mond } from './Layout'

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string
            callback: (response: { credential: string }) => void
          }) => void
          renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void
        }
      }
    }
  }
}

export function LoginPage({ startName, onSuccess }: { startName?: string; onSuccess: () => void }) {
  const [name, setName] = useState(startName ?? '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const googleButton = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!GOOGLE_CLIENT_ID || !googleButton.current) return
    const script = document.createElement('script')
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true
    document.head.appendChild(script)
    script.onload = () => {
      if (!window.google || !googleButton.current) return
      window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: async (response) => {
          try {
            const token = await freshXsrfToken()
            const headers: Record<string, string> = { 'Content-Type': 'application/json' }
            if (token) headers['X-XSRF-TOKEN'] = token
            const result = await fetch('/api/auth/google', {
              method: 'POST',
              credentials: 'same-origin',
              headers,
              body: JSON.stringify({ credential: response.credential }),
            })
            if (result.status === 403) {
              setError('Diese App ist privat. Mit diesem Google-Konto gibt es keinen Zugang.')
              return
            }
            if (!result.ok) throw new Error()
            onSuccess()
          } catch {
            setError('Anmeldung mit Google klappt gerade nicht.')
          }
        },
      })
      window.google.accounts.id.renderButton(googleButton.current, {
        theme: 'filled_blue',
        size: 'large',
        shape: 'pill',
        text: 'signin_with',
        locale: 'de',
        width: 248,
      })
    }
    return () => {
      script.remove()
    }
  }, [onSuccess])

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const n = name.trim()
    if (!n || !password || busy) return
    setBusy(true)
    setError(null)
    try {
      const token = await freshXsrfToken()
      const headers: Record<string, string> = { 'Content-Type': 'application/x-www-form-urlencoded' }
      if (token) headers['X-XSRF-TOKEN'] = token
      const response = await fetch('/api/login', {
        method: 'POST',
        credentials: 'same-origin',
        headers,
        body: new URLSearchParams({ username: n, password }).toString(),
      })
      if (response.status === 401) {
        setError('Name oder Passwort stimmen nicht.')
        return
      }
      if (!response.ok) throw new Error()
      onSuccess()
    } catch {
      setError('Die App antwortet gerade nicht.')
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
        <h1 className="text-2xl font-semibold">LuneFi</h1>
      </div>
      <form onSubmit={submit} className="flex w-full max-w-xs flex-col gap-4">
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
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoFocus={!!startName}
            autoComplete="current-password"
            className="w-full rounded-xl bg-karte px-4 py-3 outline-none ring-strich focus:ring-2"
          />
        </div>
        {error && <p className="text-sm text-schlecht">{error}</p>}
        <button
          type="submit"
          disabled={busy}
          className="rounded-xl bg-akzentstark py-3 font-semibold text-akzenttext transition-transform active:scale-[0.98] disabled:opacity-50"
        >
          Anmelden
        </button>
        {GOOGLE_CLIENT_ID && (
          <>
            <div className="flex items-center gap-3 text-xs text-textschwach" aria-hidden="true">
              <span className="h-px flex-1 bg-strich" />
              oder
              <span className="h-px flex-1 bg-strich" />
            </div>
            <div ref={googleButton} className="flex justify-center" />
          </>
        )}
      </form>
    </div>
  )
}
