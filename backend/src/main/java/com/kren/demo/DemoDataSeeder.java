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
			String bio, List<String> interests) {

		String email() {
			return key + "@demo.kren";
		}

	}

	private static final List<Persona> PERSONAS = List.of(
			new Persona("emma", "Emma", "US", "en", "ko",
					"Visiting Seoul next spring! Looking for someone to chat with about K-dramas and food.",
					List.of("Movies", "Food", "K-Culture")),
			new Persona("liam", "Liam", "CA", "en", "ko",
					"Grad student in Vancouver. I love hiking, indie music and trying new recipes.",
					List.of("Music", "Travel", "Sports")),
			new Persona("olivia", "Olivia", "GB", "en", "ko",
					"Bookworm and amateur photographer. Learning Korean through webtoons.",
					List.of("Books", "Photography", "K-Culture")),
			new Persona("minji", "Minji", "KR", "ko", "en",
					"서울에 사는 개발자예요. Let's exchange letters about tech and travel!",
					List.of("Travel", "Technology", "Language Learning")),
			new Persona("jihoon", "Jihoon", "KR", "ko", "en",
					"Busan native, barista, and a big football fan. 영어로 편지 쓰는 연습 중이에요.",
					List.of("Sports", "Food", "Music")),
			new Persona("yuna", "Yuna", "KR", "ko", "en",
					"Illustrator who loves museums. I want to write about art and everyday life.",
					List.of("Art", "Books", "Photography")));

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
					p.learningLang(), p.bio(), interestIds));
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
		// Liam's letter is still in the air, so the tracking map has something to show.
		letterService.send(liam, minji, "Hey Minji! Vancouver here. 반가워요 — want to swap hiking spots?");
		log.info("Seeded {} demo accounts (password '{}')", PERSONAS.size(), PASSWORD);
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
