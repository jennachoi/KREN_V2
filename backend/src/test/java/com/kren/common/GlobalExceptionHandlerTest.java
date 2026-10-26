package com.kren.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void wrongPasswordIsA401() {
		var response = handler.handleAuthenticationException(new BadCredentialsException("bad"));
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getBody().getProperties()).containsEntry("code", "BAD_CREDENTIALS");
	}

	/** Found when the dev database was down: login answered "invalid email or password". */
	@Test
	void failingUserLookupIsA500NotBadCredentials() {
		var response = handler.handleAuthenticationServiceFailure(
				new InternalAuthenticationServiceException("Connection is not available"));
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().getProperties()).containsEntry("code", "INTERNAL_ERROR");
	}

}
