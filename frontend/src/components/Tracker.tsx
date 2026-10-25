import { useEffect, useRef, type ReactNode } from 'react'
import { Clock3, Navigation, Stamp } from 'lucide-react'
import type { Letter } from '../api/types'
import { countryName, letterDate } from '../lib/locale'
import { flightProgress, formatRemaining, LIVE_TICK_MS, useNow } from '../lib/time'
import { FlightMap } from './map/FlightMap'
import { PaperPlane } from './map/PaperPlane'

/**
 * Live delivery tracker: the map with the plane moving along the route, and a status card with the
 * progress bar, time left and distance. Calls `onArrive` once the ETA passes so the page can refetch
 * the (now unsealed) letter.
 */
export function Tracker({ letter, mine, onArrive }: { letter: Letter; mine: boolean; onArrive?: () => void }) {
  const now = useNow(LIVE_TICK_MS)
  const progress = flightProgress(letter.sentAt, letter.deliverAt, now)
  const remaining = new Date(letter.deliverAt).getTime() - now
  const arrivedRef = useRef(false)

  useEffect(() => {
    if (remaining <= 0 && !arrivedRef.current) {
      arrivedRef.current = true
      onArrive?.()
    }
  }, [remaining, onArrive])

  const { from, to, distanceKm } = letter.route
  return (
    <div className="relative">
      <FlightMap route={letter.route} progress={progress} />
      <div className="relative mx-3 -mt-16 rounded-[10px] border border-white/70 bg-white/95 p-5 shadow-lg backdrop-blur sm:mx-8">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-full bg-[#e8f1fc]">
            <svg viewBox="-24 -16 48 32" className="h-6 w-8">
              <PaperPlane size={34} />
            </svg>
          </span>
          <div>
            <p className="font-display text-[16px] font-semibold text-ink">
              {mine ? 'Your letter is on its way!' : `A letter from ${letter.sender.displayName} is on its way!`}
            </p>
            <p className="text-[12px] text-muted">
              From {countryName(from.country)} to {countryName(to.country)}
            </p>
          </div>
        </div>
        <div className="mt-4 h-2 overflow-hidden rounded-full bg-[#e6e9ef]">
          <div className="h-full rounded-full bg-primary transition-[width] duration-[15000ms] ease-linear" style={{ width: `${progress * 100}%` }} />
        </div>
        <div className="mt-4 grid grid-cols-3 gap-2 text-[13px]">
          <Stat icon={<Clock3 size={16} />} value={formatRemaining(remaining)} label="Est. delivery time" />
          <Stat
            icon={<Navigation size={16} />}
            value={distanceKm == null ? '—' : `${distanceKm.toLocaleString()} km`}
            label="Total distance"
          />
          <Stat icon={<Stamp size={16} />} value={letterDate(letter.deliverAt)} label={mine ? 'Arrives' : 'Opens'} />
        </div>
        {!mine && <p className="mt-4 text-center text-[12px] text-muted">The letter stays sealed until it lands. Please wait a little.</p>}
      </div>
    </div>
  )
}

function Stat({ icon, value, label }: { icon: ReactNode; value: string; label: string }) {
  return (
    <div className="flex items-start gap-2">
      <span className="mt-0.5 text-soft">{icon}</span>
      <span>
        <span className="block font-medium text-ink">{value}</span>
        <span className="block text-[11px] text-muted">{label}</span>
      </span>
    </div>
  )
}
