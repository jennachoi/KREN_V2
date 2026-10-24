import { useState } from 'react'
import { Link, NavLink, useNavigate, useParams } from 'react-router'
import { CornerUpLeft, CornerUpRight, Send } from 'lucide-react'
import { ApiError } from '../api/client'
import { useLetter, useMailbox, useMe, useSendLetter } from '../api/hooks'
import type { LetterSummary } from '../api/types'
import { Avatar, Button, Card, ErrorText, Pager, Spinner, StampIcon } from '../components/ui'
import { MAX_BODY } from '../lib/domain'
import { letterDate } from '../lib/locale'

/**
 * v1 letter box: letter cards on the left (sender, reply arrow, stamp, date, preview), the open
 * letter and the reply composer on the right. /letters, /letters/sent, /letters/:id.
 */
export function LettersPage({ box }: { box: 'inbox' | 'sent' }) {
  const params = useParams()
  const selectedId = params.id ? Number(params.id) : null
  const [page, setPage] = useState(0)
  const { data, isLoading, error } = useMailbox(box, page)

  const tab = ({ isActive }: { isActive: boolean }) =>
    `rounded-full px-4 py-1.5 text-[13px] ${isActive ? 'bg-primary text-white' : 'bg-white text-soft border border-line hover:text-ink'}`

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
      <div className={selectedId ? 'max-lg:hidden' : ''}>
        <div className="mb-4 flex gap-2">
          <NavLink to="/letters" className={() => tab({ isActive: box === 'inbox' })}>
            Inbox
          </NavLink>
          <NavLink to="/letters/sent" className={tab}>
            Sent
          </NavLink>
        </div>
        <ErrorText error={error} />
        {isLoading ? (
          <Spinner />
        ) : data?.content.length === 0 ? (
          <Card className="px-6 py-12 text-center">
            <img src="/img/globe-people.svg" alt="" className="mx-auto w-36" />
            <p className="mt-4 text-[13px] text-muted">
              {box === 'inbox' ? 'No letters yet.' : "You haven't sent any letters yet."}
            </p>
            <Link to="/friends" className="mt-3 inline-block text-[13px] text-primary hover:underline">
              Find friends →
            </Link>
          </Card>
        ) : (
          <div className="space-y-3">
            {data?.content.map((l) => (
              <LetterCard key={l.id} l={l} box={box} selected={l.id === selectedId} />
            ))}
          </div>
        )}
        {data && <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />}
      </div>

      <div className="lg:border-l lg:border-line lg:pl-6">
        {selectedId ? (
          <LetterDetail key={selectedId} id={selectedId} />
        ) : (
          <div className="hidden h-full min-h-[300px] items-center justify-center text-[13px] text-muted lg:flex">
            Select a letter to read it.
          </div>
        )}
      </div>
    </div>
  )
}

function LetterCard({ l, box, selected }: { l: LetterSummary; box: 'inbox' | 'sent'; selected: boolean }) {
  const unread = box === 'inbox' && !l.readAt
  return (
    <Link to={`/letters/${l.id}`} className="block">
      <div
        className={`rounded-[6px] border bg-white px-5 py-4 transition-shadow ${
          selected ? 'border-line shadow-[0_4px_14px_rgba(0,0,0,0.09)]' : 'border-line hover:shadow-md'
        }`}
      >
        <div className="flex items-start gap-3">
          <Avatar name={l.counterpart.displayName} country={l.counterpart.country} size={40} />
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-1">
              <span className={`text-[13px] ${selected ? 'text-primary' : 'text-soft'} ${unread ? 'font-semibold text-ink' : ''}`}>
                {box === 'sent' ? `${l.counterpart.displayName}` : l.counterpart.displayName}
              </span>
              {box === 'inbox' ? (
                <CornerUpLeft size={15} strokeWidth={2.4} className="text-coral" />
              ) : (
                <CornerUpRight size={15} strokeWidth={2.4} className="text-primary" />
              )}
              {unread && <span className="ml-1 h-1.5 w-1.5 rounded-full bg-primary" />}
            </div>
          </div>
          <StampIcon className="h-11 w-11 shrink-0" />
        </div>
        <div className="mt-1 text-right text-[12px] text-soft">{letterDate(l.sentAt)}</div>
        <p className="mt-1.5 line-clamp-2 text-[12px] leading-relaxed text-[#888]">{l.preview}</p>
        {box === 'sent' && <p className="mt-1 text-[11px] text-muted">{l.readAt ? `✓ Read ${letterDate(l.readAt)}` : 'Not read yet'}</p>}
      </div>
    </Link>
  )
}

function LetterDetail({ id }: { id: number }) {
  const navigate = useNavigate()
  const { data: me } = useMe()
  const { data: letter, isLoading, error } = useLetter(id)
  const reply = useSendLetter()
  const [body, setBody] = useState('')

  if (isLoading) return <Spinner />
  if (error instanceof ApiError && (error.status === 403 || error.status === 404)) {
    return <Card className="px-6 py-12 text-center text-[13px] text-muted">This letter isn't available.</Card>
  }
  if (!letter || !me) return <ErrorText error={error} />

  const mine = letter.sender.id === me.id
  const other = mine ? letter.recipient : letter.sender
  const outOfStamps = reply.error instanceof ApiError && reply.error.code === 'INSUFFICIENT_STAMPS'

  return (
    <div className="space-y-6">
      <Link to={mine ? '/letters/sent' : '/letters'} className="text-[13px] text-muted hover:text-ink lg:hidden">
        ← Back
      </Link>
      <Card className="px-6 py-5">
        <div className="flex items-center gap-3 border-b border-line pb-4">
          <Link to={`/users/${letter.sender.id === me.id ? me.id : letter.sender.id}`}>
            <Avatar name={letter.sender.displayName} country={letter.sender.country} size={42} />
          </Link>
          <div>
            <div className="text-[14px] text-ink">
              {letter.sender.displayName}
              <span className="text-muted"> → {letter.recipient.displayName}</span>
            </div>
            <div className="text-[12px] text-muted">{letterDate(letter.sentAt)}</div>
          </div>
        </div>
        {letter.parentId && (
          <Link to={`/letters/${letter.parentId}`} className="mt-3 inline-block text-[12px] text-primary hover:underline">
            ↩ In reply to an earlier letter
          </Link>
        )}
        <p className="mt-4 text-[14px] leading-[1.9] whitespace-pre-line text-[#444]">{letter.body}</p>
        {mine && (
          <p className="mt-5 text-right text-[12px] text-muted">{letter.readAt ? `✓ Read ${letterDate(letter.readAt)}` : 'Not read yet'}</p>
        )}
      </Card>

      <div>
        <h3 className="mb-2 text-[15px] text-ink">
          Reply to {other.displayName} ({body.length} / {MAX_BODY})
        </h3>
        <Card className="p-5">
          <textarea
            rows={9}
            maxLength={MAX_BODY}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Write your letter."
            className="w-full resize-none text-[14px] leading-relaxed outline-none placeholder:text-[#bbb]"
          />
          <div className="mt-3 flex flex-wrap items-center gap-3">
            <Button
              disabled={reply.isPending || !body.trim()}
              onClick={() => reply.mutate({ replyTo: letter.id, body }, { onSuccess: (l) => navigate(`/letters/${l.id}`) })}
            >
              <Send size={14} /> Send letter
            </Button>
            <span className="text-[12px] text-muted">Costs 1 stamp · you have {me.stampBalance}</span>
          </div>
          <div className="mt-2">
            {outOfStamps ? (
              <p className="text-[12px] text-coral">You're out of stamps. Use the Daily Event on the home page to get one.</p>
            ) : (
              <ErrorText error={reply.error} />
            )}
          </div>
        </Card>
      </div>
    </div>
  )
}
