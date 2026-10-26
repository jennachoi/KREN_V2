package com.kren.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;

import com.kren.IntegrationTestSupport;
import com.kren.auth.AuthService;
import com.kren.correction.CorrectionService;
import com.kren.letter.LetterService;
import com.kren.stamp.StampService;
import com.kren.user.InterestRepository;
import com.kren.user.UserRepository;
import com.kren.user.UserService;

class DemoDataSeederTest extends IntegrationTestSupport {

	@Autowired AuthService authService;
	@Autowired UserService userService;
	@Autowired LetterService letterService;
	@Autowired CorrectionService correctionService;
	@Autowired StampService stampService;
	@Autowired EntityManager entityManager;
	@Autowired PlatformTransactionManager transactionManager;
	@Autowired UserRepository userRepository;
	@Autowired InterestRepository interestRepository;

	/** run() is @Transactional on the real bean; mirror that here since this instance isn't proxied. */
	static void seed(DemoDataSeeder seeder, PlatformTransactionManager transactionManager) {
		new TransactionTemplate(transactionManager)
			.executeWithoutResult(s -> seeder.run(new DefaultApplicationArguments()));
	}

	DemoDataSeeder seeder() {
		return new DemoDataSeeder(authService, userService, letterService, correctionService, stampService,
				userRepository, interestRepository, new DemoTimeline(jdbc, entityManager), jdbc);
	}

	@Test
	void seedsOnceAndDemoAccountsCanLogIn() throws Exception {
		var seeder = seeder();
		seed(seeder, transactionManager);
		seed(seeder, transactionManager); // e.g. an app restart

		assertThat(count("SELECT count(*) FROM users WHERE email LIKE '%@demo.kren'")).isEqualTo(26);
		assertThat(count("SELECT count(*) FROM users WHERE avatar IS NULL")).isZero();
		assertThat(count("SELECT count(*) FROM letters")).isEqualTo(24);
		assertThat(count("SELECT count(*) FROM corrections")).isEqualTo(6);
		// some letters are still in the air for the maps
		assertThat(jdbc.queryForObject("SELECT count(*) FROM letters WHERE deliver_at > ?", Long.class,
				Timestamp.from(clock.instant()))).isEqualTo(9);
		// seeded through the real services, so stamps were actually spent and earned
		assertThat(stampBalance("emma@demo.kren")).as("5 + 20 - 7 letters + 3 corrections").isEqualTo(21);
		assertThat(stampBalance("minji@demo.kren")).as("5 + 20 - 2 letters + 2 corrections").isEqualTo(25);

		mvc.perform(post("/api/auth/login")
				.with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"emma@demo.kren\",\"password\":\"" + DemoDataSeeder.PASSWORD + "\"}"))
			.andExpect(status().isOk());
	}

	@Test
	void emmaHasHistoryWaitingLettersAndLettersInTheAir() {
		seed(seeder(), transactionManager);

		var dashboard = letterService.dashboard(userId("emma@demo.kren"));
		assertThat(dashboard.incoming()).hasSize(2);
		assertThat(dashboard.outgoing()).hasSize(2);
		assertThat(dashboard.awaitingReplyCount()).isEqualTo(3);
		assertThat(correctionService.notebook(userId("emma@demo.kren"), 0, 20).content()).hasSize(3);
		// a member for weeks, with the oldest thread three weeks back
		assertThat(jdbc.queryForObject("SELECT created_at < ? FROM users WHERE email = 'emma@demo.kren'",
				Boolean.class, Timestamp.from(clock.instant().minusSeconds(30L * 86400)))).isTrue();
		assertThat(jdbc.queryForObject("""
				SELECT min(sent_at) < ? FROM letters l JOIN users u ON u.id = l.sender_id
				WHERE u.email = 'emma@demo.kren'
				""", Boolean.class, Timestamp.from(clock.instant().minusSeconds(20L * 86400)))).isTrue();
	}

	@Test
	void newVersionReplacesDemoDataButKeepsRealUsers() throws Exception {
		signup("real@example.com", "en", "ko");
		var seeder = seeder();
		seed(seeder, transactionManager);
		jdbc.update("UPDATE app_settings SET value = 'old' WHERE key = 'demo_seed_version'");

		seed(seeder, transactionManager);

		assertThat(count("SELECT count(*) FROM users WHERE email LIKE '%@demo.kren'")).isEqualTo(26);
		assertThat(count("SELECT count(*) FROM letters")).isEqualTo(24);
		assertThat(count("SELECT count(*) FROM users WHERE email = 'real@example.com'")).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT value FROM app_settings WHERE key = 'demo_seed_version'", String.class))
			.isEqualTo(DemoDataSeeder.VERSION);
	}

	private long count(String sql) {
		return jdbc.queryForObject(sql, Long.class);
	}

}
