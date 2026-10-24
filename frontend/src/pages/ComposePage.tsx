import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { Send } from 'lucide-react'
import { ApiError } from '../api/client'
import { useMe, useSendLetter, useUser } from '../api/hooks'
import { Button, Card, ErrorText, Spinner } from '../components/ui'
import { MAX_BODY } from '../lib/domain'

/** v1 "write a letter": a postcard (POST CARD heading, stamp in the corner), TO. / FROM., counter. */
export function ComposePage() {
  const id = Number(useParams().userId)
  const navigate = useNavigate()
  const { data: me } = useMe()
  const { data: to, isLoading } = useUser(id)
  const send = useSendLetter()
  const [body, setBody] = useState('')
  const outOfStamps = send.error instanceof ApiError && send.error.code === 'INSUFFICIENT_STAMPS'

  if (isLoading || !me) return <Spinner />
  if (!to) return <Card className="px-6 py-12 text-center text-[13px] text-muted">This person isn't available.</Card>

  return (
    <div className="mx-auto max-w-[880px]">
      <div
        className="relative bg-[length:100%_100%] bg-no-repeat px-[7%] pt-[12%] pb-[6%]"
        style={{ backgroundImage: 'url(/img/postcard.png)', aspectRatio: '863 / 587' }}
      >
        <div className="flex h-full flex-col">
          <h1 className="font-display text-[clamp(16px,2.2vw,22px)] font-semibold text-ink">TO.{to.displayName}</h1>
          <textarea
            autoFocus
            maxLength={MAX_BODY}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Dear…"
            className="mt-3 w-full flex-1 resize-none bg-[repeating-linear-gradient(transparent,transparent_31px,#ececec_32px)] text-[14px] leading-8 text-[#333] outline-none placeholder:text-[#bbb]"
          />
          <div className="mt-3 flex flex-wrap items-center gap-3">
            <span className="font-display text-[clamp(14px,1.9vw,19px)] font-semibold text-ink">FROM.{me.displayName}</span>
            <span className="ml-auto font-display text-[clamp(13px,1.7vw,17px)] font-semibold text-ink">
              ({body.length} / {MAX_BODY})
            </span>
            <Button variant="white" onClick={() => navigate(-1)}>
              Cancel
            </Button>
            <Button
              variant="deep"
              disabled={send.isPending || !body.trim()}
              onClick={() => send.mutate({ recipientId: id, body }, { onSuccess: (l) => navigate(`/letters/${l.id}`) })}
            >
              <Send size={14} /> Send letter
            </Button>
          </div>
        </div>
      </div>
      <div className="mt-3 text-center text-[12px]">
        {outOfStamps ? (
          <p className="text-coral">You're out of stamps. Use the Daily Event on the home page to get one.</p>
        ) : send.error ? (
          <ErrorText error={send.error} />
        ) : (
          <p className="text-muted">Sending costs 1 stamp · you have {me.stampBalance}</p>
        )}
      </div>
    </div>
  )
}
