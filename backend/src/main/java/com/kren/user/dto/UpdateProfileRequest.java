package com.kren.user.dto;

import java.util.Set;

import com.kren.common.IsoCode;
import com.kren.common.IsoCode.Kind;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Full replacement of the editable profile (PUT semantics). */
public record UpdateProfileRequest(
		@NotBlank @Size(max = 50) String displayName,
		@NotNull @IsoCode(Kind.COUNTRY) String country,
		@NotNull @IsoCode(Kind.LANGUAGE) String nativeLang,
		@NotNull @IsoCode(Kind.LANGUAGE) String learningLang,
		@Size(max = 500) String bio,
		@NotNull @Size(max = 10) Set<@NotNull Long> interestIds) {
}
