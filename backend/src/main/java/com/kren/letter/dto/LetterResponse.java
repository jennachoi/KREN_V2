package com.kren.letter.dto;

import java.time.Instant;

import com.kren.letter.CountryLocations;
import com.kren.letter.Letter;
import com.kren.user.dto.UserSummary;

/**
 * {@code body} is null while the letter is still flying to the viewer (sealed); the sender always
 * sees it. {@code delivered} is evaluated at response time. {@code corrected}: the recipient has
 * marked it up (fetch it from {@code /api/letters/{id}/correction}).
 */
public record LetterResponse(Long id, UserSummary sender, UserSummary recipient, Long parentId, String body,
		Instant sentAt, Instant deliverAt, boolean delivered, Instant readAt, Route route, boolean corrected) {

	public static LetterResponse from(Letter letter, long viewerId, Instant now, CountryLocations locations,
			boolean corrected) {
		return new LetterResponse(letter.getId(), UserSummary.from(letter.getSender()),
				UserSummary.from(letter.getRecipient()), letter.getParent() == null ? null : letter.getParent().getId(),
				letter.isSealedFor(viewerId, now) ? null : letter.getBody(), letter.getSentAt(), letter.getDeliverAt(),
				letter.isDeliveredAt(now), letter.getReadAt(), Route.of(letter, locations), corrected);
	}

}
