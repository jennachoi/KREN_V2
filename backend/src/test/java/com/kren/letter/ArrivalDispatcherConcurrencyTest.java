package com.kren.letter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

import com.kren.IntegrationTestSupport;

/**
 * Several dispatchers (think: several app instances) poll at the same moment. With FOR UPDATE SKIP
 * LOCKED each landed letter must be claimed by exactly one of them: no letter missed, none twice
 * (which would mean a duplicate "your letter arrived" notification).
 */
class ArrivalDispatcherConcurrencyTest extends IntegrationTestSupport {

	static final int LETTERS = 450; // several batches' worth
	static final int DISPATCHERS = 4;

	@Autowired
	LetterArrivalDispatcher dispatcher;

	@RepeatedTest(3)
	void concurrentDispatchersPartitionTheWork() throws Exception {
		signup("mina@example.com", "ko", "en", "KR");
		signup("alex@example.com", "en", "ko", "US");
		long minaId = userId("mina@example.com");
		long alexId = userId("alex@example.com");
		// Letters that landed an hour ago and haven't been processed (inserted directly: no stamps involved).
		jdbc.update("""
				INSERT INTO letters (sender_id, recipient_id, body, sent_at, origin_country, destination_country,
				                     distance_km, deliver_at)
				SELECT ?, ?, 'letter ' || g, now() - interval '25 hours', 'KR', 'US', 11000, now() - interval '1 hour'
				FROM generate_series(1, ?) g
				""", minaId, alexId, LETTERS);

		CountDownLatch start = new CountDownLatch(1);
		List<Future<List<Long>>> results = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(DISPATCHERS)) {
			for (int i = 0; i < DISPATCHERS; i++) {
				results.add(pool.submit(() -> {
					start.await();
					List<Long> mine = new ArrayList<>();
					List<Long> batch;
					do {
						batch = dispatcher.dispatchBatch();
						mine.addAll(batch);
					}
					while (!batch.isEmpty());
					return mine;
				}));
			}
			start.countDown();

			List<Long> all = new ArrayList<>();
			for (Future<List<Long>> r : results) {
				all.addAll(r.get());
			}
			Set<Long> unique = new HashSet<>(all);
			assertThat(all).as("no letter claimed twice").hasSize(unique.size());
			assertThat(unique).as("no letter missed").hasSize(LETTERS);
		}
	}

}
