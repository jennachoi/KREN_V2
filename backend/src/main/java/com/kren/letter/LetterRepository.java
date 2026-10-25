package com.kren.letter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Served by ix_letters_inbox / ix_letters_outbox. The entity graph loads the other party in the same
// query so a page of letters doesn't trigger one user lookup per row.
public interface LetterRepository extends JpaRepository<Letter, Long> {

	@EntityGraph(attributePaths = "sender")
	Page<Letter> findByRecipient_IdOrderBySentAtDescIdDesc(long recipientId, Pageable pageable);

	@EntityGraph(attributePaths = "recipient")
	Page<Letter> findBySender_IdOrderBySentAtDescIdDesc(long senderId, Pageable pageable);

	/**
	 * Claims letters that have landed but whose arrival hasn't been processed. FOR UPDATE SKIP LOCKED
	 * lets several dispatchers (threads or app instances) run at once: each locks a disjoint batch and
	 * skips rows another one already holds, so every arrival is handled exactly once.
	 */
	@Query(nativeQuery = true, value = """
			SELECT id FROM letters
			WHERE arrival_notified_at IS NULL AND deliver_at <= :now
			ORDER BY deliver_at
			LIMIT :limit
			FOR UPDATE SKIP LOCKED
			""")
	List<Long> lockArrivedBatch(@Param("now") Instant now, @Param("limit") int limit);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(nativeQuery = true, value = "UPDATE letters SET arrival_notified_at = :now WHERE id IN (:ids)")
	int markArrivalNotified(@Param("ids") Collection<Long> ids, @Param("now") Instant now);

	@EntityGraph(attributePaths = { "sender", "recipient" })
	List<Letter> findByIdIn(Collection<Long> ids);

	/** Letters to or from me that are still in the air, soonest arrival first. */
	@EntityGraph(attributePaths = { "sender", "recipient" })
	@Query("""
			SELECT l FROM Letter l
			WHERE (l.sender.id = :me OR l.recipient.id = :me) AND l.deliverAt > :now
			ORDER BY l.deliverAt ASC, l.id ASC
			""")
	List<Letter> findInFlight(@Param("me") long me, @Param("now") Instant now, Pageable pageable);

	/** Letters that reached me and that I haven't answered (no reply of mine points at them). */
	@EntityGraph(attributePaths = "sender")
	@Query(value = """
			SELECT l FROM Letter l
			WHERE l.recipient.id = :me AND l.deliverAt <= :now
			  AND NOT EXISTS (SELECT 1 FROM Letter r WHERE r.parent = l AND r.sender.id = :me)
			ORDER BY l.deliverAt DESC, l.id DESC
			""", countQuery = """
			SELECT count(l) FROM Letter l
			WHERE l.recipient.id = :me AND l.deliverAt <= :now
			  AND NOT EXISTS (SELECT 1 FROM Letter r WHERE r.parent = l AND r.sender.id = :me)
			""")
	Page<Letter> findAwaitingReply(@Param("me") long me, @Param("now") Instant now, Pageable pageable);

	@Query(nativeQuery = true, value = "SELECT EXISTS (SELECT 1 FROM corrections WHERE letter_id = :letterId)")
	boolean hasCorrection(@Param("letterId") long letterId);

}
