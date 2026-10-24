package com.kren.letter.dto;

import java.time.Instant;

import com.kren.letter.Letter;
import com.kren.user.dto.UserSummary;

/** Mailbox row: the other party plus a preview, which is null while the letter is still on its way to you. */
public record LetterSummary(Long id, UserSummary counterpart, String preview, Instant sentAt, Instant deliverAt,
		boolean delivered, Instant readAt) {

	private static final int PREVIEW_LENGTH = 100;

	public static LetterSummary from(Letter letter, long viewerId, Instant now) {
		String preview = null;
		if (!letter.isSealedFor(viewerId, now)) {
			String body = letter.getBody();
			preview = body.length() <= PREVIEW_LENGTH ? body : body.substring(0, PREVIEW_LENGTH) + "…";
		}
		return new LetterSummary(letter.getId(), UserSummary.from(letter.counterpartOf(viewerId)), preview,
				letter.getSentAt(), letter.getDeliverAt(), letter.isDeliveredAt(now), letter.getReadAt());
	}

}
