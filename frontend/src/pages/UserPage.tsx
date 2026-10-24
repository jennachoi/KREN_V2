import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { useBlock, useUser } from '../api/hooks'
import { Avatar, Button, Card, ErrorBanner, InterestChips, LangPair, Spinner } from '../components/ui'
import { countryName } from '../lib/locale'

export function UserPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const { data: user, isLoading, error } = useUser(id)
  const block = useBlock()
  const [confirmBlock, setConfirmBlock] = useState(false)

  if (isLoading) return <Spinner />
  if (error instanceof ApiError && error.status === 404) {
    return <Card className="text-center text-sm text-muted">This profile isn't available.</Card>
  }
  if (!user) return <ErrorBanner error={error} />

  return (
    <Card className="mx-auto max-w-xl">
      <div className="flex items-start gap-4">
        <Avatar name={user.displayName} country={user.country} />
        <div>
          <h1 className="font-serif text-2xl font-semibold">{user.displayName}</h1>
          <div className="text-sm text-muted">{countryName(user.country)}</div>
          <LangPair native={user.nativeLang} learning={user.learningLang} />
        </div>
      </div>
      {user.bio && <p className="mt-5 whitespace-pre-line text-ink/90">{user.bio}</p>}
      {user.interests.length > 0 && (
        <div className="mt-5">
          <InterestChips all={user.interests} selected={[]} />
        </div>
      )}
      <div className="mt-6 flex flex-wrap items-center gap-2 border-t border-line pt-5">
        <Link to={`/compose/${user.id}`}>
          <Button>Write a letter ✉</Button>
        </Link>
        <div className="ml-auto">
          {confirmBlock ? (
            <span className="flex items-center gap-2 text-sm">
              <span className="text-muted">Hide each other and stop new letters?</span>
              <Button
                variant="danger"
                disabled={block.isPending}
                onClick={() => block.mutate({ userId: user.id, block: true }, { onSuccess: () => navigate('/penpals') })}
              >
                Block
              </Button>
              <Button variant="ghost" onClick={() => setConfirmBlock(false)}>
                Cancel
              </Button>
            </span>
          ) : (
            <Button variant="ghost" onClick={() => setConfirmBlock(true)}>
              Block
            </Button>
          )}
        </div>
      </div>
      <div className="mt-3">
        <ErrorBanner error={block.error} />
      </div>
    </Card>
  )
}
