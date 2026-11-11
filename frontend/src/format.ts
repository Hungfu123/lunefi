export function euro(betrag: number): string {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(betrag)
}

export function betragKompakt(betrag: number, abKompakt = 100000): string {
  const abs = Math.abs(betrag)
  if (abs >= abKompakt) return kompaktZahl(betrag) + ' €'
  return new Intl.NumberFormat('de-DE', { maximumFractionDigits: 0 }).format(betrag) + ' €'
}

function kompaktZahl(betrag: number): string {
  const vorzeichen = betrag < 0 ? '-' : ''
  const abs = Math.abs(betrag)
  if (abs < 1000000) {
    return vorzeichen + new Intl.NumberFormat('de-DE', { maximumFractionDigits: 1 }).format(abs / 1000) + 'k'
  }
  return (
    vorzeichen +
    new Intl.NumberFormat('de-DE', { notation: 'compact', maximumFractionDigits: 2 }).format(abs)
  )
}

export function monatAlsString(datum: Date): string {
  const monat = String(datum.getMonth() + 1).padStart(2, '0')
  return `${datum.getFullYear()}-${monat}`
}

export function monatVerschieben(monat: string, delta: number): string {
  const [jahr, m] = monat.split('-').map(Number)
  const neu = new Date(jahr, m - 1 + delta, 1)
  return monatAlsString(neu)
}

export function monatLabel(monat: string): string {
  const [jahr, m] = monat.split('-').map(Number)
  const name = new Date(jahr, m - 1, 1).toLocaleDateString('de-DE', { month: 'long', year: 'numeric' })
  return name
}

export function datumKurz(iso: string): string {
  const datum = new Date(iso + 'T00:00:00')
  return datum.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit' })
}

export function heuteIso(): string {
  const jetzt = new Date()
  const monat = String(jetzt.getMonth() + 1).padStart(2, '0')
  const tag = String(jetzt.getDate()).padStart(2, '0')
  return `${jetzt.getFullYear()}-${monat}-${tag}`
}

export function betragAusEingabe(text: string): number | null {
  const bereinigt = text.trim().replace(/\s/g, '')
  if (bereinigt === '') return null
  if (/^\d{1,3}(\.\d{3})+(,\d{1,2})?$/.test(bereinigt)) {
    const wert = Number(bereinigt.replace(/\./g, '').replace(',', '.'))
    return Number.isFinite(wert) ? wert : null
  }
  const wert = Number(bereinigt.replace(',', '.'))
  return Number.isFinite(wert) ? wert : null
}
