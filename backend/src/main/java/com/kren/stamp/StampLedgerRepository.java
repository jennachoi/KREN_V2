package com.kren.stamp;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StampLedgerRepository extends JpaRepository<StampLedgerEntry, Long> {

	Page<StampLedgerEntry> findByUserIdOrderByCreatedAtDescIdDesc(long userId, Pageable pageable);

	/**
	 * The {@code daily_checkins} primary key is what makes check-in once-per-day; ON CONFLICT turns a
	 * duplicate into "0 rows" instead of an exception that would abort the transaction.
	 */
	@Modifying
	@Query(nativeQuery = true, value = """
			INSERT INTO daily_checkins (user_id, checkin_date) VALUES (:userId, :date)
			ON CONFLICT DO NOTHING
			""")
	int insertCheckin(@Param("userId") long userId, @Param("date") LocalDate date);

}
