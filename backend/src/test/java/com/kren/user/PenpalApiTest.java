package com.kren.user;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.jayway.jsonpath.JsonPath;
import com.kren.IntegrationTestSupport;

class PenpalApiTest extends IntegrationTestSupport {

	@Test
	void matchesOnlyTheReverseLanguagePairAndExcludesBlocks() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");
		signup("match1@example.com", "en", "ko");
		signup("match2@example.com", "en", "ko");
		signup("wronglearning@example.com", "en", "ja");
		signup("wrongnative@example.com", "ja", "ko");
		signup("sameasme@example.com", "ko", "en");
		signup("blockedme@example.com", "en", "ko");
		signup("iblocked@example.com", "en", "ko");
		block("blockedme@example.com", "mina@example.com");
		block("mina@example.com", "iblocked@example.com");

		mvc.perform(get("/api/penpals").session(me))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2))
			// newest first
			.andExpect(jsonPath("$.content[*].displayName").value(contains("match2", "match1")))
			.andExpect(jsonPath("$.content[0].email").doesNotExist());
	}

	@Test
	void interestFilterMatchesAnyOfTheGivenInterests() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");
		long music = interestId("Music");
		long food = interestId("Food");
		long art = interestId("Art");
		setInterests(signup("music@example.com", "en", "ko"), music);
		setInterests(signup("food@example.com", "en", "ko"), food);
		setInterests(signup("art@example.com", "en", "ko"), art);

		mvc.perform(get("/api/penpals").session(me)
				.param("interestId", String.valueOf(music))
				.param("interestId", String.valueOf(food)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].displayName").value(contains("food", "music")))
			.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void paginatesWithStableOrder() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");
		for (int i = 1; i <= 3; i++) {
			signup("p" + i + "@example.com", "en", "ko");
		}

		mvc.perform(get("/api/penpals").session(me).param("size", "2"))
			.andExpect(jsonPath("$.content[*].displayName").value(contains("p3", "p2")))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(2));
		mvc.perform(get("/api/penpals").session(me).param("size", "2").param("page", "1"))
			.andExpect(jsonPath("$.content[*].displayName").value(contains("p1")));
	}

	@Test
	void pageSizeIsBounded() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");
		mvc.perform(get("/api/penpals").session(me).param("size", "500"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("size"));
	}

	@Test
	void requiresAuthentication() throws Exception {
		mvc.perform(get("/api/penpals")).andExpect(status().isUnauthorized());
	}

	private void setInterests(MockHttpSession session, long interestId) throws Exception {
		mvc.perform(put("/api/users/me").with(xsrf()).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"displayName":"%s","country":"US","nativeLang":"en","learningLang":"ko",
						 "interestIds":[%d]}
						""".formatted(currentName(session), interestId)))
			.andExpect(status().isOk());
	}

	private String currentName(MockHttpSession session) throws Exception {
		String body = mvc.perform(get("/api/users/me").session(session)).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.displayName");
	}

}
