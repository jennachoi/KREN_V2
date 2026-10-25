import { lazy, Suspense } from 'react'
import { Link } from 'react-router'
import { Mail } from 'lucide-react'
import { ApiError } from '../api/client'
import { useCheckIn, useDashboard, useMe, usePenpals } from '../api/hooks'
import type { Flight, MyProfile } from '../api/types'
import { Avatar, Card, CardHead, Spinner } from '../components/ui'
import { matchedCount } from '../lib/domain'
import { countryName, languageName, timeAgo } from '../lib/locale'
import { flightProgress, formatRemaining, useNow } from '../lib/time'

/** v1 main page layout: left widgets, centre stream, right suggestions. */
export function HomePage() {
  const { data: me } = useMe()
  if (!me) return <Spinner />
  return (
    <div className="grid gap-5 lg:grid-cols-[240px_1fr_240px]">
      <aside className="space-y-5">
        <DailyEvent />
        <Card>
          <CardHead right={<span className="rounded-full bg-chip px-2 py-0.5 text-[12px]">{me.stampBalance}</span>}>
            My Stamp
          </CardHead>
          <div className="px-5 py-4 text-[13px] leading-relaxed text-soft">
            Every letter costs one stamp. Check in daily to earn more.
            <Link to="/profile" className="mt-2 block text-primary hover:underline">
              Stamp History →
            </Link>
          </div>
        </Card>
      </aside>

      <section className="min-w-0 space-y-4">
        <LetterDashboard />
      </section>

      <aside className="space-y-5">
        <Suggestions me={me} />
        <div className="relative overflow-hidden rounded-[6px] bg-teal px-5 py-8 text-center text-white">
          <img src="/img/globe-people.svg" alt="" className="mx-auto mb-3 w-28 opacity-90" />
          <p className="font-display text-[14px] leading-snug font-medium">
            Find a native {languageName(me.learningLang)} speaker and send them your first letter.
          </p>
          <Link
            to="/friends"
            className="mt-4 inline-block rounded-[4px] border border-white/80 px-4 py-1.5 text-[12px] font-medium hover:bg-white/10"
          >
            ✍ Write a letter
          </Link>
        </div>
      </aside>
    </div>
  )
}

function DailyEvent() {
  const checkIn = useCheckIn()
  const already = checkIn.error instanceof ApiError && checkIn.error.code === 'ALREADY_CHECKED_IN'
  const done = checkIn.isSuccess || already
  return (
    <Card>
      <CardHead>Daily Event</CardHead>
      <button
        disabled={checkIn.isPending || done}
        onClick={() => checkIn.mutate()}
        className="w-full px-5 py-6 text-center transition-colors enabled:hover:bg-canvas"
      >
        <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-[#d4f5e4]">
          <img src="/img/clover.svg" alt="" className="h-8 w-8" />
        </span>
        <span className="mt-3 block text-[13px] text-ink">
          {checkIn.isSuccess ? '+1 stamp! See you tomorrow.' : already ? 'Already checked in today.' : "Click here to receive today's stamps"}
        </span>
      </button>
    </Card>
  )
}

// The map pulls in d3-geo + world data; keep it out of the first page load.
const AirspaceMap = lazy(() => import('../components/map/AirspaceMap').then((m) => ({ default: m.AirspaceMap })))

/** Home dashboard: everything in the air on one map, then what's arriving, what's flying, what needs a reply. */
function LetterDashboard() {
  const { data, isLoading } = useDashboard()
  const now = useNow(1000)
  if (isLoading || !data) return <Spinner />

  const flights = [...data.incoming, ...data.outgoing]
  return (
    <>
      <Card className="overflow-hidden">
        <CardHead
          right={
            <span className="flex items-center gap-3 text-[11px]">
              <Legend color="bg-[#f2786d]" label="Coming to you" />
              <Legend color="bg-deep" label="On its way" />
            </span>
          }
        >
          Letters in the air
        </CardHead>
        <div className="relative">
          <Suspense fallback={<div className="aspect-[800/380] bg-[#cfe6f7]" />}>
            <AirspaceMap home={data.home} flights={flights} />
          </Suspense>
          {flights.length === 0 && (
            <div className="pointer-events-none absolute inset-x-0 bottom-4 text-center">
              <span className="rounded-full bg-white/90 px-4 py-1.5 text-[12px] text-soft shadow-sm">
                No letters in the air right now — write one and watch it fly.
              </span>
            </div>
          )}
        </div>
      </Card>

      <div className="grid grid-cols-3 gap-3">
        <Tile n={data.incoming.length} label="Arriving soon" tone="text-[#e0645a]" />
        <Tile n={data.outgoing.length} label="On its way" tone="text-deep" />
        <Tile n={data.awaitingReplyCount} label="Awaiting your reply" tone="text-[#2aa79a]" />
      </div>

      {data.incoming.length > 0 && (
        <Card>
          <CardHead>Arriving soon</CardHead>
          {data.incoming.map((f) => (
            <FlightRow key={f.id} flight={f} now={now} />
          ))}
        </Card>
      )}

      {data.outgoing.length > 0 && (
        <Card>
          <CardHead>Your letters on their way</CardHead>
          {data.outgoing.map((f) => (
            <FlightRow key={f.id} flight={f} now={now} />
          ))}
        </Card>
      )}

      <Card>
        <CardHead right={data.awaitingReplyCount > data.awaitingReply.length ? `${data.awaitingReplyCount} total` : undefined}>
          Awaiting your reply
        </CardHead>
        {data.awaitingReply.length === 0 ? (
          <p className="px-5 py-5 text-[13px] text-muted">You're all caught up. ✉</p>
        ) : (
          data.awaitingReply.map((l) => (
            <Link key={l.id} to={`/letters/${l.id}`} className="flex items-start gap-3 border-b border-line px-5 py-4 last:border-0 hover:bg-canvas">
              <Avatar name={l.counterpart.displayName} country={l.counterpart.country} size={38} />
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className={`text-[13px] ${l.readAt ? 'text-ink' : 'font-semibold text-ink'}`}>{l.counterpart.displayName}</span>
                  {!l.readAt && <span className="h-1.5 w-1.5 rounded-full bg-primary" />}
                  <span className="ml-auto text-[11px] text-muted">arrived {timeAgo(l.deliverAt)} ago</span>
                </div>
                <p className="mt-0.5 line-clamp-2 text-[13px] text-[#666]">{l.preview}</p>
              </div>
              <span className="mt-1 shrink-0 rounded-[4px] border border-line px-2.5 py-1 text-[12px] text-primary">Reply</span>
            </Link>
          ))
        )}
      </Card>
    </>
  )
}

function Legend({ color, label }: { color: string; label: string }) {
  return (
    <span className="flex items-center gap-1 text-soft">
      <span className={`h-2 w-2 rounded-full ${color}`} /> {label}
    </span>
  )
}

function Tile({ n, label, tone }: { n: number; label: string; tone: string }) {
  return (
    <Card className="px-4 py-3 text-center">
      <div className={`font-display text-[24px] font-semibold ${tone}`}>{n}</div>
      <div className="text-[11px] tracking-wide text-muted uppercase">{label}</div>
    </Card>
  )
}

function FlightRow({ flight, now }: { flight: Flight; now: number }) {
  const incoming = flight.direction === 'INCOMING'
  const progress = flightProgress(flight.sentAt, flight.deliverAt, now)
  const left = new Date(flight.deliverAt).getTime() - now
  return (
    <Link to={`/letters/${flight.id}`} className="block border-b border-line px-5 py-3.5 last:border-0 hover:bg-canvas">
      <div className="flex items-center gap-3">
        <Avatar name={flight.counterpart.displayName} country={flight.counterpart.country} size={34} />
        <div className="min-w-0 flex-1">
          <p className="text-[13px] text-ink">
            {incoming ? (
              <>From <b className="font-medium">{flight.counterpart.displayName}</b></>
            ) : (
              <>To <b className="font-medium">{flight.counterpart.displayName}</b></>
            )}
            <span className="text-muted">
              {' '}· {countryName(flight.route.from.country)} → {countryName(flight.route.to.country)}
            </span>
          </p>
          <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-[#e6e9ef]">
            <div
              className={`h-full rounded-full transition-[width] duration-1000 ease-linear ${incoming ? 'bg-[#f2786d]' : 'bg-deep'}`}
              style={{ width: `${progress * 100}%` }}
            />
          </div>
        </div>
        <div className="w-20 shrink-0 text-right">
          <div className="text-[13px] font-medium text-ink">{formatRemaining(left)}</div>
          <div className="text-[10px] text-muted">{incoming ? 'until it opens' : 'until delivery'}</div>
        </div>
      </div>
    </Link>
  )
}

function Suggestions({ me }: { me: MyProfile }) {
  const { data } = usePenpals([], 0)
  const people = (data?.content ?? []).slice().sort((a, b) => matchedCount(me, b) - matchedCount(me, a)).slice(0, 5)
  return (
    <Card>
      <CardHead>Suggest for You</CardHead>
      {people.length === 0 ? (
        <p className="px-5 py-5 text-[13px] text-muted">No matches yet.</p>
      ) : (
        people.map((p) => (
          <div key={p.id} className="flex items-center gap-3 border-b border-line px-4 py-3 last:border-0">
            <Link to={`/users/${p.id}`} className="flex min-w-0 flex-1 items-center gap-3">
              <Avatar name={p.displayName} country={p.country} size={36} />
              <span className="min-w-0">
                <span className="block truncate text-[13px] text-ink">{p.displayName}</span>
                <span className="block text-[11px] text-muted">Matched : {matchedCount(me, p)}</span>
              </span>
            </Link>
            <Link to={`/compose/${p.id}`} title="Write a letter" className="text-[#aaa] hover:text-primary">
              <Mail size={16} strokeWidth={1.6} />
            </Link>
          </div>
        ))
      )}
    </Card>
  )
}
