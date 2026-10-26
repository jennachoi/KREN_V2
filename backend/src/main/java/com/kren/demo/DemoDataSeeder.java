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
import org.springframework.transaction.annotation.Transactional;

import com.kren.auth.AuthService;
import com.kren.auth.dto.SignupRequest;
import com.kren.correction.CorrectionService;
import com.kren.correction.dto.CorrectionRequest;
import com.kren.demo.DemoCast.Persona;
import com.kren.letter.LetterService;
import com.kren.stamp.StampService;
import com.kren.user.Interest;
import com.kren.user.InterestRepository;
import com.kren.user.UserRepository;
import com.kren.user.UserService;
import com.kren.user.dto.UpdateProfileRequest;

/**
 * Creates the public demo accounts and a few weeks of conversations, so visitors to the live site can
 * look around without signing up. Off by default; enabled with {@code kren.demo.seed=true}.
 *
 * <p>Everything goes through the real services (stamps, ledger, delivery rules); only the timestamps
 * are moved afterwards. The loaded version is stored in {@code app_settings}: when {@link #VERSION}
 * changes, the demo accounts (and everything attached to them) are deleted and created again.
 */
@Component
@ConditionalOnProperty(name = "kren.demo.seed", havingValue = "true")
class DemoDataSeeder implements ApplicationRunner {

	static final String VERSION = "2";
	static final String PASSWORD = DemoCast.PASSWORD;

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

	private final AuthService authService;
	private final UserService userService;
	private final LetterService letterService;
	private final CorrectionService correctionService;
	private final StampService stampService;
	private final UserRepository userRepository;
	private final InterestRepository interestRepository;
	private final DemoTimeline timeline;
	private final JdbcTemplate jdbc;

	DemoDataSeeder(AuthService authService, UserService userService, LetterService letterService,
			CorrectionService correctionService, StampService stampService, UserRepository userRepository,
			InterestRepository interestRepository, DemoTimeline timeline, JdbcTemplate jdbc) {
		this.authService = authService;
		this.userService = userService;
		this.letterService = letterService;
		this.correctionService = correctionService;
		this.stampService = stampService;
		this.userRepository = userRepository;
		this.interestRepository = interestRepository;
		this.timeline = timeline;
		this.jdbc = jdbc;
	}

	/**
	 * All or nothing: if seeding fails half-way, nothing is kept and the stored version doesn't change,
	 * so the next start tries again.
	 */
	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		String loaded = jdbc.query("SELECT value FROM app_settings WHERE key = 'demo_seed_version'",
				rs -> rs.next() ? rs.getString(1) : null);
		if (VERSION.equals(loaded)) {
			return;
		}
		int removed = jdbc.update("DELETE FROM users WHERE email LIKE '%' || ?", DemoCast.DOMAIN);
		createAccounts();
		writeConversations();
		timeline.backdateAccounts();
		setting("demo_seed_version", VERSION);
		// DemoTraffic prunes only letters after this one, so the seeded history stays
		setting("demo_seed_last_letter", String.valueOf(jdbc.queryForObject("SELECT max(id) FROM letters", Long.class)));
		log.info("Seeded demo data v{}: {} accounts (replaced {}), password '{}'", VERSION, DemoCast.ALL.size(),
				removed, PASSWORD);
	}

	private void createAccounts() {
		List<Interest> catalogue = interestRepository.findAll();
		for (Persona p : DemoCast.ALL) {
			long id = authService.signup(new SignupRequest(p.email(), PASSWORD, p.name(), p.country(), p.nativeLang(),
					p.learningLang()));
			Set<Long> interestIds = catalogue.stream()
				.filter(i -> p.interests().contains(i.getName()))
				.map(Interest::getId)
				.collect(Collectors.toSet());
			userService.updateProfile(id, new UpdateProfileRequest(p.name(), p.country(), p.nativeLang(),
					p.learningLang(), p.bio(), interestIds, p.avatar()));
			// enough stamps for the scripted letters below
			stampService.topUpDemoAccount(id, 20);
		}
	}

	/**
	 * Emma (the "try a demo" account) gets the richest history: long threads, corrections in both
	 * directions, letters waiting for her reply, and letters in the air both ways. Some letters with
	 * mistakes are left uncorrected so visitors can try correcting them.
	 */
	private void writeConversations() {
		long emma = id("emma");
		long minji = id("minji");
		long jihoon = id("jihoon");
		long yuna = id("yuna");

		// Emma <-> Minji: four letters over three weeks, corrected both ways.
		long m1 = landed(emma, minji, null, 480, """
				Hi Minji!

				I saw you like travel too. I'm visiting Seoul next April for two weeks. Which neighborhoods would you \
				recommend for someone who loves food and old streets?

				저는 한국 음식을 너무 좋아 해요. 서울에 처음 가서 정말 설레요!

				안녕히 계세요,
				Emma""");
		timeline.read(m1, 3);
		correct(minji, m1, 5, "Your Korean is really good! 😊",
				fix("저는 한국 음식을 너무 좋아 해요.", "저는 한국 음식을 정말 좋아해요.",
						"좋아하다 is one word; 정말 sounds more natural than 너무 here"),
				fix("서울에 처음 가서 정말 설레요!", "서울에 처음 가는 거라 정말 설레요!", "-는 거라 = because it's my first time going"));
		long m2 = landed(minji, emma, m1, 440, """
				Hi Emma! April is perfect, the cherry blossoms will be out.

				For food and old streets, try Seochon near Gyeongbokgung. I am recommend Mangwon Market for snacks, it is \
				very famous for local people.

				I corrected a couple of your Korean sentences, check the correction tab!
				Minji""");
		timeline.read(m2, 1);
		correct(emma, m2, 2, null,
				fix("I am recommend Mangwon Market for snacks, it is very famous for local people.",
						"I recommend Mangwon Market for snacks; it's very popular with locals.",
						"'recommend' doesn't need 'am'; 'popular with locals' sounds more natural"));
		long m3 = landed(emma, minji, m2, 300, """
				민지 씨, 고마워요!

				서촌에 꼭 갈 거예요. 저는 떡볶이를 먹고 싶어요 근데 너무 매울까 봐 걱정이에요. 망원시장에서 뭐 먹어야 돼요?

				P.S. My Korean teacher says I should write more, so I'm trying!
				Emma""");
		timeline.read(m3, 4);
		correct(minji, m3, 6, "점점 더 자연스러워져요!",
				fix("저는 떡볶이를 먹고 싶어요 근데 너무 매울까 봐 걱정이에요.", "떡볶이를 먹고 싶은데, 너무 매울까 봐 걱정이에요.",
						"Join the two clauses with -는데 instead of 근데"),
				fix("망원시장에서 뭐 먹어야 돼요?", "망원시장에서는 뭘 먹어야 해요?", "-야 해요 reads more naturally in writing"));
		landed(minji, emma, m3, 30, """
				Emma, don't worry about the spicy! Just ask for 덜 맵게 해 주세요, less spicy please. At Mangwon Market \
				you must try the hotteok and the dakgangjeong.

				Your Korean is getting better every letter. Write me again soon!
				Minji""");

		// Emma <-> Jihoon: he writes in English, she answers in Korean, his next letter is on its way.
		long j1 = landed(jihoon, emma, null, 200, """
				Hello Emma!

				My name is Jihoon and I am working at a coffee shop in Busan. I saw your profile and you likes \
				K-dramas. Me too! Recently I watched a drama about Jeju island and I cried so much.

				Do you drink coffee? What is your favorite?

				From Busan,
				Jihoon""");
		timeline.read(j1, 2);
		correct(emma, j1, 3, "Your English is great!",
				fix("My name is Jihoon and I am working at a coffee shop in Busan.",
						"My name is Jihoon and I work at a coffee shop in Busan.", "Use the simple present for a regular job"),
				fix("I saw your profile and you likes K-dramas.", "I saw on your profile that you like K-dramas.",
						"'you like', with no -s"));
		long j2 = landed(emma, jihoon, j1, 170, """
				지훈 씨, 안녕하세요!

				저는 커피를 매일 마셔요. 제일 좋아하는 커피는 아이스 라떼예요. 겨울에도 아이스 마셔요!

				부산에 가고 싶어요. 바다 근처에 좋은 카페 있어요?

				Emma""");
		timeline.read(j2, 1);
		correct(jihoon, j2, 2, null,
				fix("겨울에도 아이스 마셔요!", "겨울에도 아이스로 마셔요!", "아이스로 = 'iced', as the way you drink it"),
				fix("바다 근처에 좋은 카페 있어요?", "바다 근처에 좋은 카페가 있어요?", "Keep the 가 in writing"));
		inFlight(jihoon, emma, j2, 0.6, """
				Emma! Iced latte in winter has a name in Korea: 얼죽아, "iced even if I freeze to death". You are a real \
				Korean now 😄

				There are many cafes near Haeundae beach. I will draw you a map in my next letter!""");

		// Emma <-> Yuna: Yuna wrote first, Emma corrected and replied, no answer yet.
		long y1 = landed(yuna, emma, null, 120, """
				Hi Emma, I'm Yuna. I'm an illustrator in Seoul and I am loving museums. When you come to Seoul, you \
				should visit the National Museum of Korea, it is free!

				What kind of art do you like?
				Yuna""");
		timeline.read(y1, 5);
		correct(emma, y1, 6, null, fix("I'm an illustrator in Seoul and I am loving museums.",
				"I'm an illustrator in Seoul and I love museums.", "'love' for a general preference isn't used in -ing"));
		long y2 = landed(emma, yuna, y1, 90, """
				유나 씨, 고마워요! 국립중앙박물관에 꼭 가 볼게요. 저는 수채화를 좋아해요.

				Do you have a favorite Korean painter?
				Emma""");
		timeline.read(y2, 8);

		// Waiting for Emma's reply (one unread, one read), with mistakes left for visitors to correct.
		landed(id("seoyeon"), emma, null, 10, """
				Hi Emma! I'm Seoyeon, a graduate student. I read in your profile that you are visiting Seoul.

				Have you read any Korean novels in English? I recently finished The Vegetarian again. I would love to \
				know what books you are liking!
				Seoyeon""");
		long d1 = landed(emma, id("dohyun"), null, 60, """
				Hello! This is a random letter from Emma in the US 😊

				저는 한국어를 배우고 있어요. 요즘 한국 음식 만드는 거 배우고 있어요. 좋아하는 음식이 뭐예요?""");
		timeline.read(d1, 2);
		long d2 = landed(id("dohyun"), emma, d1, 12, """
				Hi Emma! What a nice surprise. My favorite food is 김치찌개. If you are learning to cook Korean food, you \
				should try 계란말이 first, it is easy and delicious.

				Good luck!
				Dohyun""");
		timeline.read(d2, 1);

		// In the air: two letters to Emma, two from her.
		inFlight(id("haeun"), emma, null, 0.8, """
				Hi Emma! I'm Haeun, a pastry chef in Daegu. I heard you like Korean food. Have you tried 약과? I can send \
				you my recipe in my next letter!""");
		inFlight(emma, id("junho"), null, 0.3, """
				Hi Junho! I love that you're a radio producer. Which song should I listen to first to get into Korean \
				ballads?

				Emma""");
		inFlight(emma, id("soyeon"), null, 0.5, """
				Soyeon! I saw you're going to Australia on a working holiday. 저는 멜버른에 친구가 있어요. Do you want some tips?

				Emma""");

		// Around the rest of the cast, so other accounts and the maps aren't empty.
		landed(id("chloe"), minji, null, 20,
				"Hi Minji! I'm Chloe from Dublin. 저는 한국 노래를 정말 좋아해요. What do you listen to on your commute?");
		landed(id("olivia"), id("seoyeon"), null, 26,
				"Hello Seoyeon! Which English novel are you reading now? I just finished Pachinko.");
		landed(id("haruto"), id("jisoo"), null, 40, "지수 씨, 안녕하세요! 오사카에 사는 하루토입니다. 부산에 가 본 적 있어요?");
		landed(id("sofia"), id("daniel"), null, 22,
				"Hi Daniel! I'm Sofia from Madrid. Do you want to practise Spanish with me? ¡Hasta pronto!");
		inFlight(id("liam"), minji, null, 0.4, "Hey Minji! Vancouver here. 반가워요! Want to swap hiking spots?");
		inFlight(id("ava"), minji, null, 0.7, "Kia ora Minji! I'm Ava from Wellington. 한국어를 배우고 있어요. Nice to meet you!");
		inFlight(jihoon, id("ethan"), null, 0.2, "Hi Ethan! Do you play FIFA? I'm a big Busan IPark fan ⚽");
		inFlight(id("camille"), id("seojun"), null, 0.5,
				"Bonjour Seojun ! 저는 리옹에 사는 카미유예요. Quel est ton film préféré ?");
		inFlight(id("haeun"), id("noah"), null, 0.35,
				"Hi Noah! What's the best brunch in Melbourne? I'm a pastry chef in Daegu 🍰");
	}

	/** Sends (or replies) and moves the letter so it landed {@code hoursAgo} hours ago. */
	private long landed(long from, long to, Long parentId, double hoursAgo, String body) {
		long id = write(from, to, parentId, body);
		timeline.landed(id, hoursAgo);
		return id;
	}

	private void inFlight(long from, long to, Long parentId, double progress, String body) {
		timeline.inFlight(write(from, to, parentId, body), progress);
	}

	private long write(long from, long to, Long parentId, String body) {
		return parentId == null ? letterService.send(from, to, body).id() : letterService.reply(from, parentId, body).id();
	}

	private void correct(long correctorId, long letterId, double hoursAfterLanding, String comment,
			CorrectionRequest.Line... lines) {
		correctionService.correct(correctorId, letterId, new CorrectionRequest(List.of(lines), comment));
		timeline.corrected(letterId, hoursAfterLanding);
	}

	private static CorrectionRequest.Line fix(String original, String corrected, String note) {
		return new CorrectionRequest.Line(original, corrected, note);
	}

	private void setting(String key, String value) {
		jdbc.update("""
				INSERT INTO app_settings (key, value) VALUES (?, ?)
				ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value
				""", key, value);
	}

	private long id(String key) {
		return userRepository.findByEmail(key + DemoCast.DOMAIN).orElseThrow().getId();
	}

}
