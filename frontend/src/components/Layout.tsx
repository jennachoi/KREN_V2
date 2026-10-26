import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, NavLink, Outlet, useNavigate } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { Bell, Home, LogOut, Mail, SpellCheck, User, Users } from 'lucide-react'
import { useLogout, useMe } from '../api/hooks'
import type { Correction, LetterSummary } from '../api/types'
import { timeAgo } from '../lib/locale'
import { Avatar, Spinner } from './ui'

type Notice =
  | { kind: 'incoming' | 'letter'; letter: LetterSummary; at: string }
  | { kind: 'correction'; correction: Correction; at: string }

function noticeText(n: Notice) {
  switch (n.kind) {
    case 'incoming':
      return { title: `A letter from ${n.letter.counterpart.displayName} is coming.`, sub: 'Track it on the map.', href: `/letters/${n.letter.id}` }
    case 'letter':
      return { title: `A letter from ${n.letter.counterpart.displayName} has arrived!`, sub: n.letter.preview ?? '', href: `/letters/${n.letter.id}` }
    case 'correction':
      return { title: `${n.correction.corrector.displayName} corrected your letter.`, sub: `${n.correction.lines.length} sentence(s) — see your notebook.`, href: `/letters/${n.correction.letterId}` }
  }
}

/** Shell for every signed-in page: the v1 top bar, live letter notifications, page content. */
export function Layout() {
  const { data: me, isLoading } = useMe()
  const notices = useLetterNotifications(!!me)

  if (isLoading) return <Spinner />
  if (!me) return <Navigate to="/login" replace />

  return (
    <div className="min-h-screen">
      <Navbar name={me.displayName} country={me.country} avatar={me.avatar} notices={notices} />
      {/* bottom padding leaves room for the mobile tab bar */}
      <main className="mx-auto max-w-[1280px] px-3 pt-5 pb-24 sm:px-4 sm:py-7">
        <Outlet />
      </main>
      <MobileTabBar />
    </div>
  )
}

const NAV = [
  { to: '/', label: 'HOME', short: 'Home', icon: Home, end: true },
  { to: '/friends', label: 'FRIENDS', short: 'Friends', icon: Users, end: false },
  { to: '/letters', label: 'LETTER', short: 'Letters', icon: Mail, end: false },
  { to: '/notebook', label: 'NOTEBOOK', short: 'Notebook', icon: SpellCheck, end: false },
]

/** Phones: the four sections move to a fixed bottom bar, like a native app. */
function MobileTabBar() {
  return (
    <nav className="fixed inset-x-0 bottom-0 z-20 border-t border-line bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden">
      <div className="grid grid-cols-4">
        {NAV.map(({ to, short, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `flex flex-col items-center gap-0.5 py-2 text-[11px] ${isActive ? 'text-primary' : 'text-[#999]'}`
            }
          >
            <Icon size={20} strokeWidth={1.7} />
            {short}
          </NavLink>
        ))}
      </div>
    </nav>
  )
}

function Navbar({
  name,
  country,
  avatar,
  notices,
}: {
  name: string
  country: string
  avatar: string | null
  notices: ReturnType<typeof useLetterNotifications>
}) {
  const link = ({ isActive }: { isActive: boolean }) =>
    `font-display text-[15px] tracking-wide transition-colors ${isActive ? 'text-primary' : 'text-[#999] hover:text-ink'}`

  return (
    <header className="sticky top-0 z-20 border-b border-line bg-white">
      <div className="mx-auto flex h-[54px] max-w-[1240px] items-center gap-8 px-3 sm:h-[58px] sm:px-4">
        <Link to="/" className="shrink-0">
          <img src="/logo.png" alt="KREN" className="h-8 sm:h-9" />
        </Link>
        <nav className="hidden gap-6 md:flex">
          {NAV.map(({ to, label, end }) => (
            <NavLink key={to} to={to} end={end} className={link}>
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="ml-auto flex items-center gap-4 text-ink">
          <NotificationBell {...notices} />
          <Link to="/letters" title="Letters" className="hidden text-[#555] hover:text-primary md:flex">
            <Mail size={18} strokeWidth={1.6} />
          </Link>
          <AccountMenu name={name} country={country} avatar={avatar} />
        </div>
      </div>
    </header>
  )
}

function useClickOutside(onOutside: () => void) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) onOutside()
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [onOutside])
  return ref
}

function NotificationBell({ items, unseen, markSeen, clear }: ReturnType<typeof useLetterNotifications>) {
  const [open, setOpen] = useState(false)
  const ref = useClickOutside(() => setOpen(false))
  const navigate = useNavigate()

  return (
    <div ref={ref} className="relative flex">
      <button
        title="Notifications"
        className="relative flex text-[#555] hover:text-primary"
        onClick={() => {
          setOpen((o) => !o)
          markSeen()
        }}
      >
        <Bell size={18} strokeWidth={1.6} />
        {unseen && <span className="absolute -top-0.5 -right-0.5 h-2 w-2 rounded-full bg-primary" />}
      </button>
      {open && (
        <div className="fixed inset-x-3 top-[58px] overflow-hidden rounded-[6px] border border-line bg-white shadow-lg sm:absolute sm:inset-x-auto sm:top-auto sm:right-0 sm:mt-3 sm:w-80">
          <div className="flex items-center justify-between border-b border-line px-4 py-3">
            <span className="font-display text-[14px] text-[#9aa0b4]">Notifications</span>
            <Bell size={15} className="text-[#9aa0b4]" />
          </div>
          <div className="max-h-80 overflow-y-auto">
            {items.length === 0 ? (
              <p className="px-4 py-6 text-center text-[13px] text-muted">No new notifications.</p>
            ) : (
              items.map((n, i) => {
                const t = noticeText(n)
                return (
                  <button
                    key={`${n.kind}-${n.at}-${i}`}
                    onClick={() => {
                      setOpen(false)
                      navigate(t.href)
                    }}
                    className="block w-full border-b border-line px-4 py-3 text-left last:border-0 hover:bg-canvas"
                  >
                    <p className="text-[13px] text-ink">{t.title}</p>
                    {t.sub && <p className="mt-0.5 truncate text-[12px] text-muted">{t.sub}</p>}
                    <p className="mt-1 text-[11px] text-[#bbb]">{timeAgo(n.at)}</p>
                  </button>
                )
              })
            )}
          </div>
          {items.length > 0 && (
            <button onClick={clear} className="w-full border-t border-line py-2.5 text-[13px] text-soft hover:bg-canvas">
              Clear all
            </button>
          )}
        </div>
      )}
    </div>
  )
}

function AccountMenu({ name, country, avatar }: { name: string; country: string; avatar: string | null }) {
  const [open, setOpen] = useState(false)
  const ref = useClickOutside(() => setOpen(false))
  const navigate = useNavigate()
  const logout = useLogout()

  return (
    <div ref={ref} className="relative">
      <button className="flex items-center gap-2" onClick={() => setOpen((o) => !o)}>
        <Avatar name={name} country={country} avatar={avatar} size={30} />
        <span className="hidden text-[13px] text-ink sm:inline">{name}</span>
      </button>
      {open && (
        <div className="absolute right-0 mt-3 w-44 overflow-hidden rounded-[6px] border border-line bg-white py-1 text-[13px] shadow-lg">
          <button
            className="flex w-full items-center gap-2 px-4 py-2 hover:bg-canvas"
            onClick={() => {
              setOpen(false)
              navigate('/profile')
            }}
          >
            <User size={15} /> My profile
          </button>
          <button
            className="flex w-full items-center gap-2 px-4 py-2 hover:bg-canvas"
            onClick={() => logout.mutate(undefined, { onSettled: () => navigate('/login') })}
          >
            <LogOut size={15} /> Logout
          </button>
        </div>
      )}
    </div>
  )
}

/** Server-Sent Events: the backend pushes `letter` events the moment a letter to us is committed. */
function useLetterNotifications(enabled: boolean) {
  const qc = useQueryClient()
  const [items, setItems] = useState<Notice[]>([])
  const [unseen, setUnseen] = useState(false)

  useEffect(() => {
    if (!enabled) return
    const source = new EventSource('/api/notifications/stream')
    const add = (n: Notice) => {
      setItems((prev) => [n, ...prev].slice(0, 20))
      setUnseen(true)
    }
    const at = () => new Date().toISOString()
    // takeoff: a letter to me is in the air
    source.addEventListener('incoming', (e) => {
      add({ kind: 'incoming', letter: JSON.parse((e as MessageEvent).data), at: at() })
      qc.invalidateQueries({ queryKey: ['letters'] })
    })
    // landing: it's readable now
    source.addEventListener('letter', (e) => {
      const letter: LetterSummary = JSON.parse((e as MessageEvent).data)
      add({ kind: 'letter', letter, at: at() })
      qc.invalidateQueries({ queryKey: ['letters'] })
      qc.invalidateQueries({ queryKey: ['letter', letter.id] })
    })
    source.addEventListener('correction', (e) => {
      const correction: Correction = JSON.parse((e as MessageEvent).data)
      add({ kind: 'correction', correction, at: at() })
      qc.invalidateQueries({ queryKey: ['letter', correction.letterId] })
      qc.invalidateQueries({ queryKey: ['notebook'] })
    })
    return () => source.close()
  }, [enabled, qc])

  return { items, unseen, markSeen: () => setUnseen(false), clear: () => setItems([]) }
}
