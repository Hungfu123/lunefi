# Thema: Konten und Gesamtsaldo

Quelle: Nutzerrückmeldung 11.11.2026 („keine Übersicht, wie viel Geld ich insgesamt habe"). Beispielwerte sind Vorschläge und im Chat zu bestätigen.

## Story

Als ich
möchte ich meine Bankkonten mit Startsaldo anlegen und meinen Gesamtsaldo sehen
damit ich auf einen Blick weiß, wie viel Geld ich habe — später ersetzt der FinTS-Import die manuellen Konten durch echte

## Regeln

| ID | Regel | Status |
|----|-------|--------|
| K1 | Jedes Konto gehört einer Person und hat einen Namen und einen Startsaldo | bestätigt 11.11.2026 (Startsaldo + Bewegungen) |
| K2 | Der Gesamtsaldo einer Person ist die Summe aller ihrer Konten | bestätigt 11.11.2026 |
| K3 | Ein Eintrag, der einem Konto zugeordnet ist, verändert dessen Saldo um seinen Betrag (Zuordnung ist freiwillig) | bestätigt 11.11.2026 (opt-in) |

## Beispiele

| Regel | Gegeben | Wenn | Dann |
|-------|---------|------|------|
| K1 | noch kein Konto angelegt | das Konto „Girokonto" mit Startsaldo 3.450,00 € von „Alex" angelegt wird | zeigt die Kontenübersicht „Girokonto" mit Saldo 3.450,00 € |
| K2 | Konten „Girokonto" 3.450,00 € und „Sparkonto" 12.000,00 € von „Alex" | die Kontenübersicht aufgerufen wird | beträgt der Gesamtsaldo von „Alex" 15.450,00 € |
| K3 | Konto „Girokonto" 3.450,00 € von „Alex" | ein Gehalt-Eintrag +2.000,00 € von „Alex" auf dem „Girokonto" erfasst wird | zeigt die Kontenübersicht „Girokonto" mit Saldo 5.450,00 € |

## Offene Fragen

| ID | Frage | Entschieden am |
|----|-------|----------------|
| KF1 | Google-Anmeldung: dafür brauchst du eine Google-Client-ID (Google Cloud Console, OAuth-Client „Web"). Bis dahin bleibt der Personen-Wechsler. Anleitung folgt. | |
| KF2 | Bank-Verknüpfung „einmalig verknüpfen": Wie funktioniert das mit FinTS konkret (Commerzbank: SCA bei jedem Abruf? Decoupled?) — FinTS-Spike vor der Umsetzung | |
| KF3 | Sparziel-Einzahlungen: vom Konto abgebucht oder virtuell (kein Kontobezug)? | |
| KF4 | Sieht man in der Übersicht auch die Konten/Salden der Partnerin oder nur die eigenen? | |

## Daraus entstanden

- `features/konten/konto-mit-startsaldo.feature` (K1 + K2)
- `features/konten/eintrag-veraendert-kontostand.feature` (K3)
