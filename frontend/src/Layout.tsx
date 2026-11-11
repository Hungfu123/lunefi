import { NavLink, Outlet, useLocation } from 'react-router'
import { ArrowsLeftRight, ChartPie, GearSix, Target, type Icon as PhosphorIcon } from '@phosphor-icons/react'
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

export function Layout({ onEintragNeu }: { onEintragNeu: () => void }) {
  const { aktivePerson } = useApp()
  const ort = useLocation()
  const zeigeFab = ort.pathname === '/'

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-[560px] flex-col">
      <header className="sticky top-0 z-10 flex items-center justify-between bg-base/90 px-4 py-3 backdrop-blur">
        <div className="flex items-center gap-2">
          <span className="text-akzent">
            <Mond />
          </span>
          <span className="text-lg font-semibold tracking-wide">LuneFi</span>
        </div>
        {aktivePerson && (
          <span className="rounded-full bg-karte px-3 py-1 text-xs text-textschwach">{aktivePerson}</span>
        )}
      </header>

      <main className="flex-1 px-4 pb-40 pt-2">
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

      <nav className="fixed inset-x-0 bottom-0 z-10 mx-auto flex max-w-[560px] border-t border-strich bg-karte/95 pb-[env(safe-area-inset-bottom)] backdrop-blur">
        {tabs.map((t) => (
          <Tab key={t.zu} {...t} />
        ))}
      </nav>
    </div>
  )
}
