package com.kren.auth;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kren.auth.dto.SignupRequest;
import com.kren.common.ApiException;
import com.kren.common.ErrorCode;
import com.kren.user.User;
import com.kren.user.UserRepository;
import com.kren.user.UserService;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User signup(SignupRequest req) {
		UserService.requireDifferentLanguages(req.nativeLang(), req.learningLang());
		String email = normalizeEmail(req.email());
		// Fast path for the common case; the unique constraint below is what actually guarantees it.
		if (userRepository.existsByEmail(email)) {
			throw new ApiException(ErrorCode.EMAIL_TAKEN);
		}
		User user = new User(email, passwordEncoder.encode(req.password()), req.displayName().strip(),
				req.country(), req.nativeLang(), req.learningLang());
		try {
			return userRepository.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException e) {
			// Two concurrent signups with the same email: the loser hits uq_users_email.
			throw new ApiException(ErrorCode.EMAIL_TAKEN);
		}
	}

	static String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

}
