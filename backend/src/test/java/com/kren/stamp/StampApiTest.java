package com.kren.stamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import com.kren.IntegrationTestSupport;
import com.kren.common.ApiException;
import com.kren.common.ErrorCode;

class StampApiTest extends IntegrationTestSupport {

	@Autowired
	StampService stampService;

	@Test
	void signupGrantsBonusWithLedgerEntry() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");

		mvc.perform(get("/api/users/me").session(me))
			.andExpect(jsonPath("$.stampBalance").value(StampService.SIGNUP_BONUS));
		mvc.perform(get("/api/stamps/ledger").session(me))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].reason").value(contains("SIGNUP")))
			.andExpect(jsonPath("$.content[0].amount").value(StampService.SIGNUP_BONUS));
	}

	@Test
	void checkInOncePerDay() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");

		mvc.perform(post("/api/stamps/checkin").with(xsrf()).session(me))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.balance").value(StampService.SIGNUP_BONUS + 1));
		mvc.perform(post("/api/stamps/checkin").with(xsrf()).session(me))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_CHECKED_IN"));

		assertThat(stampBalance("mina@example.com")).isEqualTo(StampService.SIGNUP_BONUS + 1);
	}

	/** Double-clicking "check in" from several tabs at once must still pay out exactly once. */
	@Test
	void concurrentCheckInsPayOutExactlyOnce() throws Exception {
		signup("mina@example.com", "ko", "en");
		long userId = userId("mina@example.com");
		int threads = 8;
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
			for (int i = 0; i < threads; i++) {
				results.add(pool.submit(() -> {
					start.await();
					try {
						stampService.checkIn(userId);
						return true;
					}
					catch (ApiException e) {
						assertThat(e.errorCode()).isEqualTo(ErrorCode.ALREADY_CHECKED_IN);
						return false;
					}
				}));
			}
			start.countDown();
			long successes = 0;
			for (Future<Boolean> r : results) {
				if (r.get()) {
					successes++;
				}
			}
			assertThat(successes).isEqualTo(1);
		}
		assertThat(stampBalance("mina@example.com")).isEqualTo(StampService.SIGNUP_BONUS + 1);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM stamp_ledger WHERE reason = 'CHECKIN'", Long.class))
			.isEqualTo(1);
	}

}
