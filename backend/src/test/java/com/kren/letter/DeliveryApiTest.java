package com.kren.letter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.kren.IntegrationTestSupport;

/** Seoul (KR) to Washington (US) is ~11,000 km: 2h handling + 1h per 250 km ≈ 46h. */
class DeliveryApiTest extends IntegrationTestSupport {

	@Autowired
	LetterArrivalDispatcher dispatcher;

	MockHttpSession mina; // KR
	MockHttpSession alex; // US
	long minaId;
	long alexId;

	@BeforeEach
	void users() throws Exception {
		mina = signup("mina@example.com", "ko", "en", "KR");
		alex = signup("alex@example.com", "en", "ko", "US");
		minaId = userId("mina@example.com");
		alexId = userId("alex@example.com");
	}

	@Test
	void flightTimeAndRouteComeFromTheCountries() throws Exception {
		String json = send(mina, alexId, "Hello from Seoul")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.route.from.country").value("KR"))
			.andExpect(jsonPath("$.route.to.country").value("US"))
			.andExpect(jsonPath("$.route.from.lat").isNumber())
			.andExpect(jsonPath("$.delivered").value(false))
			.andReturn().getResponse().getContentAsString();

		int distance = JsonPath.read(json, "$.route.distanceKm");
		assertThat(distance).isBetween(10_500, 11_500);
		Duration flight = Duration.between(Instant.parse(JsonPath.read(json, "$.sentAt")),
				Instant.parse(JsonPath.read(json, "$.deliverAt")));
		assertThat(flight).isEqualTo(DeliveryPolicy.realTravelTime(distance));
		assertThat(flight).isBetween(Duration.ofHours(44), Duration.ofHours(48));
	}

	@Test
	void sameCountryTakesTwoHours() throws Exception {
		signup("jun@example.com", "ko", "ja", "KR");
		String json = send(mina, userId("jun@example.com"), "Hi neighbour").andReturn().getResponse().getContentAsString();
		assertThat((int) JsonPath.read(json, "$.route.distanceKm")).isZero();
		assertThat(Duration.between(Instant.parse(JsonPath.read(json, "$.sentAt")),
				Instant.parse(JsonPath.read(json, "$.deliverAt")))).isEqualTo(Duration.ofHours(2));
	}

	@Test
	void unknownCountryFallsBackToADay() throws Exception {
		signup("sione@example.com", "en", "ko", "FJ"); // valid ISO code, not in the location table
		String json = send(mina, userId("sione@example.com"), "Bula!").andReturn().getResponse().getContentAsString();
		assertThat((Object) JsonPath.read(json, "$.route.distanceKm")).isNull();
		assertThat(Duration.between(Instant.parse(JsonPath.read(json, "$.sentAt")),
				Instant.parse(JsonPath.read(json, "$.deliverAt")))).isEqualTo(Duration.ofHours(24));
	}

	@Test
	void letterIsSealedForTheRecipientUntilItLands() throws Exception {
		long id = idOf(send(mina, alexId, "Secret until it lands"));

		// in flight: recipient sees the tracker, not the words; sender sees everything
		mvc.perform(get("/api/letters/{id}", id).session(alex))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.body").value(nullValue()))
			.andExpect(jsonPath("$.delivered").value(false))
			.andExpect(jsonPath("$.readAt").value(nullValue()));
		mvc.perform(get("/api/letters/inbox").session(alex))
			.andExpect(jsonPath("$.content[0].preview").value(nullValue()))
			.andExpect(jsonPath("$.content[0].delivered").value(false));
		mvc.perform(get("/api/letters/{id}", id).session(mina))
			.andExpect(jsonPath("$.body").value("Secret until it lands"));
		assertThat(jdbc.queryForObject("SELECT read_at FROM letters WHERE id = ?", Instant.class, id))
			.as("opening a sealed letter is not a read receipt").isNull();

		Instant deliverAt = Instant.parse(JsonPath.read(
				mvc.perform(get("/api/letters/{id}", id).session(mina)).andReturn().getResponse().getContentAsString(),
				"$.deliverAt"));
		clock.advance(Duration.between(clock.instant(), deliverAt).minusMinutes(1));
		mvc.perform(get("/api/letters/{id}", id).session(alex)).andExpect(jsonPath("$.body").value(nullValue()));

		clock.advance(Duration.ofMinutes(1));
		mvc.perform(get("/api/letters/{id}", id).session(alex))
			.andExpect(jsonPath("$.body").value("Secret until it lands"))
			.andExpect(jsonPath("$.delivered").value(true))
			.andExpect(jsonPath("$.readAt").isNotEmpty());
	}

	@Test
	void cannotReplyToALetterStillInFlight() throws Exception {
		long id = idOf(send(mina, alexId, "On its way"));

		reply(alex, id, "Too early").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("LETTER_IN_TRANSIT"));
		reply(mina, id, "P.S. forgot to say hi").andExpect(status().isCreated()); // sender's follow-up is fine

		letLettersArrive();
		reply(alex, id, "Got it!").andExpect(status().isCreated());
	}

	@Test
	void dispatcherLandsEachLetterExactlyOnce() throws Exception {
		send(mina, alexId, "one");
		send(mina, alexId, "two");

		assertThat(dispatcher.dispatchAllDue()).as("nothing has landed yet").isZero();
		letLettersArrive();
		assertThat(dispatcher.dispatchAllDue()).isEqualTo(2);
		assertThat(dispatcher.dispatchAllDue()).as("already processed").isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM letters WHERE arrival_notified_at IS NULL", Long.class))
			.isZero();
	}

	private ResultActions send(MockHttpSession session, long recipientId, String body) throws Exception {
		return mvc.perform(post("/api/letters").with(xsrf()).session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"recipientId\":%d,\"body\":\"%s\"}".formatted(recipientId, body)));
	}

	private ResultActions reply(MockHttpSession session, long letterId, String body) throws Exception {
		return mvc.perform(post("/api/letters/{id}/reply", letterId).with(xsrf()).session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"body\":\"%s\"}".formatted(body)));
	}

	private static long idOf(ResultActions result) throws Exception {
		return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
	}

}
