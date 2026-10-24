package com.kren.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kren.correction.CorrectionReceivedEvent;
import com.kren.letter.LetterArrivedEvent;
import com.kren.letter.LetterSentEvent;

@Component
class LetterNotificationListener {

	private static final Logger log = LoggerFactory.getLogger(LetterNotificationListener.class);

	private final SseHub hub;

	LetterNotificationListener(SseHub hub) {
		this.hub = hub;
	}

	/**
	 * AFTER_COMMIT: a send that rolls back (e.g. out of stamps) never notifies anyone. The letter is
	 * already saved by now, so a push failure is logged and swallowed rather than turning a successful
	 * send into a 500; the recipient still sees it in their inbox.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onLetterSent(LetterSentEvent event) {
		push(event.recipientId(), "incoming", event.letter());
	}

	/** Fired by the arrival dispatcher once a letter lands; same after-commit rules. */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onLetterArrived(LetterArrivedEvent event) {
		push(event.recipientId(), "letter", event.letter());
	}

	/** "Your letter came back corrected." */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onCorrectionReceived(CorrectionReceivedEvent event) {
		push(event.writerId(), "correction", event.correction());
	}

	private void push(long userId, String eventName, Object payload) {
		try {
			hub.sendTo(userId, eventName, payload);
		}
		catch (RuntimeException e) {
			log.warn("Failed to push {} notification to user {}", eventName, userId, e);
		}
	}

}
