package com.kren.stamp;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kren.auth.AuthUser;
import com.kren.common.PageResponse;
import com.kren.stamp.dto.CheckinResponse;
import com.kren.stamp.dto.StampLedgerEntryResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Current balance is on {@code GET /api/users/me}; this exposes how it got there. */
@RestController
@RequestMapping("/api/stamps")
public class StampController {

	private final StampService stampService;

	public StampController(StampService stampService) {
		this.stampService = stampService;
	}

	@PostMapping("/checkin")
	@ResponseStatus(HttpStatus.CREATED)
	public CheckinResponse checkIn(@AuthenticationPrincipal AuthUser me) {
		return stampService.checkIn(me.id());
	}

	@GetMapping("/ledger")
	public PageResponse<StampLedgerEntryResponse> ledger(@AuthenticationPrincipal AuthUser me,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return stampService.ledger(me.id(), page, size);
	}

}
