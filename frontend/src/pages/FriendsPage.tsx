import { useState } from 'react'
import { Link } from 'react-router'
import { MapPin, SlidersHorizontal, X } from 'lucide-react'
import { useInterests, useMe, usePenpals } from '../api/hooks'
import type { MyProfile, PublicProfile } from '../api/types'
import { Avatar, Button, Card, Chips, ErrorText, Pager, Spinner } from '../components/ui'
import { matchedCount } from '../lib/domain'
import { countryName, languageName } from '../lib/locale'

/** v1 friend search: filter toggle + tabs on top, conditions panel on the left, profile cards grid. */
export function FriendsPage() {
  const { data: me } = useMe()
  const { data: interests = [] } = useInterests()
  const [showFilter, setShowFilter] = useState(false)
  const [draft, setDraft] = useState<number[]>([])
  const [applied, setApplied] = useState<number[]>([])
  const [page, setPage] = useState(0)
  const { data, isLoading, error } = usePenpals(applied, page)

  const toggle = (id: number) => setDraft((d) => (d.includes(id) ? d.filter((x) => x !== id) : [...d, id].sort((a, b) => a - b)))
  const tab = (active: boolean) =>
    `border-b-2 px-4 pb-2.5 text-[14px] ${active ? 'border-ink text-ink' : 'border-transparent text-[#bbb] hover:text-soft'}`

  return (
    <div>
      <div className="mb-5 flex items-end border-b border-line">
        <button className="mr-3 pb-2.5 text-soft hover:text-ink" title="Search conditions" onClick={() => setShowFilter((s) => !s)}>
          {showFilter ? <X size={18} /> : <SlidersHorizontal size={18} />}
        </button>
        <button className={tab(showFilter)} onClick={() => setShowFilter(true)}>
          Search
        </button>
        <button
          className={tab(!showFilter && applied.length === 0)}
          onClick={() => {
            setShowFilter(false)
            setDraft([])
            setApplied([])
            setPage(0)
          }}
        >
          All
        </button>
        <span className="mb-2.5 ml-auto border-r border-line pr-4 text-[13px] text-soft">
          {data ? `${data.totalElements} friends` : ''}
        </span>
      </div>

      {me && (
        <p className="mb-4 text-[13px] text-soft">
          People who speak <b className="text-ink">{languageName(me.learningLang)}</b> and are learning{' '}
          <b className="text-ink">{languageName(me.nativeLang)}</b>.
        </p>
      )}

      <div className={`grid gap-5 ${showFilter ? 'md:grid-cols-[280px_1fr]' : ''}`}>
        {showFilter && (
          <Card className="h-fit p-5">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="font-display text-[16px] font-semibold">Search Conditions</h3>
              <Button
                className="rounded-full px-4 py-1.5"
                onClick={() => {
                  setApplied(draft)
                  setPage(0)
                }}
              >
                Search
              </Button>
            </div>
            <p className="mb-2 text-[13px] text-ink">Topics of Interest</p>
            <Chips all={interests} selected={draft} onToggle={toggle} />
            <p className="mt-3 text-[12px] text-muted">Shows people who share at least one of these.</p>
          </Card>
        )}

        <div>
          <ErrorText error={error} />
          {isLoading ? (
            <Spinner />
          ) : data && data.content.length === 0 ? (
            <Card className="px-6 py-14 text-center">
              <img src="/img/globe-people.svg" alt="" className="mx-auto w-40" />
              <p className="mt-4 text-[13px] text-muted">No friends match yet. Try fewer conditions.</p>
            </Card>
          ) : (
            <div className={`grid gap-4 sm:grid-cols-2 ${showFilter ? 'lg:grid-cols-3' : 'md:grid-cols-3 lg:grid-cols-4'}`}>
              {me && data?.content.map((p) => <FriendCard key={p.id} me={me} p={p} />)}
            </div>
          )}
          {data && <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />}
        </div>
      </div>
    </div>
  )
}

function FriendCard({ me, p }: { me: MyProfile; p: PublicProfile }) {
  return (
    <Link to={`/users/${p.id}`} className="block">
      <Card className="px-4 pt-6 pb-4 text-center transition-shadow hover:shadow-md">
        <div className="flex justify-center">
          <Avatar name={p.displayName} country={p.country} avatar={p.avatar} size={72} />
        </div>
        <div className="mt-2 font-display text-[16px] font-medium text-ink">{p.displayName}</div>
        <div className="mt-0.5 flex items-center justify-center gap-1 text-[12px] text-muted">
          <MapPin size={12} /> {countryName(p.country)}
        </div>
        <div className="mt-2 text-[12px] text-soft">
          Matched interests <b className="text-ink">{matchedCount(me, p)}</b>
        </div>
        <div className="mt-3 grid grid-cols-3 divide-x divide-line">
          <Stat label="Native" value={p.nativeLang.toUpperCase()} />
          <Stat label="Learning" value={p.learningLang.toUpperCase()} />
          <Stat label="Topics" value={String(p.interests.length)} />
        </div>
      </Card>
    </Link>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="font-display text-[9px] tracking-wider text-muted uppercase">{label}</div>
      <div className="font-display text-[15px] text-ink">{value}</div>
    </div>
  )
}
