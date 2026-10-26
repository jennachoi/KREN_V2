package com.kren.letter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Letters grouped by the other person: the mailbox as a list of conversations. */
interface ConversationRepository extends Repository<Letter, Long> {

	/** One row per penpal; numbers are about the letters between me and them. */
	interface ConversationRow {

		Long getPenpalId();

		Long getLastLetterId();

		long getUnread();

		long getAwaiting();

		long getInFlight();

	}

	/**
	 * All my conversations in one query, most recent activity first. Each of my letters is tagged with
	 * the other person and a few flags, then grouped: the newest letter per penpal comes from an
	 * ordered {@code array_agg}, the counts from {@code FILTER} clauses. People I blocked are left out.
	 */
	@Query(nativeQuery = true, value = """
			WITH mine AS (
			  SELECT l.id, l.sent_at,
			         CASE WHEN l.sender_id = :me THEN l.recipient_id ELSE l.sender_id END AS penpal_id,
			         (l.recipient_id = :me AND l.deliver_at <= :now AND l.read_at IS NULL) AS unread,
			         (l.recipient_id = :me AND l.deliver_at <= :now
			            AND NOT EXISTS (SELECT 1 FROM letters r WHERE r.parent_id = l.id AND r.sender_id = :me)) AS awaiting,
			         (l.deliver_at > :now) AS in_flight
			  FROM letters l
			  WHERE l.sender_id = :me OR l.recipient_id = :me
			)
			SELECT penpal_id AS "penpalId",
			       (array_agg(id ORDER BY sent_at DESC, id DESC))[1] AS "lastLetterId",
			       count(*) FILTER (WHERE unread) AS "unread",
			       count(*) FILTER (WHERE awaiting) AS "awaiting",
			       count(*) FILTER (WHERE in_flight) AS "inFlight"
			FROM mine
			WHERE NOT EXISTS (SELECT 1 FROM blocks b WHERE b.blocker_id = :me AND b.blocked_id = mine.penpal_id)
			GROUP BY penpal_id
			ORDER BY max(sent_at) DESC, penpal_id
			""", countQuery = """
			SELECT count(DISTINCT penpal_id) FROM (
			  SELECT CASE WHEN l.sender_id = :me THEN l.recipient_id ELSE l.sender_id END AS penpal_id
			  FROM letters l WHERE l.sender_id = :me OR l.recipient_id = :me
			) mine
			WHERE NOT EXISTS (SELECT 1 FROM blocks b WHERE b.blocker_id = :me AND b.blocked_id = mine.penpal_id)
			""")
	Page<ConversationRow> findConversations(@Param("me") long me, @Param("now") Instant now, Pageable pageable);

	/** The newest letters between two people (served by ix_letters_pair), newest first. */
	@EntityGraph(attributePaths = { "sender", "recipient" })
	@Query("""
			SELECT l FROM Letter l
			WHERE (l.sender.id = :me AND l.recipient.id = :penpal) OR (l.sender.id = :penpal AND l.recipient.id = :me)
			ORDER BY l.sentAt DESC, l.id DESC
			""")
	List<Letter> findThread(@Param("me") long me, @Param("penpal") long penpal, Pageable pageable);

	/**
	 * The page before a given letter (keyset pagination): stable even while new letters arrive, which
	 * an OFFSET would shift.
	 */
	@EntityGraph(attributePaths = { "sender", "recipient" })
	@Query("""
			SELECT l FROM Letter l
			WHERE ((l.sender.id = :me AND l.recipient.id = :penpal) OR (l.sender.id = :penpal AND l.recipient.id = :me))
			  AND (l.sentAt < :sentAt OR (l.sentAt = :sentAt AND l.id < :id))
			ORDER BY l.sentAt DESC, l.id DESC
			""")
	List<Letter> findThreadBefore(@Param("me") long me, @Param("penpal") long penpal, @Param("sentAt") Instant sentAt,
			@Param("id") long id, Pageable pageable);

	@Query(nativeQuery = true, value = "SELECT letter_id FROM corrections WHERE letter_id IN (:ids)")
	Set<Long> findCorrectedAmong(@Param("ids") Collection<Long> ids);

}
