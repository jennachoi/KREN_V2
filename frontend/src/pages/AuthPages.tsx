import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useLogin, useMe, useSignup } from '../api/hooks'
import { Button, Card, ErrorBanner, Field, Input, Select } from '../components/ui'
import { COUNTRIES, LANGUAGES, countryName, flag, languageName } from '../lib/locale'

function AuthShell({ title, children }: { title: string; children: ReactNode }) {
  const { data: me } = useMe()
  if (me) return <Navigate to="/penpals" replace />
  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 py-12">
      <div className="mb-8 text-center">
        <div className="font-serif text-5xl font-semibold text-stamp">KREN</div>
        <p className="mt-2 text-muted">Write letters to people who speak the language you're learning.</p>
      </div>
      <Card className="w-full max-w-md">
        <h1 className="mb-5 font-serif text-2xl font-semibold">{title}</h1>
        {children}
      </Card>
    </div>
  )
}

export function LoginPage() {
  const navigate = useNavigate()
  const login = useLogin()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const submit = (e: FormEvent) => {
    e.preventDefault()
    login.mutate({ email, password }, { onSuccess: () => navigate('/penpals') })
  }

  return (
    <AuthShell title="Welcome back">
      <form onSubmit={submit} className="space-y-4">
        <Field label="Email">
          <Input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </Field>
        <Field label="Password">
          <Input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </Field>
        <ErrorBanner error={login.error} />
        <Button type="submit" className="w-full" disabled={login.isPending}>
          {login.isPending ? 'Signing in…' : 'Sign in'}
        </Button>
      </form>
      <button
        type="button"
        onClick={() => {
          setEmail('emma@demo.kren')
          setPassword('demo1234')
        }}
        className="mt-4 w-full rounded-lg border border-dashed border-line px-3 py-2 text-left text-xs text-muted hover:border-sky/50"
      >
        <b className="text-ink">Just looking?</b> Use a demo account: emma@demo.kren or minji@demo.kren, password{' '}
        <code>demo1234</code> (click to fill)
      </button>
      <p className="mt-5 text-center text-sm text-muted">
        New here?{' '}
        <Link to="/signup" className="font-medium text-sky hover:underline">
          Create an account
        </Link>
      </p>
    </AuthShell>
  )
}

export function SignupPage() {
  const navigate = useNavigate()
  const signup = useSignup()
  const [form, setForm] = useState({
    email: '',
    password: '',
    displayName: '',
    country: 'KR',
    nativeLang: 'ko',
    learningLang: 'en',
  })
  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm({ ...form, [k]: e.target.value })
  const err = signup.error instanceof ApiError ? signup.error : null

  const submit = (e: FormEvent) => {
    e.preventDefault()
    signup.mutate(form, { onSuccess: () => navigate('/profile?welcome=1') })
  }

  return (
    <AuthShell title="Create your account">
      <form onSubmit={submit} className="space-y-4">
        <Field label="Email" error={err?.fieldError('email')}>
          <Input type="email" autoComplete="email" value={form.email} onChange={set('email')} required />
        </Field>
        <Field label="Password (8+ characters)" error={err?.fieldError('password')}>
          <Input type="password" autoComplete="new-password" value={form.password} onChange={set('password')} minLength={8} maxLength={72} required />
        </Field>
        <Field label="Display name" error={err?.fieldError('displayName')}>
          <Input value={form.displayName} onChange={set('displayName')} maxLength={50} required />
        </Field>
        <Field label="Country" error={err?.fieldError('country')}>
          <Select value={form.country} onChange={set('country')}>
            {COUNTRIES.map((c) => (
              <option key={c} value={c}>
                {flag(c)} {countryName(c)}
              </option>
            ))}
          </Select>
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="I speak" error={err?.fieldError('nativeLang')}>
            <Select value={form.nativeLang} onChange={set('nativeLang')}>
              {LANGUAGES.map((l) => (
                <option key={l} value={l}>
                  {languageName(l)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="I'm learning" error={err?.fieldError('learningLang')}>
            <Select value={form.learningLang} onChange={set('learningLang')}>
              {LANGUAGES.map((l) => (
                <option key={l} value={l}>
                  {languageName(l)}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        {err?.fieldErrors.length ? null : <ErrorBanner error={signup.error} />}
        <Button type="submit" className="w-full" disabled={signup.isPending}>
          {signup.isPending ? 'Creating…' : 'Sign up — get 5 free stamps'}
        </Button>
      </form>
      <p className="mt-5 text-center text-sm text-muted">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-sky hover:underline">
          Sign in
        </Link>
      </p>
    </AuthShell>
  )
}
