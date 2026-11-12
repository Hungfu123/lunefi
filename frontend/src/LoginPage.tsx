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

function GoogleSignIn({
  onSuccess,
  onError,
}: {
  onSuccess: () => void
  onError: (message: string) => void
}) {
  const host = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!GOOGLE_CLIENT_ID || !host.current) return
    const render = () => {
      if (!window.google || !host.current) return
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
              onError('Diese App ist privat. Mit diesem Google-Konto gibt es keinen Zugang.')
              return
            }
            if (!result.ok) throw new Error()
            onSuccess()
          } catch {
            onError('Anmeldung mit Google klappt gerade nicht.')
          }
        },
      })
      window.google.accounts.id.renderButton(host.current, {
        type: 'standard',
        size: 'large',
        shape: 'pill',
        text: 'signin_with',
        locale: 'de',
        width: 268,
      })
    }
    if (window.google) {
      render()
      return
    }
    const script = document.createElement('script')
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true
    script.onload = render
    document.head.appendChild(script)
    return () => {
      script.remove()
    }
  }, [onError, onSuccess])

  return (
    <div className="relative mx-auto h-11 w-[268px] max-w-full">
      <div className="absolute inset-0 overflow-hidden rounded-full">
        <div ref={host} className="h-full w-full" />
      </div>
      <div className="pointer-events-none absolute inset-0 flex items-center justify-center gap-3 rounded-full border border-strich bg-karte2 text-sm font-medium">
        <svg viewBox="0 0 48 48" className="h-[18px] w-[18px]" aria-hidden="true">
          <path
            fill="#EA4335"
            d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"
          />
          <path
            fill="#4285F4"
            d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"
          />
          <path
            fill="#FBBC05"
            d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"
          />
          <path
            fill="#34A853"
            d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
          />
        </svg>
        Über Google anmelden
      </div>
    </div>
  )
}

export function LoginPage({ startName, onSuccess }: { startName?: string; onSuccess: () => void }) {
  const [name, setName] = useState(startName ?? '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [showForm, setShowForm] = useState(!GOOGLE_CLIENT_ID)
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
      {GOOGLE_CLIENT_ID && !showForm ? (
        <div className="flex w-full max-w-xs flex-col items-center gap-5">
          <GoogleSignIn onSuccess={onSuccess} onError={setError} />
          {error && <p className="text-sm text-schlecht">{error}</p>}
          <button
            type="button"
            onClick={() => setShowForm(true)}
            className="text-xs text-textschwach underline underline-offset-2"
          >
            Lieber mit Passwort anmelden
          </button>
        </div>
      ) : (
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
              <GoogleSignIn onSuccess={onSuccess} onError={setError} />
            </>
          )}
        </form>
      )}
    </div>
  )
}
