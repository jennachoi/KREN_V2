package com.kren.letter;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "origin_country", nullable = false, length = 2, updatable = false)
	private String originCountry;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "destination_country", nullable = false, length = 2, updatable = false)
	private String destinationCountry;

	@Column(name = "distance_km", updatable = false)
	private Integer distanceKm;

	/** The letter is in flight until this instant; before it the recipient can't read it. */
	@Column(name = "deliver_at", nullable = false, updatable = false)
	private Instant deliverAt;

	protected Letter() {
	}

	public Letter(User sender, User recipient, Letter parent, String body, Instant sentAt, Integer distanceKm,
			Instant deliverAt) {
		this.sender = sender;
		this.recipient = recipient;
		this.parent = parent;
		this.body = body;
		this.sentAt = sentAt;
		this.originCountry = sender.getCountry();
		this.destinationCountry = recipient.getCountry();
		this.distanceKm = distanceKm;
		this.deliverAt = deliverAt;
	}

	public boolean isDeliveredAt(Instant now) {
		return !now.isBefore(deliverAt);
	}

	public boolean isRecipient(long userId) {
		return recipient.getId() == userId;
	}

	/** The recipient can't see what's inside until the letter has landed. */
	public boolean isSealedFor(long userId, Instant now) {
		return isRecipient(userId) && !isDeliveredAt(now);
	}

	public boolean isParticipant(long userId) {
		return sender.getId() == userId || recipient.getId() == userId;
	}

	/** The person on the other side of this letter from {@code userId}'s point of view. */
	public User counterpartOf(long userId) {
		return sender.getId() == userId ? recipient : sender;
	}

	/** Read receipt: only the recipient opening a delivered letter counts, and only the first time. */
	public void markReadBy(long userId, Instant now) {
		if (readAt == null && isRecipient(userId) && isDeliveredAt(now)) {
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

	public String getOriginCountry() {
		return originCountry;
	}

	public String getDestinationCountry() {
		return destinationCountry;
	}

	public Integer getDistanceKm() {
		return distanceKm;
	}

	public Instant getDeliverAt() {
		return deliverAt;
	}

}
