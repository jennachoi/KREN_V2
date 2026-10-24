package com.kren.correction;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrectionRepository extends JpaRepository<Correction, Long> {

	@EntityGraph(attributePaths = { "corrector", "lines" })
	Optional<Correction> findByLetter_Id(long letterId);

	/** The notebook: corrections other people made to letters I wrote, newest first. */
	@EntityGraph(attributePaths = "corrector")
	@Query(value = "SELECT c FROM Correction c WHERE c.letter.sender.id = :writerId ORDER BY c.createdAt DESC, c.id DESC",
			countQuery = "SELECT count(c) FROM Correction c WHERE c.letter.sender.id = :writerId")
	Page<Correction> findNotebook(@Param("writerId") long writerId, Pageable pageable);

}
