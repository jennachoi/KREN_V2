package com.kren.correction;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One sentence: what the writer wrote, what a native speaker would write, and optionally why. */
@Embeddable
public record CorrectionLine(@Column(nullable = false, length = 1000) String original,
		@Column(nullable = false, length = 1000) String corrected, @Column(length = 300) String note) {
}
