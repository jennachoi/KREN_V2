package com.kren.auth;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.kren.common.ApiException;
import com.kren.common.ErrorCode;

/**
 * Session-cookie auth for a same-origin SPA. CSRF uses the double-submit cookie pattern
 * ({@code XSRF-TOKEN} cookie, echoed back in the {@code X-XSRF-TOKEN} header). Everything not
 * explicitly opened is denied; GETs outside /api and /actuator serve the bundled SPA.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository,
			SecurityContextRepository securityContextRepository,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/interests", "/api/avatars", "/actuator/health").permitAll()
				.requestMatchers("/error").permitAll()
				.requestMatchers("/api/**").authenticated()
				.requestMatchers("/actuator/**").denyAll()
				// The React app: static assets and client-side routes (see SpaConfig). Public on purpose;
				// the data behind them is all under /api.
				.requestMatchers(HttpMethod.GET, "/**").permitAll()
				.anyRequest().denyAll())
			.csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
			.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
			.securityContext(ctx -> ctx.securityContextRepository(securityContextRepository))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(logout -> logout
				.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
			// Hand filter-level failures to the MVC exception handler so they get the same JSON shape.
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint((req, res, e) -> exceptionResolver.resolveException(req, res, null,
						new ApiException(ErrorCode.UNAUTHENTICATED)))
				.accessDeniedHandler((req, res, e) -> exceptionResolver.resolveException(req, res, null,
						new ApiException(e instanceof CsrfException ? ErrorCode.CSRF_INVALID : ErrorCode.FORBIDDEN))));
		return http.build();
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository() {
		return CookieCsrfTokenRepository.withHttpOnlyFalse();
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
		return new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(),
				new CsrfAuthenticationStrategy(csrfTokenRepository)));
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	/** Delegating encoder: hashes are stored as {@code {bcrypt}...} so the algorithm can change later. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
