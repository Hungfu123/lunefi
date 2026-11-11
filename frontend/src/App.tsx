import { useState } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AppProvider, useApp } from './AppKontext'
import { Layout } from './Layout'
import { Onboarding } from './Onboarding'
import { Uebersicht } from './Uebersicht'
import { EintragSheet } from './EintragSheet'
import { AusgleichSeite } from './AusgleichSeite'
import { SparzieleSeite } from './SparzieleSeite'
import { EinstellungenSeite } from './EinstellungenSeite'

function Inhalt() {
  const { personen, personenLaden } = useApp()
  const [sheetOffen, setSheetOffen] = useState(false)
  const [neuGeladen, setNeuGeladen] = useState(0)

  if (personenLaden) {
    return (
      <div className="flex min-h-dvh items-center justify-center">
        <div className="h-10 w-10 animate-pulse rounded-full bg-akzent/40" />
      </div>
    )
  }

  if (personen.length === 0) {
    return <Onboarding />
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout onEintragNeu={() => setSheetOffen(true)} />}>
          <Route index element={<Uebersicht key={neuGeladen} />} />
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
    </BrowserRouter>
  )
}

export default function App() {
  return (
    <AppProvider>
      <Inhalt />
    </AppProvider>
  )
}
