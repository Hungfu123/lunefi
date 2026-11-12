import type { Entry } from './types'

/** Liefert zu einer Kategorie die passenden Einträge mit Teil-Beträgen:
 *  Ein geteilter Eintrag zählt mit der Summe seiner Teile dieser Kategorie,
 *  ungeteilte mit ihrem vollen Betrag. */
export function entriesForCategory(entries: Entry[], category: string): Entry[] {
  const result: Entry[] = []
  for (const entry of entries) {
    if (entry.teile && entry.teile.length > 0) {
      const amount = entry.teile
        .filter((t) => t.kategorie === category)
        .reduce((sum, t) => sum + t.betrag, 0)
      if (amount !== 0) {
        result.push({ ...entry, betrag: amount })
      }
    } else if (entry.kategorie === category) {
      result.push(entry)
    }
  }
  return result
}
