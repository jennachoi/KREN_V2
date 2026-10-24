package com.kren.common;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.kren.IntegrationTestSupport;

/** Needs the frontend build in classpath:/static (CI runs `npm run build` before `mvnw verify`). */
class SpaRoutingTest extends IntegrationTestSupport {

	@Test
	void clientSideRoutesServeTheAppShellWithoutLogin() throws Exception {
		mvc.perform(get("/letters/42"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("<div id=\"root\">")));
		mvc.perform(get("/")).andExpect(status().isOk());
	}

	@Test
	void unknownApiPathIsJson404NotTheAppShell() throws Exception {
		MockHttpSession me = signup("mina@example.com", "ko", "en");
		mvc.perform(get("/api/does-not-exist").session(me))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void apiStillRequiresLogin() throws Exception {
		mvc.perform(get("/api/does-not-exist")).andExpect(status().isUnauthorized());
	}

}
