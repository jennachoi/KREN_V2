package com.kren.letter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls for landed letters. Disabled in tests, which drive the dispatcher directly with a fake clock. */
@Component
@ConditionalOnProperty(name = "kren.delivery.scheduler-enabled", havingValue = "true", matchIfMissing = true)
class LetterArrivalScheduler {

	private final LetterArrivalDispatcher dispatcher;

	LetterArrivalScheduler(LetterArrivalDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Scheduled(fixedDelayString = "${kren.delivery.poll-interval:5s}")
	void dispatch() {
		dispatcher.dispatchAllDue();
	}

}
