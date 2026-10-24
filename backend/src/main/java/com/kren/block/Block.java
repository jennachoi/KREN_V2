package com.kren.block;

import java.time.Instant;

import com.kren.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/** Read-side mapping; rows are written with an idempotent INSERT in {@link BlockRepository}. */
@Entity
@Table(name = "blocks")
public class Block {

	@EmbeddedId
	private BlockId id;

	@MapsId("blockedId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "blocked_id")
	private User blocked;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Block() {
	}

	public BlockId getId() {
		return id;
	}

	public User getBlocked() {
		return blocked;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
