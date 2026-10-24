import { useEffect, useState } from 'react'
import { Navigate, NavLink, Outlet, useNavigate } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/client'
import { useCheckIn, useLogout, useMe } from '../api/hooks'
import type { LetterSummary } from '../api/types'
import { Spinner } from './ui'

/** Shell for every signed-in page: nav, stamp balance, daily check-in, live letter notifications. */
export function Layout() {
  const { data: me, isLoading } = useMe()
  const toast = useLetterNotifications(!!me)

  if (isLoading) return <Spinner />
  if (!me) return <Navigate to="/login" replace />

  return (
    <div className="min-h-screen">
      <Header balance={me.stampBalance} name={me.displayName} />
      <main className="mx-auto max-w-4xl px-4 py-8">
        <Outlet />
      </main>
      {toast}
    </div>
  )
}

function Header({ balance, name }: { balance: number; name: string }) {
  const navigate = useNavigate()
  const logout = useLogout()
  const checkIn = useCheckIn()
  const already = checkIn.error instanceof ApiError && checkIn.error.code === 'ALREADY_CHECKED_IN'

  const link = ({ isActive }: { isActive: boolean }) =>
    `rounded-lg px-3 py-1.5 text-sm ${isActive ? 'bg-ink text-white' : 'text-muted hover:text-ink'}`

  return (
    <header className="border-b border-line bg-card/80 backdrop-blur">
      <div className="mx-auto flex max-w-4xl flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3">
        <NavLink to="/penpals" className="font-serif text-xl font-semibold tracking-tight text-stamp">
          KREN
        </NavLink>
        <nav className="flex gap-1">
          <NavLink to="/penpals" className={link}>
            Find penpals
          </NavLink>
          <NavLink to="/inbox" className={link}>
            Letters
          </NavLink>
          <NavLink to="/profile" className={link}>
            {name}
          </NavLink>
        </nav>
        <div className="ml-auto flex items-center gap-2">
          <span
            className="rounded-full border border-stamp/30 bg-stamp/5 px-3 py-1 text-sm font-medium text-stamp"
            title="Stamps: each letter costs one"
          >
            ✉ {balance}
          </span>
          <button
            className="rounded-lg px-2 py-1 text-xs text-muted hover:text-ink disabled:opacity-50"
            disabled={checkIn.isPending || checkIn.isSuccess || already}
            onClick={() => checkIn.mutate()}
          >
            {checkIn.isSuccess ? '+1 today ✓' : already ? 'Checked in ✓' : 'Daily check-in'}
          </button>
          <button
            className="rounded-lg px-2 py-1 text-xs text-muted hover:text-ink"
            onClick={() => logout.mutate(undefined, { onSettled: () => navigate('/login') })}
          >
            Log out
          </button>
        </div>
      </div>
    </header>
  )
}

/** Server-Sent Events: the backend pushes `letter` events the moment a letter to us is committed. */
function useLetterNotifications(enabled: boolean) {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [latest, setLatest] = useState<LetterSummary | null>(null)

  useEffect(() => {
    if (!enabled) return
    const source = new EventSource('/api/notifications/stream')
    source.addEventListener('letter', (e) => {
      setLatest(JSON.parse((e as MessageEvent).data))
      qc.invalidateQueries({ queryKey: ['letters'] })
    })
    return () => source.close()
  }, [enabled, qc])

  useEffect(() => {
    if (!latest) return
    const t = setTimeout(() => setLatest(null), 6000)
    return () => clearTimeout(t)
  }, [latest])

  if (!latest) return null
  return (
    <button
      onClick={() => {
        navigate(`/letters/${latest.id}`)
        setLatest(null)
      }}
      className="fixed right-4 bottom-4 left-4 mx-auto max-w-sm rounded-2xl border border-line bg-card p-4 text-left shadow-lg sm:left-auto"
    >
      <div className="text-xs font-medium tracking-wide text-stamp uppercase">New letter</div>
      <div className="mt-1 text-sm">
        <b>{latest.counterpart.displayName}</b> wrote to you
      </div>
      <div className="mt-1 line-clamp-2 text-sm text-muted">{latest.preview}</div>
    </button>
  )
}
