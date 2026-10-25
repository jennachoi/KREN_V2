package com.kren.user;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Profile pictures are illustrated characters picked from a fixed set of presets ("m01".."m40");
 * the frontend draws each preset (DiceBear "avataaars"). A fixed set means profiles can't carry
 * arbitrary text or URLs.
 */
public final class Avatars {

	public static final List<String> ALL = IntStream.rangeClosed(1, 40).mapToObj(i -> "m%02d".formatted(i)).toList();

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
