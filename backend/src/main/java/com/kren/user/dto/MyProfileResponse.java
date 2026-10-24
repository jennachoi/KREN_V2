package com.kren.user.dto;

import java.time.Instant;
import java.util.List;

import com.kren.user.User;

/** The signed-in user's own view: includes private fields like email and stamp balance. */
public record MyProfileResponse(Long id, String email, String displayName, String country, String nativeLang,
		String learningLang, String bio, List<InterestResponse> interests, int stampBalance, Instant createdAt) {

	public static MyProfileResponse from(User user) {
		return new MyProfileResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCountry(),
				user.getNativeLang(), user.getLearningLang(), user.getBio(), PublicProfileResponse.interestsOf(user),
				user.getStampBalance(), user.getCreatedAt());
	}

}
