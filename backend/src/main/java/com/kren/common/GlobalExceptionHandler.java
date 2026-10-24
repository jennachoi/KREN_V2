package com.kren.common;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every error leaves the API as an RFC 9457 {@link ProblemDetail} with a machine-readable
 * {@code code} property, plus {@code errors} for validation failures. Security filters route their
 * 401/403s here too (see {@code SecurityConfig}), so clients only ever see one shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	public record FieldErrorItem(String field, String message) {
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
		return problem(ex.errorCode(), ex.getMessage(), null);
	}

	/** Only the login endpoint authenticates explicitly, so any failure here is bad credentials. */
	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException ex) {
		return problem(ErrorCode.BAD_CREDENTIALS, ErrorCode.BAD_CREDENTIALS.defaultMessage(), null);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return problem(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), null);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldErrorItem> errors = new ArrayList<>();
		for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
			errors.add(new FieldErrorItem(fe.getField(), fe.getDefaultMessage()));
		}
		ex.getBindingResult().getGlobalErrors()
			.forEach(ge -> errors.add(new FieldErrorItem(ge.getObjectName(), ge.getDefaultMessage())));
		return asObject(problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultMessage(), errors));
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldErrorItem> errors = new ArrayList<>();
		ex.getParameterValidationResults().forEach(result -> {
			String name = result.getMethodParameter().getParameterName();
			result.getResolvableErrors().forEach(err -> errors.add(new FieldErrorItem(name, err.getDefaultMessage())));
		});
		return asObject(problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultMessage(), errors));
	}

	/**
	 * Framework-raised errors (malformed JSON, 404, 405 ...) get a {@code code} derived from the status.
	 * Hooked here rather than in handleExceptionInternal because for many of them the ProblemDetail
	 * body is only created after that method runs.
	 */
	@Override
	protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		if (body instanceof ProblemDetail pd && (pd.getProperties() == null || !pd.getProperties().containsKey("code"))) {
			HttpStatus resolved = HttpStatus.resolve(statusCode.value());
			pd.setProperty("code", resolved != null ? resolved.name() : String.valueOf(statusCode.value()));
		}
		return super.createResponseEntity(body, headers, statusCode, request);
	}

	private static ResponseEntity<ProblemDetail> problem(ErrorCode code, String message,
			@Nullable List<FieldErrorItem> errors) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.status(), message);
		pd.setProperty("code", code.name());
		if (errors != null) {
			pd.setProperty("errors", errors);
		}
		return ResponseEntity.status(code.status()).body(pd);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static ResponseEntity<Object> asObject(ResponseEntity<ProblemDetail> response) {
		return (ResponseEntity) response;
	}

}
