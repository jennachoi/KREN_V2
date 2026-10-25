package com.kren.letter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeliveryPolicyTest {

	@ParameterizedTest(name = "{0} km -> {1} min")
	@CsvSource({ "0,120", "250,180", "1150,396", "8850,2244", "11000,2760", "20000,4320" })
	void twoHoursPlusOneHourPer250KmCappedAt72h(int km, int minutes) {
		assertThat(new DeliveryPolicy(1).travelTime(km)).isEqualTo(Duration.ofMinutes(minutes));
	}

	@Test
	void growsSmoothlyWithDistance() {
		var policy = new DeliveryPolicy(1);
		// v1's table jumped 4 hours between 9,999 and 10,000 km; this never jumps more than a minute
		assertThat(policy.travelTime(10_000).minus(policy.travelTime(9_999))).isLessThanOrEqualTo(Duration.ofMinutes(1));
	}

	@Test
	void timeScaleCompressesFlights() {
		// 11,000 km = 46h real = 46 min at 60x
		assertThat(new DeliveryPolicy(60).travelTime(11_000)).isEqualTo(Duration.ofMinutes(46));
	}

	@Test
	void unknownDistanceUsesTheFallback() {
		assertThat(new DeliveryPolicy(1).travelTime(null)).isEqualTo(Duration.ofHours(24));
	}

	@Test
	void rejectsNonPositiveScale() {
		assertThatThrownBy(() -> new DeliveryPolicy(0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void haversineMatchesKnownDistance() throws Exception {
		var locations = new CountryLocations();
		// Seoul -> London is about 8,850 km
		assertThat(locations.distanceKm("KR", "GB")).hasValueSatisfying(km -> assertThat(km).isBetween(8_700, 9_000));
		assertThat(locations.distanceKm("KR", "KR")).hasValue(0);
		assertThat(locations.distanceKm("KR", "FJ")).isEmpty();
	}

}
