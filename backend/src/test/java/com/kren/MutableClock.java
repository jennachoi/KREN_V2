package com.kren;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/** A clock tests can move forward, e.g. to let a letter's flight time pass instantly. */
public class MutableClock extends Clock {

	private volatile Instant now;

	public MutableClock() {
		reset();
	}

	/** Back to real time (truncated to Postgres' microsecond precision so round-trips compare equal). */
	public void reset() {
		now = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

	public void advance(Duration duration) {
		now = now.plus(duration);
	}

	@Override
	public Instant instant() {
		return now;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		throw new UnsupportedOperationException();
	}

}
