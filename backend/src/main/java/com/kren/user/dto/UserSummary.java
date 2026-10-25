package com.kren.user.dto;

import com.kren.user.User;

/** Minimal "who is this" shown next to a letter. */
public record UserSummary(Long id, String displayName, String country, String nativeLang, String avatar) {

	public static UserSummary from(User user) {
		return new UserSummary(user.getId(), user.getDisplayName(), user.getCountry(), user.getNativeLang(),
				user.getAvatar());
	}

}
