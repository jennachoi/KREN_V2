package com.kren.demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;

/**
 * Moves demo letters through time with plain SQL, so the demo can open on conversations that span
 * weeks. Callers run inside a transaction that also uses JPA, so every change flushes first and clears
 * the persistence context afterwards; otherwise a later read would see the cached, unmoved letter.
 */
@Component
class DemoTimeline {

	private final JdbcTemplate jdbc;
	private final EntityManager entityManager;

	DemoTimeline(JdbcTemplate jdbc, EntityManager entityManager) {
		this.jdbc = jdbc;
		this.entityManager = entityManager;
	}

	/** The letter landed {@code hoursAgo} hours ago, after its normal flight time. */
	void landed(long letterId, double hoursAgo) {
		update("""
				UPDATE letters
				SET sent_at = sent_at - (deliver_at - sent_at) - make_interval(secs => ?),
				    deliver_at = deliver_at - (deliver_at - sent_at) - make_interval(secs => ?),
				    arrival_notified_at = deliver_at - (deliver_at - sent_at) - make_interval(secs => ?)
				WHERE id = ?
				""", hoursAgo * 3600, hoursAgo * 3600, hoursAgo * 3600, letterId);
	}

	/** The letter is still flying and has covered {@code progress} (0..1) of its route. */
	void inFlight(long letterId, double progress) {
		update("""
				UPDATE letters
				SET sent_at = sent_at - (deliver_at - sent_at) * ?,
				    deliver_at = deliver_at - (deliver_at - sent_at) * ?
				WHERE id = ?
				""", progress, progress, letterId);
	}

	/** The recipient opened it {@code hoursAfterLanding} hours after it arrived. */
	void read(long letterId, double hoursAfterLanding) {
		update("UPDATE letters SET read_at = deliver_at + make_interval(secs => ?) WHERE id = ?",
				hoursAfterLanding * 3600, letterId);
	}

	/** A correction (and its stamp reward) happened {@code hoursAfterLanding} after the letter arrived. */
	void corrected(long letterId, double hoursAfterLanding) {
		update("""
				UPDATE corrections c SET created_at = l.deliver_at + make_interval(secs => ?)
				FROM letters l WHERE c.letter_id = l.id AND l.id = ?
				""", hoursAfterLanding * 3600, letterId);
	}

	/**
	 * Gives demo accounts a history: members since a few weeks ago, and ledger rows dated by the
	 * letters and corrections they belong to instead of all saying "today".
	 */
	void backdateAccounts() {
		update("""
				UPDATE users SET created_at = created_at - make_interval(days => 30 + (id % 25)::int)
				WHERE email LIKE '%' || ?
				""", DemoCast.DOMAIN);
		update("""
				UPDATE stamp_ledger s SET created_at = u.created_at
				FROM users u WHERE s.user_id = u.id AND u.email LIKE '%' || ? AND s.reason IN ('SIGNUP', 'DEMO_TOPUP')
				""", DemoCast.DOMAIN);
		update("""
				UPDATE stamp_ledger s SET created_at = l.sent_at
				FROM letters l WHERE s.letter_id = l.id AND s.reason = 'SEND_LETTER'
				""");
		update("""
				UPDATE stamp_ledger s SET created_at = c.created_at
				FROM corrections c WHERE s.letter_id = c.letter_id AND s.reason = 'CORRECTION'
				""");
	}

	private void update(String sql, Object... args) {
		entityManager.flush();
		jdbc.update(sql, args);
		entityManager.clear();
	}

}
