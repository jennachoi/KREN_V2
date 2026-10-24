package com.kren.stamp;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Append-only: rows are inserted, never updated. {@code users.stamp_balance} is the running total. */
@Entity
@Table(name = "stamp_ledger")
public class StampLedgerEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Column(nullable = false, updatable = false)
	private int amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20, updatable = false)
	private StampReason reason;

	@Column(name = "letter_id", updatable = false)
	private Long letterId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected StampLedgerEntry() {
	}

	public StampLedgerEntry(long userId, int amount, StampReason reason, Long letterId) {
		this.userId = userId;
		this.amount = amount;
		this.reason = reason;
		this.letterId = letterId;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public int getAmount() {
		return amount;
	}

	public StampReason getReason() {
		return reason;
	}

	public Long getLetterId() {
		return letterId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
