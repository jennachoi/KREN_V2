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

/** "1d 2h", "3h 12m", "4m 05s", "arriving…" */
export function formatRemaining(ms: number) {
  if (ms <= 0) return 'arriving…'
  const s = Math.floor(ms / 1000)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (d > 0) return `${d}d ${h}h`
  if (h > 0) return `${h}h ${m}m`
  return `${m}m ${String(s % 60).padStart(2, '0')}s`
}

export function flightProgress(sentAt: string, deliverAt: string, now: number) {
  const start = new Date(sentAt).getTime()
  const end = new Date(deliverAt).getTime()
  if (end <= start) return 1
  return Math.min(1, Math.max(0, (now - start) / (end - start)))
}
