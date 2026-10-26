package com.kren.demo;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.kren.correction.dto.CorrectionRequest;

/**
 * Letters the demo traffic generator sends, picked by the language the writer is learning. Each
 * comes with the corrections a native speaker would make, so the generator can mark them up too.
 * Every {@code original} must appear verbatim in the body.
 */
final class DemoLetters {

	record Template(String body, List<CorrectionRequest.Line> corrections, String comment) {

		CorrectionRequest correction() {
			return new CorrectionRequest(corrections, comment);
		}

	}

	private static CorrectionRequest.Line fix(String original, String corrected, String note) {
		return new CorrectionRequest.Line(original, corrected, note);
	}

	/** Written by people learning Korean. */
	static final List<Template> KOREAN_LEARNERS = List.of(
			new Template("""
					안녕하세요! 오늘은 비가 와서 집에 있었어요.

					저는 라면을 끓였는데 너무 매워서 우유를 마셨어요. 한국 사람들은 매운 음식을 어떻게 잘 먹어요?

					다음 편지에서 좋아하는 음식 알려 주세요!""",
					List.of(fix("한국 사람들은 매운 음식을 어떻게 잘 먹어요?", "한국 사람들은 어떻게 매운 음식을 그렇게 잘 먹어요?",
							"어떻게 usually comes right before the verb phrase it asks about")),
					"맞아요, 우유가 최고예요 😄"),
			new Template("""
					Hi! 요즘 한국어 공부를 매일 하고 있어요.

					어제 처음으로 한국 드라마를 자막 없이 봤어요. 반만 이해했지만 기분이 좋았어요. 추천하는 드라마 있어요?""",
					List.of(fix("반만 이해했지만 기분이 좋았어요.", "반밖에 이해 못 했지만 기분이 좋았어요.",
							"반밖에 + negative sounds more natural for 'only half'")),
					"자막 없이 봤다니 대단해요!"),
			new Template("""
					안녕하세요! 저는 주말에 친구하고 한국 식당에 갔어요.

					김치찌개를 먹었어요. 정말 맛있었어요 근데 조금 짰어요. 당신은 김치찌개 좋아해요?""",
					List.of(fix("정말 맛있었어요 근데 조금 짰어요.", "정말 맛있었는데 조금 짰어요.",
							"Join the clauses with -는데 instead of 근데"),
							fix("당신은 김치찌개 좋아해요?", "김치찌개 좋아하세요?",
									"당신 sounds distant in letters; just drop it")),
					null),
			new Template("""
					반가워요! 다음 달에 서울에 여행 가요.

					지하철 타는 거 어려워요? 저는 길을 잘 잃어버려서 걱정돼요. 서울에서 꼭 가야 하는 곳 하나만 알려 주세요.""",
					List.of(fix("지하철 타는 거 어려워요?", "지하철 타기 어려워요?",
							"Both work; -기 is a little more natural in writing")),
					"서울 지하철은 정말 쉬워요. 걱정하지 마세요!"),
			new Template("""
					안녕! 오늘 처음으로 한글로 일기를 썼어요.

					아직 문법이 많이 틀려요. 그래도 재미있어요. 혹시 제 편지를 고쳐 줄 수 있어요?""",
					List.of(fix("아직 문법이 많이 틀려요.", "아직 문법을 많이 틀려요.",
							"틀리다 takes the thing you get wrong as an object (을/를)")),
					"물론이죠! 계속 써 주세요."));

	/** Written by people learning English. */
	static final List<Template> ENGLISH_LEARNERS = List.of(
			new Template("""
					Hello! How are you today?

					I am very busy this week because I have an exam. After the exam I will go to the cafe and eat a cake. Do you like sweet things?""",
					List.of(fix("After the exam I will go to the cafe and eat a cake.",
							"After the exam I'm going to go to a cafe and have some cake.",
							"'a cafe' (any cafe); 'have some cake' sounds more natural than 'eat a cake'")),
					"Good luck on your exam!"),
			new Template("""
					Hi! Yesterday I watched a movie with my friends.

					The movie was very touching so I cried a lot. Do you cry when you watch sad movie?""",
					List.of(fix("Do you cry when you watch sad movie?", "Do you cry when you watch sad movies?",
							"Use the plural for things in general")),
					null),
			new Template("""
					Nice to meet you! I live in Seoul and I am working as a nurse.

					My hobby is hiking. Last weekend I climbed Bukhansan and the view was amazing. What is your hobby?""",
					List.of(fix("I live in Seoul and I am working as a nurse.", "I live in Seoul and I work as a nurse.",
							"Use the simple present for your regular job")),
					"Bukhansan looks beautiful!"),
			new Template("""
					Hi there! It is snowing in Korea now.

					I want to learn English more better, so I write letters every day. Can you recommend me a good English book?""",
					List.of(fix("I want to learn English more better, so I write letters every day.",
							"I want to get better at English, so I write letters every day.",
							"'more better' is a double comparative; 'get better at' is the natural phrase"),
							fix("Can you recommend me a good English book?", "Can you recommend a good English book?",
									"'recommend' doesn't take an indirect object like 'me'")),
					"Writing every day is the best way!"),
			new Template("""
					Hello again! Thank you for your last letter.

					I visited Busan on holiday. The sea was so beautiful and the seafood was very fresh. Have you ever been in Korea?""",
					List.of(fix("Have you ever been in Korea?", "Have you ever been to Korea?",
							"'been to' a place you visited")),
					null));

	/** For the other language pairs, without corrections. */
	static final List<Template> OTHERS = List.of(
			new Template("Hi! Greetings from my city. What does a normal weekend look like where you live?", List.of(),
					null),
			new Template("Thanks for your letter! I tried the recipe you mentioned. What should I cook next?",
					List.of(), null),
			new Template("Hello! I just started learning your language. Any tips for a beginner?", List.of(), null));

	private DemoLetters() {
	}

	static Template pick(String learningLang) {
		List<Template> pool = switch (learningLang) {
			case "ko" -> KOREAN_LEARNERS;
			case "en" -> ENGLISH_LEARNERS;
			default -> OTHERS;
		};
		return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
	}

	/** The template a letter body came from, if any (so its prepared corrections can be applied). */
	static Template forBody(String body) {
		for (List<Template> pool : List.of(KOREAN_LEARNERS, ENGLISH_LEARNERS, OTHERS)) {
			for (Template t : pool) {
				if (t.body().strip().equals(body.strip())) {
					return t;
				}
			}
		}
		return null;
	}

}
