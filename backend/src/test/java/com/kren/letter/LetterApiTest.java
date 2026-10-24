package com.kren.letter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
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
import com.kren.stamp.StampService;

class LetterApiTest extends IntegrationTestSupport {

	MockHttpSession mina;
	MockHttpSession alex;
	MockHttpSession eve;
	long minaId;
	long alexId;

	@BeforeEach
	void users() throws Exception {
		mina = signup("mina@example.com", "ko", "en");
		alex = signup("alex@example.com", "en", "ko");
		eve = signup("eve@example.com", "en", "ko");
		minaId = userId("mina@example.com");
		alexId = userId("alex@example.com");
	}

	@Test
	void sendDeductsOneStampAndWritesLedger() throws Exception {
		send(mina, alexId, "Hello Alex!")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.sender.displayName").value("mina"))
			.andExpect(jsonPath("$.recipient.id").value(alexId))
			.andExpect(jsonPath("$.readAt").value(nullValue()));

		assertThat(stampBalance("mina@example.com")).isEqualTo(StampService.SIGNUP_BONUS - 1);
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM stamp_ledger WHERE reason = 'SEND_LETTER' AND amount = -1 AND letter_id IS NOT NULL",
				Long.class)).isEqualTo(1);
	}

	@Test
	void noStampsMeansNoLetterAndNoLedgerRow() throws Exception {
		setStampBalance("mina@example.com", 0);

		send(mina, alexId, "Hello?")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_STAMPS"));

		assertThat(jdbc.queryForObject("SELECT count(*) FROM letters", Long.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM stamp_ledger WHERE reason = 'SEND_LETTER'", Long.class))
			.isZero();
	}

	@Test
	void failedSendDoesNotSpendAStamp() throws Exception {
		send(mina, 999999, "to nobody").andExpect(status().isNotFound());
		send(mina, minaId, "to myself")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CANNOT_SEND_TO_SELF"));
		assertThat(stampBalance("mina@example.com")).isEqualTo(StampService.SIGNUP_BONUS);
	}

	@Test
	void cannotWriteToSomeoneInABlockRelationship() throws Exception {
		block("alex@example.com", "mina@example.com");
		send(mina, alexId, "Hi").andExpect(status().isNotFound());
		send(alex, minaId, "Hi").andExpect(status().isNotFound());
		assertThat(stampBalance("mina@example.com")).isEqualTo(StampService.SIGNUP_BONUS);
	}

	@Test
	void onlyParticipantsCanReadOrReply() throws Exception {
		long letterId = idOf(send(mina, alexId, "private"));

		mvc.perform(get("/api/letters/{id}", letterId).session(eve))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		reply(eve, letterId, "butting in").andExpect(status().isForbidden());
		mvc.perform(get("/api/letters/{id}", 999999).session(eve)).andExpect(status().isNotFound());

		assertThat(stampBalance("eve@example.com")).isEqualTo(StampService.SIGNUP_BONUS);
	}

	@Test
	void readReceiptIsSetOnlyWhenRecipientOpensIt() throws Exception {
		long letterId = idOf(send(mina, alexId, "read me"));
		letLettersArrive();

		mvc.perform(get("/api/letters/{id}", letterId).session(mina))
			.andExpect(jsonPath("$.readAt").value(nullValue()));
		mvc.perform(get("/api/letters/{id}", letterId).session(alex))
			.andExpect(jsonPath("$.readAt").isNotEmpty());

		String firstReadAt = jdbc.queryForObject("SELECT read_at::text FROM letters WHERE id = ?", String.class,
				letterId);
		mvc.perform(get("/api/letters/{id}", letterId).session(alex));
		assertThat(jdbc.queryForObject("SELECT read_at::text FROM letters WHERE id = ?", String.class, letterId))
			.as("re-opening keeps the first read time").isEqualTo(firstReadAt);
	}

	@Test
	void replyGoesToTheOtherPartyAndLinksParent() throws Exception {
		long letterId = idOf(send(mina, alexId, "first"));
		letLettersArrive();

		reply(alex, letterId, "reply")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.recipient.id").value(minaId))
			.andExpect(jsonPath("$.parentId").value(letterId));
		assertThat(stampBalance("alex@example.com")).isEqualTo(StampService.SIGNUP_BONUS - 1);
	}

	@Test
	void inboxAndSentAreNewestFirstWithPreview() throws Exception {
		send(mina, alexId, "one");
		send(eve(), alexId, "x".repeat(150));
		send(mina, alexId, "three");
		letLettersArrive();

		mvc.perform(get("/api/letters/inbox").session(alex))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.content[*].counterpart.displayName").value(contains("mina", "eve", "mina")))
			.andExpect(jsonPath("$.content[1].preview").value("x".repeat(100) + "…"));
		mvc.perform(get("/api/letters/sent").session(mina))
			.andExpect(jsonPath("$.content[*].preview").value(contains("three", "one")))
			.andExpect(jsonPath("$.content[0].counterpart.id").value(alexId));
		mvc.perform(get("/api/letters/inbox").session(mina)).andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void bodyIsValidated() throws Exception {
		send(mina, alexId, " ").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("body"));
		send(mina, alexId, "x".repeat(5001)).andExpect(status().isBadRequest());
	}

	private MockHttpSession eve() {
		return eve;
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
