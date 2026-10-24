package com.kren.user;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	// Penpal matching: their native language is what I'm learning and vice versa (served by
	// ix_users_lang_pair), excluding anyone on either side of a block. Newest members first; id breaks
	// ties so pages stay stable.
	String PENPAL_FROM_WHERE = """
			FROM users u
			WHERE u.native_lang = :learningLang
			  AND u.learning_lang = :nativeLang
			  AND u.id <> :meId
			  AND NOT EXISTS (
			      SELECT 1 FROM blocks b
			      WHERE (b.blocker_id = :meId AND b.blocked_id = u.id)
			         OR (b.blocker_id = u.id AND b.blocked_id = :meId))
			""";

	String HAS_ANY_INTEREST = """
			  AND EXISTS (
			      SELECT 1 FROM user_interests ui
			      WHERE ui.user_id = u.id AND ui.interest_id IN (:interestIds))
			""";

	@Query(nativeQuery = true,
			value = "SELECT u.* " + PENPAL_FROM_WHERE + " ORDER BY u.created_at DESC, u.id DESC",
			countQuery = "SELECT count(*) " + PENPAL_FROM_WHERE)
	Page<User> findPenpals(@Param("meId") long meId, @Param("nativeLang") String nativeLang,
			@Param("learningLang") String learningLang, Pageable pageable);

	@Query(nativeQuery = true,
			value = "SELECT u.* " + PENPAL_FROM_WHERE + HAS_ANY_INTEREST + " ORDER BY u.created_at DESC, u.id DESC",
			countQuery = "SELECT count(*) " + PENPAL_FROM_WHERE + HAS_ANY_INTEREST)
	Page<User> findPenpalsWithAnyInterest(@Param("meId") long meId, @Param("nativeLang") String nativeLang,
			@Param("learningLang") String learningLang, @Param("interestIds") Collection<Long> interestIds,
			Pageable pageable);

	// Stamp balance is only ever changed through these two statements (never via the entity), so
	// the database row lock serialises concurrent changes. clearAutomatically drops any User already
	// loaded in this transaction so callers can't read a stale balance afterwards.

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(nativeQuery = true, value = "UPDATE users SET stamp_balance = stamp_balance + :amount WHERE id = :id")
	int addStamps(@Param("id") long id, @Param("amount") int amount);

	/**
	 * Check-and-deduct in one statement: returns 0 instead of going negative. Under concurrency a
	 * second transaction blocks on the row lock, then Postgres re-checks the WHERE against the
	 * committed balance, so N parallel sends can never spend more than the balance.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(nativeQuery = true,
			value = "UPDATE users SET stamp_balance = stamp_balance - 1 WHERE id = :id AND stamp_balance >= 1")
	int deductOneStamp(@Param("id") long id);

}
