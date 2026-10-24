package com.kren.correction.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Only the sentences that change are sent; untouched sentences are simply left out. */
public record CorrectionRequest(@NotEmpty @Size(max = 50) List<@Valid Line> lines, @Size(max = 1000) String comment) {

	public record Line(@NotBlank @Size(max = 1000) String original, @NotBlank @Size(max = 1000) String corrected,
			@Size(max = 300) String note) {
	}

}
