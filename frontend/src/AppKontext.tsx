import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { ApiFehler, lese, type Person } from './types'

export type WerErgebnis = 'server' | 'legacy' | 'abwesend'

interface AppState {
  personen: Person[]
  personenLaden: boolean
  personenNeuLaden: () => void
  aktivePerson: string | null
  werLaden: () => Promise<WerErgebnis>
}

const AppKontext = createContext<AppState>({
  personen: [],
  personenLaden: true,
  personenNeuLaden: () => {},
  aktivePerson: null,
  werLaden: async () => 'abwesend',
})

export function AppProvider({ children }: { children: ReactNode }) {
  const [personen, setPersonen] = useState<Person[]>([])
  const [personenLaden, setPersonenLaden] = useState(true)
  const [aktivePerson, setAktive] = useState<string | null>(null)

  const personenNeuLaden = useCallback(() => {
    setPersonenLaden(true)
    lese<Person[]>('/api/personen')
      .then((p) => {
        setPersonen(p)
        setPersonenLaden(false)
        setAktive((alt) => {
          if (alt && p.some((x) => x.name === alt)) return alt
          return p[0]?.name ?? null
        })
      })
      .catch(() => setPersonenLaden(false))
  }, [])

  useEffect(personenNeuLaden, [personenNeuLaden])

  const werLaden = useCallback(async (): Promise<WerErgebnis> => {
    try {
      const wer = await lese<{ name: string }>('/api/wer')
      setAktive(wer.name)
      return 'server'
    } catch (e) {
      if (e instanceof ApiFehler && e.status === 404) return 'legacy'
      return 'abwesend'
    }
  }, [])

  useEffect(() => {
    const gespeichert = localStorage.getItem('lunefi:person')
    if (gespeichert) setAktive(gespeichert)
  }, [])

  return (
    <AppKontext.Provider value={{ personen, personenLaden, personenNeuLaden, aktivePerson, werLaden }}>
      {children}
    </AppKontext.Provider>
  )
}

export function useApp(): AppState {
  return useContext(AppKontext)
}
