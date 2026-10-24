import { Link } from 'react-router'
import { Mail } from 'lucide-react'
import { ApiError } from '../api/client'
import { useCheckIn, useMailbox, useMe, usePenpals } from '../api/hooks'
import type { MyProfile } from '../api/types'
import { Avatar, Card, CardHead, Spinner, StampIcon } from '../components/ui'
import { matchedCount } from '../lib/domain'
import { languageName, timeAgo } from '../lib/locale'

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

      <section className="space-y-4">
        <NewLetters />
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

function NewLetters() {
  const { data, isLoading } = useMailbox('inbox', 0)
  if (isLoading) return <Spinner />
  const letters = data?.content ?? []
  if (letters.length === 0) {
    return (
      <Card className="px-6 py-12 text-center">
        <img src="/img/globe-people.svg" alt="" className="mx-auto w-44" />
        <p className="mt-5 font-display text-[15px] text-ink">No letters yet.</p>
        <p className="mt-1 text-[13px] text-muted">Find a penpal and write first — replies show up here.</p>
        <Link to="/friends" className="mt-5 inline-block rounded-[4px] bg-primary px-5 py-2 text-[13px] text-white">
          Find friends
        </Link>
      </Card>
    )
  }
  return (
    <>
      <div className="flex items-center justify-between px-1">
        <h2 className="font-display text-[15px] font-medium">New letters</h2>
        <Link to="/letters" className="text-[13px] text-primary hover:underline">
          Open letter box
        </Link>
      </div>
      {letters.slice(0, 6).map((l) => (
        <Link key={l.id} to={`/letters/${l.id}`} className="block">
          <Card className="p-5 transition-shadow hover:shadow-md">
            <div className="flex items-start gap-3">
              <Avatar name={l.counterpart.displayName} country={l.counterpart.country} size={42} />
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className={`text-[14px] ${l.readAt ? 'text-ink' : 'font-semibold text-ink'}`}>{l.counterpart.displayName}</span>
                  {!l.readAt && <span className="h-1.5 w-1.5 rounded-full bg-primary" />}
                </div>
                <span className="text-[12px] text-muted">{timeAgo(l.sentAt)}</span>
              </div>
              <StampIcon className="h-9 w-9 opacity-80" />
            </div>
            <p className="mt-3 line-clamp-2 text-[14px] leading-relaxed text-[#555]">{l.preview}</p>
          </Card>
        </Link>
      ))}
    </>
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
