export interface FieldError {
  field: string
  message: string
}

/** Every backend error is a ProblemDetail with a machine-readable `code` (see GlobalExceptionHandler). */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: FieldError[]

  constructor(status: number, code: string, message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }

  fieldError(field: string): string | undefined {
    return this.fieldErrors.find((e) => e.field === field)?.message
  }
}

/** Spring Security's double-submit CSRF: echo the XSRF-TOKEN cookie back as a header. */
function csrfToken(): string | undefined {
  const cookie = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='))
  return cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : undefined
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

export async function api<T>(path: string, method: Method = 'GET', body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (method !== 'GET') {
    const token = csrfToken()
    if (token) headers['X-XSRF-TOKEN'] = token
  }

  const res = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    credentials: 'same-origin',
  })

  if (res.status === 204) return undefined as T
  const data = await res.json().catch(() => null)
  if (!res.ok) {
    throw new ApiError(res.status, data?.code ?? 'UNKNOWN', data?.detail ?? res.statusText, data?.errors ?? [])
  }
  return data as T
}
