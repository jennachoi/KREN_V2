package com.kren.user;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 50)
	private String displayName;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 2)
	private String country;

	@Column(name = "native_lang", nullable = false, length = 8)
	private String nativeLang;

	@Column(name = "learning_lang", nullable = false, length = 8)
	private String learningLang;

	@Column(length = 500)
	private String bio;

	/** An emoji from {@link Avatars}. */
	@Column(length = 16)
	private String avatar;

	/**
	 * Mutated only through conditional SQL updates (see the stamp domain), never via this field, so
	 * concurrent sends cannot overdraw it.
	 */
	@Column(name = "stamp_balance", nullable = false, insertable = false, updatable = false)
	private int stampBalance;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@ManyToMany
	@JoinTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "interest_id"))
	private Set<Interest> interests = new HashSet<>();

	protected User() {
	}

	public User(String email, String passwordHash, String displayName, String country, String nativeLang,
			String learningLang) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.country = country;
		this.nativeLang = nativeLang;
		this.learningLang = learningLang;
		this.avatar = Avatars.random();
		this.createdAt = Instant.now();
	}

	public void changeAvatar(String avatar) {
		this.avatar = avatar;
	}

	public void updateProfile(String displayName, String country, String nativeLang, String learningLang,
			String bio, Set<Interest> interests) {
		this.displayName = displayName;
		this.country = country;
		this.nativeLang = nativeLang;
		this.learningLang = learningLang;
		this.bio = bio;
		this.interests.clear();
		this.interests.addAll(interests);
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getCountry() {
		return country;
	}

	public String getNativeLang() {
		return nativeLang;
	}

	public String getLearningLang() {
		return learningLang;
	}

	public String getBio() {
		return bio;
	}

	public String getAvatar() {
		return avatar;
	}

	public int getStampBalance() {
		return stampBalance;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Set<Interest> getInterests() {
		return interests;
	}

}
