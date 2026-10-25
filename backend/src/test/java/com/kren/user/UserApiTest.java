package com.kren.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.kren.IntegrationTestSupport;

class UserApiTest extends IntegrationTestSupport {

	@Test
	void updateProfileReplacesFieldsAndInterests() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");
		long music = interestId("Music");
		long travel = interestId("Travel");

		mvc.perform(put("/api/users/me").with(xsrf()).session(me)
				.contentType(MediaType.APPLICATION_JSON)
				.content(profileJson("ja", "[%d, %d]".formatted(travel, music))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.learningLang").value("ja"))
			.andExpect(jsonPath("$.bio").value("Hello!"))
			.andExpect(jsonPath("$.interests[*].name").value(contains("Music", "Travel")));

		mvc.perform(put("/api/users/me").with(xsrf()).session(me)
				.contentType(MediaType.APPLICATION_JSON)
				.content(profileJson("ja", "[%d]".formatted(travel))))
			.andExpect(jsonPath("$.interests[*].name").value(contains("Travel")));
	}

	@Test
	void everyoneGetsAnAvatarAndCanPickFromTheSet() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");
		String assigned = jdbc.queryForObject("SELECT avatar FROM users", String.class);
		assertThat(com.kren.user.Avatars.isAllowed(assigned)).isTrue();

		mvc.perform(put("/api/users/me").with(xsrf()).session(me).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"displayName":"Mina","country":"KR","nativeLang":"ko","learningLang":"en","interestIds":[],"avatar":"m07"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.avatar").value("m07"));
		mvc.perform(put("/api/users/me").with(xsrf()).session(me).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"displayName":"Mina","country":"KR","nativeLang":"ko","learningLang":"en","interestIds":[],"avatar":"<script>"}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_AVATAR"));
		mvc.perform(get("/api/avatars")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(40));
	}

	@Test
	void unknownInterestIdIsRejected() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");
		mvc.perform(put("/api/users/me").with(xsrf()).session(me)
				.contentType(MediaType.APPLICATION_JSON)
				.content(profileJson("en", "[999999]")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INTEREST"));
	}

	@Test
	void profileUpdateCannotMakeLanguagesEqual() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");
		mvc.perform(put("/api/users/me").with(xsrf()).session(me)
				.contentType(MediaType.APPLICATION_JSON)
				.content(profileJson("ko", "[]")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("LANGUAGES_MUST_DIFFER"));
	}

	@Test
	void publicProfileHidesPrivateFields() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");
		signup("alex@example.com", "en", "ko");

		mvc.perform(get("/api/users/{id}", userId("alex@example.com")).session(me))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("alex"))
			.andExpect(jsonPath("$.email").doesNotExist())
			.andExpect(jsonPath("$.stampBalance").doesNotExist());
	}

	@Test
	void blockedUsersAreInvisibleInBothDirections() throws Exception {
		MockHttpSession mina = signup("mina@example.com", "ko", "en");
		MockHttpSession alex = signup("alex@example.com", "en", "ko");
		block("alex@example.com", "mina@example.com");

		mvc.perform(get("/api/users/{id}", userId("alex@example.com")).session(mina))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
		mvc.perform(get("/api/users/{id}", userId("mina@example.com")).session(alex))
			.andExpect(status().isNotFound());
	}

	@Test
	void missingUserIs404() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");
		mvc.perform(get("/api/users/{id}", 999999).session(me))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	private static String profileJson(String learningLang, String interestIds) {
		return """
				{"displayName":"Mina","country":"KR","nativeLang":"ko","learningLang":"%s",
				 "bio":"  Hello!  ","interestIds":%s}
				""".formatted(learningLang, interestIds);
	}

}
