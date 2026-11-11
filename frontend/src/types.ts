export interface Person {
  id: number
  name: string
}

export interface Konto {
  id: number
  name: string
  person: string
  startSaldo: number
  saldo: number
}

export interface Eintrag {
  id: number
  datum: string
  betrag: number
  kategorie: string
  person: string | null
  gemeinsam: boolean
  zahler: string | null
  konto: string | null
}

export interface VerlaufMonat {
  monat: string
  einnahmen: number
  ausgaben: number
  differenz: number
}

export interface BudgetInfo {
  limit: number
  ist: number
  warnung: boolean
}

export interface Monatsuebersicht {
  monat: string
  ausgabenJeKategorie: Record<string, number>
  einnahmenJeKategorie: Record<string, number>
  eintraege: Eintrag[]
  budgets: Record<string, BudgetInfo>
}

export interface AusgleichPosition {
  id: number
  kategorie: string
  betrag: number
  zahler: string
  anteile: Record<string, number>
}

export interface Ausgleich {
  monat: string
  laeuftNoch: boolean
  ausgeglichen: boolean
  saldi: Record<string, number>
  vorschlag: string | null
  positionen: AusgleichPosition[]
}

export interface AusgleichMonat {
  monat: string
  laeuftNoch: boolean
  ausgeglichen: boolean
  saldi: Record<string, number>
  vorschlag: string | null
}

export interface Einzahlung {
  id: number
  betrag: number
  person: string
  datum: string
}

export interface Sparziel {
  id: number
  name: string
  zielbetrag: number
  eingezahlt: number
  erreicht: boolean
  einzahlungen: Einzahlung[]
}

export interface SplitKonfiguration {
  art: 'HALBHALB' | 'ANTEILE'
  anteile: Record<string, number> | null
}

export type Haltung = 'SPAREN' | 'FLEXIBEL' | 'BEWUSST_MEHR'

export interface KategorieInfo {
  name: string
  split: SplitKonfiguration
  haltung: Haltung
}

export interface CoachingEmpfehlung {
  kategorie: string
  haltung: Haltung
  ist: number
  limit: number
  spielraum: number
  tipp: string
}

export interface Coaching {
  monat: string
  budgetsUebrig: Record<string, number>
  empfehlungen: CoachingEmpfehlung[]
  gutGespart: { kategorie: string; haltung: Haltung; ist: number; limit: number; spielraum: number }[]
}

export interface EintragNeu {
  datum: string
  betrag: number
  kategorie: string
  person?: string
  gemeinsam?: boolean
  zahler?: string
  konto?: string
}

export class ApiFehler extends Error {
  constructor(readonly status: number) {
    super(status === 400 ? 'Das hat die App nicht akzeptiert. Bitte prüfen.' : 'Die App antwortet gerade nicht.')
    this.name = 'ApiFehler'
  }
}

export const SITZUNG_ABGELAUFEN = 'lunefi:sitzung-abgelaufen'

function xsrfToken(): string | null {
  const eintrag = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='))
  if (!eintrag) return null
  return decodeURIComponent(eintrag.slice('XSRF-TOKEN='.length))
}

export async function xsrfTokenFrisch(): Promise<string | null> {
  try {
    await fetch('/api/personen', { credentials: 'same-origin' })
  } catch {
    return null
  }
  return xsrfToken()
}

function schreibend(methode?: string): boolean {
  return methode === 'POST' || methode === 'PUT' || methode === 'DELETE'
}

async function api<T>(pfad: string, init?: RequestInit): Promise<T> {
  const kopf: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((init?.headers as Record<string, string>) ?? {}),
  }
  if (schreibend(init?.method)) {
    const token = xsrfToken()
    if (token) kopf['X-XSRF-TOKEN'] = token
  }
  let antwort: Response
  try {
    antwort = await fetch(pfad, { ...init, credentials: 'same-origin', headers: kopf })
  } catch {
    throw new ApiFehler(0)
  }
  if (antwort.status === 401) {
    window.dispatchEvent(new Event(SITZUNG_ABGELAUFEN))
  }
  if (!antwort.ok) {
    throw new ApiFehler(antwort.status)
  }
  if (antwort.status === 204) {
    return undefined as T
  }
  return antwort.json() as Promise<T>
}

export const lese = <T>(pfad: string) => api<T>(pfad)

export const schreibe = <T>(pfad: string, body: unknown, methode: 'POST' | 'PUT' = 'POST') =>
  api<T>(pfad, { method: methode, body: JSON.stringify(body) })

export const sende = async (pfad: string, methode: 'POST' | 'PUT' | 'DELETE' = 'POST'): Promise<void> => {
  const kopf: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = xsrfToken()
  if (token) kopf['X-XSRF-TOKEN'] = token
  let antwort: Response
  try {
    antwort = await fetch(pfad, { method: methode, credentials: 'same-origin', headers: kopf })
  } catch {
    throw new ApiFehler(0)
  }
  if (antwort.status === 401) {
    window.dispatchEvent(new Event(SITZUNG_ABGELAUFEN))
  }
  if (!antwort.ok) {
    throw new ApiFehler(antwort.status)
  }
}

export const loesche = (pfad: string) => sende(pfad, 'DELETE')
