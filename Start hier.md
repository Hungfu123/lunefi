# Finance — BDD-Experiment

Spec zuerst, Code danach. Nichts hier braucht Jira, Miro oder einen Server.

## Reihenfolge

1. **[[example-mapping|Canvas öffnen]]** und füllen: Story gelb, Regeln cyan, Beispiele grün, offene Fragen rot.
2. Inhalt nach `specs/<thema>.md` übertragen — Vorlage: [[_vorlage]]
3. Aus den **grünen** Beispielen wird je ein Szenario in `features/<thema>/<name>.feature`
4. Steps schreiben, rot sehen, Code bis grün, dann eine Erwartung verbiegen und wieder rot sehen

## Ordner

| Pfad | Inhalt |
|---|---|
| `specs/` | Canvas und Spec-Dateien — das Was |
| `features/` | die ausführbaren Beispiele |
| `steps/` | Step-Definitionen |
| `src/` | der Code |

## Agenten-Start

Der Startprompt liegt in [[PROMPT]]. Copy-paste in eine neue Session in diesem Ordner.

## Die vier Regeln, die zählen

1. Ein `Wenn` je Szenario.
2. Das `Dann` muss von außen beobachtbar sein — kein „die Methode wird aufgerufen".
3. Keine Technik im Szenariotext.
4. Grün zählt erst, wenn du dasselbe Szenario einmal rot gesehen hast.
