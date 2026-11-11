import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import {
  ArrowsLeftRight,
  ChartPie,
  LockSimple,
  PiggyBank,
  Scales,
  Tag,
  type Icon,
} from '@phosphor-icons/react'

function Abschnitt({ icon: IconKomponente, titel, text }: { icon: Icon; titel: string; text: string }) {
  return (
    <div>
      <div className="mb-1 flex items-center gap-2.5">
        <IconKomponente size={18} className="shrink-0 text-akzent" />
        <h4 className="font-semibold">{titel}</h4>
      </div>
      <p className="pl-[30px] text-sm leading-relaxed text-textschwach">{text}</p>
    </div>
  )
}

export function HilfeModal({ offen, zu }: { offen: boolean; zu: () => void }) {
  const reduziert = useReducedMotion()

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
          <motion.div
            className="relative mx-auto max-h-[calc(100dvh-16px)] w-full max-w-[560px] overflow-y-auto overscroll-contain rounded-t-3xl border-t border-strich bg-karte px-5 pb-5 pt-4"
            initial={{ transform: 'translateY(100%)' }}
            animate={{ transform: 'translateY(0%)' }}
            exit={{ transform: 'translateY(100%)' }}
            transition={reduziert ? { duration: 0 } : { type: 'spring', duration: 0.5, bounce: 0.2 }}
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
            <h3 className="mb-4 text-lg font-semibold">So funktioniert LuneFi</h3>
            <div className="flex flex-col gap-4">
              <Abschnitt
                icon={ArrowsLeftRight}
                titel="Gemeinsame Ausgaben"
                text="Ein Einkauf, der euch beiden gehört. Der Zahler legt hin. Ihr teilt den Betrag."
              />
              <Abschnitt
                icon={Scales}
                titel="Ausgleich"
                text="Wer mehr bezahlt hat, bekommt den Rest vom anderen. Ihr macht das einmal im Monat. Danach auf „Ausgeglichen“ tippen."
              />
              <Abschnitt
                icon={PiggyBank}
                titel="Sparen"
                text="Ein Sparziel zeigt, wie weit ihr seid. „Gespart“ vermerkt nur euren Fortschritt. Es wird kein Geld überwiesen."
              />
              <Abschnitt
                icon={ChartPie}
                titel="Budget"
                text="Ein Limit je Kategorie. Liegt ihr darüber, warnt euch die App. Ihr setzt die Limits selbst."
              />
              <Abschnitt
                icon={LockSimple}
                titel="Privat"
                text="Jeder sieht nur sein eigenes Geld. Geteilt sind nur die Dinge, die ihr zusammen macht."
              />
              <Abschnitt
                icon={Tag}
                titel="Kategorien"
                text="Kategorien bekommen automatisch passende Icons. Tippt eine an, seht ihr alles, was dazugehört."
              />
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}
