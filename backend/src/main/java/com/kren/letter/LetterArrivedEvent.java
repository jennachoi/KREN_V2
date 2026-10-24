package com.kren.letter;

import com.kren.letter.dto.LetterSummary;

/** A letter has landed and is now readable by its recipient. Delivered to listeners after commit. */
public record LetterArrivedEvent(long recipientId, LetterSummary letter) {
}
