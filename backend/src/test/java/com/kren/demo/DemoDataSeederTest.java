package com.kren.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.http.MediaType;

import com.kren.IntegrationTestSupport;
import com.kren.auth.AuthService;
import com.kren.letter.LetterService;
import com.kren.user.InterestRepository;
import com.kren.user.UserRepository;
import com.kren.user.UserService;

class DemoDataSeederTest extends IntegrationTestSupport {

	@Autowired AuthService authService;
	@Autowired UserService userService;
	@Autowired LetterService letterService;
	@Autowired UserRepository userRepository;
	@Autowired InterestRepository interestRepository;

	@Test
	void seedsOnceAndDemoAccountsCanLogIn() throws Exception {
		var seeder = new DemoDataSeeder(authService, userService, letterService, userRepository, interestRepository, jdbc);
		seeder.run(new DefaultApplicationArguments());
		seeder.run(new DefaultApplicationArguments()); // e.g. an app restart

		assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email LIKE '%@demo.kren'", Long.class))
			.isEqualTo(6);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM letters", Long.class)).isEqualTo(3);
		// seeded through the real services, so stamps were actually spent
		assertThat(stampBalance("minji@demo.kren")).isEqualTo(4);
		// the Emma/Minji exchange has landed; Liam's letter is still in the air for the tracking map
		assertThat(jdbc.queryForObject("SELECT count(*) FROM letters WHERE deliver_at > ?", Long.class,
				java.sql.Timestamp.from(clock.instant()))).isEqualTo(1);

		mvc.perform(post("/api/auth/login")
				.with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"emma@demo.kren\",\"password\":\"" + DemoDataSeeder.PASSWORD + "\"}"))
			.andExpect(status().isOk());
	}

}
