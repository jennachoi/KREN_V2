import { useState } from 'react'
import { Link } from 'react-router'
import { useNotebook } from '../api/hooks'
import { CorrectionDiff } from '../components/Corrections'
import { Avatar, Card, CardHead, ErrorText, Pager, Spinner } from '../components/ui'
import { letterDate } from '../lib/locale'

/** Every correction native speakers made to my letters: a personal record of what to fix next time. */
export function NotebookPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, error } = useNotebook(page)

  return (
    <div className="mx-auto max-w-[760px]">
      <div className="mb-5">
        <h1 className="font-display text-[20px] font-semibold text-ink">My Notebook</h1>
        <p className="mt-1 text-[13px] text-muted">Sentences your penpals corrected for you — what a native speaker would write.</p>
      </div>
      <ErrorText error={error} />
      {isLoading ? (
        <Spinner />
      ) : data?.content.length === 0 ? (
        <Card className="px-6 py-14 text-center">
          <img src="/img/globe-people.svg" alt="" className="mx-auto w-40" />
          <p className="mt-4 text-[14px] text-ink">No corrections yet.</p>
          <p className="mt-1 text-[13px] text-muted">Write a letter in the language you're learning — your penpal can correct it.</p>
          <Link to="/friends" className="mt-4 inline-block text-[13px] text-primary hover:underline">
            Find friends →
          </Link>
        </Card>
      ) : (
        <div className="space-y-4">
          {data?.content.map((c) => (
            <Card key={c.id}>
              <CardHead right={<Link to={`/letters/${c.letterId}`} className="text-[12px] text-primary hover:underline">Open letter</Link>}>
                <span className="flex items-center gap-2">
                  <Avatar name={c.corrector.displayName} country={c.corrector.country} avatar={c.corrector.avatar} size={26} />
                  {c.corrector.displayName}
                  <span className="text-[12px] font-normal text-muted">· {letterDate(c.createdAt)}</span>
                </span>
              </CardHead>
              <div className="divide-y divide-line px-5">
                {c.lines.map((l, i) => (
                  <CorrectionDiff key={i} {...l} />
                ))}
              </div>
              {c.comment && <p className="mx-5 mb-4 rounded-[4px] bg-canvas px-3 py-2 text-[13px]">“{c.comment}”</p>}
            </Card>
          ))}
        </div>
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />}
    </div>
  )
}
