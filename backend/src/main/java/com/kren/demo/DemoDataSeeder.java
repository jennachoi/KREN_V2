package com.kren.demo;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.kren.auth.AuthService;
import com.kren.auth.dto.SignupRequest;
import com.kren.correction.CorrectionService;
import com.kren.correction.dto.CorrectionRequest;
import com.kren.letter.LetterService;
import com.kren.user.Interest;
import com.kren.user.InterestRepository;
import com.kren.user.UserRepository;
import com.kren.user.UserService;
import com.kren.user.dto.UpdateProfileRequest;

/**
 * Creates a handful of public demo accounts (and a conversation) so visitors to the live site can
 * look around without signing up. Off by default; enabled with {@code kren.demo.seed=true}.
 * Goes through the real services, so seeded data obeys every rule real users do (stamps, ledger…).
 * Runs only when the demo accounts don't exist yet, so restarts don't duplicate anything.
 */
@Component
@ConditionalOnProperty(name = "kren.demo.seed", havingValue = "true")
class DemoDataSeeder implements ApplicationRunner {

	static final String PASSWORD = "demo1234";

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

	private record Persona(String key, String name, String country, String nativeLang, String learningLang,
			String avatar, String bio, List<String> interests) {

		String email() {
			return key + "@demo.kren";
		}

	}

	private static final List<Persona> PERSONAS = List.of(
			// English speakers learning Korean
			new Persona("emma", "Emma", "US", "en", "ko", "🌸",
					"Visiting Seoul next spring! Looking for someone to chat with about K-dramas and food.",
					List.of("Movies", "Food", "K-Culture")),
			new Persona("liam", "Liam", "CA", "en", "ko", "🐻",
					"Grad student in Vancouver. I love hiking, indie music and trying new recipes.",
					List.of("Music", "Travel", "Sports")),
			new Persona("olivia", "Olivia", "GB", "en", "ko", "📷",
					"Bookworm and amateur photographer. Learning Korean through webtoons.",
					List.of("Books", "Photography", "K-Culture")),
			new Persona("noah", "Noah", "AU", "en", "ko", "🐨",
					"Surf in the morning, Korean flashcards at night. Melbourne coffee snob.",
					List.of("Sports", "Food", "Language Learning")),
			new Persona("ava", "Ava", "NZ", "en", "ko", "🐧",
					"Primary school teacher in Wellington. Learning Korean so I can talk with my students' parents.",
					List.of("Books", "Travel", "Language Learning")),
			new Persona("ethan", "Ethan", "US", "en", "ko", "🎮",
					"Game dev in Seattle. Esports got me into Korean — now I want to read the forums myself.",
					List.of("Gaming", "Technology", "K-Culture")),
			new Persona("chloe", "Chloe", "IE", "en", "ko", "🍀",
					"Dublin nurse who fell in love with Korean ballads. Slowly working through TTMIK.",
					List.of("Music", "K-Culture", "Language Learning")),
			new Persona("james", "James", "GB", "en", "ko", "☕",
					"History nerd and tea enthusiast. Planning a temple stay in Gyeongju.",
					List.of("Travel", "Books", "Food")),
			new Persona("mia", "Mia", "CA", "en", "ko", "🎨",
					"Illustrator from Toronto. I draw every letter I get, so write me something fun!",
					List.of("Art", "K-Culture", "Movies")),
			new Persona("lucas", "Lucas", "US", "en", "ko", "🎸",
					"Session guitarist in Austin. Trying to understand the lyrics of my favourite bands.",
					List.of("Music", "Language Learning", "Technology")),
			// Korean speakers learning English
			new Persona("minji", "Minji", "KR", "ko", "en", "🦊",
					"서울에 사는 개발자예요. Let's exchange letters about tech and travel!",
					List.of("Travel", "Technology", "Language Learning")),
			new Persona("jihoon", "Jihoon", "KR", "ko", "en", "⚽",
					"Busan native, barista, and a big football fan. 영어로 편지 쓰는 연습 중이에요.",
					List.of("Sports", "Food", "Music")),
			new Persona("yuna", "Yuna", "KR", "ko", "en", "🌙",
					"Illustrator who loves museums. I want to write about art and everyday life.",
					List.of("Art", "Books", "Photography")),
			new Persona("seoyeon", "Seoyeon", "KR", "ko", "en", "📚",
					"대학원생이에요. Reading English novels without a dictionary is my dream.",
					List.of("Books", "Movies", "Language Learning")),
			new Persona("dohyun", "Dohyun", "KR", "ko", "en", "🐯",
					"Software engineer in Pangyo. Ask me anything about Korean food or tech.",
					List.of("Technology", "Gaming", "Food")),
			new Persona("haeun", "Haeun", "KR", "ko", "en", "🍓",
					"Pastry chef in Daegu! I'd love to swap recipes from your country.",
					List.of("Food", "Travel", "Photography")),
			new Persona("junho", "Junho", "KR", "ko", "en", "🎧",
					"Radio producer. Tell me your favourite songs and I'll tell you mine.",
					List.of("Music", "Movies", "K-Culture")),
			new Persona("soyeon", "Soyeon", "KR", "ko", "en", "🌻",
					"Preparing for a working holiday in Australia. 영어 편지 친구 구해요!",
					List.of("Travel", "Sports", "Language Learning")),
			// Other pairs, so the map isn't only Korea and English-speaking countries
			new Persona("haruto", "Haruto", "JP", "ja", "ko", "🐱",
					"Osaka salaryman learning Korean for a trip to Busan. Let's eat everything.",
					List.of("Food", "Travel", "K-Culture")),
			new Persona("jisoo", "Jisoo", "KR", "ko", "ja", "🐼",
					"Anime fan studying Japanese. 일본어 편지 친구 찾아요!",
					List.of("Movies", "Art", "Language Learning")),
			new Persona("sofia", "Sofia", "ES", "es", "en", "🍉",
					"Architecture student in Madrid. I want to practise writing in English.",
					List.of("Art", "Travel", "Photography")),
			new Persona("mateo", "Mateo", "MX", "es", "en", "🌵",
					"Teacher from Guadalajara, learning English for my master's abroad.",
					List.of("Books", "Music", "Language Learning")),
			new Persona("daniel", "Daniel", "US", "en", "es", "⭐",
					"Nurse in San Diego learning Spanish to talk with my patients.",
					List.of("Sports", "Food", "Language Learning")),
			new Persona("camille", "Camille", "FR", "fr", "ko", "🦋",
					"Lyon. K-pop got me started, now I'm hooked on Korean poetry.",
					List.of("Music", "Books", "K-Culture")),
			new Persona("seojun", "Seojun", "KR", "ko", "fr", "🐳",
					"프랑스 유학 준비 중이에요. Let's talk about films and wine.",
					List.of("Movies", "Food", "Travel")),
			new Persona("isla", "Isla", "AU", "en", "ko", "🦉",
					"Night-owl librarian in Sydney. I collect stamps — real ones too!",
					List.of("Books", "Photography", "Language Learning")));

	private final AuthService authService;
	private final UserService userService;
	private final LetterService letterService;
	private final CorrectionService correctionService;
	private final UserRepository userRepository;
	private final InterestRepository interestRepository;
	private final JdbcTemplate jdbc;

	DemoDataSeeder(AuthService authService, UserService userService, LetterService letterService,
			CorrectionService correctionService, UserRepository userRepository, InterestRepository interestRepository,
			JdbcTemplate jdbc) {
		this.authService = authService;
		this.userService = userService;
		this.letterService = letterService;
		this.correctionService = correctionService;
		this.userRepository = userRepository;
		this.interestRepository = interestRepository;
		this.jdbc = jdbc;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (userRepository.existsByEmail(PERSONAS.getFirst().email())) {
			return;
		}
		List<Interest> catalogue = interestRepository.findAll();
		for (Persona p : PERSONAS) {
			long id = authService.signup(new SignupRequest(p.email(), PASSWORD, p.name(), p.country(), p.nativeLang(),
					p.learningLang()));
			Set<Long> interestIds = catalogue.stream()
				.filter(i -> p.interests().contains(i.getName()))
				.map(Interest::getId)
				.collect(Collectors.toSet());
			userService.updateProfile(id, new UpdateProfileRequest(p.name(), p.country(), p.nativeLang(),
					p.learningLang(), p.bio(), interestIds, p.avatar()));
		}

		long emma = idOf("emma");
		long minji = idOf("minji");
		long liam = idOf("liam");
		var first = letterService.send(emma, minji, """
				Hi Minji!

				I saw you like travel too. I'm visiting Seoul next April for two weeks. \
				Which neighborhoods would you recommend for someone who loves food and old streets?

				저는 한국 음식을 너무 좋아 해요. 서울에 처음 가서 정말 설레요!

				안녕히 계세요,
				Emma""");
		backdate(first.id(), 3);
		// Minji marks up Emma's Korean (and earns a stamp for it), so the demo shows a correction.
		correctionService.correct(minji, first.id(), new CorrectionRequest(List.of(
				new CorrectionRequest.Line("저는 한국 음식을 너무 좋아 해요.", "저는 한국 음식을 정말 좋아해요.",
						"좋아하다 is one word; 정말 sounds more natural than 너무 here"),
				new CorrectionRequest.Line("서울에 처음 가서 정말 설레요!", "서울에 처음 가는 거라 정말 설레요!",
						"-는 거라 = because it's my first time going")),
				"Your Korean is really good! 😊"));
		var answer = letterService.reply(minji, first.id(), """
				Hi Emma! April is perfect, the cherry blossoms will be out.

				For food and old streets, try Seochon near Gyeongbokgung, and Mangwon Market for snacks. \
				Tell me what dishes you want to try and I'll find good places!

				I corrected a couple of your Korean sentences — check the correction tab!
				Minji""");
		backdate(answer.id(), 2);

		// Letters that arrived and are waiting for an answer (fills the "awaiting your reply" list).
		landed(idOf("chloe"), minji, 1, "Hi Minji! I'm Chloe from Dublin. 저는 한국 노래를 정말 좋아해요. What do you listen to on your commute?");
		landed(idOf("olivia"), idOf("seoyeon"), 1, "Hello Seoyeon! Which English novel are you reading now? I just finished 'Pachinko'.");
		landed(idOf("haruto"), idOf("jisoo"), 2, "지수 씨, 안녕하세요! 오사카에 사는 하루토입니다. 부산에 가 본 적 있어요?");
		landed(idOf("sofia"), idOf("daniel"), 1, "Hi Daniel! I'm Sofia from Madrid. Do you want to practise Spanish with me? ¡Hasta pronto!");

		// Letters still in the air, so the maps have something to show on first login.
		letterService.send(liam, minji, "Hey Minji! Vancouver here. 반가워요 — want to swap hiking spots?");
		letterService.send(idOf("ava"), minji, "Kia ora Minji! I'm Ava from Wellington. 한국어를 배우고 있어요. Nice to meet you!");
		letterService.send(idOf("jihoon"), idOf("ethan"), "Hi Ethan! Do you play FIFA? I'm a big Busan IPark fan ⚽");
		letterService.send(idOf("camille"), idOf("seojun"), "Bonjour Seojun ! 저는 리옹에 사는 카미유예요. Quel est ton film préféré ?");
		letterService.send(idOf("haeun"), idOf("noah"), "Hi Noah! What's the best brunch in Melbourne? I'm a pastry chef in Daegu 🍰");
		log.info("Seeded {} demo accounts (password '{}')", PERSONAS.size(), PASSWORD);
	}

	private void landed(long from, long to, int daysAgo, String body) {
		backdate(letterService.send(from, to, body).id(), daysAgo);
	}

	/**
	 * Pretends a seeded letter was sent {@code days} ago and has long since landed, so the demo opens on
	 * a conversation already in progress. The arrival is marked processed: nobody was online to notify.
	 */
	private void backdate(long letterId, int days) {
		jdbc.update("""
				UPDATE letters
				SET sent_at = sent_at - make_interval(days => ?), deliver_at = sent_at - make_interval(days => ?),
				    arrival_notified_at = sent_at - make_interval(days => ?)
				WHERE id = ?
				""", days, days, days, letterId);
	}

	private long idOf(String key) {
		return userRepository.findByEmail(key + "@demo.kren").orElseThrow().getId();
	}

}
