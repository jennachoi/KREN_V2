package com.kren.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.kren.IntegrationTestSupport;

class AuthApiTest extends IntegrationTestSupport {

	@Test
	void signupCreatesAccountAndSignsIn() throws Exception {
		mvc.perform(post("/api/auth/signup").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupJson("Mina@Example.com", "ko", "en")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("mina@example.com"))
			.andExpect(jsonPath("$.nativeLang").value("ko"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist());

		MockHttpSession session = signup("jun@example.com", "ko", "ja");
		mvc.perform(get("/api/users/me").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("jun@example.com"));
	}

	@Test
	void passwordIsStoredAsBcryptHash() throws Exception {
		signup("mina@example.com", "ko", "en");
		String hash = jdbc.queryForObject("SELECT password_hash FROM users", String.class);
		assertThat(hash).startsWith("{bcrypt}").doesNotContain(PASSWORD);
	}

	@Test
	void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
		signup("mina@example.com", "ko", "en");
		mvc.perform(post("/api/auth/signup").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupJson("MINA@example.com", "ko", "en")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
	}

	@Test
	void invalidSignupReturnsFieldErrors() throws Exception {
		mvc.perform(post("/api/auth/signup").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"not-an-email","password":"short","displayName":" ",
						 "country":"XX","nativeLang":"en-US","learningLang":"ko"}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field")
				.value(hasItems("email", "password", "displayName", "country", "nativeLang")));
	}

	@Test
	void sameNativeAndLearningLanguageIsRejected() throws Exception {
		mvc.perform(post("/api/auth/signup").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupJson("mina@example.com", "ko", "ko")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("LANGUAGES_MUST_DIFFER"));
	}

	@Test
	void malformedJsonGetsProblemShape() throws Exception {
		mvc.perform(post("/api/auth/signup").with(xsrf()).contentType(MediaType.APPLICATION_JSON).content("{"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	@Test
	void loginWithCorrectPasswordStartsSession() throws Exception {
		signup("mina@example.com", "ko", "en");
		MvcResult result = mvc.perform(login("MINA@example.com", PASSWORD))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("mina@example.com"))
			.andReturn();

		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		mvc.perform(get("/api/users/me").session(session)).andExpect(status().isOk());
	}

	@Test
	void wrongPasswordAndUnknownEmailLookTheSame() throws Exception {
		signup("mina@example.com", "ko", "en");
		mvc.perform(login("mina@example.com", "wrong-password"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
		mvc.perform(login("nobody@example.com", PASSWORD))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
	}

	@Test
	void loginRotatesSessionIdToPreventFixation() throws Exception {
		signup("mina@example.com", "ko", "en");
		MockHttpSession preLogin = new MockHttpSession();
		String idBefore = preLogin.getId();

		MvcResult result = mvc.perform(login("mina@example.com", PASSWORD).session(preLogin).with(req -> {
			// Simulate the browser presenting the (possibly attacker-planted) session cookie.
			req.setRequestedSessionId(idBefore);
			req.setRequestedSessionIdValid(true);
			return req;
		}))
			.andExpect(status().isOk())
			.andReturn();

		assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(idBefore);
	}

	@Test
	void protectedEndpointWithoutSessionIs401() throws Exception {
		mvc.perform(get("/api/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void stateChangingRequestWithoutCsrfTokenIs403() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"a@b.com\",\"password\":\"whatever1\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
	}

	@Test
	void mismatchedCsrfHeaderIs403() throws Exception {
		mvc.perform(post("/api/auth/signup").with(xsrf("cookie-value", "attacker-guess"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupJson("mina@example.com", "ko", "en")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
	}

	@Test
	void csrfCookieIsIssuedForTheSpa() throws Exception {
		mvc.perform(get("/api/interests"))
			.andExpect(status().isOk())
			.andExpect(header().string("Set-Cookie", containsString("XSRF-TOKEN=")));
	}

	@Test
	void loginRotatesCsrfTokenInTheSameResponse() throws Exception {
		signup("mina@example.com", "ko", "en");
		mvc.perform(login("mina@example.com", PASSWORD))
			.andExpect(status().isOk())
			.andExpect(result -> assertThat(result.getResponse().getHeaders("Set-Cookie"))
				.filteredOn(c -> c.startsWith("XSRF-TOKEN=") && !c.contains("Max-Age=0"))
				.hasSize(1));
	}

	@Test
	void logoutInvalidatesSession() throws Exception {
		MockHttpSession session = signup("mina@example.com", "ko", "en");
		mvc.perform(post("/api/auth/logout").with(xsrf()).session(session)).andExpect(status().isNoContent());
		mvc.perform(get("/api/users/me").session(session)).andExpect(status().isUnauthorized());
	}

	@Test
	void unmappedPathsAreDeniedByDefault() throws Exception {
		MockHttpSession session = signup("mina@example.com", "ko", "en");
		mvc.perform(get("/actuator/env").session(session))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void interestsArePublic() throws Exception {
		mvc.perform(get("/api/interests"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name").value(hasItem("K-Culture")));
	}

	private static MockHttpServletRequestBuilder login(String email,
			String password) {
		return post("/api/auth/login").with(xsrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
	}

}
