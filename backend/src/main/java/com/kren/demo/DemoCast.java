package com.kren.demo;

import java.util.List;
import java.util.Set;

/** The public demo accounts: who they are, what they speak and what they like. */
final class DemoCast {

	static final String DOMAIN = "@demo.kren";
	static final String PASSWORD = "demo1234";

	/** The accounts visitors log into. The traffic generator never writes or corrects as them. */
	static final Set<String> LOGIN_ACCOUNTS = Set.of("emma", "minji");

	record Persona(String key, String name, String country, String nativeLang, String learningLang, String avatar,
			String bio, List<String> interests) {

		String email() {
			return key + DOMAIN;
		}

	}

	private DemoCast() {
	}

	static final List<Persona> ALL = List.of(
			// English speakers learning Korean
			new Persona("emma", "Emma", "US", "en", "ko", "m06",
					"Visiting Seoul next spring! Looking for someone to chat with about K-dramas and food.",
					List.of("Movies", "Food", "K-Culture")),
			new Persona("liam", "Liam", "CA", "en", "ko", "m02",
					"Grad student in Vancouver. I love hiking, indie music and trying new recipes.",
					List.of("Music", "Travel", "Sports")),
			new Persona("olivia", "Olivia", "GB", "en", "ko", "m04",
					"Bookworm and amateur photographer. Learning Korean through webtoons.",
					List.of("Books", "Photography", "K-Culture")),
			new Persona("noah", "Noah", "AU", "en", "ko", "m34",
					"Surf in the morning, Korean flashcards at night. Melbourne coffee snob.",
					List.of("Sports", "Food", "Language Learning")),
			new Persona("ava", "Ava", "NZ", "en", "ko", "m25",
					"Primary school teacher in Wellington. Learning Korean so I can talk with my students' parents.",
					List.of("Books", "Travel", "Language Learning")),
			new Persona("ethan", "Ethan", "US", "en", "ko", "m20",
					"Game dev in Seattle. Esports got me into Korean — now I want to read the forums myself.",
					List.of("Gaming", "Technology", "K-Culture")),
			new Persona("chloe", "Chloe", "IE", "en", "ko", "m05",
					"Dublin nurse who fell in love with Korean ballads. Slowly working through TTMIK.",
					List.of("Music", "K-Culture", "Language Learning")),
			new Persona("james", "James", "GB", "en", "ko", "m30",
					"History nerd and tea enthusiast. Planning a temple stay in Gyeongju.",
					List.of("Travel", "Books", "Food")),
			new Persona("mia", "Mia", "CA", "en", "ko", "m14",
					"Illustrator from Toronto. I draw every letter I get, so write me something fun!",
					List.of("Art", "K-Culture", "Movies")),
			new Persona("lucas", "Lucas", "US", "en", "ko", "m09",
					"Session guitarist in Austin. Trying to understand the lyrics of my favourite bands.",
					List.of("Music", "Language Learning", "Technology")),
			// Korean speakers learning English
			new Persona("minji", "Minji", "KR", "ko", "en", "m40",
					"서울에 사는 개발자예요. Let's exchange letters about tech and travel!",
					List.of("Travel", "Technology", "Language Learning")),
			new Persona("jihoon", "Jihoon", "KR", "ko", "en", "m32",
					"Busan native, barista, and a big football fan. 영어로 편지 쓰는 연습 중이에요.",
					List.of("Sports", "Food", "Music")),
			new Persona("yuna", "Yuna", "KR", "ko", "en", "m33",
					"Illustrator who loves museums. I want to write about art and everyday life.",
					List.of("Art", "Books", "Photography")),
			new Persona("seoyeon", "Seoyeon", "KR", "ko", "en", "m12",
					"대학원생이에요. Reading English novels without a dictionary is my dream.",
					List.of("Books", "Movies", "Language Learning")),
			new Persona("dohyun", "Dohyun", "KR", "ko", "en", "m23",
					"Software engineer in Pangyo. Ask me anything about Korean food or tech.",
					List.of("Technology", "Gaming", "Food")),
			new Persona("haeun", "Haeun", "KR", "ko", "en", "m11",
					"Pastry chef in Daegu! I'd love to swap recipes from your country.",
					List.of("Food", "Travel", "Photography")),
			new Persona("junho", "Junho", "KR", "ko", "en", "m19",
					"Radio producer. Tell me your favourite songs and I'll tell you mine.",
					List.of("Music", "Movies", "K-Culture")),
			new Persona("soyeon", "Soyeon", "KR", "ko", "en", "m10",
					"Preparing for a working holiday in Australia. 영어 편지 친구 구해요!",
					List.of("Travel", "Sports", "Language Learning")),
			// Other pairs, so the map isn't only Korea and English-speaking countries
			new Persona("haruto", "Haruto", "JP", "ja", "ko", "m36",
					"Osaka salaryman learning Korean for a trip to Busan. Let's eat everything.",
					List.of("Food", "Travel", "K-Culture")),
			new Persona("jisoo", "Jisoo", "KR", "ko", "ja", "m31",
					"Anime fan studying Japanese. 일본어 편지 친구 찾아요!",
					List.of("Movies", "Art", "Language Learning")),
			new Persona("sofia", "Sofia", "ES", "es", "en", "m37",
					"Architecture student in Madrid. I want to practise writing in English.",
					List.of("Art", "Travel", "Photography")),
			new Persona("mateo", "Mateo", "MX", "es", "en", "m13",
					"Teacher from Guadalajara, learning English for my master's abroad.",
					List.of("Books", "Music", "Language Learning")),
			new Persona("daniel", "Daniel", "US", "en", "es", "m29",
					"Nurse in San Diego learning Spanish to talk with my patients.",
					List.of("Sports", "Food", "Language Learning")),
			new Persona("camille", "Camille", "FR", "fr", "ko", "m35",
					"Lyon. K-pop got me started, now I'm hooked on Korean poetry.",
					List.of("Music", "Books", "K-Culture")),
			new Persona("seojun", "Seojun", "KR", "ko", "fr", "m21",
					"프랑스 유학 준비 중이에요. Let's talk about films and wine.",
					List.of("Movies", "Food", "Travel")),
			new Persona("isla", "Isla", "AU", "en", "ko", "m08",
					"Night-owl librarian in Sydney. I collect stamps — real ones too!",
					List.of("Books", "Photography", "Language Learning")));
}
