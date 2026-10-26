import { useState } from 'react'
import { SpellCheck } from 'lucide-react'
import { useSubmitCorrection } from '../api/hooks'
import type { Correction, Letter } from '../api/types'
import { diffSides, splitSentences } from '../lib/diff'
import { letterDate } from '../lib/locale'
import { Avatar, Button, Card, ErrorText } from './ui'

/**
 * One corrected sentence as two lines: the original with removed words in red, then the
 * correction with added words in blue.
 */
export function CorrectionDiff({ original, corrected, note }: { original: string; corrected: string; note: string | null }) {
  const { before, after } = diffSides(original, corrected)
  return (
    <div className="space-y-1 py-3 text-[14px] leading-relaxed">
      <p className="flex gap-3">
        <span className="w-12 shrink-0 pt-0.5 text-[11px] uppercase tracking-wide text-muted">Before</span>
        <span>
          {before.map((part, i) =>
            part.kind === 'same' ? (
              <span key={i}>{part.text}</span>
            ) : (
              <span key={i} className="rounded-[2px] bg-coral/10 text-coral line-through decoration-coral/60">
                {part.text}
              </span>
            ),
          )}
        </span>
      </p>
      <p className="flex gap-3">
        <span className="w-12 shrink-0 pt-0.5 text-[11px] uppercase tracking-wide text-primary">After</span>
        <span>
          {after.map((part, i) =>
            part.kind === 'same' ? (
              <span key={i}>{part.text}</span>
            ) : (
              <span key={i} className="rounded-[2px] bg-primary/10 font-medium text-primary">
                {part.text}
              </span>
            ),
          )}
        </span>
      </p>
      {note && <p className="pl-[60px] text-[12px] text-muted">💬 {note}</p>}
    </div>
  )
}

/** The correction a letter received (seen by both the writer and the corrector). */
export function CorrectionView({ correction }: { correction: Correction }) {
  return (
    <Card className="px-6 py-5">
      <div className="mb-2 flex items-center gap-3">
        <Avatar name={correction.corrector.displayName} country={correction.corrector.country} avatar={correction.corrector.avatar} size={34} />
        <div>
          <p className="text-[13px] text-ink">
            <b className="font-medium">{correction.corrector.displayName}</b> corrected this letter
          </p>
          <p className="text-[11px] text-muted">{letterDate(correction.createdAt)}</p>
        </div>
        <SpellCheck size={18} className="ml-auto text-primary" />
      </div>
      <div className="divide-y divide-line">
        {correction.lines.map((l, i) => (
          <CorrectionDiff key={i} {...l} />
        ))}
      </div>
      {correction.comment && (
        <p className="mt-3 rounded-[4px] bg-canvas px-3 py-2 text-[13px] text-ink">“{correction.comment}”</p>
      )}
    </Card>
  )
}

/**
 * v1's letter correction screen: each sentence of the letter on the left, an editable copy on the
 * right. Only sentences that were changed are sent. Correcting earns a stamp.
 */
export function CorrectionEditor({ letter, onDone, onCancel }: { letter: Letter; onDone: () => void; onCancel: () => void }) {
  const sentences = splitSentences(letter.body ?? '')
  const [edits, setEdits] = useState<string[]>(sentences)
  const [notes, setNotes] = useState<string[]>(sentences.map(() => ''))
  const [comment, setComment] = useState('')
  const submit = useSubmitCorrection(letter.id)

  const changed = sentences
    .map((original, i) => ({ original, corrected: edits[i].trim(), note: notes[i].trim() || null }))
    .filter((l) => l.corrected && l.corrected !== l.original)

  return (
    <Card className="px-6 py-5">
      <div className="mb-1 flex items-center gap-2">
        <SpellCheck size={18} className="text-primary" />
        <h3 className="font-display text-[15px] font-medium">Correct {letter.sender.displayName}'s letter</h3>
      </div>
      <p className="mb-4 text-[12px] text-muted">
        Edit the sentences that could sound more natural. Leave the rest as they are. You'll earn 1 stamp.
      </p>
      <div className="divide-y divide-line border-y border-line">
        {sentences.map((s, i) => (
          <div key={i} className="grid gap-3 py-3 md:grid-cols-2">
            <p className="text-[13px] leading-relaxed text-[#555]">{s}</p>
            <div className="space-y-1.5">
              <textarea
                rows={2}
                value={edits[i]}
                onChange={(e) => setEdits(edits.map((v, j) => (j === i ? e.target.value : v)))}
                className={`w-full resize-y rounded-[3px] border px-2.5 py-1.5 text-[13px] leading-relaxed outline-none focus:border-primary ${
                  edits[i].trim() !== s ? 'border-primary bg-primary/5' : 'border-line'
                }`}
              />
              {edits[i].trim() !== s && (
                <input
                  value={notes[i]}
                  maxLength={300}
                  onChange={(e) => setNotes(notes.map((v, j) => (j === i ? e.target.value : v)))}
                  placeholder="Why? (optional)"
                  className="w-full rounded-[3px] border border-line px-2.5 py-1 text-[12px] outline-none focus:border-primary"
                />
              )}
            </div>
          </div>
        ))}
      </div>
      <textarea
        rows={2}
        maxLength={1000}
        value={comment}
        onChange={(e) => setComment(e.target.value)}
        placeholder="A note for the writer (optional) — e.g. “Your Korean is really good!”"
        className="mt-4 w-full resize-none rounded-[3px] border border-line px-3 py-2 text-[13px] outline-none focus:border-primary"
      />
      <div className="mt-3 flex flex-wrap items-center gap-2">
        <Button
          disabled={changed.length === 0 || submit.isPending}
          onClick={() => submit.mutate({ lines: changed, comment }, { onSuccess: onDone })}
        >
          <SpellCheck size={14} /> Send correction (+1 stamp)
        </Button>
        <Button variant="white" onClick={onCancel}>
          Cancel
        </Button>
        <span className="text-[12px] text-muted">
          {changed.length} sentence{changed.length === 1 ? '' : 's'} changed
        </span>
      </div>
      <div className="mt-2">
        <ErrorText error={submit.error} />
      </div>
    </Card>
  )
}
