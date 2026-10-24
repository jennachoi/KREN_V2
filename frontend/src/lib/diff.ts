export type DiffPart = { kind: 'same' | 'removed' | 'added'; text: string }

const MAX_CELLS = 400_000

/**
 * Character-level diff (longest common subsequence) for showing corrections inline, v1-style:
 * removed text struck through, added text highlighted. Characters rather than words because Korean
 * and Japanese corrections often change a particle inside a "word".
 */
export function diffChars(before: string, after: string): DiffPart[] {
  const a = [...before]
  const b = [...after]
  if (a.length * b.length > MAX_CELLS) {
    return [
      { kind: 'removed', text: before },
      { kind: 'added', text: after },
    ]
  }
  const dp: Uint16Array[] = Array.from({ length: a.length + 1 }, () => new Uint16Array(b.length + 1))
  for (let i = a.length - 1; i >= 0; i--) {
    for (let j = b.length - 1; j >= 0; j--) {
      dp[i][j] = a[i] === b[j] ? dp[i + 1][j + 1] + 1 : Math.max(dp[i + 1][j], dp[i][j + 1])
    }
  }
  const parts: DiffPart[] = []
  const push = (kind: DiffPart['kind'], ch: string) => {
    const last = parts[parts.length - 1]
    if (last && last.kind === kind) last.text += ch
    else parts.push({ kind, text: ch })
  }
  let i = 0
  let j = 0
  while (i < a.length && j < b.length) {
    if (a[i] === b[j]) {
      push('same', a[i])
      i++
      j++
    } else if (dp[i + 1][j] >= dp[i][j + 1]) {
      push('removed', a[i++])
    } else {
      push('added', b[j++])
    }
  }
  while (i < a.length) push('removed', a[i++])
  while (j < b.length) push('added', b[j++])
  return parts
}

/** Splits a letter into sentences (keeping their punctuation) so each can be corrected separately. */
export function splitSentences(body: string): string[] {
  return body
    .split(/\n+/)
    .flatMap((para) => para.match(/[^.!?。！？]+[.!?。！？]+["')\]]*|[^.!?。！？]+$/g) ?? [])
    .map((s) => s.trim())
    .filter(Boolean)
}
