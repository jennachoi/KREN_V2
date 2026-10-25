import { useMemo } from 'react'
import { geoDistance, geoInterpolate, geoNaturalEarth1, geoPath, geoGraticule10 } from 'd3-geo'
import type { Feature, LineString } from 'geojson'
import type { Route } from '../../api/types'
import { countryName } from '../../lib/locale'
import { BORDERS, LAND, type LngLat } from './geo'
import { PaperPlane } from './PaperPlane'

const W = 800
const H = 420
const SAMPLES = 96

/**
 * World map with the letter's great-circle route. The plane sits at `progress` (0..1) along the arc,
 * rotated to its heading; the flown part of the route is solid, the rest dashed. The projection is
 * re-centred and zoomed on the route so a Seoul→Vancouver flight doesn't get cut at the date line.
 */
export function FlightMap({ route, progress }: { route: Route; progress: number }) {
  const geo = useMemo(() => {
    const { from, to } = route
    if (from.lat == null || from.lng == null || to.lat == null || to.lng == null) return null
    const a: LngLat = [from.lng, from.lat]
    const b: LngLat = [to.lng, to.lat]
    const interpolate = geoInterpolate(a, b)
    const arc: LngLat[] = Array.from({ length: SAMPLES + 1 }, (_, i) => interpolate(i / SAMPLES) as LngLat)
    const mid = interpolate(0.5) as LngLat
    const short = geoDistance(a, b) < 0.08 // ~500 km: zoom out a bit so it's not a dot on a blur

    const line: Feature<LineString> = { type: 'Feature', properties: {}, geometry: { type: 'LineString', coordinates: arc } }
    const projection = geoNaturalEarth1().rotate([-mid[0], 0])
    if (short) {
      projection.center([0, mid[1]]).scale(900).translate([W / 2, H / 2])
    } else {
      // Extra room at the bottom: the status card overlaps the lower edge of the map.
      projection.fitExtent(
        [
          [80, 70],
          [W - 80, H - 120],
        ],
        line,
      )
    }
    return { a, b, arc, interpolate, projection, path: geoPath(projection) }
  }, [route])

  if (!geo) {
    return (
      <div className="flex h-56 items-center justify-center rounded-[6px] bg-[#cfe6f7] text-[13px] text-soft">
        {countryName(route.from.country)} → {countryName(route.to.country)}
      </div>
    )
  }

  const { a, b, arc, interpolate, projection, path } = geo
  const p = Math.min(1, Math.max(0, progress))
  const cut = Math.round(p * SAMPLES)
  const flown: Feature<LineString> = {
    type: 'Feature',
    properties: {},
    geometry: { type: 'LineString', coordinates: [...arc.slice(0, cut + 1), interpolate(p) as LngLat] },
  }
  const whole: Feature<LineString> = { type: 'Feature', properties: {}, geometry: { type: 'LineString', coordinates: arc } }

  const here = projection(interpolate(p) as LngLat) ?? [W / 2, H / 2]
  const ahead = projection(interpolate(Math.min(1, p + 0.01)) as LngLat) ?? here
  const behind = projection(interpolate(Math.max(0, p - 0.01)) as LngLat) ?? here
  const heading = (Math.atan2(ahead[1] - behind[1], ahead[0] - behind[0]) * 180) / Math.PI
  const start = projection(a) ?? [0, 0]
  const end = projection(b) ?? [0, 0]
  const sameSpot = route.distanceKm === 0

  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="block w-full rounded-[6px] bg-[#bfe0f5]" role="img"
      aria-label={`Letter route from ${countryName(route.from.country)} to ${countryName(route.to.country)}`}>
      <defs>
        <radialGradient id="sea" cx="50%" cy="45%" r="75%">
          <stop offset="0%" stopColor="#cdeafb" />
          <stop offset="100%" stopColor="#9fcdea" />
        </radialGradient>
        <filter id="lift" x="-50%" y="-50%" width="200%" height="200%">
          <feDropShadow dx="0" dy="6" stdDeviation="4" floodColor="#1d3d63" floodOpacity="0.28" />
        </filter>
      </defs>
      <rect width={W} height={H} fill="url(#sea)" />
      <path d={path(geoGraticule10()) ?? ''} fill="none" stroke="#ffffff" strokeOpacity={0.35} strokeWidth={0.6} />
      <path d={path(LAND) ?? ''} fill="#cfe5b4" stroke="#b2cf93" strokeWidth={0.6} />
      <path d={path(BORDERS) ?? ''} fill="none" stroke="#ffffff" strokeOpacity={0.8} strokeWidth={0.6} />

      {!sameSpot && (
        <>
          <path d={path(whole) ?? ''} fill="none" stroke="#4a4f63" strokeWidth={3} strokeDasharray="2 9" strokeLinecap="round" opacity={0.75} />
          <path d={path(flown) ?? ''} fill="none" stroke="#3d70b2" strokeWidth={3.2} strokeLinecap="round" />
        </>
      )}

      {/* origin label below its pin so the plane, which starts there, doesn't cover it */}
      <Pin x={start[0]} y={start[1]} label={countryName(route.from.country)} below={!sameSpot} />
      {!sameSpot && <Pin x={end[0]} y={end[1]} label={countryName(route.to.country)} dim={p < 1} />}

      <g
        transform={`translate(${here[0]} ${sameSpot ? here[1] - 26 : here[1]}) rotate(${sameSpot ? -20 : heading})`}
        filter="url(#lift)"
        className="transition-transform duration-1000 ease-linear"
      >
        <PaperPlane size={46} />
      </g>
    </svg>
  )
}

export function Pin({ x, y, label, dim = false, below = false }: { x: number; y: number; label: string; dim?: boolean; below?: boolean }) {
  const width = Math.max(64, label.length * 7.4 + 20)
  return (
    <g transform={`translate(${x} ${y})`} opacity={dim ? 0.85 : 1}>
      <circle r={7} fill="#fff" stroke="#4a4f63" strokeWidth={2.5} />
      <circle r={2.5} fill="#4a4f63" />
      <g transform={`translate(${-width / 2} ${below ? 14 : -40})`}>
        <rect width={width} height={24} rx={6} fill="#fff" stroke="#d8dde6" />
        <text x={width / 2} y={16} textAnchor="middle" fontSize={12} fontFamily="Montserrat, sans-serif" fill="#393a4f">
          {label}
        </text>
      </g>
    </g>
  )
}
