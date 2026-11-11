import { useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router'
import { ArrowsLeftRight, ChartLineUp, ChartPie, GearSix, SignOut, Target, UserCircle, type Icon as PhosphorIcon } from '@phosphor-icons/react'
import { useApp } from './AppContext'

const tabs = [
  { to: '/', label: 'Übersicht', Icon: ChartPie },
  { to: '/ausgleich', label: 'Ausgleich', Icon: ArrowsLeftRight },
  { to: '/sparziele', label: 'Ziele', Icon: Target },
  { to: '/statistik', label: 'Statistik', Icon: ChartLineUp },
  { to: '/einstellungen', label: 'Mehr', Icon: GearSix },
]

export function Mond() {
  return (
    <svg width="22" height="22" viewBox="0 0 64 64" aria-hidden="true">
      <path d="M40 12a20 20 0 1 0 12 36A22 22 0 1 1 40 12z" fill="currentColor" />
    </svg>
  )
}

function Tab({
  to,
  label,
  Icon,
}: {
  to: string
  label: string
  Icon: PhosphorIcon
}) {
  return (
    <NavLink
      to={to}
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

export function Layout({
  onNewEntry,
  canLogOut,
  onLoggedOut,
}: {
  onNewEntry: () => void
  canLogOut: boolean
  onLoggedOut: () => void
}) {
  const { activePerson } = useApp()
  const location = useLocation()
  const showFab = location.pathname === '/'
  const [confirmed, setConfirmed] = useState(false)

  const logout = () => {
    if (!confirmed) {
      setConfirmed(true)
      window.setTimeout(() => setConfirmed(false), 3000)
      return
    }
    setConfirmed(false)
    onLoggedOut()
  }

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-[560px] flex-col">
      <header className="sticky top-0 z-10 flex items-center justify-between bg-base/90 px-4 py-2.5 backdrop-blur">
        <div className="flex items-center gap-2">
          <span className="text-akzent">
            <Mond />
          </span>
          <span className="text-lg font-semibold tracking-wide">LuneFi</span>
        </div>
        <div className="flex items-center gap-1.5">
          {activePerson && (
            <span className="flex items-center gap-1 rounded-full bg-karte px-3 py-1 text-xs text-textschwach">
              <UserCircle size={16} aria-hidden="true" />
              {activePerson}
            </span>
          )}
          {canLogOut && (
            <button
              onClick={logout}
              aria-label={confirmed ? 'Wirklich abmelden?' : 'Abmelden'}
              className={`flex h-8 items-center justify-center rounded-full px-2.5 text-[11px] font-medium transition-colors ${
                confirmed ? 'bg-schlecht/15 text-schlecht' : 'text-textschwach hover:text-text'
              }`}
            >
              {confirmed ? 'Sicher?' : <SignOut size={16} aria-hidden="true" />}
            </button>
          )}
        </div>
      </header>

      <main className="flex-1 px-4 pb-24 pt-0.5">
        <div key={location.pathname} className="seite">
          <Outlet />
        </div>
      </main>

      {showFab && (
        <button
          onClick={onNewEntry}
          aria-label="Eintrag erfassen"
          className="fixed bottom-24 right-4 z-20 flex h-14 w-14 items-center justify-center rounded-full bg-akzentstark text-3xl font-light text-akzenttext shadow-lg shadow-black/40 transition-transform active:scale-95 sm:right-auto sm:left-[calc(50%+208px)]"
        >
          +
        </button>
      )}

      <nav className="fixed inset-x-0 bottom-0 z-10 mx-auto flex max-w-[560px] border-t border-strich bg-karte/95 pb-[env(safe-area-inset-bottom)] backdrop-blur">
        {tabs.map((t) => (
          <Tab key={t.to} {...t} />
        ))}
      </nav>
    </div>
  )
}
