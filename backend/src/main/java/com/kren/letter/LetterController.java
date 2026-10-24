package com.kren.letter;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kren.auth.AuthUser;
import com.kren.common.PageResponse;
import com.kren.letter.dto.LetterResponse;
import com.kren.letter.dto.LetterSummary;
import com.kren.letter.dto.ReplyRequest;
import com.kren.letter.dto.SendLetterRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/letters")
public class LetterController {

	private final LetterService letterService;

	public LetterController(LetterService letterService) {
		this.letterService = letterService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LetterResponse send(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody SendLetterRequest request) {
		return letterService.send(me.id(), request.recipientId(), request.body());
	}

	@PostMapping("/{letterId}/reply")
	@ResponseStatus(HttpStatus.CREATED)
	public LetterResponse reply(@AuthenticationPrincipal AuthUser me, @PathVariable long letterId,
			@Valid @RequestBody ReplyRequest request) {
		return letterService.reply(me.id(), letterId, request.body());
	}

	@GetMapping("/{letterId}")
	public LetterResponse read(@AuthenticationPrincipal AuthUser me, @PathVariable long letterId) {
		return letterService.read(me.id(), letterId);
	}

	@GetMapping("/inbox")
	public PageResponse<LetterSummary> inbox(@AuthenticationPrincipal AuthUser me,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return letterService.inbox(me.id(), page, size);
	}

	@GetMapping("/sent")
	public PageResponse<LetterSummary> sent(@AuthenticationPrincipal AuthUser me,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return letterService.sent(me.id(), page, size);
	}

}
