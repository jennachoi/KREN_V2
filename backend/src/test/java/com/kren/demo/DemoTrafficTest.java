package com.kren.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManager;

import com.kren.IntegrationTestSupport;
import com.kren.auth.AuthService;
import com.kren.correction.CorrectionService;
import com.kren.letter.LetterService;
import com.kren.stamp.StampService;
import com.kren.user.InterestRepository;
import com.kren.user.UserRepository;
import com.kren.user.UserService;

class DemoTrafficTest extends IntegrationTestSupport {

	@Autowired AuthService authService;
	@Autowired UserService userService;
	@Autowired LetterService letterService;
	@Autowired CorrectionService correctionService;
	@Autowired StampService stampService;
	@Autowired EntityManager entityManager;
	@Autowired PlatformTransactionManager transactionManager;
	@Autowired UserRepository userRepository;
	@Autowired InterestRepository interestRepository;

	DemoTraffic traffic;

	@BeforeEach
	void seed() {
		DemoDataSeederTest.seed(new DemoDataSeeder(authService, userService, letterService, correctionService,
				stampService, userRepository, interestRepository, new DemoTimeline(jdbc, entityManager), jdbc),
				transactionManager);
		traffic = new DemoTraffic(letterService, correctionService, stampService, jdbc, clock);
	}

	@Test
	void keepsLettersInTheAirAndEmmaAndMinjiBusy() {
		clock.advance(Duration.ofDays(3)); // everything seeded has landed

		for (int i = 0; i < 5; i++) {
			traffic.tick();
		}

		assertThat(inFlight()).isGreaterThanOrEqualTo(DemoTraffic.TARGET_IN_FLIGHT);
		for (String email : new String[] { "emma@demo.kren", "minji@demo.kren" }) {
			var dashboard = letterService.dashboard(userId(email));
			assertThat(dashboard.incoming()).as(email).isNotEmpty();
			assertThat(dashboard.awaitingReplyCount()).as(email).isPositive();
		}
	}

	@Test
	void penpalsCorrectTemplateLettersButEmmaAndMinjiNeverDo() {
		var template = DemoLetters.ENGLISH_LEARNERS.getFirst();
		long letter = letterService.send(userId("jihoon@demo.kren"), userId("liam@demo.kren"), template.body()).id();
		clock.advance(Duration.ofDays(3));
		for (int i = 0; i < 5; i++) {
			traffic.tick();
			clock.advance(Duration.ofHours(12));
		}
		for (int i = 0; i < 20; i++) {
			traffic.readAndCorrect();
		}

		assertThat(jdbc.queryForObject("SELECT count(*) FROM corrections WHERE letter_id = ?", Long.class, letter))
			.isEqualTo(1);
		assertThat(jdbc.queryForObject("""
				SELECT count(*) FROM corrections c JOIN users u ON u.id = c.corrector_id
				WHERE u.email IN ('emma@demo.kren', 'minji@demo.kren') AND c.created_at > ?
				""", Long.class, Timestamp.from(clock.instant().minus(Duration.ofDays(6))))).isZero();
	}

	@Test
	void prunesOldGeneratedLettersButKeepsTheSeededHistory() {
		long seeded = jdbc.queryForObject("SELECT count(*) FROM letters", Long.class);
		clock.advance(Duration.ofDays(3));
		traffic.tick();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM letters", Long.class)).isGreaterThan(seeded);

		clock.advance(DemoTraffic.KEEP.plusDays(1));
		traffic.prune();

		assertThat(jdbc.queryForObject("SELECT count(*) FROM letters", Long.class)).isEqualTo(seeded);
	}

	@Test
	void topsUpDemoAccountsRunningLow() {
		jdbc.update("UPDATE users SET stamp_balance = 0 WHERE email = 'liam@demo.kren'");

		traffic.topUp();

		assertThat(stampBalance("liam@demo.kren")).isEqualTo(5);
	}

	private long inFlight() {
		return jdbc.queryForObject("SELECT count(*) FROM letters WHERE deliver_at > ?", Long.class,
				Timestamp.from(clock.instant()));
	}

}
