export interface Person {
  id: number
  name: string
}

export interface Account {
  id: number
  name: string
  person: string
  startSaldo: number
  saldo: number
}

export interface Entry {
  id: number
  datum: string
  betrag: number
  kategorie: string
  person: string | null
  gemeinsam: boolean
  zahler: string | null
  konto: string | null
}

export interface HistoryMonth {
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

export interface MonthlyOverview {
  monat: string
  ausgabenJeKategorie: Record<string, number>
  einnahmenJeKategorie: Record<string, number>
  eintraege: Entry[]
  budgets: Record<string, BudgetInfo>
}

export interface SettlementPosition {
  id: number
  kategorie: string
  betrag: number
  zahler: string
  anteile: Record<string, number>
}

export interface Settlement {
  monat: string
  laeuftNoch: boolean
  ausgeglichen: boolean
  saldi: Record<string, number>
  vorschlag: string | null
  positionen: SettlementPosition[]
}

export interface SettlementMonth {
  monat: string
  laeuftNoch: boolean
  ausgeglichen: boolean
  saldi: Record<string, number>
  vorschlag: string | null
}

export interface Deposit {
  id: number
  betrag: number
  person: string
  datum: string
}

export interface SavingsGoal {
  id: number
  name: string
  zielbetrag: number
  eingezahlt: number
  erreicht: boolean
  einzahlungen: Deposit[]
}

export interface SplitConfiguration {
  art: 'HALBHALB' | 'ANTEILE'
  anteile: Record<string, number> | null
}

export type Attitude = 'SPAREN' | 'FLEXIBEL' | 'BEWUSST_MEHR'

export interface CategoryInfo {
  name: string
  split: SplitConfiguration
  haltung: Attitude
}

export interface CoachingRecommendation {
  kategorie: string
  haltung: Attitude
  ist: number
  limit: number
  spielraum: number
  tipp: string
}

export interface Coaching {
  monat: string
  budgetsUebrig: Record<string, number>
  empfehlungen: CoachingRecommendation[]
  gutGespart: { kategorie: string; haltung: Attitude; ist: number; limit: number; spielraum: number }[]
}

export interface NewEntry {
  datum: string
  betrag: number
  kategorie: string
  person?: string
  gemeinsam?: boolean
  zahler?: string
  konto?: string
}

export class ApiError extends Error {
  constructor(readonly status: number) {
    super(status === 400 ? 'Das hat die App nicht akzeptiert. Bitte prüfen.' : 'Die App antwortet gerade nicht.')
    this.name = 'ApiFehler'
  }
}

export const SESSION_EXPIRED = 'lunefi:sitzung-abgelaufen'

function xsrfToken(): string | null {
  const entry = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='))
  if (!entry) return null
  return decodeURIComponent(entry.slice('XSRF-TOKEN='.length))
}

export async function freshXsrfToken(): Promise<string | null> {
  try {
    await fetch('/api/personen', { credentials: 'same-origin' })
  } catch {
    return null
  }
  return xsrfToken()
}

function isMutating(method?: string): boolean {
  return method === 'POST' || method === 'PUT' || method === 'DELETE'
}

async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((init?.headers as Record<string, string>) ?? {}),
  }
  if (isMutating(init?.method)) {
    const token = xsrfToken()
    if (token) headers['X-XSRF-TOKEN'] = token
  }
  let response: Response
  try {
    response = await fetch(path, { ...init, credentials: 'same-origin', headers })
  } catch {
    throw new ApiError(0)
  }
  if (response.status === 401) {
    window.dispatchEvent(new Event(SESSION_EXPIRED))
  }
  if (!response.ok) {
    throw new ApiError(response.status)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export const read = <T>(path: string) => api<T>(path)

export const write = <T>(path: string, body: unknown, method: 'POST' | 'PUT' = 'POST') =>
  api<T>(path, { method, body: JSON.stringify(body) })

export const send = async (path: string, method: 'POST' | 'PUT' | 'DELETE' = 'POST'): Promise<void> => {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = xsrfToken()
  if (token) headers['X-XSRF-TOKEN'] = token
  let response: Response
  try {
    response = await fetch(path, { method, credentials: 'same-origin', headers })
  } catch {
    throw new ApiError(0)
  }
  if (response.status === 401) {
    window.dispatchEvent(new Event(SESSION_EXPIRED))
  }
  if (!response.ok) {
    throw new ApiError(response.status)
  }
}

export const remove = (path: string) => send(path, 'DELETE')
