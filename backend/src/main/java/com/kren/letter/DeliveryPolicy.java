package com.kren.letter;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * How long a letter flies, using KREN v1's distance table. {@code kren.delivery.time-scale} divides
 * every duration (60 turns an hour into a minute) so a live demo doesn't make visitors wait a day.
 */
@Component
public class DeliveryPolicy {

	private record Leg(int upToKm, int hours) {
	}

	private static final Leg[] TABLE = { new Leg(300, 1), new Leg(2_000, 4), new Leg(4_000, 8), new Leg(6_000, 12),
			new Leg(8_000, 16), new Leg(10_000, 20), new Leg(Integer.MAX_VALUE, 24) };

	/** Used when a country isn't in the location table. */
	static final int UNKNOWN_DISTANCE_HOURS = 12;

	private final double timeScale;

	public DeliveryPolicy(@Value("${kren.delivery.time-scale:1}") double timeScale) {
		if (timeScale <= 0) {
			throw new IllegalArgumentException("kren.delivery.time-scale must be positive");
		}
		this.timeScale = timeScale;
	}

	public Duration travelTime(Integer distanceKm) {
		return Duration.ofSeconds(Math.round(hoursFor(distanceKm) * 3600 / timeScale));
	}

	static int hoursFor(Integer distanceKm) {
		if (distanceKm == null) {
			return UNKNOWN_DISTANCE_HOURS;
		}
		for (Leg leg : TABLE) {
			if (distanceKm < leg.upToKm()) {
				return leg.hours();
			}
		}
		throw new IllegalStateException("unreachable");
	}

}
