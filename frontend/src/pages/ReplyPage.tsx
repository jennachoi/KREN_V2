import { useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router'
import { Send, SpellCheck } from 'lucide-react'
import { ApiError } from '../api/client'
import { useCorrection, useLetter, useMe, useSendLetter } from '../api/hooks'
import { CorrectionEditor, CorrectionView } from '../components/Corrections'
import { Postcard } from '../components/Postcard'
import { Avatar, Button, Card, ErrorText, Spinner } from '../components/ui'
import { MAX_BODY } from '../lib/domain'
import { letterDate } from '../lib/locale'

/**
 * /letters/:id/reply — answering a letter in one place: their letter on the left, where you can
 * correct it sentence by sentence (+1 stamp), and your reply on the right (−1 stamp). On phones the
 * two halves are tabs.
 */
export function ReplyPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const { data: me } = useMe()
  const { data: letter, isLoading, error } = useLetter(id)
  const { data: correction } = useCorrection(id, !!letter?.corrected)
  const reply = useSendLetter()
  const [body, setBody] = useState('')
  const [correcting, setCorrecting] = useState(true)
  const [tab, setTab] = useState<'letter' | 'reply'>('letter')

  if (isLoading || !me) return <Spinner />
  if (error instanceof ApiError && (error.status === 403 || error.status === 404)) {
    return <Card className="px-6 py-12 text-center text-[13px] text-muted">This letter isn't available.</Card>
  }
  if (!letter) return <ErrorText error={error} />
  const from = letter.sender
  // Only a letter that reached you can be answered here; anything else goes back to the conversation.
  if (from.id === me.id || letter.body == null) {
    return <Navigate to={`/letters/with/${from.id === me.id ? letter.recipient.id : from.id}?letter=${id}`} replace />
  }

  const outOfStamps = reply.error instanceof ApiError && reply.error.code === 'INSUFFICIENT_STAMPS'
  const tabClass = (t: typeof tab) =>
    `flex-1 rounded-full py-1.5 text-[13px] ${tab === t ? 'bg-primary text-white' : 'border border-line bg-white text-soft'}`

  return (
    <div className="space-y-4">
      <Link to={`/letters/with/${from.id}?letter=${id}`} className="text-[13px] text-muted hover:text-ink">
        ← Back to {from.displayName}
      </Link>
      <div className="flex gap-2 lg:hidden">
        <button type="button" className={tabClass('letter')} onClick={() => setTab('letter')}>
          {from.displayName}'s letter {letter.corrected ? '✓' : '· correct'}
        </button>
        <button type="button" className={tabClass('reply')} onClick={() => setTab('reply')}>
          Your reply
        </button>
      </div>

      <div className="grid items-start gap-6 lg:grid-cols-2">
        <section className={`space-y-4 ${tab === 'letter' ? '' : 'max-lg:hidden'}`}>
          <Card className="px-6 py-5">
            <div className="flex items-center gap-3 border-b border-line pb-3">
              <Avatar name={from.displayName} country={from.country} avatar={from.avatar} size={38} />
              <div>
                <p className="text-[14px] text-ink">{from.displayName}</p>
                <p className="text-[11px] text-muted">{letterDate(letter.sentAt)}</p>
              </div>
            </div>
            <p className="mt-3 text-[14px] leading-[1.9] whitespace-pre-line text-[#444]">{letter.body}</p>
          </Card>

          {letter.corrected ? (
            correction && <CorrectionView correction={correction} />
          ) : correcting ? (
            <CorrectionEditor stacked letter={letter} onDone={() => setCorrecting(false)} onCancel={() => setCorrecting(false)} />
          ) : (
            <Button variant="grey" onClick={() => setCorrecting(true)}>
              <SpellCheck size={14} /> Correct this letter (+1 stamp)
            </Button>
          )}
          <Button variant="white" className="lg:hidden" onClick={() => setTab('reply')}>
            Write your reply →
          </Button>
        </section>

        <section className={`lg:sticky lg:top-4 ${tab === 'reply' ? '' : 'max-lg:hidden'}`}>
          <Postcard>
            <h1 className="font-display text-[clamp(15px,1.8vw,20px)] font-semibold text-ink">TO.{from.displayName}</h1>
            <textarea
              maxLength={MAX_BODY}
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="Dear…"
              className="mt-2 w-full flex-1 resize-none bg-[repeating-linear-gradient(transparent,transparent_31px,#ececec_32px)] text-[14px] leading-8 text-[#333] outline-none placeholder:text-[#bbb]"
            />
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <span className="font-display text-[clamp(13px,1.5vw,17px)] font-semibold text-ink">FROM.{me.displayName}</span>
              <span className="ml-auto text-[12px] text-muted">
                {body.length} / {MAX_BODY}
              </span>
              <Button
                variant="deep"
                disabled={reply.isPending || !body.trim()}
                onClick={() =>
                  reply.mutate({ replyTo: id, body }, { onSuccess: (l) => navigate(`/letters/with/${from.id}?letter=${l.id}`) })
                }
              >
                <Send size={14} /> Send reply
              </Button>
            </div>
          </Postcard>
          <div className="mt-3 text-center text-[12px]">
            {outOfStamps ? (
              <p className="text-coral">You're out of stamps. Correct the letter on the left to earn one.</p>
            ) : reply.error ? (
              <ErrorText error={reply.error} />
            ) : (
              <p className="text-muted">
                Sending costs 1 stamp · you have {me.stampBalance}
                {!letter.corrected && ' · correcting earns 1'}
              </p>
            )}
          </div>
        </section>
      </div>
    </div>
  )
}
