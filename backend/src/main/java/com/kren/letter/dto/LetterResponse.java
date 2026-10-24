package com.kren.letter.dto;

import java.time.Instant;

import com.kren.letter.Letter;
import com.kren.user.dto.UserSummary;

public record LetterResponse(Long id, UserSummary sender, UserSummary recipient, Long parentId, String body,
		Instant sentAt, Instant readAt) {

	public static LetterResponse from(Letter letter) {
		return new LetterResponse(letter.getId(), UserSummary.from(letter.getSender()),
				UserSummary.from(letter.getRecipient()), letter.getParent() == null ? null : letter.getParent().getId(),
				letter.getBody(), letter.getSentAt(), letter.getReadAt());
	}

}
