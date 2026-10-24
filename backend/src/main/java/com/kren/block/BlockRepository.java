package com.kren.block;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlockRepository extends JpaRepository<Block, BlockId> {

	/** Blocking hides people from each other, so a block in either direction counts. */
	@Query(nativeQuery = true, value = """
			SELECT EXISTS (
			    SELECT 1 FROM blocks
			    WHERE (blocker_id = :a AND blocked_id = :b) OR (blocker_id = :b AND blocked_id = :a))
			""")
	boolean existsBetween(@Param("a") long a, @Param("b") long b);

	/** Idempotent: blocking someone twice (or two requests racing) is a no-op, not a PK violation. */
	@Modifying
	@Query(nativeQuery = true, value = """
			INSERT INTO blocks (blocker_id, blocked_id) VALUES (:blockerId, :blockedId)
			ON CONFLICT DO NOTHING
			""")
	int insertIfAbsent(@Param("blockerId") long blockerId, @Param("blockedId") long blockedId);

	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM blocks WHERE blocker_id = :blockerId AND blocked_id = :blockedId")
	int deleteByPair(@Param("blockerId") long blockerId, @Param("blockedId") long blockedId);

	@Query(value = "SELECT b FROM Block b JOIN FETCH b.blocked WHERE b.id.blockerId = :blockerId ORDER BY b.createdAt DESC",
			countQuery = "SELECT count(b) FROM Block b WHERE b.id.blockerId = :blockerId")
	Page<Block> findByBlocker(@Param("blockerId") long blockerId, Pageable pageable);

}
