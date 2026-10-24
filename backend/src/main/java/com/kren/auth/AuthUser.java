package com.kren.auth;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal stored in the HTTP session. Holds only the user id and email; controllers receive it via
 * {@code @AuthenticationPrincipal} and services always reload the {@code User} by id.
 */
public final class AuthUser implements UserDetails, CredentialsContainer, Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

	private final long id;
	private final String email;
	private String passwordHash;

	public AuthUser(long id, String email, String passwordHash) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
	}

	public long id() {
		return id;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return AUTHORITIES;
	}

	/** Called by ProviderManager after login so the hash never ends up in the session. */
	@Override
	public void eraseCredentials() {
		this.passwordHash = null;
	}

}
