package com.kren.correction;

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
import com.kren.correction.dto.CorrectionRequest;
import com.kren.correction.dto.CorrectionResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api")
public class CorrectionController {

	private final CorrectionService correctionService;

	public CorrectionController(CorrectionService correctionService) {
		this.correctionService = correctionService;
	}

	@PostMapping("/letters/{letterId}/correction")
	@ResponseStatus(HttpStatus.CREATED)
	public CorrectionResponse correct(@AuthenticationPrincipal AuthUser me, @PathVariable long letterId,
			@Valid @RequestBody CorrectionRequest request) {
		return correctionService.correct(me.id(), letterId, request);
	}

	@GetMapping("/letters/{letterId}/correction")
	public CorrectionResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable long letterId) {
		return correctionService.get(me.id(), letterId);
	}

	@GetMapping("/corrections/notebook")
	public PageResponse<CorrectionResponse> notebook(@AuthenticationPrincipal AuthUser me,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return correctionService.notebook(me.id(), page, size);
	}

}
