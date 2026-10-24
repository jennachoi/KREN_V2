package com.kren.auth.dto;

import com.kren.common.IsoCode;
import com.kren.common.IsoCode.Kind;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotBlank @Email @Size(max = 255) String email,
		// BCrypt only looks at the first 72 bytes; cap it so long passwords aren't silently truncated.
		@NotNull @Size(min = 8, max = 72) String password,
		@NotBlank @Size(max = 50) String displayName,
		@NotNull @IsoCode(Kind.COUNTRY) String country,
		@NotNull @IsoCode(Kind.LANGUAGE) String nativeLang,
		@NotNull @IsoCode(Kind.LANGUAGE) String learningLang) {
}
