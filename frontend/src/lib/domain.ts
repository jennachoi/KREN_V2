import type { MyProfile, PublicProfile } from '../api/types'

/** Mirrors the backend's @Size(max = 5000) on letter bodies. */
export const MAX_BODY = 5000

/** How many of their interests you share — v1's "Matched : N". */
export const matchedCount = (me: MyProfile, other: PublicProfile) =>
  other.interests.filter((i) => me.interests.some((m) => m.id === i.id)).length
