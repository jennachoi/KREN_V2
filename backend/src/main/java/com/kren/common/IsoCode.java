package com.kren.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;
import java.util.Set;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Validates an ISO 3166-1 alpha-2 country ("KR") or ISO 639-1 language ("ko") code. Matching compares
 * these strings for equality, so free-form values like "en-US" or "EN" must be rejected at the edge.
 */
@Documented
@Constraint(validatedBy = IsoCode.Validator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface IsoCode {

	Kind value();

	String message() default "must be a valid ISO code";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	enum Kind {

		COUNTRY(Set.of(Locale.getISOCountries())), LANGUAGE(Set.of(Locale.getISOLanguages()));

		private final Set<String> codes;

		Kind(Set<String> codes) {
			this.codes = codes;
		}

	}

	class Validator implements ConstraintValidator<IsoCode, String> {

		private Kind kind;

		@Override
		public void initialize(IsoCode annotation) {
			this.kind = annotation.value();
		}

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			// null is @NotNull's job
			return value == null || kind.codes.contains(value);
		}

	}

}
