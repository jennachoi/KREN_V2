package com.kren.letter;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.kren.letter.dto.LetterSummary;

/**
 * Lands letters whose flight time is over: marks each arrival as processed and announces it so the
 * recipient gets a live "letter arrived" notification. Safe to run concurrently (see
 * {@link LetterRepository#lockArrivedBatch}).
 */
@Service
public class LetterArrivalDispatcher {

	static final int BATCH_SIZE = 100;

	private final LetterRepository letterRepository;
	private final ApplicationEventPublisher events;
	private final Clock clock;
	private final TransactionTemplate tx;

	public LetterArrivalDispatcher(LetterRepository letterRepository, ApplicationEventPublisher events, Clock clock,
			PlatformTransactionManager transactionManager) {
		this.letterRepository = letterRepository;
		this.events = events;
		this.clock = clock;
		this.tx = new TransactionTemplate(transactionManager);
	}

	/**
	 * Processes one batch in its own transaction and returns the ids it landed (empty when nothing is
	 * due). The transaction is opened explicitly rather than with @Transactional because
	 * {@link #dispatchAllDue} calls this from inside the class, which would bypass Spring's proxy.
	 */
	public List<Long> dispatchBatch() {
		return tx.execute(status -> landBatch());
	}

	private List<Long> landBatch() {
		Instant now = clock.instant();
		List<Long> ids = letterRepository.lockArrivedBatch(now, BATCH_SIZE);
		if (ids.isEmpty()) {
			return ids;
		}
		letterRepository.markArrivalNotified(ids, now);
		for (Letter letter : letterRepository.findByIdIn(ids)) {
			long recipientId = letter.getRecipient().getId();
			events.publishEvent(new LetterArrivedEvent(recipientId, LetterSummary.from(letter, recipientId, now)));
		}
		return ids;
	}

	/** Drains everything that's due, batch by batch. */
	public int dispatchAllDue() {
		int total = 0;
		List<Long> batch;
		do {
			batch = dispatchBatch();
			total += batch.size();
		}
		while (batch.size() == BATCH_SIZE);
		return total;
	}

}
