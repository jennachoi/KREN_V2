package com.kren.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.kren.user.UserRepository;

@Service
public class AuthUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public AuthUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return userRepository.findByEmail(AuthService.normalizeEmail(email))
			.map(u -> new AuthUser(u.getId(), u.getEmail(), u.getPasswordHash()))
			.orElseThrow(() -> new UsernameNotFoundException("No user with that email"));
	}

}
