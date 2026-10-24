package com.kren.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.kren.IntegrationTestSupport;
import com.kren.letter.LetterArrivalDispatcher;

class NotificationApiTest extends IntegrationTestSupport {

	@Autowired
	SseHub hub;

	@Autowired
	LetterArrivalDispatcher dispatcher;

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

	/** Ids restart per test, so a stream left open by an earlier test would belong to "the same" user. */
	@AfterEach
	void closeStreams() {
		hub.shutdown();
	}

	/** Takeoff: "a letter is coming" with the contents still sealed. Landing: "it's here", with a preview. */
	@Test
	void recipientIsToldOnTakeoffAndAgainOnArrival() throws Exception {
		MvcResult alexStream = openStream(alex);
		assertThat(hub.connectionCount(alexId)).isEqualTo(1);
		assertThat(body(alexStream)).contains("event:connected");

		sendLetter(mina, alexId).andExpect(status().isCreated());

		assertThat(body(alexStream)).contains("event:incoming").contains("\"displayName\":\"mina\"")
			.contains("\"preview\":null")
			.doesNotContain("event:letter");

		letLettersArrive();
		dispatcher.dispatchAllDue();

		assertThat(body(alexStream)).contains("event:letter").contains("\"preview\":\"Hello Alex\"");
	}

	@Test
	void onlyTheRecipientIsNotified() throws Exception {
		MvcResult eveStream = openStream(eve);
		MvcResult minaStream = openStream(mina);

		sendLetter(mina, alexId).andExpect(status().isCreated());

		assertThat(body(eveStream)).doesNotContain("event:incoming");
		assertThat(body(minaStream)).as("sender is not notified of their own letter").doesNotContain("event:incoming");
	}

	/** The event is AFTER_COMMIT, so a send that rolls back must not produce a phantom notification. */
	@Test
	void rolledBackSendDoesNotNotify() throws Exception {
		MvcResult alexStream = openStream(alex);
		setStampBalance("mina@example.com", 0);

		sendLetter(mina, alexId).andExpect(status().isConflict());

		assertThat(body(alexStream)).doesNotContain("event:incoming");
	}

	@Test
	void everyOpenTabReceivesTheEvent() throws Exception {
		MvcResult tab1 = openStream(alex);
		MvcResult tab2 = openStream(alex);

		sendLetter(mina, alexId).andExpect(status().isCreated());

		assertThat(body(tab1)).contains("event:incoming");
		assertThat(body(tab2)).contains("event:incoming");
	}

	@Test
	void streamRequiresLogin() throws Exception {
		mvc.perform(get("/api/notifications/stream")).andExpect(status().isUnauthorized());
	}

	private MvcResult openStream(MockHttpSession session) throws Exception {
		return mvc.perform(get("/api/notifications/stream").session(session).accept(MediaType.TEXT_EVENT_STREAM))
			.andExpect(request().asyncStarted())
			.andReturn();
	}

	private static String body(MvcResult result) throws Exception {
		return result.getResponse().getContentAsString();
	}

	private ResultActions sendLetter(MockHttpSession session, long to) throws Exception {
		return mvc.perform(post("/api/letters").with(xsrf()).session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"recipientId\":%d,\"body\":\"Hello Alex\"}".formatted(to)));
	}

}
