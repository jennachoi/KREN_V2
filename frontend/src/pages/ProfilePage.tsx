import { useState, type ReactNode } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router'
import { ArrowLeftRight, MapPin } from 'lucide-react'
import { ApiError } from '../api/client'
import { useAvatars, useBlock, useBlocks, useCheckIn, useInterests, useLedger, useMe, useUpdateProfile, useUser } from '../api/hooks'
import type { MyProfile, PublicProfile } from '../api/types'
import { Avatar, Button, Card, CardHead, Chips, ErrorText, Spinner } from '../components/ui'
import { useAvatarUri } from '../lib/avatar'
import { COUNTRIES, LANGUAGES, countryName, dayDate, languageName } from '../lib/locale'

/** /users/:id — someone else's profile (or yours, if the id is you). */
export function UserProfilePage() {
  const id = Number(useParams().id)
  const { data: me } = useMe()
  const { data: user, isLoading, error } = useUser(id)
  if (me && me.id === id) return <Navigate to="/profile" replace />
  if (isLoading || !me) return <Spinner />
  if (error instanceof ApiError && error.status === 404) {
    return (
      <Card className="mx-auto max-w-[900px] px-6 py-16 text-center">
        <img src="/img/globe-people.svg" alt="" className="mx-auto w-40" />
        <p className="mt-4 text-[14px] text-muted">This profile isn't available.</p>
      </Card>
    )
  }
  if (!user) return <ErrorText error={error} />
  return <OtherProfile me={me} user={user} />
}

/** /profile and /profile/edit — your own profile, view or edit mode (v1 used the same frame for both). */
export function MyProfilePage({ edit = false }: { edit?: boolean }) {
  const { data: me } = useMe()
  if (!me) return <Spinner />
  return edit ? <EditProfile me={me} /> : <OwnProfile me={me} />
}

function ProfileHeader({
  person,
  stampBalance,
  actions,
  stats,
}: {
  person: { displayName: string; country: string; nativeLang: string; learningLang: string; avatar: string | null }
  stampBalance?: number
  actions: ReactNode
  stats: ReactNode
}) {
  return (
    <Card className="overflow-hidden">
      {/* v1 had a map of the user's city here; a soft banner with their country stands in for it. */}
      <div className="relative h-[120px] bg-[linear-gradient(120deg,#a8cdf5_0%,#cfe2f8_45%,#e6d9b8_100%)]">
        <svg className="absolute inset-0 h-full w-full opacity-30" aria-hidden>
          <defs>
            <pattern id="dots" width="16" height="16" patternUnits="userSpaceOnUse">
              <circle cx="2" cy="2" r="1.4" fill="#3d70b2" />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#dots)" />
        </svg>
        <span className="absolute right-4 bottom-3 flex items-center gap-1 rounded-[4px] bg-[#555]/70 px-2.5 py-1 text-[12px] text-white">
          <MapPin size={12} /> {countryName(person.country)}
        </span>
      </div>
      <div className="relative grid grid-cols-1 items-end gap-3 px-5 pt-14 pb-5 md:grid-cols-3 md:pt-4">
        <div className="order-2 space-y-2 md:order-1">
          {stampBalance !== undefined && (
            <div className="flex items-center gap-2 text-[12px] text-soft">
              <img src="/img/stamp-box.png" alt="" className="h-6 w-6 opacity-70" />
              My Stamp
              <span className="rounded-full bg-chip px-2 py-0.5 text-[12px] text-ink">{stampBalance}</span>
            </div>
          )}
          <div className="flex gap-4 font-display text-[11px] tracking-wider text-muted uppercase">{stats}</div>
        </div>
        <div className="order-1 -mt-24 text-center md:order-2 md:-mt-20">
          <div className="flex justify-center">
            <Avatar name={person.displayName} country={person.country} avatar={person.avatar} size={96} />
          </div>
          <h1 className="mt-2 font-display text-[22px] text-ink">{person.displayName}</h1>
        </div>
        <div className="order-3 flex items-center justify-center gap-3 md:justify-end">
          <span className="flex items-center gap-1.5 font-display text-[14px] text-ink" title="Native ⇆ Learning">
            {person.nativeLang.toUpperCase()} <ArrowLeftRight size={13} className="text-muted" /> {person.learningLang.toUpperCase()}
          </span>
          {actions}
        </div>
      </div>
    </Card>
  )
}

const Stat = ({ label, value }: { label: string; value: ReactNode }) => (
  <span>
    {label} <b className="ml-0.5 text-[15px] font-medium text-ink">{value}</b>
  </span>
)

function InfoRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-center justify-between border-b border-line px-5 py-3 text-[13px] last:border-0">
      <span className="text-soft">{label}</span>
      <span className="text-ink">{value}</span>
    </div>
  )
}

function BasicInfos({ p }: { p: { createdAt: string; country: string; nativeLang: string; learningLang: string; bio: string | null } }) {
  return (
    <Card>
      <CardHead>Basic Infos</CardHead>
      <InfoRow label="Joining Date" value={dayDate(p.createdAt)} />
      <InfoRow label="Country" value={countryName(p.country)} />
      <InfoRow label="Native" value={languageName(p.nativeLang)} />
      <InfoRow label="Learning" value={languageName(p.learningLang)} />
      <div className="px-5 py-4">
        <h4 className="mb-1.5 text-[14px] font-medium text-ink">About Me</h4>
        <p className="text-[13px] leading-relaxed whitespace-pre-line text-[#555]">{p.bio || 'No introduction yet.'}</p>
      </div>
    </Card>
  )
}

function OtherProfile({ me, user }: { me: MyProfile; user: PublicProfile }) {
  const navigate = useNavigate()
  const block = useBlock()
  const [confirm, setConfirm] = useState(false)
  const mine = me.interests.map((i) => i.id)
  const matched = user.interests.filter((i) => mine.includes(i.id)).map((i) => i.id)

  return (
    <div className="mx-auto max-w-[900px] space-y-5">
      <ProfileHeader
        person={user}
        stats={
          <>
            <Stat label="Topics" value={user.interests.length} />
            <Stat label="Matched" value={matched.length} />
          </>
        }
        actions={
          <>
            <Link to={`/compose/${user.id}`}>
              <Button variant="deep">✍ Write a letter</Button>
            </Link>
            <Button variant="white" onClick={() => setConfirm(true)}>
              Block
            </Button>
          </>
        }
      />
      {confirm && (
        <Card className="flex flex-wrap items-center gap-3 px-5 py-3 text-[13px]">
          <span className="text-soft">Block {user.displayName}? You'll stop seeing each other and can't exchange new letters.</span>
          <span className="ml-auto flex gap-2">
            <Button variant="white" onClick={() => setConfirm(false)}>
              Cancel
            </Button>
            <Button
              variant="deep"
              disabled={block.isPending}
              onClick={() => block.mutate({ userId: user.id, block: true }, { onSuccess: () => navigate('/friends') })}
            >
              Block
            </Button>
          </span>
        </Card>
      )}
      <div className="grid gap-5 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
        <div className="space-y-5">
          <BasicInfos p={user} />
          <Card>
            <CardHead>Topics of Interest</CardHead>
            <div className="p-4">
              {/* v1: interests you share are highlighted */}
              <Chips all={user.interests} selected={matched} />
            </div>
          </Card>
        </div>
        <Card className="h-fit overflow-hidden">
          <CardHead>Letters</CardHead>
          <div className="relative">
            <img src="/img/postcard.png" alt="" className="w-full" />
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 px-8 text-center">
              <p className="font-postcard text-[17px] text-ink">To. {user.displayName}</p>
              <p className="max-w-xs text-[13px] text-soft">
                Write in {languageName(user.nativeLang)} or {languageName(user.learningLang)} — a letter costs one stamp.
              </p>
              <Link to={`/compose/${user.id}`}>
                <Button variant="deep">✍ Write a letter</Button>
              </Link>
            </div>
          </div>
        </Card>
      </div>
    </div>
  )
}

const REASON = { SIGNUP: 'Welcome gift', CHECKIN: 'Daily event', SEND_LETTER: 'Write a letter', CORRECTION: 'Corrected a letter' }

function OwnProfile({ me }: { me: MyProfile }) {
  const { data: ledger } = useLedger()
  const checkIn = useCheckIn()
  const already = checkIn.error instanceof ApiError && checkIn.error.code === 'ALREADY_CHECKED_IN'

  return (
    <div className="mx-auto max-w-[900px] space-y-5">
      <ProfileHeader
        person={me}
        stampBalance={me.stampBalance}
        stats={<Stat label="Topics" value={me.interests.length} />}
        actions={
          <Link to="/profile/edit">
            <Button variant="deep">✍ Edit My profile</Button>
          </Link>
        }
      />
      <div className="grid gap-5 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
        <div className="space-y-5">
          <BasicInfos p={me} />
          <Card>
            <CardHead>Topics of Interest</CardHead>
            <div className="p-4">
              {me.interests.length ? (
                <Chips all={me.interests} selected={[]} />
              ) : (
                <Link to="/profile/edit" className="text-[13px] text-primary hover:underline">
                  Add some interests →
                </Link>
              )}
            </div>
          </Card>
          <BlockedList />
        </div>
        <div className="space-y-5">
          <Card>
            <CardHead right={<span className="text-[11px] text-muted">*Most recent 10 records.</span>}>Stamp History</CardHead>
            <div className="space-y-1.5 bg-canvas/60 p-3">
              {ledger?.content.map((e) => (
                <div key={e.id} className="grid grid-cols-[90px_1fr_50px] items-center rounded-[4px] border border-line bg-white px-3 py-2.5 text-[12px]">
                  <span className="text-soft">{dayDate(e.createdAt).replace(/\//g, '-')}</span>
                  <span className="border-l border-line pl-3 text-ink">{REASON[e.reason]}</span>
                  <span className={`border-l border-line text-center font-medium ${e.amount > 0 ? 'text-[#5b5bd6]' : 'text-coral'}`}>
                    {e.amount > 0 ? `+${e.amount}` : e.amount}
                  </span>
                </div>
              ))}
            </div>
          </Card>
          <Card className="px-5 py-7 text-center">
            <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-[#d4f5e4]">
              <img src="/img/clover.svg" alt="" className="h-8 w-8" />
            </span>
            <p className="mt-3 text-[13px] text-ink">Would you like to write to more friends?</p>
            <Button
              variant="grey"
              className="mt-3 px-6"
              disabled={checkIn.isPending || checkIn.isSuccess || already}
              onClick={() => checkIn.mutate()}
            >
              {checkIn.isSuccess ? '+1 stamp ✓' : already ? 'Checked in today ✓' : 'Daily check-in'}
            </Button>
          </Card>
        </div>
      </div>
    </div>
  )
}

function BlockedList() {
  const { data } = useBlocks()
  const block = useBlock()
  if (!data || data.content.length === 0) return null
  return (
    <Card>
      <CardHead>Blocked</CardHead>
      {data.content.map((b) => (
        <div key={b.user.id} className="flex items-center gap-3 border-b border-line px-5 py-3 last:border-0">
          <Avatar name={b.user.displayName} country={b.user.country} avatar={b.user.avatar} size={32} />
          <span className="flex-1 text-[13px]">{b.user.displayName}</span>
          <Button variant="white" className="px-3 py-1 text-[12px]" onClick={() => block.mutate({ userId: b.user.id, block: false })}>
            Unblock
          </Button>
        </div>
      ))}
    </Card>
  )
}

function PresetImage({ preset }: { preset: string }) {
  const uri = useAvatarUri(preset)
  return uri ? <img src={uri} alt="" className="h-full w-full rounded-full" /> : <span className="block h-full w-full rounded-full bg-canvas" />
}

const MAX_BIO = 500
const MAX_TOPICS = 10

function EditProfile({ me }: { me: MyProfile }) {
  const navigate = useNavigate()
  const { data: interests = [] } = useInterests()
  const update = useUpdateProfile()
  const [form, setForm] = useState({
    displayName: me.displayName,
    country: me.country,
    nativeLang: me.nativeLang,
    learningLang: me.learningLang,
    bio: me.bio ?? '',
    interestIds: me.interests.map((i) => i.id),
    avatar: me.avatar,
  })
  const { data: avatars = [] } = useAvatars()
  const set = (k: 'displayName' | 'country' | 'nativeLang' | 'learningLang' | 'bio') => (e: { target: { value: string } }) =>
    setForm({ ...form, [k]: e.target.value })
  const toggle = (id: number) =>
    setForm({
      ...form,
      interestIds: form.interestIds.includes(id)
        ? form.interestIds.filter((x) => x !== id)
        : form.interestIds.length >= MAX_TOPICS
          ? form.interestIds
          : [...form.interestIds, id],
    })
  const save = () => update.mutate(form, { onSuccess: () => navigate('/profile') })
  const err = update.error instanceof ApiError ? update.error : null

  const row = 'grid grid-cols-[110px_1fr] items-center gap-3 border-b border-line px-5 py-2.5 last:border-0'
  const input = 'w-full rounded-[3px] border border-line bg-white px-2.5 py-1.5 text-[13px] outline-none focus:border-primary'

  return (
    <div className="mx-auto max-w-[900px] space-y-5">
      <ProfileHeader
        person={{ ...me, displayName: form.displayName || me.displayName, avatar: form.avatar }}
        stampBalance={me.stampBalance}
        stats={<Stat label="Topics" value={form.interestIds.length} />}
        actions={
          <>
            <Button variant="white" onClick={() => navigate('/profile')}>
              Cancel
            </Button>
            <Button variant="deep" className="px-6" disabled={update.isPending} onClick={save}>
              SAVE
            </Button>
          </>
        }
      />
      {update.error && (
        <Card className="px-5 py-3">
          <ErrorText error={update.error} />
        </Card>
      )}
      <div className="grid gap-5 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
        <Card className="h-fit">
          <CardHead>Edit Basic Infos</CardHead>
          <div className={row}>
            <span className="text-[13px] text-soft">Email</span>
            <span className="truncate text-[13px] text-muted">{me.email}</span>
          </div>
          <div className={row}>
            <span className="text-[13px] text-soft">Name</span>
            <input className={input} value={form.displayName} maxLength={50} onChange={set('displayName')} />
          </div>
          <div className={row}>
            <span className="text-[13px] text-soft">Location</span>
            <select className={input} value={form.country} onChange={set('country')}>
              {(COUNTRIES.includes(form.country) ? COUNTRIES : [form.country, ...COUNTRIES]).map((c) => (
                <option key={c} value={c}>
                  {countryName(c)}
                </option>
              ))}
            </select>
          </div>
          <div className={row}>
            <span className="text-[13px] text-soft">Native</span>
            <select className={input} value={form.nativeLang} onChange={set('nativeLang')}>
              {LANGUAGES.map((l) => (
                <option key={l} value={l}>
                  {languageName(l)}
                </option>
              ))}
            </select>
          </div>
          <div className={row}>
            <span className="text-[13px] text-soft">Language</span>
            <select className={input} value={form.learningLang} onChange={set('learningLang')}>
              {LANGUAGES.map((l) => (
                <option key={l} value={l}>
                  {languageName(l)}
                </option>
              ))}
            </select>
          </div>
          {err?.fieldError('displayName') && <p className="px-5 pb-3 text-[12px] text-coral">{err.fieldError('displayName')}</p>}
        </Card>
        <div className="space-y-5">
          <Card>
            <CardHead>Profile Picture</CardHead>
            <div className="grid grid-cols-6 gap-2 p-4 sm:grid-cols-8">
              {avatars.map((a) => (
                <button
                  key={a}
                  type="button"
                  onClick={() => setForm({ ...form, avatar: a })}
                  className={`aspect-square rounded-full p-0.5 transition ${
                    form.avatar === a ? 'ring-[3px] ring-primary' : 'hover:ring-2 hover:ring-line'
                  }`}
                  aria-label={`Use character ${a} as profile picture`}
                >
                  <PresetImage preset={a} />
                </button>
              ))}
            </div>
          </Card>
          <Card>
            <CardHead right={`(${form.bio.length} / ${MAX_BIO})`}>About Me</CardHead>
            <textarea
              rows={8}
              maxLength={MAX_BIO}
              value={form.bio}
              onChange={set('bio')}
              className="block w-full resize-none rounded-b-[6px] px-5 py-4 text-[13px] leading-relaxed outline-none"
              placeholder="What do you like to talk about?"
            />
          </Card>
          <Card>
            <CardHead right={`(${form.interestIds.length} / ${MAX_TOPICS})`}>Topics of Interest</CardHead>
            <div className="p-4">
              <Chips all={interests} selected={form.interestIds} onToggle={toggle} />
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}
