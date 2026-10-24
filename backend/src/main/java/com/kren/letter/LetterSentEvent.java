package com.kren.letter;

import com.kren.letter.dto.LetterSummary;

/**
 * A letter has taken off towards {@code recipientId} (still sealed for them). Published inside the
 * send transaction, delivered to listeners only after it commits. Carries plain data (no entities)
 * because listeners run after the persistence context is gone.
 */
public record LetterSentEvent(long recipientId, LetterSummary letter) {
}
