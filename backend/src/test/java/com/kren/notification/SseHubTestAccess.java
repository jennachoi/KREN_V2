package com.kren.notification;

import org.springframework.stereotype.Component;

/** Lets tests outside this package close leftover SSE streams (SseHub#shutdown is package-private). */
@Component
public class SseHubTestAccess {

	private final SseHub hub;

	SseHubTestAccess(SseHub hub) {
		this.hub = hub;
	}

	public void closeAll() {
		hub.shutdown();
	}

}
