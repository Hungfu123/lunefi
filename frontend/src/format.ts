export function euro(betrag: number): string {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(betrag)
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
