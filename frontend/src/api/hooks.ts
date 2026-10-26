import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from './client'
import type {
  BlockedUser,
  CheckinResult,
  ConversationSummary,
  ConversationThread,
  Correction,
  CorrectionLine,
  Dashboard,
  Interest,
  LedgerEntry,
  Letter,
  MyProfile,
  Page,
  PublicProfile,
} from './types'

export const keys = {
  me: ['me'] as const,
  interests: ['interests'] as const,
  penpals: (interestIds: number[], page: number) => ['penpals', interestIds, page] as const,
  user: (id: number) => ['user', id] as const,
  letter: (id: number) => ['letter', id] as const,
  ledger: ['ledger'] as const,
  correction: (letterId: number) => ['correction', letterId] as const,
  notebook: (page: number) => ['notebook', page] as const,
  blocks: ['blocks'] as const,
}

/** null = not logged in. */
export function useMe() {
  return useQuery({
    queryKey: keys.me,
    queryFn: async () => {
      try {
        return await api<MyProfile>('/api/users/me')
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) return null
        throw e
      }
    },
    staleTime: 30_000,
  })
}

export function useInterests() {
  return useQuery({
    queryKey: keys.interests,
    queryFn: () => api<Interest[]>('/api/interests'),
    staleTime: Infinity,
  })
}

export function usePenpals(interestIds: number[], page: number) {
  const params = new URLSearchParams({ page: String(page), size: '12' })
  interestIds.forEach((id) => params.append('interestId', String(id)))
  return useQuery({
    queryKey: keys.penpals(interestIds, page),
    queryFn: () => api<Page<PublicProfile>>(`/api/penpals?${params}`),
  })
}

export function useUser(id: number) {
  return useQuery({ queryKey: keys.user(id), queryFn: () => api<PublicProfile>(`/api/users/${id}`), retry: false })
}

/** Under the 'letters' key, like everything mailbox-shaped, so SSE events and sends refresh it. */
export function useConversations(page: number) {
  return useQuery({
    queryKey: ['letters', 'conversations', page],
    queryFn: () => api<Page<ConversationSummary>>(`/api/conversations?page=${page}&size=20`),
  })
}

/** Letters with one penpal. Page 0 is the newest; each further page reaches further back. */
export function useThread(userId: number) {
  return useInfiniteQuery({
    queryKey: ['letters', 'thread', userId],
    queryFn: ({ pageParam }) =>
      api<ConversationThread>(`/api/conversations/${userId}?size=15${pageParam ? `&before=${pageParam}` : ''}`),
    initialPageParam: null as number | null,
    getNextPageParam: (page) => (page.hasEarlier ? page.letters[0].id : null),
    retry: false,
  })
}

export function useLetter(id: number, enabled = true) {
  const qc = useQueryClient()
  return useQuery({
    enabled,
    queryKey: keys.letter(id),
    queryFn: async () => {
      const letter = await api<Letter>(`/api/letters/${id}`)
      // Opening it may have just set the read receipt.
      qc.invalidateQueries({ queryKey: ['letters'] })
      return letter
    },
    retry: false,
  })
}

/** Under the 'letters' key so SSE takeoff/arrival events refresh it along with the mailboxes. */
export function useDashboard() {
  return useQuery({
    queryKey: ['letters', 'dashboard'],
    queryFn: () => api<Dashboard>('/api/letters/dashboard'),
    refetchInterval: 60_000,
  })
}

export function useCorrection(letterId: number, enabled: boolean) {
  return useQuery({
    queryKey: keys.correction(letterId),
    queryFn: () => api<Correction>(`/api/letters/${letterId}/correction`),
    enabled,
    retry: false,
  })
}

export function useSubmitCorrection(letterId: number) {
  const qc = useQueryClient()
  const refreshStamps = useRefreshStamps()
  return useMutation({
    mutationFn: (v: { lines: CorrectionLine[]; comment: string }) =>
      api<Correction>(`/api/letters/${letterId}/correction`, 'POST', v),
    onSuccess: (c) => {
      qc.setQueryData(keys.correction(letterId), c)
      qc.invalidateQueries({ queryKey: keys.letter(letterId) })
      refreshStamps()
    },
  })
}

export function useNotebook(page: number) {
  return useQuery({
    queryKey: keys.notebook(page),
    queryFn: () => api<Page<Correction>>(`/api/corrections/notebook?page=${page}&size=10`),
  })
}

export function useLedger() {
  return useQuery({ queryKey: keys.ledger, queryFn: () => api<Page<LedgerEntry>>('/api/stamps/ledger?size=10') })
}

export function useBlocks() {
  return useQuery({ queryKey: keys.blocks, queryFn: () => api<Page<BlockedUser>>('/api/blocks?size=50') })
}

/** After anything that moves stamps, refresh the balance and history. */
function useRefreshStamps() {
  const qc = useQueryClient()
  return () => {
    qc.invalidateQueries({ queryKey: keys.me })
    qc.invalidateQueries({ queryKey: keys.ledger })
  }
}

export function useSendLetter() {
  const qc = useQueryClient()
  const refreshStamps = useRefreshStamps()
  return useMutation({
    mutationFn: (v: { recipientId: number; body: string } | { replyTo: number; body: string }) =>
      'replyTo' in v
        ? api<Letter>(`/api/letters/${v.replyTo}/reply`, 'POST', { body: v.body })
        : api<Letter>('/api/letters', 'POST', v),
    onSuccess: () => {
      refreshStamps()
      qc.invalidateQueries({ queryKey: ['letters'] })
    },
  })
}

export function useCheckIn() {
  const refreshStamps = useRefreshStamps()
  return useMutation({
    mutationFn: () => api<CheckinResult>('/api/stamps/checkin', 'POST'),
    onSettled: refreshStamps,
  })
}

export function useBlock() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, block }: { userId: number; block: boolean }) =>
      api<void>(`/api/blocks/${userId}`, block ? 'PUT' : 'DELETE'),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.blocks })
      qc.invalidateQueries({ queryKey: ['penpals'] })
      qc.invalidateQueries({ queryKey: ['user'] })
    },
  })
}

export interface ProfileInput {
  displayName: string
  country: string
  nativeLang: string
  learningLang: string
  bio: string
  interestIds: number[]
  avatar?: string | null
}

export function useAvatars() {
  return useQuery({ queryKey: ['avatars'], queryFn: () => api<string[]>('/api/avatars'), staleTime: Infinity })
}

export interface RandomQuota {
  limit: number
  used: number
  remaining: number
}

export function useRandomQuota() {
  return useQuery({ queryKey: ['random-quota'], queryFn: () => api<RandomQuota>('/api/letters/random/quota') })
}

/** "Send to a random penpal": the server picks the recipient; the response says who. */
export function useSendRandom() {
  const qc = useQueryClient()
  const refreshStamps = useRefreshStamps()
  return useMutation({
    mutationFn: (body: string) => api<Letter>('/api/letters/random', 'POST', { body }),
    onSettled: () => qc.invalidateQueries({ queryKey: ['random-quota'] }),
    onSuccess: () => {
      refreshStamps()
      qc.invalidateQueries({ queryKey: ['letters'] })
    },
  })
}

export function useUpdateProfile() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (input: ProfileInput) => api<MyProfile>('/api/users/me', 'PUT', input),
    onSuccess: (me) => {
      qc.setQueryData(keys.me, me)
      qc.invalidateQueries({ queryKey: ['penpals'] })
    },
  })
}

export function useLogin() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { email: string; password: string }) => api<MyProfile>('/api/auth/login', 'POST', v),
    onSuccess: (me) => qc.setQueryData(keys.me, me),
  })
}

export interface SignupInput {
  email: string
  password: string
  displayName: string
  country: string
  nativeLang: string
  learningLang: string
}

export function useSignup() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: SignupInput) => api<MyProfile>('/api/auth/signup', 'POST', v),
    onSuccess: (me) => qc.setQueryData(keys.me, me),
  })
}

export function useLogout() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => api<void>('/api/auth/logout', 'POST'),
    onSettled: () => {
      qc.clear()
      qc.setQueryData(keys.me, null)
    },
  })
}
