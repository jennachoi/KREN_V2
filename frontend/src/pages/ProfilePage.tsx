import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { useBlock, useBlocks, useInterests, useLedger, useMe, useUpdateProfile } from '../api/hooks'
import type { MyProfile } from '../api/types'
import { Avatar, Button, Card, ErrorBanner, Field, Input, InterestChips, PageTitle, Select, Spinner, Textarea } from '../components/ui'
import { COUNTRIES, LANGUAGES, countryName, flag, formatDate, languageName } from '../lib/locale'

export function ProfilePage() {
  const { data: me } = useMe()
  const [params] = useSearchParams()
  if (!me) return <Spinner />
  return (
    <>
      <PageTitle sub={params.get('welcome') ? 'Welcome! Add a short bio and some interests so penpals can find you.' : me.email}>
        Your profile
      </PageTitle>
      <div className="grid gap-6 lg:grid-cols-[1fr_18rem]">
        <ProfileForm me={me} />
        <div className="space-y-6">
          <StampHistory />
          <BlockedList />
        </div>
      </div>
    </>
  )
}

function ProfileForm({ me }: { me: MyProfile }) {
  const { data: interests = [] } = useInterests()
  const update = useUpdateProfile()
  const [form, setForm] = useState({
    displayName: me.displayName,
    country: me.country,
    nativeLang: me.nativeLang,
    learningLang: me.learningLang,
    bio: me.bio ?? '',
    interestIds: me.interests.map((i) => i.id),
  })
  const set = (k: 'displayName' | 'country' | 'nativeLang' | 'learningLang' | 'bio') => (e: { target: { value: string } }) =>
    setForm({ ...form, [k]: e.target.value })
  const toggle = (id: number) =>
    setForm({
      ...form,
      interestIds: form.interestIds.includes(id) ? form.interestIds.filter((x) => x !== id) : [...form.interestIds, id],
    })
  const err = update.error instanceof ApiError ? update.error : null

  const submit = (e: FormEvent) => {
    e.preventDefault()
    update.mutate(form)
  }

  return (
    <Card>
      <form onSubmit={submit} className="space-y-4">
        <Field label="Display name" error={err?.fieldError('displayName')}>
          <Input value={form.displayName} onChange={set('displayName')} maxLength={50} required />
        </Field>
        <Field label="Country">
          <Select value={form.country} onChange={set('country')}>
            {(COUNTRIES.includes(form.country) ? COUNTRIES : [form.country, ...COUNTRIES]).map((c) => (
              <option key={c} value={c}>
                {flag(c)} {countryName(c)}
              </option>
            ))}
          </Select>
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="I speak">
            <Select value={form.nativeLang} onChange={set('nativeLang')}>
              {LANGUAGES.map((l) => (
                <option key={l} value={l}>
                  {languageName(l)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="I'm learning">
            <Select value={form.learningLang} onChange={set('learningLang')}>
              {LANGUAGES.map((l) => (
                <option key={l} value={l}>
                  {languageName(l)}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <Field label="About you" error={err?.fieldError('bio')}>
          <Textarea rows={4} value={form.bio} onChange={set('bio')} maxLength={500} placeholder="What do you like to talk about?" />
        </Field>
        <Field label="Interests (up to 10)" error={err?.fieldError('interestIds')}>
          <InterestChips all={interests} selected={form.interestIds} onToggle={toggle} />
        </Field>
        {!err?.fieldErrors.length && <ErrorBanner error={update.error} />}
        <div className="flex items-center gap-3">
          <Button type="submit" disabled={update.isPending}>
            {update.isPending ? 'Saving…' : 'Save profile'}
          </Button>
          {update.isSuccess && <span className="text-sm text-sky">Saved ✓</span>}
        </div>
      </form>
    </Card>
  )
}

const REASON_LABEL = { SIGNUP: 'Welcome bonus', CHECKIN: 'Daily check-in', SEND_LETTER: 'Sent a letter' }

function StampHistory() {
  const { data } = useLedger()
  return (
    <Card>
      <h2 className="mb-3 font-serif text-lg font-semibold">Stamp history</h2>
      <ul className="space-y-2 text-sm">
        {data?.content.map((e) => (
          <li key={e.id} className="flex justify-between gap-2">
            <span>
              {REASON_LABEL[e.reason]}
              <span className="block text-xs text-muted">{formatDate(e.createdAt)}</span>
            </span>
            <span className={e.amount > 0 ? 'font-medium text-sky' : 'font-medium text-stamp'}>
              {e.amount > 0 ? `+${e.amount}` : e.amount}
            </span>
          </li>
        ))}
      </ul>
    </Card>
  )
}

function BlockedList() {
  const { data } = useBlocks()
  const block = useBlock()
  if (!data || data.content.length === 0) return null
  return (
    <Card>
      <h2 className="mb-3 font-serif text-lg font-semibold">Blocked</h2>
      <ul className="space-y-3">
        {data.content.map((b) => (
          <li key={b.user.id} className="flex items-center gap-3">
            <Avatar name={b.user.displayName} country={b.user.country} />
            <span className="flex-1 text-sm">{b.user.displayName}</span>
            <Button variant="ghost" className="px-2 py-1 text-xs" onClick={() => block.mutate({ userId: b.user.id, block: false })}>
              Unblock
            </Button>
          </li>
        ))}
      </ul>
    </Card>
  )
}
