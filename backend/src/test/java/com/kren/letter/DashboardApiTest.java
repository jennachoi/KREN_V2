package com.kren.letter;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.kren.IntegrationTestSupport;

class DashboardApiTest extends IntegrationTestSupport {

	MockHttpSession mina; // KR
	MockHttpSession alex; // US
	MockHttpSession liam; // CA
	long minaId;
	long alexId;
	long liamId;

	@BeforeEach
	void users() throws Exception {
		mina = signup("mina@example.com", "ko", "en", "KR");
		alex = signup("alex@example.com", "en", "ko", "US");
		liam = signup("liam@example.com", "en", "ko", "CA");
		minaId = userId("mina@example.com");
		alexId = userId("alex@example.com");
		liamId = userId("liam@example.com");
	}

	@Test
	void splitsFlightsByDirectionWithRoutes() throws Exception {
		send(alex, minaId, "to mina");
		send(mina, liamId, "from mina");

		mvc.perform(get("/api/letters/dashboard").session(mina))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.home.country").value("KR"))
			.andExpect(jsonPath("$.home.lat").isNumber())
			.andExpect(jsonPath("$.incoming[*].counterpart.displayName").value(contains("alex")))
			.andExpect(jsonPath("$.incoming[0].direction").value("INCOMING"))
			.andExpect(jsonPath("$.incoming[0].route.from.country").value("US"))
			.andExpect(jsonPath("$.outgoing[*].counterpart.displayName").value(contains("liam")))
			.andExpect(jsonPath("$.awaitingReplyCount").value(0));
	}

	@Test
	void landedLettersMoveToAwaitingReplyUntilAnswered() throws Exception {
		long fromAlex = idOf(send(alex, minaId, "Hi Mina"));
		send(liam, minaId, "Hi from Canada");
		letLettersArrive();

		mvc.perform(get("/api/letters/dashboard").session(mina))
			.andExpect(jsonPath("$.incoming.length()").value(0))
			.andExpect(jsonPath("$.awaitingReplyCount").value(2))
			.andExpect(jsonPath("$.awaitingReply[*].preview").value(contains("Hi from Canada", "Hi Mina"))); // newest arrival first (same ETA: later letter first)

		mvc.perform(post("/api/letters/{id}/reply", fromAlex).with(xsrf()).session(mina)
			.contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Hi Alex!\"}"))
			.andExpect(status().isCreated());

		mvc.perform(get("/api/letters/dashboard").session(mina))
			.andExpect(jsonPath("$.awaitingReplyCount").value(1))
			.andExpect(jsonPath("$.awaitingReply[0].counterpart.displayName").value("liam"))
			.andExpect(jsonPath("$.outgoing[0].counterpart.displayName").value("alex"));
	}

	@Test
	void othersLettersNeverShowUp() throws Exception {
		send(alex, liamId, "between them");
		mvc.perform(get("/api/letters/dashboard").session(mina))
			.andExpect(jsonPath("$.incoming.length()").value(0))
			.andExpect(jsonPath("$.outgoing.length()").value(0));
	}

	private ResultActions send(MockHttpSession session, long to, String body) throws Exception {
		return mvc.perform(post("/api/letters").with(xsrf()).session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"recipientId\":%d,\"body\":\"%s\"}".formatted(to, body)))
			.andExpect(status().isCreated());
	}

	private static long idOf(ResultActions result) throws Exception {
		return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
	}

}
