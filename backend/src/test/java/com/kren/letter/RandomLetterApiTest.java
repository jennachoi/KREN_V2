package com.kren.letter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import com.kren.IntegrationTestSupport;
import com.kren.common.ApiException;
import com.kren.common.ErrorCode;

class RandomLetterApiTest extends IntegrationTestSupport {

	@Autowired
	RandomLetterService randomLetterService;

	MockHttpSession mina; // ko -> en
	long minaId;

	@BeforeEach
	void mina() throws Exception {
		mina = signup("mina@example.com", "ko", "en", "KR");
		minaId = userId("mina@example.com");
		setStampBalance("mina@example.com", 20);
	}

	@Test
	void goesToSomeoneWhoseLanguagesMirrorMine() throws Exception {
		signup("alex@example.com", "en", "ko", "US");
		signup("kenji@example.com", "ja", "ko", "JP"); // learns Korean but doesn't speak English
		signup("jun@example.com", "ko", "en", "KR"); // same direction as me

		sendRandom(mina).andExpect(status().isCreated())
			.andExpect(jsonPath("$.recipient.displayName").value("alex"))
			.andExpect(jsonPath("$.delivered").value(false));
		assertThat(stampBalance("mina@example.com")).isEqualTo(19);
	}

	@Test
	void skipsPeopleIAlreadyWriteWithAndBlockedPeople() throws Exception {
		MockHttpSession alex = signup("alex@example.com", "en", "ko", "US");
		signup("liam@example.com", "en", "ko", "CA");
		signup("noah@example.com", "en", "ko", "AU");
		// alex wrote to me already; I blocked liam -> only noah is "new"
		mvc.perform(post("/api/letters").with(xsrf()).session(alex).contentType(MediaType.APPLICATION_JSON)
			.content("{\"recipientId\":%d,\"body\":\"hi\"}".formatted(minaId))).andExpect(status().isCreated());
		block("mina@example.com", "liam@example.com");

		sendRandom(mina).andExpect(jsonPath("$.recipient.displayName").value("noah"));
		sendRandom(mina).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NO_RANDOM_MATCH"));
	}

	@Test
	void noMatchDoesNotUseUpASlot() throws Exception {
		sendRandom(mina).andExpect(status().isNotFound());
		mvc.perform(get("/api/letters/random/quota").session(mina))
			.andExpect(jsonPath("$.used").value(0))
			.andExpect(jsonPath("$.remaining").value(3));
	}

	@Test
	void threePerDayThenResetsTomorrow() throws Exception {
		for (int i = 0; i < 5; i++) {
			signup("p" + i + "@example.com", "en", "ko", "US");
		}
		for (int i = 0; i < 3; i++) {
			sendRandom(mina).andExpect(status().isCreated());
		}
		sendRandom(mina).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RANDOM_LIMIT_REACHED"));
		mvc.perform(get("/api/letters/random/quota").session(mina)).andExpect(jsonPath("$.remaining").value(0));

		clock.advance(Duration.ofDays(1));
		sendRandom(mina).andExpect(status().isCreated());
	}

	/** Six taps at once with three slots left: exactly three letters go out. */
	@RepeatedTest(3)
	void parallelSendsRespectTheDailyLimit() throws Exception {
		for (int i = 0; i < 8; i++) {
			signup("p" + i + "@example.com", "en", "ko", "US");
		}
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(6)) {
			for (int i = 0; i < 6; i++) {
				results.add(pool.submit(() -> {
					start.await();
					try {
						randomLetterService.send(minaId, "Hello, stranger!");
						return true;
					}
					catch (ApiException e) {
						assertThat(e.errorCode()).isEqualTo(ErrorCode.RANDOM_LIMIT_REACHED);
						return false;
					}
				}));
			}
			start.countDown();
			long sent = 0;
			for (Future<Boolean> r : results) {
				if (r.get()) {
					sent++;
				}
			}
			assertThat(sent).isEqualTo(3);
		}
		assertThat(jdbc.queryForObject("SELECT count(*) FROM letters WHERE sender_id = ?", Long.class, minaId))
			.isEqualTo(3);
	}

	private ResultActions sendRandom(MockHttpSession session) throws Exception {
		return mvc.perform(post("/api/letters/random").with(xsrf()).session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"body\":\"Hello, stranger!\"}"));
	}

}
