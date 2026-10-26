import { useState, type CSSProperties, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { useLogin, useMe } from '../api/hooks'
import { ErrorText } from '../components/ui'

// Scattered dots behind the split, like the v1 landing, each drifting slowly in its own direction.
// Pseudo-random but fixed per dot so the layout doesn't reshuffle on every render.
const DOTS = Array.from({ length: 70 }, (_, i) => {
  const r = (n: number) => ((Math.sin(i * 12.9898 + n * 78.233) * 43758.5453) % 1 + 1) % 1
  const angle = r(5) * Math.PI * 2
  const reach = 20 + r(6) * 50
  return {
    left: r(1) * 100,
    top: r(2) * 100,
    size: 3 + r(3) * 7,
    opacity: 0.35 + r(4) * 0.55,
    drift: {
      '--dx': `${Math.cos(angle) * reach}px`,
      '--dy': `${Math.sin(angle) * reach}px`,
      animationDuration: `${7 + r(7) * 9}s`,
      animationDelay: `${-r(8) * 10}s`,
    } as CSSProperties,
  }
})

const DEMO = { email: 'emma@demo.kren', password: 'demo1234' }

const pill =
  'h-10 rounded-full text-[12px] font-semibold tracking-wider transition-colors'

/** v1 main screen: white/blue diagonal split, big KREN wordmark and the login form on the blue side. */
export function LoginPage() {
  const { data: me } = useMe()
  const navigate = useNavigate()
  const login = useLogin()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  if (me) return <Navigate to="/" replace />

  const submit = (e: FormEvent) => {
    e.preventDefault()
    login.mutate({ email, password }, { onSuccess: () => navigate('/') })
  }

  return (
    <div className="relative min-h-screen overflow-hidden bg-white">
      {DOTS.map((d, i) => (
        <span
          key={i}
          className="drift absolute rounded-full bg-primary"
          style={{ left: `${d.left}%`, top: `${d.top}%`, width: d.size, height: d.size, opacity: d.opacity, ...d.drift }}
        />
      ))}
      <div
        className="absolute inset-0 bg-[#6a9fe6] max-md:[clip-path:polygon(0_45%,100%_25%,100%_100%,0_100%)] md:[clip-path:polygon(25%_0,100%_0,100%_100%,72%_100%)]"
        aria-hidden
      >
        {DOTS.slice(0, 35).map((d, i) => (
          <span
            key={i}
            className="drift absolute rounded-full bg-[#4f86d4]"
            style={{ left: `${(d.left + 37) % 100}%`, top: `${(d.top + 51) % 100}%`, width: d.size, height: d.size, ...d.drift }}
          />
        ))}
      </div>

      <div className="relative mx-auto flex min-h-screen max-w-[1100px] flex-col items-center justify-center gap-10 px-6 py-12 md:flex-row md:justify-between">
        <div className="flex flex-col items-center md:w-1/2">
          <img src="/img/friends.svg" alt="" className="w-72 md:w-[420px]" />
        </div>

        <form onSubmit={submit} className="w-full max-w-[330px] md:mr-6">
          <h1 className="mb-6 text-center font-display text-[64px] leading-none font-extrabold tracking-wide text-white [text-shadow:4px_4px_0_#3d70b2]">
            KREN
          </h1>
          <div className="space-y-2.5">
            <input
              type="email"
              autoComplete="email"
              placeholder="EMAIL"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="h-10 w-full rounded-[3px] bg-white/90 px-4 text-[13px] text-ink outline-none placeholder:text-[11px] placeholder:tracking-wider placeholder:text-[#aaa] focus:bg-white"
            />
            <input
              type="password"
              autoComplete="current-password"
              placeholder="PASSWORD"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="h-10 w-full rounded-[3px] bg-white/90 px-4 text-[13px] text-ink outline-none placeholder:text-[11px] placeholder:tracking-wider placeholder:text-[#aaa] focus:bg-white"
            />
            <button type="submit" disabled={login.isPending} className={`${pill} w-full bg-navy text-white hover:bg-[#25426b] disabled:opacity-60`}>
              {login.isPending ? 'SIGNING IN…' : 'LOGIN'}
            </button>
            {login.error && (
              <div className="rounded-[3px] bg-white/90 px-3 py-2">
                <ErrorText error={login.error} />
              </div>
            )}
            <div className="grid grid-cols-2 gap-2.5 pt-1.5">
              <Link to="/signup" className={`${pill} flex items-center justify-center border border-white/70 text-white hover:bg-white/10`}>
                SIGN UP
              </Link>
              <button
                type="button"
                disabled={login.isPending}
                // One click straight into the demo account, for visitors who just want to look around.
                onClick={() => {
                  setEmail(DEMO.email)
                  setPassword(DEMO.password)
                  login.mutate(DEMO, { onSuccess: () => navigate('/') })
                }}
                className={`${pill} border border-white/70 text-white hover:bg-white/10`}
              >
                TRY A DEMO
              </button>
            </div>
            <p className="pt-2 text-center text-[11px] text-white/85">
              Demo accounts: emma@demo.kren · minji@demo.kren — password demo1234
            </p>
          </div>
        </form>
      </div>
    </div>
  )
}
