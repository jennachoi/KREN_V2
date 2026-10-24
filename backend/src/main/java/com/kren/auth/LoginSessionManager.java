package com.kren.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Does for the JSON login endpoint what {@code UsernamePasswordAuthenticationFilter} does for form
 * login: rotate the session id and CSRF token (session fixation), then persist the security context
 * to the session explicitly (Spring Security 6+ no longer saves it implicitly).
 */
@Component
public class LoginSessionManager {

	private final SessionAuthenticationStrategy sessionStrategy;
	private final SecurityContextRepository contextRepository;
	private final SecurityContextHolderStrategy holderStrategy = SecurityContextHolder.getContextHolderStrategy();

	public LoginSessionManager(SessionAuthenticationStrategy sessionStrategy,
			SecurityContextRepository contextRepository) {
		this.sessionStrategy = sessionStrategy;
		this.contextRepository = contextRepository;
	}

	public void establish(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
		sessionStrategy.onAuthentication(authentication, request, response);
		// The strategy above rotated the CSRF token; load it now so this response already carries the
		// new XSRF-TOKEN cookie and the SPA's very next POST doesn't 403.
		if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken csrfToken) {
			csrfToken.getToken();
		}
		SecurityContext context = holderStrategy.createEmptyContext();
		context.setAuthentication(authentication);
		holderStrategy.setContext(context);
		contextRepository.saveContext(context, request, response);
	}

}
