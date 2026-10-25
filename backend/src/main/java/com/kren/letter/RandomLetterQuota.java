package com.kren.letter;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Per-user, per-day counter for random letters (table random_letter_quota). */
@Repository
public class RandomLetterQuota {

	private final JdbcTemplate jdbc;

	public RandomLetterQuota(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Uses one of today's slots if any are left. A single statement: the first send of the day inserts
	 * the row, later ones increment it only while it's under the limit. Concurrent callers serialise on
	 * the row, so the limit holds without any application-side check-then-act.
	 *
	 * @return false when today's limit is already used up
	 */
	public boolean tryConsume(long userId, LocalDate day, int limit) {
		int changed = jdbc.update("""
				INSERT INTO random_letter_quota (user_id, day, used) VALUES (?, ?, 1)
				ON CONFLICT (user_id, day) DO UPDATE SET used = random_letter_quota.used + 1
				WHERE random_letter_quota.used < ?
				""", userId, day, limit);
		return changed == 1;
	}

	public int usedOn(long userId, LocalDate day) {
		Integer used = jdbc.query("SELECT used FROM random_letter_quota WHERE user_id = ? AND day = ?",
				rs -> rs.next() ? rs.getInt(1) : 0, userId, day);
		return used == null ? 0 : used;
	}

}
