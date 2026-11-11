import { useEffect, useState, type FormEvent } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { ApiFehler, lese, schreibe, type Eintrag, type EintragNeu, type KategorieInfo, type Konto } from './types'
import { betragAusEingabe, euro, heuteIso } from './format'
import {
  KategorieIconTile,
  PICKER_ICONS,
  iconNameVon,
  kategorieIconName,
  setzeKategorieIcon,
} from './kategorieIcon'
import { VORDEFINIERTE_KATEGORIEN } from './kategorien'
import { useApp } from './AppKontext'

export function EintragSheet({
  offen,
  zu,
  onErfasst,
}: {
  offen: boolean
  zu: () => void
  onErfasst: () => void
}) {
  const { personen, aktivePerson } = useApp()
  const reduziert = useReducedMotion()
  const [art, setArt] = useState<'ausgabe' | 'einnahme'>('ausgabe')
  const [betrag, setBetrag] = useState('')
  const [kategorie, setKategorie] = useState('')
  const [iconWahl, setIconWahl] = useState<string | null>(null)
  const [kategorien, setKategorien] = useState<KategorieInfo[]>([])
  const [konten, setKonten] = useState<Konto[]>([])
  const [konto, setKonto] = useState<string | null>(null)
  const [datum, setDatum] = useState(heuteIso())
  const [gemeinsam, setGemeinsam] = useState(false)
  const [person, setPerson] = useState<string | null>(null)
  const [zahler, setZahler] = useState<string | null>(null)
  const [fehler, setFehler] = useState<string | null>(null)
  const [laeuft, setLaeuft] = useState(false)

  useEffect(() => {
    if (!offen) return
    lese<KategorieInfo[]>('/api/kategorien')
      .then(setKategorien)
      .catch(() => setKategorien([]))
    lese<Konto[]>('/api/konten')
      .then(setKonten)
      .catch(() => setKonten([]))
    setArt('ausgabe')
    setBetrag('')
    setKategorie('')
    setIconWahl(null)
    setDatum(heuteIso())
    setGemeinsam(false)
    setPerson(aktivePerson)
    setZahler(aktivePerson)
    setKonto(null)
    setFehler(null)
  }, [offen, aktivePerson])

  const zustaendigePerson = gemeinsam ? zahler : person
  const eigeneKonten = konten.filter((k) => k.person === zustaendigePerson)
  const vorhandeneKategorien = new Set(kategorien.map((k) => k.name.toLowerCase()))
  const vorschlaege = VORDEFINIERTE_KATEGORIEN.filter(
    (v) => !vorhandeneKategorien.has(v.name.toLowerCase()),
  )

  const betragWert = betragAusEingabe(betrag)
  const splitInfo = kategorien.find((k) => k.name === kategorie)?.split
  const anteileText =
    splitInfo?.art === 'ANTEILE' && splitInfo.anteile
      ? Object.values(splitInfo.anteile).join('/')
      : null
  const schuldner = personen.map((p) => p.name).find((n) => n !== zahler) ?? null
  const schuldnerAnteil =
    splitInfo?.art === 'ANTEILE' && splitInfo.anteile && schuldner && splitInfo.anteile[schuldner] != null
      ? splitInfo.anteile[schuldner] / 100
      : 0.5
  const schuldBetrag =
    gemeinsam && betragWert !== null && betragWert !== 0 && zahler && schuldner
      ? Math.round(betragWert * schuldnerAnteil * 100) / 100
      : null

  const absenden = async (e: FormEvent) => {
    e.preventDefault()
    const wert = betragAusEingabe(betrag)
    if (wert === null || wert === 0) {
      setFehler('Bitte einen Betrag ungleich 0 eintragen.')
      return
    }
    const gerichtet = art === 'ausgabe' ? -wert : wert
    if (!kategorie.trim()) {
      setFehler('Bitte eine Kategorie wählen oder eingeben.')
      return
    }
    if (iconWahl) {
      setzeKategorieIcon(kategorie.trim(), iconWahl)
    }
    const body: EintragNeu = {
      datum,
      betrag: gerichtet,
      kategorie: kategorie.trim(),
    }
    if (gemeinsam) {
      body.gemeinsam = true
      body.zahler = zahler ?? undefined
    } else {
      body.person = person ?? undefined
    }
    if (konto) {
      body.konto = konto
    }
    setLaeuft(true)
    try {
      await schreibe<Eintrag>('/api/eintraege', body)
      zu()
      onErfasst()
    } catch (err) {
      setFehler(err instanceof ApiFehler ? err.message : 'Das hat nicht geklappt.')
    } finally {
      setLaeuft(false)
    }
  }

  const weiche = reduziert ? { duration: 0 } : { duration: 0.22, ease: [0.22, 1, 0.36, 1] as const }

  return (
    <AnimatePresence>
      {offen && (
        <div className="fixed inset-0 z-30 flex flex-col justify-end">
          <motion.button
            aria-label="Schließen"
            className="absolute inset-0 bg-black/60"
            onClick={zu}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.18 }}
          />
          <motion.form
            onSubmit={absenden}
            className="relative mx-auto max-h-[calc(100dvh-16px)] w-full max-w-[560px] overflow-y-auto overscroll-contain rounded-t-3xl border-t border-strich bg-karte px-5 pb-5 pt-4"
            initial={{ transform: 'translateY(100%)' }}
            animate={{ transform: 'translateY(0%)' }}
            exit={{ transform: 'translateY(100%)' }}
            transition={
              reduziert ? { duration: 0 } : { type: 'spring', duration: 0.5, bounce: 0.2 }
            }
            drag="y"
            dragConstraints={{ top: 0, bottom: 0 }}
            dragElastic={{ top: 0, bottom: 0.6 }}
            onDragEnd={(_, info) => {
              if (info.offset.y > 120 || info.velocity.y > 500) {
                zu()
              }
            }}
          >
            <div className="mx-auto mb-2.5 h-1 w-10 rounded-full bg-strich" />

            <div className="mb-2.5 flex gap-2">
              <div className="flex flex-1 gap-2 rounded-xl bg-karte2 p-1">
                <button
                  type="button"
                  onClick={() => setArt('ausgabe')}
                  className={`flex min-h-11 flex-1 items-center justify-center rounded-lg text-sm font-semibold transition-colors ${
                    art === 'ausgabe' ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
                  }`}
                >
                  Ausgabe
                </button>
                <button
                  type="button"
                  onClick={() => setArt('einnahme')}
                  className={`flex min-h-11 flex-1 items-center justify-center rounded-lg text-sm font-semibold transition-colors ${
                    art === 'einnahme' ? 'bg-akzentstark text-akzenttext' : 'text-textschwach'
                  }`}
                >
                  Einnahme
                </button>
              </div>
              <input
                aria-label="Betrag"
                value={betrag}
                onChange={(e) => setBetrag(e.target.value)}
                inputMode="decimal"
                placeholder="0,00"
                className="betragszahl w-32 shrink-0 rounded-xl bg-karte2 px-3 text-right text-xl font-semibold outline-none ring-strich focus:ring-2"
              />
            </div>

            <label className="mb-1 block text-xs text-textschwach">Kategorie</label>
            {kategorien.length > 0 && (
              <div className="mb-1.5 flex flex-wrap gap-2">
                {kategorien.map((k) => (
                  <button
                    type="button"
                    key={k.name}
                    onClick={() => setKategorie(k.name)}
                    className={`flex min-h-11 items-center gap-2 rounded-full py-1 pl-1 pr-3 text-sm transition-colors ${
                      kategorie === k.name ? 'bg-akzentstark text-akzenttext' : 'bg-karte2 text-textschwach'
                    }`}
                  >
                    <KategorieIconTile
                      name={k.name}
                      groesse={24}
                      iconGroesse={14}
                      iconKlasse={kategorie === k.name ? 'text-akzenttext' : undefined}
                    />
                    {k.name}
                  </button>
                ))}
              </div>
            )}
            <input
              value={kategorie}
              onChange={(e) => setKategorie(e.target.value)}
              placeholder="Neue Kategorie"
              className="mb-2 w-full rounded-xl bg-karte2 px-4 py-2.5 outline-none ring-strich focus:ring-2"
            />

            <AnimatePresence initial={false}>
              {kategorie.trim() !== '' ? (
                <motion.div
                  key="picker"
                  className="mb-2.5"
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={weiche}
                >
                  <div className="flex gap-2 overflow-x-auto overscroll-x-contain pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
                    {PICKER_ICONS.map((Icon) => {
                      const name = iconNameVon(Icon)
                      const gewaehlt = name === (iconWahl ?? kategorieIconName(kategorie))
                      return (
                        <button
                          type="button"
                          key={name}
                          aria-label={name}
                          onClick={() => setIconWahl(iconWahl === name ? null : name)}
                          className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full transition-colors ${
                            gewaehlt
                              ? 'bg-akzentstark/20 text-akzent ring-2 ring-akzent'
                              : 'bg-karte2 text-textschwach'
                          }`}
                        >
                          <Icon size={17} weight={gewaehlt ? 'fill' : 'regular'} />
                        </button>
                      )
                    })}
                  </div>
                </motion.div>
              ) : vorschlaege.length > 0 ? (
                <motion.div
                  key="vorschlaege"
                  className="mb-2.5"
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={weiche}
                >
                  <p className="mb-1 text-[11px] font-medium uppercase tracking-[0.08em] text-textschwach">
                    Vorschläge
                  </p>
                  <div className="flex flex-wrap gap-1.5">
                    {vorschlaege.map((v) => (
                      <button
                        type="button"
                        key={v.name}
                        onClick={() => {
                          setKategorie(v.name)
                          setIconWahl(iconNameVon(v.icon))
                        }}
                        className="flex min-h-8 items-center gap-1.5 rounded-full bg-karte2 px-3 text-xs text-textschwach transition-colors"
                      >
                        <v.icon size={14} className="shrink-0" />
                        {v.name}
                      </button>
                    ))}
                  </div>
                </motion.div>
              ) : null}
            </AnimatePresence>

            <input
              type="date"
              aria-label="Datum"
              value={datum}
              onChange={(e) => setDatum(e.target.value)}
              className="mb-2.5 w-full rounded-xl bg-karte2 px-4 py-2.5 outline-none ring-strich focus:ring-2"
            />

            <div className="mb-2.5 rounded-xl bg-karte2 px-4 py-2.5">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Gemeinsame Ausgabe</span>
                <button
                  type="button"
                  role="switch"
                  aria-label="Gemeinsame Ausgabe"
                  aria-checked={gemeinsam}
                  onClick={() => setGemeinsam(!gemeinsam)}
                  className={`relative h-7 w-12 shrink-0 rounded-full transition-colors before:absolute before:-inset-3 before:content-[''] ${
                    gemeinsam ? 'bg-akzentstark' : 'bg-strich'
                  }`}
                >
                  <span
                    className={`block h-7 w-7 rounded-full bg-white transition-transform duration-200 ${
                      gemeinsam ? 'translate-x-5' : 'translate-x-0'
                    }`}
                  />
                </button>
              </div>
              <p className="mt-0.5 text-[12px] text-textschwach">
                {anteileText ? `Wird ${anteileText} geteilt` : 'Wird zu gleichen Teilen geteilt'}
              </p>
            </div>

            <label className="mb-1 block text-xs text-textschwach">
              {gemeinsam ? 'Bezahlt von' : 'Person'}
            </label>
            <div className="mb-2.5 flex gap-2">
              {personen.map((p) => {
                const gewaehlt = gemeinsam ? zahler === p.name : person === p.name
                return (
                  <button
                    type="button"
                    key={p.id}
                    onClick={() => (gemeinsam ? setZahler(p.name) : setPerson(p.name))}
                    className={`flex min-h-11 flex-1 items-center justify-center rounded-xl px-3 transition-colors ${
                      gewaehlt ? 'bg-akzentstark text-akzenttext' : 'bg-karte2 text-textschwach'
                    }`}
                  >
                    {p.name}
                  </button>
                )
              })}
            </div>

            <AnimatePresence initial={false}>
              {schuldBetrag !== null && schuldner && zahler && (
                <motion.p
                  key={`${schuldner}-${zahler}-${schuldBetrag}`}
                  className="betragszahl mb-2.5 text-[13px] text-akzent"
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={weiche}
                >
                  → {schuldner} schuldet {zahler} {euro(schuldBetrag)}
                </motion.p>
              )}
            </AnimatePresence>

            <div className="mb-2.5">
              <label className="mb-1 block text-xs text-textschwach">Konto</label>
              {eigeneKonten.length > 0 ? (
                <div className="flex flex-wrap gap-2">
                  {eigeneKonten.map((k) => (
                    <button
                      type="button"
                      key={k.id}
                      onClick={() => setKonto(konto === k.name ? null : k.name)}
                      className={`flex min-h-9 items-center rounded-full px-4 text-sm transition-colors ${
                        konto === k.name ? 'bg-akzentstark text-akzenttext' : 'bg-karte2 text-textschwach'
                      }`}
                    >
                      {k.name}
                    </button>
                  ))}
                </div>
              ) : (
                zustaendigePerson && (
                  <p className="text-xs text-textschwach">{zustaendigePerson} hat noch kein Konto</p>
                )
              )}
            </div>

            {fehler && <p className="mb-2 text-sm text-schlecht">{fehler}</p>}

            <button
              type="submit"
              disabled={laeuft}
              className="w-full rounded-xl bg-akzentstark py-2.5 font-semibold text-akzenttext transition-transform active:scale-[0.98] disabled:opacity-50"
            >
              Speichern
            </button>
          </motion.form>
        </div>
      )}
    </AnimatePresence>
  )
}
