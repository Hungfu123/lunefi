import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { lese, type Person } from './types'

interface AppState {
  personen: Person[]
  personenLaden: boolean
  personenNeuLaden: () => void
  aktivePerson: string | null
  setAktivePerson: (name: string) => void
}

const AppKontext = createContext<AppState>({
  personen: [],
  personenLaden: true,
  personenNeuLaden: () => {},
  aktivePerson: null,
  setAktivePerson: () => {},
})

export function AppProvider({ children }: { children: ReactNode }) {
  const [personen, setPersonen] = useState<Person[]>([])
  const [personenLaden, setPersonenLaden] = useState(true)
  const [aktivePerson, setAktive] = useState<string | null>(null)

  const personenNeuLaden = useCallback(() => {
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

  const setAktivePerson = useCallback((name: string) => {
    setAktive(name)
    localStorage.setItem('lunefi:person', name)
  }, [])

  useEffect(() => {
    const gespeichert = localStorage.getItem('lunefi:person')
    if (gespeichert) setAktive(gespeichert)
  }, [])

  return (
    <AppKontext.Provider value={{ personen, personenLaden, personenNeuLaden, aktivePerson, setAktivePerson }}>
      {children}
    </AppKontext.Provider>
  )
}

export function useApp(): AppState {
  return useContext(AppKontext)
}
