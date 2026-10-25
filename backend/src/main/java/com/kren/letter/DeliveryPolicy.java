package com.kren.letter;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * How long a letter flies: a 2-hour handling time plus 1 hour per 250 km, capped at 72 hours
 * (same country 2h, Seoul→Tokyo ~7h, Seoul→London ~37h, Seoul→New York ~46h). Proportional rather
 * than v1's step table, so nearby distances never differ by hours. {@code kren.delivery.time-scale}
 * divides every duration (60 turns an hour into a minute) so a live demo doesn't make visitors wait.
 */
@Component
public class DeliveryPolicy {

	static final Duration HANDLING = Duration.ofHours(2);
	static final int KM_PER_HOUR = 250;
	static final Duration MAX = Duration.ofHours(72);

	/** Used when a country isn't in the location table. */
	static final Duration UNKNOWN_DISTANCE = Duration.ofHours(24);

	private final double timeScale;

	public DeliveryPolicy(@Value("${kren.delivery.time-scale:1}") double timeScale) {
		if (timeScale <= 0) {
			throw new IllegalArgumentException("kren.delivery.time-scale must be positive");
		}
		this.timeScale = timeScale;
	}

	public Duration travelTime(Integer distanceKm) {
		Duration real = realTravelTime(distanceKm);
		return Duration.ofSeconds(Math.round(real.toSeconds() / timeScale));
	}

	/** Unscaled flight time, rounded to the minute. */
	static Duration realTravelTime(Integer distanceKm) {
		if (distanceKm == null) {
			return UNKNOWN_DISTANCE;
		}
		long flightMinutes = Math.round(distanceKm * 60.0 / KM_PER_HOUR);
		Duration total = HANDLING.plusMinutes(flightMinutes);
		return total.compareTo(MAX) > 0 ? MAX : total;
	}

}
