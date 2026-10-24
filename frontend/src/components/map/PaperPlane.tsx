/**
 * The KREN logo's paper plane: sharp nose on the right, wings swept back, so it reads clearly when
 * rotated to the flight heading (0° = flying right).
 */
export function PaperPlane({ size = 40 }: { size?: number }) {
  const s = size / 40
  return (
    <g transform={`scale(${s}) translate(-24 -13)`}>
      {/* upper wing: long sharp nose, blunt trailing edge so the heading reads at a glance */}
      <path d="M48 13 L4 1 L10 7 L14 13 Z" fill="#c6e5f3" stroke="#9cc7e2" strokeWidth={0.6} strokeLinejoin="round" />
      {/* lower wing */}
      <path d="M48 13 L14 13 L10 20 L8 25 Z" fill="#5596e6" />
      {/* keel fold */}
      <path d="M48 13 L14 13 L16 19 Z" fill="#3d70b2" />
    </g>
  )
}
