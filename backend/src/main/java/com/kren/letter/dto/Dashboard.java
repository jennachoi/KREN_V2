package com.kren.letter.dto;

import java.util.List;

/**
 * Home page summary. {@code home} is where the viewer's letters take off, for centring the map.
 * Lists are capped; the counts are exact.
 */
public record Dashboard(Route.Place home, List<Flight> incoming, List<Flight> outgoing, List<LetterSummary> awaitingReply,
		long awaitingReplyCount) {
}
