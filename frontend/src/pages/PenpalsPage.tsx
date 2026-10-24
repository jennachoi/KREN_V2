import { useState } from 'react'
import { Link } from 'react-router'
import { useInterests, useMe, usePenpals } from '../api/hooks'
import { Avatar, Card, ErrorBanner, InterestChips, LangPair, PageTitle, Pager, Spinner } from '../components/ui'
import { languageName } from '../lib/locale'

export function PenpalsPage() {
  const { data: me } = useMe()
  const { data: interests = [] } = useInterests()
  const [selected, setSelected] = useState<number[]>([])
  const [page, setPage] = useState(0)
  const { data, isLoading, error } = usePenpals(selected, page)

  const toggle = (id: number) => {
    setPage(0)
    setSelected((s) => (s.includes(id) ? s.filter((x) => x !== id) : [...s, id].sort((a, b) => a - b)))
  }

  return (
    <>
      <PageTitle
        sub={
          me && (
            <>
              Native {languageName(me.learningLang)} speakers who are learning {languageName(me.nativeLang)}.
            </>
          )
        }
      >
        Find penpals
      </PageTitle>

      <div className="mb-6">
        <div className="mb-2 text-xs font-medium tracking-wide text-muted uppercase">Shares any of these interests</div>
        <InterestChips all={interests} selected={selected} onToggle={toggle} />
      </div>

      <ErrorBanner error={error} />
      {isLoading ? (
        <Spinner />
      ) : data && data.content.length === 0 ? (
        <Card className="text-center text-sm text-muted">
          No matches yet. Try removing interest filters, or check back as more people join.
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {data?.content.map((p) => (
            <Link key={p.id} to={`/users/${p.id}`}>
              <Card className="h-full transition-shadow hover:shadow-md">
                <div className="flex items-start gap-3">
                  <Avatar name={p.displayName} country={p.country} />
                  <div className="min-w-0">
                    <div className="font-medium">{p.displayName}</div>
                    <LangPair native={p.nativeLang} learning={p.learningLang} />
                  </div>
                </div>
                {p.bio && <p className="mt-3 line-clamp-2 text-sm text-ink/80">{p.bio}</p>}
                {p.interests.length > 0 && (
                  <div className="mt-3">
                    <InterestChips all={p.interests} selected={p.interests.filter((i) => selected.includes(i.id)).map((i) => i.id)} />
                  </div>
                )}
              </Card>
            </Link>
          ))}
        </div>
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />}
    </>
  )
}
