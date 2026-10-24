package com.kren.correction.dto;

import java.time.Instant;
import java.util.List;

import com.kren.correction.Correction;
import com.kren.correction.CorrectionLine;
import com.kren.user.dto.UserSummary;

public record CorrectionResponse(Long id, Long letterId, UserSummary corrector, String comment, Instant createdAt,
		List<CorrectionLine> lines) {

	public static CorrectionResponse from(Correction c) {
		return new CorrectionResponse(c.getId(), c.getLetter().getId(), UserSummary.from(c.getCorrector()),
				c.getComment(), c.getCreatedAt(), List.copyOf(c.getLines()));
	}

}
