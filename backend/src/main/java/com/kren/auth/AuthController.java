package com.kren.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kren.auth.dto.LoginRequest;
import com.kren.auth.dto.SignupRequest;
import com.kren.user.UserService;
import com.kren.user.dto.MyProfileResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/** Logout is handled by Spring Security's LogoutFilter at {@code POST /api/auth/logout}. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final UserService userService;
	private final AuthenticationManager authenticationManager;
	private final LoginSessionManager loginSessionManager;

	public AuthController(AuthService authService, UserService userService,
			AuthenticationManager authenticationManager, LoginSessionManager loginSessionManager) {
		this.authService = authService;
		this.userService = userService;
		this.authenticationManager = authenticationManager;
		this.loginSessionManager = loginSessionManager;
	}

	/** Creates the account and signs the new user in. */
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public MyProfileResponse signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		long userId = authService.signup(request);
		AuthUser principal = new AuthUser(userId, AuthService.normalizeEmail(request.email()), null);
		Authentication auth = UsernamePasswordAuthenticationToken.authenticated(principal, null,
				principal.getAuthorities());
		loginSessionManager.establish(auth, httpRequest, httpResponse);
		return userService.getMyProfile(userId);
	}

	@PostMapping("/login")
	public MyProfileResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		// Throws BadCredentialsException (-> 401) for unknown email or wrong password alike.
		Authentication auth = authenticationManager
			.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
		loginSessionManager.establish(auth, httpRequest, httpResponse);
		return userService.getMyProfile(((AuthUser) auth.getPrincipal()).id());
	}

}
