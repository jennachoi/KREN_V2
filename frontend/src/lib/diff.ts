export type DiffPart = { kind: 'same' | 'removed' | 'added'; text: string }

const MAX_CELLS = 400_000

/**
 * Splits text into diff units: a run of Latin letters/digits (an English word) is one unit, so
 * "famous" → "popular" reads as a word swap rather than a jumble of letters; every other character
 * (Hangul, kana, spaces, punctuation) is its own unit, because Korean corrections often change a
 * single particle inside a word.
 */
function tokenize(text: string): string[] {
  return text.match(/[\p{Script=Latin}\p{N}']+|[\s\S]/gu) ?? []
}

/**
 * Diff (longest common subsequence over {@link tokenize} units) for showing corrections: what was
 * removed from the original and what was added in the correction.
 */
export function diffText(before: string, after: string): DiffPart[] {
  const a = tokenize(before)
  const b = tokenize(after)
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
  const push = (kind: DiffPart['kind'], t: string) => {
    const last = parts[parts.length - 1]
    if (last && last.kind === kind) last.text += t
    else parts.push({ kind, text: t })
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
  return absorbSpaces(parts)
}

/**
 * A lone space kept as "same" between two changes splits one edit in two ("famous for" →
 * "popular with" would show as two islands). Fold such spaces into the surrounding change.
 */
function absorbSpaces(parts: DiffPart[]): DiffPart[] {
  const out: DiffPart[] = []
  for (let k = 0; k < parts.length; k++) {
    const p = parts[k]
    const changedAround = k > 0 && k < parts.length - 1 && parts[k - 1].kind !== 'same' && parts[k + 1].kind !== 'same'
    if (p.kind === 'same' && /^\s+$/.test(p.text) && changedAround) {
      out.push({ kind: 'removed', text: p.text }, { kind: 'added', text: p.text })
    } else {
      out.push({ ...p })
    }
  }
  const merged: DiffPart[] = []
  for (const p of out) {
    const last = merged[merged.length - 1]
    if (last && last.kind === p.kind) last.text += p.text
    else merged.push(p)
  }
  return merged
}

/** The two lines of a correction: the original (unchanged + removed) and the fix (unchanged + added). */
export function diffSides(before: string, after: string): { before: DiffPart[]; after: DiffPart[] } {
  const parts = diffText(before, after)
  const side = (hide: DiffPart['kind']) => {
    const out: DiffPart[] = []
    for (const p of parts) {
      if (p.kind === hide) continue
      const last = out[out.length - 1]
      if (last && last.kind === p.kind) last.text += p.text
      else out.push({ ...p })
    }
    return out
  }
  return { before: side('added'), after: side('removed') }
}

/** Splits a letter into sentences (keeping their punctuation) so each can be corrected separately. */
export function splitSentences(body: string): string[] {
  return body
    .split(/\n+/)
    .flatMap((para) => para.match(/[^.!?。！？]+[.!?。！？]+["')\]]*|[^.!?。！？]+$/g) ?? [])
    .map((s) => s.trim())
    .filter(Boolean)
}
