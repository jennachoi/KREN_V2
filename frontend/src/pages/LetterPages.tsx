import { useState, type FormEvent } from 'react'
import { Link, NavLink, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { useLetter, useMailbox, useMe, useSendLetter, useUser } from '../api/hooks'
import { Avatar, Button, Card, ErrorBanner, PageTitle, Pager, Spinner, Textarea } from '../components/ui'
import { formatDate } from '../lib/locale'

const MAX_BODY = 5000

export function MailboxPage({ box }: { box: 'inbox' | 'sent' }) {
  const [page, setPage] = useState(0)
  const { data, isLoading, error } = useMailbox(box, page)
  const tab = ({ isActive }: { isActive: boolean }) =>
    `border-b-2 px-1 pb-2 text-sm font-medium ${isActive ? 'border-stamp text-ink' : 'border-transparent text-muted hover:text-ink'}`

  return (
    <>
      <PageTitle>Letters</PageTitle>
      <div className="mb-4 flex gap-6 border-b border-line">
        <NavLink to="/inbox" className={tab}>
          Inbox
        </NavLink>
        <NavLink to="/sent" className={tab}>
          Sent
        </NavLink>
      </div>
      <ErrorBanner error={error} />
      {isLoading ? (
        <Spinner />
      ) : data?.content.length === 0 ? (
        <Card className="text-center text-sm text-muted">
          {box === 'inbox' ? (
            <>
              No letters yet. <Link to="/penpals" className="text-sky hover:underline">Find a penpal</Link> and write first!
            </>
          ) : (
            'You haven’t sent any letters yet.'
          )}
        </Card>
      ) : (
        <Card className="divide-y divide-line p-0">
          {data?.content.map((l) => {
            const unread = box === 'inbox' && !l.readAt
            return (
              <Link key={l.id} to={`/letters/${l.id}`} className="flex items-start gap-3 px-5 py-4 hover:bg-paper">
                <Avatar name={l.counterpart.displayName} country={l.counterpart.country} />
                <div className="min-w-0 flex-1">
                  <div className="flex items-baseline gap-2">
                    <span className={unread ? 'font-semibold' : 'font-medium'}>
                      {box === 'sent' && <span className="font-normal text-muted">To </span>}
                      {l.counterpart.displayName}
                    </span>
                    {unread && <span className="h-2 w-2 rounded-full bg-stamp" aria-label="unread" />}
                    <span className="ml-auto shrink-0 text-xs text-muted">{formatDate(l.sentAt)}</span>
                  </div>
                  <p className={`truncate text-sm ${unread ? 'text-ink' : 'text-muted'}`}>{l.preview}</p>
                  {box === 'sent' && (
                    <span className="text-xs text-muted">{l.readAt ? `Read ${formatDate(l.readAt)}` : 'Not read yet'}</span>
                  )}
                </div>
              </Link>
            )
          })}
        </Card>
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />}
    </>
  )
}

function Composer({
  onSend,
  pending,
  error,
  label,
}: {
  onSend: (body: string) => void
  pending: boolean
  error: unknown
  label: string
}) {
  const { data: me } = useMe()
  const [body, setBody] = useState('')
  const outOfStamps = error instanceof ApiError && error.code === 'INSUFFICIENT_STAMPS'

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (body.trim()) onSend(body)
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <Textarea rows={10} value={body} maxLength={MAX_BODY} onChange={(e) => setBody(e.target.value)} placeholder="Dear…" />
      <div className="flex flex-wrap items-center gap-3">
        <Button type="submit" disabled={pending || !body.trim()}>
          {pending ? 'Sending…' : label}
        </Button>
        <span className="text-xs text-muted">
          Costs 1 stamp · you have {me?.stampBalance ?? '…'} · {body.length}/{MAX_BODY}
        </span>
      </div>
      {outOfStamps ? (
        <div className="rounded-lg border border-stamp/30 bg-stamp/5 px-3 py-2 text-sm text-stamp">
          You're out of stamps. Use “Daily check-in” at the top to earn one.
        </div>
      ) : (
        <ErrorBanner error={error} />
      )}
    </form>
  )
}

export function ComposePage() {
  const id = Number(useParams().userId)
  const navigate = useNavigate()
  const { data: to, isLoading } = useUser(id)
  const send = useSendLetter()

  if (isLoading) return <Spinner />
  if (!to) return <Card className="text-center text-sm text-muted">This person isn't available.</Card>

  return (
    <Card className="mx-auto max-w-2xl">
      <div className="mb-4 flex items-center gap-3">
        <Avatar name={to.displayName} country={to.country} />
        <div>
          <div className="text-xs text-muted">To</div>
          <div className="font-medium">{to.displayName}</div>
        </div>
      </div>
      <Composer
        label="Send letter"
        pending={send.isPending}
        error={send.error}
        onSend={(body) => send.mutate({ recipientId: id, body }, { onSuccess: (l) => navigate(`/letters/${l.id}`) })}
      />
    </Card>
  )
}

/** Keyed by id so moving to another letter (e.g. right after replying) starts with fresh state. */
export function LetterPage() {
  const id = useParams().id
  return <LetterView key={id} id={Number(id)} />
}

function LetterView({ id }: { id: number }) {
  const navigate = useNavigate()
  const { data: me } = useMe()
  const { data: letter, isLoading, error } = useLetter(id)
  const reply = useSendLetter()
  const [replying, setReplying] = useState(false)

  if (isLoading) return <Spinner />
  if (error instanceof ApiError && (error.status === 403 || error.status === 404)) {
    return <Card className="text-center text-sm text-muted">This letter isn't available.</Card>
  }
  if (!letter || !me) return <ErrorBanner error={error} />

  const mine = letter.sender.id === me.id
  const other = mine ? letter.recipient : letter.sender

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <Link to={mine ? '/sent' : '/inbox'} className="text-sm text-muted hover:text-ink">
        ← Back to {mine ? 'sent' : 'inbox'}
      </Link>
      <Card className="relative">
        <div className="absolute top-5 right-5 flex h-14 w-12 rotate-3 items-center justify-center rounded-sm border-2 border-dashed border-stamp/50 text-xl text-stamp">
          ✉
        </div>
        <div className="flex items-center gap-3 pr-16">
          <Avatar name={letter.sender.displayName} country={letter.sender.country} />
          <div className="text-sm">
            <div>
              <b>{letter.sender.displayName}</b> <span className="text-muted">to</span> <b>{letter.recipient.displayName}</b>
            </div>
            <div className="text-muted">{formatDate(letter.sentAt)}</div>
          </div>
        </div>
        {letter.parentId && (
          <Link to={`/letters/${letter.parentId}`} className="mt-3 inline-block text-xs text-sky hover:underline">
            In reply to an earlier letter
          </Link>
        )}
        <p className="mt-5 font-serif text-lg leading-relaxed whitespace-pre-line">{letter.body}</p>
        {mine && (
          <div className="mt-5 text-xs text-muted">
            {letter.readAt ? `✓ Read ${formatDate(letter.readAt)}` : 'Not read yet'}
          </div>
        )}
      </Card>

      <Card>
        {replying ? (
          <Composer
            label={`Reply to ${other.displayName}`}
            pending={reply.isPending}
            error={reply.error}
            onSend={(body) => reply.mutate({ replyTo: letter.id, body }, { onSuccess: (l) => navigate(`/letters/${l.id}`) })}
          />
        ) : (
          <Button onClick={() => setReplying(true)}>Reply to {other.displayName}</Button>
        )}
      </Card>
    </div>
  )
}
