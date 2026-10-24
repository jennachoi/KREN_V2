package com.kren.correction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.kren.IntegrationTestSupport;
import com.kren.common.ApiException;
import com.kren.common.ErrorCode;
import com.kren.correction.dto.CorrectionRequest;
import com.kren.notification.SseHubTestAccess;
import com.kren.stamp.StampService;

class CorrectionApiTest extends IntegrationTestSupport {

	static final String BODY = "안녕하세요. 저는 에마 입니다. 한국어를 공부해요.";

	@Autowired
	CorrectionService correctionService;

	@Autowired
	SseHubTestAccess sse;

	MockHttpSession emma; // writes in Korean
	MockHttpSession minji; // native Korean speaker, corrects
	MockHttpSession eve;
	long letterId;

	@BeforeEach
	void letterHasLanded() throws Exception {
		emma = signup("emma@example.com", "en", "ko", "US");
		minji = signup("minji@example.com", "ko", "en", "KR");
		eve = signup("eve@example.com", "en", "ko", "US");
		letterId = idOf(mvc.perform(post("/api/letters").with(xsrf()).session(emma)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"recipientId\":%d,\"body\":\"%s\"}".formatted(userId("minji@example.com"), BODY))));
		letLettersArrive();
	}

	@AfterEach
	void closeStreams() {
		sse.closeAll();
	}

	@Test
	void recipientCorrectsOnceAndEarnsAStamp() throws Exception {
		int before = stampBalance("minji@example.com");

		correct(minji, validCorrection())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.corrector.displayName").value("minji"))
			.andExpect(jsonPath("$.lines[0].original").value("저는 에마 입니다."))
			.andExpect(jsonPath("$.lines[0].corrected").value("저는 에마입니다."))
			.andExpect(jsonPath("$.lines[0].note").value("이다 attaches to the noun"))
			.andExpect(jsonPath("$.comment").value("Great first letter!"));

		assertThat(stampBalance("minji@example.com")).isEqualTo(before + StampService.CORRECTION_REWARD);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM stamp_ledger WHERE reason = 'CORRECTION' AND letter_id = ?",
				Long.class, letterId)).isEqualTo(1);

		// the writer sees it on the letter and can open it
		mvc.perform(get("/api/letters/{id}", letterId).session(emma)).andExpect(jsonPath("$.corrected").value(true));
		mvc.perform(get("/api/letters/{id}/correction", letterId).session(emma))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lines.length()").value(2));
	}

	@Test
	void secondCorrectionIsRejectedAndPaysNothing() throws Exception {
		correct(minji, validCorrection()).andExpect(status().isCreated());
		int after = stampBalance("minji@example.com");

		correct(minji, validCorrection()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_CORRECTED"));
		assertThat(stampBalance("minji@example.com")).isEqualTo(after);
	}

	@Test
	void onlyTheRecipientMayCorrect() throws Exception {
		correct(emma, validCorrection()).andExpect(status().isForbidden()); // the writer
		correct(eve, validCorrection()).andExpect(status().isForbidden()); // a stranger
		mvc.perform(get("/api/letters/{id}/correction", letterId).session(eve)).andExpect(status().isForbidden());
	}

	@Test
	void cannotCorrectWhatHasNotArrived() throws Exception {
		long inFlight = idOf(mvc.perform(post("/api/letters").with(xsrf()).session(emma)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"recipientId\":%d,\"body\":\"또 안녕하세요.\"}".formatted(userId("minji@example.com")))));
		mvc.perform(post("/api/letters/{id}/correction", inFlight).with(xsrf()).session(minji)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"lines\":[{\"original\":\"또 안녕하세요.\",\"corrected\":\"다시 안녕하세요.\"}]}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("LETTER_IN_TRANSIT"));
	}

	@Test
	void linesMustComeFromTheLetterAndChangeSomething() throws Exception {
		correct(minji, "{\"lines\":[{\"original\":\"I never wrote this.\",\"corrected\":\"Something else.\"}]}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_CORRECTION"));
		correct(minji, "{\"lines\":[{\"original\":\"안녕하세요.\",\"corrected\":\"안녕하세요.\"}]}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_CORRECTION"));
		correct(minji, "{\"lines\":[]}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		assertThat(jdbc.queryForObject("SELECT count(*) FROM corrections", Long.class)).isZero();
	}

	@Test
	void correctionsCollectInTheWritersNotebook() throws Exception {
		correct(minji, validCorrection());

		mvc.perform(get("/api/corrections/notebook").session(emma))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].letterId").value(letterId))
			.andExpect(jsonPath("$.content[0].lines[*].corrected").value(contains("저는 에마입니다.", "한국어를 공부하고 있어요.")));
		mvc.perform(get("/api/corrections/notebook").session(minji)).andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void writerIsNotifiedLive() throws Exception {
		MvcResult stream = mvc.perform(get("/api/notifications/stream").session(emma).accept(MediaType.TEXT_EVENT_STREAM))
			.andExpect(request().asyncStarted())
			.andReturn();

		correct(minji, validCorrection()).andExpect(status().isCreated());

		// SSE is always UTF-8 (per spec); MockMvc would otherwise decode as ISO-8859-1.
		assertThat(stream.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("event:correction").contains("저는 에마입니다.");
	}

	/** Double-click / retry storm on "submit correction": exactly one correction, exactly one stamp. */
	@RepeatedTest(3)
	void parallelSubmitsPayOutOnce() throws Exception {
		long minjiId = userId("minji@example.com");
		int before = stampBalance("minji@example.com");
		var req = new CorrectionRequest(List.of(new CorrectionRequest.Line("저는 에마 입니다.", "저는 에마입니다.", null)), null);

		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(6)) {
			for (int i = 0; i < 6; i++) {
				results.add(pool.submit(() -> {
					start.await();
					try {
						correctionService.correct(minjiId, letterId, req);
						return true;
					}
					catch (ApiException e) {
						assertThat(e.errorCode()).isEqualTo(ErrorCode.ALREADY_CORRECTED);
						return false;
					}
				}));
			}
			start.countDown();
			long wins = 0;
			for (Future<Boolean> r : results) {
				if (r.get()) {
					wins++;
				}
			}
			assertThat(wins).isEqualTo(1);
		}
		assertThat(stampBalance("minji@example.com")).isEqualTo(before + 1);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM corrections", Long.class)).isEqualTo(1);
	}

	private static String validCorrection() {
		return """
				{"lines":[
				  {"original":"저는 에마 입니다.","corrected":"저는 에마입니다.","note":"이다 attaches to the noun"},
				  {"original":"한국어를 공부해요.","corrected":"한국어를 공부하고 있어요."}
				 ],
				 "comment":"Great first letter!"}
				""";
	}

	private ResultActions correct(MockHttpSession session, String json) throws Exception {
		return mvc.perform(post("/api/letters/{id}/correction", letterId).with(xsrf()).session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private static long idOf(ResultActions result) throws Exception {
		return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
	}

}
