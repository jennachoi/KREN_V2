package com.kren.user;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** The emoji a profile picture can be. A fixed set, so profiles can't carry arbitrary text. */
public final class Avatars {

	public static final List<String> ALL = List.of(
			"🐶", "🐱", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐸", "🐵", "🐧", "🐤", "🦉", "🦄", "🐙", "🐳",
			"🐬", "🦋", "🐢", "🦔", "🌻", "🌸", "🌵", "🍀", "🍓", "🍑", "🍉", "🍩", "☕", "🎧", "🎨", "📚",
			"🌏", "🌙", "⭐", "🌈", "⚽", "🎸", "🎮", "📷");

	private static final Set<String> ALLOWED = Set.copyOf(ALL);

	private Avatars() {
	}

	public static boolean isAllowed(String avatar) {
		return ALLOWED.contains(avatar);
	}

	public static String random() {
		return ALL.get(ThreadLocalRandom.current().nextInt(ALL.size()));
	}

}
