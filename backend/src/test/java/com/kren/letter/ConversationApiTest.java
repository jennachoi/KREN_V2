package com.kren.letter;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import com.jayway.jsonpath.JsonPath;
import com.kren.IntegrationTestSupport;

class ConversationApiTest extends IntegrationTestSupport {

	@Autowired LetterService letterService;

	MockHttpSession mina;
	long minaId;
	long alexId;
	long eveId;

	@BeforeEach
	void users() throws Exception {
		mina = signup("mina@example.com", "ko", "en");
		signup("alex@example.com", "en", "ko");
		signup("eve@example.com", "en", "ko");
		minaId = userId("mina@example.com");
		alexId = userId("alex@example.com");
		eveId = userId("eve@example.com");
	}

	@Test
	void listsOneRowPerPenpalWithTheNewestLetterAndCounts() throws Exception {
		long first = letterService.send(alexId, minaId, "Hi Mina").id();
		clock.advance(Duration.ofDays(1));
		letterService.reply(minaId, first, "Hi Alex");
		clock.advance(Duration.ofMinutes(1));
		letterService.send(eveId, minaId, "Hello from Eve");
		clock.advance(Duration.ofDays(1));
		letterService.send(alexId, minaId, "Still there?"); // in the air, then landed, unanswered, unread
		clock.advance(Duration.ofMinutes(1));
		letterService.send(minaId, eveId, "Eve, my reply is on its way"); // in the air

		mvc.perform(get("/api/conversations").session(mina))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2))
			// newest activity first: the letter to Eve was sent last
			.andExpect(jsonPath("$.content[*].penpal.displayName", contains("eve", "alex")))
			.andExpect(jsonPath("$.content[0].lastFromMe").value(true))
			.andExpect(jsonPath("$.content[0].inFlight").value(1))
			.andExpect(jsonPath("$.content[0].unread").value(1))
			.andExpect(jsonPath("$.content[1].lastLetter.preview").value(nullValue())) // still sealed for Mina
			.andExpect(jsonPath("$.content[1].inFlight").value(1));

		clock.advance(Duration.ofDays(1));
		mvc.perform(get("/api/conversations").session(mina))
			.andExpect(jsonPath("$.content[*].penpal.displayName", contains("eve", "alex")))
			.andExpect(jsonPath("$.content[1].lastLetter.preview").value("Still there?"))
			// "Hi Mina" was answered without being opened, so it still counts as unread
			.andExpect(jsonPath("$.content[1].unread").value(2))
			.andExpect(jsonPath("$.content[1].awaitingReply").value(1))
			.andExpect(jsonPath("$.content[1].inFlight").value(0));
	}

	@Test
	void threadIsOldestFirstAndPagesBackwardsWithACursor() throws Exception {
		long previous = letterService.send(alexId, minaId, "letter 1").id();
		for (int i = 2; i <= 5; i++) {
			clock.advance(Duration.ofDays(1));
			long from = i % 2 == 0 ? minaId : alexId;
			previous = letterService.reply(from, previous, "letter " + i).id();
		}
		clock.advance(Duration.ofDays(1));
		letterService.send(eveId, minaId, "not part of this conversation");

		String json = mvc.perform(get("/api/conversations/{id}?size=3", alexId).session(mina))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.penpal.id").value(alexId))
			.andExpect(jsonPath("$.letters[*].body", contains("letter 3", "letter 4", "letter 5")))
			.andExpect(jsonPath("$.hasEarlier").value(true))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long oldestShown = JsonPath.<Number>read(json, "$.letters[0].id").longValue();

		mvc.perform(get("/api/conversations/{id}?size=3&before={before}", alexId, oldestShown).session(mina))
			.andExpect(jsonPath("$.letters[*].body", contains("letter 1", "letter 2")))
			.andExpect(jsonPath("$.hasEarlier").value(false));
	}

	@Test
	void showsWhichLettersHaveBeenCorrected() throws Exception {
		long letter = letterService.send(alexId, minaId, "I am go to school.").id();
		clock.advance(Duration.ofDays(1));
		mvc.perform(post("/api/letters/{id}/correction", letter)
			.session(mina)
			.with(xsrf())
			.contentType("application/json")
			.content("""
					{"lines":[{"original":"I am go to school.","corrected":"I go to school.","note":null}]}
					"""))
			.andExpect(status().isCreated());

		mvc.perform(get("/api/conversations/{id}", alexId).session(mina))
			.andExpect(jsonPath("$.letters", hasSize(1)))
			.andExpect(jsonPath("$.letters[0].corrected").value(true));
	}

	@Test
	void aCursorFromSomeoneElsesLettersIsForbidden() throws Exception {
		long theirs = letterService.send(alexId, eveId, "private").id();

		mvc.perform(get("/api/conversations/{id}?before={before}", alexId, theirs).session(mina))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/conversations/{id}", 999999).session(mina)).andExpect(status().isNotFound());
		mvc.perform(get("/api/conversations")).andExpect(status().isUnauthorized());
	}

	@Test
	void blockedPeopleDropOutOfTheList() throws Exception {
		letterService.send(alexId, minaId, "Hi");
		letterService.send(eveId, minaId, "Hi");

		mvc.perform(put("/api/blocks/{id}", eveId).session(mina).with(xsrf())).andExpect(status().is2xxSuccessful());

		mvc.perform(get("/api/conversations").session(mina))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[*].penpal.displayName", contains("alex")));
	}

}
