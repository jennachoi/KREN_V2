package com.kren.letter;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kren.auth.AuthUser;
import com.kren.common.PageResponse;
import com.kren.letter.dto.ConversationSummary;
import com.kren.letter.dto.ConversationThread;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

	private final ConversationService conversationService;

	public ConversationController(ConversationService conversationService) {
		this.conversationService = conversationService;
	}

	@GetMapping
	public PageResponse<ConversationSummary> list(@AuthenticationPrincipal AuthUser me,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return conversationService.list(me.id(), page, size);
	}

	@GetMapping("/{userId}")
	public ConversationThread thread(@AuthenticationPrincipal AuthUser me, @PathVariable long userId,
			@RequestParam(required = false) Long before,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return conversationService.thread(me.id(), userId, before, size);
	}

}
