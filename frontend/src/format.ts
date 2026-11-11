export function euro(betrag: number): string {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(betrag)
}

export function amountCompact(betrag: number, abKompakt = 100000): string {
  const abs = Math.abs(betrag)
  if (abs >= abKompakt) return compactNumber(betrag) + ' €'
  return new Intl.NumberFormat('de-DE', { maximumFractionDigits: 0 }).format(betrag) + ' €'
}

function compactNumber(betrag: number): string {
  const sign = betrag < 0 ? '-' : ''
  const abs = Math.abs(betrag)
  if (abs < 1000000) {
    return sign + new Intl.NumberFormat('de-DE', { maximumFractionDigits: 1 }).format(abs / 1000) + 'k'
  }
  return (
    sign +
    new Intl.NumberFormat('de-DE', { notation: 'compact', maximumFractionDigits: 2 }).format(abs)
  )
}

export function monthAsString(datum: Date): string {
  const monat = String(datum.getMonth() + 1).padStart(2, '0')
  return `${datum.getFullYear()}-${monat}`
}

export function shiftMonth(monat: string, delta: number): string {
  const [jahr, m] = monat.split('-').map(Number)
  const neu = new Date(jahr, m - 1 + delta, 1)
  return monthAsString(neu)
}

export function monthLabel(monat: string): string {
  const [jahr, m] = monat.split('-').map(Number)
  const name = new Date(jahr, m - 1, 1).toLocaleDateString('de-DE', { month: 'long', year: 'numeric' })
  return name
}

export function shortMonth(monat: string): string {
  const [jahr, m] = monat.split('-').map(Number)
  return new Date(jahr, m - 1, 1).toLocaleDateString('de-DE', { month: 'short' })
}

export function shortDate(iso: string): string {
  const datum = new Date(iso + 'T00:00:00')
  return datum.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit' })
}

export function todayIso(): string {
  const jetzt = new Date()
  const monat = String(jetzt.getMonth() + 1).padStart(2, '0')
  const tag = String(jetzt.getDate()).padStart(2, '0')
  return `${jetzt.getFullYear()}-${monat}-${tag}`
}

export function isThisWeek(iso: string): boolean {
  const jetzt = new Date()
  const start = new Date(jetzt.getFullYear(), jetzt.getMonth(), jetzt.getDate())
  start.setDate(start.getDate() - ((start.getDay() + 6) % 7))
  const ende = new Date(start)
  ende.setDate(ende.getDate() + 7)
  const datum = new Date(iso + 'T00:00:00')
  return datum >= start && datum < ende
}

export function amountFromInput(text: string): number | null {
  const bereinigt = text.trim().replace(/\s/g, '')
  if (bereinigt === '') return null
  if (/^\d{1,3}(\.\d{3})+(,\d{1,2})?$/.test(bereinigt)) {
    const wert = Number(bereinigt.replace(/\./g, '').replace(',', '.'))
    return Number.isFinite(wert) ? wert : null
  }
  const wert = Number(bereinigt.replace(',', '.'))
  return Number.isFinite(wert) ? wert : null
}
