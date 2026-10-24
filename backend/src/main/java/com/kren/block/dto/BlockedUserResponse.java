package com.kren.block.dto;

import java.time.Instant;

import com.kren.block.Block;
import com.kren.user.dto.UserSummary;

public record BlockedUserResponse(UserSummary user, Instant blockedAt) {

	public static BlockedUserResponse from(Block block) {
		return new BlockedUserResponse(UserSummary.from(block.getBlocked()), block.getCreatedAt());
	}

}
