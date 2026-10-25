package com.kren.letter.dto;

import java.time.Instant;

import com.kren.letter.CountryLocations;
import com.kren.letter.Letter;
import com.kren.user.dto.UserSummary;

/** A letter in the air, from the viewer's point of view: coming to me, or going from me. */
public record Flight(Long id, Direction direction, UserSummary counterpart, Instant sentAt, Instant deliverAt,
		Route route) {

	public enum Direction {

		INCOMING, OUTGOING

	}

	public static Flight from(Letter letter, long viewerId, CountryLocations locations) {
		Direction direction = letter.isRecipient(viewerId) ? Direction.INCOMING : Direction.OUTGOING;
		return new Flight(letter.getId(), direction, UserSummary.from(letter.counterpartOf(viewerId)),
				letter.getSentAt(), letter.getDeliverAt(), Route.of(letter, locations));
	}

}
