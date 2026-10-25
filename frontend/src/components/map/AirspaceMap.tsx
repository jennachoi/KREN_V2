import { useMemo } from 'react'
import { useNavigate } from 'react-router'
import { geoGraticule10, geoInterpolate, geoNaturalEarth1, geoPath } from 'd3-geo'
import type { Feature, LineString, MultiPoint } from 'geojson'
import type { Flight, Place } from '../../api/types'
import { countryName } from '../../lib/locale'
import { flightProgress, useNow } from '../../lib/time'
import { BORDERS, LAND, type LngLat } from './geo'
import { PaperPlane } from './PaperPlane'

const W = 800
const H = 380
const SAMPLES = 64

const COLOR = { INCOMING: '#f2786d', OUTGOING: '#3d70b2' } as const

const known = (p: Place): p is Place & { lat: number; lng: number } => p.lat != null && p.lng != null

/**
 * Every letter of mine that's in the air, on one map: incoming routes in coral, outgoing in blue,
 * each with a paper plane at its live position. Clicking a plane opens that letter's tracker.
 */
export function AirspaceMap({ home, flights }: { home: Place; flights: Flight[] }) {
  const navigate = useNavigate()
  const now = useNow(1000)

  const geo = useMemo(() => {
    const routes = flights
      .filter((f) => known(f.route.from) && known(f.route.to))
      .map((f) => {
        const a: LngLat = [f.route.from.lng!, f.route.from.lat!]
        const b: LngLat = [f.route.to.lng!, f.route.to.lat!]
        const interpolate = geoInterpolate(a, b)
        const arc = Array.from({ length: SAMPLES + 1 }, (_, i) => interpolate(i / SAMPLES) as LngLat)
        return { flight: f, a, b, interpolate, arc }
      })
    const homePoint: LngLat | null = known(home) ? [home.lng, home.lat] : null
    const projection = geoNaturalEarth1().rotate([-(homePoint?.[0] ?? 0), 0])
    const points: LngLat[] = [...(homePoint ? [homePoint] : []), ...routes.flatMap((r) => r.arc)]
    if (routes.length > 0) {
      const all: Feature<MultiPoint> = { type: 'Feature', properties: {}, geometry: { type: 'MultiPoint', coordinates: points } }
      projection.fitExtent(
        [
          [60, 50],
          [W - 60, H - 40],
        ],
        all,
      )
    } else {
      // Nothing in the air: the whole world, centred on home.
      projection.fitSize([W, H], { type: 'Sphere' })
    }
    return { routes, homePoint, projection, path: geoPath(projection) }
  }, [flights, home])

  const { routes, homePoint, projection, path } = geo
  const homeXY = homePoint ? projection(homePoint) : null

  // Live plane positions. Planes close together (e.g. two letters that just took off from home)
  // get their name tags stacked instead of drawn on top of each other.
  const planes: { flight: Flight; here: [number, number]; heading: number; labelDy: number }[] = []
  for (const { flight, interpolate } of routes) {
    const p = flightProgress(flight.sentAt, flight.deliverAt, now)
    const here = (projection(interpolate(p) as LngLat) ?? [0, 0]) as [number, number]
    const ahead = projection(interpolate(Math.min(1, p + 0.01)) as LngLat) ?? here
    const behind = projection(interpolate(Math.max(0, p - 0.01)) as LngLat) ?? here
    const heading = (Math.atan2(ahead[1] - behind[1], ahead[0] - behind[0]) * 180) / Math.PI
    const crowd = planes.filter((o) => Math.hypot(o.here[0] - here[0], o.here[1] - here[1]) < 40).length
    planes.push({ flight, here, heading, labelDy: crowd * 21 })
  }

  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="block w-full rounded-[6px]" role="img" aria-label="Your letters in the air">
      <defs>
        <radialGradient id="air-sea" cx="50%" cy="45%" r="75%">
          <stop offset="0%" stopColor="#d3edfc" />
          <stop offset="100%" stopColor="#a2cfeb" />
        </radialGradient>
        <filter id="air-lift" x="-50%" y="-50%" width="200%" height="200%">
          <feDropShadow dx="0" dy="4" stdDeviation="3" floodColor="#1d3d63" floodOpacity="0.25" />
        </filter>
      </defs>
      <rect width={W} height={H} fill="url(#air-sea)" />
      <path d={path(geoGraticule10()) ?? ''} fill="none" stroke="#fff" strokeOpacity={0.35} strokeWidth={0.6} />
      <path d={path(LAND) ?? ''} fill="#cfe5b4" stroke="#b2cf93" strokeWidth={0.6} />
      <path d={path(BORDERS) ?? ''} fill="none" stroke="#fff" strokeOpacity={0.8} strokeWidth={0.6} />

      {routes.map(({ flight, interpolate, arc }) => {
        const p = flightProgress(flight.sentAt, flight.deliverAt, now)
        const whole: Feature<LineString> = { type: 'Feature', properties: {}, geometry: { type: 'LineString', coordinates: arc } }
        const flown: Feature<LineString> = {
          type: 'Feature',
          properties: {},
          geometry: { type: 'LineString', coordinates: [...arc.slice(0, Math.round(p * SAMPLES) + 1), interpolate(p) as LngLat] },
        }
        return (
          <g key={`route-${flight.id}`}>
            <path d={path(whole) ?? ''} fill="none" stroke={COLOR[flight.direction]} strokeWidth={2.2} strokeDasharray="2 7" strokeLinecap="round" opacity={0.7} />
            <path d={path(flown) ?? ''} fill="none" stroke={COLOR[flight.direction]} strokeWidth={2.6} strokeLinecap="round" />
          </g>
        )
      })}

      {homeXY && (
        <g transform={`translate(${homeXY[0]} ${homeXY[1]})`}>
          <circle r={9} fill="#3d70b2" opacity={0.18} />
          <circle r={4.5} fill="#fff" stroke="#3d70b2" strokeWidth={2.5} />
        </g>
      )}

      {planes.map(({ flight, here, heading, labelDy }) => {
        const label = flight.counterpart.displayName
        const w = label.length * 7 + 16
        return (
          <g
            key={`plane-${flight.id}`}
            className="cursor-pointer"
            onClick={() => navigate(`/letters/${flight.id}`)}
            role="link"
            aria-label={`${flight.direction === 'INCOMING' ? 'From' : 'To'} ${label}, ${countryName(
              flight.direction === 'INCOMING' ? flight.route.from.country : flight.route.to.country,
            )}`}
          >
            <g transform={`translate(${here[0]} ${here[1]}) rotate(${heading})`} filter="url(#air-lift)">
              <PaperPlane size={34} />
            </g>
            <g transform={`translate(${here[0] - w / 2} ${here[1] + 16 + labelDy})`}>
              <rect width={w} height={18} rx={9} fill={COLOR[flight.direction]} />
              <text x={w / 2} y={12.5} textAnchor="middle" fontSize={10.5} fontFamily="Montserrat, sans-serif" fill="#fff">
                {label}
              </text>
            </g>
          </g>
        )
      })}
    </svg>
  )
}
