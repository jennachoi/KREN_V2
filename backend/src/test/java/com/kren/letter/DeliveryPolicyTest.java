package com.kren.letter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeliveryPolicyTest {

	@ParameterizedTest(name = "{0} km -> {1} h")
	@CsvSource({ "0,1", "299,1", "300,4", "1999,4", "2000,8", "4000,12", "6000,16", "8000,20", "9999,20",
			"10000,24", "19000,24" })
	void followsTheV1DistanceTable(int km, int hours) {
		assertThat(new DeliveryPolicy(1).travelTime(km)).isEqualTo(Duration.ofHours(hours));
	}

	@Test
	void timeScaleCompressesFlights() {
		assertThat(new DeliveryPolicy(60).travelTime(10_000)).isEqualTo(Duration.ofMinutes(24));
	}

	@Test
	void unknownDistanceUsesTheFallback() {
		assertThat(new DeliveryPolicy(1).travelTime(null)).isEqualTo(Duration.ofHours(12));
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
