package com.kren.correction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.kren.letter.Letter;
import com.kren.user.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "corrections")
public class Correction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "letter_id", nullable = false, updatable = false)
	private Letter letter;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "corrector_id", nullable = false, updatable = false)
	private User corrector;

	@Column(length = 1000)
	private String comment;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@ElementCollection
	@CollectionTable(name = "correction_lines", joinColumns = @JoinColumn(name = "correction_id"))
	@OrderColumn(name = "line_no")
	private List<CorrectionLine> lines = new ArrayList<>();

	protected Correction() {
	}

	public Correction(Letter letter, User corrector, String comment, List<CorrectionLine> lines, Instant createdAt) {
		this.letter = letter;
		this.corrector = corrector;
		this.comment = comment;
		this.lines.addAll(lines);
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Letter getLetter() {
		return letter;
	}

	public User getCorrector() {
		return corrector;
	}

	public String getComment() {
		return comment;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<CorrectionLine> getLines() {
		return lines;
	}

}
