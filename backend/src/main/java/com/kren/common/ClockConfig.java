package com.kren.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Injected wherever "today" matters (daily check-in) so tests can pin the date. */
@Configuration
public class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
