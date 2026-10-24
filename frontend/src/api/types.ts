// Mirrors the backend DTO records (com.kren.*.dto).

export interface Interest {
  id: number
  name: string
}

export interface MyProfile {
  id: number
  email: string
  displayName: string
  country: string
  nativeLang: string
  learningLang: string
  bio: string | null
  interests: Interest[]
  stampBalance: number
  createdAt: string
}

export interface PublicProfile {
  id: number
  displayName: string
  country: string
  nativeLang: string
  learningLang: string
  bio: string | null
  interests: Interest[]
  createdAt: string
}

export interface UserSummary {
  id: number
  displayName: string
  country: string
  nativeLang: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Place {
  country: string
  lat: number | null
  lng: number | null
}

export interface Route {
  from: Place
  to: Place
  distanceKm: number | null
}

export interface Letter {
  id: number
  sender: UserSummary
  recipient: UserSummary
  parentId: number | null
  /** null while the letter is still flying to you */
  body: string | null
  sentAt: string
  deliverAt: string
  delivered: boolean
  readAt: string | null
  route: Route
  corrected: boolean
}

export interface LetterSummary {
  id: number
  counterpart: UserSummary
  /** null while the letter is still flying to you */
  preview: string | null
  sentAt: string
  deliverAt: string
  delivered: boolean
  readAt: string | null
}

export interface CorrectionLine {
  original: string
  corrected: string
  note: string | null
}

export interface Correction {
  id: number
  letterId: number
  corrector: UserSummary
  comment: string | null
  createdAt: string
  lines: CorrectionLine[]
}

export interface LedgerEntry {
  id: number
  amount: number
  reason: 'SIGNUP' | 'CHECKIN' | 'SEND_LETTER' | 'CORRECTION'
  letterId: number | null
  createdAt: string
}

export interface CheckinResult {
  date: string
  reward: number
  balance: number
}

export interface BlockedUser {
  user: UserSummary
  blockedAt: string
}
