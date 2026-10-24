package com.kren.block;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kren.auth.AuthUser;
import com.kren.block.dto.BlockedUserResponse;
import com.kren.common.PageResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** PUT/DELETE on the blocked user's id: both are idempotent, so retries and double-clicks are safe. */
@RestController
@RequestMapping("/api/blocks")
public class BlockController {

	private final BlockService blockService;

	public BlockController(BlockService blockService) {
		this.blockService = blockService;
	}

	@PutMapping("/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void block(@AuthenticationPrincipal AuthUser me, @PathVariable long userId) {
		blockService.block(me.id(), userId);
	}

	@DeleteMapping("/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unblock(@AuthenticationPrincipal AuthUser me, @PathVariable long userId) {
		blockService.unblock(me.id(), userId);
	}

	@GetMapping
	public PageResponse<BlockedUserResponse> list(@AuthenticationPrincipal AuthUser me,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return blockService.list(me.id(), page, size);
	}

}
