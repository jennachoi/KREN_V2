package com.kren.letter.dto;

import java.util.List;

import com.kren.user.dto.UserSummary;

/**
 * Letters between me and one penpal, oldest first. {@code hasEarlier}: more letters before the first
 * one; ask again with {@code before} = its id.
 */
public record ConversationThread(UserSummary penpal, List<LetterResponse> letters, boolean hasEarlier) {
}
