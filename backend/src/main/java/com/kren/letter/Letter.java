package com.kren.letter;

import java.time.Instant;

import com.kren.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "letters")
public class Letter {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sender_id", nullable = false, updatable = false)
	private User sender;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_id", nullable = false, updatable = false)
	private User recipient;

	/** The letter this one replies to, or null for a first letter. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id", updatable = false)
	private Letter parent;

	@Column(nullable = false, length = 5000, updatable = false)
	private String body;

	@Column(name = "sent_at", nullable = false, updatable = false)
	private Instant sentAt;

	@Column(name = "read_at")
	private Instant readAt;

	protected Letter() {
	}

	public Letter(User sender, User recipient, Letter parent, String body) {
		this.sender = sender;
		this.recipient = recipient;
		this.parent = parent;
		this.body = body;
		this.sentAt = Instant.now();
	}

	public boolean isParticipant(long userId) {
		return sender.getId() == userId || recipient.getId() == userId;
	}

	/** The person on the other side of this letter from {@code userId}'s point of view. */
	public User counterpartOf(long userId) {
		return sender.getId() == userId ? recipient : sender;
	}

	/** Read receipt: only the recipient opening it counts, and only the first time. */
	public void markReadBy(long userId, Instant now) {
		if (readAt == null && recipient.getId() == userId) {
			readAt = now;
		}
	}

	public Long getId() {
		return id;
	}

	public User getSender() {
		return sender;
	}

	public User getRecipient() {
		return recipient;
	}

	public Letter getParent() {
		return parent;
	}

	public String getBody() {
		return body;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public Instant getReadAt() {
		return readAt;
	}

}
