package com.kren.notification;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;

/**
 * Open SSE connections per user (a user may have several tabs). In-memory, so it only reaches users
 * connected to this instance; scaling past one instance would need a broker (Redis pub/sub or
 * Postgres LISTEN/NOTIFY) to fan events out to every node.
 */
@Component
public class SseHub {

	private static final Logger log = LoggerFactory.getLogger(SseHub.class);

	/** Browsers' EventSource reconnects on its own when this expires. */
	static final Duration TIMEOUT = Duration.ofMinutes(30);

	private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

	public SseEmitter subscribe(long userId) {
		SseEmitter emitter = new SseEmitter(TIMEOUT.toMillis());
		emitters.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(emitter);
		Runnable remove = () -> remove(userId, emitter);
		emitter.onCompletion(remove);
		emitter.onTimeout(remove);
		emitter.onError(e -> remove.run());
		// Commits the response immediately so the client knows the stream is live.
		send(userId, emitter, SseEmitter.event().name("connected").data("ok"));
		return emitter;
	}

	public void sendTo(long userId, String eventName, Object data) {
		for (SseEmitter emitter : emitters.getOrDefault(userId, Set.of())) {
			send(userId, emitter, SseEmitter.event().name(eventName).data(data));
		}
	}

	/** Comment line every 25s so proxies and load balancers don't kill an idle connection. */
	@Scheduled(fixedRate = 25_000)
	void heartbeat() {
		emitters.forEach((userId, set) -> set
			.forEach(emitter -> send(userId, emitter, SseEmitter.event().comment("keep-alive"))));
	}

	/** Close every stream cleanly on shutdown so clients reconnect to the next instance right away. */
	@PreDestroy
	void shutdown() {
		emitters.values().forEach(set -> set.forEach(SseEmitter::complete));
		emitters.clear();
	}

	int connectionCount(long userId) {
		return emitters.getOrDefault(userId, Set.of()).size();
	}

	private void send(long userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
		try {
			emitter.send(event);
		}
		catch (IOException | IllegalStateException e) {
			// Client went away (closed tab, network drop); forget it rather than fail the caller.
			log.debug("Dropping SSE connection for user {}: {}", userId, e.toString());
			remove(userId, emitter);
		}
	}

	private void remove(long userId, SseEmitter emitter) {
		emitters.computeIfPresent(userId, (id, set) -> {
			set.remove(emitter);
			return set.isEmpty() ? null : set;
		});
	}

}
