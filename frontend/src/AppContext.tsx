import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { ApiError, read, type Person } from './types'

export type WhoResult = 'server' | 'legacy' | 'abwesend'

interface AppState {
  persons: Person[]
  personsLoading: boolean
  reloadPersons: () => void
  activePerson: string | null
  loadWho: () => Promise<WhoResult>
}

const AppContext = createContext<AppState>({
  persons: [],
  personsLoading: true,
  reloadPersons: () => {},
  activePerson: null,
  loadWho: async () => 'abwesend',
})

export function AppProvider({ children }: { children: ReactNode }) {
  const [persons, setPersons] = useState<Person[]>([])
  const [personsLoading, setPersonsLoading] = useState(true)
  const [activePerson, setActive] = useState<string | null>(null)

  const reloadPersons = useCallback(() => {
    setPersonsLoading(true)
    read<Person[]>('/api/personen')
      .then((p) => {
        setPersons(p)
        setPersonsLoading(false)
        setActive((alt) => {
          if (alt && p.some((x) => x.name === alt)) return alt
          return p[0]?.name ?? null
        })
      })
      .catch(() => setPersonsLoading(false))
  }, [])

  useEffect(reloadPersons, [reloadPersons])

  const loadWho = useCallback(async (): Promise<WhoResult> => {
    try {
      const who = await read<{ name: string }>('/api/wer')
      setActive(who.name)
      return 'server'
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) return 'legacy'
      return 'abwesend'
    }
  }, [])

  useEffect(() => {
    const gespeichert = localStorage.getItem('lunefi:person')
    if (gespeichert) setActive(gespeichert)
  }, [])

  return (
    <AppContext.Provider value={{ persons, personsLoading, reloadPersons, activePerson, loadWho }}>
      {children}
    </AppContext.Provider>
  )
}

export function useApp(): AppState {
  return useContext(AppContext)
}
