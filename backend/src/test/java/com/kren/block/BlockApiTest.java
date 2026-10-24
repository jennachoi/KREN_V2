package com.kren.block;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import com.kren.IntegrationTestSupport;

class BlockApiTest extends IntegrationTestSupport {

	MockHttpSession mina;
	MockHttpSession alex;
	long minaId;
	long alexId;

	@BeforeEach
	void users() throws Exception {
		mina = signup("mina@example.com", "ko", "en");
		alex = signup("alex@example.com", "en", "ko");
		minaId = userId("mina@example.com");
		alexId = userId("alex@example.com");
	}

	@Test
	void blockHidesBothSidesEverywhereAndUnblockRestores() throws Exception {
		mvc.perform(put("/api/blocks/{id}", alexId).with(xsrf()).session(mina)).andExpect(status().isNoContent());

		// the blocker and the blocked person both lose sight of each other
		mvc.perform(get("/api/penpals").session(mina)).andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(get("/api/penpals").session(alex)).andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(get("/api/users/{id}", minaId).session(alex)).andExpect(status().isNotFound());
		sendLetter(alex, minaId).andExpect(status().isNotFound());

		mvc.perform(delete("/api/blocks/{id}", alexId).with(xsrf()).session(mina)).andExpect(status().isNoContent());

		mvc.perform(get("/api/penpals").session(mina)).andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get("/api/users/{id}", minaId).session(alex)).andExpect(status().isOk());
		sendLetter(alex, minaId).andExpect(status().isCreated());
	}

	@Test
	void blockAndUnblockAreIdempotent() throws Exception {
		mvc.perform(put("/api/blocks/{id}", alexId).with(xsrf()).session(mina)).andExpect(status().isNoContent());
		mvc.perform(put("/api/blocks/{id}", alexId).with(xsrf()).session(mina)).andExpect(status().isNoContent());
		mvc.perform(get("/api/blocks").session(mina)).andExpect(jsonPath("$.totalElements").value(1));

		mvc.perform(delete("/api/blocks/{id}", alexId).with(xsrf()).session(mina)).andExpect(status().isNoContent());
		mvc.perform(delete("/api/blocks/{id}", alexId).with(xsrf()).session(mina)).andExpect(status().isNoContent());
		mvc.perform(get("/api/blocks").session(mina)).andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void listShowsOnlyMyBlocksNewestFirst() throws Exception {
		signup("eve@example.com", "en", "ko");
		long eveId = userId("eve@example.com");
		mvc.perform(put("/api/blocks/{id}", alexId).with(xsrf()).session(mina));
		mvc.perform(put("/api/blocks/{id}", eveId).with(xsrf()).session(mina));
		mvc.perform(put("/api/blocks/{id}", minaId).with(xsrf()).session(alex));

		mvc.perform(get("/api/blocks").session(mina))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].user.displayName").value(contains("eve", "alex")))
			.andExpect(jsonPath("$.content[0].blockedAt").isNotEmpty());
	}

	@Test
	void cannotBlockSelfOrNobody() throws Exception {
		mvc.perform(put("/api/blocks/{id}", minaId).with(xsrf()).session(mina))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CANNOT_BLOCK_SELF"));
		mvc.perform(put("/api/blocks/{id}", 999999).with(xsrf()).session(mina))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	void pastLettersStayInMailboxAfterBlocking() throws Exception {
		sendLetter(alex, minaId).andExpect(status().isCreated());
		mvc.perform(put("/api/blocks/{id}", alexId).with(xsrf()).session(mina));

		mvc.perform(get("/api/letters/inbox").session(mina)).andExpect(jsonPath("$.totalElements").value(1));
	}

	private ResultActions sendLetter(MockHttpSession session, long to)
			throws Exception {
		return mvc.perform(post("/api/letters").with(xsrf()).session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"recipientId\":%d,\"body\":\"hi\"}".formatted(to)));
	}

}
