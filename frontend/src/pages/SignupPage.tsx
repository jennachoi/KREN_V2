import { useState, type ReactNode } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { Flag, Lock, Map, PenLine, User } from 'lucide-react'
import { ApiError } from '../api/client'
import { useInterests, useMe, useSignup, useUpdateProfile } from '../api/hooks'
import { BoxInput, BoxSelect, Button, Card, Chips, ErrorText, FieldBox } from '../components/ui'
import { COUNTRIES, LANGUAGES, countryName, flag, languageName } from '../lib/locale'

const STEPS = [
  { icon: Lock, title: "Let's get started." },
  { icon: Map, title: 'Where are you?' },
  { icon: User, title: 'Tell us more about you.' },
  { icon: PenLine, title: 'Introduce yourself.' },
  { icon: Flag, title: "You're all set. Ready?" },
]

const MIN_TOPICS = 3
const MAX_TOPICS = 10
const MAX_BIO = 500

/** v1 multi-step sign-up: progress rail of icon circles, one card per step, Back / Next below. */
export function SignupPage() {
  const { data: me } = useMe()
  const navigate = useNavigate()
  const { data: interests = [] } = useInterests()
  const signup = useSignup()
  const updateProfile = useUpdateProfile()

  const [step, setStep] = useState(0)
  const [form, setForm] = useState({
    email: '',
    displayName: '',
    password: '',
    passwordCheck: '',
    country: 'KR',
    nativeLang: 'ko',
    learningLang: 'en',
    bio: '',
    interestIds: [] as number[],
  })
  const [touched, setTouched] = useState(false)
  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm({ ...form, [k]: e.target.value })
  const serverError = signup.error instanceof ApiError ? signup.error : null

  // Already signed in (and not in the middle of finishing sign-up) -> nothing to do here.
  if (me && step < 4) return <Navigate to="/" replace />

  const stepErrors: Record<number, Record<string, string | undefined>> = {
    0: {
      email: !/^\S+@\S+\.\S+$/.test(form.email) ? 'Enter your email.' : serverError?.fieldError('email'),
      displayName: !form.displayName.trim() ? 'Name cannot be empty.' : serverError?.fieldError('displayName'),
      password: form.password.length < 8 ? 'At least 8 characters.' : serverError?.fieldError('password'),
      passwordCheck: form.password !== form.passwordCheck ? 'Passwords do not match.' : undefined,
    },
    2: {
      learningLang: form.nativeLang === form.learningLang ? 'Pick a language other than your native one.' : undefined,
      interestIds: form.interestIds.length < MIN_TOPICS ? `Please select at least ${MIN_TOPICS} interests.` : undefined,
    },
  }
  const errors = stepErrors[step] ?? {}
  const shown = (k: string) => (touched ? errors[k] : undefined)
  const emailTaken = serverError?.code === 'EMAIL_TAKEN'

  const next = () => {
    setTouched(true)
    if (Object.values(errors).some(Boolean)) return
    setTouched(false)
    if (step === 3) return finish()
    setStep(step + 1)
  }

  // Create the account, then save bio + interests (the profile endpoint owns those).
  const finish = () => {
    signup.mutate(
      {
        email: form.email,
        password: form.password,
        displayName: form.displayName,
        country: form.country,
        nativeLang: form.nativeLang,
        learningLang: form.learningLang,
      },
      {
        onSuccess: () =>
          updateProfile.mutate(
            {
              displayName: form.displayName,
              country: form.country,
              nativeLang: form.nativeLang,
              learningLang: form.learningLang,
              bio: form.bio,
              interestIds: form.interestIds,
            },
            { onSettled: () => setStep(4) },
          ),
        onError: (e) => {
          if (e instanceof ApiError && (e.code === 'EMAIL_TAKEN' || e.fieldErrors.length)) setStep(0)
        },
      },
    )
  }

  const toggleTopic = (id: number) =>
    setForm({
      ...form,
      interestIds: form.interestIds.includes(id)
        ? form.interestIds.filter((x) => x !== id)
        : form.interestIds.length >= MAX_TOPICS
          ? form.interestIds
          : [...form.interestIds, id],
    })

  return (
    <div className="min-h-screen px-4 pt-6 pb-16">
      <div className="mx-auto max-w-[520px]">
        <Link to="/login" className="mb-6 flex justify-center">
          <img src="/logo.png" alt="KREN" className="h-10" />
        </Link>
        <ProgressRail step={step} />
        <h1 className="mt-7 mb-5 text-center font-display text-[17px] text-ink">{STEPS[step].title}</h1>

        {step === 0 && (
          <Card className="space-y-3 p-5">
            <FieldBox label="Email" error={emailTaken ? 'This email is already in use.' : shown('email')}>
              <BoxInput type="email" autoComplete="email" placeholder="Enter your email." value={form.email} onChange={set('email')} />
            </FieldBox>
            <FieldBox label="Name" error={shown('displayName')}>
              <BoxInput placeholder="The name penpals will see (up to 50 characters)." maxLength={50} value={form.displayName} onChange={set('displayName')} />
            </FieldBox>
            <FieldBox label="Password" error={shown('password')}>
              <BoxInput type="password" autoComplete="new-password" placeholder="Enter your password." maxLength={72} value={form.password} onChange={set('password')} />
            </FieldBox>
            <FieldBox label="Password check" error={shown('passwordCheck')}>
              <BoxInput type="password" autoComplete="new-password" placeholder="Enter your password again." maxLength={72} value={form.passwordCheck} onChange={set('passwordCheck')} />
            </FieldBox>
          </Card>
        )}

        {step === 1 && (
          <Card className="flex items-center gap-6 p-6">
            <div className="flex h-28 w-28 shrink-0 items-center justify-center rounded-[6px] bg-gradient-to-br from-[#d9e7fa] to-[#a9c7f0] text-6xl">
              {flag(form.country)}
            </div>
            <div className="flex-1">
              <FieldBox label="Country">
                <BoxSelect value={form.country} onChange={set('country')}>
                  {COUNTRIES.map((c) => (
                    <option key={c} value={c}>
                      {countryName(c)}
                    </option>
                  ))}
                </BoxSelect>
              </FieldBox>
              <p className="mt-3 text-[18px] text-ink">{countryName(form.country)}</p>
              <p className="text-[12px] text-muted">Shown on your profile so penpals know where you write from.</p>
            </div>
          </Card>
        )}

        {step === 2 && (
          <Card className="p-6">
            <div className="mx-auto mb-6 grid max-w-xs grid-cols-2 gap-4 text-center">
              <LangPick label="Native" value={form.nativeLang} onChange={set('nativeLang')} />
              <LangPick label="Learn" value={form.learningLang} onChange={set('learningLang')} />
            </div>
            {shown('learningLang') && <p className="-mt-3 mb-4 text-center text-[12px] text-coral">{shown('learningLang')}</p>}
            <div className="mb-3 flex items-center justify-between">
              <span className="font-display text-[13px] font-semibold text-ink">Topics of Interest</span>
              <span className="text-[13px] text-soft">
                ( {form.interestIds.length} / {MAX_TOPICS} )
              </span>
            </div>
            <Chips all={interests} selected={form.interestIds} onToggle={toggleTopic} />
            {shown('interestIds') && <p className="mt-3 text-[12px] text-coral">{shown('interestIds')}</p>}
          </Card>
        )}

        {step === 3 && (
          <Card className="p-5">
            <div className="mb-2 flex items-center justify-between">
              <span className="font-display text-[13px] font-semibold">About Me</span>
              <span className="text-[12px] text-soft">
                ({form.bio.length} / {MAX_BIO})
              </span>
            </div>
            <textarea
              rows={7}
              maxLength={MAX_BIO}
              value={form.bio}
              onChange={set('bio')}
              placeholder="Hello! I'm looking for a friend to do a language exchange with me…"
              className="w-full resize-none rounded-[4px] border border-line bg-[#fafafa] p-3 text-[13px] leading-relaxed outline-none focus:border-primary focus:bg-white"
            />
            <p className="mt-1 text-[12px] text-muted">Optional — you can change it later on your profile.</p>
            <div className="mt-3">
              <ErrorText error={signup.error} />
            </div>
          </Card>
        )}

        {step === 4 && (
          <Card className="px-6 py-10 text-center">
            <img src="/logo-mark.png" alt="" className="mx-auto w-40" />
            <p className="mt-5 font-display text-[16px] text-ink">Congratz, you successfully created your account.</p>
            <p className="mt-1 text-[13px] text-muted">We put 5 stamps in your stamp box — each letter costs one.</p>
            <Button className="mt-7 px-8" onClick={() => navigate('/')}>
              HOME
            </Button>
          </Card>
        )}

        {step < 4 && (
          <div className="mt-5 flex justify-end gap-2">
            {step > 0 && (
              <Button variant="white" className="w-20" onClick={() => setStep(step - 1)}>
                Back
              </Button>
            )}
            <Button variant="white" className="w-20" disabled={signup.isPending || updateProfile.isPending} onClick={next}>
              {step === 3 ? (signup.isPending ? '…' : 'Finish') : 'Next'}
            </Button>
          </div>
        )}
        {step === 0 && (
          <p className="mt-6 text-center text-[13px] text-muted">
            Already have an account?{' '}
            <Link to="/login" className="text-primary hover:underline">
              Login
            </Link>
          </p>
        )}
      </div>
    </div>
  )
}

function ProgressRail({ step }: { step: number }) {
  return (
    <div className="flex items-center justify-center">
      {STEPS.map(({ icon: Icon }, i) => (
        <Rail key={i} first={i === 0} done={i <= step}>
          <span
            className={`flex h-9 w-9 items-center justify-center rounded-full border-2 bg-white shadow-[0_0_0_6px_rgba(255,255,255,0.8)] ${
              i === step ? 'border-deep text-deep' : i < step ? 'border-primary text-primary' : 'border-[#e5e5e5] text-[#bbb]'
            }`}
          >
            <Icon size={15} strokeWidth={1.8} />
          </span>
        </Rail>
      ))}
    </div>
  )
}

function Rail({ first, done, children }: { first: boolean; done: boolean; children: ReactNode }) {
  return (
    <>
      {!first && <span className={`h-[5px] w-12 sm:w-16 ${done ? 'bg-primary' : 'bg-[#e5e5e5]'}`} />}
      {children}
    </>
  )
}

function LangPick({ label, value, onChange }: { label: string; value: string; onChange: (e: { target: { value: string } }) => void }) {
  return (
    <label className="block">
      <span className="mb-1 block font-display text-[13px] font-semibold text-ink">{label}</span>
      <select value={value} onChange={onChange} className="w-full rounded-[3px] border border-[#ccc] bg-white px-2 py-1 text-[13px]">
        {LANGUAGES.map((l) => (
          <option key={l} value={l}>
            {languageName(l)}
          </option>
        ))}
      </select>
    </label>
  )
}
