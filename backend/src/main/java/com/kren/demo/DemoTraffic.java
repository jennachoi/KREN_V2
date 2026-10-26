package com.kren.demo;

import java.time.Clock;
import java.time.Duration;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kren.correction.CorrectionService;
import com.kren.letter.LetterService;
import com.kren.letter.dto.Dashboard;
import com.kren.letter.dto.LetterSummary;
import com.kren.stamp.StampService;

/**
 * The demo's postman: every few minutes the demo accounts write, read, correct and answer each other,
 * so the live site always has planes in the air. It runs inside the app on the same server, so it
 * costs nothing extra, and it only ever touches {@code @demo.kren} accounts.
 *
 * <p>Everything goes through the real services. The two login accounts (Emma and Minji) always have a
 * letter coming in; they answer only when their pile of unanswered letters gets long, so visitors
 * still find letters to reply to, and they never correct anything, so that's left to visitors too.
 * Generated letters are deleted after {@link #KEEP}; the seeded history stays.
 */
@Component
@ConditionalOnProperty(name = "kren.demo.traffic.enabled", havingValue = "true")
class DemoTraffic {

	static final int TARGET_IN_FLIGHT = 6;
	/** Emma or Minji answer a letter themselves once more than this many are waiting. */
	static final int AWAITING_BEFORE_REPLY = 3;
	static final Duration KEEP = Duration.ofDays(7);

	private static final Logger log = LoggerFactory.getLogger(DemoTraffic.class);

	record Member(long id, String key, String learningLang) {

		boolean canLogIn() {
			return DemoCast.LOGIN_ACCOUNTS.contains(key);
		}

	}

	private final LetterService letterService;
	private final CorrectionService correctionService;
	private final StampService stampService;
	private final JdbcTemplate jdbc;
	private final Clock clock;

	DemoTraffic(LetterService letterService, CorrectionService correctionService, StampService stampService,
			JdbcTemplate jdbc, Clock clock) {
		this.letterService = letterService;
		this.correctionService = correctionService;
		this.stampService = stampService;
		this.jdbc = jdbc;
		this.clock = clock;
	}

	@Scheduled(initialDelayString = "${kren.demo.traffic.initial-delay:1m}",
			fixedDelayString = "${kren.demo.traffic.interval:3m}")
	void tick() {
		List<Member> members = members();
		if (members.size() < 3) {
			return; // not seeded (yet)
		}
		attempt("prune", this::prune);
		attempt("top up", this::topUp);
		attempt("read and correct", this::readAndCorrect);
		for (Member m : members) {
			if (m.canLogIn()) {
				attempt("keep " + m.key() + " busy", () -> keepBusy(m, members));
			}
		}
		attempt("fill the sky", () -> fillSky(members));
	}

	/** Each step is independent: one failing (say, a race with a visitor) shouldn't stop the rest. */
	private static void attempt(String step, Runnable action) {
		try {
			action.run();
		}
		catch (RuntimeException e) {
			log.warn("Demo traffic: {} failed: {}", step, e.toString());
		}
	}

	void prune() {
		Long seededUpTo = jdbc.query("SELECT value FROM app_settings WHERE key = 'demo_seed_last_letter'",
				rs -> rs.next() ? Long.valueOf(rs.getString(1)) : null);
		jdbc.update("""
				DELETE FROM letters l USING users s, users r
				WHERE l.sender_id = s.id AND l.recipient_id = r.id
				  AND s.email LIKE '%' || ? AND r.email LIKE '%' || ?
				  AND l.id > ? AND l.sent_at < ?
				""", DemoCast.DOMAIN, DemoCast.DOMAIN, seededUpTo == null ? Long.MAX_VALUE : seededUpTo,
				Timestamp.from(clock.instant().minus(KEEP)));
	}

	void topUp() {
		jdbc.queryForList("SELECT id FROM users WHERE email LIKE '%' || ? AND stamp_balance < 2", Long.class,
				DemoCast.DOMAIN)
			.forEach(id -> stampService.topUpDemoAccount(id, 5));
	}

	/** Penpals (not Emma or Minji) open what reached them and correct it when it's a known template. */
	void readAndCorrect() {
		var arrived = jdbc.queryForList("""
				SELECT l.id, l.recipient_id, l.body FROM letters l JOIN users r ON r.id = l.recipient_id
				WHERE r.email LIKE '%' || ? AND r.email NOT IN (?, ?)
				  AND l.deliver_at <= ? AND l.read_at IS NULL
				ORDER BY l.deliver_at LIMIT 3
				""", DemoCast.DOMAIN, loginEmail(0), loginEmail(1), Timestamp.from(clock.instant()));
		for (var row : arrived) {
			long letterId = ((Number) row.get("id")).longValue();
			long recipientId = ((Number) row.get("recipient_id")).longValue();
			letterService.read(recipientId, letterId);
			DemoLetters.Template template = DemoLetters.forBody((String) row.get("body"));
			if (template != null && !template.corrections().isEmpty()) {
				correctionService.correct(recipientId, letterId, template.correction());
			}
		}
	}

	/**
	 * Emma and Minji always have a letter on its way to them, and send one back now and then, so their
	 * home map is never empty.
	 */
	void keepBusy(Member account, List<Member> members) {
		Dashboard dashboard = letterService.dashboard(account.id());
		if (dashboard.incoming().isEmpty()) {
			Member penpal = random(members.stream().filter(m -> !m.canLogIn()).toList());
			LetterSummary theirs = awaitingFrom(penpal, account.id());
			if (theirs != null) {
				letterService.reply(penpal.id(), theirs.id(), DemoLetters.pick(penpal.learningLang()).body());
			}
			else {
				letterService.send(penpal.id(), account.id(), DemoLetters.pick(penpal.learningLang()).body());
			}
		}
		if (dashboard.outgoing().isEmpty() && dashboard.awaitingReplyCount() > AWAITING_BEFORE_REPLY) {
			// the newest one: the seeded conversations further down stay for visitors to answer
			letterService.reply(account.id(), dashboard.awaitingReply().getFirst().id(),
					DemoLetters.pick(account.learningLang()).body());
		}
	}

	/** Penpals write to each other until enough planes are in the air, answering before starting anew. */
	void fillSky(List<Member> members) {
		List<Member> penpals = members.stream().filter(m -> !m.canLogIn()).toList();
		for (int i = 0; i < 2 && inFlight() < TARGET_IN_FLIGHT; i++) {
			Member writer = random(penpals);
			String body = DemoLetters.pick(writer.learningLang()).body();
			List<LetterSummary> awaiting = letterService.dashboard(writer.id()).awaitingReply();
			if (!awaiting.isEmpty()) {
				letterService.reply(writer.id(), awaiting.getFirst().id(), body);
				continue;
			}
			Member to = random(penpals);
			if (to.id() != writer.id()) {
				letterService.send(writer.id(), to.id(), body);
			}
		}
	}

	private LetterSummary awaitingFrom(Member penpal, long fromId) {
		return letterService.dashboard(penpal.id())
			.awaitingReply()
			.stream()
			.filter(l -> l.counterpart().id() == fromId)
			.findFirst()
			.orElse(null);
	}

	private long inFlight() {
		return jdbc.queryForObject("""
				SELECT count(*) FROM letters l JOIN users s ON s.id = l.sender_id
				WHERE s.email LIKE '%' || ? AND l.deliver_at > ?
				""", Long.class, DemoCast.DOMAIN, Timestamp.from(clock.instant()));
	}

	private List<Member> members() {
		return jdbc.query("SELECT id, email, learning_lang FROM users WHERE email LIKE '%' || ? ORDER BY id",
				(rs, n) -> new Member(rs.getLong(1), rs.getString(2).replace(DemoCast.DOMAIN, ""), rs.getString(3)),
				DemoCast.DOMAIN);
	}

	private static String loginEmail(int index) {
		return DemoCast.LOGIN_ACCOUNTS.stream().sorted().toList().get(index) + DemoCast.DOMAIN;
	}

	private static <T> T random(List<T> list) {
		return list.get(ThreadLocalRandom.current().nextInt(list.size()));
	}

}
