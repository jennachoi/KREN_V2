package com.kren.block;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Composite primary key (blocker_id, blocked_id): a pair can only be blocked once. */
@Embeddable
public record BlockId(@Column(name = "blocker_id") Long blockerId, @Column(name = "blocked_id") Long blockedId)
		implements Serializable {
}
