import { useEffect, useState } from 'react'

/** Re-renders every `intervalMs` with the current time, for live countdowns and the moving plane. */
export function useNow(intervalMs = 1000) {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), intervalMs)
    return () => clearInterval(t)
  }, [intervalMs])
  return now
}

/** "1d 2h", "3h 12m", "12m", "< 1m" — no seconds, so the page isn't ticking constantly. */
export function formatRemaining(ms: number) {
  if (ms <= 0) return 'arriving…'
  const totalMin = Math.ceil(ms / 60_000)
  const d = Math.floor(totalMin / 1440)
  const h = Math.floor((totalMin % 1440) / 60)
  const m = totalMin % 60
  if (d > 0) return `${d}d ${h}h`
  if (h > 0) return `${h}h ${m}m`
  return totalMin <= 1 ? '< 1m' : `${m}m`
}

/** How often live views re-render: often enough for the plane to glide, rare enough to stay calm. */
export const LIVE_TICK_MS = 15_000

export function flightProgress(sentAt: string, deliverAt: string, now: number) {
  const start = new Date(sentAt).getTime()
  const end = new Date(deliverAt).getTime()
  if (end <= start) return 1
  return Math.min(1, Math.max(0, (now - start) / (end - start)))
}
