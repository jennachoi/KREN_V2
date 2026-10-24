import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'
import { ApiError } from '../api/client'
import type { Interest } from '../api/types'
import { flag, languageName } from '../lib/locale'

export function Button({
  variant = 'primary',
  className = '',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'ghost' | 'danger' }) {
  const styles = {
    primary: 'bg-stamp text-white hover:bg-stamp-dark disabled:opacity-50',
    ghost: 'border border-line bg-card text-ink hover:bg-paper disabled:opacity-50',
    danger: 'border border-stamp/40 text-stamp hover:bg-stamp/5 disabled:opacity-50',
  }[variant]
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors ${styles} ${className}`}
      {...props}
    />
  )
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-2xl border border-line bg-card p-5 shadow-sm ${className}`}>{children}</div>
}

export function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-ink">{label}</span>
      {children}
      {error && <span className="mt-1 block text-xs text-stamp">{error}</span>}
    </label>
  )
}

const inputClass =
  'w-full rounded-lg border border-line bg-white px-3 py-2 text-sm outline-none focus:border-sky focus:ring-2 focus:ring-sky/20'

export const Input = (props: InputHTMLAttributes<HTMLInputElement>) => <input className={inputClass} {...props} />
export const Select = (props: SelectHTMLAttributes<HTMLSelectElement>) => <select className={inputClass} {...props} />
export const Textarea = (props: TextareaHTMLAttributes<HTMLTextAreaElement>) => (
  <textarea className={`${inputClass} leading-relaxed`} {...props} />
)

export function ErrorBanner({ error }: { error: unknown }) {
  if (!error) return null
  const message = error instanceof ApiError ? error.message : 'Something went wrong. Please try again.'
  return <div className="rounded-lg border border-stamp/30 bg-stamp/5 px-3 py-2 text-sm text-stamp">{message}</div>
}

export function PageTitle({ children, sub }: { children: ReactNode; sub?: ReactNode }) {
  return (
    <div className="mb-6">
      <h1 className="font-serif text-3xl font-semibold text-ink">{children}</h1>
      {sub && <p className="mt-1 text-sm text-muted">{sub}</p>}
    </div>
  )
}

export function LangPair({ native, learning }: { native: string; learning: string }) {
  return (
    <span className="text-sm text-muted">
      Speaks <b className="font-medium text-ink">{languageName(native)}</b> · Learning{' '}
      <b className="font-medium text-ink">{languageName(learning)}</b>
    </span>
  )
}

export function Avatar({ name, country }: { name: string; country: string }) {
  return (
    <div className="relative flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-sky/10 font-serif text-lg font-semibold text-sky">
      {name.slice(0, 1).toUpperCase()}
      <span className="absolute -right-1 -bottom-1 text-base leading-none">{flag(country)}</span>
    </div>
  )
}

export function InterestChips({
  all,
  selected,
  onToggle,
}: {
  all: Interest[]
  selected: number[]
  onToggle?: (id: number) => void
}) {
  return (
    <div className="flex flex-wrap gap-2">
      {all.map((i) => {
        const on = selected.includes(i.id)
        return (
          <button
            key={i.id}
            type="button"
            disabled={!onToggle}
            onClick={() => onToggle?.(i.id)}
            className={`rounded-full border px-3 py-1 text-xs transition-colors ${
              on ? 'border-sky bg-sky text-white' : 'border-line bg-white text-muted hover:border-sky/50'
            } ${onToggle ? '' : 'cursor-default'}`}
          >
            {i.name}
          </button>
        )
      })}
    </div>
  )
}

export function Pager({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (p: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <div className="mt-6 flex items-center justify-center gap-3 text-sm">
      <Button variant="ghost" disabled={page === 0} onClick={() => onPage(page - 1)}>
        ← Prev
      </Button>
      <span className="text-muted">
        {page + 1} / {totalPages}
      </span>
      <Button variant="ghost" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
        Next →
      </Button>
    </div>
  )
}

export const Spinner = () => <div className="py-12 text-center text-sm text-muted">Loading…</div>
