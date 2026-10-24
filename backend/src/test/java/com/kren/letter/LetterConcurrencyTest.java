package com.kren.letter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

import com.kren.IntegrationTestSupport;
import com.kren.common.ApiException;
import com.kren.common.ErrorCode;

/**
 * The scenario the stamp design exists for: a user with B stamps fires N > B sends at the same
 * instant (double-click, several tabs, a script). Exactly B letters may go out, the balance must end
 * at 0 (never negative), and every spent stamp must have exactly one letter and one ledger row.
 */
class LetterConcurrencyTest extends IntegrationTestSupport {

	static final int BALANCE = 3;
	static final int PARALLEL_SENDS = 10;

	@Autowired
	LetterService letterService;

	@RepeatedTest(3)
	void parallelSendsNeverOverspend() throws Exception {
		signup("mina@example.com", "ko", "en");
		signup("alex@example.com", "en", "ko");
		long minaId = userId("mina@example.com");
		long alexId = userId("alex@example.com");
		setStampBalance("mina@example.com", BALANCE);

		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_SENDS)) {
			for (int i = 0; i < PARALLEL_SENDS; i++) {
				int n = i;
				results.add(pool.submit(() -> {
					start.await(); // release every thread at once to maximise overlap
					try {
						letterService.send(minaId, alexId, "letter " + n);
						return true;
					}
					catch (ApiException e) {
						assertThat(e.errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STAMPS);
						return false;
					}
				}));
			}
			start.countDown();

			long sent = 0;
			for (Future<Boolean> r : results) {
				if (r.get()) {
					sent++;
				}
			}
			assertThat(sent).isEqualTo(BALANCE);
		}

		assertThat(stampBalance("mina@example.com")).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM letters WHERE sender_id = ?", Long.class, minaId))
			.isEqualTo(BALANCE);
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM stamp_ledger WHERE user_id = ? AND reason = 'SEND_LETTER'", Long.class, minaId))
			.isEqualTo(BALANCE);
	}

}
