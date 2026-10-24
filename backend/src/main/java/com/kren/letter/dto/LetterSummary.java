package com.kren.letter.dto;

import java.time.Instant;

import com.kren.letter.Letter;
import com.kren.user.dto.UserSummary;

/** Inbox / sent-box row: the other party plus a preview, not the full body. */
public record LetterSummary(Long id, UserSummary counterpart, String preview, Instant sentAt, Instant readAt) {

	private static final int PREVIEW_LENGTH = 100;

	public static LetterSummary from(Letter letter, long viewerId) {
		String body = letter.getBody();
		String preview = body.length() <= PREVIEW_LENGTH ? body : body.substring(0, PREVIEW_LENGTH) + "…";
		return new LetterSummary(letter.getId(), UserSummary.from(letter.counterpartOf(viewerId)), preview,
				letter.getSentAt(), letter.getReadAt());
	}

}
