import { useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { ArrowsLeftRight, CaretDown, ChartPie, GearSix, Target, UserCircle, type Icon as PhosphorIcon } from '@phosphor-icons/react'
import { useApp } from './AppKontext'

const tabs = [
  { zu: '/', label: 'Übersicht', Icon: ChartPie },
  { zu: '/ausgleich', label: 'Ausgleich', Icon: ArrowsLeftRight },
  { zu: '/sparziele', label: 'Ziele', Icon: Target },
  { zu: '/einstellungen', label: 'Mehr', Icon: GearSix },
]

export function Mond() {
  return (
    <svg width="22" height="22" viewBox="0 0 64 64" aria-hidden="true">
      <path d="M40 12a20 20 0 1 0 12 36A22 22 0 0 1 40 12z" fill="currentColor" />
    </svg>
  )
}

function Tab({
  zu,
  label,
  Icon,
}: {
  zu: string
  label: string
  Icon: PhosphorIcon
}) {
  return (
    <NavLink
      to={zu}
      className={({ isActive }) =>
        `flex flex-1 flex-col items-center gap-0.5 py-2 text-[11px] font-medium transition-colors duration-200 ${
          isActive ? 'text-akzent' : 'text-textschwach'
        }`
      }
    >
      {({ isActive }) => (
        <>
          <span className={`transition-transform duration-200 ${isActive ? 'scale-110' : ''}`}>
            <Icon size={22} weight={isActive ? 'fill' : 'regular'} />
          </span>
          {label}
        </>
      )}
    </NavLink>
  )
}

function PersonenSheet({ offen, zu }: { offen: boolean; zu: () => void }) {
  const { personen, aktivePerson, setAktivePerson } = useApp()
  const reduziert = useReducedMotion()

  const waehlen = (name: string) => {
    setAktivePerson(name)
    zu()
  }

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
            className="relative mx-auto w-full max-w-[560px] rounded-t-3xl border-t border-strich bg-karte px-5 pb-5 pt-4"
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
            <h3 className="mb-3 text-center text-sm font-semibold">Wer bist du?</h3>
            <div className="mb-2 flex gap-2">
              {personen.map((p) => (
                <button
                  key={p.id}
                  onClick={() => waehlen(p.name)}
                  className={`flex min-h-11 flex-1 items-center justify-center rounded-xl px-3 text-sm font-medium transition-colors ${
                    aktivePerson === p.name
                      ? 'bg-akzent/15 text-akzent ring-2 ring-akzent'
                      : 'bg-karte2 text-textschwach'
                  }`}
                >
                  {p.name}
                </button>
              ))}
            </div>
            <p className="text-center text-[12px] text-textschwach">
              Steuert deine Sicht und neue Einträge
            </p>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}

export function Layout({ onEintragNeu }: { onEintragNeu: () => void }) {
  const { aktivePerson } = useApp()
  const ort = useLocation()
  const zeigeFab = ort.pathname === '/'
  const [personenOffen, setPersonenOffen] = useState(false)

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-[560px] flex-col">
      <header className="sticky top-0 z-10 flex items-center justify-between bg-base/90 px-4 py-2.5 backdrop-blur">
        <div className="flex items-center gap-2">
          <span className="text-akzent">
            <Mond />
          </span>
          <span className="text-lg font-semibold tracking-wide">LuneFi</span>
        </div>
        {aktivePerson && (
          <button
            onClick={() => setPersonenOffen(true)}
            className="flex items-center gap-1 rounded-full bg-karte px-3 py-1 text-xs text-textschwach transition-colors hover:text-text"
          >
            <UserCircle size={16} aria-hidden="true" />
            {aktivePerson}
            <CaretDown size={12} aria-hidden="true" />
          </button>
        )}
      </header>

      <main className="flex-1 px-4 pb-24 pt-0.5">
        <div key={ort.pathname} className="seite">
          <Outlet />
        </div>
      </main>

      {zeigeFab && (
        <button
          onClick={onEintragNeu}
          aria-label="Eintrag erfassen"
          className="fixed bottom-24 right-4 z-20 flex h-14 w-14 items-center justify-center rounded-full bg-akzentstark text-3xl font-light text-akzenttext shadow-lg shadow-black/40 transition-transform active:scale-95 sm:right-auto sm:left-[calc(50%+208px)]"
        >
          +
        </button>
      )}

      <PersonenSheet offen={personenOffen} zu={() => setPersonenOffen(false)} />

      <nav className="fixed inset-x-0 bottom-0 z-10 mx-auto flex max-w-[560px] border-t border-strich bg-karte/95 pb-[env(safe-area-inset-bottom)] backdrop-blur">
        {tabs.map((t) => (
          <Tab key={t.zu} {...t} />
        ))}
      </nav>
    </div>
  )
}
