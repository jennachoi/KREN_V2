package com.kren.letter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

// Served by ix_letters_inbox / ix_letters_outbox. The entity graph loads the other party in the same
// query so a page of letters doesn't trigger one user lookup per row.
public interface LetterRepository extends JpaRepository<Letter, Long> {

	@EntityGraph(attributePaths = "sender")
	Page<Letter> findByRecipient_IdOrderBySentAtDescIdDesc(long recipientId, Pageable pageable);

	@EntityGraph(attributePaths = "recipient")
	Page<Letter> findBySender_IdOrderBySentAtDescIdDesc(long senderId, Pageable pageable);

}
