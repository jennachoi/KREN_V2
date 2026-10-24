package com.kren.stamp.dto;

import java.time.Instant;

import com.kren.stamp.StampLedgerEntry;
import com.kren.stamp.StampReason;

public record StampLedgerEntryResponse(Long id, int amount, StampReason reason, Long letterId, Instant createdAt) {

	public static StampLedgerEntryResponse from(StampLedgerEntry e) {
		return new StampLedgerEntryResponse(e.getId(), e.getAmount(), e.getReason(), e.getLetterId(), e.getCreatedAt());
	}

}
