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

	@Query(nativeQuery = true, value = "SELECT EXISTS (SELECT 1 FROM corrections WHERE letter_id = :letterId)")
	boolean hasCorrection(@Param("letterId") long letterId);

}
