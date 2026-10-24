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

export interface Letter {
  id: number
  sender: UserSummary
  recipient: UserSummary
  parentId: number | null
  body: string
  sentAt: string
  readAt: string | null
}

export interface LetterSummary {
  id: number
  counterpart: UserSummary
  preview: string
  sentAt: string
  readAt: string | null
}

export interface LedgerEntry {
  id: number
  amount: number
  reason: 'SIGNUP' | 'CHECKIN' | 'SEND_LETTER'
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
