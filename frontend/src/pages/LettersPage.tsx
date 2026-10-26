import { lazy, Suspense, useEffect, useRef, useState } from 'react'
import { Link, Navigate, useParams, useSearchParams } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { ChevronDown, ChevronUp, Navigation, PenLine, Plane, Reply, SpellCheck } from 'lucide-react'
import { ApiError } from '../api/client'
import { useConversations, useCorrection, useLetter, useMe, useThread } from '../api/hooks'
import { CorrectionView } from '../components/Corrections'
import type { ConversationSummary, Letter, MyProfile } from '../api/types'
import { Avatar, Button, Card, ErrorText, Pager, Spinner } from '../components/ui'
import { countryName, letterDate, timeAgo } from '../lib/locale'
import { formatRemaining, useNow } from '../lib/time'

// The tracker pulls in the world map + d3-geo; load it only when a letter is actually in flight.
const Tracker = lazy(() => import('../components/Tracker').then((m) => ({ default: m.Tracker })))

/**
 * The mailbox by person: penpals on the left (newest activity first), the whole correspondence with
 * the selected one on the right, oldest at the top like a stack of letters. /letters and
 * /letters/with/:userId (?letter= opens a specific one).
 */
export function LettersPage() {
  const params = useParams()
  const penpalId = params.userId ? Number(params.userId) : null
  const [search] = useSearchParams()
  const focusId = search.get('letter') ? Number(search.get('letter')) : null

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,4fr)_minmax(0,8fr)]">
      <div className={`lg:h-[calc(100vh-150px)] lg:overflow-y-auto lg:pr-1 ${penpalId ? 'max-lg:hidden' : ''}`}>
        <ConversationList selectedId={penpalId} />
      </div>
      <div className="lg:h-[calc(100vh-150px)] lg:overflow-y-auto lg:border-l lg:border-line lg:pl-6">
        {penpalId ? (
          <ThreadView key={penpalId} penpalId={penpalId} focusId={focusId} />
        ) : (
          <div className="hidden h-full min-h-[300px] items-center justify-center text-[13px] text-muted lg:flex">
            Pick a penpal to read your letters together.
          </div>
        )}
      </div>
    </div>
  )
}

/** Old links (notifications, the map, the notebook) point at a letter: open its conversation. */
export function LetterRedirect() {
  const id = Number(useParams().id)
  const { data: me } = useMe()
  const { data: letter, error } = useLetter(id)
  if (error instanceof ApiError && (error.status === 403 || error.status === 404)) {
    return <Card className="px-6 py-12 text-center text-[13px] text-muted">This letter isn't available.</Card>
  }
  if (!letter || !me) return error ? <ErrorText error={error} /> : <Spinner />
  const penpal = letter.sender.id === me.id ? letter.recipient : letter.sender
  return <Navigate to={`/letters/with/${penpal.id}?letter=${id}`} replace />
}

function ConversationList({ selectedId }: { selectedId: number | null }) {
  const [page, setPage] = useState(0)
  const { data, isLoading, error } = useConversations(page)

  return (
    <>
      <h2 className="mb-3 font-display text-[15px] font-medium text-ink">Letters</h2>
      <ErrorText error={error} />
      {isLoading ? (
        <Spinner />
      ) : data?.content.length === 0 ? (
        <Card className="px-6 py-12 text-center">
          <img src="/img/globe-people.svg" alt="" className="mx-auto w-36" />
          <p className="mt-4 text-[13px] text-muted">No letters yet.</p>
          <Link to="/friends" className="mt-3 inline-block text-[13px] text-primary hover:underline">
            Find friends →
          </Link>
        </Card>
      ) : (
        <div className="space-y-2">
          {data?.content.map((c) => (
            <ConversationRow key={c.penpal.id} c={c} selected={c.penpal.id === selectedId} />
          ))}
        </div>
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />}
    </>
  )
}

function ConversationRow({ c, selected }: { c: ConversationSummary; selected: boolean }) {
  const yourTurn = c.awaitingReply > 0 && !c.lastFromMe
  const preview =
    c.lastLetter.preview == null ? '✈ A letter is on its way to you' : `${c.lastFromMe ? 'You: ' : ''}${c.lastLetter.preview}`
  return (
    <Link
      to={`/letters/with/${c.penpal.id}`}
      className={`flex gap-3 rounded-[6px] border px-4 py-3 transition-shadow ${
        selected ? 'border-primary/40 bg-primary/5' : 'border-line bg-white hover:shadow-md'
      }`}
    >
      <Avatar name={c.penpal.displayName} country={c.penpal.country} avatar={c.penpal.avatar} size={42} />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <span className={`truncate text-[14px] ${c.unread > 0 ? 'font-semibold text-ink' : 'text-soft'}`}>
            {c.penpal.displayName}
          </span>
          <span className="ml-auto shrink-0 text-[11px] text-muted">{timeAgo(c.lastLetter.sentAt)}</span>
        </div>
        <p className={`mt-0.5 truncate text-[12px] ${c.unread > 0 ? 'text-ink' : 'text-[#888]'}`}>{preview}</p>
        {(yourTurn || c.inFlight > 0 || c.unread > 0) && (
          <div className="mt-1.5 flex flex-wrap items-center gap-1.5 text-[11px]">
            {yourTurn && <span className="rounded-full bg-coral/10 px-2 py-0.5 text-coral">Your turn</span>}
            {c.inFlight > 0 && (
              <span className="flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-primary">
                <Plane size={11} /> {c.inFlight} in the air
              </span>
            )}
            {c.unread > 0 && (
              <span className="ml-auto rounded-full bg-primary px-1.5 text-[10px] font-semibold leading-4 text-white">
                {c.unread}
              </span>
            )}
          </div>
        )}
      </div>
    </Link>
  )
}

function ThreadView({ penpalId, focusId }: { penpalId: number; focusId: number | null }) {
  const { data: me } = useMe()
  const thread = useThread(penpalId)
  // Which letters are unfolded. Decided once when the thread first loads (the target plus anything
  // unread); reading a letter must not fold it back up when its unread flag clears.
  const [opened, setOpened] = useState<Set<number> | null>(null)
  const scrolled = useRef(false)

  const letters = thread.data ? [...thread.data.pages].reverse().flatMap((p) => p.letters) : []
  const penpal = thread.data?.pages[0].penpal
  const newest = letters[letters.length - 1]
  const target = focusId ?? newest?.id

  // Open on the letter you came for (or the newest), the way you'd pick up the top of the pile.
  useEffect(() => {
    if (scrolled.current || target == null) return
    scrolled.current = true
    requestAnimationFrame(() => document.getElementById(`letter-${target}`)?.scrollIntoView({ block: 'start' }))
  }, [target])

  if (thread.error instanceof ApiError && thread.error.status === 404) {
    return <Card className="px-6 py-12 text-center text-[13px] text-muted">This person isn't available.</Card>
  }
  if (thread.isLoading || !me || !penpal) return thread.error ? <ErrorText error={thread.error} /> : <Spinner />

  const fromThem = (l: Letter) => l.sender.id !== me.id
  const lastTheirs = [...letters].reverse().find((l) => fromThem(l) && l.delivered)
  const lastMine = [...letters].reverse().find((l) => !fromThem(l))
  const myTurn = lastTheirs && (!lastMine || lastMine.sentAt < lastTheirs.sentAt)
  if (opened === null) {
    setOpened(new Set(letters.filter((l) => l.id === target || (fromThem(l) && l.delivered && !l.readAt)).map((l) => l.id)))
  }
  const isOpen = (l: Letter) => opened?.has(l.id) ?? false
  const toggle = (l: Letter) => {
    const next = new Set(opened)
    if (next.has(l.id)) next.delete(l.id)
    else next.add(l.id)
    setOpened(next)
  }

  return (
    <div className="space-y-3 pb-4">
      <Link to="/letters" className="text-[13px] text-muted hover:text-ink lg:hidden">
        ← All letters
      </Link>
      <div className="flex items-center gap-3 border-b border-line pb-3">
        <Link to={`/users/${penpal.id}`}>
          <Avatar name={penpal.displayName} country={penpal.country} avatar={penpal.avatar} size={44} />
        </Link>
        <div className="min-w-0">
          <Link to={`/users/${penpal.id}`} className="text-[15px] text-ink hover:text-primary">
            {penpal.displayName}
          </Link>
          <p className="text-[12px] text-muted">
            {countryName(penpal.country)} · {letters.length}
            {thread.hasNextPage ? '+' : ''} letter{letters.length === 1 ? '' : 's'}
          </p>
        </div>
      </div>

      {thread.hasNextPage && (
        <div className="text-center">
          <Button variant="white" disabled={thread.isFetchingNextPage} onClick={() => thread.fetchNextPage()}>
            Show earlier letters
          </Button>
        </div>
      )}

      {letters.map((l) => (
        <ThreadLetter
          key={l.id}
          letter={l}
          me={me}
          open={isOpen(l)}
          onToggle={() => toggle(l)}
        />
      ))}

      <div className="sticky bottom-0 -mx-1 flex flex-wrap items-center justify-end gap-2 border-t border-line bg-canvas/95 px-1 pt-3 pb-1 backdrop-blur max-md:bottom-16">
        {myTurn ? (
          <>
            <span className="mr-auto text-[12px] text-muted">{penpal.displayName} is waiting for your reply.</span>
            <Link to={`/letters/${lastTheirs.id}/reply`}>
              <Button>
                <Reply size={14} /> Reply{lastTheirs.corrected ? '' : ' & correct'}
              </Button>
            </Link>
          </>
        ) : (
          <>
            {lastMine && !lastMine.delivered && (
              <span className="mr-auto text-[12px] text-muted">Your letter is on its way.</span>
            )}
            <Link to={`/compose/${penpal.id}`}>
              <Button variant="white">
                <PenLine size={14} /> Write a new letter
              </Button>
            </Link>
          </>
        )}
      </div>
    </div>
  )
}

/**
 * One letter in the thread. Folded, it's a single line (who, when, first words); unfolded, the full
 * letter with its correction and, while flying, the map. Yours sit on the right in blue.
 */
function ThreadLetter({ letter, me, open, onToggle }: { letter: Letter; me: MyProfile; open: boolean; onToggle: () => void }) {
  const now = useNow(30_000)
  const qc = useQueryClient()
  const mine = letter.sender.id === me.id
  const sealed = letter.body == null
  const inFlight = new Date(letter.deliverAt).getTime() > now
  const [showCorrection, setShowCorrection] = useState(false)
  const { data: correction } = useCorrection(letter.id, open && letter.corrected && (showCorrection || !mine))
  const refetch = () => qc.invalidateQueries({ queryKey: ['letters', 'thread'] })
  const unread = !mine && letter.delivered && !letter.readAt

  const summary = sealed
    ? `✈ On its way · arrives in ${formatRemaining(new Date(letter.deliverAt).getTime() - now)}`
    : letter.body!.split('\n').find((line) => line.trim()) ?? ''

  return (
    <div id={`letter-${letter.id}`} className={`scroll-mt-4 ${mine ? 'ml-6 sm:ml-12' : 'mr-6 sm:mr-12'}`}>
      <div
        className={`rounded-[6px] border ${mine ? 'border-primary/20 bg-[#f4f8fe]' : 'border-line bg-white'} ${
          open ? 'shadow-[0_4px_14px_rgba(0,0,0,0.06)]' : ''
        }`}
      >
        <button type="button" onClick={onToggle} className="flex w-full items-center gap-3 px-4 py-3 text-left">
          <Avatar name={letter.sender.displayName} country={letter.sender.country} avatar={letter.sender.avatar} size={30} />
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 text-[12px]">
              <span className={unread ? 'font-semibold text-ink' : 'text-soft'}>{mine ? 'You' : letter.sender.displayName}</span>
              <span className="text-muted">{letterDate(letter.sentAt)}</span>
              {letter.corrected && (
                <span className="flex items-center gap-0.5 text-primary" title="Corrected">
                  <SpellCheck size={12} /> corrected
                </span>
              )}
              {unread && <span className="h-1.5 w-1.5 rounded-full bg-primary" />}
            </div>
            {!open && <p className="mt-0.5 truncate text-[12px] text-[#888]">{summary}</p>}
          </div>
          {open ? <ChevronUp size={16} className="text-muted" /> : <ChevronDown size={16} className="text-muted" />}
        </button>

        {open && (
          <div className="border-t border-line/70 px-4 pt-3 pb-4">
            {unread && <MarkRead id={letter.id} />}
            {(sealed || (mine && inFlight)) && (
              <div className="mb-3">
                <Suspense fallback={<Spinner />}>
                  <Tracker letter={letter} mine={mine} onArrive={() => setTimeout(refetch, 800)} />
                </Suspense>
              </div>
            )}
            {!sealed && <p className="text-[14px] leading-[1.9] whitespace-pre-line text-[#444]">{letter.body}</p>}
            <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-[11px] text-muted">
              <span className="flex items-center gap-1">
                <Navigation size={11} /> {countryName(letter.route.from.country)} → {countryName(letter.route.to.country)}
                {letter.route.distanceKm != null && ` · ${letter.route.distanceKm.toLocaleString()} km`}
              </span>
              {mine && letter.delivered && <span>{letter.readAt ? `✓ Read ${letterDate(letter.readAt)}` : 'Delivered · not read yet'}</span>}
            </div>

            {!mine && letter.delivered && !letter.corrected && (
              <div className="mt-3 flex justify-end">
                <Link to={`/letters/${letter.id}/reply`}>
                  <Button variant="grey">
                    <SpellCheck size={14} /> Correct (+1 stamp)
                  </Button>
                </Link>
              </div>
            )}
            {mine && letter.corrected && !showCorrection && (
              <button
                type="button"
                onClick={() => setShowCorrection(true)}
                className="mt-3 flex items-center gap-1 text-[12px] text-primary hover:underline"
              >
                <SpellCheck size={13} /> See {letter.recipient.displayName}'s corrections
              </button>
            )}
            {correction && (mine ? showCorrection : true) && (
              <div className="mt-3">
                <CorrectionView correction={correction} />
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

/** Opening a letter is what marks it read; fetching it through the letter endpoint does that. */
function MarkRead({ id }: { id: number }) {
  useLetter(id)
  return null
}
