import { useCallback, useEffect, useState } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AppProvider, useApp } from './AppContext'
import { read, send, SESSION_EXPIRED, type Entry, type Person } from './types'
import { Layout } from './Layout'
import { Onboarding } from './Onboarding'
import { LoginPage } from './LoginPage'
import { InitialSetupPage } from './InitialSetupPage'
import { Overview } from './Overview'
import { EntrySheet } from './EntrySheet'
import { SettlementPage } from './SettlementPage'
import { SavingsGoalsPage } from './SavingsGoalsPage'
import { StatisticsPage } from './StatisticsPage'
import { SettingsPage } from './SettingsPage'

type Status = 'pruefen' | 'offen' | 'ohneNutzer' | 'login' | 'angemeldet'

function useTheme() {
  useEffect(() => {
    const gespeichert = localStorage.getItem('lunefi:theme')
    if (gespeichert === 'sonne' || gespeichert === 'mond') {
      document.documentElement.dataset.theme = gespeichert
    }
  }, [])
}

function LoadingDot() {
  return (
    <div className="flex min-h-dvh items-center justify-center">
      <div className="h-10 w-10 animate-pulse rounded-full bg-akzent/40" />
    </div>
  )
}

function Content() {
  const { persons, loadWho, reloadPersons } = useApp()
  const [status, setStatus] = useState<Status>('pruefen')
  const [legacy, setLegacy] = useState(false)
  const [startName, setStartName] = useState<string | undefined>(undefined)
  const [sheetOpen, setSheetOpen] = useState(false)
  const [reloadCount, setReloadCount] = useState(0)
  const [editing, setEditing] = useState<Entry | null>(null)

  const check = useCallback(
    async (name?: string) => {
      if (name) setStartName(name)
      const result = await loadWho()
      if (result === 'server') {
        setStatus('angemeldet')
        reloadPersons()
        return
      }
      if (result === 'legacy') {
        setLegacy(true)
        try {
          const p = await read<Person[]>('/api/personen')
          if (p.length > 0) {
            setStatus('angemeldet')
            reloadPersons()
          } else {
            setStatus('offen')
          }
        } catch {
          setStatus('login')
        }
        return
      }
      try {
        const p = await read<Person[]>('/api/personen')
        setStatus(p.length === 0 ? 'offen' : 'ohneNutzer')
      } catch {
        setStatus('login')
      }
    },
    [loadWho, reloadPersons],
  )

  useEffect(() => {
    check()
  }, [check])

  useEffect(() => {
    if (status !== 'angemeldet') return
    const listener = () => setStatus('login')
    window.addEventListener(SESSION_EXPIRED, listener)
    return () => window.removeEventListener(SESSION_EXPIRED, listener)
  }, [status])

  const logout = useCallback(async () => {
    try {
      await send('/api/logout')
    } catch {
      return
    }
    check()
  }, [check])

  if (status === 'pruefen') {
    return <LoadingDot />
  }

  if (status === 'offen') {
    return <Onboarding onDone={check} />
  }

  if (status === 'ohneNutzer') {
    if (persons.length === 0) return <LoadingDot />
    return <InitialSetupPage persons={persons} onDone={check} />
  }

  if (status === 'login') {
    return <LoginPage startName={startName} onSuccess={() => check()} />
  }

  if (persons.length === 0) {
    return <LoadingDot />
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route
          element={
            <Layout onNewEntry={() => setSheetOpen(true)} canLogOut={!legacy} onLoggedOut={logout} />
          }
        >
          <Route index element={<Overview key={reloadCount} onEditEntry={setEditing} onNewEntry={() => setSheetOpen(true)} />} />
          <Route path="/ausgleich" element={<SettlementPage key={reloadCount} />} />
          <Route path="/sparziele" element={<SavingsGoalsPage />} />
          <Route path="/statistik" element={<StatisticsPage key={reloadCount} />} />
          <Route path="/einstellungen" element={<SettingsPage />} />
        </Route>
      </Routes>
      <EntrySheet
        open={sheetOpen}
        close={() => setSheetOpen(false)}
        onCaptured={() => {
          setSheetOpen(false)
          setReloadCount((n) => n + 1)
        }}
      />
      <EntrySheet
        entry={editing ?? undefined}
        open={editing !== null}
        close={() => setEditing(null)}
        onCaptured={() => {
          setEditing(null)
          setReloadCount((n) => n + 1)
        }}
      />
    </BrowserRouter>
  )
}

export default function App() {
  useTheme()
  return (
    <AppProvider>
      <Content />
    </AppProvider>
  )
}
