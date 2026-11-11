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

export interface KategorieInfo {
  name: string
  split: SplitKonfiguration
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

async function api<T>(pfad: string, init?: RequestInit): Promise<T> {
  let antwort: Response
  try {
    antwort = await fetch(pfad, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    })
  } catch {
    throw new ApiFehler(0)
  }
  if (!antwort.ok) {
    throw new ApiFehler(antwort.status)
  }
  return antwort.json() as Promise<T>
}

export const lese = <T>(pfad: string) => api<T>(pfad)

export const schreibe = <T>(pfad: string, body: unknown, methode: 'POST' | 'PUT' = 'POST') =>
  api<T>(pfad, { method: methode, body: JSON.stringify(body) })

export const sende = async (pfad: string, methode: 'POST' | 'PUT' = 'POST'): Promise<void> => {
  let antwort: Response
  try {
    antwort = await fetch(pfad, {
      method: methode,
      headers: { 'Content-Type': 'application/json' },
    })
  } catch {
    throw new ApiFehler(0)
  }
  if (!antwort.ok) {
    throw new ApiFehler(antwort.status)
  }
}
