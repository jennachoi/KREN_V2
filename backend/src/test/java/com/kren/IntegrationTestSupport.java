package com.kren;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import jakarta.servlet.http.Cookie;

/**
 * Full-stack tests against a real Postgres (Testcontainers), going through the security filter
 * chain via MockMvc. All subclasses share one Spring context and one container; tables are wiped
 * before each test, leaving the Flyway-seeded {@code interests} catalogue in place. Time comes from a
 * {@link MutableClock}, and the arrival scheduler is off so tests decide when letters land.
 */
@SpringBootTest(properties = "kren.delivery.scheduler-enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

	protected static final String PASSWORD = "correct-horse-battery";

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected JdbcTemplate jdbc;

	@Autowired
	protected MutableClock clock;

	@BeforeEach
	void cleanDatabase() {
		clock.reset();
		jdbc.execute("TRUNCATE users, user_interests, letters, stamp_ledger, daily_checkins, blocks RESTART IDENTITY CASCADE");
	}

	/**
	 * Acts like the SPA: sends a matching {@code XSRF-TOKEN} cookie and {@code X-XSRF-TOKEN} header.
	 * Deliberately not spring-security-test's {@code csrf()}, which swaps the shared CsrfFilter's
	 * repository for a session-based one and would leave the real cookie config untested.
	 */
	protected static RequestPostProcessor xsrf() {
		return xsrf(UUID.randomUUID().toString(), null);
	}

	protected static RequestPostProcessor xsrf(String cookieValue, String headerValue) {
		return request -> {
			List<Cookie> cookies = new ArrayList<>(
					request.getCookies() == null ? List.of() : Arrays.asList(request.getCookies()));
			cookies.add(new Cookie("XSRF-TOKEN", cookieValue));
			request.setCookies(cookies.toArray(Cookie[]::new));
			request.addHeader("X-XSRF-TOKEN", headerValue != null ? headerValue : cookieValue);
			return request;
		};
	}

	/** Signs up through the real endpoint and returns the logged-in session. */
	protected MockHttpSession signup(String email, String nativeLang, String learningLang) throws Exception {
		return signup(email, nativeLang, learningLang, "KR");
	}

	protected MockHttpSession signup(String email, String nativeLang, String learningLang, String country)
			throws Exception {
		MvcResult result = mvc.perform(post("/api/auth/signup").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupJson(email, nativeLang, learningLang, country)))
			.andExpect(status().isCreated())
			.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	protected static String signupJson(String email, String nativeLang, String learningLang) {
		return signupJson(email, nativeLang, learningLang, "KR");
	}

	protected static String signupJson(String email, String nativeLang, String learningLang, String country) {
		return """
				{"email":"%s","password":"%s","displayName":"%s","country":"%s",
				 "nativeLang":"%s","learningLang":"%s"}
				""".formatted(email, PASSWORD, email.substring(0, email.indexOf('@')), country, nativeLang, learningLang);
	}

	protected long userId(String email) {
		return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
	}

	protected void block(String blockerEmail, String blockedEmail) {
		jdbc.update("INSERT INTO blocks (blocker_id, blocked_id) VALUES (?, ?)", userId(blockerEmail),
				userId(blockedEmail));
	}

	protected int stampBalance(String email) {
		return jdbc.queryForObject("SELECT stamp_balance FROM users WHERE email = ?", Integer.class, email);
	}

	protected void setStampBalance(String email, int balance) {
		jdbc.update("UPDATE users SET stamp_balance = ? WHERE email = ?", balance, email);
	}

	/** Lets every letter in flight land (24h is the longest flight). */
	protected void letLettersArrive() {
		clock.advance(Duration.ofHours(25));
	}

	protected long interestId(String name) {
		return jdbc.queryForObject("SELECT id FROM interests WHERE name = ?", Long.class, name);
	}

}
