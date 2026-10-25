package com.kren.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
	LANGUAGES_MUST_DIFFER(HttpStatus.BAD_REQUEST, "Native and learning language must differ"),
	INVALID_INTEREST(HttpStatus.BAD_REQUEST, "One or more interest ids do not exist"),
	CANNOT_SEND_TO_SELF(HttpStatus.BAD_REQUEST, "You cannot send a letter to yourself"),
	CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "You cannot block yourself"),
	INVALID_AVATAR(HttpStatus.BAD_REQUEST, "Pick an avatar from the list"),
	INVALID_CORRECTION(HttpStatus.BAD_REQUEST, "Each corrected sentence must come from the letter and actually change it"),
	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required"),
	BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
	CSRF_INVALID(HttpStatus.FORBIDDEN, "CSRF token missing or invalid"),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
	LETTER_NOT_FOUND(HttpStatus.NOT_FOUND, "Letter not found"),
	NO_RANDOM_MATCH(HttpStatus.NOT_FOUND, "No new penpals to send a random letter to right now"),
	CORRECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "This letter hasn't been corrected yet"),
	EMAIL_TAKEN(HttpStatus.CONFLICT, "Email is already registered"),
	INSUFFICIENT_STAMPS(HttpStatus.CONFLICT, "Not enough stamps to send a letter"),
	ALREADY_CHECKED_IN(HttpStatus.CONFLICT, "Already checked in today"),
	LETTER_IN_TRANSIT(HttpStatus.CONFLICT, "This letter hasn't arrived yet"),
	RANDOM_LIMIT_REACHED(HttpStatus.CONFLICT, "You've sent all your random letters for today"),
	ALREADY_CORRECTED(HttpStatus.CONFLICT, "This letter has already been corrected"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");

	private final HttpStatus status;
	private final String defaultMessage;

	ErrorCode(HttpStatus status, String defaultMessage) {
		this.status = status;
		this.defaultMessage = defaultMessage;
	}

	public HttpStatus status() {
		return status;
	}

	public String defaultMessage() {
		return defaultMessage;
	}

}
