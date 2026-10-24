package com.kren.notification;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kren.auth.AuthUser;

/** The SPA opens {@code new EventSource("/api/notifications/stream")}; the session cookie authenticates it. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final SseHub hub;

	public NotificationController(SseHub hub) {
		this.hub = hub;
	}

	@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@AuthenticationPrincipal AuthUser me) {
		return hub.subscribe(me.id());
	}

}
