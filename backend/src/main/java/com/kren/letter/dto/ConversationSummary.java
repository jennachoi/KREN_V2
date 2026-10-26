package com.kren.letter.dto;

import com.kren.user.dto.UserSummary;

/**
 * A row in the conversation list. {@code lastFromMe}: who wrote the newest letter. {@code unread}:
 * arrived but not opened. {@code awaitingReply}: arrived and not answered yet. {@code inFlight}:
 * letters between us still in the air (either direction).
 */
public record ConversationSummary(UserSummary penpal, LetterSummary lastLetter, boolean lastFromMe, long unread,
		long awaitingReply, long inFlight) {
}
