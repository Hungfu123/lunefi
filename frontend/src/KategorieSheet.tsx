import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import type { Eintrag } from './types'
import { datumKurz, euro, istDieseWoche } from './format'
import { KategorieIconTile } from './kategorieIcon'

function GeteiltPill() {
  return <span className="text-textschwach">geteilt</span>
}

export function KategorieSheet({
  name,
  eintraege,
  zu,
}: {
  name: string | null
  eintraege: Eintrag[]
  zu: () => void
}) {
  const reduziert = useReducedMotion()
  const offen = name !== null

  const liste = [...eintraege].sort((a, b) =>
    a.datum === b.datum ? b.id - a.id : a.datum < b.datum ? 1 : -1,
  )
  const summe = liste.reduce((s, eintrag) => s + eintrag.betrag, 0)
  const woche = liste.reduce((s, eintrag) => s + (istDieseWoche(eintrag.datum) ? eintrag.betrag : 0), 0)

  return (
    <AnimatePresence>
      {offen && name !== null && (
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
            <div className="mb-3 flex items-center gap-3">
              <KategorieIconTile name={name} groesse={32} iconGroesse={18} />
              <h3 className="text-lg font-semibold">{name}</h3>
            </div>
            <p
              className={`betragszahl text-[34px] font-semibold leading-none tracking-tight ${
                summe < 0 ? 'text-schlecht' : summe > 0 ? 'text-gut' : 'text-text'
              }`}
            >
              {euro(summe)}
            </p>
            <p className="mt-1 text-sm text-textschwach">
              Diese Woche:{' '}
              <span className="betragszahl font-medium text-text">{euro(woche)}</span>
            </p>
            {liste.length === 0 ? (
              <p className="mt-4 text-sm text-textschwach">Keine Einträge in diesem Monat.</p>
            ) : (
              <ul className="mt-4">
                {liste.map((eintrag, i) => (
                  <li
                    key={eintrag.id}
                    className={`flex items-center justify-between gap-3 py-2 ${
                      i < liste.length - 1 ? 'border-b border-strich' : ''
                    }`}
                  >
                    <p className="flex min-w-0 flex-wrap items-center gap-1.5 text-sm text-textschwach">
                      <span className="font-medium text-text">{datumKurz(eintrag.datum)}</span>
                      <span aria-hidden="true">·</span>
                      {eintrag.gemeinsam ? (
                        <>
                          <GeteiltPill />
                          {eintrag.zahler && (
                            <>
                              <span aria-hidden="true">·</span>
                              <span>{eintrag.zahler}</span>
                            </>
                          )}
                        </>
                      ) : (
                        <span>{eintrag.person}</span>
                      )}
                    </p>
                    <span
                      className={`betragszahl shrink-0 text-[15px] font-semibold ${
                        eintrag.betrag < 0 ? 'text-schlecht' : 'text-gut'
                      }`}
                    >
                      {eintrag.betrag > 0 ? '+' : ''}
                      {euro(eintrag.betrag)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}
