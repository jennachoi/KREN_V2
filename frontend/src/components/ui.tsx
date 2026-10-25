import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react'
import { ApiError } from '../api/client'
import type { Interest } from '../api/types'
import { flag } from '../lib/locale'

type ButtonVariant = 'primary' | 'deep' | 'white' | 'grey'

export function Button({
  variant = 'primary',
  className = '',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant }) {
  const styles = {
    primary: 'bg-primary text-white hover:bg-[#4a88d6]',
    deep: 'bg-deep text-white hover:bg-[#33619d]',
    white: 'border border-line bg-white text-ink hover:bg-canvas',
    grey: 'bg-[#efefef] text-soft hover:bg-[#e5e5e5]',
  }[variant]
  return (
    <button
      className={`inline-flex items-center justify-center gap-1.5 rounded-[4px] px-4 py-2 text-[13px] font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${styles} ${className}`}
      {...props}
    />
  )
}

/** White panel with the thin grey border v1 used for every block. */
export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-[6px] border border-line bg-white ${className}`}>{children}</div>
}

/** v1 card heading: its own bordered strip above the card body. */
export function CardHead({ children, right }: { children: ReactNode; right?: ReactNode }) {
  return (
    <div className="flex items-center justify-between border-b border-line px-5 py-3.5">
      <h3 className="font-display text-[14px] font-medium text-ink">{children}</h3>
      {right && <span className="text-[13px] text-soft">{right}</span>}
    </div>
  )
}

/** v1 form field: a bordered box with a tiny uppercase label sitting above a borderless input. */
export function FieldBox({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div>
      <label className="block rounded-[4px] border border-line bg-[#fafafa] px-3 pt-2 pb-1.5 focus-within:border-primary focus-within:bg-white">
        <span className="block font-display text-[10px] font-medium tracking-wide text-ink uppercase">{label}</span>
        {children}
      </label>
      {error && <p className="mt-1 pl-1 text-[12px] text-coral">{error}</p>}
    </div>
  )
}

export const BoxInput = (props: InputHTMLAttributes<HTMLInputElement>) => (
  <input className="w-full bg-transparent py-0.5 text-[13px] text-ink outline-none placeholder:text-[#bbb]" {...props} />
)

export const BoxSelect = (props: SelectHTMLAttributes<HTMLSelectElement>) => (
  <select className="w-full bg-transparent py-0.5 text-[13px] text-ink outline-none" {...props} />
)

// Soft background per avatar so a page of people doesn't look like one colour.
const AVATAR_BACKGROUNDS = [
  'from-[#ffe3e3] to-[#ffc9c9]',
  'from-[#fff3bf] to-[#ffe066]',
  'from-[#d3f9d8] to-[#b2f2bb]',
  'from-[#d0ebff] to-[#a5d8ff]',
  'from-[#e5dbff] to-[#d0bfff]',
  'from-[#ffe8cc] to-[#ffd8a8]',
  'from-[#c5f6fa] to-[#99e9f2]',
  'from-[#f3d9fa] to-[#eebefa]',
]

function backgroundFor(seed: string) {
  let h = 0
  for (const ch of seed) h = (h * 31 + ch.codePointAt(0)!) >>> 0
  return AVATAR_BACKGROUNDS[h % AVATAR_BACKGROUNDS.length]
}

/** Emoji profile picture on a pastel disc, with the country flag as a badge. Falls back to the initial. */
export function Avatar({ name, country, avatar, size = 44 }: { name: string; country: string; avatar?: string | null; size?: number }) {
  return (
    <div className="relative shrink-0" style={{ width: size, height: size }}>
      <div
        className={`flex h-full w-full items-center justify-center rounded-full border-[3px] border-white bg-gradient-to-br font-display font-semibold text-deep shadow-sm ${backgroundFor(avatar ?? name)}`}
        style={{ fontSize: avatar ? size * 0.5 : size * 0.4 }}
      >
        {avatar ?? name.slice(0, 1).toUpperCase()}
      </div>
      <span
        className="absolute -bottom-0.5 -left-0.5 flex items-center justify-center rounded-full bg-white leading-none shadow-sm"
        style={{ width: size * 0.42, height: size * 0.42, fontSize: size * 0.26 }}
      >
        {flag(country)}
      </span>
    </div>
  )
}

/** v1 topic pills: grey by default, solid blue when selected or matched. */
export function Chips({
  all,
  selected,
  onToggle,
}: {
  all: Interest[]
  selected: number[]
  onToggle?: (id: number) => void
}) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {all.map((i) => {
        const on = selected.includes(i.id)
        return (
          <button
            key={i.id}
            type="button"
            disabled={!onToggle}
            onClick={() => onToggle?.(i.id)}
            className={`rounded-full px-3 py-1 text-[12px] transition-colors ${
              on ? 'bg-primary font-medium text-white' : 'bg-chip text-soft'
            } ${onToggle ? (on ? 'hover:bg-[#4a88d6]' : 'hover:bg-[#e2e2e2]') : 'cursor-default'}`}
          >
            {i.name}
          </button>
        )
      })}
    </div>
  )
}

/** The v1 letter-card stamp. */
export const StampIcon = ({ className = '' }: { className?: string }) => (
  <img src="/img/stamp.png" alt="" className={className} />
)

export function ErrorText({ error }: { error: unknown }) {
  if (!error) return null
  const message = error instanceof ApiError ? error.message : 'Something went wrong. Please try again.'
  return <p className="text-[12px] text-coral">{message}</p>
}

export function Pager({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (p: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <div className="mt-6 flex items-center justify-center gap-1 text-[13px]">
      <Button variant="white" className="px-3 py-1.5" disabled={page === 0} onClick={() => onPage(page - 1)}>
        Previous
      </Button>
      <span className="px-3 text-soft">
        {page + 1} / {totalPages}
      </span>
      <Button variant="white" className="px-3 py-1.5" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
        Next
      </Button>
    </div>
  )
}

export const Spinner = () => <div className="py-16 text-center text-[13px] text-muted">Loading…</div>
