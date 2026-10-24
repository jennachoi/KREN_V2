package com.kren.user.dto;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.kren.user.Interest;
import com.kren.user.User;

/** What other members may see: no email, no stamp balance. Also used for penpal search results. */
public record PublicProfileResponse(Long id, String displayName, String country, String nativeLang,
		String learningLang, String bio, List<InterestResponse> interests, Instant createdAt) {

	public static PublicProfileResponse from(User user) {
		return new PublicProfileResponse(user.getId(), user.getDisplayName(), user.getCountry(), user.getNativeLang(),
				user.getLearningLang(), user.getBio(), interestsOf(user), user.getCreatedAt());
	}

	static List<InterestResponse> interestsOf(User user) {
		return user.getInterests().stream()
			.sorted(Comparator.comparing(Interest::getId))
			.map(InterestResponse::from)
			.toList();
	}

}
