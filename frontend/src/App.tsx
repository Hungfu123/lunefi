import { useCallback, useEffect, useState } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AppProvider, useApp } from './AppKontext'
import { lese, sende, SITZUNG_ABGELAUFEN, type Eintrag, type Person } from './types'
import { Layout } from './Layout'
import { Onboarding } from './Onboarding'
import { LoginSeite } from './LoginSeite'
import { ErsteinrichtungSeite } from './ErsteinrichtungSeite'
import { Uebersicht } from './Uebersicht'
import { EintragSheet } from './EintragSheet'
import { AusgleichSeite } from './AusgleichSeite'
import { SparzieleSeite } from './SparzieleSeite'
import { EinstellungenSeite } from './EinstellungenSeite'

type Status = 'pruefen' | 'offen' | 'ohneNutzer' | 'login' | 'angemeldet'

function useTheme() {
  useEffect(() => {
    const gespeichert = localStorage.getItem('lunefi:theme')
    if (gespeichert === 'sonne' || gespeichert === 'mond') {
      document.documentElement.dataset.theme = gespeichert
    }
  }, [])
}

function Ladepunkt() {
  return (
    <div className="flex min-h-dvh items-center justify-center">
      <div className="h-10 w-10 animate-pulse rounded-full bg-akzent/40" />
    </div>
  )
}

function Inhalt() {
  const { personen, werLaden, personenNeuLaden } = useApp()
  const [status, setStatus] = useState<Status>('pruefen')
  const [legacy, setLegacy] = useState(false)
  const [startName, setStartName] = useState<string | undefined>(undefined)
  const [sheetOffen, setSheetOffen] = useState(false)
  const [neuGeladen, setNeuGeladen] = useState(0)
  const [bearbeitet, setBearbeitet] = useState<Eintrag | null>(null)

  const pruefen = useCallback(
    async (name?: string) => {
      if (name) setStartName(name)
      const ergebnis = await werLaden()
      if (ergebnis === 'server') {
        setStatus('angemeldet')
        personenNeuLaden()
        return
      }
      if (ergebnis === 'legacy') {
        setLegacy(true)
        try {
          const p = await lese<Person[]>('/api/personen')
          if (p.length > 0) {
            setStatus('angemeldet')
            personenNeuLaden()
          } else {
            setStatus('offen')
          }
        } catch {
          setStatus('login')
        }
        return
      }
      try {
        const p = await lese<Person[]>('/api/personen')
        setStatus(p.length === 0 ? 'offen' : 'ohneNutzer')
      } catch {
        setStatus('login')
      }
    },
    [werLaden, personenNeuLaden],
  )

  useEffect(() => {
    pruefen()
  }, [pruefen])

  useEffect(() => {
    if (status !== 'angemeldet') return
    const hoerzu = () => setStatus('login')
    window.addEventListener(SITZUNG_ABGELAUFEN, hoerzu)
    return () => window.removeEventListener(SITZUNG_ABGELAUFEN, hoerzu)
  }, [status])

  const abmelden = useCallback(async () => {
    try {
      await sende('/api/logout')
    } catch {
      return
    }
    pruefen()
  }, [pruefen])

  if (status === 'pruefen') {
    return <Ladepunkt />
  }

  if (status === 'offen') {
    return <Onboarding onFertig={pruefen} />
  }

  if (status === 'ohneNutzer') {
    if (personen.length === 0) return <Ladepunkt />
    return <ErsteinrichtungSeite personen={personen} onFertig={pruefen} />
  }

  if (status === 'login') {
    return <LoginSeite startName={startName} onErfolg={() => pruefen()} />
  }

  if (personen.length === 0) {
    return <Ladepunkt />
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route
          element={
            <Layout onEintragNeu={() => setSheetOffen(true)} kannAbmelden={!legacy} onAbgemeldet={abmelden} />
          }
        >
          <Route index element={<Uebersicht key={neuGeladen} onEintragBearbeiten={setBearbeitet} />} />
          <Route path="/ausgleich" element={<AusgleichSeite key={neuGeladen} />} />
          <Route path="/sparziele" element={<SparzieleSeite />} />
          <Route path="/einstellungen" element={<EinstellungenSeite />} />
        </Route>
      </Routes>
      <EintragSheet
        offen={sheetOffen}
        zu={() => setSheetOffen(false)}
        onErfasst={() => {
          setSheetOffen(false)
          setNeuGeladen((n) => n + 1)
        }}
      />
      <EintragSheet
        eintrag={bearbeitet ?? undefined}
        offen={bearbeitet !== null}
        zu={() => setBearbeitet(null)}
        onErfasst={() => {
          setBearbeitet(null)
          setNeuGeladen((n) => n + 1)
        }}
      />
    </BrowserRouter>
  )
}

export default function App() {
  useTheme()
  return (
    <AppProvider>
      <Inhalt />
    </AppProvider>
  )
}
